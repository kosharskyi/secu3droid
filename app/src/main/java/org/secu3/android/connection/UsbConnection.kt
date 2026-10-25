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
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.secu3.android.models.RawPacket
import org.secu3.android.models.packets.out.ChangeModePacket
import org.secu3.android.utils.Task
import org.secu3.android.utils.UserPrefs
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

internal data class UsbAttemptPacket(val attemptId: Long, val packet: RawPacket)

@Singleton
class UsbConnection @Inject constructor(
    private val usbManager: UsbManager,
    prefs: UserPrefs,
) : Connection<UsbSerialPort>(prefs) {

    private var currentDevice: UsbDevice? = null
    @Volatile private var attemptGeneration = 0L
    private val _attemptFailures = MutableSharedFlow<Long>(extraBufferCapacity = 16)
    internal val attemptFailures = _attemptFailures.asSharedFlow()
    private val _attemptPackets = MutableSharedFlow<UsbAttemptPacket>()
    internal val attemptPackets = _attemptPackets.asSharedFlow()

    /** Distinguishes an unavailable device list from a successful empty list. */
    fun attachedDevicesSnapshot(): Result<List<UsbDevice>> = try {
        Result.success(usbManager.deviceList.values.toList())
    } catch (failure: Exception) {
        Result.failure(failure)
    }

    /** Resolves an event payload back to the device that is currently attached. */
    fun attachedDevice(device: UsbDevice): UsbDevice? = runCatching {
        usbManager.deviceList.values.firstOrNull {
            it.deviceId == device.deviceId && it.deviceName == device.deviceName &&
                it.vendorId == device.vendorId && it.productId == device.productId
        }
    }.getOrNull()

    fun supportsSerialConnection(device: UsbDevice): Boolean = runCatching {
        UsbSerialProber.getDefaultProber().probeDevice(device)?.ports?.isNotEmpty() == true
    }.getOrDefault(false)

    fun hasPermission(device: UsbDevice): Boolean = runCatching { usbManager.hasPermission(device) }.getOrDefault(false)

    internal fun startConnection(device: UsbDevice, attemptId: Long) {
        stopConnection()
        val attempt = ++attemptGeneration
        connectionAttempts = 0
        isRunning = true
        currentDevice = device
        scope.launch {
            mConnectionStateFlow.emit(InProgress)
            try {
                val port = connectToDevice(device)
                currentCoroutineContext().ensureActive()
                if (attempt != attemptGeneration) return@launch
                Log.d(this.javaClass.simpleName, "Connected to USB device")
                startReadingData(port, attempt, attemptId)
            } catch (e: Exception) {
                currentCoroutineContext().ensureActive()
                if (attempt != attemptGeneration) return@launch
                isRunning = false
                currentDevice = null
                mConnectionStateFlow.emit(ConnectionFailed(e))
                mConnectionStateFlow.emit(Disconnected)
                _attemptFailures.emit(attemptId)
                Log.e(this.javaClass.simpleName, "Connection failed: ${e.message}")
            }
        }
    }

    private fun connectToDevice(usbDevice: UsbDevice): UsbSerialPort {

        val device = attachedDevice(usbDevice) ?: throw IllegalStateException("No USB devices found")

        val driver = UsbSerialProber.getDefaultProber().probeDevice(device)
            ?: throw IllegalStateException("No suitable driver found for device")

        val port = driver.ports.firstOrNull() ?: throw IllegalStateException("No serial ports found for USB device")
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
        attemptGeneration++
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

    private fun startReadingData(port: UsbSerialPort, attempt: Long, attemptId: Long) {
        if (!isCurrentEndpoint(port)) return
        sendData(ChangeModePacket.getPacket(Task.Secu3ReadFirmwareInfo))
        scope.launch {
            if (!isCurrentEndpoint(port)) return@launch
            try {
                val receiver = PacketReceiver()

                while (attempt == attemptGeneration && isCurrentEndpoint(port)) {
                    val buffer = ByteArray(MAX_PACKET_SIZE)
                    val bytesRead = port.read(buffer, 1000)
                    currentCoroutineContext().ensureActive()
                    if (attempt != attemptGeneration || !isCurrentEndpoint(port)) return@launch

                    if (bytesRead <= 0) continue

                    for (i in 0 until bytesRead) {
                        receiver.receive(buffer[i].toInt() and 0xFF)?.let { packet ->
                            if (attempt != attemptGeneration) return@launch
                            val rawPacket = RawPacket(packet)
                            _attemptPackets.emit(UsbAttemptPacket(attemptId, rawPacket))
                            mReceivedPacketFlow.emit(rawPacket)
                        }
                    }
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                currentCoroutineContext().ensureActive()
                if (attempt != attemptGeneration) return@launch
                Log.e(this.javaClass.simpleName, "Error while reading data: ${e.message}")
                if (invalidateEndpoint(port)) {
                    if (attempt != attemptGeneration) return@launch
                    isRunning = false
                    currentDevice = null
                    mConnectionStateFlow.emit(ConnectionFailed(e))
                    mConnectionStateFlow.emit(Disconnected)
                    _attemptFailures.emit(attemptId)
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
