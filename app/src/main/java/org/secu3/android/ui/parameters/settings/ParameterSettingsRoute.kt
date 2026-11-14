/*
 *    SecuDroid  - An open source, free manager for SECU-3 engine control unit
 *    Copyright (C) 2026 Vitalii O. Kosharskyi. Ukraine, Kyiv
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 */

package org.secu3.android.ui.parameters.settings

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import org.secu3.android.ui.MainActivity

@Composable
internal fun ParameterSettingsRoute(
    entry: NavBackStackEntry,
    activity: MainActivity,
    onNavigateUp: () -> Unit,
) {
    val viewModel: ParameterSettingsViewModel = viewModel(
        viewModelStoreOwner = entry,
        factory = activity.defaultViewModelProviderFactory,
        extras = entry.defaultViewModelCreationExtras,
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.saved) { if (state.saved) onNavigateUp() }
    ParameterSettingsScreen(
        densities = state.densities,
        canSave = state.densities != null && !state.saving && !state.saved,
        storageError = state.loadFailed || state.saveFailed,
        parseDensity = viewModel::parseDensity,
        onSave = viewModel::save,
        onNavigateUp = onNavigateUp,
    )
}
