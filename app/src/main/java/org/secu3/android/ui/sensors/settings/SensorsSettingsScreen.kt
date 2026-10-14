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
    Column(Modifier.fillMaxSize()) {
        SecuTopAppBar(title = stringResource(R.string.title_activity_settings), onNavigateUp = onNavigateUp, showStatus = false)
        Row(Modifier.fillMaxWidth().clickable { onOldViewChange(!oldView) }.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.enable_old_sensors_view), Modifier.weight(1f))
            Checkbox(checked = oldView, modifier = Modifier.testTag("sensors-old-view-checkbox"), onCheckedChange = onOldViewChange)
        }
        Row(Modifier.fillMaxWidth().clickable { columnsExpanded = true }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.columns_count), Modifier.weight(1f))
            Box {
                TextButton(onClick = { columnsExpanded = true }, modifier = Modifier.testTag("sensor-columns-value"), shape = DropdownShape) { Text(columns.toString()) }
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
