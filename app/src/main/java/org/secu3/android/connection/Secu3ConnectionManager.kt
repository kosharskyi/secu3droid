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
    private enum class Transport { NONE, USB, BLUETOOTH }
    private data class Selection(val transport: Transport, val generation: Long)
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val selectedTransport = MutableStateFlow(Selection(Transport.NONE, 0))
    private var nextGeneration = 0L
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

    val receivedPacketFlow: Flow<InputPacket> = selectedTransport.flatMapLatest { selection ->
        connection(selection.transport)?.receivedPacketFlow
            ?.filter { selectedTransport.value == selection } ?: emptyFlow()
    }.shareIn(scope, SharingStarted.WhileSubscribed(5_000), replay = 0)

    fun receivedPacketFlow(onSubscribed: () -> Unit): Flow<InputPacket> = flow {
        val selection = selectedTransport.value
        val owner = connection(selection.transport) ?: return@flow awaitCancellation()
        emitAll(owner.receivedPacketFlow {
            if (selectedTransport.value == selection) onSubscribed()
        }.filter { selectedTransport.value == selection })
    }

    val isConnectionRunning get() = connection(selectedTransport.value.transport)?.isRunning == true
    val isConnected get() = connection(selectedTransport.value.transport)?.isReady == true
    val isUsbSessionLocked get() = selectedTransport.value.transport == Transport.USB && usbConnection.isSessionLocked
    val activeUsbDeviceIdOrNull get() = usbConnection.activeDeviceId

    @Synchronized fun requestUsbConnection(device: UsbDevice): UsbPermissionRequest? {
        if (!appVisible) return null
        val request = usbConnection.requestConnection(device, ++nextGeneration)
        synchronizeUsbOutcome()
        return request
    }

    @Synchronized fun completeUsbPermissionRequest(device: UsbDevice, token: Long, permissionGranted: Boolean) {
        if (!appVisible) { usbConnection.cancelPendingPermission(); return }
        usbConnection.completePermissionRequest(device, token, permissionGranted, ++nextGeneration)
        synchronizeUsbOutcome()
    }

    @Synchronized fun synchronizeUsbAttachments(): UsbPermissionRequest? = synchronizeUsbAttachments(usbConnection.attachedDevicesSnapshot())
    @Synchronized internal fun synchronizeUsbAttachments(snapshot: Result<List<UsbDevice>>): UsbPermissionRequest? {
        if (!appVisible || snapshot.isFailure) return null
        val request = usbConnection.synchronizeAttachments(++nextGeneration, snapshot)
        synchronizeUsbOutcome()
        return request
    }

    @Synchronized fun onUsbDeviceDetached(deviceId: Int): Boolean {
        val detached = usbConnection.onDeviceDetached(deviceId)
        if (detached) handleConnectionEvent(Transport.USB, usbConnection.latestEvent)
        return detached
    }

    private fun synchronizeUsbOutcome() {
        if (usbConnection.isRunning && (selectedTransport.value.transport != Transport.USB || selectedTransport.value.generation != usbConnection.generation)) {
            btConnection.stopConnection()
            selectConnection(Transport.USB, usbConnection.generation)
            handleConnectionEvent(Transport.USB, usbConnection.latestEvent)
        } else if (usbConnection.latestEvent.end == ConnectionEvent.End.STOPPED) {
            handleConnectionEvent(Transport.USB, usbConnection.latestEvent)
        }
    }

    suspend fun startBtConnection() {
        val generation = synchronized(this) {
            if (!appVisible || usbConnection.activeDeviceId != null) return
            usbConnection.stopConnection()
            (++nextGeneration).also { selectConnection(Transport.BLUETOOTH, it) }
        }
        btConnection.startConnection(generation)
    }

    private fun selectConnection(transport: Transport, generation: Long) {
        if (selectedTransport.value.transport != Transport.NONE) secuLogger.stopLogging()
        selectedTransport.value = Selection(transport, generation)
        currentSessionGeneration = generation
        lastEventSequence = -1
        _firmwareInfo.value = null
        publishConnectionState(InProgress)
        reconcileConnectionService()
    }

    @Synchronized fun stopConnection(): Boolean {
        if (connection(selectedTransport.value.transport)?.allowsManualDisconnect == false) return false
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
        if (selectedTransport.value.transport == Transport.NONE) {
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
        if (selectedTransport.value.transport != Transport.NONE) connectionServiceController.refreshNotification()
    }

    @Synchronized private fun reconcileConnectionService() {
        val policy = backgroundWorkEnabled.value ?: return
        if (!policy) {
            connectionServiceController.stop()
            return
        }
        val selection = selectedTransport.value
        if (appVisible && selection.transport != Transport.NONE) {
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
        selectedTransport.value = Selection(Transport.NONE, ++nextGeneration)
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
        connection(selectedTransport.value.transport)?.takeIf { it.isReady }?.sendData(packet)
    }

    private fun connection(transport: Transport): Connection<*>? = when (transport) {
        Transport.USB -> usbConnection
        Transport.BLUETOOTH -> btConnection
        Transport.NONE -> null
    }

    @Synchronized private fun handleConnectionEvent(transport: Transport, event: ConnectionEvent) {
        val selection = selectedTransport.value
        if (selection.transport != transport || selection.generation != event.generation || event.sequence <= lastEventSequence) return
        lastEventSequence = event.sequence
        _firmwareInfo.value = event.firmware
        if (event.end != null) {
            secuLogger.stopLogging()
            if (event.end == ConnectionEvent.End.STOPPED) {
                selectedTransport.value = Selection(Transport.NONE, ++nextGeneration)
                connectionServiceController.stop()
            } else {
                currentSessionGeneration = ++nextGeneration
                connectionServiceController.updateConnectionGeneration(currentSessionGeneration)
            }
            publishConnectionState(Disconnected)
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
        scope.launch(start = CoroutineStart.UNDISPATCHED) { usbConnection.sessionEvents.collect { handleConnectionEvent(Transport.USB, it) } }
        scope.launch(start = CoroutineStart.UNDISPATCHED) { btConnection.sessionEvents.collect { handleConnectionEvent(Transport.BLUETOOTH, it) } }
        scope.launch { receivedPacketFlow.filterIsInstance<SensorsPacket>().collect { secuLogger.logPacket(it) } }
    }

    private fun applyBackgroundWorkPolicy(enabled: Boolean) {
        if (!enabled && !appVisible && selectedTransport.value.transport != Transport.NONE) {
            stopConnectionInternal(stopService = true)
        } else {
            reconcileConnectionService()
        }
    }
}
