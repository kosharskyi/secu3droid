package org.secu3.android.ui

import android.content.Intent
import android.hardware.usb.UsbDevice
import android.os.Bundle
import android.view.WindowManager
import android.graphics.drawable.ColorDrawable
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.core.content.IntentCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavDestination
import androidx.navigation.NavHostController
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: MainActivityViewModel by viewModels()
    internal val userPrefs get() = viewModel.prefs
    private var rootNavController: NavHostController? = null
    internal val rootNavigationController: NavHostController? get() = rootNavController
    private var pendingNavigation: String? = null
    private var pendingUsbIntentDevice: UsbDevice? = null
    private var startUsbIntentHandler: ((UsbDevice?) -> Unit)? = null
    private var activeComposeRoute: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) handleUsbIntent(intent)
        setContent { RootNavigation(this) }
    }

    override fun onPostResume() {
        super.onPostResume()
        processPendingNavigation()
        deliverPendingUsbIntent()
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
            pendingUsbIntentDevice = IntentCompat.getParcelableExtra(intent, android.hardware.usb.UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
            deliverPendingUsbIntent()
        }
    }

    override fun onResume() {
        super.onResume()
        if (activeComposeRoute == DASHBOARD_ROUTE) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        else applyKeepScreenOnPreference()
        processPendingNavigation()
        deliverPendingUsbIntent()
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
        if (viewModel.prefs.isWakeLockEnabled) {
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

    private companion object {
        const val ACTION_USB_DEVICE_ATTACHED = "android.hardware.usb.action.USB_DEVICE_ATTACHED"
    }
}
