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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
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
import org.secu3.android.models.packets.out.DiagOutputPacket
import org.secu3.android.ui.compose.DropdownShape
import org.secu3.android.ui.compose.LocalSecuPalette
import java.util.Locale

val secu3TOutputChannels = listOf("NONE", "IGN_OUT1", "IGN_OUT2", "IGN_OUT3", "IGN_OUT4", "IE", "FE", "ECF", "CE", "ST_BLOCK", "ADD_IO1", "ADD_IO2", "BL", "DE")
val secu3IOutputChannels = listOf("NONE", "IGN_OUT1", "IGN_OUT2", "IGN_OUT3", "IGN_OUT4", "IGN_OUT5", "ECF", "INJ_O1", "INJ_O2", "INJ_O3", "INJ_O4", "INJ_O5", "BL", "DE", "STBL_O", "CEL_O", "FPMP_O", "PWRR_O", "EVAP_O", "O2SH_0", "COND_O", "ADD_O2", "TACH_O", "GPA6_O")

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
) {
    val enabled = isSecu3T != null
    val channels = if (isSecu3T == true) secu3TOutputChannels else secu3IOutputChannels
    val chosen = selectedChannel.takeIf { it in channels.indices } ?: 0
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        DiagnosticGroupSurface(Modifier.fillMaxWidth()) {
            val analogInputs = listOf(
                "Voltage" to input?.voltage, "MAP_S" to input?.map, "TEMP" to input?.temperature,
                "ADD_IO1" to input?.addI1, "ADD_IO2" to input?.addI2, "ADD_IO3" to input?.addI3, "ADD_IO4" to input?.addI4,
                "CARB" to input?.carb, "KS_1" to input?.ks1, "KS_2" to input?.ks2,
            )
            val statuses = listOf(
                "GAS_V" to input?.gasV, "CKPS" to input?.ckps, "REF_S" to input?.refS,
                "PS" to input?.ps, "BL jmp" to input?.bl, "DE jmp" to input?.de,
            )
            analogInputs.forEach { (label, value) ->
                AnalogInput(label, value)
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
            statuses.forEachIndexed { index, (label, value) ->
                StatusInput(label, value)
                if (index != statuses.lastIndex || isSecu3T == false) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
            if (isSecu3T == false) {
                StatusInput("COND_I", input?.cond_i)
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                StatusInput("EPAS_I", input?.epas_i)
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                StatusInput("IGN_I", input?.ign_i)
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                listOf("ADD_IO5" to input?.addI5, "ADD_IO6" to input?.addI6, "ADD_IO7" to input?.addI7, "ADD_IO8" to input?.addI8).forEach { (label, value) ->
                    AnalogInput(label, value)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
                StatusInput("GPA4_I", input?.gpa4_i)
            }
        }
        DiagnosticGroupSurface(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(testSignalTitle, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                var expanded by remember(isSecu3T) { mutableStateOf(false) }
                Column {
                    Text(outputChannelTitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(4.dp))
                    OutputChannelField(
                        enabled = enabled,
                        expanded = expanded,
                        onExpandedChange = { expanded = it },
                        selectedValue = channels[chosen],
                    )
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, shape = DropdownShape) {
                        channels.forEachIndexed { index, channel ->
                            DropdownMenuItem(text = { Text(channel) }, onClick = { expanded = false; onChannelSelected(index) })
                        }
                    }
                }
                EditableValueRow(frequencyLabel, "${"%.1f".format(Locale.US, frequency)} Hz", enabled, "frequency_edit", onFrequencyEdit)
                EditableValueRow(dutyLabel, "${"%.1f".format(Locale.US, duty)} %", enabled, "duty_edit", onDutyEdit)
            }
        }
    }
}

@Composable
private fun OutputChannelField(
    enabled: Boolean,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    selectedValue: String,
) {
    Row(
        Modifier.fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(DropdownShape)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, DropdownShape)
            .clickable(enabled = enabled, role = Role.Button, onClick = { onExpandedChange(true) })
            .testTag("output_channel")
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(selectedValue, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
        Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = null, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun EditableValueRow(title: String, value: String, enabled: Boolean, tag: String, onClick: () -> Unit) {
    BoxWithConstraints(
        Modifier.fillMaxWidth()
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

@Composable
private fun AnalogInput(label: String, value: Float?) {
    val displayValue = value?.let { "%.3f".format(Locale.US, it) } ?: "—"
    Row(
        Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(horizontal = 14.dp, vertical = 8.dp).testTag("analog_$label"),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
        Text(
            buildAnnotatedString {
                withStyle(SpanStyle(fontSize = 22.sp, fontWeight = FontWeight.SemiBold, fontFeatureSettings = "tnum")) { append(displayValue) }
                withStyle(SpanStyle(fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)) { append(" V") }
            },
            Modifier.testTag("analog_${label}-value"),
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
private fun StatusInput(label: String, checked: Boolean?) {
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
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = colors.onSurface)
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

internal fun selectDiagnosticOutputChannel(packet: DiagOutputPacket, channelIndex: Int, send: () -> Unit) {
    packet.chan = channelIndex
    send()
}

internal fun updateDiagnosticOutputFrequency(packet: DiagOutputPacket, value: Float, send: () -> Unit) {
    packet.frq = value
    if (packet.chan > 0) send()
}

internal fun updateDiagnosticOutputDuty(packet: DiagOutputPacket, value: Float, send: () -> Unit) {
    packet.duty = value
    if (packet.chan > 0) send()
}
