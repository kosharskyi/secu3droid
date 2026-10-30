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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import org.secu3.android.R
import org.secu3.android.ui.compose.SettingsActionRow
import org.secu3.android.ui.compose.ChoiceDialog
import org.secu3.android.ui.compose.SettingsSection
import org.secu3.android.ui.compose.SettingsToggleRow
import org.secu3.android.ui.compose.SecuTopAppBar
import org.secu3.android.utils.AppearanceMode

data class SettingsUiState(
    val writeLog: Boolean,
    val writeCsvHeader: Boolean,
    val csvDelimiter: String,
    val writeBinaryLog: Boolean,
    val exportFolder: String,
    val connectionRetries: String,
    val keepScreenOn: Boolean,
    val wakeLock: Boolean,
    val appearanceMode: AppearanceMode = AppearanceMode.System,
    val fuelFlowPulsesPerLitre: String = "16000",
)

@Composable
internal fun SettingsContent(
    state: SettingsUiState,
    dialog: SettingsDialog?,
    onNavigateUp: () -> Unit,
    onToggle: (Int, Boolean) -> Unit,
    onSelectDelimiter: (String) -> Unit,
    onChooseFolder: () -> Unit,
    onEditRetries: () -> Unit,
    onValidateRetries: (String) -> Boolean,
    onSaveRetries: (String) -> Unit,
    onSelectDialog: (SettingsDialog?) -> Unit,
    onDismissDialog: () -> Unit,
    onShowAbout: () -> Unit,
    onSelectAppearance: (AppearanceMode) -> Unit = {},
    notificationAccessGranted: Boolean = false,
    onConnectionNotifications: () -> Unit = {},
    onEditFuelFlowPulsesPerLitre: () -> Unit = {},
    onValidateFuelFlowPulsesPerLitre: (String) -> Boolean = { false },
    onSaveFuelFlowPulsesPerLitre: (String) -> Unit = {},
) {
    val appearanceLabel: (AppearanceMode) -> Int = { mode ->
        when (mode) {
            AppearanceMode.System -> R.string.appearance_system
            AppearanceMode.Light -> R.string.appearance_light
            AppearanceMode.Dark -> R.string.appearance_dark
        }
    }
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
            Modifier.fillMaxSize().padding(contentPadding).verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SettingsSection {
                SettingsActionRow(
                    title = stringResource(R.string.appearance_title),
                    summary = stringResource(appearanceLabel(state.appearanceMode)),
                    onClick = { onSelectDialog(SettingsDialog.Appearance) },
                )
            }
            SettingsSection(title = stringResource(R.string.settings_group_logging)) {
                SettingsToggleRow(
                    title = stringResource(R.string.pref_write_log_title),
                    summary = stringResource(R.string.pref_write_log_summary),
                    checked = state.writeLog,
                    testTag = "settings-toggle-${R.string.pref_write_log_key}",
                ) { onToggle(R.string.pref_write_log_key, it) }
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
                    onClick = { onSelectDialog(SettingsDialog.Delimiter) },
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
            SettingsSection(title = stringResource(R.string.settings_group_connection)) {
                SettingsActionRow(
                    title = stringResource(R.string.pref_connection_retries_title),
                    summary = stringResource(R.string.pref_connection_retries_summary, state.connectionRetries),
                    onClick = onEditRetries,
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                SettingsToggleRow(
                    title = stringResource(R.string.pref_wakelock_title),
                    summary = stringResource(R.string.pref_wakelock_summary),
                    checked = state.wakeLock,
                    testTag = "settings-toggle-${R.string.pref_wakelock_key}",
                ) { onToggle(R.string.pref_wakelock_key, it) }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                SettingsActionRow(
                    title = stringResource(R.string.connection_notifications_title),
                    summary = stringResource(
                        if (notificationAccessGranted) R.string.connection_notifications_enabled
                        else R.string.connection_notifications_disabled,
                    ),
                    onClick = onConnectionNotifications,
                    testTag = "settings-connection-notifications",
                )
            }
            SettingsSection(title = stringResource(R.string.settings_group_instruments)) {
                SettingsActionRow(
                    title = stringResource(R.string.pref_fuel_flow_pulses_per_litre_title),
                    summary = stringResource(
                        R.string.pref_fuel_flow_pulses_per_litre_summary,
                        state.fuelFlowPulsesPerLitre,
                    ),
                    onClick = onEditFuelFlowPulsesPerLitre,
                    testTag = "settings-fuel-flow-pulses-per-litre",
                )
            }
            SettingsSection(title = stringResource(R.string.settings_group_display)) {
                SettingsToggleRow(
                    title = stringResource(R.string.pref_keep_screen_title),
                    summary = stringResource(R.string.pref_keep_screen_summary),
                    checked = state.keepScreenOn,
                    testTag = "settings-toggle-${R.string.pref_keep_screen_key}",
                ) { onToggle(R.string.pref_keep_screen_key, it) }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                SettingsActionRow(
                    title = stringResource(R.string.pref_about_title),
                    summary = stringResource(R.string.pref_about_summary),
                    onClick = onShowAbout,
                )
            }
        }
    }

    when (dialog) {
        SettingsDialog.Delimiter -> ChoiceDialog(
            title = { SettingsDialogTitle(stringResource(R.string.pref_log_csv_delimeter_title)) },
            selected = state.csvDelimiter,
            choices = delimiterValues.toList(),
            label = { value -> delimiterLabels[delimiterValues.indexOf(value)] },
            onSelect = onSelectDelimiter,
            onDismiss = onDismissDialog,
            confirmButton = { Button(onClick = onDismissDialog) { Text(stringResource(android.R.string.ok)) } },
            shape = RoundedCornerShape(24.dp),
            containerColor = MaterialTheme.colorScheme.surface,
        )
        SettingsDialog.Retries -> RetryDialog(
            initialValue = state.connectionRetries,
            onValidate = onValidateRetries,
            onConfirm = onSaveRetries,
            onDismiss = onDismissDialog,
        )
        SettingsDialog.FuelFlowPulsesPerLitre -> RetryDialog(
            initialValue = state.fuelFlowPulsesPerLitre,
            onValidate = onValidateFuelFlowPulsesPerLitre,
            onConfirm = onSaveFuelFlowPulsesPerLitre,
            onDismiss = onDismissDialog,
            titleResource = R.string.pref_fuel_flow_pulses_per_litre_title,
            preserveInput = true,
            numericKeyboard = true,
            textFieldTestTag = "settings-fuel-flow-pulses-per-litre-input",
        )
        SettingsDialog.About -> Unit
        SettingsDialog.Appearance -> ChoiceDialog(
            title = { SettingsDialogTitle(stringResource(R.string.appearance_title)) },
            selected = state.appearanceMode,
            choices = AppearanceMode.entries,
            label = { mode -> stringResource(appearanceLabel(mode)) },
            onSelect = onSelectAppearance,
            onDismiss = onDismissDialog,
            confirmButton = { Button(onClick = onDismissDialog) { Text(stringResource(android.R.string.ok)) } },
            optionTestTag = { mode -> "settings-appearance-${mode.name.lowercase()}" },
            shape = RoundedCornerShape(24.dp),
            containerColor = MaterialTheme.colorScheme.surface,
        )
        null -> Unit
    }
}

@Composable
private fun RetryDialog(
    initialValue: String,
    onValidate: (String) -> Boolean,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    titleResource: Int = R.string.pref_connection_retries_title,
    preserveInput: Boolean = false,
    numericKeyboard: Boolean = false,
    textFieldTestTag: String? = null,
) {
    var text by androidx.compose.runtime.remember(initialValue) { mutableStateOf(initialValue) }
    val valid = onValidate(text)
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = { SettingsDialogTitle(stringResource(titleResource)) },
        text = {
            OutlinedTextField(
                modifier = textFieldTestTag?.let { Modifier.testTag(it) } ?: Modifier,
                value = text,
                onValueChange = { value -> text = if (preserveInput) value else value.filter(Char::isDigit) },
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = if (numericKeyboard) KeyboardType.Number else KeyboardType.Text,
                ),
                isError = !valid,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                ),
            )
        },
        confirmButton = {
            Button(onClick = { if (valid) onConfirm(text) }, enabled = valid) {
                Text(stringResource(android.R.string.ok))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) } },
    )
}

@Composable
private fun SettingsDialogTitle(title: String) {
    Text(
        text = title,
        modifier = Modifier.fillMaxWidth(),
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center,
    )
}

enum class SettingsDialog { Delimiter, Retries, FuelFlowPulsesPerLitre, Appearance, About }
