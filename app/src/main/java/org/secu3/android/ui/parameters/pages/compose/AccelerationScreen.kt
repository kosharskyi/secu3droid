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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import org.secu3.android.R
import org.secu3.android.domain.parameters.AccelerationValues
import org.secu3.android.domain.parameters.ParameterCapabilities
import org.secu3.android.ui.parameters.compose.ParameterLoadingIndicator
import org.secu3.android.ui.parameters.compose.ParameterPage
import org.secu3.android.ui.parameters.compose.ParameterSection
import org.secu3.android.ui.parameters.compose.ParameterSelectionRow
import org.secu3.android.ui.parameters.compose.ParameterValueRow
import org.secu3.android.ui.parameters.dialogs.ParameterChoice
import org.secu3.android.ui.parameters.dialogs.ParameterChoiceDialog

enum class AccelerationNumber(@StringRes val titleRes: Int, @StringRes val unitsRes: Int?, val min: Float, val max: Float, val step: Float) {
    TPS_THRESHOLD(R.string.accel_par_tpsdot_threshold_title, R.string.units_percent_per_second, 0f, 255f, 1f),
    COLD_MULTIPLIER(R.string.accel_par_coldacc_mult_title, R.string.units_percents, 100f, 600f, 1f),
    DECAY_TIME(R.string.accel_par_ae_decay_time_title, null, 0f, 255f, 1f),
    AE_TIME(R.string.accel_par_ae_time_title, null, 0f, 255f, 1f),
    MAPDOT_THRESHOLD(R.string.accel_par_ae_mapdot_thrd_title, null, 0f, 255f, 1f),
    XTAU_START(R.string.accel_par_start_transition_title, R.string.units_pressure_kpa, -255f, 0f, 1f),
    XTAU_FINISH(R.string.accel_par_finish_transition_title, R.string.units_pressure_kpa, -255f, 0f, 1f);

    fun value(values: AccelerationValues): Float = when (this) {
        TPS_THRESHOLD -> values.tpsThreshold.toFloat()
        COLD_MULTIPLIER -> values.coldMultiplier
        DECAY_TIME -> values.decayTime.toFloat()
        AE_TIME -> values.aeTime.toFloat()
        MAPDOT_THRESHOLD -> values.mapdotThreshold.toFloat()
        XTAU_START -> values.xtauStart
        XTAU_FINISH -> values.xtauFinish
    }
}

@Composable
fun AccelerationScreen(
    values: AccelerationValues?,
    onEdit: (AccelerationNumber) -> Unit,
    onAeType: (Int) -> Unit,
    onWallwetModel: (Int) -> Unit,
    onBalance: (Float) -> Unit,
    capabilities: ParameterCapabilities = ParameterCapabilities.All,
) {
    val pageScrollState = rememberScrollState()
    if (values == null) {
        ParameterLoadingIndicator("acceleration_loading")
        return
    }
    val aeTypes = stringArrayResource(R.array.accel_enrichment_types)
    val wallwetTypes = stringArrayResource(R.array.accel_wallwet_types)
    ParameterPage(scrollState = pageScrollState) {
        ParameterSection {
            AccelerationNumber.entries.take(3).forEach { number -> NumberRow(values, number, onEdit) }
            Selector(stringResource(R.string.accel_par_ae_type_title), aeTypes, values.aeType, "acceleration_ae_type", onAeType)
            if (values.aeType != 0) NumberRow(values, AccelerationNumber.AE_TIME, onEdit, capabilities.accelerationTimeEnabled(values.aeType))
            Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 11.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.accel_par_ae_ballance_title), Modifier.weight(1f))
                    Text("${values.balance.coerceIn(0f, 100f).roundToInt()}%", Modifier.testTag("acceleration_balance_value"))
                }
                Slider(value = values.balance.coerceIn(0f, 100f), onValueChange = onBalance, valueRange = 0f..100f, modifier = Modifier.testTag("acceleration_balance"))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("0%")
                    Text("100%")
                }
            }
            NumberRow(values, AccelerationNumber.MAPDOT_THRESHOLD, onEdit)
            Selector(stringResource(R.string.accel_par_fuel_film_model_title), wallwetTypes, values.wallwetModel, "acceleration_wallwet_model", onWallwetModel, capabilities.fuelInjection)
            if (values.wallwetModel > 0) {
                NumberRow(values, AccelerationNumber.XTAU_START, onEdit, capabilities.fuelInjection)
                NumberRow(values, AccelerationNumber.XTAU_FINISH, onEdit, capabilities.fuelInjection)
            }
        }
    }
}

@Composable
private fun NumberRow(values: AccelerationValues, number: AccelerationNumber, onEdit: (AccelerationNumber) -> Unit, enabled: Boolean = true) {
    ParameterValueRow(
        stringResource(number.titleRes), number.unitsRes?.let { stringResource(it) } ?: stringResource(R.string.units_str), number.value(values), 0,
        "acceleration_${number.name.lowercase()}", enabled = enabled,
    ) { onEdit(number) }
}

@Composable
private fun Selector(title: String, options: Array<String>, selected: Int, tag: String, onSelect: (Int) -> Unit, enabled: Boolean = true) {
    val expanded = remember { mutableStateOf(false) }
    val value = options.getOrNull(selected) ?: "${stringResource(R.string.firmware_unknown_value)} ($selected)"
    Box {
        ParameterSelectionRow(title, value, tag, enabled = enabled, valueTestTag = "${tag}_value") { expanded.value = true }
        if (expanded.value) ParameterChoiceDialog(
            title = title,
            choices = options.mapIndexed { index, label -> ParameterChoice(index, label, "${tag}_option_$index") },
            onDismiss = { expanded.value = false },
            onSelect = onSelect,
        )
    }
}
