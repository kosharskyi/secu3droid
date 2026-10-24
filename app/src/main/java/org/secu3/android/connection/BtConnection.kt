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
    private var connectionAddress: String? = null

    private val pairedDeviceAddress: String?
        get() = BluetoothDeviceAddress.normalize(prefs.bluetoothDeviceAddress)

    fun startConnection() {
        val deviceAddress = pairedDeviceAddress
        if (deviceAddress == null) {
            throw IllegalStateException("Paired device address is not set in SharedPreferences")
        }
        connectionAddress = deviceAddress
        isRunning = true
        connectionAttempts = 0
        reconnect()
    }

    private fun reconnect() {
        if (!isRunning) return

        scope.launch {
            if (connectionAttempts >= maxConnectionAttempts) {
                isRunning = false

                mConnectionStateFlow.emit(Disconnected)
                mConnectionStateFlow.emit(ConnectionTimeout)

                Log.i(this.javaClass.simpleName, "Max connection attempts reached. Stopping connection attempts.")
                return@launch
            }

            mConnectionStateFlow.emit(InProgress)

            connectionAttempts++
            Log.d(this.javaClass.simpleName, "Attempting to connect: $connectionAttempts/$maxConnectionAttempts")
            if (connectionAttempts > 1) {
                mConnectionStateFlow.emit(ConnectionAttempt(connectionAttempts))
            }

            try {
                val socket = connectToDevice()
                currentCoroutineContext().ensureActive()

                Log.i(this.javaClass.simpleName, "Connected to device")
                startReadingData(socket)
            } catch (e: IOException) {
                currentCoroutineContext().ensureActive()
                mConnectionStateFlow.emit(ConnectionFailed(e))
                Log.e(this.javaClass.simpleName, "Connection failed: ${e.message}")
                delay(2000)
                reconnect()
            }
        }
    }

    private fun connectToDevice(): BluetoothSocket {
        var socket: BluetoothSocket? = null
        try {
            val address = connectionAddress ?: throw IOException("Device address is invalid")
            val device = bluetoothAdapter?.bondedDevices?.let {
                BluetoothDeviceAddress.find(address, it) { device -> device.address }
            }
                ?: throw IOException("Paired device address is unavailable")

            val newSocket = device.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"))
            socket = newSocket
            val installed = installEndpoint(newSocket)
            if (!installed) throw IOException("Bluetooth connection is no longer current")
            bluetoothAdapter.cancelDiscovery()
            newSocket.connect()
            if (!isCurrentEndpoint(newSocket)) throw IOException("Bluetooth connection was stopped while connecting")
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

    private fun startReadingData(socket: BluetoothSocket) {
        if (!isCurrentEndpoint(socket)) return
        sendData(ChangeModePacket.getPacket(Task.Secu3ReadFirmwareInfo))

        scope.launch {
            if (!isCurrentEndpoint(socket)) return@launch
            mConnectionStateFlow.emit(Connected)
            try {
                val receiver = PacketReceiver()
                val inputStream = socket.inputStream
                val reader = BufferedReader(InputStreamReader(inputStream, StandardCharsets.ISO_8859_1))

                while (isCurrentEndpoint(socket)) {
                    val packet = receiver.readNext(reader)
                    currentCoroutineContext().ensureActive()
                    if (!isCurrentEndpoint(socket)) return@launch
                    mReceivedPacketFlow.emit(RawPacket(packet))
                }
                if (isCurrentEndpoint(socket)) connectionAttempts = 0
            } catch (e: IOException) {
                currentCoroutineContext().ensureActive()
                Log.e(this.javaClass.simpleName, "Error while reading data: ${e.message}")
                if (invalidateEndpoint(socket)) {
                    connectionAttempts = 0
                    reconnect()
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

}
