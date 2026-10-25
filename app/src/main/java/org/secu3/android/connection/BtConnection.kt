/*
 *    SecuDroid  - An open source, free manager for SECU-3 engine control unit
 *    Copyright (C) 2024 Vitalii O. Kosharskyi. Ukraine, Kyiv
 *
 *    SECU-3  - An open source, free engine control unit
 *    Copyright (C) 2007-2024 Alexey A. Shabelnikov. Ukraine, Kyiv
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *    contacts:
 *                    http://secu-3.org
 *                    email: vetalkosharskiy@gmail.com
 */

package org.secu3.android.connection

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.util.Log
import kotlinx.coroutines.delay
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.secu3.android.models.RawPacket
import org.secu3.android.models.packets.out.ChangeModePacket
import org.secu3.android.utils.Task
import org.secu3.android.utils.UserPrefs
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BtConnection @Inject constructor(
    private val prefs: UserPrefs,
    bluetoothManager: BluetoothManager
) : Connection<BluetoothSocket>(prefs) {

    private class Session(val generation: Long, val address: String) {
        var attempts = 0
    }

    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter
    private val sessionLock = Any()
    private var activeSession: Session? = null
    private val _sessionEvents = MutableSharedFlow<BluetoothConnectionEvent>(extraBufferCapacity = 32)
    val sessionEvents = _sessionEvents.asSharedFlow()

    private val pairedDeviceAddress: String?
        get() = BluetoothDeviceAddress.normalize(prefs.bluetoothDeviceAddress)

    fun startConnection(generation: Long) {
        val deviceAddress = pairedDeviceAddress
        if (deviceAddress == null) {
            throw IllegalStateException("Paired device address is not set in SharedPreferences")
        }
        val session = Session(generation, deviceAddress)
        synchronized(sessionLock) {
            activeSession = session
            isRunning = true
            connectionAttempts = 0
        }
        reconnect(session)
    }

    private fun reconnect(session: Session) {
        if (!isActive(session) || !isRunning) return

        scope.launch {
            if (!isActive(session) || !isRunning) return@launch
            if (session.attempts >= maxConnectionAttempts) {
                val exhausted = synchronized(sessionLock) {
                    if (activeSession !== session || !isRunning) false
                    else { isRunning = false; true }
                }
                if (!exhausted) return@launch

                publishState(session, Disconnected)
                publishState(session, ConnectionTimeout, retriesExhausted = true)

                Log.i(this.javaClass.simpleName, "Max connection attempts reached. Stopping connection attempts.")
                return@launch
            }

            publishState(session, InProgress)

            session.attempts++
            synchronized(sessionLock) {
                if (activeSession === session) connectionAttempts = session.attempts
            }
            Log.d(this.javaClass.simpleName, "Attempting to connect: ${session.attempts}/$maxConnectionAttempts")
            if (session.attempts > 1) {
                publishState(session, ConnectionAttempt(session.attempts))
            }

            try {
                val socket = connectToDevice(session)
                currentCoroutineContext().ensureActive()
                val sessionStillActive = synchronized(sessionLock) { activeSession === session && isRunning }
                if (!sessionStillActive) return@launch

                Log.i(this.javaClass.simpleName, "Connected to device")
                startReadingData(socket, session)
            } catch (e: IOException) {
                currentCoroutineContext().ensureActive()
                if (!isActive(session) || !isRunning) return@launch
                publishState(session, ConnectionFailed(e))
                Log.e(this.javaClass.simpleName, "Connection failed: ${e.message}")
                delay(2000)
                reconnect(session)
            }
        }
    }

    private fun connectToDevice(session: Session): BluetoothSocket {
        var socket: BluetoothSocket? = null
        try {
            if (!isActive(session)) throw IOException("Bluetooth session was replaced")
            val address = session.address
            val device = bluetoothAdapter?.bondedDevices?.let {
                BluetoothDeviceAddress.find(address, it) { device -> device.address }
            }
                ?: throw IOException("Paired device address is unavailable")

            val newSocket = device.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"))
            socket = newSocket
            val installed = synchronized(sessionLock) {
                activeSession === session && isRunning && installEndpoint(newSocket)
            }
            if (!installed) throw IOException("Bluetooth connection is no longer current")
            bluetoothAdapter.cancelDiscovery()
            newSocket.connect()
            if (!isCurrentEndpoint(newSocket) || !isActive(session)) throw IOException("Bluetooth connection was stopped while connecting")
            return newSocket
        } catch (e: SecurityException) {
            socket?.let(::discardEndpoint)
            throw IOException("Missing Bluetooth permission", e)
        } catch (e: IOException) {
            socket?.let(::discardEndpoint)
            throw e
        }
    }

    override fun disconnect() {
        val socket = detachCurrentEndpoint()
        backgroundScope.launch {
            var closeFailure: IOException? = null
            try {
                socket?.close()
            } catch (e: IOException) {
                closeFailure = e
            }
            mConnectionStateFlow.emit(Disconnected)
            closeFailure?.let { failure ->
                mConnectionStateFlow.emit(ConnectionError(failure))
                Log.e(this.javaClass.simpleName, "Error while disconnecting: ${failure.message}")
            }
        }
    }

    private fun startReadingData(socket: BluetoothSocket, session: Session) {
        if (!isCurrentEndpoint(socket) || !isActive(session)) return
        sendData(ChangeModePacket.getPacket(Task.Secu3ReadFirmwareInfo))

        scope.launch {
            if (!isCurrentEndpoint(socket) || !isActive(session)) return@launch
            publishState(session, Connected)
            try {
                val receiver = PacketReceiver()
                val inputStream = socket.inputStream
                val reader = BufferedReader(InputStreamReader(inputStream, StandardCharsets.ISO_8859_1))

                while (isCurrentEndpoint(socket) && isActive(session)) {
                    val packet = receiver.readNext(reader)
                    currentCoroutineContext().ensureActive()
                    if (!isCurrentEndpoint(socket) || !isActive(session)) return@launch
                    mReceivedPacketFlow.emit(RawPacket(packet))
                }
                if (isCurrentEndpoint(socket) && isActive(session)) {
                    session.attempts = 0
                    synchronized(sessionLock) { if (activeSession === session) connectionAttempts = 0 }
                }
            } catch (e: IOException) {
                currentCoroutineContext().ensureActive()
                Log.e(this.javaClass.simpleName, "Error while reading data: ${e.message}")
                if (invalidateEndpoint(socket) && isActive(session) && isRunning) {
                    session.attempts = 0
                    synchronized(sessionLock) { if (activeSession === session) connectionAttempts = 0 }
                    reconnect(session)
                }
            }
        }
    }

    override fun isEndpointOpen(endpoint: BluetoothSocket): Boolean = endpoint.isConnected

    override fun writeEndpoint(endpoint: BluetoothSocket, bytes: ByteArray) {
        endpoint.outputStream.write(bytes)
    }

    override fun closeEndpoint(endpoint: BluetoothSocket) {
        try {
            endpoint.close()
        } catch (failure: IOException) {
            Log.e(javaClass.simpleName, "Error while closing failed Bluetooth socket: ${failure.message}")
        }
    }

    override suspend fun afterEndpointWrite() {
        delay(20)
    }

    private suspend fun publishState(
        session: Session,
        state: ConnectionState,
        retriesExhausted: Boolean = false,
    ) {
        if (!isActive(session)) return
        mConnectionStateFlow.emit(state)
        if (!isActive(session)) return
        _sessionEvents.emit(
            BluetoothConnectionEvent(
                generation = session.generation,
                state = state,
                retriesExhausted = retriesExhausted,
            ),
        )
    }

    private fun isActive(session: Session): Boolean = synchronized(sessionLock) { activeSession === session }

}
