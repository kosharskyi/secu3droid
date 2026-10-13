/*
 *    SecuDroid  - An open source, free manager for SECU-3 engine control unit
 *    Copyright (C) 2024 Vitalii O. Kosharskyi. Ukraine, Kyiv
 *
 *    SECU-3  - An open source, free engine control unit
 *    Copyright (C) 2007-2024 Alexey A. Shabelnikov. Ukraine, Kyiv
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
package org.secu3.android.ui.parameters

import android.widget.Toast
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import org.secu3.android.R
import org.secu3.android.models.packets.out.params.InjctrParPacket
import org.secu3.android.models.packets.out.params.UniOutParamPacket
import org.secu3.android.ui.parameters.pages.*
import org.secu3.android.ui.parameters.pages.compose.*

/** Direct Compose rendering for every ViewModel-owned parameter page. */
@Composable
internal fun ParameterPageContent(
    titleRes: Int,
    viewModel: ParamsViewModel,
    isActive: Boolean,
    isActiveNow: () -> Boolean,
    onEdit: (ParameterEditRequest) -> Unit,
) {
    val context = LocalContext.current
    fun intEdit(title: Int, value: Int, step: Int, max: Int, min: Int, change: (Int) -> Unit) =
        onEdit(ParameterEditRequest.IntValue(context.getString(title), value, min, max, step, change))
    fun floatEdit(title: Int, value: Float, step: Float, max: Float, min: Float, precision: Int, change: (Float) -> Unit) =
        onEdit(ParameterEditRequest.FloatValue(context.getString(title), value, min, max, step, precision, change))

    when (titleRes) {
        R.string.params_tab_start_title -> {
            val h = viewModel.starterStateHolder
            StarterScreen(h.state, { n ->
                val v = (h.state as? StarterUiState.Data)?.values ?: return@StarterScreen
                if (n in setOf(StarterNumber.STARTER_OFF, StarterNumber.SMAP_ABANDON, StarterNumber.AFTERSTART_PETROL, StarterNumber.AFTERSTART_GAS, StarterNumber.STBL_COUNTDOWN)) {
                    intEdit(n.titleRes, v.value(n).toInt(), n.step.toInt(), n.max.toInt(), n.min.toInt()) { h.updateValue(n, it.toFloat()) }
                } else {
                    floatEdit(n.titleRes, v.value(n), n.step, n.max, n.min, n.precision) { h.updateValue(n, it) }
                }
            }, h::updateFlag)
        }
        R.string.params_tab_angles_title -> {
            val h = viewModel.anglesStateHolder
            AnglesScreen(h.state, { n -> (h.state as? AnglesUiState.Data)?.values?.let { floatEdit(n.titleRes, it.value(n), n.step, n.max, n.min, n.precision) { v -> h.updateValue(n, v.toFloat()) } } }, h::updateFlag)
        }
        R.string.params_tab_idling_title -> {
            val h = viewModel.idlingStateHolder
            IdlingScreen(h.state, { n -> (h.state as? IdlingUiState.Data)?.values?.let { values -> if (n.integer) intEdit(n.titleRes, values.value(n).toInt(), n.step.toInt(), n.max.toInt(), n.min.toInt()) { h.updateValue(n, it.toFloat()) } else floatEdit(n.titleRes, values.value(n), n.step, n.max, n.min, n.precision) { h.updateValue(n, it.toFloat()) } } }, h::updateFlag)
        }
        R.string.params_tab_functions_title -> {
            val h = viewModel.functionsStateHolder
            FunctionsScreen(h.state, { n -> (h.state as? FunctionsUiState.Data)?.values?.let { floatEdit(n.titleRes, it.value(n), n.step, n.max, n.min, n.precision) { h.updateValue(n, it.toFloat()) } } }, h::updateFlag, h::updateChoice)
        }
        R.string.params_tab_temperature_title -> {
            val h = viewModel.temperatureStateHolder
            TemperatureScreen(h.state, { n -> (h.state as? TemperatureUiState.Data)?.values?.let { v -> if (n.integer) intEdit(n.titleRes, v.value(n).toInt(), n.step.toInt(), n.max.toInt(), n.min.toInt()) { h.updateValue(n, it.toFloat()) } else floatEdit(n.titleRes, v.value(n), n.step, n.max, n.min, n.precision) { h.updateValue(n, it.toFloat()) } } }, h::updateFlag)
        }
        R.string.params_tab_fuel_cutoff_title -> {
            val h = viewModel.carburStateHolder
            CarburScreen(h.state, (1..6).map(Int::toString) + "no", { n -> (h.state as? CarburUiState.Data)?.values?.let { v -> if (n.integer) intEdit(n.title, v.numbers.getValue(n).toInt(), n.step.toInt(), n.max.toInt(), n.min.toInt()) { h.updateValue(n, it.toFloat()) } else floatEdit(n.title, v.numbers.getValue(n), n.step, n.max, n.min, n.precision) { h.updateValue(n, it.toFloat()) } } }, h::updateInvers, h::updateSelector)
        }
        R.string.params_tab_adc_errors_corr_title -> {
            val h = viewModel.adcCorrectionsStateHolder
            AdcErrorsCorrectionsScreen(h.state, { n -> (h.state as? AdcCorrectionsUiState.Data)?.values?.let { onEdit(ParameterEditRequest.FloatValue(n.title, it.value(n), n.min, n.max, n.step, n.precision) { h.updateValue(n, it.toFloat()) }) } }, h::updateCompMode)
        }
        R.string.params_tab_ckps_title -> {
            val h = viewModel.ckpsStateHolder
            CkpsScreen(h.state, { n -> (h.state as? CkpsUiState.Data)?.values?.let { v -> if (n.integer) intEdit(n.title, v.numbers.getValue(n).toInt(), n.step.toInt(), n.max.toInt(), n.min.toInt()) { h.updateNumber(n, it.toFloat()) } else floatEdit(n.title, v.numbers.getValue(n), n.step, n.max, n.min, n.precision) { h.updateNumber(n, it.toFloat()) } } }, h::updateFlag, h::updateChoice)
        }
        R.string.params_tab_knock_title -> {
            val h = viewModel.knockStateHolder
            KnockScreen(h.state, { n -> (h.state as? KnockUiState.Data)?.values?.let { v -> if (n == KnockNumber.DELAY) intEdit(n.title, v.value(n).toInt(), n.step.toInt(), n.max.toInt(), n.min.toInt()) { h.updateValue(n, it.toFloat()) } else floatEdit(n.title, v.value(n), n.step, n.max, n.min, n.precision) { h.updateValue(n, it.toFloat()) } } }, h::updateEnabled, h::updateChannel, h::updateSelector)
        }
        R.string.params_tab_miscellaneous_title -> {
            val h = viewModel.miscellaneousStateHolder
            MiscellaneousScreen(h.state, h.hasValidationError, BAUD_RATE_CODES, { key ->
                val p = h.currentPacket ?: return@MiscellaneousScreen
                val parameter = when (key) {
                    MiscField.PERIOD -> Triple(R.string.miscel_period_title, p.uartPeriodTms, 0 to 500)
                    MiscField.CUTOFF -> Triple(R.string.miscel_ignition_cutoff_rpm_title, p.ignCutoffThrd, 1000 to 30000)
                    MiscField.START -> Triple(R.string.miscel_start_rel_to_tdc_title, p.hopStartAng, -6 to 12)
                    MiscField.DURATION -> Triple(R.string.miscel_hall_output_delay_title, p.hopDuratAng, 1 to 30)
                    MiscField.BEGIN -> Triple(R.string.miscel_evap_starting_air_flow_title, p.evapAfbegin, evapEditBounds(MiscField.BEGIN, p.evapAfbegin, p.evapAfEnd))
                    MiscField.END -> Triple(R.string.miscel_evap_ending_air_flow_title, p.evapAfEnd, evapEditBounds(MiscField.END, p.evapAfbegin, p.evapAfEnd))
                    MiscField.PWM0 -> Triple(R.string.miscel_pwmfrq1_title, p.pwmFrq0, 10 to 5000)
                    MiscField.PWM1 -> Triple(R.string.miscel_pwmfrq2_title, p.pwmFrq1, 10 to 5000)
                    MiscField.VSS -> Triple(R.string.miscel_num_vss_pulsed_per_dist_title, p.vssPeriodDist, 100 to 60000)
                    MiscField.TIMEOUT -> error("Float parameter routed through integer editor")
                }
                val bounds = parameter.third
                if (bounds == null || bounds.first > bounds.second) Toast.makeText(context, R.string.miscel_evap_invalid_range, Toast.LENGTH_LONG).show()
                else intEdit(parameter.first, parameter.second.coerceIn(bounds.first, bounds.second), if (key == MiscField.PERIOD) 10 else 1, bounds.second, bounds.first) { h.editInt(key, it.toInt()) }
            }, { h.currentPacket?.let { floatEdit(R.string.miscel_fuel_pump_working_time_title, it.fpTimeoutStrt, .1f, 25.5f, .1f, 1) { h.editFloat(it.toFloat()) } } }, h::selectBaud, h::toggle)
        }
        R.string.params_tab_choke_control_title -> {
            val h = viewModel.chokeControlStateHolder
            ChokeScreen(h.values, { n -> h.values?.let { v -> if (n == ChokeNumber.STEPS) intEdit(n.title, v.value(n).toInt(), 1, n.max.toInt(), n.min.toInt()) { h.edit(n, it.toFloat()) } else floatEdit(n.title, v.value(n), n.step, n.max, n.min, n.precision) { h.edit(n, it.toFloat()) } } }, h::toggle, h::selectFrequency,
                { if (isActiveNow()) h.manualStep(it) }, { if (isActiveNow()) h.flushManual() }, { if (isActiveNow()) h.finishManual() }, h::cancelManual, h::toggleTest, isActive = isActive)
        }
        R.string.params_tab_security_title -> {
            val h = viewModel.securityStateHolder
            var expanded by remember { mutableStateOf(false) }
            SecurityScreen(h.loaded, h.useBt, h.useImm, h.name, h.password, h.key0, h.key1, h.type,
                listOf("BC417", "BK3231", "BK3231S (JDY-31)", "BC352 (HC-05)", "BK3432", "BK3431S"), expanded, h.error,
                h.useReserveParams, h.checkFirmwareCrc, { expanded = it }, h::changeType, h::changeBt, h::changeImm,
                h::changeName, h::changePass, h::changeKey0, h::changeKey1, h::applyCredentials)
        }
        R.string.params_tab_universal_outputs_title -> {
            val h = viewModel.universalOutputsStateHolder
            UniversalOutputsScreen(h.values, UniOutParamPacket.CONDITION.entries.associate { it.id to context.getString(it.strId) },
                context.resources.getStringArray(R.array.logical_functions).toList(), h::updateOutput, h::updatePairFunction)
        }
        R.string.params_tab_fuel_injection_title -> {
            val h = viewModel.fuelInjectionStateHolder
            FuelInjectionScreen(h.uiState, InjctrParPacket.InjConfig.entries.associate { it.id to context.getString(it.strId) }, { n ->
                h.editSpec(n)?.let { s -> if (s.integer) intEdit(s.title, s.value.toInt(), s.step.toInt(), s.max.toInt(), s.min.toInt()) { h.edit(n, it.toFloat()) } else floatEdit(s.title, s.value, s.step, s.max, s.min, s.precision) { h.edit(n, it.toFloat()) } }
            }, h::changeConfig, h::changePulses, h::changeAngleSpec, h::changeFlag)
        }
        R.string.params_tab_lambda_control_title -> {
            val h = viewModel.lambdaControlStateHolder
            val sensorTypes = stringArrayResource(R.array.lambda_sensors_types).toList()
            LambdaControlScreen(h.values, sensorTypes, viewModel.fwInfoPacket?.isFuelInjectEnabled == true, { n -> h.values?.let { v -> if (n.integer) intEdit(n.title, v.value(n).toInt(), n.step.toInt(), n.max.toInt(), n.min.toInt()) { h.updateValue(n, it.toFloat()) } else floatEdit(n.title, v.value(n), n.step, n.max, n.min, n.precision) { h.updateValue(n, it.toFloat()) } } }, h::updateFlag, { h.updateSensorType(it, sensorTypes.size) }, { i, checked -> h.updateChannel(i, checked) })
        }
        R.string.params_tab_acceleration_title -> {
            val h = viewModel.accelerationStateHolder
            AccelerationScreen(h.values?.let(AccelerationUiState::Data) ?: AccelerationUiState.Loading, { n -> h.values?.let { v -> intEdit(n.titleRes, v.value(n).toInt(), n.step.toInt(), n.max.toInt(), n.min.toInt()) { h.updateNumber(n, it.toFloat()) } } }, h::selectAeType, h::selectWallwetModel, h::setBalance)
        }
        R.string.params_tab_gas_dose_title -> {
            val h = viewModel.gasDoseStateHolder
            GasDoseScreen(h.values, h.isFirmwareAvailable, { n -> h.values?.let { v -> if (n.integer) intEdit(n.title, v.value(n).toInt(), n.step.toInt(), n.max.toInt(), n.min.toInt()) { h.edit(n, it.toFloat()) } else floatEdit(n.title, v.value(n), n.step, n.max, n.min, n.precision) { h.edit(n, it.toFloat()) } } }, h::selectFrequency, h::setMaximumFrequency,
                { if (isActiveNow()) h.manualStep(it) }, { if (isActiveNow()) h.finishManual() }, h::cancelManual, h::toggleTest, { if (isActiveNow()) h.flushManual() }, isActive = isActive)
        }
        R.string.params_tab_ltft_title -> {
            val h = viewModel.ltftStateHolder
            LtftScreen(h.uiState, { n -> (h.uiState as? LtftUiState.Data)?.values?.let { v -> if (n.integer) intEdit(n.titleRes, v.value(n).toInt(), n.step.toInt(), n.max.toInt(), n.min.toInt()) { h.edit(n, it.toFloat()) } else floatEdit(n.titleRes, v.value(n), n.step, n.max, n.min, n.precision) { h.edit(n, it.toFloat()) } } }, h::selectMode)
        }
        R.string.params_tab_el_throttle_title -> {
            val h = viewModel.dbwStateHolder
            DbwScreen(h.uiState, { n -> (h.uiState as? DbwUiState.Data)?.values?.let { v -> floatEdit(n.titleRes, v.value(n), n.step, n.max, n.min, n.precision) { h.edit(n, it.toFloat()) } } })
        }
    }
}
