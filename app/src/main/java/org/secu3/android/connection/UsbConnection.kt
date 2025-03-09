/*
 *    SecuDroid  - An open source, free manager for SECU-3 engine control unit
 *    Copyright (C) 2025 Vitalii O. Kosharskyi. Ukraine, Kyiv
 *
 *    SECU-3  - An open source, free engine control unit
 *    Copyright (C) 2007-2025 Alexey A. Shabelnikov. Ukraine, Kyiv
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

import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.util.Log
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.secu3.android.models.RawPacket
import org.secu3.android.models.packets.base.OutputPacket
import org.secu3.android.models.packets.out.ChangeModePacket
import org.secu3.android.utils.Task
import org.secu3.android.utils.UserPrefs
import org.threeten.bp.LocalTime
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UsbConnection @Inject constructor(
    private val usbManager: UsbManager,
    prefs: UserPrefs,
) : Connection(prefs) {

    private var usbSerialPort: UsbSerialPort? = null

    override val isConnected: Boolean
        get() = usbSerialPort?.isOpen == true


    private var currentDevice: UsbDevice? = null

    fun startConnection(device: UsbDevice) {
        isRunning = true
        currentDevice = device
        reconnect()
    }

    private fun reconnect() {
        if (!isRunning) return

        scope.launch {

            if (connectionAttempts >= maxConnectionAttempts) {
                isRunning = false
                currentDevice = null

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

            currentDevice ?: return@launch

            try {
                connectToDevice(currentDevice!!)
                Log.d(this.javaClass.simpleName, "Connected to USB device")
                startReadingData()
            } catch (e: Exception) {
                mConnectionStateFlow.emit(ConnectionFailed(e))
                Log.e(this.javaClass.simpleName, "Connection failed: ${e.message}")
                delay(2000)
                reconnect()
            }
        }
    }

    private fun connectToDevice(usbDevice: UsbDevice) {

        val device = usbManager.deviceList.values.firstOrNull {
            it.deviceId == usbDevice.deviceId
        }?: throw IllegalStateException("No USB devices found")

        val connection = usbManager.openDevice(device)
            ?: throw IllegalStateException("Could not open connection to USB device")

        val driver = UsbSerialProber.getDefaultProber().probeDevice(device)
            ?: throw IllegalStateException("No suitable driver found for device")

        usbSerialPort = driver.ports.first()
        usbSerialPort?.open(connection)
        usbSerialPort?.setParameters(115200, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE)
    }

    override fun disconnect() {
        currentDevice = null
        scope.launch {
            try {
                mConnectionStateFlow.emit(Disconnected)
                usbSerialPort?.close()
                usbSerialPort = null
            } catch (e: Exception) {
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

                var idx = 0

                while (isRunning && usbSerialPort?.isOpen == true) {
                    val buffer = ByteArray(MAX_PACKET_SIZE)
                    val bytesRead = usbSerialPort?.read(buffer, 1000) ?: continue

                    if (bytesRead <= 0) continue

                    for (i in 0 until bytesRead) {
                        val byte = buffer[i].toInt() and 0xFF


                        if (byte == startMarker) {
                            idx = 0
                        }

                        if (byte != endMarker) {
                            if (idx < packetBuffer.size) {
                                packetBuffer[idx++] = byte
                            } else {
                                Log.d(this.javaClass.simpleName, "Packet buffer overflow, resetting.")
                                idx = 0
                            }
                        } else {
                            if (idx > 2) {
                                val escaped = escRxPacket(packetBuffer.sliceArray(0 until idx))

                                if (isChecksumValid(escaped)) {
                                    mReceivedPacketFlow.emit(RawPacket(escaped))
                                }
                            } else {
                                Log.d(this.javaClass.simpleName, "Incomplete packet received, skipping.")
                            }
                            idx = 0
                        }
                    }
                }
                connectionAttempts = 0
            } catch (e: Exception) {
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

            while (LocalTime.now().isBefore(endTime) && (isRunning.not() || usbSerialPort?.isOpen != true)) {
                delay(200)
            }

            if (isRunning.not() && usbSerialPort?.isOpen != true) {
                return@launch
            }

            try {

                var packet = intArrayOf(OUTPUT_PACKET_SYMBOL) + sendPacket.pack()

                val checksum = calculateChecksum(packet.sliceArray(2 until packet.size))

                packet += checksum[1].toInt()
                packet += checksum[0].toInt()

                Log.e(this.javaClass.simpleName, packet.toString())

                var escaped = escTxPacket(packet)
                escaped += END_PACKET_SYMBOL

                val buffer = escaped.map { it.toByte() }.toByteArray()

                usbSerialPort?.write(buffer, 1000) ?: throw IllegalStateException("Output stream is null")
            } catch (e: IOException) {
                Log.e(this.javaClass.simpleName, "Error while sending data: ${e.message}")
            }
        }
    }
}
