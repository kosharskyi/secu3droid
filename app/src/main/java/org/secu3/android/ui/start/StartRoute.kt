package org.secu3.android.ui.start

import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.app.PendingIntent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.content.IntentCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import org.secu3.android.ui.MainActivity
import org.secu3.android.ui.START_ROUTE
import org.secu3.android.ui.compose.SecuComposeTheme
import org.secu3.android.R
import org.secu3.android.ui.messages.UiMessageDuration
import kotlinx.coroutines.launch
import java.io.IOException


@Composable
internal fun StartRoute(entry: NavBackStackEntry, activity: MainActivity) {
    val vm: StartScreenViewModel = viewModel(viewModelStoreOwner = entry, factory = activity.defaultViewModelProviderFactory, extras = entry.defaultViewModelCreationExtras)
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val coroutineScope = rememberCoroutineScope()
    val state by vm.uiState.collectAsStateWithLifecycle()
    val continueBluetooth = remember { mutableStateOf<(() -> Unit)?>(null) }
    val pendingBluetoothContinuation = rememberSaveable { mutableStateOf(false) }
    val startRouteIsCurrent: () -> Boolean = {
        activity.rootNavigationController?.currentDestination?.route == START_ROUTE
    }
    val btPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
        val requested = vm.pendingBluetoothPermissions.isNotEmpty()
        vm.pendingBluetoothPermissions = emptyArray()
        if (requested && startRouteIsCurrent() && grants.isNotEmpty() && grants.values.all { it }) {
            if (vm.isStartRouteActive) coroutineScope.launch { continueBluetooth.value?.invoke() }
            else pendingBluetoothContinuation.value = true
        }
    }
    val btEnableLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (vm.consumeBluetoothEnableRequest() && startRouteIsCurrent() && vm.isBtEnabled()) {
            if (vm.isStartRouteActive) coroutineScope.launch { continueBluetooth.value?.invoke() }
            else pendingBluetoothContinuation.value = true
        }
    }
    val checkBluetoothConfiguration: () -> Unit = {
        when {
            !vm.isStartRouteActive -> Unit
            !vm.isBtEnabled() -> {
                vm.markBluetoothEnableRequested()
                btEnableLauncher.launch(Intent(android.bluetooth.BluetoothAdapter.ACTION_REQUEST_ENABLE))
            }
            !vm.isLocationEnabled() -> {
                vm.markLocationSettingsLaunched()
                context.startActivity(Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            }
            else -> vm.startBluetoothConnection()
        }
    }
    SideEffect { continueBluetooth.value = checkBluetoothConfiguration }
    val requestUsbPermission: (UsbDevice, Long) -> Unit = { device, token ->
        val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or if (Build.VERSION.SDK_INT >= 31) PendingIntent.FLAG_MUTABLE else 0
        val permissionIntent = Intent("org.secu3.android.USB_PERMISSION").setPackage(context.packageName).putExtra("usb_attempt_token", token)
        usbManager.requestPermission(device, PendingIntent.getBroadcast(context, token.toInt(), permissionIntent, flags))
    }
    val requestUsbPermissionOrConnect: (UsbDevice?) -> Unit = { device ->
        if (device != null) vm.requestUsbConnection(device)?.let { requestUsbPermission(it.device, it.token) }
    }
    val resumeStartRoute: () -> Unit = {
        activity.applyKeepScreenOnPreference()
        val visibleStart = startRouteIsCurrent()
        vm.onStartRouteActive(visibleStart)
        if (visibleStart) {
            val manager = context.getSystemService(Context.USB_SERVICE) as UsbManager
            val attached = manager.deviceList.values.firstOrNull()
            attached?.let(vm::newUsbDeviceAttached) ?: vm.newUsbDeviceDetached()
            vm.synchronizeUsbAttachments()?.let { requestUsbPermission(it.device, it.token) }
        }
        if (visibleStart && vm.isStartRouteActive && pendingBluetoothContinuation.value) {
            pendingBluetoothContinuation.value = false
            coroutineScope.launch { continueBluetooth.value?.invoke() }
        }
        if (visibleStart) vm.resumePendingBluetoothBond()
        if (vm.locationSettingsReturned()) coroutineScope.launch { continueBluetooth.value?.invoke() }
        val scanPermissions = vm.bluetoothPermissionPolicy.discoveryPermissions()
        val hasScanPermissions = scanPermissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
        vm.resumeBtDiscoveryIfPickerVisible(hasScanPermissions)
    }

    DisposableEffect(lifecycle, context, vm) {
        val usbReceiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context, intent: Intent) {
                val device = IntentCompat.getParcelableExtra(intent, UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
                when (intent.action) {
                    UsbManager.ACTION_USB_DEVICE_ATTACHED -> device?.let {
                        vm.newUsbDeviceAttached(it)
                        requestUsbPermissionOrConnect(it)
                    }
                    UsbManager.ACTION_USB_DEVICE_DETACHED -> vm.newUsbDeviceDetached()
                }
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
        val usbPermissionReceiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context, intent: Intent) {
                if (intent.action != "org.secu3.android.USB_PERMISSION") return
                val device = IntentCompat.getParcelableExtra(intent, UsbManager.EXTRA_DEVICE, UsbDevice::class.java) ?: return
                vm.completeUsbPermissionRequest(
                    device,
                    intent.getLongExtra("usb_attempt_token", -1L),
                    intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false),
                )
            }
        }
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                resumeStartRoute()
            }
            if (event == Lifecycle.Event.ON_PAUSE) vm.cancelBtDiscovery()
        }
        activity.setStartRouteActivityHandler { active ->
            vm.onStartRouteActive(active)
            if (!active) pendingBluetoothContinuation.value = false
        }
        activity.setStartRouteForegroundHandler(vm::onStartActivityForegrounded)
        ContextCompat.registerReceiver(context, usbReceiver, IntentFilter().apply {
            addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
            addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        }, ContextCompat.RECEIVER_EXPORTED)
        ContextCompat.registerReceiver(context, btReceiver, IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND); addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
        }, ContextCompat.RECEIVER_EXPORTED)
        ContextCompat.registerReceiver(context, usbPermissionReceiver, IntentFilter("org.secu3.android.USB_PERMISSION"), ContextCompat.RECEIVER_NOT_EXPORTED)
        lifecycle.addObserver(observer)
        activity.setStartUsbIntentHandler(requestUsbPermissionOrConnect)
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) resumeStartRoute()
        onDispose {
            if (!activity.isChangingConfigurations) vm.onStartRouteActive(false)
            activity.setStartRouteForegroundHandler(null)
            activity.setStartRouteActivityHandler(null)
            activity.setStartUsbIntentHandler(null)
            lifecycle.removeObserver(observer)
            runCatching { context.unregisterReceiver(usbReceiver) }; runCatching { context.unregisterReceiver(btReceiver) }; runCatching { context.unregisterReceiver(usbPermissionReceiver) }
        }
    }
    SecuComposeTheme {
        StartScreenContent(
            state,
            StartScreenActions(
                onConnectOrCancel = { inProgress ->
                    if (inProgress) vm.cancelConnection()
                    else if (vm.selectedUsbDevice() != null) requestUsbPermissionOrConnect(vm.selectedUsbDevice())
                    else {
                        val permissions = vm.bluetoothPermissionPolicy.connectionPermissions()
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
