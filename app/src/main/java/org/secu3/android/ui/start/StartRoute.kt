package org.secu3.android.ui.start

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.content.IntentCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import org.secu3.android.connection.Connected
import org.secu3.android.ui.MainActivity
import org.secu3.android.ui.compose.SecuComposeTheme

internal const val USB_PERMISSION_ACTION = "org.secu3.android.USB_PERMISSION"

// Permission constants are strings and this helper returns them only for matching API levels.
@SuppressLint("InlinedApi")
internal fun bluetoothPermissionsFor(apiLevel: Int, xiaomi: Boolean): Array<String> {
    val base = if (apiLevel >= 31) {
        arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN).let {
            if (apiLevel == 31 && xiaomi) it + Manifest.permission.BLUETOOTH else it
        }
    } else {
        arrayOf(Manifest.permission.BLUETOOTH, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
    }
    return base + if (apiLevel == 29) arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION) else emptyArray()
}

@Composable
internal fun StartRoute(entry: NavBackStackEntry, activity: MainActivity, onConnected: () -> Unit) {
    val vm: StartScreenViewModel = viewModel(viewModelStoreOwner = entry, factory = activity.defaultViewModelProviderFactory, extras = entry.defaultViewModelCreationExtras)
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val state by vm.uiState.collectAsStateWithLifecycle()
    val connected by vm.isConnectedLiveData.observeAsState()
    var isResumed by remember { mutableStateOf(lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) }
    val continueBluetooth = remember { mutableStateOf<(() -> Unit)?>(null) }
    val btPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
        val requested = vm.pendingBluetoothPermissions.isNotEmpty()
        vm.pendingBluetoothPermissions = emptyArray()
        if (requested && grants.isNotEmpty() && grants.values.all { it }) continueBluetooth.value?.invoke()
    }
    val btEnableLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (vm.isBtEnabled()) continueBluetooth.value?.invoke()
    }
    val checkBluetoothConfiguration = {
        when {
            !vm.isBtEnabled() -> btEnableLauncher.launch(Intent(android.bluetooth.BluetoothAdapter.ACTION_REQUEST_ENABLE))
            !vm.isLocationEnabled() -> {
                vm.markLocationSettingsLaunched()
                context.startActivity(Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            }
            vm.isBtDeviceAddressNotSelected() -> vm.startBtDiscovery()
            else -> vm.startConnection(null)
        }
    }
    SideEffect { continueBluetooth.value = checkBluetoothConfiguration }
    val requestUsbPermissionOrConnect: (UsbDevice?) -> Unit = { device ->
        if (device != null) {
            if (runCatching { vm.usbManager.hasPermission(device) }.getOrDefault(false)) vm.startConnection(device) else if (vm.beginUsbPermissionRequest(device)) {
                val intent = Intent(USB_PERMISSION_ACTION).setPackage(context.packageName)
                val flags = android.app.PendingIntent.FLAG_UPDATE_CURRENT or
                    if (Build.VERSION.SDK_INT >= 31) android.app.PendingIntent.FLAG_MUTABLE else 0
                vm.usbManager.requestPermission(device, android.app.PendingIntent.getBroadcast(context, 0, intent, flags))
            }
        }
    }

    DisposableEffect(lifecycle, context, vm) {
        val usbReceiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context, intent: Intent) {
                val device = IntentCompat.getParcelableExtra(intent, UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
                when (intent.action) {
                    UsbManager.ACTION_USB_DEVICE_ATTACHED -> device?.let(vm::newUsbDeviceAttached)
                    UsbManager.ACTION_USB_DEVICE_DETACHED -> vm.newUsbDeviceDetached()
                }
            }
        }
        val usbPermissionReceiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context, intent: Intent) {
                if (intent.action != USB_PERMISSION_ACTION) return
                val device = IntentCompat.getParcelableExtra(intent, UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
                vm.completeUsbPermissionRequest(device, intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false))
            }
        }
        val btReceiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context, intent: Intent) {
                val device = IntentCompat.getParcelableExtra(intent, BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java) ?: return
                when (intent.action) {
                    BluetoothDevice.ACTION_FOUND -> vm.addDiscoveredDevice(device)
                    BluetoothDevice.ACTION_BOND_STATE_CHANGED -> vm.onBondStateChanged(device)
                }
            }
        }
        val observer = LifecycleEventObserver { _, event ->
            isResumed = lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
            if (event == Lifecycle.Event.ON_RESUME) {
                activity.applyKeepScreenOnPreference()
                val manager = context.getSystemService(Context.USB_SERVICE) as UsbManager
                manager.deviceList.values.firstOrNull()?.let(vm::newUsbDeviceAttached) ?: vm.newUsbDeviceDetached()
                if (vm.locationSettingsReturned()) continueBluetooth.value?.invoke()
            }
            if (event == Lifecycle.Event.ON_PAUSE) vm.cancelBtDiscovery()
        }
        ContextCompat.registerReceiver(context, usbReceiver, IntentFilter().apply {
            addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED); addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        }, ContextCompat.RECEIVER_EXPORTED)
        ContextCompat.registerReceiver(context, usbPermissionReceiver, IntentFilter(USB_PERMISSION_ACTION), ContextCompat.RECEIVER_NOT_EXPORTED)
        ContextCompat.registerReceiver(context, btReceiver, IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND); addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
        }, ContextCompat.RECEIVER_EXPORTED)
        lifecycle.addObserver(observer)
        activity.setStartUsbIntentHandler(requestUsbPermissionOrConnect)
        onDispose {
            activity.setStartUsbIntentHandler(null)
            lifecycle.removeObserver(observer)
            runCatching { context.unregisterReceiver(usbReceiver) }; runCatching { context.unregisterReceiver(usbPermissionReceiver) }; runCatching { context.unregisterReceiver(btReceiver) }
        }
    }
    LaunchedEffect(connected, isResumed) {
        if (connected == Connected && isResumed && vm.claimHomeNavigation()) onConnected()
        if (connected != Connected) vm.resetHomeNavigation()
    }

    SecuComposeTheme {
        StartScreenContent(
            StartScreenState(state.connecting, state.usbDeviceDetected, state.showBluetoothDevices, state.bluetoothDevices, state.showBluetoothRationale),
            StartScreenActions(
                onConnectOrCancel = { inProgress ->
                    if (inProgress) vm.cancelConnection()
                    else if (vm.selectedUsbDevice() != null) requestUsbPermissionOrConnect(vm.selectedUsbDevice())
                    else {
                        val permissions = bluetoothPermissionsFor(Build.VERSION.SDK_INT, vm.isXiaomi)
                        if (permissions.all { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }) {
                            checkBluetoothConfiguration()
                        } else {
                            vm.pendingBluetoothPermissions = permissions
                            if (permissions.any { activity.shouldShowRequestPermissionRationale(it) }) vm.showBluetoothRationale()
                            else btPermissionLauncher.launch(permissions)
                        }
                    }
                },
                onLogs = { activity.openLogs() }, onSettings = { activity.openSettings() },
                onBluetoothDeviceSelected = { address -> vm.selectBluetoothDevice(address) },
                onBluetoothSheetDismissed = { vm.cancelBtDiscovery(); vm.dismissBluetoothDevices() },
                onRationaleConfirmed = { vm.dismissBluetoothRationale(); btPermissionLauncher.launch(vm.pendingBluetoothPermissions) },
                onRationaleCancelled = vm::dismissBluetoothRationale,
            ),
        )
    }
}
