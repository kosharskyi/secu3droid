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
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import org.secu3.android.R
import org.secu3.android.connection.Connected
import org.secu3.android.ui.MainActivity
import org.secu3.android.ui.compose.ConfirmationDialog
import org.secu3.android.ui.compose.SecuComposeTheme

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
    val confirmation by viewModel.confirmation.collectAsStateWithLifecycle()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
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
    SecuComposeTheme {
        ErrorsContent(
            connectionState = connection,
            snapshot = snapshot,
            onBackClick = onNavigateUp,
            onClearClick = { viewModel.requestConfirmation(ErrorsConfirmation.Clear) },
            onLiveChange = viewModel::setLiveEnabled,
            onHoldChange = viewModel::setHoldActiveErrors,
            onAutoReadChange = viewModel::setAutoReadSavedErrors,
            onReadSaved = viewModel::readSavedErrors,
            onRetryActive = viewModel::retryActiveRead,
            onEditSaved = viewModel::beginEditing,
            onDraftChange = viewModel::setDraftError,
            onSetAllDraft = viewModel::setAllDraftErrors,
            onDiscardDraft = viewModel::discardDraft,
            onWriteDraft = { viewModel.requestConfirmation(ErrorsConfirmation.Write) },
        )
        if (connection == Connected) {
            ErrorsConfirmationDialog(
                confirmation = confirmation,
                onConfirm = viewModel::confirmWrite,
                onDismiss = viewModel::dismissConfirmation,
            )
        }
    }
}

@Composable
internal fun ErrorsConfirmationDialog(
    confirmation: ErrorsConfirmation?,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val (title, message) = when (confirmation) {
        ErrorsConfirmation.Clear -> R.string.clear_all_errors to R.string.dialog_confirm_clear_errors_message
        ErrorsConfirmation.Write -> R.string.errors_write_saved to R.string.errors_confirm_write
        null -> return
    }
    ConfirmationDialog(title = title, message = message, onConfirm = onConfirm, onCancel = onDismiss)
}
