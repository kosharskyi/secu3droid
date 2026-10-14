/*
 *    SecuDroid  - An open source, free manager for SECU-3 engine control unit
 *    Copyright (C) 2026 Vitalii O. Kosharskyi. Ukraine, Kyiv
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 */
package org.secu3.android.ui.errors

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.graphics.toArgb
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import org.secu3.android.R
import org.secu3.android.ui.MainActivity
import org.secu3.android.ui.compose.ConfirmationDialog
import org.secu3.android.ui.compose.SecuComposeTheme
import org.secu3.android.models.CheckEngineError
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect

@Composable
internal fun ErrorsRoute(
    entry: NavBackStackEntry,
    activity: MainActivity,
    onNavigateUp: () -> Unit,
) {
    val viewModel: ErrorsViewModel = viewModel(
        viewModelStoreOwner = entry,
        factory = activity.defaultViewModelProviderFactory,
        extras = entry.defaultViewModelCreationExtras,
    )
    val connection by remember(viewModel) { viewModel.connectionStatusLiveData }.observeAsState()
    val savedPacket by remember(viewModel) { viewModel.checkEngineSavedLiveData }.observeAsState()
    val activePacket by remember(viewModel) { viewModel.checkEngineLiveData }.observeAsState()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var confirmClear by remember(viewModel) { mutableStateOf(viewModel.showClearConfirmation.value) }
    LaunchedEffect(lifecycle, viewModel) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            launch { viewModel.showClearConfirmation.collect { confirmClear = it } }
        }
    }
    val titles = androidx.compose.ui.res.stringArrayResource(R.array.errors_ecu_errors_names)
    val errors = titles.mapIndexed { index, title ->
        CheckEngineError(
            title = title,
            isActive = activePacket?.isError(index) ?: false,
            isSaved = savedPacket?.isError(index) ?: false,
        )
    }

    SecuComposeTheme {
        ErrorsSystemBarsEffect(activity)
        ErrorsScreen(
            connectionState = connection,
            errors = errors,
            isLoading = savedPacket == null && activePacket == null,
            onBackClick = onNavigateUp,
            onClearClick = viewModel::requestClearConfirmation,
        )
        if (confirmClear) {
            ConfirmationDialog(
                title = R.string.clear_all_errors,
                message = R.string.dialog_confirm_clear_errors_message,
                onConfirm = viewModel::confirmClearErrors,
                onCancel = viewModel::dismissClearConfirmation,
            )
        }
    }
}

@Composable
private fun ErrorsSystemBarsEffect(activity: MainActivity) {
    val view = LocalView.current
    val window = activity.window
    DisposableEffect(window, view) {
        val insets = WindowInsetsControllerCompat(window, window.decorView)
        val previousStatusBarColor = window.statusBarColor
        val previousNavigationBarColor = window.navigationBarColor
        val previousLightStatusBars = insets.isAppearanceLightStatusBars
        val previousLightNavigationBars = insets.isAppearanceLightNavigationBars
        window.statusBarColor = ErrorsBackground.toArgb()
        window.navigationBarColor = ErrorsBackground.toArgb()
        insets.isAppearanceLightStatusBars = false
        insets.isAppearanceLightNavigationBars = false
        onDispose {
            window.statusBarColor = previousStatusBarColor
            window.navigationBarColor = previousNavigationBarColor
            insets.isAppearanceLightStatusBars = previousLightStatusBars
            insets.isAppearanceLightNavigationBars = previousLightNavigationBars
        }
    }
}
