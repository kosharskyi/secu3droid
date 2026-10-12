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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.secu3.android.ui.parameters.compose.ParameterLoadingIndicator
import org.secu3.android.R
import org.secu3.android.models.packets.out.params.IdlingParamPacket
import org.secu3.android.ui.parameters.compose.ParameterSwitchRow
import org.secu3.android.ui.parameters.compose.ParameterValueRow

// XML order and editing limits are retained for the migrated controls.
enum class IdlingNumber(
    @StringRes val titleRes: Int,
    @StringRes val unitRes: Int,
    val precision: Int,
    val min: Float,
    val max: Float,
    val step: Float,
    val integer: Boolean = false,
) {
    IFAC1(R.string.idlreg_ifac1_title, 0, 2, 0f, 10f, .01f),
    IFAC2(R.string.idlreg_ifac2_title, 0, 2, 0f, 10f, .01f),
    MAX_ANGLE(R.string.idlreg_maximal_angle_title, 0, 2, -15f, 30f, .02f),
    MIN_ANGLE(R.string.idlreg_minimal_angle_title, 0, 2, -15f, 30f, .02f),
    RPM(R.string.idlreg_target_rpm_title, R.string.units_rpm, 0, 0f, 1800f, 5f, true),
    DEAD_BAND(R.string.idlreg_rpm_sensitivity_title, R.string.units_rpm, 0, 0f, 500f, 1f, true),
    ON_TEMP(R.string.idlreg_turn_on_temp_title, R.string.units_degrees_celcius, 2, 0f, 100f, .2f),
    IAC_ADD(R.string.idlreg_iac_add_after_exit_title, R.string.units_percents, 1, 0f, 100f, .5f),
    RPM_ADD(R.string.idlreg_rpm_add_on_run_title, R.string.units_rpm, 0, 0f, 2000f, 10f, true),
    P0(R.string.idlreg_proportional_positive_title, R.string.units_coef, 3, 0f, 5f, .005f),
    I0(R.string.idlreg_integral_positive_title, R.string.units_coef, 3, 0f, 5f, .005f),
    P1(R.string.idlreg_proportional_negative_title, R.string.units_coef, 3, 0f, 5f, .005f),
    I1(R.string.idlreg_integral_negative_title, R.string.units_coef, 3, 0f, 5f, .005f),
    THRD1(R.string.idlreg_first_transient_threshold_title, R.string.units_coef, 2, 1f, 2.99f, .01f),
    THRD2(R.string.idlreg_second_transient_threshold_title, R.string.units_coef, 2, 1f, 2.99f, .01f),
    INTEGRATOR_RPM(R.string.idlreg_integrator_rpm_limit_title, R.string.units_rpm, 0, 10f, 1200f, 10f, true),
    MAP(R.string.idlreg_pressure_load_on_idling_title, R.string.units_pressure_kpa, 1, 0f, 100f, .1f),
    IAC_MIN(R.string.idlreg_min_iac_position_title, R.string.units_percents, 1, 0f, 100f, .5f),
    IAC_MAX(R.string.idlreg_max_iac_position_title, R.string.units_percents, 1, 0f, 100f, .5f),
    IAC_DB(R.string.idlreg_iac_dead_band_title, R.string.units_rpm, 0, 0f, 500f, 1f, true),
    D(R.string.idlreg_differential_title, R.string.units_coef, 3, 0f, 5f, .005f),
    IRR_LOAD(R.string.idlreg_rigidity_load_coef_title, R.string.units_coef, 2, 0f, 48f, .05f),
    IRR_RPM(R.string.idlreg_rigidity_rpm_coef_title, R.string.units_coef, 2, 0f, 48f, .05f),
    LOAD(R.string.idlreg_load_thrd_title, R.string.units_percents, 1, 0f, 500f, .1f),
}

enum class IdlingFlag(@StringRes val titleRes: Int) {
    REGULATOR(R.string.idlreg_use_idle_regulator_title),
    ON_GAS(R.string.idlreg_use_idle_regulator_on_gas_title),
    PROPORTIONAL(R.string.idlreg_proportional_regulator_title),
    CLOSED_LOOP(R.string.idlreg_use_closed_loop_title),
    THRASSMAP(R.string.idlreg_use_table_title),
    CLOSED_LOOP_GAS(R.string.idlreg_use_closed_loop_on_gas_title),
    WORKS_IAC(R.string.idlreg_works_together_with_iac_reg_title),
}

data class IdlingValues(
    val numbers: Map<IdlingNumber, Float>,
    val flags: Map<IdlingFlag, Boolean>,
) {
    fun value(number: IdlingNumber) = numbers.getValue(number)
    fun withValue(number: IdlingNumber, value: Float) = copy(numbers = numbers + (number to value))
    fun withFlag(flag: IdlingFlag, checked: Boolean) = copy(flags = flags + (flag to checked))

    companion object {
        fun from(packet: IdlingParamPacket) = IdlingValues(
            numbers = mapOf(
                IdlingNumber.IFAC1 to packet.iFac1,
                IdlingNumber.IFAC2 to packet.iFac2,
                IdlingNumber.MAX_ANGLE to packet.idlregMaxAngle,
                IdlingNumber.MIN_ANGLE to packet.idlregMinAngle,
                IdlingNumber.RPM to packet.idlingRpm.toFloat(),
                IdlingNumber.DEAD_BAND to packet.minefr.toFloat(),
                IdlingNumber.ON_TEMP to packet.idlregTurnOnTemp,
                IdlingNumber.IAC_ADD to packet.idlToRunAdd,
                IdlingNumber.RPM_ADD to packet.rpmOnRunAdd.toFloat(),
                IdlingNumber.P0 to packet.idlRegP0,
                IdlingNumber.I0 to packet.idlRegI0,
                IdlingNumber.P1 to packet.idlRegP1,
                IdlingNumber.I1 to packet.idlRegI1,
                IdlingNumber.THRD1 to packet.coefThrd1,
                IdlingNumber.THRD2 to packet.coefThrd2,
                IdlingNumber.INTEGRATOR_RPM to packet.integratorRpmLim.toFloat(),
                IdlingNumber.MAP to packet.mapValue,
                IdlingNumber.IAC_MIN to packet.iacMinPos,
                IdlingNumber.IAC_MAX to packet.iacMaxPos,
                IdlingNumber.IAC_DB to packet.iacRegDb.toFloat(),
                IdlingNumber.D to packet.idlRegD,
                IdlingNumber.IRR_LOAD to packet.irrKLoad,
                IdlingNumber.IRR_RPM to packet.irrKRpm,
                IdlingNumber.LOAD to packet.loadIdlThrd,
            ),
            flags = mapOf(
                IdlingFlag.REGULATOR to packet.useRegulator,
                IdlingFlag.ON_GAS to packet.useRegulatorOnGas,
                IdlingFlag.PROPORTIONAL to packet.pRegMode,
                IdlingFlag.CLOSED_LOOP to packet.useClosedLoop,
                IdlingFlag.THRASSMAP to packet.useThrassmap,
                IdlingFlag.CLOSED_LOOP_GAS to packet.useClosedLoopOnGas,
                IdlingFlag.WORKS_IAC to packet.idlRegWorksWithIAC,
            ),
        )
    }
}

fun applyIdlingValue(packet: IdlingParamPacket, number: IdlingNumber, value: Float) {
    when (number) {
        IdlingNumber.IFAC1 -> packet.iFac1 = value
        IdlingNumber.IFAC2 -> packet.iFac2 = value
        IdlingNumber.MAX_ANGLE -> packet.idlregMaxAngle = value
        IdlingNumber.MIN_ANGLE -> packet.idlregMinAngle = value
        IdlingNumber.RPM -> packet.idlingRpm = value.toInt()
        IdlingNumber.DEAD_BAND -> packet.minefr = value.toInt()
        IdlingNumber.ON_TEMP -> packet.idlregTurnOnTemp = value
        IdlingNumber.IAC_ADD -> packet.idlToRunAdd = value
        IdlingNumber.RPM_ADD -> packet.rpmOnRunAdd = value.toInt()
        IdlingNumber.P0 -> packet.idlRegP0 = value
        IdlingNumber.I0 -> packet.idlRegI0 = value
        IdlingNumber.P1 -> packet.idlRegP1 = value
        IdlingNumber.I1 -> packet.idlRegI1 = value
        IdlingNumber.THRD1 -> packet.coefThrd1 = value
        IdlingNumber.THRD2 -> packet.coefThrd2 = value
        IdlingNumber.INTEGRATOR_RPM -> packet.integratorRpmLim = value.toInt()
        IdlingNumber.MAP -> packet.mapValue = value
        IdlingNumber.IAC_MIN -> packet.iacMinPos = value
        IdlingNumber.IAC_MAX -> packet.iacMaxPos = value
        IdlingNumber.IAC_DB -> packet.iacRegDb = value.toInt()
        IdlingNumber.D -> packet.idlRegD = value
        IdlingNumber.IRR_LOAD -> packet.irrKLoad = value
        IdlingNumber.IRR_RPM -> packet.irrKRpm = value
        IdlingNumber.LOAD -> packet.loadIdlThrd = value
    }
}

fun applyIdlingFlag(packet: IdlingParamPacket, flag: IdlingFlag, checked: Boolean) {
    when (flag) {
        IdlingFlag.REGULATOR -> packet.useRegulator = checked
        IdlingFlag.ON_GAS -> packet.useRegulatorOnGas = checked
        IdlingFlag.PROPORTIONAL -> packet.pRegMode = checked
        IdlingFlag.CLOSED_LOOP -> packet.useClosedLoop = checked
        IdlingFlag.THRASSMAP -> packet.useThrassmap = checked
        IdlingFlag.CLOSED_LOOP_GAS -> packet.useClosedLoopOnGas = checked
        IdlingFlag.WORKS_IAC -> packet.idlRegWorksWithIAC = checked
    }
}

sealed interface IdlingUiState {
    data object Loading : IdlingUiState
    data class Data(val values: IdlingValues) : IdlingUiState
}

@Composable
fun IdlingScreen(
    state: IdlingUiState,
    onEdit: (IdlingNumber) -> Unit,
    onFlagChanged: (IdlingFlag, Boolean) -> Unit,
) {
    when (state) {
        IdlingUiState.Loading -> ParameterLoadingIndicator("idling_loading")
        is IdlingUiState.Data -> Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            @Composable
            fun number(number: IdlingNumber) {
                ParameterValueRow(
                    title = stringResource(number.titleRes),
                    units = if (number.unitRes == 0) "" else stringResource(number.unitRes),
                    value = state.values.value(number),
                    precision = number.precision,
                    testTag = "idling_${number.name.lowercase()}",
                    onClick = { onEdit(number) },
                )
            }

            @Composable
            fun flag(flag: IdlingFlag) {
                ParameterSwitchRow(
                    title = stringResource(flag.titleRes),
                    checked = state.values.flags.getValue(flag),
                    testTag = "idling_flag_${flag.name.lowercase()}",
                    onCheckedChange = { onFlagChanged(flag, it) },
                )
            }

            // First regulator gain and angle groups.
            number(IdlingNumber.IFAC1)
            number(IdlingNumber.IFAC2)
            number(IdlingNumber.MAX_ANGLE)
            number(IdlingNumber.MIN_ANGLE)
            number(IdlingNumber.RPM)
            number(IdlingNumber.DEAD_BAND)
            number(IdlingNumber.ON_TEMP)

            flag(IdlingFlag.REGULATOR)
            flag(IdlingFlag.ON_GAS)
            flag(IdlingFlag.PROPORTIONAL)
            flag(IdlingFlag.CLOSED_LOOP)

            number(IdlingNumber.IAC_ADD)
            flag(IdlingFlag.THRASSMAP)

            // Regulator coefficients, transient thresholds, then IAC controls.
            number(IdlingNumber.RPM_ADD)
            number(IdlingNumber.P0)
            number(IdlingNumber.I0)
            number(IdlingNumber.P1)
            number(IdlingNumber.I1)
            number(IdlingNumber.THRD1)
            number(IdlingNumber.THRD2)
            number(IdlingNumber.INTEGRATOR_RPM)
            number(IdlingNumber.MAP)
            number(IdlingNumber.IAC_MIN)
            number(IdlingNumber.IAC_MAX)
            number(IdlingNumber.IAC_DB)

            flag(IdlingFlag.CLOSED_LOOP_GAS)
            flag(IdlingFlag.WORKS_IAC)

            number(IdlingNumber.D)
            number(IdlingNumber.IRR_LOAD)
            number(IdlingNumber.IRR_RPM)
            number(IdlingNumber.LOAD)
        }
    }
}
