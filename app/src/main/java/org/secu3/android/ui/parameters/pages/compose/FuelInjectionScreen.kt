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

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.secu3.android.R
import org.secu3.android.models.packets.out.params.InjctrParPacket
import org.secu3.android.ui.compose.DropdownShape
import org.secu3.android.ui.parameters.compose.ParameterLoadingIndicator
import org.secu3.android.ui.parameters.pages.injection.validInjectorConfigurations
import org.secu3.android.ui.parameters.pages.injection.validInjectorPulses

data class FuelBankValues(
    val flow: Float, val config: Int, val pulses: Int, val timing: Int, val cranking: Int,
    val useMap: Boolean, val angleSpec: Int, val minPw: Float, val maxPw: Float,
    val configOptions: List<Int>, val pulseOptions: List<Int>,
)
data class FuelInjectionValues(
    val engineDisplacement: Float, val banks: List<FuelBankValues>, val pulsesPerLiter: Int,
    val additionalCorrections: Boolean, val airDensity: Boolean, val differentialPressure: Boolean,
    val switchRows: Boolean, val fullSequentialAfterStart: Boolean, val decreaseRate: Float, val increaseRate: Float,
)
sealed interface FuelInjectionUiState {
    data object Loading : FuelInjectionUiState
    data class Data(val values: FuelInjectionValues, val injectorEnabled: Boolean = true, val secu3t: Boolean = false) : FuelInjectionUiState
}
enum class FuelInjectionNumber { ENGINE_DISPLACEMENT, FLOW_0, TIMING_0, CRANKING_0, MIN_PW_0, MAX_PW_0, FLOW_1, TIMING_1, CRANKING_1, MIN_PW_1, MAX_PW_1, PULSES_PER_LITER, DECREASE_RATE, INCREASE_RATE }
enum class FuelInjectionFlag { USE_MAP_0, USE_MAP_1, ADDITIONAL_CORRECTIONS, AIR_DENSITY, DIFFERENTIAL_PRESSURE, SWITCH_ROWS, FULL_SEQUENTIAL_AFTER_START }

fun fuelInjectionValues(packet: InjctrParPacket): FuelInjectionValues = FuelInjectionValues(
    packet.engineDisp,
    (0..1).map { bank ->
        val cfg = if (bank == 0) packet.config0 else packet.config1
        val pulseList = validInjectorPulses(cfg, packet.ckpsEngineCyl)
        FuelBankValues(packet.flowRate[bank], cfg, if (bank == 0) packet.config0Pulses else packet.config1Pulses,
            packet.timing[bank], packet.timingCrk[bank], if (bank == 0) packet.useTimingMap else packet.useTimingMapG,
            if (bank == 0) packet.angleSpec0 else packet.angleSpec1, if (bank == 0) packet.minPw0 else packet.minPw1,
            packet.injMaxPw[bank], validInjectorConfigurations(packet.ckpsEngineCyl), pulseList)
    }, packet.fffConst, packet.useAdditionalCorrections, packet.useAirDensity, packet.useDifferentialPressure,
    packet.switchSecondInjRow, packet.fullSequentialAfterStart, packet.injPwDecSpeed, packet.injPwIncSpeed,
)

@Composable
fun FuelInjectionScreen(state: FuelInjectionUiState, configLabels: Map<Int, String>, onEdit: (FuelInjectionNumber) -> Unit,
                        onConfig: (Int, Int) -> Unit, onPulses: (Int, Int) -> Unit,
                        onAngleSpec: (Int, Int) -> Unit, onFlag: (FuelInjectionFlag, Boolean) -> Unit) {
    if (state is FuelInjectionUiState.Loading) { ParameterLoadingIndicator("fuel_injection_loading"); return }
    val data = (state as FuelInjectionUiState.Data).values
    val timings = stringArrayResource(R.array.inj_timing_specifies).toList()
    var choice by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (!state.injectorEnabled) Text(stringResource(R.string.fuel_injection_unavailable), Modifier.testTag("fuel_injection_disabled").padding(bottom = 8.dp))
        else {
            NumberRow(stringResource(R.string.injctr_par_enjine_displacement_title), "L", data.engineDisplacement, 4, "inj-engine-displacement") { onEdit(FuelInjectionNumber.ENGINE_DISPLACEMENT) }
            Text(stringResource(R.string.injctr_par_primary_fuel_title), Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 4.dp), style = MaterialTheme.typography.titleSmall)
            BankSection(0, data.banks[0], configLabels, timings, onEdit, { bank, _ -> choice = bank to 0 }, { bank, _ -> choice = bank to 1 }, { bank, _ -> choice = bank to 2 }, onFlag)
            Text(stringResource(R.string.injctr_par_params_for_second_fuel_title), Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 4.dp), style = MaterialTheme.typography.titleMedium)
            BankSection(1, data.banks[1], configLabels, timings, onEdit, { bank, _ -> choice = bank to 0 }, { bank, _ -> choice = bank to 1 }, { bank, _ -> choice = bank to 2 }, onFlag)
            Text(stringResource(R.string.injctr_par_general_title), Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 4.dp), style = MaterialTheme.typography.titleMedium)
            NumberRow(stringResource(R.string.injctr_par_pulses_per_litter_of_fuel), "", data.pulsesPerLiter.toFloat(), 0, "inj-pulses-per-liter") { onEdit(FuelInjectionNumber.PULSES_PER_LITER) }
            FlagRow(stringResource(R.string.injctr_par_additional_corrections_title), data.additionalCorrections, "inj-additional-corrections", onFlag, FuelInjectionFlag.ADDITIONAL_CORRECTIONS, enabled = !state.secu3t)
            FlagRow(stringResource(R.string.injctr_par_use_air_density_correction_map_title), data.airDensity, "inj-air-density", onFlag, FuelInjectionFlag.AIR_DENSITY)
            FlagRow(stringResource(R.string.injctr_par_diff_press_for_pw_corr_from_gps_title), data.differentialPressure, "inj-differential-pressure", onFlag, FuelInjectionFlag.DIFFERENTIAL_PRESSURE, enabled = !state.secu3t)
            FlagRow(stringResource(R.string.injctr_par_switch_between_injectors_rows_title), data.switchRows, "inj-switch-rows", onFlag, FuelInjectionFlag.SWITCH_ROWS, enabled = !state.secu3t)
            FlagRow(stringResource(R.string.injctr_par_full_sequential_after_start_title), data.fullSequentialAfterStart, "inj-full-sequential-after-start", onFlag, FuelInjectionFlag.FULL_SEQUENTIAL_AFTER_START)
            NumberRow(stringResource(R.string.injctr_par_inj_pw_dec_speed_title), "ms", data.decreaseRate, 2, "inj-decrease-rate") { onEdit(FuelInjectionNumber.DECREASE_RATE) }
            NumberRow(stringResource(R.string.injctr_par_inj_pw_inc_speed_title), "ms", data.increaseRate, 2, "inj-increase-rate") { onEdit(FuelInjectionNumber.INCREASE_RATE) }
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
        AlertDialog(onDismissRequest = { choice = null }, title = { Text(title) },
            text = { Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                items.forEach { (id, label) -> Text(label, Modifier.fillMaxWidth().clickable {
                    when (kind) { 0 -> onConfig(bank, id); 1 -> onPulses(bank, id); else -> onAngleSpec(bank, id) }
                    choice = null
                }.padding(12.dp).testTag("inj-choice-$bank-$kind-$id")) }
            } }, confirmButton = { TextButton(onClick = { choice = null }) { Text(stringResource(android.R.string.cancel)) } })
    }
}

@Composable private fun BankSection(bank: Int, value: FuelBankValues, labels: Map<Int, String>, timings: List<String>, onEdit: (FuelInjectionNumber) -> Unit,
    onConfig: (Int, Int) -> Unit, onPulses: (Int, Int) -> Unit, onAngle: (Int, Int) -> Unit, onFlag: (FuelInjectionFlag, Boolean) -> Unit) {
    NumberRow(stringResource(R.string.injctr_par_injector_flow_rate_title), "cc/min", value.flow, 2, "inj-flow-$bank") { onEdit(if (bank == 0) FuelInjectionNumber.FLOW_0 else FuelInjectionNumber.FLOW_1) }
    val configTitle = stringResource(R.string.injctr_par_injector_config_title)
    ChoiceRow(configTitle, labels[value.config] ?: "Unknown configuration (${value.config})", "inj-config-$bank") { onConfig(bank, value.config) }
    ChoiceRow(stringResource(R.string.injctr_par_number_of_squirts_per_cycle_title), value.pulses.toString(), "inj-pulses-$bank", enabled = value.pulseOptions.isNotEmpty()) { onPulses(bank, value.pulses) }
    NumberRow(stringResource(R.string.injctr_par_injection_timing_title), "°", value.timing.toFloat(), 0, "inj-timing-$bank", enabled = !value.useMap) { onEdit(if (bank == 0) FuelInjectionNumber.TIMING_0 else FuelInjectionNumber.TIMING_1) }
    NumberRow(stringResource(R.string.injctr_par_cranking_injection_timing_title), "°", value.cranking.toFloat(), 0, "inj-cranking-$bank") { onEdit(if (bank == 0) FuelInjectionNumber.CRANKING_0 else FuelInjectionNumber.CRANKING_1) }
    FlagRow(stringResource(R.string.injctr_par_use_map_for_inj_timing_title), value.useMap, "inj-use-map-$bank", onFlag, if (bank == 0) FuelInjectionFlag.USE_MAP_0 else FuelInjectionFlag.USE_MAP_1)
    ChoiceRow(stringResource(R.string.injctr_par_inj_timing_specifies), timings.getOrNull(value.angleSpec) ?: "Code ${value.angleSpec}", "inj-angle-spec-$bank") { onAngle(bank, value.angleSpec) }
    NumberRow(stringResource(R.string.injctr_par_min_inj_pw_title), "ms", value.minPw, 2, "inj-min-pw-$bank") { onEdit(if (bank == 0) FuelInjectionNumber.MIN_PW_0 else FuelInjectionNumber.MIN_PW_1) }
    NumberRow(stringResource(R.string.injctr_par_max_inj_pw_title), "ms", value.maxPw, 2, "inj-max-pw-$bank") { onEdit(if (bank == 0) FuelInjectionNumber.MAX_PW_0 else FuelInjectionNumber.MAX_PW_1) }
}

@Composable private fun NumberRow(title: String, units: String, value: Float, precision: Int, tag: String, enabled: Boolean = true, onClick: () -> Unit) {
    val locale = LocalConfiguration.current.locales[0]
    Column {
        Row(Modifier.fillMaxWidth().clickable(enabled = enabled, onClick = onClick).testTag(tag).padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(title, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = .38f))
            Text(String.format(locale, "%.${precision}f", value) + if (units.isEmpty()) "" else " $units", Modifier.testTag("$tag-value"), textAlign = TextAlign.End)
        }; HorizontalDivider()
    }
}

@Composable private fun FlagRow(title: String, checked: Boolean, tag: String, onFlag: (FuelInjectionFlag, Boolean) -> Unit, flag: FuelInjectionFlag, enabled: Boolean = true) {
    Row(Modifier.fillMaxWidth().testTag(tag).padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, Modifier.weight(1f).clickable(enabled) { onFlag(flag, !checked) }.padding(end = 8.dp), style = MaterialTheme.typography.bodyLarge,
            color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = .38f))
        Switch(checked = checked, enabled = enabled, onCheckedChange = { onFlag(flag, it) }, modifier = Modifier.testTag("$tag-switch"))
    }
}

@Composable private fun ChoiceRow(title: String, value: String, tag: String, enabled: Boolean = true, onClick: () -> Unit) {
    Surface(Modifier.fillMaxWidth().semantics(mergeDescendants = true) { role = Role.Button; contentDescription = "$title, $value"; stateDescription = value }
        .clickable(enabled = enabled, role = Role.Button, onClick = onClick).testTag(tag), shape = DropdownShape, color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = if (enabled) 1f else .38f))) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { Text(title, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant); Text(value, Modifier.padding(top = 3.dp), style = MaterialTheme.typography.bodyLarge) }
            Icon(Icons.Filled.ArrowDropDown, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
