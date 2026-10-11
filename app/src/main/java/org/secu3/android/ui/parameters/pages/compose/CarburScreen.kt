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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.secu3.android.R
import org.secu3.android.models.packets.out.params.CarburParamPacket

enum class CarburNumber(@StringRes val title: Int, @StringRes val units: Int, val precision: Int, val min: Float, val max: Float, val step: Float, val integer: Boolean = false) {
    IDLE_LOW(R.string.carbur_overrun_lower_threshold_gasoline_title, R.string.units_rpm, 0, 250f, 7500f, 10f, true),
    IDLE_HIGH(R.string.carbur_overrun_upper_threshold_gasoline_title, R.string.units_rpm, 0, 250f, 7500f, 10f, true),
    IDLE_LOW_GAS(R.string.carbur_overrun_lower_threshold_gas_title, R.string.units_rpm, 0, 250f, 7500f, 10f, true),
    IDLE_HIGH_GAS(R.string.carbur_overrun_upper_threshold_gas_title, R.string.units_rpm, 0, 250f, 7500f, 10f, true),
    CUTOFF_DELAY(R.string.carbur_overrun_valve_delay_title, R.string.units_sec, 2, 0f, 2.5f, .01f),
    VALVE_PRESSURE(R.string.carbur_epm_valve_on_pressure_title, R.string.units_pressure_kpa, 1, 0f, 50f, .1f),
    TPS(R.string.carbur_tps_threshold_title, R.string.units_percents, 1, 0f, 100f, .5f),
    MAP(R.string.carbur_fuelcut_map_threshold_title, R.string.units_pressure_kpa, 1, 0f, 250f, .5f),
    CTS(R.string.carbur_fuelcut_temp_threshold_title, R.string.units_degrees_celcius, 1, -30f, 180f, .25f),
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
fun CarburScreen(state: CarburUiState, options: List<String>, onEdit: (CarburNumber) -> Unit, onInvers: (Boolean) -> Unit, onSelector: (Boolean, Int) -> Unit) {
    if (state is CarburUiState.Loading) {
        Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) { CircularProgressIndicator(Modifier.testTag("carbur_loading")) }
        return
    }
    val data = (state as CarburUiState.Data).values
    var open by remember { mutableStateOf<Boolean?>(null) }
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        CarburNumber.entries.forEach { n ->
            if (n == CarburNumber.REV_LOW) Text(stringResource(R.string.carbur_protection_of_engine_title), Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 4.dp), style = MaterialTheme.typography.titleMedium)
            ParameterValueRow(stringResource(n.title), if (n == CarburNumber.VALVE_PRESSURE) "dis.kPa" else stringResource(n.units), data.numbers.getValue(n), n.precision, "carbur_${n.name.lowercase()}") { onEdit(n) }
            if (n == CarburNumber.TPS) {
                Row(Modifier.fillMaxWidth().testTag("carbur_invers").padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.carbur_sensor_inverse_title), Modifier.weight(1f).clickable { onInvers(!data.invers) }.padding(end = 12.dp))
                    Switch(checked = data.invers, onCheckedChange = onInvers, modifier = Modifier.testTag("carbur_invers-switch"))
                }
            }
        }
        CarburChoice(stringResource(R.string.carbur_use_uniout_condition_title), selectorValue(data.fuelCutUni, options), "carbur_fuelcut_uni") { open = false }
        CarburChoice(stringResource(R.string.carbur_use_uniout_condition_for_ign_cutoff_title), selectorValue(data.ignitionCutUni, options), "carbur_igncut_uni") { open = true }
    }
    open?.let { ignition ->
        AlertDialog(onDismissRequest = { open = null }, title = { Text(if (ignition) stringResource(R.string.carbur_use_uniout_condition_for_ign_cutoff_title) else stringResource(R.string.carbur_use_uniout_condition_title)) },
            text = { Column(Modifier.verticalScroll(rememberScrollState())) { options.forEachIndexed { i, option -> Text(option, Modifier.fillMaxWidth().clickable { onSelector(ignition, i); open = null }.padding(12.dp).testTag("carbur_option_$i")) } } },
            confirmButton = { TextButton(onClick = { open = null }) { Text(stringResource(android.R.string.cancel)) } })
    }
}

private fun selectorValue(code: Int, options: List<String>): String = CarburValues.selectorCodes.indexOf(code).takeIf { it >= 0 }?.let(options::getOrNull) ?: "Code $code"

@Composable
private fun ParameterValueRow(title: String, units: String, value: Float, precision: Int, tag: String, onClick: () -> Unit) {
    Column {
        Row(Modifier.fillMaxWidth().clickable(onClick = onClick).testTag(tag).padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(title, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
            Text(String.format(java.util.Locale.getDefault(), "%.${precision}f", value) + if (units.isEmpty()) "" else " $units", Modifier.testTag("$tag-value"), textAlign = TextAlign.End)
        }
        HorizontalDivider()
    }
}

@Composable
private fun CarburChoice(title: String, value: String, tag: String, onClick: () -> Unit) {
    Surface(Modifier.fillMaxWidth().semantics(mergeDescendants = true) { role = Role.Button; contentDescription = "$title, $value"; stateDescription = value }.clickable(role = Role.Button, onClick = onClick).testTag(tag), shape = RoundedCornerShape(4.dp), color = MaterialTheme.colorScheme.surface, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { Text(title, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant); Text(value, Modifier.padding(top = 4.dp), style = MaterialTheme.typography.bodyLarge) }
            Icon(Icons.Filled.ArrowDropDown, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.testTag("$tag-dropdown-indicator"))
        }
    }
}
