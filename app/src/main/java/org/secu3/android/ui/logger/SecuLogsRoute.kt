/*
 *    SecuDroid  - An open source, free manager for SECU-3 engine control unit
 *    Copyright (C) 2026 Vitalii O. Kosharskyi. Ukraine, Kyiv
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 */
package org.secu3.android.ui.logger

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.secu3.android.R
import org.secu3.android.ui.MainActivity
import org.secu3.android.ui.compose.SecuComposeTheme
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import java.io.IOException
import java.io.File

@Composable
internal fun SecuLogsRoute(
    entry: NavBackStackEntry,
    activity: MainActivity,
    onNavigateUp: () -> Unit,
) {
    val viewModel: SecuLogsViewModel = viewModel(
        viewModelStoreOwner = entry,
        factory = activity.defaultViewModelProviderFactory,
        extras = entry.defaultViewModelCreationExtras,
    )
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var state by remember(viewModel) { mutableStateOf(viewModel.state.value) }
    var pendingDeletePath by remember(viewModel) { mutableStateOf(viewModel.pendingDeletePath.value) }
    val permissionRequest = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        viewModel.onDownloadPermissionResult(granted)
    }
    LaunchedEffect(lifecycle, viewModel, snackbarHostState) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            launch { viewModel.state.collect { state = it } }
            launch { viewModel.pendingDeletePath.collect { pendingDeletePath = it } }
            launch {
                viewModel.effects.collect { effect ->
                    val message = when (effect) {
                        SecuLogsEffect.PermissionDenied -> activity.getString(R.string.log_export_permission_denied)
                        SecuLogsEffect.ActiveLogDeleteRejected -> activity.getString(R.string.log_delete_active_rejected)
                        SecuLogsEffect.DeleteFailed -> activity.getString(R.string.log_delete_failed)
                        is SecuLogsEffect.ExportSucceeded -> activity.getString(R.string.log_export_success, effect.destination)
                        SecuLogsEffect.ExportFailed -> activity.getString(R.string.log_export_failed)
                    }
                    snackbarHostState.showSnackbar(message)
                }
            }
        }
    }
    val pendingDeleteFile = pendingDeletePath?.let(::File)

    SecuComposeTheme {
        SecuLogsScreen(
            files = state.files,
            snackbarHostState = snackbarHostState,
            onBackClick = onNavigateUp,
            onShareClick = { file -> activity.startActivity(Intent.createChooser(viewModel.getShareIntent(file), null)) },
            onDownloadClick = { file ->
                coroutineScope.launch {
                    try {
                        if (viewModel.requiresDefaultDownloadsPermission() &&
                            ContextCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                        ) {
                            viewModel.requestDownloadPermission(file)
                            permissionRequest.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        } else {
                            viewModel.exportFile(file)
                        }
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (failure: IOException) {
                        activity.uiMessageQueue.publish(R.string.preferences_storage_error, org.secu3.android.ui.messages.UiMessageDuration.LONG, scope = "logs")
                    }
                }
            },
            pendingDeleteFile = pendingDeleteFile,
            onDeleteRequest = viewModel::requestDelete,
            onDeleteCancel = viewModel::cancelDelete,
            onDeleteConfirm = viewModel::confirmDelete,
        )
    }
}
