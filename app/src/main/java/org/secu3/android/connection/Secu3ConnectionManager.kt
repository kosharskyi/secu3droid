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
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext
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
    private val bluetoothManager: BluetoothManager,
    private val secuLogger: SecuLogger,
) {
    private enum class Transport { NONE, USB, BLUETOOTH }
    private data class Selection(val transport: Transport, val generation: Long)
    private val scope = CoroutineScope(Dispatchers.Default + Job())
    private val selectedTransport = MutableStateFlow(Selection(Transport.NONE, 0))
    private val usbSelectionReady = mutableMapOf<Long, CompletableDeferred<Unit>>()
    private var usbOpenJob: Job? = null
    private val _connectionState = MutableSharedFlow<ConnectionState>(replay = 1, extraBufferCapacity = 32).also { it.tryEmit(Disconnected) }
    @Volatile var currentConnectionState: ConnectionState = Disconnected
        private set
    val connectionStateFlow: SharedFlow<ConnectionState> = _connectionState.asSharedFlow()
    private val _usbSessionLocked = MutableStateFlow(false)
    val usbSessionLocked: StateFlow<Boolean> = _usbSessionLocked.asStateFlow()
    private val usbSessionFence = UsbSessionFence()
    @Volatile private var usbAttemptFailed = false
    @Volatile var fwInfo: FirmwareInfoPacket? = null
        private set

    val receivedPacketFlow: Flow<InputPacket> = selectedTransport.flatMapLatest { selection ->
        val transport = selection.transport
        val streamGeneration = selection.generation
        when (transport) {
            Transport.USB -> usbConnection.receivedPacketFlow.onSubscription {
                synchronized(usbSelectionReady) { usbSelectionReady[streamGeneration]?.complete(Unit) }
            }
            Transport.BLUETOOTH -> btConnection.receivedPacketFlow
            Transport.NONE -> emptyFlow()
        }.filter { selectedTransport.value == selection && usbSessionFence.generation() == streamGeneration }
            .map { raw -> withContext(Dispatchers.IO) { raw.parse(fwInfo) } }
            .filterNotNull()
            .onEach { packet ->
                if (transport == Transport.USB && packet is FirmwareInfoPacket && selectedTransport.value == selection && usbSessionFence.generation() == streamGeneration && !usbAttemptFailed) {
                    confirmUsbFirmware(selection, packet)
                } else if (transport == Transport.BLUETOOTH && packet is FirmwareInfoPacket && selectedTransport.value == selection && usbSessionFence.generation() == streamGeneration) {
                    fwInfo = packet
                }
            }
    }.shareIn(scope, SharingStarted.WhileSubscribed(5_000), replay = 0)

    /** Subscribe to the active transport before sending the page request. */
    fun receivedPacketFlow(onSubscribed: () -> Unit): Flow<InputPacket> = flow {
        val selection = selectedTransport.value
        val transport = selection.transport
        val generationAtSubscribe = selection.generation
        val raw = when (transport) {
            Transport.USB -> usbConnection.receivedPacketFlow
            Transport.BLUETOOTH -> btConnection.receivedPacketFlow
            Transport.NONE -> awaitCancellation()
        }
        emitAll(raw.onSubscription { onSubscribed() }
            .filter { selectedTransport.value == selection && usbSessionFence.generation() == generationAtSubscribe }
            .map { item -> withContext(Dispatchers.IO) { item.parse(fwInfo) } }
            .filterNotNull()
            .onEach { packet ->
                if (transport == Transport.USB && packet is FirmwareInfoPacket && selectedTransport.value == selection && usbSessionFence.generation() == generationAtSubscribe && !usbAttemptFailed) {
                    confirmUsbFirmware(selection, packet)
                }
            })
    }

    val firmwareLiveData: LiveData<FirmwareInfoPacket> = receivedPacketFlow.filterIsInstance<FirmwareInfoPacket>().asLiveData()
    val isConnectionRunning get() = isUsbRunning || isBtRunning
    val isBtRunning get() = btConnection.isRunning
    val isUsbRunning get() = usbConnection.isRunning
    val isConnected get() = isUsbConnected || (selectedTransport.value.transport == Transport.BLUETOOTH && btConnection.isConnected)
    val isUsbConnected get() = selectedTransport.value.let { selection -> selection.transport == Transport.USB && activeUsbDeviceIdOrNull?.let { usbSessionFence.isConfirmed(it, selection.generation) } == true && usbConnection.isConnected }
    val isBtConnected get() = selectedTransport.value.transport == Transport.BLUETOOTH && btConnection.isConnected
    val activeUsbDeviceIdOrNull: Int? get() = usbSessionFence.activeDeviceId().takeIf { selectedTransport.value.transport == Transport.USB }
    val activeUsbSessionGeneration: Long? get() = selectedTransport.value.takeIf { it.transport == Transport.USB }?.generation

    @Synchronized fun startUsbConnection(device: UsbDevice) {
        cancelUsbOpenAttempt()
        val attemptGeneration = usbSessionFence.begin(device.deviceId)
        selectedTransport.value = Selection(Transport.NONE, attemptGeneration)
        _usbSessionLocked.value = false
        usbAttemptFailed = false
        fwInfo = null
        publishConnectionState(InProgress)
        btConnection.stopConnection()
        usbConnection.stopConnection()
        val selection = Selection(Transport.USB, attemptGeneration)
        synchronized(usbSelectionReady) { usbSelectionReady[attemptGeneration] = CompletableDeferred() }
        selectedTransport.value = selection
        usbOpenJob = scope.launch {
            val ready = synchronized(usbSelectionReady) { usbSelectionReady[attemptGeneration] } ?: return@launch
            ready.await()
            synchronized(usbSelectionReady) { usbSelectionReady.remove(attemptGeneration) }
            if (!usbSessionFence.isCurrent(device.deviceId, attemptGeneration) || selectedTransport.value != selection) return@launch
            usbConnection.startConnection(device)
            delay(5_000)
            synchronized(this@Secu3ConnectionManager) {
                if (usbSessionFence.isCurrent(device.deviceId, attemptGeneration) && selectedTransport.value == selection && !_usbSessionLocked.value && !usbAttemptFailed) {
                    usbConnection.stopConnection()
                    failUsbSession(ConnectionFailed(IllegalStateException("No SECU firmware response received")))
                }
            }
            if (usbOpenJob === coroutineContext[Job]) usbOpenJob = null
        }
    }

    @Synchronized fun onUsbDeviceDetached(deviceId: Int): Boolean {
        if (selectedTransport.value.transport != Transport.USB || !usbSessionFence.detach(deviceId)) return false
        cancelUsbOpenAttempt()
        val nextGeneration = usbSessionFence.generation()
        selectedTransport.value = Selection(Transport.NONE, nextGeneration)
        _usbSessionLocked.value = false
        usbAttemptFailed = false
        fwInfo = null
        usbConnection.stopConnection()
        secuLogger.stopLogging()
        publishConnectionState(Disconnected)
        return true
    }

    @Synchronized fun startBtConnection() {
        if (BluetoothDeviceAddress.normalize(mPrefs.bluetoothDeviceAddress) == null || runCatching { !bluetoothManager.adapter.isEnabled }.getOrDefault(true)) return
        if (selectedTransport.value.transport == Transport.USB && (usbConnection.isRunning || _usbSessionLocked.value)) return
        cancelUsbOpenAttempt()
        val nextGeneration = usbSessionFence.advance()
        selectedTransport.value = Selection(Transport.BLUETOOTH, nextGeneration)
        _usbSessionLocked.value = false
        usbAttemptFailed = false
        fwInfo = null
        publishConnectionState(InProgress)
        usbConnection.stopConnection()
        btConnection.stopConnection()
        btConnection.startConnection()
    }

    @Synchronized fun stopConnection(): Boolean {
        if (_usbSessionLocked.value || activeUsbDeviceIdOrNull?.let { usbSessionFence.isConfirmed(it, selectedTransport.value.generation) } == true) return false
        cancelUsbOpenAttempt()
        val nextGeneration = usbSessionFence.advance()
        selectedTransport.value = Selection(Transport.NONE, nextGeneration)
        fwInfo = null
        _usbSessionLocked.value = false
        usbConnection.stopConnection()
        btConnection.stopConnection()
        publishConnectionState(Disconnected)
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

    private fun cancelUsbOpenAttempt() {
        usbOpenJob?.cancel()
        usbOpenJob = null
        synchronized(usbSelectionReady) { usbSelectionReady.clear() }
    }

    @Synchronized private fun confirmUsbFirmware(selection: Selection, packet: FirmwareInfoPacket) {
        if (selectedTransport.value != selection || usbAttemptFailed || !usbConnection.isConnected) return
        val deviceId = activeUsbDeviceIdOrNull ?: return
        if (!usbSessionFence.confirm(deviceId, selection.generation)) return
        fwInfo = packet
        _usbSessionLocked.value = true
        publishConnectionState(Connected)
    }

    @Synchronized private fun failUsbSession(state: ConnectionFailed) {
        if (selectedTransport.value.transport != Transport.USB || usbAttemptFailed) return
        cancelUsbOpenAttempt()
        activeUsbDeviceIdOrNull?.let { usbSessionFence.invalidateConfirmation(it, selectedTransport.value.generation) }
        _usbSessionLocked.value = false
        fwInfo = null
        usbAttemptFailed = true
        publishConnectionState(state)
        secuLogger.stopLogging()
    }

    @Synchronized private fun publishConnectionState(state: ConnectionState) {
        if (currentConnectionState == state) return
        currentConnectionState = state
        _connectionState.tryEmit(state)
    }

    init {
        scope.launch {
            merge(
                usbConnection.connectionStateFlow.map { Transport.USB to it },
                btConnection.connectionStateFlow.map { Transport.BLUETOOTH to it },
            ).collect { (transport, state) ->
                if (transport != selectedTransport.value.transport) return@collect
                when {
                    transport == Transport.USB && state is ConnectionFailed -> {
                        failUsbSession(state)
                    }
                    transport == Transport.BLUETOOTH -> {
                        if (state == Disconnected && btConnection.isRunning) return@collect
                        publishConnectionState(state)
                    }
                }
            }
        }
        scope.launch {
            receivedPacketFlow.filterIsInstance<SensorsPacket>().collect { secuLogger.logPacket(it) }
        }
    }
}
