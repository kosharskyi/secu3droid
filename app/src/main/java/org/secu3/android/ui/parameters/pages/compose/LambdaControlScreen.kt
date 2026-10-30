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

import org.secu3.android.domain.parameters.LambdaValues
import org.secu3.android.domain.parameters.ParameterCapabilities
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.secu3.android.R
import org.secu3.android.ui.parameters.compose.ParameterLoadingIndicator
import org.secu3.android.ui.parameters.compose.ParameterPage
import org.secu3.android.ui.parameters.compose.ParameterSection
import org.secu3.android.ui.parameters.compose.ParameterSelectionRow
import org.secu3.android.ui.parameters.dialogs.ParameterChoice
import org.secu3.android.ui.parameters.dialogs.ParameterChoiceDialog
import org.secu3.android.ui.parameters.compose.ParameterTextValueRow
import org.secu3.android.ui.parameters.compose.ParameterSwitchRow
import org.secu3.android.ui.parameters.compose.ParameterValueRow

enum class LambdaNumber(@StringRes val title: Int, @StringRes val units: Int, val precision: Int, val min: Float, val max: Float, val step: Float, val integer: Boolean = false) {
    STROKES(R.string.lambda_par_strokes_per_step_title, R.string.units_num, 0, 0f, 255f, 1f, true),
    MS(R.string.lambda_par_ms_per_step_title, R.string.units_ms, 0, 0f, 2500f, 10f, true),
    STEP_POS(R.string.lambda_par_step_size_positive_title, R.string.units_percents, 2, 0f, 45f, .25f),
    STEP_NEG(R.string.lambda_par_step_size_negative_title, R.string.units_percents, 2, 0f, 45f, .25f),
    LIMIT_POS(R.string.lambda_par_correction_positive_limit_title, R.string.units_percents, 2, .5f, 100f, .5f),
    LIMIT_NEG(R.string.lambda_par_correction_negative_limit_title, R.string.units_percents, 2, .5f, 100f, .5f),
    SWITCH(R.string.lambda_par_switch_point_title, R.string.units_volts, 2, .1f, 5f, .01f),
    CTS(R.string.lambda_par_temp_threshold_title, R.string.units_degrees_celcius, 2, -40f, 300f, .25f),
    RPM(R.string.lambda_par_rpm_threshold_title, R.string.units_rpm, 0, 0f, 30000f, 50f, true),
    DELAY(R.string.lambda_par_activation_delay_title, R.string.units_sec, 0, 0f, 255f, 1f, true),
    DEADBAND(R.string.lambda_par_switch_point_deadband_title, R.string.units_volts, 2, 0f, 5f, .01f),
    STOICH(R.string.lambda_par_stoichiom_ratio_for_gas_title, R.string.units_afr, 1, 8f, 22f, .1f),
    HEAT_COLD(R.string.lambda_par_heating_time_without_pwm_on_cold_title, R.string.units_sec, 0, 0f, 255f, 1f, true),
    HEAT_HOT(R.string.lambda_par_heating_time_without_pwm_on_hot_title, R.string.units_sec, 0, 0f, 255f, 1f, true),
    HEAT_TEMP(R.string.lambda_par_hot_cold_temperature_threshold_title, R.string.units_degrees_celcius, 0, 0f, 255f, 1f, true),
    HEAT_PWM(R.string.lambda_par_turn_on_time_in_pwm_mode_title, R.string.units_sec, 2, 0f, 2.55f, .01f),
    AIR_FLOW(R.string.lambda_par_air_flow_threshold_for_turning_heating_off_title, R.string.units_rxl, 0, 100f, 2_000_000f, 10f, true);

    fun value(values: LambdaValues): Float = when (this) {
        STROKES -> values.strokesPerStep.toFloat()
        MS -> values.msPerStep.toFloat()
        STEP_POS -> values.positiveStep
        STEP_NEG -> values.negativeStep
        LIMIT_POS -> values.positiveLimit
        LIMIT_NEG -> values.negativeLimit
        SWITCH -> values.switchPoint
        CTS -> values.coolantThreshold
        RPM -> values.rpmThreshold.toFloat()
        DELAY -> values.activationDelay.toFloat()
        DEADBAND -> values.deadband
        STOICH -> values.stoichiometricRatio
        HEAT_COLD -> values.coldHeatingTime.toFloat()
        HEAT_HOT -> values.hotHeatingTime.toFloat()
        HEAT_TEMP -> values.heatingTemperature.toFloat()
        HEAT_PWM -> values.heatingPwmTime
        AIR_FLOW -> values.airFlowThreshold
    }
}

enum class LambdaFlag(@StringRes val title: Int) {
    HEATING_VOLTAGE(R.string.lambda_par_determine_heating_voltage_title),
    IDLING_CORRECTION(R.string.lambda_par_correction_on_idling_title),
    BEFORE_CRANKING(R.string.lambda_par_heating_before_cranking_title),
    MIX_SENSORS(R.string.lambda_par_mix_sensors_value_title);

    fun value(values: LambdaValues): Boolean = when (this) {
        HEATING_VOLTAGE -> values.heatingVoltage
        IDLING_CORRECTION -> values.idlingCorrection
        BEFORE_CRANKING -> values.beforeCranking
        MIX_SENSORS -> values.mixSensors
    }
}

@Composable
fun LambdaControlScreen(values: LambdaValues?, sensors: List<String>, capabilities: ParameterCapabilities, onEdit: (LambdaNumber) -> Unit,
    onFlag: (LambdaFlag, Boolean) -> Unit, onSensorType: (Int) -> Unit, onChannel: (Int, Boolean) -> Unit) {
    val pageScrollState = rememberScrollState()
    if (values == null) { ParameterLoadingIndicator("lambda_loading"); return }
    ParameterPage(scrollState = pageScrollState) {
        ParameterSection {
            SensorSelector(values.sensorTypeCode, sensors, capabilities.lambdaBaseEditing, onSensorType)
            NumberRow(LambdaNumber.STROKES, values, onEdit, enabled = capabilities.lambdaBaseEditing)
            NumberRow(LambdaNumber.MS, values, onEdit, enabled = capabilities.lambdaMsEditing(values.strokesPerStep))
            listOf(LambdaNumber.STEP_POS, LambdaNumber.STEP_NEG, LambdaNumber.LIMIT_POS, LambdaNumber.LIMIT_NEG,
                LambdaNumber.SWITCH, LambdaNumber.CTS, LambdaNumber.RPM, LambdaNumber.DELAY, LambdaNumber.DEADBAND).forEach { NumberRow(it, values, onEdit, enabled = capabilities.lambdaBaseEditing) }
            listOf(LambdaFlag.HEATING_VOLTAGE, LambdaFlag.IDLING_CORRECTION).forEach { FlagRow(it, values, onFlag, capabilities.lambdaBaseEditing) }
            NumberRow(LambdaNumber.STOICH, values, onEdit, enabled = capabilities.lambdaStoich)
        }
        ParameterSection(stringResource(R.string.lambda_control_sensors_heating_title)) {
            listOf(LambdaNumber.HEAT_COLD, LambdaNumber.HEAT_HOT, LambdaNumber.HEAT_TEMP, LambdaNumber.HEAT_PWM, LambdaNumber.AIR_FLOW).forEach { NumberRow(it, values, onEdit, enabled = capabilities.lambdaBaseEditing) }
            FlagRow(LambdaFlag.BEFORE_CRANKING, values, onFlag, capabilities.lambdaBaseEditing)
        }
        ParameterSection(stringResource(R.string.lambda_par_select_an_oxygen_sensor)) {
            values.selectedChannels.chunked(4).forEachIndexed { rowIndex, rowChannels ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    rowChannels.forEachIndexed { indexInRow, checked ->
                        val i = rowIndex * 4 + indexInRow
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text((i + 1).toString())
                            androidx.compose.material3.Checkbox(
                                checked,
                                { onChannel(i, it) },
                                enabled = capabilities.lambdaChannelSelection,
                                modifier = Modifier.testTag("lambda_channel_${i + 1}"),
                            )
                        }
                    }
                }
            }
            FlagRow(LambdaFlag.MIX_SENSORS, values, onFlag, capabilities.lambdaBaseEditing)
        }
}
}

@Composable private fun NumberRow(n: LambdaNumber, values: LambdaValues, onEdit: (LambdaNumber) -> Unit, enabled: Boolean = true) {
    val locale = LocalConfiguration.current.locales[0]
    if (enabled) ParameterValueRow(stringResource(n.title), stringResource(n.units), n.value(values), n.precision, "lambda_${n.name.lowercase()}") { onEdit(n) }
    else Column(Modifier.fillMaxWidth().testTag("lambda_${n.name.lowercase()}_disabled")) {
        ParameterTextValueRow(
            title = stringResource(n.title),
            value = String.format(locale, "%.${n.precision}f", n.value(values)) + " " + stringResource(n.units),
            testTag = "lambda_${n.name.lowercase()}_disabled-field",
            numberText = String.format(locale, "%.${n.precision}f", n.value(values)),
            units = stringResource(n.units),
            onClick = null,
        )
    }
}

@Composable private fun FlagRow(flag: LambdaFlag, values: LambdaValues, onFlag: (LambdaFlag, Boolean) -> Unit, enabled: Boolean) =
    ParameterSwitchRow(stringResource(flag.title), flag.value(values), "lambda_${flag.name.lowercase()}", enabled = enabled) { onFlag(flag, it) }

@Composable private fun SensorSelector(index: Int, sensors: List<String>, enabled: Boolean, onSelect: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val display = when (index) { 0, 1 -> sensors.getOrNull(index) ?: "Code $index"; else -> "Code $index" }
    Box {
        ParameterSelectionRow(stringResource(R.string.lambda_par_sensor_type_title), display, "lambda_sensor_type", enabled = enabled) { expanded = true }
        if (expanded) ParameterChoiceDialog(
            title = stringResource(R.string.lambda_par_sensor_type_title),
            choices = sensors.take(2).mapIndexed { i, sensor -> ParameterChoice(i, sensor, "lambda_sensor_option_$i") },
            onDismiss = { expanded = false },
            onSelect = onSelect,
        )
    }
}
