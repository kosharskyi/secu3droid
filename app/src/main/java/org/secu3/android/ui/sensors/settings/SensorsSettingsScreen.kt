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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import org.secu3.android.models.SensorGaugeStyle
import org.secu3.android.models.InjectionTimingMode
import org.secu3.android.ui.compose.SettingsActionRow
import org.secu3.android.ui.compose.ChoiceDialog
import org.secu3.android.ui.compose.SettingsSection
import org.secu3.android.ui.compose.SettingsToggleRow
import org.secu3.android.ui.compose.SecuTopAppBar

@Composable
internal fun SensorsSettingsScreen(
    oldView: Boolean,
    onOldViewChange: (Boolean) -> Unit,
    onNavigateUp: () -> Unit,
    gaugeStyle: SensorGaugeStyle = SensorGaugeStyle.Default,
    onGaugeStyleChange: (SensorGaugeStyle) -> Unit = {},
    injectionTimingMode: InjectionTimingMode = InjectionTimingMode.Default,
    onInjectionTimingModeChange: (InjectionTimingMode) -> Unit = {},
) {
    var gaugeStyleDialog by remember { mutableStateOf(false) }
    var injectionTimingDialog by remember { mutableStateOf(false) }
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
                SettingsActionRow(
                    title = stringResource(R.string.sensor_gauge_style),
                    onClick = { gaugeStyleDialog = true },
                    testTag = "sensor-gauge-style",
                    value = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(gaugeStyle.labelResource()), Modifier.testTag("sensor-gauge-style-value"), style = MaterialTheme.typography.bodyLarge)
                            Icon(Icons.Default.ExpandMore, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    },
                )
            }
            SettingsSection {
                SettingsActionRow(
                    title = stringResource(R.string.injection_timing_mode),
                    onClick = { injectionTimingDialog = true },
                    testTag = "injection-timing-mode",
                    value = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(injectionTimingMode.labelResource()), Modifier.testTag("injection-timing-mode-value"), style = MaterialTheme.typography.bodyLarge)
                            Icon(Icons.Default.ExpandMore, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    },
                )
            }
        }
    }
    if (gaugeStyleDialog) {
        ChoiceDialog(
            title = { Text(stringResource(R.string.sensor_gauge_style)) },
            selected = gaugeStyle,
            choices = SensorGaugeStyle.entries,
            label = { style -> stringResource(style.labelResource()) },
            onSelect = { style ->
                onGaugeStyleChange(style)
                gaugeStyleDialog = false
            },
            onDismiss = { gaugeStyleDialog = false },
            confirmButton = { TextButton(onClick = { gaugeStyleDialog = false }) { Text(stringResource(R.string.close)) } },
            optionTestTag = { style -> "sensor-gauge-style-${style.preferenceValue}" },
            optionMinHeight = 48.dp,
            optionHorizontalPadding = 0.dp,
        )
    }
    if (injectionTimingDialog) {
        ChoiceDialog(
            title = { Text(stringResource(R.string.injection_timing_mode)) },
            selected = injectionTimingMode,
            choices = InjectionTimingMode.entries,
            label = { mode -> stringResource(mode.labelResource()) },
            onSelect = { mode ->
                onInjectionTimingModeChange(mode)
                injectionTimingDialog = false
            },
            onDismiss = { injectionTimingDialog = false },
            confirmButton = { TextButton(onClick = { injectionTimingDialog = false }) { Text(stringResource(R.string.close)) } },
            optionTestTag = { mode -> "injection-timing-mode-${mode.preferenceValue}" },
            optionMinHeight = 48.dp,
            optionHorizontalPadding = 0.dp,
        )
    }
}

private fun SensorGaugeStyle.labelResource(): Int = when (this) {
    SensorGaugeStyle.Current -> R.string.sensor_gauge_style_current
    SensorGaugeStyle.PreciseArc -> R.string.sensor_gauge_style_precise_arc
    SensorGaugeStyle.ThinNeedle -> R.string.sensor_gauge_style_thin_needle
}

private fun InjectionTimingMode.labelResource(): Int = when (this) {
    InjectionTimingMode.Btdc720 -> R.string.injection_timing_btdc_720
    InjectionTimingMode.Atdc720 -> R.string.injection_timing_atdc_720
    InjectionTimingMode.Btdc360 -> R.string.injection_timing_btdc_360
    InjectionTimingMode.Atdc360 -> R.string.injection_timing_atdc_360
}
