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

import org.secu3.android.ui.compose.DropdownShape

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import org.secu3.android.ui.parameters.compose.ParameterLoadingIndicator
import org.secu3.android.R
import org.secu3.android.models.packets.out.params.FunSetParamPacket
import org.secu3.android.ui.parameters.compose.ParameterValueRow
import org.secu3.android.ui.parameters.compose.ParameterSwitchRow

enum class FunctionsNumber(
    @StringRes val titleRes: Int,
    @StringRes val unitRes: Int,
    val precision: Int,
    val min: Float,
    val max: Float,
    val step: Float,
) {
    LOWER(R.string.funset_lower_pressure_title, R.string.units_pressure_kpa, 2, .25f, 105f, .25f),
    UPPER(R.string.funset_upper_pressure_title, R.string.units_pressure_kpa, 2, .25f, 500f, .25f),
    MAP_OFFSET(R.string.funset_map_sensor_offset_title, R.string.units_volts, 3, -5f, 5f, .003f),
    MAP_GRADIENT(R.string.funset_map_sensor_gradient_title, R.string.units_kpa_per_volt, 3, -150f, 150f, .01f),
    TPS_OFFSET(R.string.funset_tps_curve_offset_title, R.string.units_volts, 3, -5f, 5f, .003f),
    TPS_GRADIENT(R.string.funset_tps_curve_gradient_title, R.string.units_percent_per_volts, 3, 0f, 100f, .01f),
    MAP2_OFFSET(R.string.funset_map2_sensor_offset_title, R.string.units_volts, 3, -5f, 5f, .003f),
    MAP2_GRADIENT(R.string.funset_map2_sensor_gradient_title, R.string.units_kpa_per_volt, 3, -150f, 150f, .01f),
    GPS_OFFSET(R.string.funset_gas_pressure_sensor_offset_title, R.string.units_volts, 3, -5f, 5f, .003f),
    GPS_GRADIENT(R.string.funset_gas_pressure_sensor_gradient_title, R.string.units_kpa_per_volt, 3, -150f, 150f, .01f),
    FPS_OFFSET(R.string.funset_fuel_pressure_sensor_offset_title, R.string.units_volts, 3, -5f, 5f, .0025f),
    FPS_GRADIENT(R.string.funset_fuel_pressure_sensor_gradient_title, R.string.units_kpa_per_volt, 3, -500f, 500f, .01f),
    APPS_OFFSET(R.string.funset_apps_offset_title, R.string.units_volts, 3, -5f, 5f, .0025f),
    APPS_GRADIENT(R.string.funset_apps_gradient_title, R.string.units_percent_per_volts, 3, -500f, 500f, .01f),
}

data class FunctionsValues(val numbers: Map<FunctionsNumber, Float>, val useLoadGrid: Boolean) {
    fun value(number: FunctionsNumber): Float = numbers.getValue(number)

    fun withValue(number: FunctionsNumber, value: Float): FunctionsValues =
        copy(numbers = numbers + (number to value))

    companion object {
        fun from(packet: FunSetParamPacket) = FunctionsValues(
            numbers = mapOf(
                FunctionsNumber.LOWER to packet.loadLower,
                FunctionsNumber.UPPER to packet.loadUpper,
                FunctionsNumber.MAP_OFFSET to packet.mapCurveOffset,
                FunctionsNumber.MAP_GRADIENT to packet.mapCurveGradient,
                FunctionsNumber.TPS_OFFSET to packet.tpsCurveOffset,
                FunctionsNumber.TPS_GRADIENT to packet.tpsCurveGradient,
                FunctionsNumber.MAP2_OFFSET to packet.map2CurveOffset,
                FunctionsNumber.MAP2_GRADIENT to packet.map2CurveGradient,
                FunctionsNumber.GPS_OFFSET to packet.gpsCurveOffset,
                FunctionsNumber.GPS_GRADIENT to packet.gpsCurveGradient,
                FunctionsNumber.FPS_OFFSET to packet.fpsCurveOffset,
                FunctionsNumber.FPS_GRADIENT to packet.fpsCurveGradient,
                FunctionsNumber.APPS_OFFSET to packet.apps1CurveOffset,
                FunctionsNumber.APPS_GRADIENT to packet.apps1CurveGradient,
            ),
            useLoadGrid = packet.useLoadGrid,
        )
    }
}

fun applyFunctionsValue(packet: FunSetParamPacket, number: FunctionsNumber, value: Float) {
    when (number) {
        FunctionsNumber.LOWER -> packet.loadLower = value
        FunctionsNumber.UPPER -> packet.loadUpper = value
        FunctionsNumber.MAP_OFFSET -> packet.mapCurveOffset = value
        FunctionsNumber.MAP_GRADIENT -> packet.mapCurveGradient = value
        FunctionsNumber.TPS_OFFSET -> packet.tpsCurveOffset = value
        FunctionsNumber.TPS_GRADIENT -> packet.tpsCurveGradient = value
        FunctionsNumber.MAP2_OFFSET -> packet.map2CurveOffset = value
        FunctionsNumber.MAP2_GRADIENT -> packet.map2CurveGradient = value
        FunctionsNumber.GPS_OFFSET -> packet.gpsCurveOffset = value
        FunctionsNumber.GPS_GRADIENT -> packet.gpsCurveGradient = value
        FunctionsNumber.FPS_OFFSET -> packet.fpsCurveOffset = value
        FunctionsNumber.FPS_GRADIENT -> packet.fpsCurveGradient = value
        FunctionsNumber.APPS_OFFSET -> packet.apps1CurveOffset = value
        FunctionsNumber.APPS_GRADIENT -> packet.apps1CurveGradient = value
    }
}

fun applyFunctionsFlag(packet: FunSetParamPacket, checked: Boolean) {
    packet.useLoadGrid = checked
}

data class FunctionsChoices(val selected: Map<Selector, String?>, val options: Map<Selector, List<String>>) {
    enum class Selector {
        GASOLINE_MAP,
        GAS_MAP,
        LOAD_SOURCE,
        MAPSEL_PETROL,
        MAPSEL_GAS,
        BAROCORR,
        VE2,
        GAS_V_UNI,
    }

    fun withSelection(selector: Selector, index: Int) =
        copy(selected = selected + (selector to options[selector]?.getOrNull(index)))

    companion object {
        val mapselCodes = listOf(1, 2, 3, 4, 5, 6, 15)

        fun from(
            packet: FunSetParamPacket,
            names: List<String>,
            loads: List<String>,
            bars: List<String>,
            ve2: List<String>,
        ): FunctionsChoices {
            val mapOptions = mapselCodes.map { code -> if (code == 15) "no" else code.toString() }
            val optionMap = Selector.entries.associateWith { selector ->
                when (selector) {
                    Selector.GASOLINE_MAP, Selector.GAS_MAP -> names
                    Selector.LOAD_SOURCE -> loads
                    Selector.MAPSEL_PETROL, Selector.MAPSEL_GAS, Selector.GAS_V_UNI -> mapOptions
                    Selector.BAROCORR -> bars
                    Selector.VE2 -> ve2
                }
            }

            fun mapOption(code: Int): String? =
                mapselCodes.indexOf(code).takeIf { it >= 0 }?.let(mapOptions::getOrNull)

            return FunctionsChoices(
                selected = mapOf(
                    Selector.GASOLINE_MAP to names.getOrNull(packet.fnGasoline),
                    Selector.GAS_MAP to names.getOrNull(packet.fnGas),
                    Selector.LOAD_SOURCE to loads.getOrNull(packet.loadSrcCfg),
                    Selector.MAPSEL_PETROL to mapOption(packet.mapselUniPetrol),
                    Selector.MAPSEL_GAS to mapOption(packet.mapselUniGas),
                    Selector.BAROCORR to bars.getOrNull(packet.barocorrType),
                    Selector.VE2 to ve2.getOrNull(packet.ve2MapFunc),
                    Selector.GAS_V_UNI to mapOption(packet.gasVUni),
                ),
                options = optionMap,
            )
        }
    }
}

fun applyFunctionsChoice(
    packet: FunSetParamPacket,
    selector: FunctionsChoices.Selector,
    index: Int,
    names: List<String>,
): Boolean {
    when (selector) {
        FunctionsChoices.Selector.GASOLINE_MAP -> if (index in names.indices) packet.fnGasoline = index else return false
        FunctionsChoices.Selector.GAS_MAP -> if (index in names.indices) packet.fnGas = index else return false
        FunctionsChoices.Selector.LOAD_SOURCE -> if (index in 0..4) packet.loadSrcCfg = index else return false
        FunctionsChoices.Selector.MAPSEL_PETROL -> if (index in FunctionsChoices.mapselCodes.indices) packet.mapselUniPetrol = FunctionsChoices.mapselCodes[index] else return false
        FunctionsChoices.Selector.MAPSEL_GAS -> if (index in FunctionsChoices.mapselCodes.indices) packet.mapselUniGas = FunctionsChoices.mapselCodes[index] else return false
        FunctionsChoices.Selector.BAROCORR -> if (index in 0..3) packet.barocorrType = index else return false
        FunctionsChoices.Selector.VE2 -> if (index in 0..2) packet.ve2MapFunc = index else return false
        FunctionsChoices.Selector.GAS_V_UNI -> if (index in FunctionsChoices.mapselCodes.indices) packet.gasVUni = FunctionsChoices.mapselCodes[index] else return false
    }
    return true
}

sealed interface FunctionsUiState {
    data object Loading : FunctionsUiState
    data class Data(val values: FunctionsValues, val choices: FunctionsChoices) : FunctionsUiState
}

@Composable
fun FunctionsScreen(
    state: FunctionsUiState,
    onEdit: (FunctionsNumber) -> Unit,
    onFlag: (Boolean) -> Unit,
    onChoice: (FunctionsChoices.Selector, Int) -> Unit,
) {
    when (state) {
        FunctionsUiState.Loading -> ParameterLoadingIndicator("functions_loading")
        is FunctionsUiState.Data -> {
            var open by remember { mutableStateOf<FunctionsChoices.Selector?>(null) }
            Column(
                Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FunctionsNumber.entries.forEach { n ->
                    if (n == FunctionsNumber.LOWER) {
                        SectionTitle(stringResource(R.string.functions_section_general))
                    }
                    if (n == FunctionsNumber.MAP_OFFSET) {
                        ParameterSwitchRow(
                            stringResource(R.string.idlreg_use_load_grid_title),
                            state.values.useLoadGrid,
                            "functions_load_grid",
                            onFlag,
                        )
                        SectionTitle(stringResource(R.string.functions_section_map_tps))
                    }
                    if (n == FunctionsNumber.MAP2_OFFSET) {
                        SectionTitle(stringResource(R.string.functions_section_map2))
                    }
                    if (n == FunctionsNumber.GPS_OFFSET) {
                        SectionTitle(stringResource(R.string.functions_section_pressures_apps))
                    }
                    ParameterValueRow(
                        title = stringResource(n.titleRes),
                        units = stringResource(n.unitRes),
                        value = state.values.value(n),
                        precision = n.precision,
                        testTag = "functions_${n.name.lowercase()}",
                    ) { onEdit(n) }
                    if (n == FunctionsNumber.MAP_GRADIENT) {
                        SectionTitle(stringResource(R.string.functions_section_tables))
                        ChoiceRow(
                            stringResource(R.string.funset_maps_set_for_gasoline),
                            state.choices.selected[FunctionsChoices.Selector.GASOLINE_MAP],
                            "functions_gasoline",
                        ) { open = FunctionsChoices.Selector.GASOLINE_MAP }
                        ChoiceRow(
                            stringResource(R.string.funset_maps_set_for_gas),
                            state.choices.selected[FunctionsChoices.Selector.GAS_MAP],
                            "functions_gas",
                        ) { open = FunctionsChoices.Selector.GAS_MAP }
                    }
                    if (n == FunctionsNumber.TPS_GRADIENT) {
                        SectionTitle(stringResource(R.string.functions_section_sources))
                        ChoiceRow(
                            stringResource(R.string.funset_load_measurement),
                            state.choices.selected[FunctionsChoices.Selector.LOAD_SOURCE],
                            "functions_load_source",
                        ) { open = FunctionsChoices.Selector.LOAD_SOURCE }
                        ChoiceRow(
                            stringResource(R.string.funset_mapsel0_condition_for_petrol),
                            state.choices.selected[FunctionsChoices.Selector.MAPSEL_PETROL],
                            "functions_mapsel_petrol",
                        ) { open = FunctionsChoices.Selector.MAPSEL_PETROL }
                        ChoiceRow(
                            stringResource(R.string.funset_mapsel0_condition_for_gas),
                            state.choices.selected[FunctionsChoices.Selector.MAPSEL_GAS],
                            "functions_mapsel_gas",
                        ) { open = FunctionsChoices.Selector.MAPSEL_GAS }
                        ChoiceRow(
                            stringResource(R.string.funset_barometric_correction),
                            state.choices.selected[FunctionsChoices.Selector.BAROCORR],
                            "functions_barocorr",
                        ) { open = FunctionsChoices.Selector.BAROCORR }
                    }
                    if (n == FunctionsNumber.MAP2_GRADIENT) {
                        ChoiceRow(
                            stringResource(R.string.funset_ve2_map_func_title),
                            state.choices.selected[FunctionsChoices.Selector.VE2],
                            "functions_ve2",
                        ) { open = FunctionsChoices.Selector.VE2 }
                        ChoiceRow(
                            stringResource(R.string.funset_gas_v_uni_title),
                            state.choices.selected[FunctionsChoices.Selector.GAS_V_UNI],
                            "functions_gas_v_uni",
                        ) { open = FunctionsChoices.Selector.GAS_V_UNI }
                    }
                }
            }
            open?.let { selector ->
                val options = state.choices.options[selector].orEmpty()
                AlertDialog(
                    onDismissRequest = { open = null },
                    title = { ChoiceTitle(selector) },
                    text = {
                        Column(Modifier.verticalScroll(rememberScrollState())) {
                            options.forEachIndexed { i, option ->
                                Text(option, Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onChoice(selector, i)
                                        open = null
                                    }
                                    .padding(12.dp)
                                    .testTag("functions_option_$i"))
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { open = null }) {
                            Text(stringResource(android.R.string.cancel))
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        title,
        Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 4.dp),
        style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
    )
}

@Composable
private fun ChoiceRow(title: String, value: String?, tag: String, onClick: () -> Unit) {
    val selectedValue = value.orEmpty()

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                role = Role.Button
                contentDescription = if (selectedValue.isEmpty()) title else "$title, $selectedValue"
                if (selectedValue.isNotEmpty()) stateDescription = selectedValue
            }
            .clickable(role = Role.Button, onClick = onClick)
            .testTag(tag),
        shape = DropdownShape,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (selectedValue.isNotEmpty()) {
                    Text(
                        text = selectedValue,
                        modifier = Modifier.padding(top = 4.dp),
                        style = MaterialTheme.typography.bodyLarge,
                        softWrap = true,
                    )
                }
            }
            Icon(
                imageVector = Icons.Filled.ArrowDropDown,
                contentDescription = null,
                modifier = Modifier
                    .size(24.dp)
                    .testTag("$tag-dropdown-indicator"),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ChoiceTitle(selector: FunctionsChoices.Selector) {
    val title = when (selector) {
        FunctionsChoices.Selector.GASOLINE_MAP -> stringResource(R.string.funset_maps_set_for_gasoline)
        FunctionsChoices.Selector.GAS_MAP -> stringResource(R.string.funset_maps_set_for_gas)
        FunctionsChoices.Selector.LOAD_SOURCE -> stringResource(R.string.funset_load_measurement)
        FunctionsChoices.Selector.MAPSEL_PETROL -> stringResource(R.string.funset_mapsel0_condition_for_petrol)
        FunctionsChoices.Selector.MAPSEL_GAS -> stringResource(R.string.funset_mapsel0_condition_for_gas)
        FunctionsChoices.Selector.BAROCORR -> stringResource(R.string.funset_barometric_correction)
        FunctionsChoices.Selector.VE2 -> stringResource(R.string.funset_ve2_map_func_title)
        FunctionsChoices.Selector.GAS_V_UNI -> stringResource(R.string.funset_gas_v_uni_title)
    }
    Text(title)
}
