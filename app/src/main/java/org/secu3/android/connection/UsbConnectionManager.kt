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

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.util.Log
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import dagger.hilt.android.qualifiers.ApplicationContext
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
import org.threeten.bp.LocalTime
import java.io.IOException
import java.nio.charset.StandardCharsets
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UsbConnectionManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val usbManager: UsbManager
) {

    var connectionAttempts = 0
        private set

    private var usbSerialPort: UsbSerialPort? = null
    var isRunning = false

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val mReceivedPacketFlow = MutableSharedFlow<RawPacket>()
    val receivedPacketFlow: Flow<RawPacket>
        get() = mReceivedPacketFlow

    private val usbPermissionActionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (ACTION_USB_PERMISSION == intent.action) {
                synchronized(this) {
                    val device = intent.getParcelableExtra<android.hardware.usb.UsbDevice>(android.hardware.usb.UsbManager.EXTRA_DEVICE)
                    if (intent.getBooleanExtra(android.hardware.usb.UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        device?.let { connectToDevice(it) }
                    } else {
                        println("Permission denied for device $device")
                    }
                }
            }
        }
    }

    fun startConnection() {
        val filter = IntentFilter(ACTION_USB_PERMISSION)
        context.registerReceiver(usbPermissionActionReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        isRunning = true
        reconnect()
    }

    fun stopConnection() {
        isRunning = false
        scope.cancel()
        context.unregisterReceiver(usbPermissionActionReceiver)
        disconnect()
    }

    private fun reconnect() {
        if (!isRunning) return

        println("Attempting to connect")

        scope.launch {
            try {
                findAndRequestPermission()
                println("Connected to USB device")
                startReadingData()
            } catch (e: Exception) {
                println("Connection failed: ${e.message}")
                delay(2000)
                reconnect()
            }
        }
    }

    private fun findAndRequestPermission() {
        val usbManager = context.getSystemService(Context.USB_SERVICE) as android.hardware.usb.UsbManager
        val availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager)

        val driver = availableDrivers.firstOrNull()
            ?: throw IllegalStateException("No USB devices found")

        val device = driver.device
        if (!usbManager.hasPermission(device)) {
            val permissionIntent = PendingIntent.getBroadcast(
                context, 0, Intent(ACTION_USB_PERMISSION), PendingIntent.FLAG_IMMUTABLE
            )
            usbManager.requestPermission(device, permissionIntent)
        } else {
            connectToDevice(device)
        }
    }

    private fun connectToDevice(device: android.hardware.usb.UsbDevice) {
        val connection = usbManager.openDevice(device)
            ?: throw IllegalStateException("Could not open connection to USB device")

        val driver = UsbSerialProber.getDefaultProber().probeDevice(device)
            ?: throw IllegalStateException("No suitable driver found for device")

        usbSerialPort = driver.ports.first()
        usbSerialPort?.open(connection)
        usbSerialPort?.setParameters(115200, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE)
    }

    private fun disconnect() {
        try {
            usbSerialPort?.close()
            usbSerialPort = null
        } catch (e: Exception) {
            println("Error while disconnecting: ${e.message}")
        }
    }

    private fun startReadingData() {
        scope.launch {
            try {
                val packetBuffer = IntArray(2048) // Збільшений розмір для великих пакетів
                val startMarker: Char = BaseSecu3Packet.INPUT_PACKET_SYMBOL
                val endMarker: Char = END_PACKET_SYMBOL
                var idx = 0

                while (isRunning && usbSerialPort?.isOpen == true) {
                    val buffer = ByteArray(256) // Збільшений буфер для читання
                    val bytesRead = usbSerialPort?.read(buffer, 1000) ?: continue

                    if (bytesRead <= 0) continue

                    for (i in 0 until bytesRead) {
                        val unsignedByte = buffer[i].toInt() and 0xFF
                        val char = unsignedByte.toChar()

                        if (char == startMarker) {
                            idx = 0
                        }

                        if (char != endMarker) {
                            if (idx < packetBuffer.size) {
                                packetBuffer[idx++] = unsignedByte
                            } else {
                                println("Packet buffer overflow, resetting.")
                                idx = 0
                            }
                        } else {
                            if (idx > 2) {
                                val escaped = PacketUtils.EscRxPacket(packetBuffer.sliceArray(0 until idx))
                                val line = String(escaped.map { it.toByte() }.toByteArray(), Charsets.ISO_8859_1)
                                mReceivedPacketFlow.emit(RawPacket(line))
                            } else {
                                println("Incomplete packet received, skipping.")
                            }
                            idx = 0
                        }
                    }
                }
                connectionAttempts = 0
            } catch (e: Exception) {
                println("Error while reading data: ${e.message}")
                connectionAttempts = 0
                reconnect()
            }
        }

        sendData(ChangeModePacket.getPacket(Task.Secu3ReadFirmwareInfo))
    }

    fun sendData(packet: BaseOutputPacket) {
        scope.launch {
            val endTime = LocalTime.now().plusSeconds(10)

            while (LocalTime.now().isBefore(endTime) && (isRunning.not() || usbSerialPort?.isOpen != true)) {
                delay(200)
            }

            if (isRunning.not() && usbSerialPort?.isOpen != true) {
                return@launch
            }

            try {

                var packet = packet.pack()

                val checksum = PacketUtils.calculateChecksum(packet.substring(2, packet.length))

                packet += checksum[1].toInt().toChar()
                packet += checksum[0].toInt().toChar()

                Log.e(this.javaClass.simpleName, packet)
                var escaped = PacketUtils.EscTxPacket(packet)
                escaped += END_PACKET_SYMBOL

                val buffer = escaped.toByteArray(Charsets.ISO_8859_1)

                // Формуємо масив беззнакових байтів
                val unsignedBuffer = ByteArray(buffer.size) { i -> (buffer[i].toInt() and 0xFF).toByte() }

                // Відправляємо через USB
                usbSerialPort?.write(unsignedBuffer, 1000) ?: throw IllegalStateException("Output stream is null")
            } catch (e: IOException) {
                println("Error while sending data: ${e.message}")
            }
        }
    }

    companion object {
        private const val ACTION_USB_PERMISSION = "com.example.USB_PERMISSION"
    }
}
