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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.secu3.android.R
import org.secu3.android.ui.compose.ChoiceDialog
import org.secu3.android.ui.compose.SecuTopAppBar
import org.secu3.android.ui.compose.SettingsActionRow
import org.secu3.android.ui.compose.SettingsSection
import org.secu3.android.ui.compose.SettingsToggleRow

@Composable
internal fun LogSettingsScreen(
    state: LogSettingsUiState?,
    showDelimiter: Boolean,
    onNavigateUp: () -> Unit,
    onToggle: (Int, Boolean) -> Unit,
    onOpenDelimiter: () -> Unit,
    onSelectDelimiter: (String) -> Unit,
    onDismissDelimiter: () -> Unit,
    onChooseFolder: () -> Unit,
) {
    Scaffold(topBar = {
        SecuTopAppBar(
            title = stringResource(R.string.log_settings_title),
            onNavigateUp = onNavigateUp,
            showStatus = false,
        )
    }) { contentPadding ->
        if (state == null) {
            Box(Modifier.fillMaxSize().padding(contentPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        val delimiterValues = stringArrayResource(R.array.csv_delimeters_values)
        val delimiterLabels = stringArrayResource(R.array.csv_delimeters)
        val delimiterIndex = delimiterValues.indexOf(state.csvDelimiter).takeIf { it >= 0 } ?: 1
        Column(
            Modifier.fillMaxSize().padding(contentPadding).verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
        ) {
            SettingsSection {
                SettingsToggleRow(
                    title = stringResource(R.string.pref_write_log_title),
                    summary = stringResource(R.string.pref_write_log_summary),
                    checked = state.showLogControls,
                    testTag = "settings-toggle-${R.string.pref_write_log_key}",
                ) { onToggle(R.string.pref_write_log_key, it) }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                SettingsToggleRow(
                    title = stringResource(R.string.pref_always_write_log_title),
                    summary = stringResource(R.string.pref_always_write_log_summary),
                    checked = state.alwaysWriteLog,
                    testTag = "settings-toggle-${R.string.pref_always_write_log_key}",
                ) { onToggle(R.string.pref_always_write_log_key, it) }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                SettingsToggleRow(
                    title = stringResource(R.string.pref_log_csv_write_title),
                    checked = state.writeCsvHeader,
                    testTag = "settings-toggle-${R.string.pref_log_csv_write_title_key}",
                ) { onToggle(R.string.pref_log_csv_write_title_key, it) }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                SettingsActionRow(
                    title = stringResource(R.string.pref_log_csv_delimeter_title),
                    summary = delimiterLabels[delimiterIndex],
                    onClick = onOpenDelimiter,
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                SettingsToggleRow(
                    title = stringResource(R.string.pref_write_binary_log_title),
                    checked = state.writeBinaryLog,
                    testTag = "settings-toggle-${R.string.pref_write_binary_log_key}",
                ) { onToggle(R.string.pref_write_binary_log_key, it) }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                SettingsActionRow(
                    title = stringResource(R.string.pref_log_export_directory_title),
                    summary = state.exportFolder,
                    onClick = onChooseFolder,
                    testTag = "settings-export-folder",
                )
            }
        }
        if (showDelimiter) {
            ChoiceDialog(
                title = {
                    Text(
                        text = stringResource(R.string.pref_log_csv_delimeter_title),
                        modifier = Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                    )
                },
                selected = state.csvDelimiter,
                choices = delimiterValues.toList(),
                label = { value -> delimiterLabels[delimiterValues.indexOf(value)] },
                onSelect = onSelectDelimiter,
                onDismiss = onDismissDelimiter,
                confirmButton = { Button(onClick = onDismissDelimiter) { Text(stringResource(android.R.string.ok)) } },
                shape = RoundedCornerShape(24.dp),
                containerColor = MaterialTheme.colorScheme.surface,
            )
        }
    }
}
