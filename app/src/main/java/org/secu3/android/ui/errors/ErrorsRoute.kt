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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import org.secu3.android.R
import org.secu3.android.ui.MainActivity
import org.secu3.android.ui.compose.ConfirmationDialog
import org.secu3.android.ui.compose.SecuComposeTheme
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
    val connection by viewModel.connectionState.collectAsStateWithLifecycle(initialValue = null)
    val snapshot by viewModel.errorsSnapshot.collectAsStateWithLifecycle()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var confirmClear by remember(viewModel) { mutableStateOf(viewModel.showClearConfirmation.value) }
    LaunchedEffect(lifecycle, viewModel) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            launch { viewModel.showClearConfirmation.collect { confirmClear = it } }
        }
    }
    DisposableEffect(lifecycle, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> viewModel.onRouteResumed()
                Lifecycle.Event.ON_PAUSE -> viewModel.onRoutePaused()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) viewModel.onRouteResumed()
        onDispose {
            viewModel.onRoutePaused()
            lifecycle.removeObserver(observer)
        }
    }
    val titles = androidx.compose.ui.res.stringArrayResource(R.array.errors_ecu_errors_names)

    SecuComposeTheme {
        ErrorsContent(
            connectionState = connection,
            snapshot = snapshot,
            errorTitles = titles.toList(),
            canClearErrors = snapshot.saved != null &&
                snapshot.clearStatus != ErrorsClearStatus.Clearing &&
                connection == org.secu3.android.connection.Connected,
            onBackClick = onNavigateUp,
            onClearClick = viewModel::requestClearConfirmation,
            onRetryClick = viewModel::retryRead,
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
