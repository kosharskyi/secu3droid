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

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Switch
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import org.secu3.android.ui.parameters.compose.ParameterLoadingIndicator
import org.secu3.android.ui.parameters.compose.ParameterPage
import org.secu3.android.ui.parameters.dialogs.ParameterAlertDialog
import org.secu3.android.ui.parameters.dialogs.ParameterCancelButton
import org.secu3.android.ui.parameters.dialogs.ParameterChoiceList
import org.secu3.android.ui.parameters.dialogs.ParameterChoiceOption
import org.secu3.android.ui.parameters.*
import org.secu3.android.R
import org.secu3.android.domain.parameters.FunctionsValues

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
    MAP_OFFSET(R.string.funset_map_sensor_offset_title, R.string.units_volts, 4, -5f, 5f, .0025f),
    MAP_GRADIENT(R.string.funset_map_sensor_gradient_title, R.string.units_kpa_per_volt, 3, -150f, 150f, .01f),
    TPS_OFFSET(R.string.funset_tps_curve_offset_title, R.string.units_volts, 4, -5f, 5f, .0025f),
    TPS_GRADIENT(R.string.funset_tps_curve_gradient_title, R.string.units_percent_per_volts, 3, -100f, 100f, .01f),
    MAP2_OFFSET(R.string.funset_map2_sensor_offset_title, R.string.units_volts, 4, -5f, 5f, .0025f),
    MAP2_GRADIENT(R.string.funset_map2_sensor_gradient_title, R.string.units_kpa_per_volt, 3, -150f, 150f, .01f),
    GPS_OFFSET(R.string.funset_gas_pressure_sensor_offset_title, R.string.units_volts, 4, -5f, 5f, .0025f),
    GPS_GRADIENT(R.string.funset_gas_pressure_sensor_gradient_title, R.string.units_kpa_per_volt, 3, -150f, 150f, .01f),
    FPS_OFFSET(R.string.funset_fuel_pressure_sensor_offset_title, R.string.units_volts, 4, -5f, 5f, .0025f),
    FPS_GRADIENT(R.string.funset_fuel_pressure_sensor_gradient_title, R.string.units_kpa_per_volt, 3, -500f, 500f, .01f),
    APPS_OFFSET(R.string.funset_apps_offset_title, R.string.units_volts, 4, -5f, 5f, .0025f),
    APPS_GRADIENT(R.string.funset_apps_gradient_title, R.string.units_percent_per_volts, 3, -500f, 500f, .01f);

    fun value(values: FunctionsValues): Float = when (this) {
        LOWER -> values.loadLower
        UPPER -> values.loadUpper
        MAP_OFFSET -> values.mapCurveOffset
        MAP_GRADIENT -> values.mapCurveGradient
        TPS_OFFSET -> values.tpsCurveOffset
        TPS_GRADIENT -> values.tpsCurveGradient
        MAP2_OFFSET -> values.map2CurveOffset
        MAP2_GRADIENT -> values.map2CurveGradient
        GPS_OFFSET -> values.gpsCurveOffset
        GPS_GRADIENT -> values.gpsCurveGradient
        FPS_OFFSET -> values.fpsCurveOffset
        FPS_GRADIENT -> values.fpsCurveGradient
        APPS_OFFSET -> values.apps1CurveOffset
        APPS_GRADIENT -> values.apps1CurveGradient
    }

    fun enabled(values: FunctionsValues, capabilities: ParameterCapabilities): Boolean = when (this) {
        LOWER -> capabilities.functionsLowerLoadEnabled(values.useLoadGrid)
        UPPER -> capabilities.functionsUpperLoadEnabled(values.useLoadGrid, values.loadSource)
        GPS_OFFSET, GPS_GRADIENT, FPS_OFFSET, FPS_GRADIENT, APPS_OFFSET, APPS_GRADIENT -> capabilities.dbw
        else -> true
    }
}

data class FunctionsChoices(val selected: Map<Selector, String?>, val options: Map<Selector, List<Option>>) {
    data class Option(val code: Int, val label: String)

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

    companion object {
        fun from(
            values: FunctionsValues,
            loads: List<String>,
            bars: List<String>,
            ve2: List<String>,
        ): FunctionsChoices {
            val mapOptions = (0..5).map { code -> Option(code, (code + 1).toString()) } + Option(15, "no")
            val optionMap = Selector.entries.associateWith { selector ->
                when (selector) {
                    Selector.GASOLINE_MAP, Selector.GAS_MAP -> values.functionNames.mapIndexed { index, name -> Option(index, name) }
                    Selector.LOAD_SOURCE -> loads.mapIndexed { index, label -> Option(index, label) }
                    Selector.MAPSEL_PETROL, Selector.MAPSEL_GAS, Selector.GAS_V_UNI -> mapOptions
                    Selector.BAROCORR -> bars.mapIndexed { index, label -> Option(index, label) }
                    Selector.VE2 -> ve2.mapIndexed { index, label -> Option(index, label) }
                }
            }

            fun selected(selector: Selector, code: Int): String? =
                optionMap[selector]?.firstOrNull { it.code == code }?.label

            return FunctionsChoices(
                selected = mapOf(
                    Selector.GASOLINE_MAP to selected(Selector.GASOLINE_MAP, values.fnGasoline),
                    Selector.GAS_MAP to selected(Selector.GAS_MAP, values.fnGas),
                    Selector.LOAD_SOURCE to selected(Selector.LOAD_SOURCE, values.loadSource),
                    Selector.MAPSEL_PETROL to selected(Selector.MAPSEL_PETROL, values.mapselPetrol),
                    Selector.MAPSEL_GAS to selected(Selector.MAPSEL_GAS, values.mapselGas),
                    Selector.BAROCORR to selected(Selector.BAROCORR, values.barocorrType),
                    Selector.VE2 to selected(Selector.VE2, values.ve2MapFunc),
                    Selector.GAS_V_UNI to selected(Selector.GAS_V_UNI, values.gasVUni),
                ),
                options = optionMap,
            )
        }
    }
}

@Composable
fun FunctionsScreen(
    values: FunctionsValues?,
    onEdit: (FunctionsNumber) -> Unit,
    onFlag: (Boolean) -> Unit,
    onChoice: (FunctionsChoices.Selector, Int) -> Unit,
    capabilities: ParameterCapabilities = ParameterCapabilities.All,
) {
    val pageScrollState = rememberScrollState()
    if (values == null) {
        ParameterLoadingIndicator("functions_loading")
    } else {
            val choices = FunctionsChoices.from(
                values,
                stringArrayResource(R.array.load_measurement_items).toList(),
                stringArrayResource(R.array.barocorr_items).toList(),
                stringArrayResource(R.array.ve2_map_func_items).toList(),
            )
            var open by remember { mutableStateOf<FunctionsChoices.Selector?>(null) }
            ParameterPage(scrollState = pageScrollState) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                ) {
                    Column {
                        FunctionsNumber.entries.forEach { n ->
                            if (n == FunctionsNumber.LOWER) {
                                SectionTitle(stringResource(R.string.functions_section_general))
                            }
                            if (n == FunctionsNumber.MAP_OFFSET) {
                                FunctionsSwitchRow(
                                    stringResource(R.string.idlreg_use_load_grid_title),
                                    values.useLoadGrid,
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
                            FunctionsNumberRow(
                                title = stringResource(n.titleRes),
                                units = stringResource(if (n == FunctionsNumber.LOWER || n == FunctionsNumber.UPPER) {
                                    if (values.loadSource < 2) R.string.units_pressure_kpa else R.string.units_percents
                                } else n.unitRes),
                                value = n.value(values),
                                precision = n.precision,
                                testTag = "functions_${n.name.lowercase()}",
                                enabled = n.enabled(values, capabilities),
                            ) { onEdit(n) }
                            if (n != FunctionsNumber.entries.last()) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            }
                            if (n == FunctionsNumber.MAP_GRADIENT) {
                                SectionTitle(stringResource(R.string.functions_section_tables))
                                ChoiceRow(
                                    stringResource(R.string.funset_maps_set_for_gasoline),
                                    choices.selected[FunctionsChoices.Selector.GASOLINE_MAP],
                                    "functions_gasoline",
                                ) { open = FunctionsChoices.Selector.GASOLINE_MAP }
                                ChoiceRow(
                                    stringResource(R.string.funset_maps_set_for_gas),
                                    choices.selected[FunctionsChoices.Selector.GAS_MAP],
                                    "functions_gas",
                                ) { open = FunctionsChoices.Selector.GAS_MAP }
                            }
                            if (n == FunctionsNumber.TPS_GRADIENT) {
                                SectionTitle(stringResource(R.string.functions_section_sources))
                                ChoiceRow(
                                    stringResource(R.string.funset_load_measurement),
                                    choices.selected[FunctionsChoices.Selector.LOAD_SOURCE],
                                    "functions_load_source",
                                ) { open = FunctionsChoices.Selector.LOAD_SOURCE }
                                ChoiceRow(
                                    stringResource(R.string.funset_mapsel0_condition_for_petrol),
                                    choices.selected[FunctionsChoices.Selector.MAPSEL_PETROL],
                                    "functions_mapsel_petrol",
                                ) { open = FunctionsChoices.Selector.MAPSEL_PETROL }
                                ChoiceRow(
                                    stringResource(R.string.funset_mapsel0_condition_for_gas),
                                    choices.selected[FunctionsChoices.Selector.MAPSEL_GAS],
                                    "functions_mapsel_gas",
                                ) { open = FunctionsChoices.Selector.MAPSEL_GAS }
                                ChoiceRow(
                                    stringResource(R.string.funset_barometric_correction),
                                    choices.selected[FunctionsChoices.Selector.BAROCORR],
                                    "functions_barocorr",
                                ) { open = FunctionsChoices.Selector.BAROCORR }
                            }
                            if (n == FunctionsNumber.MAP2_GRADIENT) {
                                ChoiceRow(
                                    stringResource(R.string.funset_ve2_map_func_title),
                                    choices.selected[FunctionsChoices.Selector.VE2],
                                    "functions_ve2", enabled = capabilities.fuelInjection,
                                ) { open = FunctionsChoices.Selector.VE2 }
                                ChoiceRow(
                                    stringResource(R.string.funset_gas_v_uni_title),
                                    choices.selected[FunctionsChoices.Selector.GAS_V_UNI],
                                    "functions_gas_v_uni",
                                ) { open = FunctionsChoices.Selector.GAS_V_UNI }
                            }
                        }
                    }
                }
                open?.let { selector ->
                    val options = choices.options[selector].orEmpty()
                    ParameterAlertDialog(
                        onDismissRequest = { open = null },
                        title = { ChoiceTitle(selector) },
                        text = {
                            ParameterChoiceList {
                                options.forEachIndexed { index, option ->
                                    ParameterChoiceOption(option.label, "functions_option_$index") {
                                        onChoice(selector, option.code)
                                        open = null
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            ParameterCancelButton { open = null }
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
        Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, top = 16.dp, bottom = 8.dp),
        style = MaterialTheme.typography.titleMedium,
    )
}

@Composable
private fun ChoiceRow(title: String, value: String?, tag: String, enabled: Boolean = true, onClick: () -> Unit) {
    val selectedValue = value.orEmpty()
    Row(
        Modifier.fillMaxWidth().heightIn(min = 66.dp)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) {
                contentDescription = if (selectedValue.isEmpty()) title else "$title, $selectedValue"
                if (selectedValue.isNotEmpty()) stateDescription = selectedValue
            }
            .testTag(tag).padding(horizontal = 14.dp, vertical = 11.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, Modifier.weight(.5f).testTag("$tag-label"), style = MaterialTheme.typography.bodyMedium)
        Text(
            selectedValue,
            Modifier.weight(.5f).testTag("$tag-value"),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.End,
        )
        Icon(
            Icons.Default.ExpandMore,
            contentDescription = null,
            modifier = Modifier.size(18.dp).testTag("$tag-dropdown-indicator"),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
private fun FunctionsNumberRow(
    title: String,
    units: String,
    value: Float,
    precision: Int,
    testTag: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val formattedValue = buildAnnotatedString {
        append(String.format(LocalConfiguration.current.locales[0], "%.${precision}f", value))
        withStyle(
            SpanStyle(
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal,
            ),
        ) { append(" $units") }
    }
    Row(
        Modifier.fillMaxWidth().heightIn(min = 66.dp)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick).testTag(testTag)
            .padding(horizontal = 14.dp, vertical = 11.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, Modifier.weight(.62f).testTag("$testTag-label"), style = MaterialTheme.typography.bodyMedium)
        Text(
            formattedValue,
            Modifier.weight(.38f).testTag("$testTag-value"),
            style = MaterialTheme.typography.titleLarge.copy(
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                fontFeatureSettings = "tnum",
            ),
            textAlign = TextAlign.End,
        )
    }
}

@Composable
private fun FunctionsSwitchRow(title: String, checked: Boolean, tag: String, onCheckedChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 66.dp).testTag(tag)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(horizontal = 14.dp, vertical = 11.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Switch(checked, onCheckedChange, modifier = Modifier.testTag("$tag-switch"))
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
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
