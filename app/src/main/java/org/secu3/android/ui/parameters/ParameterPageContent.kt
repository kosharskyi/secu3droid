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

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.secu3.android.R
import org.secu3.android.domain.parameters.ParameterCapabilities
import org.secu3.android.domain.parameters.MiscellaneousControl
import org.secu3.android.models.packets.out.params.InjctrParPacket
import org.secu3.android.models.packets.out.params.UniOutParamPacket
import org.secu3.android.domain.parameters.ParameterPage
import org.secu3.android.ui.parameters.pages.*
import kotlin.math.roundToInt
import org.secu3.android.ui.parameters.pages.compose.*
import org.secu3.android.ui.parameters.compose.ParameterReadError
import org.secu3.android.ui.messages.UiMessageDuration

/** Direct Compose rendering for every ViewModel-owned parameter page. */
@Composable
internal fun ParameterPageContent(
    page: ParameterPage,
    titleRes: Int,
    viewModel: ParamsViewModel,
    capabilities: ParameterCapabilities,
    isActive: Boolean,
    isActiveNow: () -> Boolean,
    onEdit: (ParameterEditRequest) -> Unit,
) {
    if (viewModel.failedPage == page) {
        ParameterReadError(onRetry = { viewModel.refreshPage(page) })
        return
    }
    val resources = LocalResources.current
    fun intEdit(title: Int, value: Int, step: Int, max: Int, min: Int, change: (Int) -> Unit) =
        onEdit(ParameterEditRequest.IntValue(resources.getString(title), value, min, max, step, change))
    fun floatEdit(title: Int, value: Float, step: Float, max: Float, min: Float, precision: Int, change: (Float) -> Unit) =
        onEdit(ParameterEditRequest.FloatValue(resources.getString(title), value, min, max, step, precision, change))

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
            }, h::updateFlag, capabilities = capabilities)
        }
        R.string.params_tab_angles_title -> {
            val h = viewModel.anglesStateHolder
            AnglesScreen(h.state, { n -> (h.state as? AnglesUiState.Data)?.values?.let { floatEdit(n.titleRes, it.value(n), n.step, n.max, n.min, n.precision) { v -> h.updateValue(n, v.toFloat()) } } }, h::updateFlag)
        }
        R.string.params_tab_idling_title -> {
            val h = viewModel.idlingStateHolder
            IdlingScreen(h.state, { n -> (h.state as? IdlingUiState.Data)?.values?.let { values -> if (n.integer) intEdit(n.titleRes, values.value(n).toInt(), n.step.toInt(), n.max.toInt(), n.min.toInt()) { h.updateValue(n, it.toFloat()) } else floatEdit(n.titleRes, values.value(n), n.step, n.max, n.min, n.precision) { h.updateValue(n, it.toFloat()) } } }, h::updateFlag, capabilities = capabilities)
        }
        R.string.params_tab_functions_title -> {
            val h = viewModel.functionsStateHolder
            FunctionsScreen(h.state, { n -> (h.state as? FunctionsUiState.Data)?.values?.let { floatEdit(n.titleRes, it.value(n), n.step, n.max, n.min, n.precision) { h.updateValue(n, it.toFloat()) } } }, h::updateFlag, { selector, index ->
                h.updateChoice(selector, index)
                if (selector == FunctionsChoices.Selector.LOAD_SOURCE && index == 4 && !capabilities.fuelInjection) {
                    h.engineDisplacement?.let { value ->
                        floatEdit(R.string.injctr_par_enjine_displacement_title, value, .0001f, 8f, .01f, 4, h::updateEngineDisplacement)
                    }
                }
            }, capabilities = capabilities)
        }
        R.string.params_tab_temperature_title -> {
            val h = viewModel.temperatureStateHolder
            TemperatureScreen(h.state, { n -> (h.state as? TemperatureUiState.Data)?.values?.let { v -> if (n.integer) intEdit(n.titleRes, v.value(n).roundToInt(), n.step.toInt(), n.max.toInt(), n.min.toInt()) { h.updateValue(n, it.toFloat()) } else floatEdit(n.titleRes, v.value(n), n.step, n.max, n.min, n.precision) { h.updateValue(n, it.toFloat()) } } }, h::updateFlag, capabilities = capabilities)
        }
        R.string.params_tab_fuel_cutoff_title -> {
            val h = viewModel.carburStateHolder
            CarburScreen(h.state, (1..6).map(Int::toString) + "no", { n -> (h.state as? CarburUiState.Data)?.values?.let { v -> if (n.integer) intEdit(n.title, v.numbers.getValue(n).toInt(), n.step.toInt(), n.max.toInt(), n.min.toInt()) { h.updateValue(n, it.toFloat()) } else floatEdit(n.title, v.numbers.getValue(n), n.step, n.max, n.min, n.precision) { h.updateValue(n, it.toFloat()) } } }, h::updateInvers, h::updateSelector, capabilities = capabilities)
        }
        R.string.params_tab_adc_errors_corr_title -> {
            val h = viewModel.adcCorrectionsStateHolder
            AdcErrorsCorrectionsScreen(h.state, { n -> (h.state as? AdcCorrectionsUiState.Data)?.values?.let { onEdit(ParameterEditRequest.FloatValue(n.title, it.value(n), n.min, n.max, n.step, n.precision) { h.updateValue(n, it.toFloat()) }) } }, h::updateCompMode, capabilities = capabilities)
        }
        R.string.params_tab_ckps_title -> {
            val values by viewModel.ckpsControl.values.collectAsStateWithLifecycle()
            CkpsScreen(values, { n -> values?.let { v -> if (n.integer) intEdit(n.title, n.value(v).toInt(), n.step.toInt(), n.max.toInt(), n.min.toInt()) { viewModel.editCkps(n, it.toFloat()) } else floatEdit(n.title, n.value(v), n.step, n.max, n.min, n.precision) { viewModel.editCkps(n, it.toFloat()) } } }, viewModel::toggleCkps, viewModel::selectCkpsChoice, capabilities = capabilities)
        }
        R.string.params_tab_knock_title -> {
            val h = viewModel.knockStateHolder
            KnockScreen(h.state, { n -> (h.state as? KnockUiState.Data)?.values?.let { v -> if (n == KnockNumber.DELAY) intEdit(n.title, v.value(n).toInt(), n.step.toInt(), n.max.toInt(), n.min.toInt()) { h.updateValue(n, it.toFloat()) } else floatEdit(n.title, v.value(n), n.step, n.max, n.min, n.precision) { h.updateValue(n, it.toFloat()) } } }, h::updateEnabled, h::updateChannel, h::updateSelector)
        }
        R.string.params_tab_miscellaneous_title -> {
            val control = viewModel.miscellaneousControl
            val values by control.values.collectAsStateWithLifecycle()
            val rates = MiscellaneousControl.BAUD_RATES.mapValues { it.value.toString() }
            MiscellaneousScreen(values, rates, { key ->
                val current = values ?: return@MiscellaneousScreen
                val parameter = when (key) {
                    MiscField.PERIOD -> Triple(R.string.miscel_period_title, current.period, MiscellaneousControl.PERIOD_MIN to MiscellaneousControl.PERIOD_MAX)
                    MiscField.CUTOFF -> Triple(R.string.miscel_ignition_cutoff_rpm_title, current.cutoff, MiscellaneousControl.CUTOFF_MIN to MiscellaneousControl.CUTOFF_MAX)
                    MiscField.START -> Triple(R.string.miscel_start_rel_to_tdc_title, current.start.roundToInt(), MiscellaneousControl.START_MIN to MiscellaneousControl.START_MAX)
                    MiscField.DURATION -> Triple(R.string.miscel_hall_output_delay_title, current.duration.roundToInt(), MiscellaneousControl.DURATION_MIN to MiscellaneousControl.DURATION_MAX)
                    MiscField.BEGIN -> Triple(R.string.miscel_evap_starting_air_flow_title, current.begin, current.beginBounds)
                    MiscField.END -> Triple(R.string.miscel_evap_ending_air_flow_title, current.end, current.endBounds)
                    MiscField.PWM0 -> Triple(R.string.miscel_pwmfrq1_title, current.pwm0, MiscellaneousControl.PWM_MIN to MiscellaneousControl.PWM_MAX)
                    MiscField.PWM1 -> Triple(R.string.miscel_pwmfrq2_title, current.pwm1, MiscellaneousControl.PWM_MIN to MiscellaneousControl.PWM_MAX)
                    MiscField.VSS -> Triple(R.string.miscel_num_vss_pulsed_per_dist_title, current.vss.roundToInt(), MiscellaneousControl.VSS_MIN to MiscellaneousControl.VSS_MAX)
                    MiscField.TIMEOUT -> error("Float parameter routed through integer editor")
                }
                val bounds = parameter.third
                if (bounds == null || bounds.first > bounds.second) viewModel.showUiMessage(R.string.miscel_evap_invalid_range, UiMessageDuration.LONG)
                else intEdit(parameter.first, parameter.second.coerceIn(bounds.first, bounds.second), if (key == MiscField.PERIOD) 10 else 1, bounds.second, bounds.first) { viewModel.editMiscellaneous(key, it.toInt()) }
            }, { values?.let { current -> floatEdit(R.string.miscel_fuel_pump_working_time_title, current.timeout, MiscellaneousControl.TIMEOUT_MIN, MiscellaneousControl.TIMEOUT_MAX, .1f, 1) { viewModel.editMiscellaneousTimeout(it.toFloat()) } } }, viewModel::selectMiscellaneousBaud, viewModel::toggleMiscellaneous, capabilities = capabilities)
        }
        R.string.params_tab_choke_control_title -> {
            val values by viewModel.chokeControl.values.collectAsStateWithLifecycle()
            ChokeScreen(values, { number ->
                values?.let { current ->
                    if (number == ChokeNumber.STEPS) {
                        intEdit(number.title, number.value(current).toInt(), 1, number.max.toInt(), number.min.toInt()) {
                            viewModel.editChoke(number, it.toFloat())
                        }
                    } else {
                        floatEdit(number.title, number.value(current), number.step, number.max, number.min, number.precision) {
                            viewModel.editChoke(number, it)
                        }
                    }
                }
            }, viewModel::toggleChoke, viewModel::selectChokeFrequency,
                { if (isActiveNow()) viewModel.stepChoke(it) }, { if (isActiveNow()) viewModel.flushChokeManual() },
                { if (isActiveNow()) viewModel.finishChokeManual() }, viewModel::cancelChokeManual,
                viewModel::toggleChokeTest, isActive = isActive, capabilities = capabilities)
        }
        R.string.params_tab_security_title -> {
            val values by viewModel.securityControl.values.collectAsStateWithLifecycle()
            var expanded by remember { mutableStateOf(false) }
            SecurityScreen(values, viewModel.securityName, viewModel.securityPassword,
                viewModel.securityKey0, viewModel.securityKey1,
                listOf("BC417", "BK3231", "BK3231S (JDY-31)", "BC352 (HC-05)", "BK3432", "BK3431S"), expanded, viewModel.securityError, viewModel::isSecurityKeyValid,
                { expanded = it }, viewModel::changeSecurityType, viewModel::changeSecurityBluetooth,
                viewModel::changeSecurityImmobilizer, viewModel::changeSecurityName, viewModel::changeSecurityPassword,
                { viewModel.changeSecurityKey(0, it) }, { viewModel.changeSecurityKey(1, it) }, viewModel::applySecurityCredentials)
        }
        R.string.params_tab_universal_outputs_title -> {
            val h = viewModel.universalOutputsStateHolder
            UniversalOutputsScreen(h.values, UniOutParamPacket.CONDITION.entries.associate { it.id to resources.getString(it.strId) },
                stringArrayResource(R.array.logical_functions).toList(), h::updateOutput, h::updatePairFunction)
        }
        R.string.params_tab_fuel_injection_title -> {
            val values by viewModel.fuelInjectionControl.values.collectAsStateWithLifecycle()
            FuelInjectionScreen(values, InjctrParPacket.InjConfig.entries.associate { it.id to resources.getString(it.strId) }, { number ->
                values?.let { current ->
                    if (number.integer) intEdit(number.title, number.value(current).roundToInt(), number.step.toInt(), number.max.toInt(), number.min.toInt()) { viewModel.editFuelInjection(number, it.toFloat()) }
                    else floatEdit(number.title, number.value(current), number.step, number.max, number.min, number.precision) { viewModel.editFuelInjection(number, it) }
                }
            }, viewModel::changeFuelInjectionConfig, viewModel::changeFuelInjectionPulses, viewModel::changeFuelInjectionAngleSpec, viewModel::changeFuelInjectionFlag, capabilities)
        }
        R.string.params_tab_lambda_control_title -> {
            val h = viewModel.lambdaControlStateHolder
            val sensorTypes = stringArrayResource(R.array.lambda_sensors_types).toList()
            LambdaControlScreen(h.values, sensorTypes, capabilities, { n -> h.values?.let { v -> if (n.integer) intEdit(n.title, v.value(n).toInt(), n.step.toInt(), n.max.toInt(), n.min.toInt()) { h.updateValue(n, it.toFloat()) } else floatEdit(n.title, v.value(n), n.step, n.max, n.min, n.precision) { h.updateValue(n, it.toFloat()) } } }, h::updateFlag, { h.updateSensorType(it, sensorTypes.size) }, { i, checked -> h.updateChannel(i, checked) })
        }
        R.string.params_tab_acceleration_title -> {
            val h = viewModel.accelerationStateHolder
            AccelerationScreen(h.values?.let(AccelerationUiState::Data) ?: AccelerationUiState.Loading, { n -> h.values?.let { v -> intEdit(n.titleRes, if (n == AccelerationNumber.COLD_MULTIPLIER) v.value(n).roundToInt() else v.value(n).toInt(), n.step.toInt(), n.max.toInt(), n.min.toInt()) { h.updateNumber(n, it.toFloat()) } } }, h::selectAeType, h::selectWallwetModel, h::setBalance, capabilities = capabilities)
        }
        R.string.params_tab_gas_dose_title -> {
            val values by viewModel.gasDoseControl.values.collectAsStateWithLifecycle()
            GasDoseScreen(values, capabilities, { n -> values?.let { v ->
                if (n.integer) intEdit(n.title, n.value(v).toInt(), n.step.toInt(), n.max.toInt(), n.min.toInt()) { viewModel.editGasDose(n, it.toFloat()) }
                else floatEdit(n.title, n.value(v), n.step, n.max, n.min, n.precision) { viewModel.editGasDose(n, it.toFloat()) }
            } }, viewModel::selectGasDoseFrequency, viewModel::setGasDoseMaximumFrequency,
                { if (isActiveNow()) viewModel.stepGasDose(it) }, viewModel::finishGasDoseManual, viewModel::cancelGasDoseManual,
                viewModel::toggleGasDoseTest, { if (isActiveNow()) viewModel.flushGasDoseManual() }, isActive = isActive)
        }
        R.string.params_tab_ltft_title -> {
            val h = viewModel.ltftStateHolder
            LtftScreen(h.uiState, { n -> (h.uiState as? LtftUiState.Data)?.values?.let { v -> if (n.integer) intEdit(n.titleRes, v.value(n).toInt(), n.step.toInt(), n.max.toInt(), n.min.toInt()) { h.edit(n, it.toFloat()) } else floatEdit(n.titleRes, v.value(n), n.step, n.max, n.min, n.precision) { h.edit(n, it.toFloat()) } } }, h::selectMode)
        }
        R.string.params_tab_el_throttle_title -> {
            val h = viewModel.dbwStateHolder
            DbwScreen(h.uiState, { n -> (h.uiState as? DbwUiState.Data)?.values?.let { v -> floatEdit(n.titleRes, v.value(n), n.step, n.max, n.min, n.precision) { h.edit(n, it.toFloat()) } } }, onRefresh = { viewModel.refreshPage(ParameterPage.EL_THROTTLE) })
        }
    }
}
