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
import android.os.SystemClock
import android.view.accessibility.AccessibilityManager
import javax.inject.Inject
import org.secu3.android.connection.Connected
import org.secu3.android.connection.ConnectionFailed
import org.secu3.android.connection.ConnectionLost
import android.hardware.usb.UsbManager
import android.app.PendingIntent
import android.app.KeyguardManager
import android.os.Build
import org.secu3.android.R
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import androidx.navigation.NavDestination
import androidx.navigation.NavHostController
import dagger.hilt.android.AndroidEntryPoint
import org.secu3.android.connection.ConnectionSessionTermination
import org.secu3.android.connection.UsbConnectionRequest
import org.secu3.android.ui.messages.UiMessageQueue
import org.secu3.android.ui.messages.UiMessageDuration

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var uiMessageQueue: UiMessageQueue
    private val viewModel: MainActivityViewModel by viewModels()
    internal val userPrefs get() = viewModel.prefs
    private var rootNavController: NavHostController? = null
    internal val rootNavigationController: NavHostController? get() = rootNavController
    private var pendingNavigation: String? = null
    private var pendingUsbHomeGeneration: Long? = null
    private var pendingTerminalStartGeneration: Long? = null
    private var pendingUsbIntentDevice: UsbDevice? = null
    private var startUsbIntentHandler: ((UsbDevice?) -> Unit)? = null
    private var activeComposeRoute: String? = null
    private lateinit var usbReceiver: BroadcastReceiver
    private lateinit var usbPermissionReceiver: BroadcastReceiver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        uiMessageQueue.onActivityAttached()
        registerUsbLifecycle()
        lifecycle.addObserver(LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                restorePendingTerminalStart()
                processPendingNavigation()
                deliverPendingUsbIntent()
            }
        })
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                while (true) {
                    uiMessageQueue.discardExpired()
                    val message = uiMessageQueue.availableMessages.first { it.isNotEmpty() }.first()
                    if (message.expiresAtMillis <= SystemClock.elapsedRealtime()) {
                        uiMessageQueue.discardExpired()
                        continue
                    }
                    val text = if (message.arguments.isEmpty()) getString(message.textRes) else getString(
                        message.textRes,
                        *message.arguments.map { it.value() }.toTypedArray(),
                    )
                    val toast = Toast.makeText(this@MainActivity, text, toastLength(message.duration))
                    try {
                        toast.show()
                        uiMessageQueue.acknowledge(message.id)
                        delay(toastPacingMillis(message.duration))
                    } catch (cancelled: CancellationException) {
                        toast.cancel()
                        throw cancelled
                    }
                }
            }
        }
        if (savedInstanceState == null) handleUsbIntent(intent)
        setContent { RootNavigation(this) }
        lifecycleScope.launch {
            viewModel.connectionManager.connectionStateFlow.collect { state ->
                if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) && isDeviceUnlocked()) {
                    viewModel.connectionManager.onConnectionStateObservedInForeground(state)
                }
                if (state == Connected && viewModel.connectionManager.usbSessionLocked.value) {
                    openUsbHome()
                } else if ((state is ConnectionFailed || state is ConnectionLost) && viewModel.connectionManager.activeUsbDeviceIdOrNull != null) {
                    val failureMessage = if (state is ConnectionLost) R.string.usb_connection_lost else R.string.usb_connection_failed
                    uiMessageQueue.publish(failureMessage, org.secu3.android.ui.messages.UiMessageDuration.LONG)
                }
            }
        }
        lifecycleScope.launch {
            viewModel.connectionManager.terminalSession.collect(::onTerminalSessionChanged)
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
    internal fun onBackgroundWorkPreferenceChanged(enabled: Boolean) {
        viewModel.connectionManager.onBackgroundWorkPreferenceChanged(enabled)
    }

    internal fun refreshConnectionNotification() {
        viewModel.connectionManager.refreshConnectionNotification()
    }

    fun openStart(): Boolean {
        val controller = rootNavController ?: run { pendingNavigation = START_ROUTE; return false }
        if (controller.currentDestination == null || !lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) || !isDeviceUnlocked()) {
            pendingNavigation = START_ROUTE
            return false
        }
        pendingNavigation = null
        pendingUsbHomeGeneration = null
        pendingTerminalStartGeneration?.let(viewModel::completeTerminalStart)
        pendingTerminalStartGeneration = null
        if (controller.currentDestination?.route == START_ROUTE) return true
        if (!controller.popBackStack(START_ROUTE, inclusive = false)) {
            controller.navigate(START_ROUTE) { popUpTo(controller.graph.id) { inclusive = true }; launchSingleTop = true }
        }
        return true
    }

    private fun navigateTo(route: String) {
        val controller = rootNavController
        if (controller == null || controller.currentDestination == null ||
            !lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) || !isDeviceUnlocked()
        ) {
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
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) && isDeviceUnlocked()) {
            viewModel.connectionManager.onAppForegrounded()
        } else if (!isDeviceUnlocked()) {
            viewModel.connectionManager.onAppBackgrounded()
        }
        processPendingNavigation()
        deliverPendingUsbIntent()
    }

    internal fun detachRootNavController(controller: NavHostController) {
        if (rootNavController === controller) rootNavController = null
    }

    private fun processPendingNavigation() {
        if (!lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) || !isDeviceUnlocked()) return
        val route = pendingNavigation ?: return
        val terminalGeneration = pendingTerminalStartGeneration
        if (route == START_ROUTE && terminalGeneration != null && viewModel.connectionManager.terminalSession.value?.generation != terminalGeneration) {
            pendingTerminalStartGeneration = null
            viewModel.completeTerminalStart(terminalGeneration)
            if (route == START_ROUTE) pendingNavigation = null
            return
        }
        val controller = rootNavController ?: return
        if (controller.currentDestination == null) return
        pendingNavigation = null
        if (route == START_ROUTE) {
            openStart()
        } else if (route == HOME_ROUTE && pendingUsbHomeGeneration != null) {
            val generation = pendingUsbHomeGeneration
            pendingUsbHomeGeneration = null
            if (viewModel.connectionManager.activeUsbHomeGeneration == generation) openUsbHome()
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
            !lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) || !isDeviceUnlocked()
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
                    UsbManager.ACTION_USB_DEVICE_DETACHED -> viewModel.connectionManager.onUsbDeviceDetached(device.deviceId)
                }
            }
        }
        usbPermissionReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action != USB_PERMISSION_ACTION) return
                val device = IntentCompat.getParcelableExtra(intent, UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
                val token = intent.getLongExtra(EXTRA_USB_ATTEMPT_TOKEN, -1L)
                val resultDevice = device ?: return
                handleUsbConnectionRequest(viewModel.connectionManager.completeUsbPermissionRequest(
                    device = resultDevice,
                    token = token,
                    permissionGranted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false),
                ))
            }
        }
        ContextCompat.registerReceiver(this, usbReceiver, IntentFilter().apply {
            addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED); addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        }, ContextCompat.RECEIVER_EXPORTED)
        ContextCompat.registerReceiver(this, usbPermissionReceiver, IntentFilter(USB_PERMISSION_ACTION), ContextCompat.RECEIVER_NOT_EXPORTED)
    }

    internal fun requestUsbDeviceConnection(device: UsbDevice) {
        handleUsbConnectionRequest(viewModel.connectionManager.requestUsbConnection(device))
    }

    private fun handleUsbConnectionRequest(request: UsbConnectionRequest) {
        when (request) {
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
        val generation = viewModel.connectionManager.activeUsbHomeGeneration ?: return
        val controller = rootNavController ?: run { pendingNavigation = HOME_ROUTE; pendingUsbHomeGeneration = generation; return }
        if (!lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) || !isDeviceUnlocked() || controller.currentDestination == null) {
            pendingNavigation = HOME_ROUTE
            pendingUsbHomeGeneration = generation
            return
        }
        if (!viewModel.claimUsbHomeNavigation(generation)) return
        pendingUsbHomeGeneration = null
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
        if (!isDeviceUnlocked()) {
            viewModel.connectionManager.onAppBackgrounded()
            return
        }
        viewModel.connectionManager.onAppForegrounded()
        handleUsbConnectionRequest(viewModel.connectionManager.synchronizeUsbAttachments())
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (!isDeviceUnlocked()) {
            viewModel.connectionManager.onAppBackgrounded()
        } else if (hasFocus && lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            viewModel.connectionManager.onAppForegrounded()
            handleUsbConnectionRequest(viewModel.connectionManager.synchronizeUsbAttachments())
            processPendingNavigation()
            deliverPendingUsbIntent()
        }
    }

    private fun isDeviceUnlocked(): Boolean =
        getSystemService(KeyguardManager::class.java)?.isKeyguardLocked != true

    override fun onStop() {
        if (!isChangingConfigurations) {
            viewModel.connectionManager.onAppBackgrounded()
        }
        super.onStop()
    }

    internal fun updateActiveComposeRoute(route: String?) {
        if (route != activeComposeRoute) uiMessageQueue.onActiveScopeChanged(route)
        activeComposeRoute = route
    }

    private fun toastLength(duration: UiMessageDuration): Int = when (duration) {
        UiMessageDuration.SHORT -> Toast.LENGTH_SHORT
        UiMessageDuration.LONG -> Toast.LENGTH_LONG
    }

    private fun toastPacingMillis(duration: UiMessageDuration): Long = when (duration) {
        UiMessageDuration.SHORT -> 2_000L
        UiMessageDuration.LONG -> 3_500L
    }.let { nominalMillis ->
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return nominalMillis
        val accessibility = getSystemService(ACCESSIBILITY_SERVICE) as? AccessibilityManager
            ?: return nominalMillis
        accessibility.getRecommendedTimeoutMillis(nominalMillis.toInt(), AccessibilityManager.FLAG_CONTENT_TEXT).toLong()
    }

    private fun onTerminalSessionChanged(termination: ConnectionSessionTermination?) {
        if (termination == null) {
            val pendingGeneration = viewModel.cancelPendingTerminalStart()
            val wasQueued = pendingTerminalStartGeneration != null || pendingGeneration != null
            pendingTerminalStartGeneration = null
            if (wasQueued && pendingNavigation == START_ROUTE) pendingNavigation = null
            return
        }
        if (viewModel.connectionManager.terminalSession.value?.generation != termination.generation) return
        if (!viewModel.queueTerminalStart(termination.generation)) return
        pendingTerminalStartGeneration = termination.generation
        pendingNavigation = START_ROUTE
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) && isDeviceUnlocked()) openStart()
    }

    private fun restorePendingTerminalStart() {
        val generation = viewModel.pendingTerminalStart() ?: return
        if (viewModel.connectionManager.terminalSession.value?.generation == generation) {
            pendingTerminalStartGeneration = generation
            pendingNavigation = START_ROUTE
        } else {
            viewModel.cancelPendingTerminalStart()
            if (pendingTerminalStartGeneration == generation && pendingNavigation == START_ROUTE) pendingNavigation = null
            pendingTerminalStartGeneration = null
        }
    }

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

    private fun isInsideDiagnosticsGraph(destination: NavDestination?): Boolean {
        var current = destination
        while (current != null) {
            if (current.route == DIAGNOSTICS_GRAPH_ROUTE) return true
            current = current.parent
        }
        return false
    }

}
