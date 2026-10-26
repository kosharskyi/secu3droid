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
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
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

    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter
    private val connectionLock = Any()
    private var connectionJob: Job? = null
    private val _sessionEvents = MutableSharedFlow<BluetoothConnectionEvent>(extraBufferCapacity = 32)
    val sessionEvents = _sessionEvents.asSharedFlow()

    private val pairedDeviceAddress: String?
        get() = BluetoothDeviceAddress.normalize(prefs.bluetoothDeviceAddress)

    fun startConnection(generation: Long) {
        val deviceAddress = pairedDeviceAddress
        if (deviceAddress == null) {
            throw IllegalStateException("Paired device address is not set in SharedPreferences")
        }
        stopConnection()
        val job = scope.launch(start = CoroutineStart.LAZY) {
            runConnection(currentCoroutineContext()[Job]!!, generation, deviceAddress)
        }
        synchronized(connectionLock) {
            connectionJob = job
            isRunning = true
        }
        job.start()
    }

    private suspend fun runConnection(job: Job, generation: Long, address: String) {
        try {
            var attempts = 0
            while (isCurrent(job)) {
                if (attempts >= maxConnectionAttempts) {
                    synchronized(connectionLock) {
                        if (connectionJob === job && isRunning) isRunning = false
                        else return
                    }
                    publishState(job, generation, Disconnected)
                    publishState(job, generation, ConnectionTimeout, retriesExhausted = true)
                    Log.i(javaClass.simpleName, "Max connection attempts reached. Stopping connection attempts.")
                    return
                }

                publishState(job, generation, InProgress)
                attempts++
                if (!isCurrent(job)) return
                Log.d(javaClass.simpleName, "Attempting to connect: $attempts/$maxConnectionAttempts")
                if (attempts > 1) publishState(job, generation, ConnectionAttempt(attempts))

                val socket = try {
                    connectToDevice(job, address)
                } catch (e: IOException) {
                    currentCoroutineContext().ensureActive()
                    if (!isCurrent(job)) return
                    publishState(job, generation, ConnectionFailed(e))
                    Log.e(javaClass.simpleName, "Connection failed: ${e.message}")
                    delay(RETRY_DELAY_MS)
                    continue
                }

                currentCoroutineContext().ensureActive()
                if (!isCurrent(job)) return
                Log.i(javaClass.simpleName, "Connected to device")
                sendData(ChangeModePacket.getPacket(Task.Secu3ReadFirmwareInfo))
                publishState(job, generation, Connected)
                try {
                    readSocket(job, socket)
                    if (!isCurrentEndpoint(socket)) return
                } catch (e: IOException) {
                    currentCoroutineContext().ensureActive()
                    if (!isCurrent(job)) return
                    Log.e(javaClass.simpleName, "Error while reading data: ${e.message}")
                    if (!invalidateEndpoint(socket)) return
                    attempts = 0
                }
            }
        } finally {
            synchronized(connectionLock) {
                if (connectionJob === job) connectionJob = null
            }
        }
    }

    private fun connectToDevice(job: Job, address: String): BluetoothSocket {
        var socket: BluetoothSocket? = null
        try {
            if (!isCurrent(job)) throw IOException("Bluetooth connection was stopped")
            val device = bluetoothAdapter?.bondedDevices?.let {
                BluetoothDeviceAddress.find(address, it) { device -> device.address }
            }
                ?: throw IOException("Paired device address is unavailable")

            val newSocket = device.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"))
            socket = newSocket
            val installed = synchronized(connectionLock) {
                connectionJob === job && isRunning && installEndpoint(newSocket)
            }
            if (!installed) throw IOException("Bluetooth connection is no longer current")
            bluetoothAdapter.cancelDiscovery()
            newSocket.connect()
            if (!isCurrentEndpoint(newSocket) || !isCurrent(job)) throw IOException("Bluetooth connection was stopped while connecting")
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

    private suspend fun readSocket(job: Job, socket: BluetoothSocket) {
        if (!isCurrentEndpoint(socket) || !isCurrent(job)) return

        val receiver = PacketReceiver()
        val inputStream = socket.inputStream
        val reader = BufferedReader(InputStreamReader(inputStream, StandardCharsets.ISO_8859_1))
        while (isCurrentEndpoint(socket) && isCurrent(job)) {
            val packet = receiver.readNext(reader)
            currentCoroutineContext().ensureActive()
            if (!isCurrentEndpoint(socket) || !isCurrent(job)) return
            mReceivedPacketFlow.emit(RawPacket(packet))
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
        job: Job,
        generation: Long,
        state: ConnectionState,
        retriesExhausted: Boolean = false,
    ) {
        if (!isOwned(job)) return
        mConnectionStateFlow.emit(state)
        if (!isOwned(job)) return
        _sessionEvents.emit(
            BluetoothConnectionEvent(
                generation = generation,
                state = state,
                retriesExhausted = retriesExhausted,
            ),
        )
    }

    private fun isCurrent(job: Job): Boolean = synchronized(connectionLock) {
        connectionJob === job && job.isActive && isRunning
    }

    private fun isOwned(job: Job): Boolean = synchronized(connectionLock) {
        connectionJob === job && job.isActive
    }

    private companion object {
        const val RETRY_DELAY_MS = 2_000L
    }

}
