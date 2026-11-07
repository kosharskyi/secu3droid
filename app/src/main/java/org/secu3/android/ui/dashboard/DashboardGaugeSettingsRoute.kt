/*
 *SecuDroid  - An open source, free manager for SECU-3 engine control unit
 *Copyright (C) 2026 Vitalii O. Kosharskyi. Ukraine, Kyiv
 *
 *SECU-3  - An open source, free engine control unit
 *Copyright (C) 2007-2026 Alexey A. Shabelnikov. Ukraine, Kyiv
 *
 *This program is free software: you can redistribute it and/or modify
 *it under the terms of the GNU General Public License as published by
 *the Free Software Foundation, either version 3 of the License, or
 *(at your option) any later version.
 *
 *This program is distributed in the hope that it will be useful,
 *but WITHOUT ANY WARRANTY; without even the implied warranty of
 *MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *GNU General Public License for more details.
 *
 *You should have received a copy of the GNU General Public License
 *along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *contacts:
 *                http://secu-3.org
 *                email: vetalkosharskiy@gmail.com
 */

package org.secu3.android.ui.dashboard

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import org.secu3.android.ui.MainActivity

@Composable
internal fun DashboardGaugeSettingsRoute(
    entry: NavBackStackEntry,
    activity: MainActivity,
    onNavigateUp: () -> Unit,
) {
    val viewModel: DashboardGaugeSettingsViewModel = viewModel(
        viewModelStoreOwner = entry,
        factory = activity.defaultViewModelProviderFactory,
        extras = entry.defaultViewModelCreationExtras,
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state.saveStatus, entry) {
        if (state.saveStatus == DashboardGaugeSaveStatus.Saved) onNavigateUp()
    }
    DashboardGaugeSettingsScreen(
        state = state,
        presentationCatalog = viewModel.presentationCatalog,
        onSave = viewModel::saveGaugeSettings,
        onNavigateUp = onNavigateUp,
    )
}