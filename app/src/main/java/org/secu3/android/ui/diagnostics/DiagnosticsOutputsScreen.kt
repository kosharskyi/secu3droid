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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import org.secu3.android.R
import org.secu3.android.domain.diagnostics.DiagnosticOutput
import org.secu3.android.domain.diagnostics.DiagnosticsState
import org.secu3.android.domain.diagnostics.DiagnosticsSession

@Composable
internal fun DiagnosticsOutputsScreen(
    isSecu3T: Boolean?,
    state: DiagnosticsState,
    onStartAutoTest: () -> Unit = {},
    onStopAutoTest: () -> Unit = {},
    onOutputChanged: (DiagnosticOutput, Boolean) -> Unit,
) {
    val familyOutputs = when (isSecu3T) {
        true -> DiagnosticOutput.secu3TChannels
        false -> DiagnosticOutput.secu3IChannels
        null -> emptyList()
    }
    val outputLabels = stringArrayResource(
        if (isSecu3T == true) R.array.diagnostics_secu3t_output_labels
        else R.array.diagnostics_secu3i_output_labels,
    )
    val labelsByOutput = familyOutputs.zip(outputLabels).toMap()
    val outputHelp = stringArrayResource(
        if (isSecu3T == true) R.array.diag_help_outputs_t
        else R.array.diag_help_outputs_i,
    )
    val helpByOutput = familyOutputs.zip(outputHelp).toMap()
    val outputs = familyOutputs.filter { it != DiagnosticOutput.TACH_O || state.enableTachO }
    Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (isSecu3T == null) return@Column
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                modifier = Modifier.testTag("diagnostics_auto_test_toggle"),
                enabled = state.canStartOutputAutoTest || (state.controlsEnabled && state.isAutoTestingOutputs),
                onClick = if (state.isAutoTestingOutputs) onStopAutoTest else onStartAutoTest,
            ) {
                Text(stringResource(if (state.isAutoTestingOutputs) R.string.diag_auto_test_stop else R.string.diag_auto_test_start))
            }
            if (state.isAutoTestingOutputs) {
                Text(
                    stringResource(R.string.diag_auto_test_running, DiagnosticsSession.AUTO_TEST_TICK_MILLIS),
                    Modifier.testTag("diagnostics_auto_test_status"),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        val ignitionCount = if (isSecu3T) 4 else 5
        OutputGroup(
            title = stringResource(R.string.diag_outputs_ignition_group),
            outputs = outputs.take(ignitionCount),
            labelsByOutput = labelsByOutput,
            helpByOutput = helpByOutput,
            state = state,
            onOutputChanged = onOutputChanged,
        )
        OutputGroup(
            title = stringResource(if (isSecu3T) R.string.diag_outputs_other_group else R.string.diag_outputs_injection_group),
            outputs = outputs.drop(ignitionCount),
            labelsByOutput = labelsByOutput,
            helpByOutput = helpByOutput,
            state = state,
            onOutputChanged = onOutputChanged,
        )
    }
}

@Composable
private fun OutputGroup(
    title: String,
    outputs: List<DiagnosticOutput>,
    labelsByOutput: Map<DiagnosticOutput, String>,
    helpByOutput: Map<DiagnosticOutput, String>,
    state: DiagnosticsState,
    onOutputChanged: (DiagnosticOutput, Boolean) -> Unit,
) {
    DiagnosticGroupSurface(Modifier.fillMaxWidth()) {
        Column {
            Text(
                title,
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            outputs.forEachIndexed { index, output ->
                OutputToggle(
                    output,
                    labelsByOutput.getValue(output),
                    stringResource(R.string.diag_help_output_common_note, helpByOutput.getValue(output)),
                    state.outputBits,
                    state.canSetOutput(output),
                    isGeneratorOutput = output == state.generatorOutput,
                    onOutputChanged = onOutputChanged,
                )
                if (index != outputs.lastIndex) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
}

@Composable
private fun OutputToggle(
    output: DiagnosticOutput,
    label: String,
    helpText: String,
    bits: Int,
    enabled: Boolean,
    isGeneratorOutput: Boolean,
    onOutputChanged: (DiagnosticOutput, Boolean) -> Unit,
) {
    val checked = bits and (1 shl output.bit) != 0
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(Modifier.weight(1f).heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(
                Modifier.weight(1f, fill = false).widthIn(min = 48.dp).heightIn(min = 48.dp)
                    .clickable(enabled = enabled) { onOutputChanged(output, !checked) }
                    .testTag("output_row_${output.name}"),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(label, color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f))
                if (isGeneratorOutput) {
                    Text(
                        text = stringResource(R.string.diag_output_controlled_by_generator),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.testTag("generator_control_caption_${output.name}"),
                    )
                }
            }
            DiagnosticHelpButton(label, helpText, "diagnostic_help_output_${output.name}")
        }
        Checkbox(
            modifier = Modifier.testTag("output_${output.name}"),
            checked = checked,
            enabled = enabled,
            onCheckedChange = { newValue ->
                onOutputChanged(output, newValue)
            },
        )
    }
}
