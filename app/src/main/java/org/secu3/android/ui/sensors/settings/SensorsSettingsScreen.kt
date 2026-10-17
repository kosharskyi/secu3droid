/*
 *    SecuDroid  - An open source, free manager for SECU-3 engine control unit
 *    Copyright (C) 2026 Vitalii O. Kosharskyi. Ukraine, Kyiv
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 */
package org.secu3.android.ui.sensors.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.secu3.android.R
import org.secu3.android.ui.compose.DropdownShape
import org.secu3.android.ui.compose.SettingsActionRow
import org.secu3.android.ui.compose.SettingsSection
import org.secu3.android.ui.compose.SettingsToggleRow
import org.secu3.android.ui.compose.SecuTopAppBar

@Composable
internal fun SensorsSettingsScreen(
    oldView: Boolean,
    columns: Int,
    onOldViewChange: (Boolean) -> Unit,
    onColumnsChange: (Int) -> Unit,
    onNavigateUp: () -> Unit,
) {
    var columnsExpanded by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        SecuTopAppBar(title = stringResource(R.string.title_activity_settings), onNavigateUp = onNavigateUp, showStatus = false)
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SettingsSection {
                SettingsToggleRow(
                    title = stringResource(R.string.enable_old_sensors_view),
                    checked = oldView,
                    testTag = "sensors-old-view-checkbox",
                    onCheckedChange = onOldViewChange,
                )
            }
            SettingsSection {
                Box(Modifier.fillMaxWidth()) {
                    SettingsActionRow(
                        title = stringResource(R.string.columns_count),
                        onClick = { columnsExpanded = true },
                        testTag = "sensor-columns",
                        value = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(columns.toString(), Modifier.testTag("sensor-columns-value"), style = MaterialTheme.typography.bodyLarge)
                                Icon(Icons.Default.ExpandMore, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        },
                    )
                DropdownMenu(expanded = columnsExpanded, onDismissRequest = { columnsExpanded = false }, shape = DropdownShape) {
                    (2..8).forEach { count ->
                        DropdownMenuItem(text = { Text(count.toString()) }, onClick = {
                            onColumnsChange(count)
                            columnsExpanded = false
                        })
                    }
                }
                }
            }
        }
    }
}
