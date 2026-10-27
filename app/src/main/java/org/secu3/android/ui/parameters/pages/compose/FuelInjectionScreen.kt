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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.secu3.android.R
import org.secu3.android.domain.parameters.FuelBankValues
import org.secu3.android.domain.parameters.FuelInjectionControl
import org.secu3.android.domain.parameters.FuelInjectionValues
import org.secu3.android.domain.parameters.ParameterCapabilities
import org.secu3.android.ui.parameters.compose.ParameterLoadingIndicator
import org.secu3.android.ui.parameters.compose.ParameterPage
import org.secu3.android.ui.parameters.compose.ParameterSection
import org.secu3.android.ui.parameters.compose.ParameterSelectionRow
import org.secu3.android.ui.parameters.dialogs.ParameterAlertDialog
import org.secu3.android.ui.parameters.dialogs.ParameterCancelButton
import org.secu3.android.ui.parameters.dialogs.ParameterChoiceList
import org.secu3.android.ui.parameters.dialogs.ParameterChoiceOption
import org.secu3.android.ui.parameters.compose.ParameterTextValueRow
import org.secu3.android.ui.parameters.compose.ParameterSwitchRow
import kotlin.math.roundToInt

enum class FuelInjectionNumber(@StringRes val title: Int, val units: String, val precision: Int, val min: Float, val max: Float, val step: Float, val integer: Boolean = false) {
    ENGINE_DISPLACEMENT(R.string.injctr_par_enjine_displacement_title, "L", 4, FuelInjectionControl.ENGINE_DISPLACEMENT_MIN, FuelInjectionControl.ENGINE_DISPLACEMENT_MAX, .0001f),
    FLOW_0(R.string.injctr_par_injector_flow_rate_title, "cc/min", 2, FuelInjectionControl.FLOW_MIN, FuelInjectionControl.FLOW_MAX, .02f),
    TIMING_0(R.string.injctr_par_injection_timing_title, "°", 0, FuelInjectionControl.TIMING_MIN, FuelInjectionControl.TIMING_MAX, 1f, true),
    CRANKING_0(R.string.injctr_par_cranking_injection_timing_title, "°", 0, FuelInjectionControl.TIMING_MIN, FuelInjectionControl.TIMING_MAX, 1f, true),
    MIN_PW_0(R.string.injctr_par_min_inj_pw_title, "ms", 2, FuelInjectionControl.MINIMUM_PULSE_WIDTH_MIN, FuelInjectionControl.MINIMUM_PULSE_WIDTH_MAX, .025f),
    MAX_PW_0(R.string.injctr_par_max_inj_pw_title, "ms", 2, FuelInjectionControl.MAXIMUM_PULSE_WIDTH_MIN, FuelInjectionControl.MAXIMUM_PULSE_WIDTH_MAX, .1f),
    FLOW_1(R.string.injctr_par_injector_flow_rate_title, "cc/min", 2, FuelInjectionControl.FLOW_MIN, FuelInjectionControl.FLOW_MAX, .02f),
    TIMING_1(R.string.injctr_par_injection_timing_title, "°", 0, FuelInjectionControl.TIMING_MIN, FuelInjectionControl.TIMING_MAX, 1f, true),
    CRANKING_1(R.string.injctr_par_cranking_injection_timing_title, "°", 0, FuelInjectionControl.TIMING_MIN, FuelInjectionControl.TIMING_MAX, 1f, true),
    MIN_PW_1(R.string.injctr_par_min_inj_pw_title, "ms", 2, FuelInjectionControl.MINIMUM_PULSE_WIDTH_MIN, FuelInjectionControl.MINIMUM_PULSE_WIDTH_MAX, .025f),
    MAX_PW_1(R.string.injctr_par_max_inj_pw_title, "ms", 2, FuelInjectionControl.MAXIMUM_PULSE_WIDTH_MIN, FuelInjectionControl.MAXIMUM_PULSE_WIDTH_MAX, .1f),
    PULSES_PER_LITER(R.string.injctr_par_pulses_per_litter_of_fuel, "", 0, FuelInjectionControl.PULSES_PER_LITER_MIN, FuelInjectionControl.PULSES_PER_LITER_MAX, 1f, true),
    DECREASE_RATE(R.string.injctr_par_inj_pw_dec_speed_title, "ms", 2, FuelInjectionControl.RATE_MIN, FuelInjectionControl.RATE_MAX, .01f),
    INCREASE_RATE(R.string.injctr_par_inj_pw_inc_speed_title, "ms", 2, FuelInjectionControl.RATE_MIN, FuelInjectionControl.RATE_MAX, .01f);

    fun value(values: FuelInjectionValues): Float = when (this) {
        ENGINE_DISPLACEMENT -> values.engineDisplacement
        FLOW_0 -> values.banks[0].flow
        TIMING_0 -> values.banks[0].timing
        CRANKING_0 -> values.banks[0].cranking
        MIN_PW_0 -> values.banks[0].minPw
        MAX_PW_0 -> values.banks[0].maxPw
        FLOW_1 -> values.banks[1].flow
        TIMING_1 -> values.banks[1].timing
        CRANKING_1 -> values.banks[1].cranking
        MIN_PW_1 -> values.banks[1].minPw
        MAX_PW_1 -> values.banks[1].maxPw
        PULSES_PER_LITER -> values.pulsesPerLiter
        DECREASE_RATE -> values.decreaseRate
        INCREASE_RATE -> values.increaseRate
    }
}
enum class FuelInjectionFlag { USE_MAP_0, USE_MAP_1, ADDITIONAL_CORRECTIONS, AIR_DENSITY, DIFFERENTIAL_PRESSURE, SWITCH_ROWS, FULL_SEQUENTIAL_AFTER_START }

@Composable
fun FuelInjectionScreen(state: FuelInjectionValues?, configLabels: Map<Int, String>, onEdit: (FuelInjectionNumber) -> Unit,
                        onConfig: (Int, Int) -> Unit, onPulses: (Int, Int) -> Unit,
                        onAngleSpec: (Int, Int) -> Unit, onFlag: (FuelInjectionFlag, Boolean) -> Unit,
                        capabilities: ParameterCapabilities = ParameterCapabilities.All) {
    val pageScrollState = rememberScrollState()
    val data = state ?: run { ParameterLoadingIndicator("fuel_injection_loading"); return }
    val timings = stringArrayResource(R.array.inj_timing_specifies).toList()
    var choice by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    ParameterPage(scrollState = pageScrollState) {
        if (!capabilities.fuelInjection) {
            Text(stringResource(R.string.fuel_injection_unavailable), Modifier.testTag("fuel_injection_disabled").padding(bottom = 8.dp))
        } else {
            ParameterSection(stringResource(R.string.injctr_par_primary_fuel_title)) {
                NumberRow(FuelInjectionNumber.ENGINE_DISPLACEMENT, data.engineDisplacement, "inj-engine-displacement", onEdit = onEdit)
                BankSection(0, data.banks[0], configLabels, timings, onEdit, { bank, _ -> choice = bank to 0 }, { bank, _ -> choice = bank to 1 }, { bank, _ -> choice = bank to 2 }, onFlag)
            }
            ParameterSection(stringResource(R.string.injctr_par_params_for_second_fuel_title)) {
                BankSection(1, data.banks[1], configLabels, timings, onEdit, { bank, _ -> choice = bank to 0 }, { bank, _ -> choice = bank to 1 }, { bank, _ -> choice = bank to 2 }, onFlag)
            }
            ParameterSection(stringResource(R.string.injctr_par_general_title)) {
                NumberRow(FuelInjectionNumber.PULSES_PER_LITER, data.pulsesPerLiter, "inj-pulses-per-liter", onEdit = onEdit)
                ParameterSwitchRow(stringResource(R.string.injctr_par_additional_corrections_title), data.additionalCorrections, "inj-additional-corrections", capabilities.dbw) { onFlag(FuelInjectionFlag.ADDITIONAL_CORRECTIONS, it) }
                ParameterSwitchRow(stringResource(R.string.injctr_par_use_air_density_correction_map_title), data.airDensity, "inj-air-density") { onFlag(FuelInjectionFlag.AIR_DENSITY, it) }
                ParameterSwitchRow(stringResource(R.string.injctr_par_diff_press_for_pw_corr_from_gps_title), data.differentialPressure, "inj-differential-pressure", capabilities.dbw) { onFlag(FuelInjectionFlag.DIFFERENTIAL_PRESSURE, it) }
                ParameterSwitchRow(stringResource(R.string.injctr_par_switch_between_injectors_rows_title), data.switchRows, "inj-switch-rows", capabilities.dbw) { onFlag(FuelInjectionFlag.SWITCH_ROWS, it) }
                ParameterSwitchRow(stringResource(R.string.injctr_par_full_sequential_after_start_title), data.fullSequentialAfterStart, "inj-full-sequential-after-start") { onFlag(FuelInjectionFlag.FULL_SEQUENTIAL_AFTER_START, it) }
                NumberRow(FuelInjectionNumber.DECREASE_RATE, data.decreaseRate, "inj-decrease-rate", onEdit = onEdit)
                NumberRow(FuelInjectionNumber.INCREASE_RATE, data.increaseRate, "inj-increase-rate", onEdit = onEdit)
            }
        }
    }
    choice?.let { (bank, kind) ->
        val current = data.banks[bank]
        val items: List<Pair<Int, String>> = when (kind) {
            0 -> current.configOptions.mapNotNull { id -> configLabels[id]?.let { id to it } }
            1 -> current.pulseOptions.map { it to it.toString() }
            else -> timings.mapIndexed { index, label -> index to label }
        }
        val title = when (kind) {
            0 -> stringResource(R.string.injctr_par_injector_config_title)
            1 -> stringResource(R.string.injctr_par_number_of_squirts_per_cycle_title)
            else -> stringResource(R.string.injctr_par_inj_timing_specifies)
        }
        ParameterAlertDialog(onDismissRequest = { choice = null }, title = { Text(title) },
            text = { ParameterChoiceList {
                items.forEach { (id, label) -> ParameterChoiceOption(label, "inj-choice-$bank-$kind-$id") {
                    when (kind) { 0 -> onConfig(bank, id); 1 -> onPulses(bank, id); else -> onAngleSpec(bank, id) }
                    choice = null
                } }
            } }, confirmButton = { ParameterCancelButton { choice = null } })
    }
}

@Composable private fun BankSection(bank: Int, value: FuelBankValues, labels: Map<Int, String>, timings: List<String>, onEdit: (FuelInjectionNumber) -> Unit,
    onConfig: (Int, Int) -> Unit, onPulses: (Int, Int) -> Unit, onAngle: (Int, Int) -> Unit, onFlag: (FuelInjectionFlag, Boolean) -> Unit) {
    val flowNumber = if (bank == 0) FuelInjectionNumber.FLOW_0 else FuelInjectionNumber.FLOW_1
    val timingNumber = if (bank == 0) FuelInjectionNumber.TIMING_0 else FuelInjectionNumber.TIMING_1
    val crankingNumber = if (bank == 0) FuelInjectionNumber.CRANKING_0 else FuelInjectionNumber.CRANKING_1
    val minPulseNumber = if (bank == 0) FuelInjectionNumber.MIN_PW_0 else FuelInjectionNumber.MIN_PW_1
    val maxPulseNumber = if (bank == 0) FuelInjectionNumber.MAX_PW_0 else FuelInjectionNumber.MAX_PW_1
    NumberRow(flowNumber, value.flow, "inj-flow-$bank", onEdit = onEdit)
    val configTitle = stringResource(R.string.injctr_par_injector_config_title)
    ChoiceRow(configTitle, labels[value.config] ?: "Unknown configuration (${value.config})", "inj-config-$bank") { onConfig(bank, value.config) }
    ChoiceRow(stringResource(R.string.injctr_par_number_of_squirts_per_cycle_title), value.pulses.toString(), "inj-pulses-$bank", enabled = value.pulseOptions.isNotEmpty()) { onPulses(bank, value.pulses) }
    NumberRow(timingNumber, value.timing, "inj-timing-$bank", enabled = !value.useMap, onEdit = onEdit)
    NumberRow(crankingNumber, value.cranking, "inj-cranking-$bank", onEdit = onEdit)
    ParameterSwitchRow(stringResource(R.string.injctr_par_use_map_for_inj_timing_title), value.useMap, "inj-use-map-$bank") { onFlag(if (bank == 0) FuelInjectionFlag.USE_MAP_0 else FuelInjectionFlag.USE_MAP_1, it) }
    ChoiceRow(stringResource(R.string.injctr_par_inj_timing_specifies), timings.getOrNull(value.angleSpec) ?: "Code ${value.angleSpec}", "inj-angle-spec-$bank") { onAngle(bank, value.angleSpec) }
    NumberRow(minPulseNumber, value.minPw, "inj-min-pw-$bank", onEdit = onEdit)
    NumberRow(maxPulseNumber, value.maxPw, "inj-max-pw-$bank", onEdit = onEdit)
}

@Composable private fun NumberRow(number: FuelInjectionNumber, value: Float, tag: String, enabled: Boolean = true, onEdit: (FuelInjectionNumber) -> Unit) {
    val locale = LocalConfiguration.current.locales[0]
    val displayedValue = if (number.precision == 0) value.roundToInt().toFloat() else value
    ParameterTextValueRow(
        title = stringResource(number.title),
        value = String.format(locale, "%.${number.precision}f", displayedValue) + if (number.units.isEmpty()) "" else " ${number.units}",
        testTag = tag,
        enabled = enabled,
        numberText = String.format(locale, "%.${number.precision}f", displayedValue),
        units = number.units,
        onClick = { onEdit(number) },
    )
}

@Composable private fun ChoiceRow(title: String, value: String, tag: String, enabled: Boolean = true, onClick: () -> Unit) {
    ParameterSelectionRow(title, value, tag, enabled = enabled, onClick = onClick)
}
