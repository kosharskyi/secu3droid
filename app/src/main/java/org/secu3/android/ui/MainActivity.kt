package org.secu3.android.ui

import android.content.Intent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.os.Bundle
import android.view.WindowManager
import android.graphics.drawable.ColorDrawable
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.core.content.IntentCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import android.widget.Toast
import org.secu3.android.connection.Connected
import org.secu3.android.connection.ConnectionFailed
import org.secu3.android.connection.ConnectionLost
import android.hardware.usb.UsbManager
import android.app.PendingIntent
import android.os.Build
import org.secu3.android.R
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavDestination
import androidx.navigation.NavHostController
import dagger.hilt.android.AndroidEntryPoint
import org.secu3.android.connection.UsbConnectionRequest

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: MainActivityViewModel by viewModels()
    internal val userPrefs get() = viewModel.prefs
    private var rootNavController: NavHostController? = null
    internal val rootNavigationController: NavHostController? get() = rootNavController
    private var pendingNavigation: String? = null
    private var pendingUsbHome = false
    private var pendingUsbIntentDevice: UsbDevice? = null
    private var startUsbIntentHandler: ((UsbDevice?) -> Unit)? = null
    private var activeComposeRoute: String? = null
    private lateinit var usbReceiver: BroadcastReceiver
    private lateinit var usbPermissionReceiver: BroadcastReceiver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        registerUsbLifecycle()
        lifecycle.addObserver(LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                processPendingNavigation()
                deliverPendingUsbIntent()
            }
        })
        if (savedInstanceState == null) handleUsbIntent(intent)
        setContent { RootNavigation(this) }
        lifecycleScope.launch {
            viewModel.connectionManager.connectionStateFlow.collect { state ->
                if (state == Connected && viewModel.connectionManager.usbSessionLocked.value) {
                    if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) applyConnectionServicePolicy()
                    openUsbHome()
                } else if ((state is ConnectionFailed || state is ConnectionLost) && viewModel.connectionManager.activeUsbDeviceIdOrNull != null) {
                    val failureMessage = if (state is ConnectionLost) R.string.usb_connection_lost else R.string.usb_connection_failed
                    viewModel.requestStartNavigation()
                    if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) openStart()
                    stopConnectionService()
                    Toast.makeText(this@MainActivity, getString(failureMessage), Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    fun openHome() = navigateTo(HOME_ROUTE)
    fun openParameters() = navigateTo(PARAMETERS_ROUTE)
    fun openSettings() = navigateTo(SETTINGS_ROUTE)
    fun openErrors() = navigateTo(ERRORS_ROUTE)
    fun openDiagnostics() {
        if (!isInsideDiagnosticsGraph(rootNavController?.currentDestination)) navigateTo(DIAGNOSTICS_GRAPH_ROUTE)
    }
    fun openLogs() = navigateTo(LOGS_ROUTE)
    fun openFirmware() = navigateTo(FIRMWARE_ROUTE)
    fun openDashboard() = navigateTo(DASHBOARD_ROUTE)
    fun openSensors() = navigateTo(SENSORS_ROUTE)
    fun openStart(): Boolean {
        val controller = rootNavController ?: run { pendingNavigation = START_ROUTE; return false }
        if (controller.currentDestination == null || !lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            pendingNavigation = START_ROUTE
            return false
        }
        pendingNavigation = null
        pendingUsbHome = false
        viewModel.consumeStartNavigation()
        if (controller.currentDestination?.route == START_ROUTE) return true
        if (!controller.popBackStack(START_ROUTE, inclusive = false)) {
            controller.navigate(START_ROUTE) { popUpTo(controller.graph.id) { inclusive = true }; launchSingleTop = true }
        }
        return true
    }

    private fun navigateTo(route: String) {
        val controller = rootNavController
        if (controller == null || controller.currentDestination == null || !lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            pendingNavigation = route
            return
        }
        if (controller.currentDestination?.route == route) return
        if (route == DIAGNOSTICS_GRAPH_ROUTE && isInsideDiagnosticsGraph(controller.currentDestination)) return
        controller.navigate(route) { launchSingleTop = true }
    }

    internal fun attachRootNavController(controller: NavHostController) {
        rootNavController = controller
        processPendingNavigation()
        deliverPendingUsbIntent()
    }

    internal fun onRootDestinationChanged(destination: NavDestination?) {
        updateActiveComposeRoute(destination?.route)
        processPendingNavigation()
        deliverPendingUsbIntent()
    }

    internal fun detachRootNavController(controller: NavHostController) {
        if (rootNavController === controller) rootNavController = null
    }

    private fun processPendingNavigation() {
        if (!lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) return
        val route = pendingNavigation ?: return
        val controller = rootNavController ?: return
        if (controller.currentDestination == null) return
        pendingNavigation = null
        if (route == START_ROUTE) {
            openStart()
        } else if (route == HOME_ROUTE && pendingUsbHome) {
            pendingUsbHome = false
            if (viewModel.connectionManager.usbSessionLocked.value) openUsbHome() else openStart()
        } else if (controller.currentDestination?.route != route &&
            !(route == DIAGNOSTICS_GRAPH_ROUTE && isInsideDiagnosticsGraph(controller.currentDestination))
        ) {
            controller.navigate(route) { launchSingleTop = true }
        }
    }

    internal fun setStartUsbIntentHandler(handler: ((UsbDevice?) -> Unit)?) {
        startUsbIntentHandler = handler
        deliverPendingUsbIntent()
    }

    private fun deliverPendingUsbIntent() {
        if (rootNavController?.currentDestination?.route != START_ROUTE ||
            !lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
        ) return
        val device = pendingUsbIntentDevice ?: return
        val handler = startUsbIntentHandler ?: return
        pendingUsbIntentDevice = null
        handler(device)
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleUsbIntent(intent)
    }

    private fun handleUsbIntent(intent: Intent) {
        if (intent.action == ACTION_USB_DEVICE_ATTACHED) {
            val device = IntentCompat.getParcelableExtra(intent, android.hardware.usb.UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
            pendingUsbIntentDevice = null
            device?.let(::requestUsbDeviceConnection)
        }
    }

    private fun registerUsbLifecycle() {
        usbReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val device = IntentCompat.getParcelableExtra(intent, UsbManager.EXTRA_DEVICE, UsbDevice::class.java) ?: return
                when (intent.action) {
                    UsbManager.ACTION_USB_DEVICE_ATTACHED -> requestUsbDeviceConnection(device)
                    UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                        if (viewModel.connectionManager.onUsbDeviceDetached(device.deviceId)) {
                            viewModel.requestStartNavigation()
                            openStart()
                        }
                    }
                }
            }
        }
        usbPermissionReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action != USB_PERMISSION_ACTION) return
                val device = IntentCompat.getParcelableExtra(intent, UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
                val token = intent.getLongExtra(EXTRA_USB_ATTEMPT_TOKEN, -1L)
                val resultDevice = device ?: return
                viewModel.connectionManager.completeUsbPermissionRequest(
                    device = resultDevice,
                    token = token,
                    permissionGranted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false),
                )
            }
        }
        ContextCompat.registerReceiver(this, usbReceiver, IntentFilter().apply {
            addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED); addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        }, ContextCompat.RECEIVER_EXPORTED)
        ContextCompat.registerReceiver(this, usbPermissionReceiver, IntentFilter(USB_PERMISSION_ACTION), ContextCompat.RECEIVER_NOT_EXPORTED)
    }

    internal fun requestUsbDeviceConnection(device: UsbDevice) {
        when (val request = viewModel.connectionManager.requestUsbConnection(device)) {
            UsbConnectionRequest.Ignored, UsbConnectionRequest.Started -> Unit
            is UsbConnectionRequest.PermissionRequired -> requestUsbPermission(request.device, request.token)
        }
    }

    private fun requestUsbPermission(device: UsbDevice, token: Long) {
        val usbManager = getSystemService(USB_SERVICE) as UsbManager
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or if (Build.VERSION.SDK_INT >= 31) PendingIntent.FLAG_MUTABLE else 0
        val permissionIntent = Intent(USB_PERMISSION_ACTION).setPackage(packageName).putExtra(EXTRA_USB_ATTEMPT_TOKEN, token)
        usbManager.requestPermission(device, PendingIntent.getBroadcast(this, token.toInt(), permissionIntent, flags))
    }

    private fun openUsbHome() {
        val generation = viewModel.connectionManager.activeUsbSessionGeneration ?: return
        val controller = rootNavController ?: run { pendingNavigation = HOME_ROUTE; pendingUsbHome = true; return }
        if (!lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) || controller.currentDestination == null) {
            pendingNavigation = HOME_ROUTE
            pendingUsbHome = true
            return
        }
        if (!viewModel.claimUsbHomeNavigation(generation)) return
        viewModel.consumeStartNavigation()
        controller.navigate(HOME_ROUTE) {
            popUpTo(controller.graph.id) { inclusive = true }
            launchSingleTop = true
        }
    }

    override fun onDestroy() {
        runCatching { unregisterReceiver(usbReceiver) }
        runCatching { unregisterReceiver(usbPermissionReceiver) }
        super.onDestroy()
    }

    private companion object {
        const val ACTION_USB_DEVICE_ATTACHED = "android.hardware.usb.action.USB_DEVICE_ATTACHED"
        const val USB_PERMISSION_ACTION = "org.secu3.android.USB_PERMISSION"
        const val EXTRA_USB_ATTEMPT_TOKEN = "usb_attempt_token"
    }
    override fun onResume() {
        super.onResume()
        if (activeComposeRoute == DASHBOARD_ROUTE) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        else applyKeepScreenOnPreference()
        if ((viewModel.connectionManager.currentConnectionState is ConnectionFailed || viewModel.connectionManager.currentConnectionState is ConnectionLost) && viewModel.connectionManager.activeUsbDeviceIdOrNull != null) {
            viewModel.requestStartNavigation()
        }
        if (viewModel.consumeStartNavigation()) openStart()
        val usbManager = getSystemService(USB_SERVICE) as UsbManager
        val devices = usbManager.deviceList.values
        val ids = devices.mapTo(mutableSetOf()) { it.deviceId }
        viewModel.connectionManager.reconcileUsbAttachments(devices)
        val activeId = viewModel.connectionManager.activeUsbDeviceIdOrNull
        if (activeId != null && activeId !in ids && viewModel.connectionManager.onUsbDeviceDetached(activeId)) {
            viewModel.requestStartNavigation()
            openStart()
        }
        devices.firstOrNull(viewModel.connectionManager::isUsbConnectionCandidate)
            ?.let(::requestUsbDeviceConnection)
        if (viewModel.connectionManager.usbSessionLocked.value) applyConnectionServicePolicy()
    }

    internal fun updateActiveComposeRoute(route: String?) { activeComposeRoute = route }

    internal fun applyKeepScreenOnPreference() {
        if (viewModel.prefs.isKeepScreenAliveActive) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    internal fun applyAppearance(dark: Boolean) {
        val color = if (dark) android.graphics.Color.rgb(17, 24, 32) else android.graphics.Color.rgb(243, 246, 250)
        val darkNavigationBar = android.graphics.Color.rgb(17, 24, 32)
        val supportsLightNavigationIcons = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O
        window.setBackgroundDrawable(ColorDrawable(color))
        window.statusBarColor = color
        window.navigationBarColor = if (dark || !supportsLightNavigationIcons) darkNavigationBar else color
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = !dark
            isAppearanceLightNavigationBars = !dark && supportsLightNavigationIcons
        }
    }

    internal fun applyConnectionServicePolicy() {
        val intent = Intent(this, org.secu3.android.connection.SecuConnectionService::class.java)
        if (viewModel.connectionManager.usbSessionLocked.value || viewModel.prefs.isWakeLockEnabled) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) startForegroundService(intent)
            else startService(intent)
        } else {
            stopConnectionService()
        }
    }

    internal fun stopConnectionService() {
        stopService(Intent(this, org.secu3.android.connection.SecuConnectionService::class.java))
    }

    private fun isInsideDiagnosticsGraph(destination: NavDestination?): Boolean {
        var current = destination
        while (current != null) {
            if (current.route == DIAGNOSTICS_GRAPH_ROUTE) return true
            current = current.parent
        }
        return false
    }

}
