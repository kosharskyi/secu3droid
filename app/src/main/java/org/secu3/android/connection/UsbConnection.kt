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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import org.secu3.android.models.RawPacket
import org.secu3.android.models.packets.out.ChangeModePacket
import org.secu3.android.utils.Task
import org.secu3.android.utils.UserPrefs
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UsbConnection @Inject constructor(
    private val usbManager: UsbManager,
    prefs: UserPrefs,
) : Connection<UsbSerialPort>(prefs) {

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
                val port = connectToDevice(currentDevice!!)
                currentCoroutineContext().ensureActive()
                Log.d(this.javaClass.simpleName, "Connected to USB device")
                startReadingData(port)
            } catch (e: Exception) {
                currentCoroutineContext().ensureActive()
                mConnectionStateFlow.emit(ConnectionFailed(e))
                Log.e(this.javaClass.simpleName, "Connection failed: ${e.message}")
                delay(2000)
                reconnect()
            }
        }
    }

    private fun connectToDevice(usbDevice: UsbDevice): UsbSerialPort {

        val device = usbManager.deviceList.values.firstOrNull {
            it.deviceId == usbDevice.deviceId
        }?: throw IllegalStateException("No USB devices found")

        val driver = UsbSerialProber.getDefaultProber().probeDevice(device)
            ?: throw IllegalStateException("No suitable driver found for device")

        val port = driver.ports.first()
        val connection = usbManager.openDevice(device)
            ?: throw IllegalStateException("Could not open connection to USB device")

        val installed = installEndpoint(port)
        if (!installed) {
            connection.close()
            throw IOException("USB connection is no longer current")
        }
        try {
            port.open(connection)
            port.setParameters(115200, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE)
            if (!isCurrentEndpoint(port)) throw IOException("USB connection was stopped while opening")
            return port
        } catch (failure: Exception) {
            discardEndpoint(port)
            throw failure
        }
    }

    override fun disconnect() {
        currentDevice = null
        val port = detachCurrentEndpoint()
        backgroundScope.launch {
            var closeFailure: Exception? = null
            try {
                port?.close()
            } catch (e: Exception) {
                closeFailure = e
            }
            mConnectionStateFlow.emit(Disconnected)
            closeFailure?.let { failure ->
                mConnectionStateFlow.emit(ConnectionError(failure))
                Log.e(this.javaClass.simpleName, "Error while disconnecting: ${failure.message}")
            }
        }
    }

    private fun startReadingData(port: UsbSerialPort) {
        if (!isCurrentEndpoint(port)) return
        sendData(ChangeModePacket.getPacket(Task.Secu3ReadFirmwareInfo))
        scope.launch {
            if (!isCurrentEndpoint(port)) return@launch
            mConnectionStateFlow.emit(Connected)
            try {
                val receiver = PacketReceiver()

                while (isCurrentEndpoint(port)) {
                    val buffer = ByteArray(MAX_PACKET_SIZE)
                    val bytesRead = port.read(buffer, 1000)
                    currentCoroutineContext().ensureActive()
                    if (!isCurrentEndpoint(port)) return@launch

                    if (bytesRead <= 0) continue

                    for (i in 0 until bytesRead) {
                        receiver.receive(buffer[i].toInt() and 0xFF)?.let { packet ->
                            mReceivedPacketFlow.emit(RawPacket(packet))
                        }
                    }
                }
                if (isCurrentEndpoint(port)) connectionAttempts = 0
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                currentCoroutineContext().ensureActive()
                Log.e(this.javaClass.simpleName, "Error while reading data: ${e.message}")
                if (invalidateEndpoint(port)) {
                    connectionAttempts = 0
                    reconnect()
                }
            }
        }
    }

    override fun isEndpointOpen(endpoint: UsbSerialPort): Boolean = endpoint.isOpen

    override fun writeEndpoint(endpoint: UsbSerialPort, bytes: ByteArray) {
        endpoint.write(bytes, 1000)
    }

    override fun closeEndpoint(endpoint: UsbSerialPort) {
        try {
            endpoint.close()
        } catch (failure: Exception) {
            Log.e(javaClass.simpleName, "Error while closing failed USB port: ${failure.message}")
        }
    }

}
