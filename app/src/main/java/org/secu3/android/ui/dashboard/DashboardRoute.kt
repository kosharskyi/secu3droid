/*
 *    SecuDroid  - An open source, free manager for SECU-3 engine control unit
 *    Copyright (C) 2026 Vitalii O. Kosharskyi. Ukraine, Kyiv
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 */
package org.secu3.android.ui.dashboard

import android.content.pm.ActivityInfo
import android.view.Window
import android.view.WindowManager
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import kotlinx.coroutines.awaitCancellation
import org.secu3.android.R
import org.secu3.android.ui.MainActivity
import org.secu3.android.ui.compose.SecuComposeTheme
import org.secu3.android.utils.Task

@Composable
internal fun DashboardRoute(entry: NavBackStackEntry, activity: MainActivity) {
    val viewModel: DashboardViewModel = viewModel(
        viewModelStoreOwner = entry,
        factory = activity.defaultViewModelProviderFactory,
        extras = entry.defaultViewModelCreationExtras,
    )
    val packet by viewModel.packetLiveData.observeAsState()
    val connection by viewModel.statusLiveData.observeAsState()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val previousWindowState = remember(activity) { DashboardWindowState.capture(activity.window) }

    LaunchedEffect(lifecycle, viewModel, activity) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.setTask(Task.Secu3ReadSensors)
            previousWindowState.enterDashboard()
            try {
                awaitCancellation()
            } finally {
                previousWindowState.restore(viewModel.isKeepScreenAliveActive)
            }
        }
    }
    LaunchedEffect(entry, activity) {
        if (viewModel.isBluetoothDeviceAddressNotSelected()) {
            Toast.makeText(activity, activity.getString(R.string.choose_bluetooth_adapter), Toast.LENGTH_LONG).show()
        }
    }
    DisposableEffect(activity) {
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        onDispose {
            if (!activity.isChangingConfigurations) {
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }
        }
    }

    SecuComposeTheme {
        DashboardContent(data = packet, connectionState = connection)
    }
}

private data class DashboardWindowState(
    val window: Window,
    val statusBarColor: Int,
    val navigationBarColor: Int,
    val lightStatusBars: Boolean,
    val lightNavigationBars: Boolean,
    val systemBarsBehavior: Int,
    val statusBarsVisible: Boolean,
    val navigationBarsVisible: Boolean,
) {
    fun enterDashboard() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.systemBars())
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    fun restore(keepScreenOnPreference: Boolean) {
        window.statusBarColor = statusBarColor
        window.navigationBarColor = navigationBarColor
        WindowCompat.setDecorFitsSystemWindows(window, true)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = lightStatusBars
            isAppearanceLightNavigationBars = lightNavigationBars
            systemBarsBehavior = this@DashboardWindowState.systemBarsBehavior
            if (statusBarsVisible) show(WindowInsetsCompat.Type.statusBars())
            else hide(WindowInsetsCompat.Type.statusBars())
            if (navigationBarsVisible) show(WindowInsetsCompat.Type.navigationBars())
            else hide(WindowInsetsCompat.Type.navigationBars())
        }
        if (keepScreenOnPreference) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        else window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    companion object {
        fun capture(window: Window): DashboardWindowState {
            val insets = WindowInsetsControllerCompat(window, window.decorView)
            val rootInsets = ViewCompat.getRootWindowInsets(window.decorView)
            return DashboardWindowState(
                window = window,
                statusBarColor = window.statusBarColor,
                navigationBarColor = window.navigationBarColor,
                lightStatusBars = insets.isAppearanceLightStatusBars,
                lightNavigationBars = insets.isAppearanceLightNavigationBars,
                systemBarsBehavior = insets.systemBarsBehavior,
                statusBarsVisible = rootInsets?.isVisible(WindowInsetsCompat.Type.statusBars()) ?: true,
                navigationBarsVisible = rootInsets?.isVisible(WindowInsetsCompat.Type.navigationBars()) ?: true,
            )
        }
    }
}
