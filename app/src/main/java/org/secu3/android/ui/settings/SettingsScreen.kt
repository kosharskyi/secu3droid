/*
 *    SecuDroid  - An open source, free manager for SECU-3 engine control unit
 *    Copyright (C) 2024 Vitalii O. Kosharskyi. Ukraine, Kyiv
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 */
package org.secu3.android.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.secu3.android.R
import org.secu3.android.ui.compose.SecuTopAppBar

data class SettingsUiState(
    val writeLog: Boolean,
    val writeCsvHeader: Boolean,
    val csvDelimiter: String,
    val writeBinaryLog: Boolean,
    val exportFolder: String,
    val connectionRetries: String,
    val keepScreenOn: Boolean,
    val wakeLock: Boolean,
)

/** Returns a positive Int value that remains safe for UserPrefs.connectionRetries.toInt(). */
internal fun parsePositiveRetries(value: String): Int? = value.toIntOrNull()?.takeIf { it > 0 }


@Composable
internal fun SettingsContent(
    state: SettingsUiState,
    dialog: SettingsDialog?,
    onNavigateUp: () -> Unit,
    onToggle: (Int, Boolean) -> Unit,
    onSelectDelimiter: (String) -> Unit,
    onChooseFolder: () -> Unit,
    onEditRetries: () -> Unit,
    onSaveRetries: (String) -> Unit,
    onSelectDialog: (SettingsDialog?) -> Unit,
    onDismissDialog: () -> Unit,
    onShowAbout: () -> Unit,
) {
    val delimiterValues = stringArrayResource(R.array.csv_delimeters_values)
    val delimiterLabels = stringArrayResource(R.array.csv_delimeters)
    val delimiterIndex = delimiterValues.indexOf(state.csvDelimiter).takeIf { it >= 0 } ?: 1
    Scaffold(topBar = {
        SecuTopAppBar(
            title = stringResource(R.string.title_activity_settings),
            onNavigateUp = onNavigateUp,
            showStatus = false,
        )
    }) { contentPadding ->
        Column(
            Modifier.fillMaxSize().padding(contentPadding).verticalScroll(rememberScrollState()).padding(bottom = 24.dp),
        ) {
            BooleanSetting(R.string.pref_write_log_title, R.string.pref_write_log_key, R.string.pref_write_log_summary, state.writeLog) {
                onToggle(R.string.pref_write_log_key, it)
            }
            BooleanSetting(R.string.pref_log_csv_write_title, R.string.pref_log_csv_write_title_key, null, state.writeCsvHeader) {
                onToggle(R.string.pref_log_csv_write_title_key, it)
            }
            SettingRow(stringResource(R.string.pref_log_csv_delimeter_title), delimiterLabels[delimiterIndex]) {
                onSelectDialog(SettingsDialog.Delimiter)
            }
            BooleanSetting(R.string.pref_write_binary_log_title, R.string.pref_write_binary_log_key, null, state.writeBinaryLog) {
                onToggle(R.string.pref_write_binary_log_key, it)
            }
            SettingRow(stringResource(R.string.pref_log_export_directory_title), state.exportFolder, onChooseFolder)
            SettingRow(
                stringResource(R.string.pref_connection_retries_title),
                stringResource(R.string.pref_connection_retries_summary, state.connectionRetries),
                onEditRetries,
            )
            BooleanSetting(R.string.pref_keep_screen_title, R.string.pref_keep_screen_key, R.string.pref_keep_screen_summary, state.keepScreenOn) {
                onToggle(R.string.pref_keep_screen_key, it)
            }
            BooleanSetting(R.string.pref_wakelock_title, R.string.pref_wakelock_key, R.string.pref_wakelock_summary, state.wakeLock) {
                onToggle(R.string.pref_wakelock_key, it)
            }
            SettingRow(stringResource(R.string.pref_about_title), stringResource(R.string.pref_about_summary), onShowAbout)
        }
    }

    when (dialog) {
        SettingsDialog.Delimiter -> DelimiterDialog(
            selected = state.csvDelimiter,
            labels = delimiterLabels.toList(),
            values = delimiterValues.toList(),
            onSelect = onSelectDelimiter,
            onDismiss = onDismissDialog,
        )
        SettingsDialog.Retries -> RetryDialog(
            initialValue = state.connectionRetries,
            onConfirm = onSaveRetries,
            onDismiss = onDismissDialog,
        )
        SettingsDialog.About -> Unit
        null -> Unit
    }
}

@Composable
private fun BooleanSetting(titleId: Int, keyId: Int, summaryId: Int?, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable { onCheckedChange(!checked) }.padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(titleId))
            if (summaryId != null) Text(stringResource(summaryId), style = MaterialTheme.typography.bodySmall)
        }
        Checkbox(checked = checked, onCheckedChange = onCheckedChange, modifier = Modifier.testTag("settings-toggle-$keyId"))
    }
}

@Composable
private fun SettingRow(title: String, summary: String, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text(title)
        if (summary.isNotEmpty()) Text(summary, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun DelimiterDialog(
    selected: String,
    labels: List<String>,
    values: List<String>,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.pref_log_csv_delimeter_title)) },
        text = {
            Column {
                labels.forEachIndexed { index, label ->
                    Row(
                        Modifier.fillMaxWidth().clickable { onSelect(values[index]) }.padding(8.dp),
                        horizontalArrangement = Arrangement.Start,
                    ) {
                        RadioButton(selected = selected == values[index], onClick = { onSelect(values[index]) })
                        Text(label, Modifier.padding(start = 8.dp))
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.ok)) } },
    )
}

@Composable
private fun RetryDialog(initialValue: String, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var text by androidx.compose.runtime.remember(initialValue) { mutableStateOf(initialValue) }
    val parsedValue = parsePositiveRetries(text)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.pref_connection_retries_title)) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { value -> text = value.filter(Char::isDigit) },
                singleLine = true,
                isError = parsedValue == null,
            )
        },
        confirmButton = {
            TextButton(onClick = { parsedValue?.let { onConfirm(it.toString()) } }, enabled = parsedValue != null) {
                Text(stringResource(android.R.string.ok))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) } },
    )
}

enum class SettingsDialog { Delimiter, Retries, About }
