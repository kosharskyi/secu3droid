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
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.secu3.android.models.packets.base.InputPacket
import org.secu3.android.models.packets.base.OutputPacket
import org.secu3.android.models.packets.input.FirmwareInfoPacket
import org.secu3.android.models.packets.input.SensorsPacket
import org.secu3.android.utils.SecuLogger
import org.secu3.android.utils.Task
import org.secu3.android.utils.UserPrefs
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Secu3ConnectionManager @Inject constructor(
    private val usbConnection: UsbConnection,
    private val btConnection: BtConnection,
    private val mPrefs: UserPrefs,
    private val secuLogger: SecuLogger,
    private val serviceManager: ServiceManager,
    private val notificationPublisher: ConnectionNotificationPublisher,
) {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    @Volatile private var currentConnection: Connection<*>? = null
    private var appVisible = false
    private val backgroundWorkEnabled = MutableStateFlow<Boolean?>(null)
    private val backgroundPreferenceMutex = Mutex()
    private val _connectionState = MutableSharedFlow<ConnectionState>(replay = 1, extraBufferCapacity = 32).also { it.tryEmit(Disconnected) }
    @Volatile var currentConnectionState: ConnectionState = Disconnected
        private set
    val connectionStateFlow = _connectionState.asSharedFlow()
    private val _firmwareInfo = MutableStateFlow<FirmwareInfoPacket?>(null)
    val firmwareInfo = _firmwareInfo.asStateFlow()
    val fwInfo: FirmwareInfoPacket? get() = firmwareInfo.value

    val receivedPacketFlow: Flow<InputPacket> = merge(
        usbConnection.receivedPacketFlow,
        btConnection.receivedPacketFlow,
    ).shareIn(scope, SharingStarted.WhileSubscribed(5_000), replay = 0)

    fun receivedPacketFlow(onSubscribed: () -> Unit): Flow<InputPacket> = flow {
        val connection = currentConnection ?: return@flow awaitCancellation()
        emitAll(connection.receivedPacketFlow(onSubscribed))
    }

    val isConnectionRunning get() = currentConnection?.isRunning == true
    val isConnected get() = currentConnection?.isReady == true
    val isUsbSessionLocked get() = currentConnection === usbConnection && usbConnection.isSessionLocked
    val activeUsbDeviceIdOrNull get() = usbConnection.activeDeviceId

    @Synchronized fun requestUsbConnection(device: UsbDevice): UsbPermissionRequest? {
        reconcileUsbBeforeAdmission()
        val request = usbConnection.requestConnection(device, ::disconnectForTransportChange)
        synchronizeUsbOutcome()
        return request
    }

    @Synchronized fun completeUsbPermissionRequest(device: UsbDevice, token: Long, permissionGranted: Boolean) {
        reconcileUsbBeforeAdmission()
        usbConnection.completePermissionRequest(device, token, permissionGranted, ::disconnectForTransportChange)
        synchronizeUsbOutcome()
    }

    @Synchronized fun cancelUsbPermissionRequest(token: Long) = usbConnection.cancelPendingPermission(token)

    @Synchronized fun synchronizeUsbAttachments(): UsbPermissionRequest? = synchronizeUsbAttachments(usbConnection.attachedDevicesSnapshot())
    @Synchronized internal fun synchronizeUsbAttachments(snapshot: Result<List<UsbDevice>>): UsbPermissionRequest? {
        if (snapshot.isFailure) return null
        // Apply detach cleanup before this same call admits a replacement USB run.
        usbConnection.reconcileAttachments(snapshot)
        synchronizeUsbOutcome()
        val request = usbConnection.synchronizeAttachments(snapshot, ::disconnectForTransportChange)
        synchronizeUsbOutcome()
        return request
    }

    private fun reconcileUsbBeforeAdmission() {
        usbConnection.reconcileAttachments(usbConnection.attachedDevicesSnapshot())
        synchronizeUsbOutcome()
    }

    /** Reconcile physical attachment changes without starting a newly discovered device. */
    @Synchronized fun reconcileUsbAttachments() = reconcileUsbAttachments(usbConnection.attachedDevicesSnapshot())
    @Synchronized internal fun reconcileUsbAttachments(snapshot: Result<List<UsbDevice>>) {
        usbConnection.reconcileAttachments(snapshot)
        synchronizeUsbOutcome()
    }

    @Synchronized fun onUsbDeviceDetached(deviceId: Int): Boolean {
        val detached = usbConnection.onDeviceDetached(deviceId)
        if (detached) drainPendingEvents(usbConnection)
        return detached
    }

    private fun synchronizeUsbOutcome() {
        if (usbConnection.isRunning && currentConnection !== usbConnection) {
            selectConnection(usbConnection)
        }
        drainPendingEvents(usbConnection)
    }

    suspend fun startBtConnection() = coroutineScope {
        val start = synchronized(this@Secu3ConnectionManager) {
            if (usbConnection.activeDeviceId != null) return@coroutineScope
            disconnectForTransportChange()
            launch(start = CoroutineStart.UNDISPATCHED) { btConnection.startConnection() }
                .also { selectConnection(btConnection); drainPendingEvents(btConnection) }
        }
        start.join()
    }

    private fun selectConnection(connection: Connection<*>) {
        val replacingConnection = currentConnection != null
        if (replacingConnection) secuLogger.stopLogging()
        currentConnection = connection
        _firmwareInfo.value = null
        publishConnectionState(Connecting)
        reconcileConnectionService()
        drainPendingEvents(connection)
    }

    /** A transport change is a real disconnect before the new transport is admitted. */
    private fun disconnectForTransportChange() {
        val previous = currentConnection ?: return
        previous.stopConnection()
        drainPendingEvents(previous)
        if (currentConnection === previous) {
            currentConnection = null
            _firmwareInfo.value = null
            secuLogger.stopLogging()
            publishConnectionState(Disconnected)
            reconcileConnectionService()
        }
    }

    @Synchronized fun stopConnection(): Boolean {
        if (currentConnection?.allowsManualDisconnect == false) return false
        return stopConnectionInternal()
    }

    /** UI reports visibility; the manager applies the shared USB/Bluetooth background policy. */
    @Synchronized fun onAppForegrounded() {
        appVisible = true
        reconcileConnectionService()
    }

    /** Stop active work only when the app truly leaves the foreground and background work is disabled. */
    @Synchronized fun onAppBackgrounded() {
        appVisible = false
        usbConnection.cancelPendingPermission()
        if (currentConnection == null) return
        if (backgroundWorkEnabled.value != false) return
        stopConnectionInternal()
    }

    /** Preference changes update service use immediately without ending a live connection. */
    suspend fun onBackgroundWorkPreferenceChanged(enabled: Boolean) {
        backgroundPreferenceMutex.withLock {
            mPrefs.setWakeLockEnabled(enabled)
            val persistedValue = mPrefs.settings.first().isWakeLockEnabled
            backgroundWorkEnabled.first { it == persistedValue }
        }
    }

    /** Re-posts the active notification after the user grants Android's notification permission. */
    @Synchronized fun refreshConnectionNotification() {
        val state = currentConnectionState
        if (backgroundWorkEnabled.value == true && isServiceEligibleState(state)) {
            notificationPublisher.refreshPostedNotification()
        }
    }

    @Synchronized private fun reconcileConnectionService() {
        if (!isServiceEligibleState(currentConnectionState)) {
            stopConnectionService()
            return
        }
        when (backgroundWorkEnabled.value) {
            true -> try {
                serviceManager.startForegroundService(SecuConnectionService::class.java)
            } catch (failure: Exception) {
                Log.e(javaClass.simpleName, "Could not start connection service", failure)
            }
            false -> stopConnectionService()
            null -> Unit
        }
    }

    @Synchronized private fun stopConnectionInternal(): Boolean {
        currentConnection?.let { active ->
            active.stopConnection()
            drainPendingEvents(active)
        }
        currentConnection = null
        _firmwareInfo.value = null
        usbConnection.stopConnection()
        btConnection.stopConnection()
        drainPendingEvents(usbConnection)
        drainPendingEvents(btConnection)
        secuLogger.stopLogging()
        publishConnectionState(Disconnected)
        stopConnectionService()
        return true
    }

    fun sendNewTask(task: Task) = sendOutPacket(task.getPacket())
    fun sendOutPacket(packet: OutputPacket) {
        currentConnection?.takeIf { it.isReady }?.sendData(packet)
    }

    @Synchronized private fun handleConnectionEvent(connection: Connection<*>, event: ConnectionEvent) {
        if (connection !== currentConnection) return
        _firmwareInfo.value = event.firmware
        if (event.end != null) {
            secuLogger.stopLogging()
            if (event.end == ConnectionEvent.End.STOPPED) {
                currentConnection = null
                stopConnectionService()
            }
            if (connection !== btConnection || event.end == ConnectionEvent.End.STOPPED) {
                publishConnectionState(Disconnected)
                reconcileConnectionService()
            }
        }
        publishConnectionState(event.state)
        reconcileConnectionService()
    }

    private fun publishConnectionState(state: ConnectionState) {
        if (currentConnectionState == state) return
        currentConnectionState = state
        _connectionState.tryEmit(state)
    }

    init {
        scope.launch {
            mPrefs.observeSettings { failure ->
                synchronized(this@Secu3ConnectionManager) {
                    backgroundWorkEnabled.value = null
                    reconcileConnectionService()
                }
                Log.e(javaClass.simpleName, "Could not read background connection preference", failure)
            }.collect { settings ->
                synchronized(this@Secu3ConnectionManager) {
                    backgroundWorkEnabled.value = settings.isWakeLockEnabled
                    applyBackgroundWorkPolicy(settings.isWakeLockEnabled)
                }
            }
        }
        scope.launch(start = CoroutineStart.UNDISPATCHED) { usbConnection.eventAvailable.collect { drainPendingEvents(usbConnection) } }
        scope.launch(start = CoroutineStart.UNDISPATCHED) { btConnection.eventAvailable.collect { drainPendingEvents(btConnection) } }
        scope.launch { receivedPacketFlow.filterIsInstance<SensorsPacket>().collect { secuLogger.logPacket(it) } }
    }

    private fun applyBackgroundWorkPolicy(enabled: Boolean) {
        if (!enabled && !appVisible && currentConnection != null) {
            stopConnectionInternal()
        } else {
            reconcileConnectionService()
        }
    }

    @Synchronized private fun drainPendingEvents(connection: Connection<*>) {
        connection.consumePendingEvents { handleConnectionEvent(connection, it) }
    }

    private fun stopConnectionService() {
        try {
            serviceManager.stopService(SecuConnectionService::class.java)
        } catch (failure: Exception) {
            Log.e(javaClass.simpleName, "Could not stop connection service", failure)
        }
    }

    private fun isServiceEligibleState(state: ConnectionState) = state == Connected || state == Reconnecting ||
        state == UsbReconnecting || state == UsbReconnectingAwaitingEcu
}
