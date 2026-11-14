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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import org.secu3.android.R
import org.secu3.android.ui.compose.SecuTopAppBar
import org.secu3.android.utils.prefs.FuelDensities

@Composable
internal fun ParameterSettingsScreen(
    densities: FuelDensities?,
    canSave: Boolean,
    storageError: Boolean,
    parseDensity: (String) -> Float?,
    onSave: (Float, Float) -> Unit,
    onNavigateUp: () -> Unit,
) {
    Column(
        Modifier.fillMaxSize().windowInsetsPadding(
            WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom),
        ),
    ) {
        SecuTopAppBar(
            title = stringResource(R.string.parameter_settings_title),
            onNavigateUp = onNavigateUp,
            showStatus = false,
        )
        when {
            densities != null -> DensitySettingsForm(densities, canSave, storageError, parseDensity, Modifier.weight(1f), onSave)
            storageError -> Text(
                stringResource(R.string.preferences_storage_error),
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.weight(1f).fillMaxWidth().padding(24.dp).testTag("parameter-settings-storage-error"),
            )
            else -> androidx.compose.foundation.layout.Box(
                Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }
        }
    }
}

@Composable
private fun DensitySettingsForm(
    densities: FuelDensities,
    canSave: Boolean,
    storageError: Boolean,
    parseDensity: (String) -> Float?,
    modifier: Modifier,
    onSave: (Float, Float) -> Unit,
) {
    var primaryText by rememberSaveable { mutableStateOf(densities.primary.toString()) }
    var secondaryText by rememberSaveable { mutableStateOf(densities.secondary.toString()) }
    val primary = remember(primaryText) { parseDensity(primaryText) }
    val secondary = remember(secondaryText) { parseDensity(secondaryText) }
    val valid = primary != null && secondary != null
    Column(
        modifier.fillMaxWidth().imePadding().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        DensityField(
            value = primaryText,
            label = stringResource(R.string.parameter_primary_fuel_density),
            tag = "parameter-primary-fuel-density",
            onValueChange = { primaryText = it },
        )
        DensityField(
            value = secondaryText,
            label = stringResource(R.string.parameter_secondary_fuel_density),
            tag = "parameter-secondary-fuel-density",
            onValueChange = { secondaryText = it },
        )
        if (storageError) Text(
            stringResource(R.string.preferences_storage_error),
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.testTag("parameter-settings-storage-error"),
        )
        if (!valid) Text(
            stringResource(R.string.parameter_fuel_density_range),
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.testTag("parameter-density-validation"),
        )
        Button(
            onClick = { onSave(requireNotNull(primary), requireNotNull(secondary)) },
            enabled = valid && canSave,
            modifier = Modifier.fillMaxWidth().testTag("parameter-settings-save"),
        ) {
            if (canSave) Text(stringResource(R.string.save)) else CircularProgressIndicator()
        }
    }
}

@Composable
private fun DensityField(value: String, label: String, tag: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        suffix = { Text(stringResource(R.string.fuel_density_unit)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth().testTag(tag),
    )
}
