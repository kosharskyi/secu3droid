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
    private val connectionServiceController: ConnectionServiceController,
) {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var nextGeneration = 0L
    /** Identifies the admitted transport request; it remains fixed while that transport retries. */
    @Volatile private var currentConnectionGeneration = 0L
    @Volatile private var currentConnection: Connection<*>? = null
    @Volatile private var currentSessionGeneration = 0L
    private var lastEventSequence = -1L
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
        val owner = currentConnection ?: return@flow awaitCancellation()
        emitAll(owner.receivedPacketFlow(onSubscribed))
    }

    val isConnectionRunning get() = currentConnection?.isRunning == true
    val isConnected get() = currentConnection?.isReady == true
    val isUsbSessionLocked get() = currentConnection === usbConnection && usbConnection.isSessionLocked
    @get:Synchronized val isUsbSessionRetrying get() = currentConnection === usbConnection &&
        currentSessionGeneration != currentConnectionGeneration && currentConnectionState != Connected
    val activeUsbDeviceIdOrNull get() = usbConnection.activeDeviceId

    @Synchronized fun requestUsbConnection(device: UsbDevice): UsbPermissionRequest? {
        val request = usbConnection.requestConnection(device, ++nextGeneration)
        synchronizeUsbOutcome()
        return request
    }

    @Synchronized fun completeUsbPermissionRequest(device: UsbDevice, token: Long, permissionGranted: Boolean) {
        usbConnection.completePermissionRequest(device, token, permissionGranted, ++nextGeneration)
        synchronizeUsbOutcome()
    }

    @Synchronized fun cancelUsbPermissionRequest(token: Long) = usbConnection.cancelPendingPermission(token)

    @Synchronized fun synchronizeUsbAttachments(): UsbPermissionRequest? = synchronizeUsbAttachments(usbConnection.attachedDevicesSnapshot())
    @Synchronized internal fun synchronizeUsbAttachments(snapshot: Result<List<UsbDevice>>): UsbPermissionRequest? {
        if (snapshot.isFailure) return null
        val request = usbConnection.synchronizeAttachments(++nextGeneration, snapshot)
        synchronizeUsbOutcome()
        return request
    }

    /** Reconcile physical attachment changes without starting a newly discovered device. */
    @Synchronized fun reconcileUsbAttachments() = reconcileUsbAttachments(usbConnection.attachedDevicesSnapshot())
    @Synchronized internal fun reconcileUsbAttachments(snapshot: Result<List<UsbDevice>>) {
        usbConnection.reconcileAttachments(snapshot)
        synchronizeUsbOutcome()
    }

    @Synchronized fun onUsbDeviceDetached(deviceId: Int): Boolean {
        val detached = usbConnection.onDeviceDetached(deviceId)
        if (detached) handleConnectionEvent(usbConnection, usbConnection.latestEvent)
        return detached
    }

    private fun synchronizeUsbOutcome() {
        if (usbConnection.isRunning && (currentConnection !== usbConnection || currentConnectionGeneration != usbConnection.generation)) {
            btConnection.stopConnection()
            selectConnection(usbConnection, usbConnection.generation)
            handleConnectionEvent(usbConnection, usbConnection.latestEvent)
        } else if (currentConnection === usbConnection && usbConnection.latestEvent.end == ConnectionEvent.End.STOPPED) {
            handleConnectionEvent(usbConnection, usbConnection.latestEvent)
        }
    }

    suspend fun startBtConnection() {
        val generation = synchronized(this) {
            if (usbConnection.activeDeviceId != null) return
            usbConnection.stopConnection()
            (++nextGeneration).also { selectConnection(btConnection, it) }
        }
        btConnection.startConnection(generation)
    }

    private fun selectConnection(connection: Connection<*>, generation: Long) {
        if (currentConnection != null) secuLogger.stopLogging()
        currentConnection = connection
        currentConnectionGeneration = generation
        currentSessionGeneration = generation
        lastEventSequence = -1
        _firmwareInfo.value = null
        publishConnectionState(Connecting)
        reconcileConnectionService()
    }

    @Synchronized fun stopConnection(): Boolean {
        if (currentConnection?.allowsManualDisconnect == false) return false
        return stopConnectionInternal(stopService = true)
    }

    /** UI reports visibility; the manager applies the shared USB/Bluetooth background policy. */
    @Synchronized fun onAppForegrounded() {
        appVisible = true
        reconcileConnectionService()
    }

    @Synchronized fun onConnectionStateObservedInForeground(state: ConnectionState) {
        if (state == Connected) reconcileConnectionService()
    }

    /** Stop active work only when the app truly leaves the foreground and background work is disabled. */
    @Synchronized fun onAppBackgrounded() {
        appVisible = false
        usbConnection.cancelPendingPermission()
        if (currentConnection == null) {
            nextGeneration++
            return
        }
        if (backgroundWorkEnabled.value != false) return
        stopConnectionInternal(stopService = true)
    }

    /** Preference changes apply immediately to service ownership without ending a live session. */
    suspend fun onBackgroundWorkPreferenceChanged(enabled: Boolean) {
        backgroundPreferenceMutex.withLock {
            mPrefs.setWakeLockEnabled(enabled)
            val persistedValue = mPrefs.settings.first().isWakeLockEnabled
            backgroundWorkEnabled.first { it == persistedValue }
        }
    }

    /** Re-posts the active notification after the user grants Android's notification permission. */
    @Synchronized fun refreshConnectionNotification() {
        if (currentConnection != null) connectionServiceController.refreshNotification()
    }

    @Synchronized private fun reconcileConnectionService() {
        val policy = backgroundWorkEnabled.value ?: return
        if (!policy) {
            connectionServiceController.stop()
            return
        }
        if (appVisible && currentConnection != null) {
            connectionServiceController.ensureStarted(currentSessionGeneration)
        }
    }

    /** A delayed destroy from an older service instance must not stop a replacement connection. */
    @Synchronized fun onConnectionServiceDestroyed(ownerToken: Long) {
        val serviceConnectionGeneration = connectionServiceController.completeDestruction(ownerToken) ?: return
        if (currentSessionGeneration != serviceConnectionGeneration) return
        if (appVisible) {
            reconcileConnectionService()
        } else {
            stopConnectionInternal(stopService = false)
        }
    }

    @Synchronized private fun stopConnectionInternal(stopService: Boolean): Boolean {
        currentConnection = null
        currentConnectionGeneration = ++nextGeneration
        _firmwareInfo.value = null
        usbConnection.stopConnection()
        btConnection.stopConnection()
        secuLogger.stopLogging()
        publishConnectionState(Disconnected)
        if (stopService) connectionServiceController.stop()
        return true
    }

    fun sendNewTask(task: Task) = sendOutPacket(task.getPacket())
    fun sendOutPacket(packet: OutputPacket) {
        currentConnection?.takeIf { it.isReady }?.sendData(packet)
    }

    @Synchronized private fun handleConnectionEvent(connection: Connection<*>, event: ConnectionEvent) {
        if (connection !== currentConnection || currentConnectionGeneration != event.generation || event.sequence <= lastEventSequence) return
        lastEventSequence = event.sequence
        _firmwareInfo.value = event.firmware
        if (event.end != null) {
            secuLogger.stopLogging()
            if (event.end == ConnectionEvent.End.STOPPED) {
                currentConnection = null
                currentConnectionGeneration = ++nextGeneration
                connectionServiceController.stop()
            } else {
                currentSessionGeneration = ++nextGeneration
                connectionServiceController.updateConnectionGeneration(currentSessionGeneration)
            }
            if (connection !== btConnection || event.end == ConnectionEvent.End.STOPPED) {
                publishConnectionState(Disconnected)
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
                synchronized(this@Secu3ConnectionManager) { backgroundWorkEnabled.value = null }
                Log.e(javaClass.simpleName, "Could not read background connection preference", failure)
            }.collect { settings ->
                synchronized(this@Secu3ConnectionManager) {
                    backgroundWorkEnabled.value = settings.isWakeLockEnabled
                    applyBackgroundWorkPolicy(settings.isWakeLockEnabled)
                }
            }
        }
        scope.launch(start = CoroutineStart.UNDISPATCHED) { usbConnection.sessionEvents.collect { handleConnectionEvent(usbConnection, it) } }
        scope.launch(start = CoroutineStart.UNDISPATCHED) { btConnection.sessionEvents.collect { handleConnectionEvent(btConnection, it) } }
        scope.launch { receivedPacketFlow.filterIsInstance<SensorsPacket>().collect { secuLogger.logPacket(it) } }
    }

    private fun applyBackgroundWorkPolicy(enabled: Boolean) {
        if (!enabled && !appVisible && currentConnection != null) {
            stopConnectionInternal(stopService = true)
        } else {
            reconcileConnectionService()
        }
    }
}
