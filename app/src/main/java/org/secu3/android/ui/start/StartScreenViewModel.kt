package org.secu3.android.ui.start

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.pm.PackageManager
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.location.LocationManager
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import java.io.IOException
import java.util.UUID
import org.secu3.android.R
import org.secu3.android.ui.messages.UiMessageDuration
import org.secu3.android.ui.messages.UiMessageQueue
import org.secu3.android.connection.BluetoothDeviceAddress
import org.secu3.android.connection.BluetoothPermissionPolicy
import org.secu3.android.connection.ConnectionTimeout
import org.secu3.android.connection.UsbAwaitingEcu
import org.secu3.android.connection.UsbReconnecting
import org.secu3.android.connection.UsbReconnectingAwaitingEcu
import org.secu3.android.connection.Connected
import org.secu3.android.connection.Disconnected
import org.secu3.android.connection.Secu3ConnectionManager
import org.secu3.android.utils.prefs.UserPrefs
import javax.inject.Inject

internal data class UsbPermissionRequest(val device: UsbDevice, val token: String)

@HiltViewModel
internal open class StartScreenViewModel @Inject constructor(
    private val prefs: UserPrefs,
    private val bluetoothManager: BluetoothManager,
    private val locationManager: LocationManager,
    private val connectionManager: Secu3ConnectionManager,
    private val packageManager: PackageManager,
    val usbManager: UsbManager,
    val bluetoothPermissionPolicy: BluetoothPermissionPolicy,
    private val uiMessageQueue: UiMessageQueue,
) : ViewModel() {
    private val _uiState = MutableStateFlow(StartScreenState())
    val uiState = _uiState.asStateFlow()
    private val connectionState = connectionManager.connectionStateFlow
    private val bluetoothAdapter: BluetoothAdapter? get() = runCatching { bluetoothManager.adapter }.getOrNull()
    private var usbDevice: UsbDevice? = null
    private val devices = mutableListOf<BluetoothDevice>()
    var pendingBluetoothPermissions: Array<String> = emptyArray()
    private val bluetoothBondGate = BluetoothBondRequestGate()
    private var pendingBluetoothBondDevice: BluetoothDevice? = null
    private var connectionJob: Job? = null
    private var returnFromLocationSettings = false
    private var discoveryActive = false
    private var startRouteCurrent = false
    private var startActivityForeground = false
    private var pendingUsbPermission: UsbPermissionRequest? = null
    private var promptedUsbAttachment: UsbDevice? = null
    private var waitingForBluetoothEnableResult = false
    val isUsbHostSupported get() = packageManager.hasSystemFeature(PackageManager.FEATURE_USB_HOST)
    val isStartRouteActive get() = startRouteCurrent && startActivityForeground

    init {
        if (isUsbHostSupported) {
            runCatching { usbManager.deviceList.values.firstOrNull() }.getOrNull()?.let(::newUsbDeviceAttached)
        }
        viewModelScope.launch {
            connectionState.collect { refreshConnecting() }
        }
    }

    fun isBtEnabled() = try { bluetoothAdapter?.isEnabled ?: false } catch (_: SecurityException) { false }
    fun isLocationEnabled(): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.P || Build.VERSION.SDK_INT >= Build.VERSION_CODES.R || locationManager.isLocationEnabled
    fun markLocationSettingsLaunched() { returnFromLocationSettings = true }
    fun consumeLocationSettingsReturn(): Boolean = returnFromLocationSettings.also { returnFromLocationSettings = false }
    fun selectedUsbDevice(): UsbDevice? = usbDevice
    suspend fun isBtDeviceAddressNotSelected(): Boolean {
        val address = BluetoothDeviceAddress.normalize(prefs.settings.first().bluetoothDeviceAddress) ?: return true
        val adapter = bluetoothAdapter ?: return true
        val found = try {
            BluetoothDeviceAddress.find(address, adapter.bondedDevices, ::bluetoothAddress)
        } catch (_: SecurityException) {
            return true
        }
        if (found == null) prefs.clearBluetoothDeviceAddress()
        return found == null
    }
    fun getBluetoothDeviceName(device: BluetoothDevice): String? = try { device.name } catch (_: SecurityException) { null }
    private fun bluetoothAddress(device: BluetoothDevice): String? = try { device.address } catch (_: SecurityException) { null }
    private fun bondState(device: BluetoothDevice) = try { device.bondState } catch (_: SecurityException) { null }
    private suspend fun saveDevice(device: BluetoothDevice) {
        prefs.setBluetoothDevice(
            BluetoothDeviceAddress.normalize(bluetoothAddress(device)),
            getBluetoothDeviceName(device),
        )
    }

    fun startBtDiscovery() = startBtDiscovery(resetDevices = true)

    fun markBluetoothEnableRequested() { waitingForBluetoothEnableResult = isStartRouteActive }

    fun consumeBluetoothEnableRequest(): Boolean = waitingForBluetoothEnableResult.also {
        waitingForBluetoothEnableResult = false
    }

    fun onStartRouteActive(active: Boolean) {
        if (startRouteCurrent == active) return
        startRouteCurrent = active
        if (!active) {
            cancelStartPreparation(clearPendingBond = true)
            pendingBluetoothPermissions = emptyArray()
            waitingForBluetoothEnableResult = false
            returnFromLocationSettings = false
            pendingUsbPermission = null
        }
    }

    fun onStartActivityForegrounded(foreground: Boolean) {
        if (startActivityForeground == foreground) return
        startActivityForeground = foreground
        if (!foreground) {
            cancelStartPreparation(clearPendingBond = false)
            pendingUsbPermission = null
        } else refreshConnecting()
    }

    private fun cancelStartPreparation(clearPendingBond: Boolean) {
        connectionJob?.cancel()
        connectionJob = null
        if (clearPendingBond) {
            bluetoothBondGate.cancel()
            pendingBluetoothBondDevice = null
        }
        cancelBtDiscovery()
        refreshConnecting()
    }

    fun requestUsbConnection(device: UsbDevice): UsbPermissionRequest? {
        if (!isStartRouteActive) return null
        val devices = attachedUsbDevicesSnapshot().getOrNull() ?: return null
        reconcileUsbAttachments(devices)
        val attached = devices.firstOrNull { sameUsbAttachment(it, device) } ?: return null
        return requestUsbConnectionForAttached(attached)
    }

    private fun requestUsbConnectionForAttached(attached: UsbDevice): UsbPermissionRequest? {
        if (!isStartRouteActive || !connectionManager.supportsUsbDevice(attached) || connectionManager.isConnectionRunning) return null
        if (pendingUsbPermission != null) return null
        if (promptedUsbAttachment?.let { !sameUsbAttachment(it, attached) } == true) promptedUsbAttachment = null
        if (hasUsbPermission(attached)) {
            connectionManager.startUsbConnection(attached)
            return null
        }
        if (promptedUsbAttachment?.let { sameUsbAttachment(it, attached) } == true) return null
        return UsbPermissionRequest(attached, UUID.randomUUID().toString()).also {
            promptedUsbAttachment = attached
            pendingUsbPermission = it
        }
    }

    fun synchronizeUsbAttachments(): UsbPermissionRequest? {
        if (!isStartRouteActive) return null
        val devices = attachedUsbDevicesSnapshot().getOrNull() ?: return null
        reconcileUsbAttachments(devices)
        if (devices.isEmpty()) {
            newUsbDeviceDetached()
            return null
        }
        val connectable = devices.firstOrNull(connectionManager::supportsUsbDevice)
        val selected = connectable ?: devices.first()
        newUsbDeviceAttached(selected)
        return connectable?.let(::requestUsbConnectionForAttached)
    }

    fun completeUsbPermissionRequest(device: UsbDevice, token: String, permissionGranted: Boolean) {
        val request = pendingUsbPermission?.takeIf { it.token == token } ?: return
        if (!isStartRouteActive || !sameUsbAttachment(request.device, device)) return
        pendingUsbPermission = null
        if (!permissionGranted) return
        val devices = attachedUsbDevicesSnapshot().getOrNull() ?: return
        reconcileUsbAttachments(devices)
        val attached = devices.firstOrNull { sameUsbAttachment(it, request.device) } ?: return
        if (!hasUsbPermission(attached) || connectionManager.isConnectionRunning) return
        connectionManager.startUsbConnection(attached)
    }

    fun cancelUsbPermissionRequest(token: String) {
        if (pendingUsbPermission?.token == token) pendingUsbPermission = null
    }

    internal open fun attachedUsbDevicesSnapshot(): Result<List<UsbDevice>> = runCatching { usbManager.deviceList.values.toList() }
    internal open fun hasUsbPermission(device: UsbDevice): Boolean =
        try { usbManager.hasPermission(device) } catch (_: SecurityException) { false }

    private fun reconcileUsbAttachments(devices: List<UsbDevice>) {
        connectionManager.reconcileUsbAttachments(Result.success(devices))
        promptedUsbAttachment?.takeIf { prompted -> devices.none { sameUsbAttachment(it, prompted) } }?.let {
            promptedUsbAttachment = null
        }
        pendingUsbPermission?.takeIf { request -> devices.none { sameUsbAttachment(it, request.device) } }?.let {
            pendingUsbPermission = null
        }
    }

    private fun sameUsbAttachment(first: UsbDevice, second: UsbDevice): Boolean =
        first.deviceId == second.deviceId && first.deviceName == second.deviceName &&
            first.vendorId == second.vendorId && first.productId == second.productId

    private fun cancelPendingUsbPermission() {
        pendingUsbPermission = null
    }

    fun resumeBtDiscoveryIfPickerVisible(hasScanPermissions: Boolean) {
        if (!isStartRouteActive) return
        val state = _uiState.value
        if (!state.showBluetoothDevices || state.connecting || connectionManager.isConnectionRunning ||
            connectionManager.isConnected || bluetoothBondGate.isPending() || discoveryActive || !hasScanPermissions
        ) return
        startBtDiscovery(resetDevices = false)
    }

    private fun startBtDiscovery(resetDevices: Boolean) {
        if (!isStartRouteActive) return
        try {
            val adapter = bluetoothAdapter ?: return
            if (!adapter.isEnabled) return
            if (discoveryActive && adapter.isDiscovering) return
            if (adapter.isDiscovering) adapter.cancelDiscovery()
            if (resetDevices) {
                devices.clear()
                devices.addAll(adapter.bondedDevices)
            }
            discoveryActive = true
            if (resetDevices) publishDevices(show = true)
            if (!adapter.startDiscovery()) discoveryActive = false
        } catch (_: SecurityException) { discoveryActive = false; setConnecting(false) }
    }
    fun cancelBtDiscovery() {
        discoveryActive = false
        try { bluetoothAdapter?.cancelDiscovery() } catch (_: SecurityException) { }
    }
    fun addDiscoveredDevice(device: BluetoothDevice) {
        if (!discoveryActive) return
        if (getBluetoothDeviceName(device).isNullOrEmpty()) return
        val address = bluetoothAddress(device) ?: return
        if (devices.none { bluetoothAddress(it) == address }) devices += device
        publishDevices(show = true)
    }
    fun selectBluetoothDevice(address: String) {
        if (!isStartRouteActive) return
        val device = devices.firstOrNull { bluetoothAddress(it) == address } ?: return
        cancelBtDiscovery()
        when (bondState(device)) {
            BluetoothDevice.BOND_BONDED -> startBluetoothConnection(device)
            else -> {
                bluetoothBondGate.begin(address)
                pendingBluetoothBondDevice = device
                setConnecting(true)
                val started = try { device.createBond() } catch (_: SecurityException) { false }
                if (!started) { bluetoothBondGate.cancel(); pendingBluetoothBondDevice = null; setConnecting(false) }
            }
        }
        _uiState.value = _uiState.value.copy(showBluetoothDevices = false)
    }
    fun onBondStateChanged(device: BluetoothDevice) {
        if (!isStartRouteActive) return
        val address = bluetoothAddress(device) ?: return
        if (!bluetoothBondGate.matches(address)) return
        when (bondState(device)) {
            BluetoothDevice.BOND_BONDED -> { bluetoothBondGate.complete(address); pendingBluetoothBondDevice = null; startBluetoothConnection(device) }
            BluetoothDevice.BOND_BONDING -> setConnecting(true)
            else -> { bluetoothBondGate.complete(address); pendingBluetoothBondDevice = null; setConnecting(false) }
        }
    }
    fun resumePendingBluetoothBond() {
        if (!isStartRouteActive) return
        val device = pendingBluetoothBondDevice ?: return
        if (bondState(device) != null) onBondStateChanged(device)
    }
    fun showBluetoothRationale() { _uiState.value = _uiState.value.copy(showBluetoothRationale = true) }
    fun dismissBluetoothRationale() { _uiState.value = _uiState.value.copy(showBluetoothRationale = false) }
    fun dismissBluetoothDevices() { _uiState.value = _uiState.value.copy(showBluetoothDevices = false) }
    private fun publishDevices(show: Boolean) {
        _uiState.value = _uiState.value.copy(showBluetoothDevices = show, bluetoothDevices = devices.mapNotNull { d ->
            val address = bluetoothAddress(d) ?: return@mapNotNull null
            getBluetoothDeviceName(d)?.takeIf(String::isNotEmpty)?.let { StartBluetoothDevice(address, it) }
        })
    }
    private fun setConnecting(value: Boolean) { _uiState.value = _uiState.value.copy(connecting = value) }
    private fun refreshConnecting() {
        val connectionState = connectionManager.currentConnectionState
        val managerConnectionInProgress = when (connectionState) {
            Disconnected, Connected, ConnectionTimeout -> false
            UsbAwaitingEcu, UsbReconnecting, UsbReconnectingAwaitingEcu -> true
            else -> connectionManager.isConnectionRunning && !connectionManager.isConnected
        }
        val usbAwaitingEcu = usbDevice != null &&
            (connectionState == UsbAwaitingEcu || connectionState == UsbReconnectingAwaitingEcu)
        setConnecting(
            connectionJob?.isActive == true ||
                bluetoothBondGate.isPending() ||
                managerConnectionInProgress,
        )
        _uiState.value = _uiState.value.copy(usbAwaitingEcu = usbAwaitingEcu)
    }
    fun startBluetoothConnection(selectedDevice: BluetoothDevice? = null) {
        if (!isStartRouteActive) return
        if (connectionJob?.isActive == true) return
        cancelPendingUsbPermission()
        cancelBtDiscovery()
        val job = viewModelScope.launch(start = CoroutineStart.LAZY) {
            try {
                selectedDevice?.let { saveDevice(it) }
                if (isBtDeviceAddressNotSelected()) {
                    if (!isStartRouteActive) return@launch
                    startBtDiscovery()
                    return@launch
                }
                if (!isStartRouteActive) return@launch
                connectionManager.startBtConnection()
            } catch (failure: IOException) {
                uiMessageQueue.publish(R.string.preferences_storage_error, UiMessageDuration.LONG)
            } finally {
                if (connectionJob === currentCoroutineContext()[Job]) {
                    connectionJob = null
                    refreshConnecting()
                }
            }
        }
        connectionJob = job
        job.start()
        refreshConnecting()
    }
    fun cancelConnection() {
        if (connectionManager.isUsbReady) return
        cancelPendingUsbPermission()
        connectionJob?.cancel()
        connectionJob = null
        bluetoothBondGate.cancel()
        pendingBluetoothBondDevice = null
        discoveryActive = false
        connectionManager.stopConnection()
        refreshConnecting()
    }
    fun newUsbDeviceAttached(device: UsbDevice) {
        if (!isStartRouteActive) return
        usbDevice = device
        _uiState.value = _uiState.value.copy(usbDeviceDetected = true)
        refreshConnecting()
    }
    fun newUsbDeviceDetached(device: UsbDevice? = usbDevice) {
        if (!isStartRouteActive) return
        if (device == null) {
            usbDevice = null
            promptedUsbAttachment = null
            pendingUsbPermission = null
            _uiState.value = _uiState.value.copy(usbDeviceDetected = false, usbAwaitingEcu = false)
            return
        }
        if (promptedUsbAttachment?.let { sameUsbAttachment(it, device) } == true) promptedUsbAttachment = null
        if (pendingUsbPermission?.device?.let { sameUsbAttachment(it, device) } == true) pendingUsbPermission = null
        if (usbDevice?.let { sameUsbAttachment(it, device) } == true) {
            usbDevice = null
            _uiState.value = _uiState.value.copy(usbDeviceDetected = false, usbAwaitingEcu = false)
        }
    }
    fun locationSettingsReturned(): Boolean {
        if (!consumeLocationSettingsReturn()) return false
        return isLocationEnabled()
    }
    override fun onCleared() { cancelBtDiscovery() }
}
