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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.saveable.mapSaver
import androidx.compose.runtime.saveable.rememberSaveable
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
import org.secu3.android.ui.messages.UiMessageDuration
import org.secu3.android.ui.DASHBOARD_ROUTE

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
    val previousWindowState = rememberSaveable(entry.id, saver = DashboardWindowState.Saver) {
        DashboardWindowState.capture(activity.window)
    }

    LaunchedEffect(lifecycle, viewModel, activity) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.setTask(Task.Secu3ReadSensors)
            previousWindowState.enterDashboard(activity.window)
            try {
                awaitCancellation()
            } finally {
                previousWindowState.restore(activity.window, viewModel.isKeepScreenAliveActive)
            }
        }
    }
    LaunchedEffect(entry, activity) {
        if (viewModel.isBluetoothDeviceAddressNotSelected()) {
            activity.uiMessageQueue.publish(R.string.choose_bluetooth_adapter, UiMessageDuration.LONG, scope = DASHBOARD_ROUTE)
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
    val systemBarsBehavior: Int,
    val visibleSystemBarsMask: Int,
) {
    fun enterDashboard(window: Window) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.systemBars())
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    fun restore(window: Window, keepScreenOnPreference: Boolean) {
        WindowCompat.setDecorFitsSystemWindows(window, true)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            systemBarsBehavior = this@DashboardWindowState.systemBarsBehavior
            show(visibleSystemBarsMask)
            hide(WindowInsetsCompat.Type.systemBars() and visibleSystemBarsMask.inv())
        }
        if (keepScreenOnPreference) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        else window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    companion object {
        val Saver = mapSaver(
            save = { state ->
                mapOf(
                    "systemBarsBehavior" to state.systemBarsBehavior,
                    "visibleSystemBarsMask" to state.visibleSystemBarsMask,
                )
            },
            restore = { saved ->
                DashboardWindowState(
                    systemBarsBehavior = saved.getValue("systemBarsBehavior") as Int,
                    visibleSystemBarsMask = saved.getValue("visibleSystemBarsMask") as Int,
                )
            },
        )

        fun capture(window: Window): DashboardWindowState {
            val rootInsets = ViewCompat.getRootWindowInsets(window.decorView)
            val systemBarTypes = listOf(
                WindowInsetsCompat.Type.statusBars(),
                WindowInsetsCompat.Type.navigationBars(),
                WindowInsetsCompat.Type.captionBar(),
                WindowInsetsCompat.Type.systemOverlays(),
            )
            return DashboardWindowState(
                systemBarsBehavior = WindowInsetsControllerCompat(window, window.decorView).systemBarsBehavior,
                visibleSystemBarsMask = systemBarTypes.fold(0) { mask, type ->
                    if (rootInsets?.isVisible(type) != false) mask or type else mask
                },
            )
        }
    }
}
