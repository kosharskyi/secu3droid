/*
 *    SecuDroid  - An open source, free manager for SECU-3 engine control unit
 *    Copyright (C) 2026 Vitalii O. Kosharskyi. Ukraine, Kyiv
 *
 *    SECU-3  - An open source, free engine control unit
 *    Copyright (C) 2007-2026 Alexey A. Shabelnikov. Ukraine, Kyiv
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
package org.secu3.android.ui.diagnostics

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.secu3.android.R
import org.secu3.android.models.packets.input.DiagInputPacket
import org.secu3.android.domain.diagnostics.DiagnosticOutput
import org.secu3.android.domain.diagnostics.DiagnosticSample
import org.secu3.android.ui.compose.LocalSecuPalette
import org.secu3.android.ui.parameters.compose.ParameterSelectionRow
import org.secu3.android.ui.parameters.dialogs.ParameterChoice
import org.secu3.android.ui.parameters.dialogs.ParameterChoiceDialog
import java.util.Locale

@Composable
fun DiagnosticsInputsScreen(
    input: DiagInputPacket?,
    isSecu3T: Boolean?,
    selectedChannel: Int,
    frequency: Float,
    duty: Float,
    onChannelSelected: (Int) -> Unit,
    onFrequencyEdit: () -> Unit,
    onDutyEdit: () -> Unit,
    testSignalTitle: String = "Test signal",
    outputChannelTitle: String = "Output channel",
    frequencyLabel: String = "Frequency",
    dutyLabel: String = "Duty cycle",
    hasTpic8101: Boolean? = null,
    controlsEnabled: Boolean = false,
    diagnosticSamples: List<DiagnosticSample> = emptyList(),
) {
    val enabled = controlsEnabled && isSecu3T != null
    val channelOutputs = if (isSecu3T == true) DiagnosticOutput.secu3TChannels else DiagnosticOutput.secu3IChannels
    val channelLabels = stringArrayResource(
        if (isSecu3T == true) R.array.diagnostics_secu3t_output_labels
        else R.array.diagnostics_secu3i_output_labels,
    )
    val channels = listOf("NONE") + channelOutputs.indices.map { channelLabels[it] }
    val chosen = selectedChannel.takeIf { it in channels.indices } ?: 0
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        DiagnosticGroupSurface(Modifier.fillMaxWidth()) {
            val addIo4Supported = isSecu3T != null && hasTpic8101 == true
            val analogInputs = buildList {
                add(Triple("Voltage", input?.voltage, R.string.diag_help_input_voltage))
                add(Triple((if (isSecu3T == false) "MAP_I" else "MAP_S"), input?.map, R.string.diag_help_input_map))
                add(Triple("TEMP", input?.temperature, R.string.diag_help_input_temp))
                add(Triple("ADD_IO1", input?.addI1, R.string.diag_help_input_add_i1))
                add(Triple("ADD_IO2", input?.addI2, R.string.diag_help_input_add_i2))
                if (isSecu3T != true) add(Triple("ADD_IO3", input?.addI3, R.string.diag_help_input_add_i3))
                add(Triple("ADD_IO4", input?.addI4.takeIf { addIo4Supported }, R.string.diag_help_input_add_i4))
                add(Triple((if (isSecu3T == false) "TPS" else "CARB"), input?.carb, R.string.diag_help_input_carb))
                add(Triple("KS_1", input?.ks1, R.string.diag_help_input_ks1))
                add(Triple("KS_2", input?.ks2, R.string.diag_help_input_ks2))
            }
            val statuses = listOf(
                Triple("GAS_V", input?.gasV, R.string.diag_help_input_gas_v),
                Triple("CKPS", input?.ckps, R.string.diag_help_input_ckps),
                Triple("REF_S", input?.refS, R.string.diag_help_input_ref_s),
                Triple("PS", input?.ps, R.string.diag_help_input_ps),
                Triple("BL jmp", input?.bl, when (isSecu3T) {
                    true -> R.string.diag_help_input_bl_t
                    false -> R.string.diag_help_input_bl_i
                    null -> R.string.diag_help_input_bl_generic
                }),
                Triple("DE jmp", input?.de, when (isSecu3T) {
                    true -> R.string.diag_help_input_de_t
                    false -> R.string.diag_help_input_de_i
                    null -> R.string.diag_help_input_de_generic
                }),
            )
            analogInputs.forEach { (label, value, helpText) ->
                AnalogInput(label, value, stringResource(helpText), subdued = label == "ADD_IO4" && !addIo4Supported)
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
            statuses.forEachIndexed { index, (label, value, helpText) ->
                StatusInput(label, value, stringResource(helpText))
                if (index != statuses.lastIndex || isSecu3T == false) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
            if (isSecu3T == false) {
                StatusInput("COND_I", input?.cond_i, stringResource(R.string.diag_help_input_cond_i))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                StatusInput("EPAS_I", input?.epas_i, stringResource(R.string.diag_help_input_epas_i))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                StatusInput("IGN_I", input?.ign_i, stringResource(R.string.diag_help_input_ign_i))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                listOf(
                    Triple("ADD_IO5", input?.addI5, R.string.diag_help_input_add_i5),
                    Triple("ADD_IO6", input?.addI6, R.string.diag_help_input_add_i6),
                    Triple("ADD_IO7", input?.addI7, R.string.diag_help_input_add_i7),
                    Triple("ADD_IO8", input?.addI8, R.string.diag_help_input_add_i8),
                ).forEach { (label, value, helpText) ->
                    AnalogInput(label, value, stringResource(helpText))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
                StatusInput("GPA4_I", input?.gpa4_i, stringResource(R.string.diag_help_input_gpa4_i))
            }
        }
        DiagnosticSignalChart(
            label = stringResource(R.string.diag_chart_ks1_label),
            samples = diagnosticSamples,
            value = DiagnosticSample::ks1,
            helpText = stringResource(R.string.diag_help_input_ks1),
        )
        DiagnosticSignalChart(
            label = stringResource(R.string.diag_chart_ks2_label),
            samples = diagnosticSamples,
            value = DiagnosticSample::ks2,
            helpText = stringResource(R.string.diag_help_input_ks2),
        )
        DiagnosticGroupSurface(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(testSignalTitle, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                var expanded by remember(isSecu3T) { mutableStateOf(false) }
                LaunchedEffect(enabled) {
                    if (!enabled) expanded = false
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    DiagnosticHelpButton(
                        outputChannelTitle,
                        stringResource(R.string.diag_help_generator_channel),
                        "diagnostic_help_generator_channel",
                    )
                    ParameterSelectionRow(
                        title = outputChannelTitle,
                        value = channels[chosen],
                        testTag = "output_channel",
                        enabled = enabled,
                        contentDescription = "$outputChannelTitle, ${channels[chosen]}",
                        stateDescription = channels[chosen],
                        onClick = { expanded = true },
                    )
                }
                if (expanded && enabled) {
                    ParameterChoiceDialog(
                        title = outputChannelTitle,
                        choices = channels.mapIndexed { index, channel ->
                            ParameterChoice(index, channel, "output_channel_option_$index")
                        },
                        onDismiss = { expanded = false },
                        onSelect = onChannelSelected,
                    )
                }
                EditableValueRow(frequencyLabel, stringResource(R.string.diag_help_generator_frequency), "generator_frequency", "${"%.1f".format(Locale.US, frequency)} Hz", enabled, "frequency_edit", onFrequencyEdit)
                EditableValueRow(dutyLabel, stringResource(R.string.diag_help_generator_duty), "generator_duty", "${"%.1f".format(Locale.US, duty)} %", enabled, "duty_edit", onDutyEdit)
            }
        }
    }
}

@Composable
private fun EditableValueRow(title: String, helpText: String, helpTag: String, value: String, enabled: Boolean, tag: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        DiagnosticHelpButton(title, helpText, "diagnostic_help_$helpTag")
    BoxWithConstraints(
        Modifier.weight(1f)
            .heightIn(min = 64.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .testTag(tag)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        val valueColor = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
        val editTint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        if (maxWidth < 340.dp) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(value, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, color = valueColor)
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(24.dp), tint = editTint)
                }
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(title, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                Text(value, style = MaterialTheme.typography.bodyLarge, color = valueColor)
                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(24.dp), tint = editTint)
            }
        }
    }
    }
}

@Composable
private fun AnalogInput(label: String, value: Float?, helpText: String, subdued: Boolean = false) {
    val displayValue = value?.let { "%.3f".format(Locale.US, it) } ?: "—"
    val valueColor = if (subdued) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
    Row(
        Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(horizontal = 14.dp, vertical = 8.dp).testTag("analog_$label"),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            Text(label, Modifier.weight(1f, fill = false), style = MaterialTheme.typography.bodyMedium, color = valueColor)
            DiagnosticHelpButton(label, helpText, "diagnostic_help_input_$label")
        }
        Text(
            buildAnnotatedString {
                withStyle(SpanStyle(fontSize = 22.sp, fontWeight = FontWeight.SemiBold, fontFeatureSettings = "tnum", color = valueColor)) { append(displayValue) }
                withStyle(SpanStyle(fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)) { append(" V") }
            },
            Modifier.testTag("analog_${label}-value"),
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
private fun StatusInput(label: String, checked: Boolean?, helpText: String) {
    val palette = LocalSecuPalette.current
    val colors = MaterialTheme.colorScheme
    val stateDescription = when (checked) {
        true -> stringResource(R.string.sensors_status_accessibility_on)
        false -> stringResource(R.string.sensors_status_accessibility_off)
        null -> stringResource(R.string.sensors_status_accessibility_unavailable)
    }
    val fill = when (checked) {
        true -> palette.successContainer
        false -> colors.surfaceVariant
        null -> colors.background
    }
    val statusColor = when (checked) {
        true -> palette.onSuccessContainer
        false, null -> colors.onSurfaceVariant
    }
    Row(
        Modifier.fillMaxWidth().heightIn(min = 64.dp).background(colors.surface).padding(horizontal = 14.dp, vertical = 8.dp)
            .testTag("status_$label")
            .semantics { this.stateDescription = stateDescription },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            Text(label, Modifier.weight(1f, fill = false), style = MaterialTheme.typography.bodyMedium, color = colors.onSurface)
            DiagnosticHelpButton(label, helpText, "diagnostic_help_input_$label")
        }
        Text(
            stateDescription,
            Modifier.widthIn(max = 120.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(fill)
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .testTag("status_$label-value"),
            style = MaterialTheme.typography.labelMedium,
            color = statusColor,
        )
    }
}
