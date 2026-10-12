/*
 *    SecuDroid  - An open source, free manager for SECU-3 engine control unit
 *    Copyright (C) 2024 Vitalii O. Kosharskyi. Ukraine, Kyiv
 *
 *    SECU-3  - An open source, free engine control unit
 *    Copyright (C) 2007-2024 Alexey A. Shabelnikov. Ukraine, Kyiv
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *    contacts:
 *                    http://secu-3.org
 *                    email: vetalkosharskiy@gmail.com
 */
package org.secu3.android.ui.sensors.settings

import org.secu3.android.ui.compose.DropdownShape

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.testTag
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import org.secu3.android.R
import org.secu3.android.ui.compose.SecuComposeTheme
import org.secu3.android.ui.compose.SecuTopAppBar
import org.secu3.android.utils.UserPrefs
import javax.inject.Inject

@AndroidEntryPoint
class SensorsSettingsFragment : Fragment() {
    @Inject
    internal lateinit var mPrefs: UserPrefs

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                SecuComposeTheme {
                    SensorsSettingsScreen(
                        oldView = mPrefs.oldSensorViewEnabled,
                        columns = mPrefs.columnsCount.coerceIn(2, 8),
                        onOldViewChange = { mPrefs.oldSensorViewEnabled = it },
                        onColumnsChange = { mPrefs.columnsCount = it },
                        onNavigateUp = { findNavController().navigateUp() },
                    )
                }
            }
        }
}

@Composable
internal fun SensorsSettingsScreen(
    oldView: Boolean,
    columns: Int,
    onOldViewChange: (Boolean) -> Unit,
    onColumnsChange: (Int) -> Unit,
    onNavigateUp: () -> Unit,
) {
    var oldViewState by remember(oldView) { mutableStateOf(oldView) }
    var columnsState by remember(columns) { mutableIntStateOf(columns) }
    var columnsExpanded by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        SecuTopAppBar(title = stringResource(R.string.title_activity_settings), onNavigateUp = onNavigateUp, showStatus = false)
        Row(Modifier.fillMaxWidth().clickable {
            oldViewState = !oldViewState
            onOldViewChange(oldViewState)
        }.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.enable_old_sensors_view), Modifier.weight(1f))
            Checkbox(checked = oldViewState, modifier = Modifier.testTag("sensors-old-view-checkbox"), onCheckedChange = {
                oldViewState = it
                onOldViewChange(it)
            })
        }
        Row(Modifier.fillMaxWidth().clickable { columnsExpanded = true }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.columns_count), Modifier.weight(1f))
            Box {
                TextButton(onClick = { columnsExpanded = true }, modifier = Modifier.testTag("sensor-columns-value"), shape = DropdownShape) { Text(columnsState.toString()) }
                DropdownMenu(expanded = columnsExpanded, onDismissRequest = { columnsExpanded = false }, shape = DropdownShape) {
                    (2..8).forEach { count ->
                        DropdownMenuItem(text = { Text(count.toString()) }, onClick = {
                            columnsState = count
                            onColumnsChange(count)
                            columnsExpanded = false
                        })
                    }
                }
            }
        }
    }
}
