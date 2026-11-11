/*
 *    SecuDroid  - An open source, free manager for SECU-3 engine control unit
 *    Copyright (C) 2026 Vitalii O. Kosharskyi. Ukraine, Kyiv
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 */
package org.secu3.android.ui.ltft

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ContentResolver
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import org.secu3.android.R
import org.secu3.android.connection.Secu3ConnectionManager
import org.secu3.android.domain.ltft.LtftMapExport
import org.secu3.android.domain.ltft.LtftMapsSession
import org.secu3.android.domain.ltft.LtftVeImage
import org.secu3.android.ui.LTFT_MAPS_ROUTE
import org.secu3.android.ui.MainActivity
import org.secu3.android.ui.messages.UiMessageDuration
import org.secu3.android.ui.messages.UiMessageQueue
import java.util.Locale
import javax.inject.Inject

@Composable
internal fun LtftMapsRoute(entry: NavBackStackEntry, activity: MainActivity, onNavigateUp: () -> Unit) {
    val viewModel: LtftMapsViewModel = viewModel(
        viewModelStoreOwner = entry,
        factory = activity.defaultViewModelProviderFactory,
        extras = entry.defaultViewModelCreationExtras,
    )
    val connection by viewModel.connectionState.collectAsStateWithLifecycle(initialValue = null)
    val state by viewModel.state.collectAsStateWithLifecycle()
    val writeState by viewModel.writeState.collectAsStateWithLifecycle()
    val clipboard = LocalClipboard.current
    val clipboardScope = rememberCoroutineScope()
    val clipboardLabel = stringResource(R.string.ltft_maps_title)
    val locale = LocalConfiguration.current.locales[0]
    val resolver = activity.applicationContext.contentResolver
    val launchCsv = rememberLtftCsvLauncher(
        onDocumentCreated = { uri, csv -> viewModel.writeCsv(resolver, uri, csv) },
        onLaunchFailed = viewModel::showExportError,
    )
    val applyToVe = rememberLtftVeFileAction(viewModel.veFileWorkflow, resolver)
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

    LtftMapsScreen(
        state = state,
        connectionState = connection,
        writeState = writeState,
        onNavigateUp = onNavigateUp,
        onRefresh = viewModel::refresh,
        onReset = viewModel::resetMaps,
        onSave = viewModel::saveMaps,
        onCopy = { corrections, rows, columns ->
            val text = viewModel.formatTsv(corrections, rows, columns, locale)
            clipboardScope.launch { clipboard.setClipEntry(ClipEntry(ClipData.newPlainText(clipboardLabel, text))) }
        },
        onExportCsv = { table, corrections, rpmAxis, decimalSeparator ->
            val csv = viewModel.formatCsv(corrections, rpmAxis, decimalSeparator)
            launchCsv("LTFT-${table + 1}.csv", csv)
        },
        onApplyToVe = applyToVe,
    )
}

@Composable
internal fun rememberLtftCsvLauncher(
    onDocumentCreated: (Uri, String) -> Unit,
    onLaunchFailed: () -> Unit,
): (String, String) -> Unit {
    var pendingCsv by rememberSaveable { mutableStateOf<String?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        val csv = pendingCsv
        pendingCsv = null
        if (uri != null && csv != null) onDocumentCreated(uri, csv)
    }
    return { name, csv ->
        if (pendingCsv == null) {
            pendingCsv = csv
            try {
                launcher.launch(name)
            } catch (_: ActivityNotFoundException) {
                pendingCsv = null
                onLaunchFailed()
            }
        }
    }
}

@HiltViewModel
internal class LtftMapsViewModel @Inject constructor(
    connectionManager: Secu3ConnectionManager,
    private val session: LtftMapsSession,
    private val export: LtftMapExport,
    veImage: LtftVeImage,
    private val uiMessageQueue: UiMessageQueue,
) : ViewModel() {
    val connectionState = connectionManager.connectionStateFlow
    val state = session.state
    val writeState = session.writeState
    val veFileWorkflow = LtftVeFileWorkflow(veImage, viewModelScope, ::showFileMessage)

    init {
        session.start(viewModelScope)
    }

    fun onRouteResumed() = session.onRouteResumed()
    fun onRoutePaused() = session.onRoutePaused()
    fun refresh() = session.refresh()
    fun resetMaps() = session.resetMaps()
    fun saveMaps() = session.saveMaps()

    fun formatTsv(corrections: List<Float>, rows: IntRange, columns: IntRange, locale: Locale): String =
        export.tsv(corrections, rows, columns, locale)

    fun formatCsv(corrections: List<Float>, rpmAxis: List<Int>, decimalSeparator: Char): String =
        export.csv(corrections, rpmAxis, decimalSeparator)

    fun writeCsv(resolver: ContentResolver, uri: Uri, csv: String) {
        viewModelScope.launch {
            if (!export.writeCsv(resolver, uri, csv)) showExportError()
        }
    }

    fun showExportError() {
        showFileMessage(R.string.ltft_maps_export_error)
    }

    private fun showFileMessage(text: Int) {
        uiMessageQueue.publish(text, UiMessageDuration.LONG, scope = LTFT_MAPS_ROUTE)
    }

    override fun onCleared() = session.close()
}
