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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.secu3.android.ui.parameters.compose.ParameterLoadingIndicator
import org.secu3.android.ui.parameters.*
import org.secu3.android.R
import org.secu3.android.models.packets.out.params.CarburParamPacket
import org.secu3.android.ui.parameters.compose.ParameterPage
import org.secu3.android.ui.parameters.compose.ParameterSection
import org.secu3.android.ui.parameters.compose.ParameterSelectionRow
import org.secu3.android.ui.parameters.compose.ParameterSwitchRow
import org.secu3.android.ui.parameters.compose.ParameterValueRow
import org.secu3.android.ui.parameters.dialogs.ParameterAlertDialog
import org.secu3.android.ui.parameters.dialogs.ParameterCancelButton
import org.secu3.android.ui.parameters.dialogs.ParameterChoiceList
import org.secu3.android.ui.parameters.dialogs.ParameterChoiceOption

enum class CarburNumber(@StringRes val title: Int, @StringRes val units: Int, val precision: Int, val min: Float, val max: Float, val step: Float, val integer: Boolean = false) {
    IDLE_LOW(R.string.carbur_overrun_lower_threshold_gasoline_title, R.string.units_rpm, 0, 250f, 30000f, 10f, true),
    IDLE_HIGH(R.string.carbur_overrun_upper_threshold_gasoline_title, R.string.units_rpm, 0, 250f, 30000f, 10f, true),
    IDLE_LOW_GAS(R.string.carbur_overrun_lower_threshold_gas_title, R.string.units_rpm, 0, 250f, 30000f, 10f, true),
    IDLE_HIGH_GAS(R.string.carbur_overrun_upper_threshold_gas_title, R.string.units_rpm, 0, 250f, 30000f, 10f, true),
    CUTOFF_DELAY(R.string.carbur_overrun_valve_delay_title, R.string.units_sec, 2, 0f, 2.5f, .01f),
    VALVE_PRESSURE(R.string.carbur_epm_valve_on_pressure_title, R.string.units_pressure_kpa, 1, 0f, 500f, .1f),
    TPS(R.string.carbur_tps_threshold_title, R.string.units_percents, 1, 0f, 100f, .5f),
    MAP(R.string.carbur_fuelcut_map_threshold_title, R.string.units_pressure_kpa, 1, 0f, 250f, .5f),
    CTS(R.string.carbur_fuelcut_temp_threshold_title, R.string.units_degrees_celcius, 1, -40f, 300f, .25f),
    REV_LOW(R.string.carbur_rev_limitting_lower_threshold_title, R.string.units_rpm, 0, 500f, 30000f, 50f, true),
    REV_HIGH(R.string.carbur_rev_limitting_upper_threshold_title, R.string.units_rpm, 0, 500f, 30000f, 50f, true),
}

data class CarburValues(val numbers: Map<CarburNumber, Float>, val invers: Boolean, val fuelCutUni: Int, val ignitionCutUni: Int) {
    fun withValue(number: CarburNumber, value: Float) = copy(numbers = numbers + (number to value))
    companion object {
        val selectorCodes = listOf(1, 2, 3, 4, 5, 6, 15)
        fun from(packet: CarburParamPacket) = CarburValues(
            mapOf(CarburNumber.IDLE_LOW to packet.ieLot.toFloat(), CarburNumber.IDLE_HIGH to packet.ieHit.toFloat(),
                CarburNumber.IDLE_LOW_GAS to packet.ieLotG.toFloat(), CarburNumber.IDLE_HIGH_GAS to packet.ieHitG.toFloat(),
                CarburNumber.CUTOFF_DELAY to packet.shutoffDelay, CarburNumber.VALVE_PRESSURE to packet.feOnThresholds,
                CarburNumber.TPS to packet.tpsThreshold, CarburNumber.MAP to packet.fuelcutMapThrd,
                CarburNumber.CTS to packet.fuelcutCtsThrd, CarburNumber.REV_LOW to packet.revlimLot.toFloat(),
                CarburNumber.REV_HIGH to packet.revlimHit.toFloat()), packet.carbInvers > 0, packet.fuelcut_uni, packet.igncut_uni)
    }
}

fun applyCarburValue(packet: CarburParamPacket, number: CarburNumber, value: Float) {
    when (number) {
        CarburNumber.IDLE_LOW -> packet.ieLot = value.toInt()
        CarburNumber.IDLE_HIGH -> packet.ieHit = value.toInt()
        CarburNumber.IDLE_LOW_GAS -> packet.ieLotG = value.toInt()
        CarburNumber.IDLE_HIGH_GAS -> packet.ieHitG = value.toInt()
        CarburNumber.CUTOFF_DELAY -> packet.shutoffDelay = value
        CarburNumber.VALVE_PRESSURE -> packet.feOnThresholds = value
        CarburNumber.TPS -> packet.tpsThreshold = value
        CarburNumber.MAP -> packet.fuelcutMapThrd = value
        CarburNumber.CTS -> packet.fuelcutCtsThrd = value
        CarburNumber.REV_LOW -> packet.revlimLot = value.toInt()
        CarburNumber.REV_HIGH -> packet.revlimHit = value.toInt()
    }
}

fun applyCarburSelector(packet: CarburParamPacket, ignition: Boolean, index: Int): Boolean {
    val code = CarburValues.selectorCodes.getOrNull(index) ?: return false
    if (ignition) packet.igncut_uni = code else packet.fuelcut_uni = code
    return true
}

sealed interface CarburUiState {
    data object Loading : CarburUiState
    data class Data(val values: CarburValues) : CarburUiState
}

@Composable
fun CarburScreen(state: CarburUiState, options: List<String>, onEdit: (CarburNumber) -> Unit, onInvers: (Boolean) -> Unit, onSelector: (Boolean, Int) -> Unit, capabilities: ParameterCapabilities = ParameterCapabilities.All,
) {
    val pageScrollState = rememberScrollState()
    if (state is CarburUiState.Loading) {
        ParameterLoadingIndicator("carbur_loading")
        return
    }
    val data = (state as CarburUiState.Data).values
    var open by remember { mutableStateOf<Boolean?>(null) }
    ParameterPage(scrollState = pageScrollState) {
        ParameterSection {
            CarburNumber.entries.filter { it != CarburNumber.REV_LOW && it != CarburNumber.REV_HIGH }.forEach { n ->
                ParameterValueRow(stringResource(n.title), if (n == CarburNumber.VALVE_PRESSURE) "dis.kPa" else stringResource(n.units), data.numbers.getValue(n), n.precision, "carbur_${n.name.lowercase()}", enabled = n !in setOf(CarburNumber.MAP, CarburNumber.CTS) || capabilities.fuelInjection) { onEdit(n) }
                if (n == CarburNumber.TPS) {
                    ParameterSwitchRow(stringResource(R.string.carbur_sensor_inverse_title), data.invers, "carbur_invers", onInvers)
                }
            }
            listOf(
                stringResource(R.string.carbur_use_uniout_condition_title) to false,
                stringResource(R.string.carbur_use_uniout_condition_for_ign_cutoff_title) to true,
            ).forEach { (title, ignition) ->
                val code = if (ignition) data.ignitionCutUni else data.fuelCutUni
                val tag = if (ignition) "carbur_igncut_uni" else "carbur_fuelcut_uni"
                val selected = selectorValue(code, options)
                ParameterSelectionRow(
                    title = title,
                    value = selected,
                    testTag = tag,
                    enabled = ignition || capabilities.fuelInjection,
                    contentDescription = "$title, $selected",
                    stateDescription = selected,
                    onClick = { open = ignition },
                )
            }
            Text(
                stringResource(R.string.carbur_protection_of_engine_title),
                Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 11.dp),
                style = MaterialTheme.typography.titleSmall,
            )
            listOf(CarburNumber.REV_LOW, CarburNumber.REV_HIGH).forEach { number ->
                ParameterValueRow(
                    title = stringResource(number.title),
                    units = stringResource(number.units),
                    value = data.numbers.getValue(number),
                    precision = number.precision,
                    testTag = "carbur_${number.name.lowercase()}",
                    enabled = capabilities.fuelOrGasDose,
                    onClick = { onEdit(number) },
                )
            }
        }
    }
    open?.let { ignition ->
        ParameterAlertDialog(onDismissRequest = { open = null }, title = { Text(if (ignition) stringResource(R.string.carbur_use_uniout_condition_for_ign_cutoff_title) else stringResource(R.string.carbur_use_uniout_condition_title)) },
            text = { ParameterChoiceList { options.forEachIndexed { i, option ->
                ParameterChoiceOption(option, "carbur_option_$i") { onSelector(ignition, i); open = null }
            } } },
            confirmButton = { ParameterCancelButton { open = null } })
    }
}

private fun selectorValue(code: Int, options: List<String>): String = CarburValues.selectorCodes.indexOf(code).takeIf { it >= 0 }?.let(options::getOrNull) ?: "Code $code"
