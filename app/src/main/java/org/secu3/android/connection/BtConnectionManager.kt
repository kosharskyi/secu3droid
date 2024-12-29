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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import org.secu3.android.models.RawPacket
import org.secu3.android.models.packets.base.BaseOutputPacket
import org.secu3.android.models.packets.base.BaseSecu3Packet
import org.secu3.android.models.packets.base.BaseSecu3Packet.Companion.END_PACKET_SYMBOL
import org.secu3.android.models.packets.base.BaseSecu3Packet.Companion.MAX_PACKET_SIZE
import org.secu3.android.models.packets.out.ChangeModePacket
import org.secu3.android.utils.PacketUtils
import org.secu3.android.utils.Task
import org.secu3.android.utils.UserPrefs
import org.threeten.bp.LocalTime
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.lang.Thread.sleep
import java.nio.charset.StandardCharsets
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BtConnectionManager @Inject constructor(
    private val prefs: UserPrefs,
    private val bluetoothManager: BluetoothManager
) {

    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter
    private var bluetoothSocket: BluetoothSocket? = null
    var connectionAttempts = 0
        private set

    private val pairedDeviceName: String?
        get() = prefs.bluetoothDeviceName

    private val maxConnectionAttempts: Int
        get() = prefs.connectionRetries

    var isRunning = false
        private set

    private val mReceivedPacketFlow = MutableSharedFlow<RawPacket>()
    val receivedPacketFlow: Flow<RawPacket>
        get() = mReceivedPacketFlow

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun startConnection() {
        val deviceName = pairedDeviceName
        if (deviceName == null || deviceName.isBlank()) {
            throw IllegalStateException("Paired device name is not set in SharedPreferences")
        }
        isRunning = true
        connectionAttempts = 0
        reconnect()
    }

    fun stopConnection() {
        isRunning = false
        scope.cancel()
        disconnect()
    }

    private fun reconnect() {
        if (!isRunning) return

        if (connectionAttempts >= maxConnectionAttempts) {
            isRunning = false
            println("Max connection attempts reached. Stopping connection attempts.")
            return
        }

        connectionAttempts++
        println("Attempting to connect: $connectionAttempts/$maxConnectionAttempts")

        scope.launch {
            try {
                connectToDevice()

                println("Connected to device")
                listenForDisconnection()
                startReadingData()
            } catch (e: IOException) {
                println("Connection failed: ${e.message}")
                delay(2000)
                reconnect()
            }
        }
    }

    private fun connectToDevice() {
        val device = bluetoothAdapter?.bondedDevices?.find { it.name == pairedDeviceName } ?: throw IOException("Device name is null")

        bluetoothSocket = device.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"))
        bluetoothAdapter?.cancelDiscovery()
        try {
            bluetoothSocket?.connect()
        } catch (e: IOException) {
            bluetoothSocket?.close()
            bluetoothSocket = null
            throw e
        }
    }

    private fun disconnect() {
        try {
            bluetoothSocket?.close()
            bluetoothSocket = null
        } catch (e: IOException) {
            println("Error while disconnecting: ${e.message}")
        }
    }

    private fun listenForDisconnection() {
        scope.launch {
            try {
                while (isRunning && bluetoothSocket?.isConnected == true) {
                    delay(1000)
                }
                println("Connection lost")
                reconnect()
            } catch (e: Exception) {
                println("Error during listening: ${e.message}")
                reconnect()
            }
        }
    }

    private fun startReadingData() {
        scope.launch {
            try {
                val inputStream = bluetoothSocket?.inputStream ?: throw IOException("Input stream is null")
                val reader = BufferedReader(InputStreamReader(inputStream, StandardCharsets.ISO_8859_1))
                val packetBuffer = IntArray(MAX_PACKET_SIZE)
                val startMarker = BaseSecu3Packet.INPUT_PACKET_SYMBOL
                val endMarker = END_PACKET_SYMBOL

                var idx = 0

                while (isRunning && bluetoothSocket?.isConnected == true) {
                    val char = reader.read().takeIf { it != -1 }?.toChar() ?: continue
                    if (char == startMarker) {
                        idx = 0
                    }

                    if (char != endMarker) {
                        packetBuffer[idx++] = char.code
                    }

                    if (char == endMarker) {
                        val escaped = PacketUtils.EscRxPacket(packetBuffer.sliceArray(IntRange(0, idx - 1)))
                        val line = String(escaped, 0, escaped.size)
                        mReceivedPacketFlow.emit(RawPacket(line))
                        idx = 0
                    }
                }
                connectionAttempts = 0
            } catch (e: IOException) {
                println("Error while reading data: ${e.message}")
                connectionAttempts = 0
                reconnect()
            }
        }

        sendData(ChangeModePacket.getPacket(Task.Secu3ReadFirmwareInfo))
    }

    fun sendData(sendPacket: BaseOutputPacket) {
        scope.launch {

            val endTime = LocalTime.now().plusSeconds(10)

            while (LocalTime.now().isBefore(endTime) && (isRunning.not() || bluetoothSocket?.isConnected != true)) {
                delay(200)
            }

            if (isRunning.not() && bluetoothSocket?.isConnected != true) {
                return@launch
            }

            try {
                val outputStream = bluetoothSocket?.outputStream ?: throw IOException("Output stream is null")
                val writer = outputStream.bufferedWriter(StandardCharsets.ISO_8859_1)

                var packet = sendPacket.pack()

                val checksum = PacketUtils.calculateChecksum(packet.substring(2, packet.length))

                packet += checksum[1].toInt().toChar()
                packet += checksum[0].toInt().toChar()

                Log.e(this.javaClass.simpleName, packet)
                var escaped = PacketUtils.EscTxPacket(packet)
                escaped += END_PACKET_SYMBOL

                writer.append(escaped)

                delay(20)
                writer.flush()
//                println("Data sent: $data")
            } catch (e: IOException) {
                println("Error while sending data: ${e.message}")
            }
        }
    }
}