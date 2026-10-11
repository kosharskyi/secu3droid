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
package org.secu3.android.ui.parameters.pages.compose

import org.secu3.android.ui.compose.DropdownShape

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import org.secu3.android.ui.parameters.compose.ParameterLoadingIndicator
import org.secu3.android.R
import org.secu3.android.models.packets.out.params.CkpsParamPacket
import org.secu3.android.ui.parameters.compose.ParameterSwitchRow
import org.secu3.android.ui.parameters.compose.ParameterValueRow

private val tdcOptions = (12..55).toList()
private val cylinderOptions = listOf(2, 4, 6, 8)
private val mttfByMissingTeeth = listOf(0f, 1.5f, 2.5f, 3f, 4f, 5f, 6f)

enum class CkpsFlag {
    CKPS_EDGE,
    REFERENCE_EDGE,
    SPARK_RISING,
    MERGE_OUTPUTS,
    USE_CAM_REFERENCE,
}

enum class CkpsNumber(
    val title: Int,
    val units: Int,
    val precision: Int,
    val min: Float,
    val max: Float,
    val step: Float,
    val integer: Boolean = false,
) {
    WHEEL(R.string.ckps_cogs_number_title, R.string.units_empty, 0, 16f, 200f, 1f, true),
    MISSING(R.string.ckps_missing_cogs_number_title, R.string.units_empty, 0, 0f, 6f, 1f, true),
    PULSE(R.string.ckps_ignition_pulse_delay_title, R.string.units_empty, 0, 1f, 200f, 1f, true),
    HALL_WIDTH(R.string.ckps_hall_wnd_width_title, R.string.units_degrees_cs, 1, 0f, 120f, .1f),
    DEGREES(R.string.ckps_degrees_before_tdc_title, R.string.units_degrees_cs, 1, 30f, 120f, .1f),
    MTTF(R.string.ckps_missing_teeths_detection_factor_title, R.string.units_empty, 2, 1f, 8f, .01f),
}

data class CkpsValues(
    val numbers: Map<CkpsNumber, Float>,
    val ckpsEdge: Boolean,
    val refsEdge: Boolean,
    val risingSpark: Boolean,
    val mergeOuts: Boolean,
    val useCamRef: Boolean,
    val teethBtdc: Int,
    val cylinders: Int,
) {
    fun withNumber(number: CkpsNumber, value: Float) =
        copy(numbers = numbers + (number to value))

    companion object {
        fun from(packet: CkpsParamPacket) = CkpsValues(
            numbers = mapOf(
                CkpsNumber.WHEEL to packet.ckpsCogsNum.toFloat(),
                CkpsNumber.MISSING to packet.ckpsMissNum.toFloat(),
                CkpsNumber.PULSE to packet.ckpsIgnitCogs.toFloat(),
                CkpsNumber.HALL_WIDTH to packet.hallWndWidth,
                CkpsNumber.DEGREES to packet.hallDegreesBtdc,
                CkpsNumber.MTTF to packet.mttf,
            ),
            ckpsEdge = packet.ckpsEdge,
            refsEdge = packet.refsEdge,
            risingSpark = packet.risingSpark,
            mergeOuts = packet.mergeOuts,
            useCamRef = packet.useCamRef,
            teethBtdc = packet.ckpsCogsBtdc,
            cylinders = packet.ckpsEngineCyl,
        )
    }
}

fun applyCkpsNumber(packet: CkpsParamPacket, number: CkpsNumber, value: Float) {
    when (number) {
        CkpsNumber.WHEEL -> packet.ckpsCogsNum = value.toInt()
        CkpsNumber.MISSING -> {
            packet.ckpsMissNum = value.toInt()
            if (packet.ckpsMissNum > 0) {
                packet.mttf = mttfByMissingTeeth[packet.ckpsMissNum]
            }
        }
        CkpsNumber.PULSE -> packet.ckpsIgnitCogs = value.toInt()
        CkpsNumber.HALL_WIDTH -> packet.hallWndWidth = value
        CkpsNumber.DEGREES -> packet.hallDegreesBtdc = value
        CkpsNumber.MTTF -> packet.mttf = value
    }
}

fun applyCkpsFlag(packet: CkpsParamPacket, flag: CkpsFlag, value: Boolean) {
    when (flag) {
        CkpsFlag.CKPS_EDGE -> packet.ckpsEdge = value
        CkpsFlag.REFERENCE_EDGE -> packet.refsEdge = value
        CkpsFlag.SPARK_RISING -> packet.risingSpark = value
        CkpsFlag.MERGE_OUTPUTS -> packet.mergeOuts = value
        CkpsFlag.USE_CAM_REFERENCE -> packet.useCamRef = value
    }
}

fun applyCkpsChoice(packet: CkpsParamPacket, teeth: Boolean, value: Int): Boolean {
    if (teeth) {
        if (value !in tdcOptions) return false
        packet.ckpsCogsBtdc = value
    } else {
        if (value !in cylinderOptions) return false
        packet.ckpsEngineCyl = value
    }
    return true
}

sealed interface CkpsUiState {
    data object Loading : CkpsUiState
    data class Data(val values: CkpsValues) : CkpsUiState
}

@Composable
fun CkpsScreen(
    state: CkpsUiState,
    onEdit: (CkpsNumber) -> Unit,
    onFlag: (CkpsFlag, Boolean) -> Unit,
    onChoice: (Boolean, Int) -> Unit,
) {
    if (state is CkpsUiState.Loading) {
        ParameterLoadingIndicator("ckps_loading")
        return
    }

    val values = (state as CkpsUiState.Data).values
    var open by remember { mutableStateOf<Boolean?>(null) }

    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        EdgeGroup(stringResource(R.string.ckps_ckps_edge_title), values.ckpsEdge, "ckps_edge") {
            onFlag(CkpsFlag.CKPS_EDGE, it)
        }
        EdgeGroup(stringResource(R.string.ckps_ref_s_edge_title), values.refsEdge, "ckps_ref") {
            onFlag(CkpsFlag.REFERENCE_EDGE, it)
        }
        ParameterSwitchRow(
            stringResource(R.string.ckps_spark_on_rising_edge_for_cdi_title),
            values.risingSpark,
            "ckps_spark",
        ) { onFlag(CkpsFlag.SPARK_RISING, it) }
        ParameterSwitchRow(
            stringResource(R.string.ckps_merge_outputs_title),
            values.mergeOuts,
            "ckps_merge",
        ) { onFlag(CkpsFlag.MERGE_OUTPUTS, it) }

        CkpsNumber.entries.filter { it != CkpsNumber.MTTF }.forEach { n ->
            if (n == CkpsNumber.PULSE) {
                Choice(
                    title = stringResource(R.string.ckps_cogs_before_tdc_title),
                    value = selectorValue(values.teethBtdc, tdcOptions),
                    tag = "ckps_teeth_btdc",
                ) { open = true }
                Choice(
                    title = stringResource(R.string.ckps_engine_cylinders_title),
                    value = selectorValue(values.cylinders, cylinderOptions),
                    tag = "ckps_cylinders",
                ) { open = false }
            }

            ParameterValueRow(
                title = stringResource(n.title),
                units = stringResource(n.units),
                value = values.numbers.getValue(n),
                precision = n.precision,
                testTag = "ckps_${n.name.lowercase()}",
            ) { onEdit(n) }
        }

        ParameterSwitchRow(
            stringResource(R.string.ckps_use_cam_sensor_title),
            values.useCamRef,
            "ckps_cam",
        ) { onFlag(CkpsFlag.USE_CAM_REFERENCE, it) }
        ParameterValueRow(
            title = stringResource(CkpsNumber.MTTF.title),
            units = stringResource(CkpsNumber.MTTF.units),
            value = values.numbers.getValue(CkpsNumber.MTTF),
            precision = CkpsNumber.MTTF.precision,
            testTag = "ckps_mttf",
        ) { onEdit(CkpsNumber.MTTF) }
    }

    open?.let { teeth ->
        val options = if (teeth) tdcOptions else cylinderOptions
        AlertDialog(
            onDismissRequest = { open = null },
            title = {
                Text(
                    if (teeth) stringResource(R.string.ckps_cogs_before_tdc_title)
                    else stringResource(R.string.ckps_engine_cylinders_title),
                )
            },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    options.forEach { option ->
                        Text(
                            text = option.toString(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onChoice(teeth, option)
                                    open = null
                                }
                                .padding(12.dp)
                                .testTag("ckps_option_$option"),
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { open = null }) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
        )
    }
}

private fun selectorValue(value: Int, options: List<Int>): String =
    value.toString().takeIf { value in options } ?: "Code $value"

@Composable
private fun EdgeGroup(
    title: String,
    positive: Boolean,
    tag: String,
    onSelect: (Boolean) -> Unit,
) {
    Column {
        Text(title, style = MaterialTheme.typography.titleSmall)
        Row(verticalAlignment = Alignment.CenterVertically) {
            listOf(
                false to R.string.ckps_edge_negative,
                true to R.string.ckps_edge_positive,
            ).forEach { (value, label) ->
                Row(
                    modifier = Modifier
                        .clickable { onSelect(value) }
                        .testTag("${tag}_${if (value) "positive" else "negative"}"),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = value == positive, onClick = { onSelect(value) })
                    Text(stringResource(label))
                }
            }
        }
    }
}

@Composable
private fun Choice(title: String, value: String, tag: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) { role = Role.Button }
            .clickable(role = Role.Button, onClick = onClick)
            .testTag(tag),
        shape = DropdownShape,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodySmall)
                Text(value, Modifier.testTag("$tag-value"), style = MaterialTheme.typography.bodyLarge)
            }
            Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
        }
    }
}
