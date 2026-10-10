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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import org.secu3.android.models.packets.input.DiagInputPacket
import org.secu3.android.models.packets.out.DiagOutputPacket
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
    val packet = input ?: DiagInputPacket()
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(
            "Voltage" to input?.voltage, "MAP_S" to input?.map, "TEMP" to input?.temperature,
            "ADD_IO1" to input?.addI1, "ADD_IO2" to input?.addI2, "ADD_IO3" to input?.addI3, "ADD_IO4" to input?.addI4,
            "CARB" to input?.carb, "KS_1" to input?.ks1, "KS_2" to input?.ks2,
        ).forEach { (label, value) -> AnalogInput(label, value) }
        listOf("GAS_V" to packet.gasV, "CKPS" to packet.ckps, "REF_S" to packet.refS, "PS" to packet.ps, "BL jmp" to packet.bl, "DE jmp" to packet.de).forEach { StatusInput(it.first, it.second) }
        if (isSecu3T == false) {
            StatusInput("COND_I", packet.cond_i)
            StatusInput("EPAS_I", packet.epas_i)
            StatusInput("IGN_I", packet.ign_i)
            listOf("ADD_IO5" to packet.addI5, "ADD_IO6" to packet.addI6, "ADD_IO7" to packet.addI7, "ADD_IO8" to packet.addI8).forEach { (label, value) -> AnalogInput(label, value) }
            StatusInput("GPA4_I", packet.gpa4_i)
        }
        Card(
            Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(testSignalTitle, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                var expanded by remember(isSecu3T) { mutableStateOf(false) }
                Column {
                    Text(outputChannelTitle, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(4.dp))
                    OutputChannelField(
                        enabled = enabled,
                        expanded = expanded,
                        onExpandedChange = { expanded = it },
                        selectedValue = channels[chosen],
                    )
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        channels.forEachIndexed { index, channel ->
                            DropdownMenuItem(text = { Text(channel) }, onClick = { expanded = false; onChannelSelected(index) })
                        }
                    }
                }
                EditableValueRow(frequencyLabel, "${"%.1f".format(Locale.US, frequency)} Hz", enabled, "frequency_edit", onFrequencyEdit)
                EditableValueRow(dutyLabel, "${"%.1f".format(Locale.US, duty)} %", enabled, "duty_edit", onDutyEdit)
            }
        }
        Spacer(Modifier.height(8.dp))
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
            .height(56.dp)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
            .clickable(enabled = enabled, role = Role.Button, onClick = { onExpandedChange(true) })
            .testTag("output_channel")
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(selectedValue, style = MaterialTheme.typography.bodyLarge, color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
        Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = null, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun EditableValueRow(title: String, value: String, enabled: Boolean, tag: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth()
            .height(64.dp)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .testTag(tag)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyLarge, color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun AnalogInput(label: String, value: Float?) {
    Row(Modifier.fillMaxWidth().testTag("analog_$label"), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label)
        Text("${"%.3f".format(Locale.US, value ?: 0f)} V")
    }
}

@Composable
private fun StatusInput(label: String, checked: Boolean) {
    Row(Modifier.fillMaxWidth().testTag("status_$label"), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f))
        Checkbox(checked = checked, onCheckedChange = null)
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
