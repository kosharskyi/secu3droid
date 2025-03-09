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
import kotlinx.coroutines.launch
import org.secu3.android.models.RawPacket
import org.secu3.android.models.packets.base.OutputPacket
import org.secu3.android.models.packets.out.ChangeModePacket
import org.secu3.android.utils.Task
import org.secu3.android.utils.UserPrefs
import org.threeten.bp.LocalTime
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
) : Connection(prefs) {

    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter
    private var bluetoothSocket: BluetoothSocket? = null

    private val pairedDeviceName: String?
        get() = prefs.bluetoothDeviceName

    override val isConnected: Boolean
        get() = bluetoothSocket?.isConnected == true


    fun startConnection() {
        val deviceName = pairedDeviceName
        if (deviceName == null || deviceName.isBlank()) {
            throw IllegalStateException("Paired device name is not set in SharedPreferences")
        }
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
                connectToDevice()

                Log.i(this.javaClass.simpleName, "Connected to device")
                startReadingData()
            } catch (e: IOException) {
                mConnectionStateFlow.emit(ConnectionFailed(e))
                Log.e(this.javaClass.simpleName, "Connection failed: ${e.message}")
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

    override fun disconnect() {
        scope.launch {
            try {
                mConnectionStateFlow.emit(Disconnected)
                bluetoothSocket?.close()
                bluetoothSocket = null
            } catch (e: IOException) {
                mConnectionStateFlow.emit(ConnectionError(e))
                Log.e(this.javaClass.simpleName, "Error while disconnecting: ${e.message}")
            }
        }
    }

    private fun startReadingData() {
        scope.launch {
            mConnectionStateFlow.emit(Connected)
            try {
                val packetBuffer = IntArray(MAX_PACKET_SIZE)
                val startMarker = INPUT_PACKET_SYMBOL
                val endMarker = END_PACKET_SYMBOL
                val inputStream = bluetoothSocket?.inputStream ?: throw IOException("Input stream is null")
                val reader = BufferedReader(InputStreamReader(inputStream, StandardCharsets.ISO_8859_1))

                var idx = 0

                while (isRunning && bluetoothSocket?.isConnected == true) {
                    if (idx >= packetBuffer.size) {
                        Log.d(this.javaClass.simpleName, "Packet buffer overflow, resetting.")
                        idx = 0
                    }

                    val char = reader.read().takeIf { it != -1 } ?: continue
                    if (char == startMarker) {
                        idx = 0
                    }

                    if (char != endMarker) {
                        packetBuffer[idx++] = char
                    }

                    if (idx > 4 && char == endMarker) {
                        val escaped = escRxPacket(packetBuffer.sliceArray(IntRange(0, idx - 1)))

                        if (isChecksumValid(escaped)) {
                            mReceivedPacketFlow.emit(RawPacket(escaped))
                        }

                        idx = 0
                    }
                }
                connectionAttempts = 0
            } catch (e: IOException) {
                Log.e(this.javaClass.simpleName, "Error while reading data: ${e.message}")
                connectionAttempts = 0
                reconnect()
            }
        }

        sendData(ChangeModePacket.getPacket(Task.Secu3ReadFirmwareInfo))
    }

    override fun sendData(sendPacket: OutputPacket) {
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

                var packet = intArrayOf(OUTPUT_PACKET_SYMBOL) + sendPacket.pack()

                val checksum = calculateChecksum(packet.sliceArray(2 until packet.size))

                packet += checksum[1].toInt()
                packet += checksum[0].toInt()

                Log.e(this.javaClass.simpleName, packet.toString())

                var escaped = escTxPacket(packet)
                escaped += END_PACKET_SYMBOL

                outputStream.write(escaped.map { it.toByte() }.toByteArray())

                delay(20)
            } catch (e: IOException) {
                Log.e(this.javaClass.simpleName, "Error while sending data: ${e.message}")
            }
        }
    }
}