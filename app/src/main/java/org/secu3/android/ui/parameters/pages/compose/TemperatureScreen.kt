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

import androidx.compose.foundation.rememberScrollState
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import org.secu3.android.ui.parameters.compose.ParameterLoadingIndicator
import org.secu3.android.ui.parameters.*
import org.secu3.android.R
import org.secu3.android.domain.parameters.TemperatureValues
import org.secu3.android.ui.parameters.compose.ParameterSwitchRow
import org.secu3.android.ui.parameters.compose.ParameterValueRow
import org.secu3.android.ui.parameters.compose.ParameterPage
import org.secu3.android.ui.parameters.compose.ParameterSection

enum class TemperatureNumber(
    @StringRes val titleRes: Int,
    @StringRes val unitRes: Int,
    val precision: Int,
    val min: Float,
    val max: Float,
    val step: Float,
    val integer: Boolean = false,
) {
    FAN_ON(R.string.temper_fan_on_title, R.string.units_degrees_celcius, 2, -10f, 125f, .2f),
    FAN_OFF(R.string.temper_fan_off_title, R.string.units_degrees_celcius, 2, -10f, 125f, .2f),
    PWM_FREQUENCY(R.string.temper_fan_pwm_freq_title, R.string.units_hertz, 0, 10f, 5000f, 1f, true),
    // The Android XML omitted these ranges; official Manager metadata defines 0..5 V at .01 V steps.
    // Source: SECU-3 Manager `sources/ParamDesk/Params/TemperPageDlg.cpp:210-220`.
    // https://github.com/ashabelnikov/secu3man/blob/master/sources/ParamDesk/Params/TemperPageDlg.cpp
    AC_ON(R.string.temper_cond_turn_on_threshold_title, R.string.units_volts, 4, 0f, 5f, .01f),
    AC_OFF(R.string.temper_cond_turn_off_threshold_title, R.string.units_volts, 4, 0f, 5f, .01f),
    AC_MIN_RPM(R.string.temper_cond_turn_on_min_rpm_threshold_title, R.string.units_rpm, 0, 500f, 3000f, 10f, true),
    FAN_TIMER(R.string.temper_fans_timer_title, R.string.units_sec, 0, 0f, 600f, 1f, true);

    fun value(values: TemperatureValues): Float = when (this) {
        FAN_ON -> values.fanOn
        FAN_OFF -> values.fanOff
        PWM_FREQUENCY -> values.pwmFrequency.toFloat()
        AC_ON -> values.conditionerOn
        AC_OFF -> values.conditionerOff
        AC_MIN_RPM -> values.conditionerMinRpm.toFloat()
        FAN_TIMER -> values.fanTimer
    }
}

enum class TemperatureFlag(@StringRes val titleRes: Int) {
    COOLANT_SENSOR(R.string.temper_use_temp_sensor_title),
    PWM_CONTROL(R.string.temper_use_pwm_title),
    SENSOR_CURVE_TABLE(R.string.temper_use_table_title);

    fun checked(values: TemperatureValues): Boolean = when (this) {
        COOLANT_SENSOR -> values.coolantSensor
        PWM_CONTROL -> values.pwmControl
        SENSOR_CURVE_TABLE -> values.sensorCurveTable
    }
}

@Composable
fun TemperatureScreen(
    values: TemperatureValues?,
    onEdit: (TemperatureNumber) -> Unit,
    onFlagChanged: (TemperatureFlag, Boolean) -> Unit,
    capabilities: ParameterCapabilities = ParameterCapabilities.All,
) {
    val pageScrollState = rememberScrollState()
    if (values == null) {
        ParameterLoadingIndicator("temperature_loading")
        return
    }
    ParameterPage(scrollState = pageScrollState) {
        ParameterSection {
            @Composable
            fun number(number: TemperatureNumber) = ParameterValueRow(
                title = stringResource(number.titleRes),
                units = stringResource(number.unitRes),
                value = number.value(values),
                precision = number.precision,
                testTag = "temperature_${number.name.lowercase()}",
                enabled = when (number) {
                    TemperatureNumber.PWM_FREQUENCY -> capabilities.fanPwm
                    TemperatureNumber.AC_ON, TemperatureNumber.AC_OFF -> capabilities.nonSecu3TFuelInjection
                    TemperatureNumber.AC_MIN_RPM -> capabilities.fuelInjection
                    else -> true
                },
                onClick = { onEdit(number) },
            )
            @Composable
            fun flag(flag: TemperatureFlag) = ParameterSwitchRow(
                title = stringResource(flag.titleRes),
                checked = flag.checked(values),
                testTag = "temperature_flag_${flag.name.lowercase()}",
                enabled = when (flag) {
                    TemperatureFlag.PWM_CONTROL -> capabilities.fanPwm
                    TemperatureFlag.SENSOR_CURVE_TABLE -> capabilities.tempCurve
                    else -> true
                },
                onCheckedChange = { onFlagChanged(flag, it) },
            )

            // Preserve the XML order, including the switches between fan thresholds and PWM frequency.
            number(TemperatureNumber.FAN_ON)
            number(TemperatureNumber.FAN_OFF)
            flag(TemperatureFlag.COOLANT_SENSOR)
            flag(TemperatureFlag.PWM_CONTROL)
            flag(TemperatureFlag.SENSOR_CURVE_TABLE)
            number(TemperatureNumber.PWM_FREQUENCY)
            number(TemperatureNumber.AC_ON)
            number(TemperatureNumber.AC_OFF)
            number(TemperatureNumber.AC_MIN_RPM)
            number(TemperatureNumber.FAN_TIMER)
        }
    }
}
