/*
 *    SecuDroid  - An open source, free manager for SECU-3 engine control unit
 *    Copyright (C) 2026 Vitalii O. Kosharskyi. Ukraine, Kyiv
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 */
package org.secu3.android.ui.parameters.pages.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import org.secu3.android.R
import org.secu3.android.domain.parameters.PressureCurveInput
import org.secu3.android.domain.parameters.PressureCurvePreview
import org.secu3.android.domain.parameters.PressureSensor
import org.secu3.android.domain.parameters.PressureUnit
import org.secu3.android.domain.parameters.VoltageUnit
import org.secu3.android.ui.parameters.dialogs.ParameterAlertDialog

@Composable
internal fun PressureCurveDialog(
    sensor: PressureSensor,
    initial: PressureCurveInput,
    onPreview: (PressureCurveInput) -> PressureCurvePreview?,
    onApply: (PressureCurveInput) -> Boolean,
    onDismiss: () -> Unit,
) {
    var input by remember(sensor) { mutableStateOf(initial) }
    val result = onPreview(input)
    val locale = LocalConfiguration.current.locales[0]
    val pressureLabel = stringResource(R.string.pressure_calculator_pressure)
    val voltageLabel = stringResource(R.string.pressure_calculator_voltage)

    ParameterAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.pressure_calculator_title, sensor.name)) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                        .toggleable(input.knownGradient, role = Role.Checkbox) {
                            input = input.copy(knownGradient = it)
                        }
                        .testTag("pressure_calculator_known_gradient"),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(input.knownGradient, onCheckedChange = null)
                    Text(stringResource(R.string.pressure_calculator_known_gradient))
                }
                Text(stringResource(R.string.pressure_calculator_point, 1), style = MaterialTheme.typography.titleSmall)
                PressurePointField(
                    pressureLabel, input.pressure1, { input = input.copy(pressure1 = it) },
                    input.pressureUnit1, PressureUnit.entries, { input = input.copy(pressureUnit1 = it) },
                    { pressureUnitLabel(it) },
                )
                PressurePointField(
                    voltageLabel, input.voltage1, { input = input.copy(voltage1 = it) },
                    input.voltageUnit1, VoltageUnit.entries, { input = input.copy(voltageUnit1 = it) },
                    { voltageUnitLabel(it) },
                )

                if (input.knownGradient) {
                    OutlinedTextField(
                        value = input.gradient,
                        onValueChange = { input = input.copy(gradient = it.replace(',', '.')) },
                        label = { Text(stringResource(R.string.pressure_calculator_gradient)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.DecimalSigned),
                        modifier = Modifier.fillMaxWidth().testTag("pressure_calculator_gradient"),
                    )
                    FlowRow {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val pressureUnit: @Composable () -> Unit = {
                                PressureUnitMenu(
                                    input.gradientPressureUnit, PressureUnit.entries, pressureLabel,
                                    { pressureUnitLabel(it) },
                                    modifier = Modifier.testTag("pressure_calculator_gradient_pressure_unit"),
                                ) { input = input.copy(gradientPressureUnit = it) }
                            }
                            val voltageUnit: @Composable () -> Unit = {
                                PressureUnitMenu(
                                    input.gradientVoltageUnit, VoltageUnit.entries, voltageLabel,
                                    { voltageUnitLabel(it) },
                                ) { input = input.copy(gradientVoltageUnit = it) }
                            }
                            if (input.gradientReciprocal) {
                                voltageUnit()
                                Text("/")
                                pressureUnit()
                            } else {
                                pressureUnit()
                                Text("/")
                                voltageUnit()
                            }
                        }
                        IconButton(onClick = {
                            input = input.copy(gradientReciprocal = !input.gradientReciprocal)
                        }) {
                            Icon(
                                Icons.Default.SwapHoriz,
                                contentDescription = stringResource(R.string.pressure_calculator_swap_units),
                            )
                        }
                    }
                } else {
                    Text(stringResource(R.string.pressure_calculator_point, 2), style = MaterialTheme.typography.titleSmall)
                    PressurePointField(
                        pressureLabel, input.pressure2, { input = input.copy(pressure2 = it) },
                        input.pressureUnit2, PressureUnit.entries, { input = input.copy(pressureUnit2 = it) },
                        { pressureUnitLabel(it) },
                    )
                    PressurePointField(
                        voltageLabel, input.voltage2, { input = input.copy(voltage2 = it) },
                        input.voltageUnit2, VoltageUnit.entries, { input = input.copy(voltageUnit2 = it) },
                        { voltageUnitLabel(it) },
                    )
                }

                if (result == null) {
                    Text(
                        stringResource(
                            if (input.knownGradient) R.string.pressure_calculator_invalid_known
                            else R.string.pressure_calculator_invalid_points,
                        ),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                } else {
                    Text(
                        stringResource(
                            R.string.pressure_calculator_preview,
                            String.format(locale, "%.4f", result.offsetVolts),
                            stringResource(R.string.units_volts),
                            String.format(locale, "%.3f", result.gradientKpaPerVolt),
                            stringResource(R.string.units_kpa_per_volt),
                        ),
                        modifier = Modifier.testTag("pressure_calculator_preview"),
                    )
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.testTag("pressure_calculator_cancel")) {
                Text(stringResource(android.R.string.cancel))
            }
        },
        confirmButton = {
            Button(
                onClick = { if (onApply(input)) onDismiss() },
                enabled = result != null,
                modifier = Modifier.testTag("pressure_calculator_apply"),
            ) { Text(stringResource(R.string.pressure_calculator_apply)) }
        },
    )
}

@Composable
private fun <T> PressurePointField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    unit: T,
    units: List<T>,
    onUnitChange: (T) -> Unit,
    unitLabel: @Composable (T) -> String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = { onValueChange(it.replace(',', '.')) },
            label = { Text(label) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.weight(1f),
        )
        PressureUnitMenu(unit, units, label, unitLabel, onSelect = onUnitChange)
    }
}

@Composable
private fun <T> PressureUnitMenu(
    selected: T,
    units: List<T>,
    dimension: String,
    unitLabel: @Composable (T) -> String,
    modifier: Modifier = Modifier,
    onSelect: (T) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val label = unitLabel(selected)
    Box {
        TextButton(
            onClick = { expanded = true },
            modifier = modifier.semantics { contentDescription = "$dimension, $label" },
        ) {
            Text(label)
            Icon(Icons.Default.ExpandMore, contentDescription = null)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            units.forEach { unit ->
                DropdownMenuItem(
                    text = { Text(unitLabel(unit)) },
                    onClick = {
                        onSelect(unit)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun pressureUnitLabel(unit: PressureUnit): String = stringResource(
    when (unit) {
        PressureUnit.PA -> R.string.pressure_calculator_unit_pa
        PressureUnit.KPA -> R.string.units_pressure_kpa
        PressureUnit.BAR -> R.string.pressure_calculator_unit_bar
        PressureUnit.MMHG -> R.string.pressure_calculator_unit_mmhg
        PressureUnit.TECHNICAL_ATM -> R.string.pressure_calculator_unit_at
        PressureUnit.PSI -> R.string.pressure_calculator_unit_psi
    },
)

@Composable
private fun voltageUnitLabel(unit: VoltageUnit): String = stringResource(
    when (unit) {
        VoltageUnit.VOLTS -> R.string.units_volts
        VoltageUnit.MILLIVOLTS -> R.string.pressure_calculator_unit_mv
    },
)
