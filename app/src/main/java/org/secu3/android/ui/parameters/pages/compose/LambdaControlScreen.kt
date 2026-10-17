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
import org.secu3.android.models.packets.out.params.LambdaParamPacket
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
    AIR_FLOW(R.string.lambda_par_air_flow_threshold_for_turning_heating_off_title, R.string.units_rxl, 0, 100f, 2_000_000f, 10f, true),
}

enum class LambdaFlag(@StringRes val title: Int) {
    HEATING_VOLTAGE(R.string.lambda_par_determine_heating_voltage_title),
    IDLING_CORRECTION(R.string.lambda_par_correction_on_idling_title),
    BEFORE_CRANKING(R.string.lambda_par_heating_before_cranking_title),
    MIX_SENSORS(R.string.lambda_par_mix_sensors_value_title),
}

data class LambdaValues(val numbers: Map<LambdaNumber, Float>, val flags: Map<LambdaFlag, Boolean>, val sensorType: Int, val channels: List<Boolean>) {
    fun value(n: LambdaNumber) = numbers.getValue(n)
    fun withValue(n: LambdaNumber, value: Float) = copy(numbers = numbers + (n to value))
    fun withFlag(flag: LambdaFlag, value: Boolean) = copy(flags = flags + (flag to value))

    companion object {
        fun from(p: LambdaParamPacket) = LambdaValues(
            mapOf(LambdaNumber.STROKES to p.strPerStp.toFloat(), LambdaNumber.MS to p.msPerStp.toFloat(), LambdaNumber.STEP_POS to p.stepSizeP,
                LambdaNumber.STEP_NEG to p.stepSizeM, LambdaNumber.LIMIT_POS to p.corrLimitP, LambdaNumber.LIMIT_NEG to p.corrLimitM,
                LambdaNumber.SWITCH to p.swtPoint, LambdaNumber.CTS to p.tempThrd, LambdaNumber.RPM to p.rpmThrd.toFloat(),
                LambdaNumber.DELAY to p.activDelay.toFloat(), LambdaNumber.DEADBAND to p.deadBand, LambdaNumber.STOICH to p.gdStoichval,
                LambdaNumber.HEAT_COLD to p.heatingTime0.toFloat(), LambdaNumber.HEAT_HOT to p.heatingTime1.toFloat(),
                LambdaNumber.HEAT_TEMP to p.temperThrd.toFloat(), LambdaNumber.HEAT_PWM to p.heatingAct, LambdaNumber.AIR_FLOW to p.aflowThrd),
            mapOf(LambdaFlag.HEATING_VOLTAGE to p.determineLambdaHeatingByVoltage, LambdaFlag.IDLING_CORRECTION to p.lambdaCorrectionOnIdling,
                LambdaFlag.BEFORE_CRANKING to p.heatingBeforeCranking, LambdaFlag.MIX_SENSORS to p.mixSenorsValue),
            p.senstype, (0..7).map { p.lambdaSelectedChanel and (1 shl it) != 0 },
        )
    }
}

fun applyLambdaNumber(p: LambdaParamPacket, n: LambdaNumber, raw: Float) {
    val v = raw.coerceIn(n.min, n.max)
    when (n) {
        LambdaNumber.STROKES -> p.strPerStp = v.toInt()
        LambdaNumber.MS -> p.msPerStp = (v.toInt() / 10) * 10
        LambdaNumber.STEP_POS -> p.stepSizeP = v
        LambdaNumber.STEP_NEG -> p.stepSizeM = v
        LambdaNumber.LIMIT_POS -> p.corrLimitP = v
        LambdaNumber.LIMIT_NEG -> p.corrLimitM = v
        LambdaNumber.SWITCH -> p.swtPoint = v
        LambdaNumber.CTS -> p.tempThrd = v
        LambdaNumber.RPM -> p.rpmThrd = v.toInt()
        LambdaNumber.DELAY -> p.activDelay = v.toInt()
        LambdaNumber.DEADBAND -> p.deadBand = v
        LambdaNumber.STOICH -> p.gdStoichval = v
        LambdaNumber.HEAT_COLD -> p.heatingTime0 = v.toInt()
        LambdaNumber.HEAT_HOT -> p.heatingTime1 = v.toInt()
        LambdaNumber.HEAT_TEMP -> p.temperThrd = v.toInt()
        LambdaNumber.HEAT_PWM -> p.heatingAct = v
        LambdaNumber.AIR_FLOW -> p.aflowThrd = v
    }
}

fun applyLambdaFlag(p: LambdaParamPacket, flag: LambdaFlag, checked: Boolean) {
    when (flag) {
        LambdaFlag.HEATING_VOLTAGE -> p.determineLambdaHeatingByVoltage = checked
        LambdaFlag.IDLING_CORRECTION -> p.lambdaCorrectionOnIdling = checked
        LambdaFlag.BEFORE_CRANKING -> p.heatingBeforeCranking = checked
        LambdaFlag.MIX_SENSORS -> p.mixSenorsValue = checked
    }
}

fun applyLambdaChannel(p: LambdaParamPacket, index: Int, checked: Boolean) {
    if (index in 0..7) p.lambdaSelectedChanel = p.lambdaSelectedChanel.let { bits -> if (checked) bits or (1 shl index) else bits and (1 shl index).inv() }
}

@Composable
fun LambdaControlScreen(values: LambdaValues?, sensors: List<String>, fuelInjectionEnabled: Boolean, onEdit: (LambdaNumber) -> Unit,
    onFlag: (LambdaFlag, Boolean) -> Unit, onSensorType: (Int) -> Unit, onChannel: (Int, Boolean) -> Unit) {
    val pageScrollState = rememberScrollState()
    if (values == null) { ParameterLoadingIndicator("lambda_loading"); return }
    ParameterPage(scrollState = pageScrollState) {
        ParameterSection {
            SensorSelector(values.sensorType, sensors, onSensorType)
            NumberRow(LambdaNumber.STROKES, values, onEdit)
            NumberRow(LambdaNumber.MS, values, onEdit, enabled = values.value(LambdaNumber.STROKES) == 0f)
            listOf(LambdaNumber.STEP_POS, LambdaNumber.STEP_NEG, LambdaNumber.LIMIT_POS, LambdaNumber.LIMIT_NEG,
                LambdaNumber.SWITCH, LambdaNumber.CTS, LambdaNumber.RPM, LambdaNumber.DELAY, LambdaNumber.DEADBAND).forEach { NumberRow(it, values, onEdit) }
            listOf(LambdaFlag.HEATING_VOLTAGE, LambdaFlag.IDLING_CORRECTION).forEach { FlagRow(it, values, onFlag) }
            NumberRow(LambdaNumber.STOICH, values, onEdit, enabled = fuelInjectionEnabled)
        }
        ParameterSection(stringResource(R.string.lambda_control_sensors_heating_title)) {
            listOf(LambdaNumber.HEAT_COLD, LambdaNumber.HEAT_HOT, LambdaNumber.HEAT_TEMP, LambdaNumber.HEAT_PWM, LambdaNumber.AIR_FLOW).forEach { NumberRow(it, values, onEdit) }
            FlagRow(LambdaFlag.BEFORE_CRANKING, values, onFlag)
        }
        ParameterSection(stringResource(R.string.lambda_par_select_an_oxygen_sensor)) {
            values.channels.chunked(4).forEachIndexed { rowIndex, rowChannels ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    rowChannels.forEachIndexed { indexInRow, checked ->
                        val i = rowIndex * 4 + indexInRow
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text((i + 1).toString())
                            androidx.compose.material3.Checkbox(checked, { onChannel(i, it) }, modifier = Modifier.testTag("lambda_channel_${i + 1}"))
                        }
                    }
                }
            }
            FlagRow(LambdaFlag.MIX_SENSORS, values, onFlag)
        }
}
}

@Composable private fun NumberRow(n: LambdaNumber, values: LambdaValues, onEdit: (LambdaNumber) -> Unit, enabled: Boolean = true) {
    val locale = LocalConfiguration.current.locales[0]
    if (enabled) ParameterValueRow(stringResource(n.title), stringResource(n.units), values.value(n), n.precision, "lambda_${n.name.lowercase()}") { onEdit(n) }
    else Column(Modifier.fillMaxWidth().testTag("lambda_${n.name.lowercase()}_disabled")) {
        ParameterTextValueRow(
            title = stringResource(n.title),
            value = String.format(locale, "%.${n.precision}f", values.value(n)) + " " + stringResource(n.units),
            testTag = "lambda_${n.name.lowercase()}_disabled-field",
            numberText = String.format(locale, "%.${n.precision}f", values.value(n)),
            units = stringResource(n.units),
            onClick = null,
        )
    }
}

@Composable private fun FlagRow(flag: LambdaFlag, values: LambdaValues, onFlag: (LambdaFlag, Boolean) -> Unit) =
    ParameterSwitchRow(stringResource(flag.title), values.flags[flag] == true, "lambda_${flag.name.lowercase()}") { onFlag(flag, it) }

@Composable private fun SensorSelector(index: Int, sensors: List<String>, onSelect: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val display = sensors.getOrNull(index) ?: "Code $index"
    Box {
        ParameterSelectionRow(stringResource(R.string.lambda_par_sensor_type_title), display, "lambda_sensor_type") { expanded = true }
        if (expanded) ParameterChoiceDialog(
            title = stringResource(R.string.lambda_par_sensor_type_title),
            choices = sensors.mapIndexed { i, sensor -> ParameterChoice(i, sensor, "lambda_sensor_option_$i") },
            onDismiss = { expanded = false },
            onSelect = onSelect,
        )
    }
}
