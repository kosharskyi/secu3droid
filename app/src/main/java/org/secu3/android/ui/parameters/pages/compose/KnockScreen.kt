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
import org.secu3.android.ui.parameters.compose.ParameterLoadingIndicator
import org.secu3.android.R
import org.secu3.android.domain.parameters.KnockValues
import org.secu3.android.ui.parameters.compose.ParameterSwitchRow
import org.secu3.android.ui.parameters.compose.ParameterValueRow
import org.secu3.android.ui.parameters.compose.ParameterPage
import org.secu3.android.ui.parameters.compose.ParameterSection
import org.secu3.android.ui.parameters.compose.ParameterSelectionRow
import org.secu3.android.ui.parameters.dialogs.ParameterChoice
import org.secu3.android.ui.parameters.dialogs.ParameterChoiceDialog

enum class KnockNumber(@StringRes val title: Int, val units: KnockUnits, val precision: Int, val min: Float, val max: Float, val step: Float) {
    BEGIN(R.string.knock_par_phase_window_begin_title, KnockUnits.DEGREES, 2, -12f, 54f, 1f),
    END(R.string.knock_par_phase_window_end_title, KnockUnits.DEGREES, 2, -12f, 54f, 1f),
    RETARD(R.string.knock_par_angle_displacement_step_title, KnockUnits.DEGREES, 2, 0f, 20f, .25f),
    ADVANCE(R.string.knock_par_angle_retard_step_title, KnockUnits.DEGREES, 2, 0f, 5f, .02f),
    MAX_RETARD(R.string.knock_par_maximal_angle_displacement_title, KnockUnits.DEGREES, 2, 0f, 25f, .25f),
    THRESHOLD(R.string.knock_par_knock_threshold_title, KnockUnits.VOLTS, 2, .1f, 5f, .01f),
    DELAY(R.string.knock_par_angle_recovery_delay_title, KnockUnits.STR, 0, 1f, 99f, 1f),
    CONTROL(R.string.knock_par_knock_threshold_control_title, KnockUnits.CELSIUS, 2, -40f, 120f, .25f);

    fun value(values: KnockValues): Float = when (this) {
        BEGIN -> values.windowBegin
        END -> values.windowEnd
        RETARD -> values.retardStep
        ADVANCE -> values.advanceStep
        MAX_RETARD -> values.maxRetard
        THRESHOLD -> values.voltageThreshold
        DELAY -> values.recoveryDelay.toFloat()
        CONTROL -> values.coolantThreshold
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
@Composable
fun KnockScreen(
    values: KnockValues?,
    bpfOptions: List<String>,
    integrationOptions: List<String>,
    onEdit: (KnockNumber) -> Unit,
    onEnable: (Boolean) -> Unit,
    onChannel: (Int, Boolean) -> Unit,
    onSelector: (KnockSelector, Int) -> Unit,
) {
    val pageScrollState = rememberScrollState()
    if (values == null) {
        ParameterLoadingIndicator("knock_loading")
    } else {
        ParameterPage(scrollState = pageScrollState) {
            ParameterSection {
                ParameterSwitchRow(
                    title = stringResource(R.string.knock_par_use_knock_sensor_title),
                    checked = values.useKnockChannel > 0,
                    testTag = "knock_enable",
                    onCheckedChange = onEnable,
                )
                listOf(KnockNumber.BEGIN, KnockNumber.END).forEach { number ->
                    NumberRow(number, values, onEdit)
                }
                KnockSelector.entries.forEach { selector ->
                    val options = if (selector == KnockSelector.BPF) {
                        bpfOptions
                    } else {
                        integrationOptions
                    }
                    val code = if (selector == KnockSelector.BPF) values.bpfFrequencyCode else values.integrationTimeoutCode
                    val selected = options.getOrNull(code) ?: "Code $code"
                    KnockSelectorRow(selector, selected, options) { code ->
                        onSelector(selector, code)
                    }
                }
                listOf(
                    KnockNumber.RETARD,
                    KnockNumber.ADVANCE,
                    KnockNumber.MAX_RETARD,
                    KnockNumber.THRESHOLD,
                    KnockNumber.DELAY,
                ).forEach { number ->
                    NumberRow(number, values, onEdit)
                }
                Text(
                    stringResource(R.string.knock_par_select_knock_sensor_title),
                    Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 11.dp),
                    style = MaterialTheme.typography.titleMedium,
                )
                (0..7).chunked(4).forEachIndexed { rowIndex, channels ->
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 66.dp).padding(horizontal = 14.dp, vertical = 11.dp),
                        horizontalArrangement = Arrangement.spacedBy(0.dp),
                    ) {
                        channels.forEach { channel ->
                            Row(
                                Modifier.weight(1f)
                                    .testTag("knock_channel_${channel + 1}"),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Checkbox(
                                    checked = values.isChannelSelected(channel),
                                    onCheckedChange = { onChannel(channel, it) },
                                    modifier = Modifier.testTag("knock_channel_checkbox_${channel + 1}"),
                                )
                                Text("${channel + 1}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                    if (rowIndex < 1) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
                NumberRow(KnockNumber.CONTROL, values, onEdit)
            }
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
        value = number.value(values),
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
        ParameterSelectionRow(
            title = stringResource(selector.title),
            value = value,
            testTag = "knock_selector_${selector.name.lowercase()}",
            onClick = { expanded = true },
        )
        if (expanded) ParameterChoiceDialog(
            title = stringResource(selector.title),
            choices = options.mapIndexed { index, option -> ParameterChoice(index, option, "knock_selector_option_$index") },
            onDismiss = { expanded = false },
            onSelect = onSelect,
        )
    }
}
