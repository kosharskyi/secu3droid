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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import org.secu3.android.R
import org.secu3.android.models.packets.out.DiagOutputPacket

internal enum class DiagnosticOutput(val label: String, val bit: Int, val secondBit: Int? = null) {
    IGN_OUT1("IGN_OUT1", 0), IGN_OUT2("IGN_OUT2", 1), IGN_OUT3("IGN_OUT3", 2), IGN_OUT4("IGN_OUT4", 3),
    IE("IE", 4), FE("FE", 5), ECF("ECF", 6), CE("CE", 7), ST_BLOCK("ST_BLOCK", 8), ADD_IO1("ADD_IO1", 9), ADD_IO2("ADD_IO2", 10),
    IGN_OUT5("IGN_OUT5", 4), ECF0("ECF0", 5), INJ_O1("INJ_O1", 6), INJ_O2("INJ_O2", 7), INJ_O3("INJ_O3", 8),
    INJ_O4("INJ_O4", 9), INJ_O5("INJ_O5", 10), BL("BL", 11, 12), DE("DE", 13, 14), STBL_O("STBL_O", 15),
    CEL_O("CEL_O", 16), FPMP_O("FPMP_O", 17), PWRR_O("PWRR_O", 18), EVAP_O("EVAP_O", 19), O2SH_O("O2SH_O", 20),
    COND_O("COND_O", 21), ADD_O2("ADD_O2", 22), TACH_O("TACH_O", 23, 24), GPA6_O("GPA6_O", 25),
}

internal fun setDiagnosticOutput(packet: DiagOutputPacket, output: DiagnosticOutput, enabled: Boolean) {
    when (output) {
        DiagnosticOutput.IGN_OUT1 -> packet.ignOut1 = enabled
        DiagnosticOutput.IGN_OUT2 -> packet.ignOut2 = enabled
        DiagnosticOutput.IGN_OUT3 -> packet.ignOut3 = enabled
        DiagnosticOutput.IGN_OUT4 -> packet.ignOut4 = enabled
        DiagnosticOutput.IE -> packet.ie = enabled
        DiagnosticOutput.FE -> packet.fe = enabled
        DiagnosticOutput.ECF -> packet.ecf = enabled
        DiagnosticOutput.CE -> packet.ce = enabled
        DiagnosticOutput.ST_BLOCK -> packet.stBlock = enabled
        DiagnosticOutput.ADD_IO1 -> packet.addIo1 = enabled
        DiagnosticOutput.ADD_IO2 -> packet.addIo2 = enabled
        DiagnosticOutput.IGN_OUT5 -> packet.ignOut5 = enabled
        DiagnosticOutput.ECF0 -> packet.ecf0 = enabled
        DiagnosticOutput.INJ_O1 -> packet.injO1 = enabled
        DiagnosticOutput.INJ_O2 -> packet.injO2 = enabled
        DiagnosticOutput.INJ_O3 -> packet.injO3 = enabled
        DiagnosticOutput.INJ_O4 -> packet.injO4 = enabled
        DiagnosticOutput.INJ_O5 -> packet.injO5 = enabled
        DiagnosticOutput.BL -> packet.bl = enabled
        DiagnosticOutput.DE -> packet.de = enabled
        DiagnosticOutput.STBL_O -> packet.stblO = enabled
        DiagnosticOutput.CEL_O -> packet.celO = enabled
        DiagnosticOutput.FPMP_O -> packet.fpmpO = enabled
        DiagnosticOutput.PWRR_O -> packet.pwrrO = enabled
        DiagnosticOutput.EVAP_O -> packet.evapO = enabled
        DiagnosticOutput.O2SH_O -> packet.o2shO = enabled
        DiagnosticOutput.COND_O -> packet.condO = enabled
        DiagnosticOutput.ADD_O2 -> packet.addO2 = enabled
        DiagnosticOutput.TACH_O -> packet.tachO = enabled
        DiagnosticOutput.GPA6_O -> packet.gpa6_O = enabled
    }
}

private val secu3TOutputs = listOf(
    DiagnosticOutput.IGN_OUT1, DiagnosticOutput.IGN_OUT2, DiagnosticOutput.IGN_OUT3, DiagnosticOutput.IGN_OUT4,
    DiagnosticOutput.IE, DiagnosticOutput.FE, DiagnosticOutput.ECF, DiagnosticOutput.CE, DiagnosticOutput.ST_BLOCK,
    DiagnosticOutput.ADD_IO1, DiagnosticOutput.ADD_IO2, DiagnosticOutput.BL, DiagnosticOutput.DE,
)

private val secu3IOutputs = listOf(
    DiagnosticOutput.IGN_OUT1, DiagnosticOutput.IGN_OUT2, DiagnosticOutput.IGN_OUT3, DiagnosticOutput.IGN_OUT4,
    DiagnosticOutput.IGN_OUT5, DiagnosticOutput.ECF0, DiagnosticOutput.INJ_O1, DiagnosticOutput.INJ_O2,
    DiagnosticOutput.INJ_O3, DiagnosticOutput.INJ_O4, DiagnosticOutput.INJ_O5, DiagnosticOutput.BL, DiagnosticOutput.DE,
    DiagnosticOutput.STBL_O, DiagnosticOutput.CEL_O, DiagnosticOutput.FPMP_O, DiagnosticOutput.PWRR_O, DiagnosticOutput.EVAP_O,
    DiagnosticOutput.O2SH_O, DiagnosticOutput.COND_O, DiagnosticOutput.ADD_O2, DiagnosticOutput.TACH_O, DiagnosticOutput.GPA6_O,
)

@Composable
internal fun DiagnosticsOutputsScreen(
    isSecu3T: Boolean?,
    outputBits: Int,
    enableBlDe: Boolean,
    enableTachO: Boolean,
    onOutputChanged: (DiagnosticOutput, Boolean) -> Unit,
) {
    var currentBits by remember(outputBits, enableBlDe, enableTachO) { mutableIntStateOf(outputBits) }
    val outputs = when (isSecu3T) {
        true -> secu3TOutputs
        false -> secu3IOutputs
        null -> emptyList()
    }.filter { it != DiagnosticOutput.TACH_O || enableTachO }
    Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        if (isSecu3T == null) return@Column
        val ignitionCount = if (isSecu3T) 4 else 5
        Text(stringResource(R.string.diag_outputs_ignition_group), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
        outputs.take(ignitionCount).forEach { output ->
            OutputToggle(output, currentBits, enabledFor(output, enableBlDe, enableTachO), onOutputChanged) { currentBits = it }
        }
        Text(
            stringResource(if (isSecu3T) R.string.diag_outputs_other_group else R.string.diag_outputs_injection_group),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
        )
        outputs.drop(ignitionCount).forEach { output ->
            OutputToggle(output, currentBits, enabledFor(output, enableBlDe, enableTachO), onOutputChanged) { currentBits = it }
        }
    }
}

private fun enabledFor(output: DiagnosticOutput, enableBlDe: Boolean, enableTachO: Boolean) = when (output) {
    DiagnosticOutput.BL, DiagnosticOutput.DE -> enableBlDe
    DiagnosticOutput.TACH_O -> enableTachO
    else -> true
}

@Composable
private fun OutputToggle(
    output: DiagnosticOutput,
    bits: Int,
    enabled: Boolean,
    onOutputChanged: (DiagnosticOutput, Boolean) -> Unit,
    updateBits: (Int) -> Unit,
) {
    val checked = bits and (1 shl output.bit) != 0
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(enabled = enabled) {
            val newValue = !checked
            onOutputChanged(output, newValue)
            var updated = if (newValue) bits or (1 shl output.bit) else bits and (1 shl output.bit).inv()
            output.secondBit?.let { bit -> updated = if (newValue) updated or (1 shl bit) else updated and (1 shl bit).inv() }
            updateBits(updated)
        }.padding(start = 8.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(output.label, modifier = Modifier.weight(1f), color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f))
        Checkbox(
            modifier = Modifier.testTag("output_${output.name}"),
            checked = checked,
            enabled = enabled,
            onCheckedChange = { newValue ->
                onOutputChanged(output, newValue)
                var updated = if (newValue) bits or (1 shl output.bit) else bits and (1 shl output.bit).inv()
                output.secondBit?.let { bit -> updated = if (newValue) updated or (1 shl bit) else updated and (1 shl bit).inv() }
                updateBits(updated)
            },
        )
    }
}
