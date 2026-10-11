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

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.secu3.android.R
import org.secu3.android.models.packets.out.params.LtftParamPacket
import org.secu3.android.ui.parameters.compose.ParameterLoadingIndicator
import org.secu3.android.ui.parameters.compose.ParameterValueRow

enum class LtftNumber(
    @StringRes val titleRes: Int,
    @StringRes val unitRes: Int,
    val precision: Int,
    val min: Float,
    val max: Float,
    val step: Float,
    val integer: Boolean = false,
) {
    LOWER_CLT(R.string.ltft_par_lower_clt_threshold_title, R.string.units_degrees_celcius, 2, 0f, 270f, .25f),
    UPPER_CLT(R.string.ltft_par_upper_clt_threshold_title, R.string.units_degrees_celcius, 2, 0f, 270f, .25f),
    UPPER_IAT(R.string.ltft_par_upper_iat_threshold_title, R.string.units_degrees_celcius, 2, 0f, 270f, .25f),
    LEARNING_GRADIENT(R.string.ltft_par_learning__gradient_factor_title, R.string.units_empty, 3, 0f, .99f, .005f),
    ABS_GAS_PRESSURE(R.string.ltft_par_abs_gas_pressure_treshold_title, R.string.units_pressure_kpa, 1, 0f, 500f, .1f),
    DIFF_GAS_PRESSURE(R.string.ltft_par_diff_gas_pressure_treshold_title, R.string.units_pressure_kpa, 1, 0f, 500f, .1f),
    BOTTOM_LIMIT(R.string.ltft_par_bottom_limit_correction_title, R.string.units_percents, 1, -24.6f, 0f, .1f),
    TOP_LIMIT(R.string.ltft_par_top_limit_correction_title, R.string.units_percents, 1, 0f, 24.6f, .1f),
    LOWER_RPM(R.string.ltft_par_lower_rpm_threshold_title, R.string.units_rpm, 0, 0f, 25000f, 10f, true),
    UPPER_RPM(R.string.ltft_par_upper_rpm_threshold_title, R.string.units_rpm, 0, 0f, 25000f, 10f, true),
    LOWER_LOAD(R.string.ltft_par_lower_load_threshold_title, R.string.units_pressure_kpa, 2, 0f, 500f, 1f),
    UPPER_LOAD(R.string.ltft_par_upper_load_threshold_title, R.string.units_pressure_kpa, 2, 0f, 500f, 1f),
    NEGATIVE_INSENSITIVITY(R.string.ltft_par_negative_insensitivity_threshold_title, R.string.units_percents, 1, 0f, 10f, .1f),
    POSITIVE_INSENSITIVITY(R.string.ltft_par_positive_insensitivity_threshold_title, R.string.units_percents, 1, 0f, 10f, .1f),
}

data class LtftValues(val numbers: Map<LtftNumber, Float>, val mode: Int) {
    fun value(number: LtftNumber) = numbers.getValue(number)
    fun withValue(number: LtftNumber, value: Float) = copy(numbers = numbers + (number to value))

    companion object {
        fun from(packet: LtftParamPacket) = LtftValues(
            numbers = mapOf(
                LtftNumber.LOWER_CLT to packet.learnClt,
                LtftNumber.UPPER_CLT to packet.learnCltUp,
                LtftNumber.UPPER_IAT to packet.learnIatUp,
                LtftNumber.LEARNING_GRADIENT to packet.learnGrad,
                LtftNumber.ABS_GAS_PRESSURE to packet.learnGpa,
                LtftNumber.DIFF_GAS_PRESSURE to packet.learnGpd,
                LtftNumber.BOTTOM_LIMIT to packet.min,
                LtftNumber.TOP_LIMIT to packet.max,
                LtftNumber.LOWER_RPM to packet.learnRpm0.toFloat(),
                LtftNumber.UPPER_RPM to packet.learnRpm1.toFloat(),
                LtftNumber.LOWER_LOAD to packet.learnLoad0,
                LtftNumber.UPPER_LOAD to packet.learnLoad1,
                LtftNumber.NEGATIVE_INSENSITIVITY to packet.deadBand0,
                LtftNumber.POSITIVE_INSENSITIVITY to packet.deadBand1,
            ),
            mode = packet.mode,
        )
    }
}

fun applyLtftValue(packet: LtftParamPacket, number: LtftNumber, value: Float) {
    when (number) {
        LtftNumber.LOWER_CLT -> packet.learnClt = value
        LtftNumber.UPPER_CLT -> packet.learnCltUp = value
        LtftNumber.UPPER_IAT -> packet.learnIatUp = value
        LtftNumber.LEARNING_GRADIENT -> packet.learnGrad = value
        LtftNumber.ABS_GAS_PRESSURE -> packet.learnGpa = value
        LtftNumber.DIFF_GAS_PRESSURE -> packet.learnGpd = value
        LtftNumber.BOTTOM_LIMIT -> packet.min = value
        LtftNumber.TOP_LIMIT -> packet.max = value
        LtftNumber.LOWER_RPM -> packet.learnRpm0 = value.toInt()
        LtftNumber.UPPER_RPM -> packet.learnRpm1 = value.toInt()
        LtftNumber.LOWER_LOAD -> packet.learnLoad0 = value
        LtftNumber.UPPER_LOAD -> packet.learnLoad1 = value
        LtftNumber.NEGATIVE_INSENSITIVITY -> packet.deadBand0 = value
        LtftNumber.POSITIVE_INSENSITIVITY -> packet.deadBand1 = value
    }
}

sealed interface LtftUiState {
    data object Loading : LtftUiState
    data class Data(val values: LtftValues) : LtftUiState
}

@Composable
fun LtftScreen(state: LtftUiState, onEdit: (LtftNumber) -> Unit, onMode: (Int) -> Unit) {
    when (state) {
        LtftUiState.Loading -> ParameterLoadingIndicator("ltft_loading")
        is LtftUiState.Data -> {
            val modes = stringArrayResource(R.array.ltft_mode_items)
            Column(
                Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                LtftModeSelector(modes, state.values.mode, onMode)
                LtftNumber.entries.forEach { number ->
                    ParameterValueRow(
                        title = stringResource(number.titleRes),
                        units = stringResource(number.unitRes),
                        value = state.values.value(number),
                        precision = number.precision,
                        testTag = "ltft_${number.name.lowercase()}",
                        onClick = { onEdit(number) },
                    )
                }
            }
        }
    }
}

@Composable
private fun LtftModeSelector(modes: Array<String>, mode: Int, onMode: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val selected = modes.getOrNull(mode) ?: "Unknown ($mode)"
    Column {
        Surface(
            modifier = Modifier.fillMaxWidth().clickable { expanded = true }.testTag("ltft_mode"),
            shape = RoundedCornerShape(4.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        ) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.ltft_par_mode_title), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(selected, Modifier.testTag("ltft_mode_value").padding(top = 4.dp), style = MaterialTheme.typography.bodyLarge)
                }
                Icon(Icons.Filled.ArrowDropDown, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            modes.forEachIndexed { index, item ->
                DropdownMenuItem(
                    text = { Text(item) },
                    modifier = Modifier.testTag("ltft_mode_option_$index"),
                    onClick = { expanded = false; onMode(index) },
                )
            }
        }
    }
}
