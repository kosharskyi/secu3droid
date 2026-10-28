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
import org.secu3.android.R
import org.secu3.android.ui.messages.UiMessageDuration
import org.secu3.android.ui.messages.UiMessageQueue
import org.secu3.android.connection.BluetoothDeviceAddress
import org.secu3.android.connection.ConnectionTimeout
import org.secu3.android.connection.Connected
import org.secu3.android.connection.Disconnected
import org.secu3.android.connection.Secu3ConnectionManager
import org.secu3.android.utils.UserPrefs
import javax.inject.Inject

@HiltViewModel
class StartScreenViewModel @Inject constructor(
    private val prefs: UserPrefs,
    private val bluetoothManager: BluetoothManager,
    private val locationManager: LocationManager,
    private val connectionManager: Secu3ConnectionManager,
    private val packageManager: PackageManager,
    val usbManager: UsbManager,
    private val uiMessageQueue: UiMessageQueue,
) : ViewModel() {
    private val _uiState = MutableStateFlow(StartScreenState())
    val uiState = _uiState.asStateFlow()
    val connectionState = connectionManager.connectionStateFlow
    val isUsbSessionLocked get() = connectionManager.isUsbSessionLocked
    private val bluetoothAdapter: BluetoothAdapter? get() = runCatching { bluetoothManager.adapter }.getOrNull()
    private var usbDevice: UsbDevice? = null
    private val devices = mutableListOf<BluetoothDevice>()
    private var homeNavigationSent = false
    var pendingBluetoothPermissions: Array<String> = emptyArray()
    private val bluetoothBondGate = BluetoothBondRequestGate()
    private var connectionJob: Job? = null
    private var returnFromLocationSettings = false
    private var discoveryActive = false
    val isUsbHostSupported get() = packageManager.hasSystemFeature(PackageManager.FEATURE_USB_HOST)
    val isXiaomi = Build.MANUFACTURER.equals("Xiaomi", true) || Build.BRAND.equals("Xiaomi", true) || Build.BRAND.equals("POCO", true) || Build.MODEL.contains("POCO", true)

    init {
        if (isUsbHostSupported) usbManager.deviceList.values.firstOrNull()?.let(::newUsbDeviceAttached)
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

    fun resumeBtDiscoveryIfPickerVisible(hasScanPermissions: Boolean) {
        val state = _uiState.value
        if (!shouldResumeBluetoothDiscovery(
                pickerVisible = state.showBluetoothDevices,
                connecting = state.connecting,
                connectionRunning = connectionManager.isConnectionRunning,
                connected = connectionManager.isConnected,
                pairing = bluetoothBondGate.isPending(),
            ) || discoveryActive || !hasScanPermissions
        ) return
        startBtDiscovery(resetDevices = false)
    }

    private fun startBtDiscovery(resetDevices: Boolean) {
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
        val device = devices.firstOrNull { bluetoothAddress(it) == address } ?: return
        cancelBtDiscovery()
        when (bondState(device)) {
            BluetoothDevice.BOND_BONDED -> startBluetoothConnection(device)
            else -> {
                bluetoothBondGate.begin(address)
                setConnecting(true)
                val started = try { device.createBond() } catch (_: SecurityException) { false }
                if (!started) { bluetoothBondGate.cancel(); setConnecting(false) }
            }
        }
        _uiState.value = _uiState.value.copy(showBluetoothDevices = false)
    }
    fun onBondStateChanged(device: BluetoothDevice) {
        val address = bluetoothAddress(device) ?: return
        if (!bluetoothBondGate.matches(address)) return
        when (bondState(device)) {
            BluetoothDevice.BOND_BONDED -> { bluetoothBondGate.complete(address); startBluetoothConnection(device) }
            BluetoothDevice.BOND_BONDING -> setConnecting(true)
            else -> { bluetoothBondGate.complete(address); setConnecting(false) }
        }
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
            else -> connectionManager.isConnectionRunning && !connectionManager.isConnected
        }
        setConnecting(
            connectionJob?.isActive == true ||
                bluetoothBondGate.isPending() ||
                managerConnectionInProgress,
        )
    }
    fun startBluetoothConnection(selectedDevice: BluetoothDevice? = null) {
        if (connectionJob?.isActive == true) return
        if (!connectionManager.isConnected) homeNavigationSent = false
        cancelBtDiscovery()
        val job = viewModelScope.launch(start = CoroutineStart.LAZY) {
            try {
                selectedDevice?.let { saveDevice(it) }
                if (isBtDeviceAddressNotSelected()) {
                    startBtDiscovery()
                    return@launch
                }
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
        connectionJob?.cancel()
        connectionJob = null
        bluetoothBondGate.cancel()
        discoveryActive = false
        connectionManager.stopConnection()
        refreshConnecting()
    }
    fun claimHomeNavigation(): Boolean {
        if (homeNavigationSent || !connectionManager.isConnected) return false
        homeNavigationSent = true
        return true
    }
    fun resetHomeNavigation() { if (!connectionManager.isConnected) homeNavigationSent = false }
    fun newUsbDeviceAttached(device: UsbDevice) { usbDevice = device; _uiState.value = _uiState.value.copy(usbDeviceDetected = true) }
    fun newUsbDeviceDetached() { usbDevice = null; _uiState.value = _uiState.value.copy(usbDeviceDetected = false) }
    fun locationSettingsReturned(): Boolean {
        if (!consumeLocationSettingsReturn()) return false
        return isLocationEnabled()
    }
    override fun onCleared() { cancelBtDiscovery() }
}
