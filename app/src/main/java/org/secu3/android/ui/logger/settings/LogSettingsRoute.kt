/*
 *    SecuDroid  - An open source, free manager for SECU-3 engine control unit
 *    Copyright (C) 2026 Vitalii O. Kosharskyi. Ukraine, Kyiv
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 */
package org.secu3.android.ui.logger.settings

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import org.secu3.android.ui.MainActivity

@Composable
internal fun LogSettingsRoute(
    entry: NavBackStackEntry,
    activity: MainActivity,
    onNavigateUp: () -> Unit,
) {
    val viewModel: LogSettingsViewModel = viewModel(
        viewModelStoreOwner = entry,
        factory = activity.defaultViewModelProviderFactory,
        extras = entry.defaultViewModelCreationExtras,
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val showDelimiter by viewModel.showDelimiter.collectAsStateWithLifecycle()
    val directoryPicker = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val data = result.data
        if (result.resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri -> viewModel.saveExportFolder(uri, data.flags) }
        }
    }

    LogSettingsScreen(
        state = state,
        showDelimiter = showDelimiter,
        onNavigateUp = onNavigateUp,
        onToggle = viewModel::setBooleanPreference,
        onOpenDelimiter = viewModel::openDelimiter,
        onSelectDelimiter = viewModel::setDelimiter,
        onDismissDelimiter = viewModel::dismissDelimiter,
        onChooseFolder = {
            directoryPicker.launch(Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
                addFlags(
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                        Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or
                        Intent.FLAG_GRANT_PREFIX_URI_PERMISSION,
                )
            })
        },
    )
}
