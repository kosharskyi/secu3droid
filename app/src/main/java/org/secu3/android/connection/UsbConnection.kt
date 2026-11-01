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
import android.os.SystemClock
import android.util.Log
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import kotlinx.coroutines.*
import org.secu3.android.models.RawPacket
import org.secu3.android.utils.UserPrefs
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

internal class UsbEcuResponseTimeoutException : IOException("ECU stopped sending valid USB packets")

@Singleton
open class UsbConnection @Inject constructor(
    private val usbManager: UsbManager,
    prefs: UserPrefs,
) : Connection<UsbSerialPort>() {
    private var activeDevice: UsbDevice? = null

    init { observePacketCalibration(prefs) }

    fun attachedDevicesSnapshot(): Result<List<UsbDevice>> = runCatching { usbManager.deviceList.values.toList() }
    open fun attachedDevice(device: UsbDevice): UsbDevice? = attachedDevicesSnapshot().getOrNull()?.firstOrNull {
        samePhysicalDevice(it, device)
    }
    open fun supportsSerialConnection(device: UsbDevice): Boolean = runCatching {
        UsbSerialProber.getDefaultProber().probeDevice(device)?.ports?.isNotEmpty() == true
    }.getOrDefault(false)

    fun reconcileAttachments(snapshot: Result<List<UsbDevice>>) {
        synchronized(connectionLock) {
            val devices = snapshot.getOrNull() ?: return@synchronized
            activeDevice?.takeIf { active -> devices.none { samePhysicalDevice(it, active) } }?.let { onDeviceDetached(it.deviceId) }
        }
    }

    fun onDeviceDetached(deviceId: Int): Boolean = synchronized(connectionLock) {
        if (activeDevice?.deviceId != deviceId) return false
        stopConnection()
        true
    }

    internal fun startConnection(device: UsbDevice): Boolean = synchronized(connectionLock) {
        val attached = attachedDevice(device) ?: return false
        if (!supportsSerialConnection(attached) || !hasUsbPermission(attached)) return false
        if (isRunning) return false
        launchConnection { job -> runConnection(job, attached) }
        activeDevice = attached
        true
    }

    protected open fun hasUsbPermission(device: UsbDevice): Boolean =
        try { usbManager.hasPermission(device) } catch (_: SecurityException) { false }

    override fun stopConnection() = synchronized(connectionLock) {
        activeDevice = null
        super.stopConnection()
    }

    private suspend fun runConnection(job: Job, device: UsbDevice) {
        var established = false
        while (isCurrent(job)) {
            val attempt = beginAttempt(job) ?: return
            if (!hasUsbPermission(device)) {
                stopAttemptForPermissionFailure(job)
                return
            }
            if (currentConnectionState != UsbAwaitingEcu && currentConnectionState != UsbReconnecting &&
                currentConnectionState != UsbReconnectingAwaitingEcu
            ) {
                publishState(job, if (established) UsbReconnecting else Connecting)
            }
            val handshakeTimeout = scope.launch {
                delay(ECU_PACKET_TIMEOUT_MS)
                val target = synchronized(connectionLock) {
                    if (!isCurrent(job) || !isCurrentAttempt(attempt) || readyAttemptId == attempt) return@launch
                    invalidateAttempt(job, attempt, if (established) UsbReconnectingAwaitingEcu else UsbAwaitingEcu)
                }
                target?.let(::closeEndpoint)
            }
            try {
                val port = connectToDevice(device)
                currentCoroutineContext().ensureActive()
                if (!isCurrent(job)) return
                if (!isCurrentAttempt(attempt) || !isCurrentEndpoint(port)) throw IOException("USB opening expired")
                requestFirmware()
                readPort(job, port, attempt)
            } catch (failure: Exception) {
                currentCoroutineContext().ensureActive()
                if (!isCurrent(job)) return
                if (failure is SecurityException || !hasUsbPermission(device)) {
                    stopAttemptForPermissionFailure(job)
                    Log.e(javaClass.simpleName, "USB permission was revoked during an attempt", failure)
                    return
                }
                val end = if (readyAttemptId == attempt) ConnectionEvent.End.RETRYING else null
                if (end == ConnectionEvent.End.RETRYING) established = true
                val state = when {
                    failure is UsbEcuResponseTimeoutException || currentConnectionState == UsbAwaitingEcu ||
                        currentConnectionState == UsbReconnectingAwaitingEcu ->
                        if (established) UsbReconnectingAwaitingEcu else UsbAwaitingEcu
                    established -> UsbReconnecting
                    else -> Connecting
                }
                finishAttempt(job, state, end)
                Log.e(javaClass.simpleName, "USB attempt failed: ${failure.message}")
            } finally {
                handshakeTimeout.cancel()
            }
            delay(RETRY_DELAY_MS)
        }
    }

    private suspend fun readPort(job: Job, port: UsbSerialPort, attempt: Long) {
        val receiver = PacketReceiver()
        var lastPacketAt = SystemClock.elapsedRealtime()
        while (isCurrent(job) && isCurrentEndpoint(port) && isCurrentAttempt(attempt)) {
            val buffer = ByteArray(MAX_PACKET_SIZE)
            val count = port.read(buffer, 1_000)
            currentCoroutineContext().ensureActive()
            if (!isCurrent(job) || !isCurrentAttempt(attempt)) return
            if (!isCurrentEndpoint(port)) throw IOException("USB serial port closed")
            for (index in 0 until count.coerceAtLeast(0)) {
                receiver.receive(buffer[index].toInt() and 255)?.let {
                    receivePacket(attempt, RawPacket(it))
                    // Downstream/logger suspension must not consume the USB silence deadline.
                    lastPacketAt = SystemClock.elapsedRealtime()
                }
            }
            if (SystemClock.elapsedRealtime() - lastPacketAt >= ECU_PACKET_TIMEOUT_MS) throw UsbEcuResponseTimeoutException()
        }
        if (isCurrent(job) && isCurrentAttempt(attempt)) throw IOException("USB serial port closed")
    }

    override fun onFirmwarePacket(job: Job, attempt: Long) { markConnected(job, attempt) }

    private fun stopAttemptForPermissionFailure(job: Job) {
        synchronized(connectionLock) {
            if (!isCurrent(job)) return
            stopConnection()
        }
    }

    protected open fun connectToDevice(usbDevice: UsbDevice): UsbSerialPort {

        val device = attachedDevice(usbDevice) ?: throw IllegalStateException("No USB devices found")

        val driver = UsbSerialProber.getDefaultProber().probeDevice(device)
            ?: throw IllegalStateException("No suitable driver found for device")

        val port = driver.ports.firstOrNull() ?: throw IllegalStateException("No serial ports found for USB device")
        if (!hasUsbPermission(device)) throw SecurityException("USB permission is no longer granted")
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

    private fun samePhysicalDevice(first: UsbDevice, second: UsbDevice): Boolean =
        first.deviceId == second.deviceId && first.deviceName == second.deviceName &&
            first.vendorId == second.vendorId && first.productId == second.productId
    private companion object {
        const val ECU_PACKET_TIMEOUT_MS = 5_000L
        const val RETRY_DELAY_MS = 2_000L
    }
}
