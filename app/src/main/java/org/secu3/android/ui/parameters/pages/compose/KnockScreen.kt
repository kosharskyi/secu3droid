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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.secu3.android.R
import org.secu3.android.models.packets.out.params.KnockParamPacket
import org.secu3.android.ui.parameters.compose.ParameterSwitchRow
import org.secu3.android.ui.parameters.compose.ParameterValueRow

enum class KnockNumber(@StringRes val title: Int, val units: KnockUnits, val precision: Int, val min: Float, val max: Float, val step: Float) {
    BEGIN(R.string.knock_par_phase_window_begin_title, KnockUnits.DEGREES, 2, -12f, 54f, 1f),
    END(R.string.knock_par_phase_window_end_title, KnockUnits.DEGREES, 2, -12f, 54f, 1f),
    RETARD(R.string.knock_par_angle_displacement_step_title, KnockUnits.DEGREES, 2, 0f, 20f, .25f),
    ADVANCE(R.string.knock_par_angle_retard_step_title, KnockUnits.DEGREES, 2, 0f, 5f, .2f),
    MAX_RETARD(R.string.knock_par_maximal_angle_displacement_title, KnockUnits.DEGREES, 2, 0f, 25f, .25f),
    THRESHOLD(R.string.knock_par_knock_threshold_title, KnockUnits.VOLTS, 2, .1f, 5f, .01f),
    DELAY(R.string.knock_par_angle_recovery_delay_title, KnockUnits.STR, 0, 1f, 99f, 1f),
    CONTROL(R.string.knock_par_knock_threshold_control_title, KnockUnits.CELSIUS, 2, -40f, 120f, .25f),
}
fun applyKnockNumber(packet: KnockParamPacket, number: KnockNumber, value: Float) {
    when (number) {
        KnockNumber.BEGIN -> packet.kWndBeginAngle = value
        KnockNumber.END -> packet.kWndEndAngle = value
        KnockNumber.RETARD -> packet.retardStep = value
        KnockNumber.ADVANCE -> packet.advanceStep = value
        KnockNumber.MAX_RETARD -> packet.maxRetard = value
        KnockNumber.THRESHOLD -> packet.threshold = value
        KnockNumber.DELAY -> packet.recoveryDelay = value.toInt()
        KnockNumber.CONTROL -> packet.knkctlThrd = value
    }
}

enum class KnockUnits(@StringRes val resource: Int) {
    DEGREES(R.string.units_degrees_cs),
    VOLTS(R.string.units_volts),
    STR(R.string.units_str),
    CELSIUS(R.string.units_degrees_celcius),
}

enum class KnockSelector(@StringRes val title: Int) {
    BPF(R.string.knock_par_knock_filter_frequency_title),
    INTEGRATION(R.string.knock_par_integration_timeout_title),
}
data class KnockValues(
    val numbers: Map<KnockNumber, Float>,
    val selectedChannels: Int,
    val bpfIndex: Int,
    val integrationIndex: Int,
    val bpfOptions: List<String>,
    val integrationOptions: List<String>,
) {
    fun value(number: KnockNumber): Float = numbers.getValue(number)
    fun channels(): List<Boolean> = (0..7).map { selectedChannels and (1 shl it) != 0 }
    fun selector(selector: KnockSelector): String {
        val index = if (selector == KnockSelector.BPF) bpfIndex else integrationIndex
        val options = if (selector == KnockSelector.BPF) bpfOptions else integrationOptions
        return options.getOrNull(index) ?: "Code $index"
    }

    companion object {
        fun from(packet: KnockParamPacket, bpfOptions: List<String>, integrationOptions: List<String>) = KnockValues(
            numbers = mapOf(
                KnockNumber.BEGIN to packet.kWndBeginAngle,
                KnockNumber.END to packet.kWndEndAngle,
                KnockNumber.RETARD to packet.retardStep,
                KnockNumber.ADVANCE to packet.advanceStep,
                KnockNumber.MAX_RETARD to packet.maxRetard,
                KnockNumber.THRESHOLD to packet.threshold,
                KnockNumber.DELAY to packet.recoveryDelay.toFloat(),
                KnockNumber.CONTROL to packet.knkctlThrd,
            ),
            selectedChannels = packet.selectedChanels,
            bpfIndex = packet.bpfFrequency,
            integrationIndex = packet.intTimeCost,
            bpfOptions = bpfOptions.toList(),
            integrationOptions = integrationOptions.toList(),
        )
    }
}

sealed interface KnockUiState {
    data object Loading : KnockUiState
    data class Data(val values: KnockValues, val enabled: Boolean) : KnockUiState
}

@Composable
fun KnockScreen(
    state: KnockUiState,
    onEdit: (KnockNumber) -> Unit,
    onEnable: (Boolean) -> Unit,
    onChannel: (Int, Boolean) -> Unit,
    onSelector: (KnockSelector, Int) -> Unit,
) {
    when (state) {
        KnockUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(Modifier.testTag("knock_loading"))
        }

        is KnockUiState.Data -> Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ParameterSwitchRow(
                title = stringResource(R.string.knock_par_use_knock_sensor_title),
                checked = state.enabled,
                testTag = "knock_enable",
                onCheckedChange = onEnable,
            )
            listOf(KnockNumber.BEGIN, KnockNumber.END).forEach { number ->
                NumberRow(number, state.values, onEdit)
            }
            KnockSelector.entries.forEach { selector ->
                val options = if (selector == KnockSelector.BPF) {
                    state.values.bpfOptions
                } else {
                    state.values.integrationOptions
                }
                KnockSelectorRow(selector, state.values.selector(selector), options) { index ->
                    onSelector(selector, index)
                }
            }
            listOf(
                KnockNumber.RETARD,
                KnockNumber.ADVANCE,
                KnockNumber.MAX_RETARD,
                KnockNumber.THRESHOLD,
                KnockNumber.DELAY,
            ).forEach { number ->
                NumberRow(number, state.values, onEdit)
            }
            Text(
                stringResource(R.string.knock_par_select_knock_sensor_title),
                style = MaterialTheme.typography.titleMedium,
            )
            (0..7).chunked(4).forEach { channels ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    channels.forEach { channel ->
                        Row(
                            Modifier.weight(1f).testTag("knock_channel_${channel + 1}"),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(
                                checked = state.values.channels()[channel],
                                onCheckedChange = { onChannel(channel, it) },
                                modifier = Modifier.testTag("knock_channel_checkbox_${channel + 1}"),
                            )
                            Text("${channel + 1}")
                        }
                    }
                }
            }
            NumberRow(KnockNumber.CONTROL, state.values, onEdit)
        }
    }
}

@Composable
private fun NumberRow(
    number: KnockNumber,
    values: KnockValues,
    onEdit: (KnockNumber) -> Unit,
) {
    ParameterValueRow(
        title = stringResource(number.title),
        units = stringResource(number.units.resource),
        value = values.value(number),
        precision = number.precision,
        testTag = "knock_${number.name.lowercase()}",
        onClick = { onEdit(number) },
    )
}

@Composable
private fun KnockSelectorRow(
    selector: KnockSelector,
    value: String,
    options: List<String>,
    onSelect: (Int) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Column {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth().testTag("knock_selector_${selector.name.lowercase()}"),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 14.dp),
        ) {
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.Start) {
                Text(stringResource(selector.title), style = MaterialTheme.typography.labelMedium)
                Text(value)
            }
            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEachIndexed { index, option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        expanded = false
                        onSelect(index)
                    },
                    modifier = Modifier.testTag("knock_selector_option_$index"),
                )
            }
        }
    }
}
