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

import org.secu3.android.domain.parameters.ParameterCapabilities
import org.secu3.android.domain.parameters.IdlingValues

import androidx.compose.foundation.rememberScrollState
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import org.secu3.android.ui.parameters.compose.ParameterLoadingIndicator
import org.secu3.android.ui.parameters.*
import org.secu3.android.R
import org.secu3.android.ui.parameters.compose.ParameterSwitchRow
import org.secu3.android.ui.parameters.compose.ParameterValueRow
import org.secu3.android.ui.parameters.compose.ParameterPage
import org.secu3.android.ui.parameters.compose.ParameterSection

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
    MAX_ANGLE(R.string.idlreg_maximal_angle_title, 0, 3, -15f, 30f, .025f),
    MIN_ANGLE(R.string.idlreg_minimal_angle_title, 0, 3, -15f, 30f, .025f),
    RPM(R.string.idlreg_target_rpm_title, R.string.units_rpm, 0, 0f, 1800f, 5f, true),
    DEAD_BAND(R.string.idlreg_rpm_sensitivity_title, R.string.units_rpm, 0, 0f, 500f, 1f, true),
    ON_TEMP(R.string.idlreg_turn_on_temp_title, R.string.units_degrees_celcius, 2, -40f, 300f, .2f),
    IAC_ADD(R.string.idlreg_iac_add_after_exit_title, R.string.units_percents, 1, 0f, 100f, .5f),
    RPM_ADD(R.string.idlreg_rpm_add_on_run_title, R.string.units_rpm, 0, 0f, 2000f, 10f, true),
    P0(R.string.idlreg_proportional_positive_title, R.string.units_coef, 3, 0f, 5f, .005f),
    I0(R.string.idlreg_integral_positive_title, R.string.units_coef, 3, 0f, 5f, .005f),
    P1(R.string.idlreg_proportional_negative_title, R.string.units_coef, 3, 0f, 5f, .005f),
    I1(R.string.idlreg_integral_negative_title, R.string.units_coef, 3, 0f, 5f, .005f),
    THRD1(R.string.idlreg_first_transient_threshold_title, R.string.units_coef, 2, 1f, 2.99f, .01f),
    THRD2(R.string.idlreg_second_transient_threshold_title, R.string.units_coef, 2, 1f, 2.99f, .01f),
    INTEGRATOR_RPM(R.string.idlreg_integrator_rpm_limit_title, R.string.units_rpm, 0, 0f, 1200f, 10f, true),
    MAP(R.string.idlreg_pressure_load_on_idling_title, R.string.units_pressure_kpa, 1, 0f, 100f, .1f),
    IAC_MIN(R.string.idlreg_min_iac_position_title, R.string.units_percents, 1, 0f, 100f, .5f),
    IAC_MAX(R.string.idlreg_max_iac_position_title, R.string.units_percents, 1, 0f, 100f, .5f),
    IAC_DB(R.string.idlreg_iac_dead_band_title, R.string.units_rpm, 0, 0f, 500f, 1f, true),
    D(R.string.idlreg_differential_title, R.string.units_coef, 3, 0f, 5f, .005f),
    IRR_LOAD(R.string.idlreg_rigidity_load_coef_title, R.string.units_coef, 2, 0f, 48f, .05f),
    IRR_RPM(R.string.idlreg_rigidity_rpm_coef_title, R.string.units_coef, 2, 0f, 48f, .05f),
    LOAD(R.string.idlreg_load_thrd_title, R.string.units_percents, 1, 0f, 500f, .1f);

    internal fun value(values: IdlingValues): Float = when (this) {
        IFAC1 -> values.iFac1
        IFAC2 -> values.iFac2
        MAX_ANGLE -> values.maxAngle
        MIN_ANGLE -> values.minAngle
        RPM -> values.targetRpm.toFloat()
        DEAD_BAND -> values.deadBandRpm.toFloat()
        ON_TEMP -> values.turnOnTemperature
        IAC_ADD -> values.iacAddAfterExit
        RPM_ADD -> values.rpmAddOnRun.toFloat()
        // Legacy UI tags keep their existing titles: P0/I0 are positive, P1/I1 negative.
        P0 -> values.proportionalPositive
        I0 -> values.integralPositive
        P1 -> values.proportionalNegative
        I1 -> values.integralNegative
        THRD1 -> values.transientThreshold1
        THRD2 -> values.transientThreshold2
        INTEGRATOR_RPM -> values.integratorRpmLimit.toFloat()
        MAP -> values.mapThreshold
        IAC_MIN -> values.iacMinimumPosition
        IAC_MAX -> values.iacMaximumPosition
        IAC_DB -> values.iacDeadBandRpm.toFloat()
        D -> values.differential
        IRR_LOAD -> values.rigidityLoad
        IRR_RPM -> values.rigidityRpm
        LOAD -> values.loadThreshold
    }
}
enum class IdlingFlag(@StringRes val titleRes: Int) {
    REGULATOR(R.string.idlreg_use_idle_regulator_title),
    ON_GAS(R.string.idlreg_use_idle_regulator_on_gas_title),
    PROPORTIONAL(R.string.idlreg_proportional_regulator_title),
    CLOSED_LOOP(R.string.idlreg_use_closed_loop_title),
    THRASSMAP(R.string.idlreg_use_table_title),
    CLOSED_LOOP_GAS(R.string.idlreg_use_closed_loop_on_gas_title),
    WORKS_IAC(R.string.idlreg_works_together_with_iac_reg_title);

    internal fun value(values: IdlingValues): Boolean = when (this) {
        REGULATOR -> values.regulatorEnabled
        ON_GAS -> values.regulatorOnGas
        PROPORTIONAL -> values.proportionalMode
        CLOSED_LOOP -> values.closedLoop
        THRASSMAP -> values.throttleAssistMap
        CLOSED_LOOP_GAS -> values.closedLoopOnGas
        WORKS_IAC -> values.worksWithIacRegulator
    }
}

@Composable
fun IdlingScreen(
    values: IdlingValues?,
    onEdit: (IdlingNumber) -> Unit,
    onFlagChanged: (IdlingFlag, Boolean) -> Unit,
    capabilities: ParameterCapabilities = ParameterCapabilities.All,
) {
    val pageScrollState = rememberScrollState()
    if (values == null) {
        ParameterLoadingIndicator("idling_loading")
        return
    }
    ParameterPage(scrollState = pageScrollState) {
            @Composable
            fun number(number: IdlingNumber) {
                ParameterValueRow(
                    title = stringResource(number.titleRes),
                    units = if (number.unitRes == 0) "" else stringResource(number.unitRes),
                    value = number.value(values),
                    precision = number.precision,
                    testTag = "idling_${number.name.lowercase()}",
                    enabled = when (number) {
                        IdlingNumber.IFAC1, IdlingNumber.IFAC2, IdlingNumber.MAX_ANGLE, IdlingNumber.MIN_ANGLE,
                        IdlingNumber.RPM, IdlingNumber.DEAD_BAND, IdlingNumber.ON_TEMP -> true
                        IdlingNumber.IAC_ADD -> capabilities.iacAdditionEnabled(values.throttleAssistMap)
                        else -> capabilities.fuelInjection
                    },
                    onClick = { onEdit(number) },
                )
            }

            @Composable
            fun flag(flag: IdlingFlag) {
                ParameterSwitchRow(
                    title = stringResource(flag.titleRes),
                    checked = flag.value(values),
                    testTag = "idling_flag_${flag.name.lowercase()}",
                    enabled = flag in setOf(IdlingFlag.REGULATOR, IdlingFlag.ON_GAS, IdlingFlag.PROPORTIONAL) || capabilities.fuelInjection,
                    onCheckedChange = { onFlagChanged(flag, it) },
                )
            }

            ParameterSection {
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
