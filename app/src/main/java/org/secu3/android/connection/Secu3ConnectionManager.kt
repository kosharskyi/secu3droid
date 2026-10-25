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

import android.bluetooth.BluetoothManager
import android.hardware.usb.UsbDevice
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.coroutineContext
import org.secu3.android.models.RawPacket
import org.secu3.android.models.packets.base.InputPacket
import org.secu3.android.models.packets.base.OutputPacket
import org.secu3.android.models.packets.input.FirmwareInfoPacket
import org.secu3.android.models.packets.input.SensorsPacket
import org.secu3.android.utils.SecuLogger
import org.secu3.android.utils.Task
import org.secu3.android.utils.UserPrefs
import javax.inject.Inject
import javax.inject.Singleton

data class UsbPermissionRequest(val device: UsbDevice, val token: Long)

internal data class UsbAttachmentIdentity(
    val deviceId: Int,
    val deviceName: String,
    val vendorId: Int,
    val productId: Int,
)

@Singleton
class Secu3ConnectionManager @Inject constructor(
    private val usbConnection: UsbConnection,
    private val btConnection: BtConnection,
    private val mPrefs: UserPrefs,
    private val bluetoothManager: BluetoothManager,
    private val secuLogger: SecuLogger,
    private val connectionServiceController: ConnectionServiceController,
) {
    private enum class Transport { NONE, USB, BLUETOOTH }
    private data class Selection(val transport: Transport, val generation: Long)
    private enum class UsbAttemptSignal { FIRMWARE_CONFIRMED, FAILED }
    private val scope = CoroutineScope(Dispatchers.Default + Job())
    private val selectedTransport = MutableStateFlow(Selection(Transport.NONE, 0))
    private var usbSelectionReady: CompletableDeferred<Unit>? = null
    private var usbRetryJob: Job? = null
    private var nextUsbAttemptId = 0L
    @Volatile private var activeUsbAttemptId: Long? = null
    @Volatile private var confirmedUsbAttemptId: Long? = null
    private var usbAttemptSignals: Channel<UsbAttemptSignal>? = null
    private val _connectionState = MutableSharedFlow<ConnectionState>(replay = 1, extraBufferCapacity = 32).also { it.tryEmit(Disconnected) }
    @Volatile var currentConnectionState: ConnectionState = Disconnected
        private set
    val connectionStateFlow: SharedFlow<ConnectionState> = _connectionState.asSharedFlow()
    private val _terminalSessionGeneration = MutableStateFlow<Long?>(null)
    val terminalSessionGeneration: StateFlow<Long?> = _terminalSessionGeneration.asStateFlow()
    private val usbSessionFence = UsbSessionFence()
    private val promptedUsbAttachments = mutableSetOf<UsbAttachmentIdentity>()
    private var nextUsbPermissionToken = 0L
    private var pendingUsbPermission: UsbPermissionRequest? = null
    private var activeUsbAttachment: UsbAttachmentIdentity? = null
    private var appVisible = false
    @Volatile var fwInfo: FirmwareInfoPacket? = null
        private set

    val receivedPacketFlow: Flow<InputPacket> = selectedTransport.flatMapLatest { selection ->
        packetsForSelection(selection) {
            if (selection.transport == Transport.USB) {
                synchronized(this@Secu3ConnectionManager) {
                    usbSelectionReady?.takeIf { selectedTransport.value == selection }?.complete(Unit)
                }
            }
        }.onEach { packet ->
            if (selection.transport == Transport.BLUETOOTH && packet is FirmwareInfoPacket &&
                selectedTransport.value == selection && usbSessionFence.generation() == selection.generation
            ) {
                fwInfo = packet
            }
        }
    }.shareIn(scope, SharingStarted.WhileSubscribed(5_000), replay = 0)

    /** Subscribe to the active transport before sending the page request. */
    fun receivedPacketFlow(onSubscribed: () -> Unit): Flow<InputPacket> = flow {
        val selection = selectedTransport.value
        if (selection.transport == Transport.NONE) awaitCancellation()
        emitAll(packetsForSelection(selection, onSubscribed))
    }

    private fun packetsForSelection(selection: Selection, onSubscribed: () -> Unit): Flow<InputPacket> {
        val transport = selection.transport
        val raw: Flow<Pair<Long?, RawPacket>> = when (transport) {
            Transport.USB -> usbConnection.attemptPackets.onSubscription { onSubscribed() }
                .map { it.attemptId to it.packet }
            Transport.BLUETOOTH -> btConnection.receivedPacketFlow.onSubscription { onSubscribed() }.map { null to it }
            Transport.NONE -> emptyFlow()
        }
        return raw.filter { selectedTransport.value == selection && usbSessionFence.generation() == selection.generation }
            .mapNotNull { (attemptId, item) ->
                val packet = withContext(Dispatchers.IO) { item.parse(fwInfo) } ?: return@mapNotNull null
                attemptId to packet
            }
            .onEach { (attemptId, packet) ->
                if (transport == Transport.USB && packet is FirmwareInfoPacket && selectedTransport.value == selection && usbSessionFence.generation() == selection.generation) {
                    attemptId?.let { confirmUsbFirmware(selection, it, packet) }
                }
            }
            .filter { (attemptId, _) -> transport != Transport.USB || attemptId == activeUsbAttemptId }
            .map { it.second }
    }

    val firmwareLiveData: LiveData<FirmwareInfoPacket> = receivedPacketFlow.filterIsInstance<FirmwareInfoPacket>().asLiveData()
    val isConnectionRunning get() = isUsbRunning || isBtRunning
    val isBtRunning get() = btConnection.isRunning
    val isUsbRunning get() = usbConnection.isRunning
    val isConnected get() = isUsbConnected || (selectedTransport.value.transport == Transport.BLUETOOTH && btConnection.isConnected)
    val isUsbSessionLocked: Boolean
        get() {
            val confirmedAttemptId = confirmedUsbAttemptId ?: return false
            val selection = selectedTransport.value
            val deviceId = activeUsbDeviceIdOrNull ?: return false
            return selection.transport == Transport.USB && confirmedAttemptId == activeUsbAttemptId &&
                usbSessionFence.isCurrent(deviceId, selection.generation)
        }
    val isUsbConnected get() = isUsbSessionLocked && usbConnection.isConnected
    val isBtConnected get() = selectedTransport.value.transport == Transport.BLUETOOTH && btConnection.isConnected
    val activeUsbDeviceIdOrNull: Int? get() = usbSessionFence.activeDeviceId().takeIf { selectedTransport.value.transport == Transport.USB }
    val activeUsbSessionGeneration: Long? get() = selectedTransport.value.takeIf { it.transport == Transport.USB }?.generation
    val activeUsbHomeGeneration: Long? get() = confirmedUsbAttemptId.takeIf {
        it != null && isUsbSessionLocked
    }

    /** Admit or restart USB retries for the currently attached supported ECU. */
    @Synchronized fun requestUsbConnection(device: UsbDevice): UsbPermissionRequest? {
        if (!appVisible) return null
        val attachedDevice = usbConnection.attachedDevice(device) ?: return null
        return requestAttachedUsbConnection(attachedDevice)
    }

    private fun requestAttachedUsbConnection(attachedDevice: UsbDevice): UsbPermissionRequest? {
        val supported = usbConnection.supportsSerialConnection(attachedDevice)
        val activeId = activeUsbDeviceIdOrNull
        val attachment = attachmentIdentity(attachedDevice)
        if (activeUsbAttachment == attachment && selectedTransport.value.transport == Transport.USB &&
            (usbRetryJob?.isActive == true || usbConnection.isRunning)
        ) return null
        val pendingPermission = pendingUsbPermission
        if (pendingPermission?.device?.deviceId == attachedDevice.deviceId &&
            !samePhysicalDevice(pendingPermission.device, attachedDevice)
        ) {
            this.pendingUsbPermission = null
        }
        if (!supported || (activeId != null && activeId != attachedDevice.deviceId)) return null
        return requestPermissionOrStart(
            attachedDevice,
            attachment,
            usbConnection.hasPermission(attachedDevice),
        )
    }

    private fun requestPermissionOrStart(
        device: UsbDevice,
        attachment: UsbAttachmentIdentity,
        hasPermission: Boolean,
    ): UsbPermissionRequest? {
        if (pendingUsbPermission != null) return null
        if (hasPermission) {
            startUsbConnectionInternal(device)
            return null
        }
        if (!promptedUsbAttachments.add(attachment)) return null
        val request = UsbPermissionRequest(device, ++nextUsbPermissionToken)
        pendingUsbPermission = request
        return request
    }

    /** Completes the pending Android permission request after fresh platform checks. */
    @Synchronized fun completeUsbPermissionRequest(
        device: UsbDevice,
        token: Long,
        permissionGranted: Boolean,
    ) {
        if (pendingUsbPermission?.let { it.token == token && samePhysicalDevice(it.device, device) } != true) return
        val attachedDevice = usbConnection.attachedDevice(device)
        completePendingUsbPermission(
            device = device,
            token = token,
            permissionGranted = permissionGranted,
            attachedDevice = attachedDevice,
            hasPermission = attachedDevice?.let(usbConnection::hasPermission) == true,
            supported = attachedDevice?.let(usbConnection::supportsSerialConnection) == true,
            conflictsWithActiveDevice = attachedDevice?.deviceId.let { deviceId ->
                activeUsbDeviceIdOrNull?.let { activeId -> activeId != deviceId } == true
            },
        )
    }

    private fun completePendingUsbPermission(
        device: UsbDevice,
        token: Long,
        permissionGranted: Boolean,
        attachedDevice: UsbDevice?,
        hasPermission: Boolean,
        supported: Boolean,
        conflictsWithActiveDevice: Boolean,
    ) {
        if (pendingUsbPermission?.let { it.token == token && samePhysicalDevice(it.device, device) } != true) return
        pendingUsbPermission = null
        if (!permissionGranted || !hasPermission || !supported || conflictsWithActiveDevice ||
            attachedDevice == null || !appVisible
        ) return
        startUsbConnectionInternal(attachedDevice)
    }

    /** Synchronizes current USB attachments and admits the first eligible device. */
    @Synchronized fun synchronizeUsbAttachments(): UsbPermissionRequest? =
        synchronizeUsbAttachments(usbConnection.attachedDevicesSnapshot())

    /** Applies one device-list snapshot; a failed result must not be interpreted as no attachments. */
    @Synchronized internal fun synchronizeUsbAttachments(
        snapshot: Result<List<UsbDevice>>,
    ): UsbPermissionRequest? {
        val attachedDevices = snapshot.getOrElse { return null }
        val attachedIdentities = attachedDevices.map(::attachmentIdentity).toSet()
        val missedDetach = isActiveUsbAttachmentMissing(attachedIdentities)
        if (missedDetach) activeUsbDeviceIdOrNull?.let(::onUsbDeviceDetached)

        promptedUsbAttachments.retainAll(attachedIdentities)
        if (pendingUsbPermission?.let { attachmentIdentity(it.device) !in attachedIdentities } == true) {
            pendingUsbPermission = null
        }
        val firstEligible = attachedDevices.firstOrNull { candidate ->
            usbConnection.supportsSerialConnection(candidate) &&
                (usbConnection.hasPermission(candidate) ||
                    (attachmentIdentity(candidate) !in promptedUsbAttachments && pendingUsbPermission == null))
        }
        return firstEligible?.let(::requestUsbConnection)
    }

    private fun isActiveUsbAttachmentMissing(
        attachedIdentities: Set<UsbAttachmentIdentity>,
    ): Boolean {
        val activeDeviceId = activeUsbDeviceIdOrNull ?: return false
        val activeIdentity = activeUsbAttachment
        return if (activeIdentity != null) {
            activeIdentity !in attachedIdentities
        } else {
            // Legacy and isolated test sessions may have only a fenced device ID.
            attachedIdentities.none { it.deviceId == activeDeviceId }
        }
    }

    @Synchronized private fun startUsbConnectionInternal(device: UsbDevice) {
        confirmedUsbAttemptId = null
        _terminalSessionGeneration.value = null
        cancelUsbRetrySession()
        activeUsbAttachment = attachmentIdentity(device)
        val attemptGeneration = usbSessionFence.begin(device.deviceId)
        selectedTransport.value = Selection(Transport.NONE, attemptGeneration)
        fwInfo = null
        publishConnectionState(InProgress)
        btConnection.stopConnection()
        usbConnection.stopConnection()
        val selection = Selection(Transport.USB, attemptGeneration)
        val ready = CompletableDeferred<Unit>()
        usbSelectionReady = ready
        selectedTransport.value = selection
        reconcileConnectionService()
        usbRetryJob = scope.launch {
            ready.await()
            synchronized(this@Secu3ConnectionManager) {
                if (usbSelectionReady === ready) usbSelectionReady = null
            }
            while (isCurrentUsbSelection(device.deviceId, selection)) {
                val attemptId: Long
                val signals: Channel<UsbAttemptSignal>
                synchronized(this@Secu3ConnectionManager) {
                    if (!isCurrentUsbSelection(device.deviceId, selection)) return@launch
                    attemptId = ++nextUsbAttemptId
                    signals = Channel(Channel.UNLIMITED)
                    activeUsbAttemptId = attemptId
                    usbAttemptSignals = signals
                    confirmedUsbAttemptId = null
                }
                usbConnection.startConnection(device, attemptId)

                var firmwareConfirmed = false
                var retry = false
                while (isCurrentUsbAttempt(device.deviceId, selection, attemptId)) {
                    val signal = if (firmwareConfirmed) {
                        signals.receive()
                    } else {
                        withTimeoutOrNull(USB_FIRMWARE_HANDSHAKE_TIMEOUT_MS) { signals.receive() }
                            ?: UsbAttemptSignal.FAILED
                    }
                    when (signal) {
                        UsbAttemptSignal.FIRMWARE_CONFIRMED -> firmwareConfirmed = true
                        UsbAttemptSignal.FAILED -> {
                            retry = true
                            break
                        }
                    }
                }

                synchronized(this@Secu3ConnectionManager) {
                    if (!isCurrentUsbSelection(device.deviceId, selection)) return@launch
                    if (retry) {
                        confirmedUsbAttemptId = null
                        fwInfo = null
                        publishConnectionState(InProgress)
                        usbConnection.stopConnection()
                    }
                    if (usbAttemptSignals === signals) {
                        usbAttemptSignals = null
                        activeUsbAttemptId = null
                    }
                }
                signals.close()
                if (!retry) return@launch
                delay(USB_RETRY_DELAY_MS)
            }
            synchronized(this@Secu3ConnectionManager) {
                if (usbRetryJob === coroutineContext[Job]) usbRetryJob = null
            }
        }
    }

    @Synchronized fun onUsbDeviceDetached(deviceId: Int): Boolean {
        promptedUsbAttachments.removeAll { it.deviceId == deviceId }
        if (pendingUsbPermission?.device?.deviceId == deviceId) pendingUsbPermission = null
        val endedSelection = selectedTransport.value
        if (selectedTransport.value.transport != Transport.USB || !usbSessionFence.detach(deviceId)) return false
        cancelUsbRetrySession()
        activeUsbAttachment = null
        confirmedUsbAttemptId = null
        val nextGeneration = usbSessionFence.generation()
        selectedTransport.value = Selection(Transport.NONE, nextGeneration)
        fwInfo = null
        usbConnection.stopConnection()
        secuLogger.stopLogging()
        connectionServiceController.stop()
        publishConnectionState(Disconnected)
        _terminalSessionGeneration.value = endedSelection.generation
        return true
    }

    @Synchronized fun startBtConnection() {
        if (BluetoothDeviceAddress.normalize(mPrefs.bluetoothDeviceAddress) == null || runCatching { !bluetoothManager.adapter.isEnabled }.getOrDefault(true)) return
        if (selectedTransport.value.transport == Transport.USB && activeUsbAttachment != null) return
        confirmedUsbAttemptId = null
        _terminalSessionGeneration.value = null
        cancelUsbRetrySession()
        val nextGeneration = usbSessionFence.advance()
        selectedTransport.value = Selection(Transport.BLUETOOTH, nextGeneration)
        activeUsbAttachment = null
        fwInfo = null
        publishConnectionState(InProgress)
        usbConnection.stopConnection()
        btConnection.stopConnection()
        btConnection.startConnection(nextGeneration)
        reconcileConnectionService()
    }

    @Synchronized fun stopConnection(): Boolean {
        if (isUsbSessionLocked) return false
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
        pendingUsbPermission = null
        if (mPrefs.isWakeLockEnabled || selectedTransport.value.transport == Transport.NONE) return
        stopConnectionInternal(
            stopService = true,
            publishTerminal = true,
        )
    }

    /** Preference changes apply immediately to service ownership without ending a live session. */
    @Synchronized fun onBackgroundWorkPreferenceChanged(enabled: Boolean) {
        mPrefs.isWakeLockEnabled = enabled
        reconcileConnectionService()
    }

    /** Re-posts the active notification after the user grants Android's notification permission. */
    @Synchronized fun refreshConnectionNotification() {
        if (isConnectionSessionActive()) connectionServiceController.refreshNotification()
    }

    @Synchronized private fun reconcileConnectionService() {
        if (!mPrefs.isWakeLockEnabled) {
            connectionServiceController.stop()
            return
        }
        val selection = selectedTransport.value
        if (appVisible && selection.transport != Transport.NONE && isConnectionSessionActive()) {
            connectionServiceController.ensureStarted(selection.generation)
        }
    }

    @Synchronized private fun isConnectionSessionActive(): Boolean =
        selectedTransport.value.transport != Transport.NONE

    @Synchronized private fun isCurrentUsbSelection(deviceId: Int, selection: Selection): Boolean =
        selectedTransport.value == selection && usbSessionFence.isCurrent(deviceId, selection.generation)

    @Synchronized private fun isCurrentUsbAttempt(deviceId: Int, selection: Selection, attemptId: Long): Boolean =
        isCurrentUsbSelection(deviceId, selection) && activeUsbAttemptId == attemptId

    /** A delayed destroy from an older service instance must not stop a replacement connection. */
    @Synchronized fun onConnectionServiceDestroyed(ownerToken: Long) {
        val serviceConnectionGeneration = connectionServiceController.completeDestruction(ownerToken) ?: return
        if (selectedTransport.value.generation != serviceConnectionGeneration) return
        if (appVisible) {
            reconcileConnectionService()
        } else {
            stopConnectionInternal(
                stopService = false,
                publishTerminal = true,
            )
        }
    }

    @Synchronized private fun stopConnectionInternal(
        stopService: Boolean,
        publishTerminal: Boolean = false,
    ): Boolean {
        val endingSelection = selectedTransport.value
        _terminalSessionGeneration.value = null
        pendingUsbPermission = null
        confirmedUsbAttemptId = null
        cancelUsbRetrySession()
        val nextGeneration = usbSessionFence.advance()
        selectedTransport.value = Selection(Transport.NONE, nextGeneration)
        activeUsbAttachment = null
        fwInfo = null
        usbConnection.stopConnection()
        btConnection.stopConnection()
        secuLogger.stopLogging()
        publishConnectionState(Disconnected)
        if (stopService) connectionServiceController.stop()
        if (publishTerminal && endingSelection.transport != Transport.NONE) {
            _terminalSessionGeneration.value = endingSelection.generation
        }
        return true
    }

    fun sendNewTask(task: Task) = sendOutPacket(task.getPacket())
    fun sendOutPacket(packet: OutputPacket) {
        when (selectedTransport.value.transport) {
            Transport.USB -> if (isUsbConnected) usbConnection.sendData(packet)
            Transport.BLUETOOTH -> if (isBtConnected) btConnection.sendData(packet)
            Transport.NONE -> Unit
        }
    }

    private fun cancelUsbRetrySession() {
        usbRetryJob?.cancel()
        usbRetryJob = null
        activeUsbAttemptId = null
        usbAttemptSignals?.close()
        usbAttemptSignals = null
        synchronized(this) { usbSelectionReady = null }
    }

    private fun samePhysicalDevice(first: UsbDevice, second: UsbDevice): Boolean =
        first.deviceId == second.deviceId && first.deviceName == second.deviceName &&
            first.vendorId == second.vendorId && first.productId == second.productId

    private fun attachmentIdentity(device: UsbDevice) = UsbAttachmentIdentity(
        deviceId = device.deviceId,
        deviceName = device.deviceName,
        vendorId = device.vendorId,
        productId = device.productId,
    )

    @Synchronized private fun confirmUsbFirmware(selection: Selection, attemptId: Long, packet: FirmwareInfoPacket) {
        if (selectedTransport.value != selection || activeUsbAttemptId != attemptId || !usbConnection.isConnected) return
        val deviceId = activeUsbDeviceIdOrNull ?: return
        if (!usbSessionFence.isCurrent(deviceId, selection.generation)) return
        fwInfo = packet
        confirmedUsbAttemptId = attemptId
        usbAttemptSignals?.trySend(UsbAttemptSignal.FIRMWARE_CONFIRMED)
        publishConnectionState(Connected)
        reconcileConnectionService()
    }

    @Synchronized private fun handleUsbAttemptFailure(attemptId: Long) {
        if (selectedTransport.value.transport != Transport.USB || activeUsbAttemptId != attemptId) return
        usbAttemptSignals?.trySend(UsbAttemptSignal.FAILED)
    }

    @Synchronized private fun finishBluetoothSession(event: BluetoothConnectionEvent) {
        val selection = selectedTransport.value
        if (selection.transport != Transport.BLUETOOTH || selection.generation != event.generation || !event.retriesExhausted) return
        // Invalidate transport selection before stop cleanup can publish its asynchronous Disconnected event.
        selectedTransport.value = Selection(Transport.NONE, usbSessionFence.advance())
        fwInfo = null
        publishConnectionState(event.state)
        btConnection.stopConnection()
        secuLogger.stopLogging()
        connectionServiceController.stop()
        _terminalSessionGeneration.value = event.generation
    }

    @Synchronized private fun publishConnectionState(state: ConnectionState) {
        if (currentConnectionState == state) return
        currentConnectionState = state
        _connectionState.tryEmit(state)
    }

    init {
        scope.launch {
            usbConnection.attemptFailures.collect(::handleUsbAttemptFailure)
        }
        scope.launch {
            btConnection.sessionEvents.collect { event ->
                synchronized(this@Secu3ConnectionManager) {
                    val selection = selectedTransport.value
                    if (selection.transport != Transport.BLUETOOTH || selection.generation != event.generation) return@synchronized
                    if (event.retriesExhausted) finishBluetoothSession(event)
                    else {
                        publishConnectionState(event.state)
                        reconcileConnectionService()
                    }
                }
            }
        }
        scope.launch {
            receivedPacketFlow.filterIsInstance<SensorsPacket>().collect { secuLogger.logPacket(it) }
        }
    }

    private companion object {
        const val USB_FIRMWARE_HANDSHAKE_TIMEOUT_MS = 5_000L
        const val USB_RETRY_DELAY_MS = 2_000L
    }
}
