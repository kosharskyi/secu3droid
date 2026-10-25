package org.secu3.android.ui.home

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import org.secu3.android.R
import androidx.compose.ui.res.stringResource
import org.secu3.android.ui.MainActivity
import org.secu3.android.ui.HOME_ROUTE
import org.secu3.android.ui.messages.UiMessageDuration

@Composable
internal fun HomeRoute(
    entry: NavBackStackEntry, activity: MainActivity, activeRoute: String?,
    onStart: () -> Boolean, onDashboard: () -> Unit, onSensors: () -> Unit,
    onParameters: () -> Unit, onErrors: () -> Unit, onLogs: () -> Unit,
    onSettings: () -> Unit, onFirmware: () -> Unit, onDiagnostics: () -> Unit,
) {
    val vm: HomeViewModel = viewModel(viewModelStoreOwner = entry, factory = activity.defaultViewModelProviderFactory, extras = entry.defaultViewModelCreationExtras)
    val ui by vm.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var resumed by remember { mutableStateOf(lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) }
    val releasePermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        vm.completeReleasePermission(granted)
        vm.dismissDialog()
    }
    DisposableEffect(lifecycle, vm) {
        val observer = LifecycleEventObserver { _, event ->
            resumed = lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
            if (event == Lifecycle.Event.ON_RESUME) {
                activity.applyKeepScreenOnPreference()
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    BackHandler(enabled = resumed) {
        if (!ui.usbSessionLocked) {
            if (vm.isConnectionRunning) vm.requestExit() else vm.confirmExit()
        }
    }
    LaunchedEffect(vm, resumed, activeRoute) {
        if (!resumed) return@LaunchedEffect
        vm.returnToStart.collect { shouldReturn ->
            if (shouldReturn && onStart()) vm.consumeReturnToStart()
        }
    }
    HomeScreen(
        state = HomeScreenState(
            connectionStatus = ui.connectionStatus,
            firmwareAvailable = ui.firmwareAvailable,
            connectionRunning = vm.isConnectionRunning,
            usbSessionLocked = ui.usbSessionLocked,
            dialog = ui.dialog,
            releaseName = ui.releaseName,
            releaseMessage = ui.releaseName?.let { stringResource(R.string.the_version_is_available_do_you_want_to_download, it) },
        ),
        actions = HomeScreenActions(
            onDashboard = onDashboard, onSensors = onSensors, onParameters = onParameters,
            onErrors = onErrors, onLogs = onLogs, onSettings = onSettings, onFirmware = onFirmware,
            onExitImmediately = vm::confirmExit, onExitRequested = vm::requestExit,
            onExitCancelled = vm::dismissDialog, onExitConfirmed = vm::confirmExit,
            onDiagnosticsRequested = vm::requestDiagnostics,
            onDialogDismissed = vm::dismissDialog,
            onPermissionRationaleConfirmed = {
                vm.dismissDialog()
                releasePermission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            },
            onPermissionRationaleCancelled = { vm.cancelReleasePermission(); vm.dismissDialog() },
            onDiagnosticsConfirmed = {
                vm.dismissDialog()
                if (vm.firmware?.isDiagnosticsEnabled == true) onDiagnostics()
                else activity.uiMessageQueue.publish(R.string.diagnostics_not_supported_title, UiMessageDuration.LONG, scope = HOME_ROUTE)
            },
            onDownloadRelease = {
                val item = vm.pendingRelease() ?: return@HomeScreenActions
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q && ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    vm.beginReleasePermission(item)
                    if (activity.shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) vm.requestReleasePermissionRationale()
                    else releasePermission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                } else vm.downloadRelease(item)
            },
            onReleaseNotes = { vm.pendingRelease()?.let { CustomTabsIntent.Builder().build().launchUrl(context, it.htmlUrl.toUri()) } },
        ),
    )
}
