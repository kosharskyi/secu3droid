/*
 *    SecuDroid  - An open source, free manager for SECU-3 engine control unit
 *    Copyright (C) 2026 Vitalii O. Kosharskyi. Ukraine, Kyiv
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 */
package org.secu3.android.ui.settings

import android.app.Activity
import android.content.Intent
import androidx.core.net.toUri
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.secu3.android.ui.MainActivity
import org.secu3.android.ui.compose.SecuComposeTheme

@Composable
internal fun SettingsRoute(
    entry: NavBackStackEntry,
    activity: MainActivity,
    onNavigateUp: () -> Unit,
) {
    val viewModel: SettingsViewModel = viewModel(
        viewModelStoreOwner = entry,
        factory = activity.defaultViewModelProviderFactory,
        extras = entry.defaultViewModelCreationExtras,
    )
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var state by remember(viewModel) { mutableStateOf(viewModel.state.value) }
    var dialog by remember(viewModel) { mutableStateOf(viewModel.dialog.value) }
    val directoryPicker = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        consumeFolderPickerResult(
            resultCode = result.resultCode,
            data = result.data,
            takePermission = activity.contentResolver::takePersistableUriPermission,
            save = viewModel::saveExportFolder,
        )
    }
    LaunchedEffect(lifecycle, viewModel) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            launch { viewModel.state.collect { state = it } }
            launch { viewModel.dialog.collect { dialog = it } }
        }
    }

    SecuComposeTheme {
        SettingsContent(
            state = state,
            dialog = dialog,
            onNavigateUp = onNavigateUp,
            onToggle = viewModel::setBooleanPreference,
            onSelectDelimiter = viewModel::setDelimiter,
            onChooseFolder = {
                directoryPicker.launch(folderPickerIntent())
            },
            onEditRetries = { viewModel.showDialog(SettingsDialog.Retries) },
            onSaveRetries = viewModel::saveRetries,
            onSelectDialog = viewModel::showDialog,
            onDismissDialog = viewModel::dismissDialog,
            onShowAbout = { viewModel.showDialog(SettingsDialog.About) },
        )
        if (dialog == SettingsDialog.About) {
            Dialog(onDismissRequest = viewModel::dismissDialog) {
                Surface(Modifier.fillMaxWidth().widthIn(max = 560.dp).testTag("settings-about-dialog")) {
                    AboutContent(open = { url -> activity.startActivity(Intent(Intent.ACTION_VIEW, url.toUri())) })
                }
            }
        }
    }
}

internal fun folderPickerIntent(): Intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
    addFlags(
        Intent.FLAG_GRANT_READ_URI_PERMISSION or
            Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
            Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or
            Intent.FLAG_GRANT_PREFIX_URI_PERMISSION,
    )
}

internal fun consumeFolderPickerResult(
    resultCode: Int,
    data: Intent?,
    takePermission: (Uri, Int) -> Unit,
    save: (String) -> Unit,
) {
    if (resultCode != Activity.RESULT_OK) return
    val uri = data?.data ?: return
    val grantedFlags = data.flags and (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
    if (grantedFlags != 0 && runCatching { takePermission(uri, grantedFlags) }.isFailure) return
    save(uri.toString())
}
