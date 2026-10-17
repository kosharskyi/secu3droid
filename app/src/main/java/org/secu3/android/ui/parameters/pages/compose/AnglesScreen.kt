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

import androidx.compose.foundation.rememberScrollState
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import org.secu3.android.ui.parameters.compose.ParameterLoadingIndicator
import org.secu3.android.R
import org.secu3.android.models.packets.out.params.AnglesParamPacket
import org.secu3.android.ui.parameters.compose.ParameterSwitchRow
import org.secu3.android.ui.parameters.compose.ParameterValueRow
import org.secu3.android.ui.parameters.compose.ParameterPage
import org.secu3.android.ui.parameters.compose.ParameterSection

enum class AnglesNumber(@StringRes val titleRes: Int, val unitRes: Int, val precision: Int, val min: Float, val max: Float, val step: Float) {
    MAX(R.string.angles_max_angle_title, R.string.units_degree, 2, -15f, 60f, .25f),
    MIN(R.string.angles_min_angle_title, R.string.units_degree, 2, -15f, 60f, .25f),
    DECREASE(R.string.angles_angle_decrement_step_title, R.string.units_degree_per_stroke, 3, 0f, 10f, .025f),
    INCREASE(R.string.angles_angle_increment_step_title, R.string.units_degree_per_stroke, 3, 0f, 10f, .025f),
    OCTANE(R.string.angles_octane_correction_title, R.string.units_degree, 2, -30f, 30f, .25f),
    SHIFT(R.string.angles_ign_timing_when_shifting_title, R.string.units_degree, 1, -15f, 55f, .1f),
}

enum class AnglesFlag(@StringRes val titleRes: Int) {
    ZERO(R.string.angles_zero_angle_title),
    WORKING_MAP(R.string.angles_always_use_ing_timing_map_title),
    MANUAL_CORRECTION(R.string.angles_apply_manual_timing_corr_title),
    ZERO_WITH_OCTANE(R.string.angles_zero_adv_correction_title),
}

data class AnglesValues(val numbers: Map<AnglesNumber, Float>, val flags: Map<AnglesFlag, Boolean>) {
    fun value(number: AnglesNumber) = numbers.getValue(number)
    fun withValue(number: AnglesNumber, value: Float) = copy(numbers = numbers + (number to value))
    fun withFlag(flag: AnglesFlag, checked: Boolean) = copy(flags = flags + (flag to checked))

    companion object {
        fun from(packet: AnglesParamPacket) = AnglesValues(
            mapOf(AnglesNumber.MAX to packet.maxAngle, AnglesNumber.MIN to packet.minAngle,
                AnglesNumber.DECREASE to packet.angleDecSpeed, AnglesNumber.INCREASE to packet.angleIncSpeed,
                AnglesNumber.OCTANE to packet.angleCorrection, AnglesNumber.SHIFT to packet.shift_ingtim),
            mapOf(AnglesFlag.ZERO to (packet.zeroAdvAngle > 0), AnglesFlag.WORKING_MAP to packet.alwaysUseIgnitionMap,
                AnglesFlag.MANUAL_CORRECTION to packet.applyManualTimingCorrOnIdl,
                AnglesFlag.ZERO_WITH_OCTANE to packet.zeroAdvAngleWithCorr),
        )
    }
}

fun applyAnglesValue(packet: AnglesParamPacket, number: AnglesNumber, value: Float) {
    when (number) {
        AnglesNumber.MAX -> packet.maxAngle = value
        AnglesNumber.MIN -> packet.minAngle = value
        AnglesNumber.DECREASE -> packet.angleDecSpeed = value
        AnglesNumber.INCREASE -> packet.angleIncSpeed = value
        AnglesNumber.OCTANE -> packet.angleCorrection = value
        AnglesNumber.SHIFT -> packet.shift_ingtim = value
    }
}

fun applyAnglesFlag(packet: AnglesParamPacket, flag: AnglesFlag, checked: Boolean) {
    when (flag) {
        AnglesFlag.ZERO -> packet.zeroAdvAngle = if (checked) 1 else 0
        AnglesFlag.WORKING_MAP -> packet.alwaysUseIgnitionMap = checked
        AnglesFlag.MANUAL_CORRECTION -> packet.applyManualTimingCorrOnIdl = checked
        AnglesFlag.ZERO_WITH_OCTANE -> packet.zeroAdvAngleWithCorr = checked
    }
}

sealed interface AnglesUiState {
    data object Loading : AnglesUiState
    data class Data(val values: AnglesValues) : AnglesUiState
}

@Composable
fun AnglesScreen(state: AnglesUiState, onEdit: (AnglesNumber) -> Unit, onFlagChanged: (AnglesFlag, Boolean) -> Unit) {
    val pageScrollState = rememberScrollState()
    when (state) {
        AnglesUiState.Loading -> ParameterLoadingIndicator("angles_loading")
        is AnglesUiState.Data -> ParameterPage(scrollState = pageScrollState) {
            ParameterSection {
                AnglesNumber.entries.filter { it != AnglesNumber.SHIFT }.forEach { number ->
                    ParameterValueRow(stringResource(number.titleRes), stringResource(number.unitRes), state.values.value(number), number.precision,
                        "angles_${number.name.lowercase()}", onClick = { onEdit(number) })
                    if (number == AnglesNumber.OCTANE) {
                        val flag = AnglesFlag.ZERO
                        ParameterSwitchRow(stringResource(flag.titleRes), state.values.flags.getValue(flag), "angles_flag_${flag.name.lowercase()}") {
                            onFlagChanged(flag, it)
                        }
                    }
                }
                AnglesNumber.SHIFT.let { number ->
                    ParameterValueRow(stringResource(number.titleRes), stringResource(number.unitRes), state.values.value(number), number.precision,
                        "angles_${number.name.lowercase()}", onClick = { onEdit(number) })
                }
                AnglesFlag.entries.filter { it != AnglesFlag.ZERO }.forEach { flag ->
                    ParameterSwitchRow(stringResource(flag.titleRes), state.values.flags.getValue(flag), "angles_flag_${flag.name.lowercase()}") {
                        onFlagChanged(flag, it)
                    }
                }
            }
        }
    }
}
