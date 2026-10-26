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
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.secu3.android.ui.parameters.compose.ParameterLoadingIndicator
import org.secu3.android.ui.parameters.compose.ParameterPage
import org.secu3.android.ui.parameters.compose.ParameterSection
import org.secu3.android.ui.parameters.compose.ParameterSelectionRow
import org.secu3.android.ui.parameters.dialogs.ParameterChoice
import org.secu3.android.ui.parameters.dialogs.ParameterChoiceDialog
import org.secu3.android.ui.parameters.compose.ParameterTextValueRow
import org.secu3.android.ui.parameters.*
import org.secu3.android.R
import org.secu3.android.ui.parameters.compose.ParameterSwitchRow
import org.secu3.android.ui.parameters.compose.ParameterValueRow

enum class ChokeNumber(@StringRes val title: Int, val precision: Int, val min: Float, val max: Float, val step: Float, @StringRes val units: Int = R.string.units_str) {
    STEPS(R.string.choke_steps_title, 0, 50f, 2000f, 1f, R.string.units_str),
    REGULATOR_FACTOR(R.string.choke_rpm_if_factor_title, 2, .01f, 1f, .01f, R.string.units_str),
    COLD_DURATION(R.string.choke_cranking_map_lasting_on_cold_title, 1, .1f, 10f, .1f, R.string.units_sec),
    HOT_DURATION(R.string.choke_cranking_map_lasting_on_hot_title, 1, .1f, 10f, .1f, R.string.units_sec),
    CRANK_TO_RUN(R.string.choke_time_from_crank_to_run_position_for_choke_title, 2, .1f, 99f, .01f, R.string.units_sec),
    CLOSED_LOOP(R.string.choke_use_closed_loop_rpm_regulator_title, 0, 0f, 1f, 1f),
    DISABLE_ON_GAS(R.string.choke_dont_use_rpm_regulator_on_gas_title, 0, 0f, 1f, 1f),
    THROTTLE_INIT(R.string.choke_use_throttle_position_in_choke_init_title, 0, 0f, 1f, 1f),
    MAX_FREQUENCY_INIT(R.string.maximum_step_frequency_at_init, 0, 0f, 1f, 1f),
}

enum class ChokeSelector(@StringRes val title: Int) { FREQUENCY(R.string.choke_frequency_of_pulses) }

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChokeScreen(
    values: ChokeValues?, onEdit: (ChokeNumber) -> Unit,
    onToggle: (ChokeNumber, Boolean) -> Unit, onFrequency: (Int) -> Unit,
    onManualStep: (Int) -> Unit, onManualFlush: () -> Unit, onManualRelease: () -> Unit, onManualCancel: () -> Unit,
    onTest: () -> Unit, isActive: Boolean = true,
    capabilities: ParameterCapabilities = ParameterCapabilities.All,
) {
    val pageScrollState = rememberScrollState()
    if (values == null) {
        ParameterLoadingIndicator("choke_loading")
        return
    }
    DisposableEffect(isActive) { if (!isActive) onManualCancel(); onDispose(onManualCancel) }
    ParameterPage(scrollState = pageScrollState) {
        ParameterSection(stringResource(R.string.choke_manual_control)) {
        NumberRow(ChokeNumber.STEPS, values, onEdit)
        Row(Modifier.fillMaxWidth().heightIn(min = 66.dp).padding(horizontal = 14.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                HoldButton("choke_manual_dec", -1, stringResource(R.string.choke_manual_decrease), onManualStep, onManualFlush, onManualRelease, onManualCancel, "‹", isActive && capabilities.choke,
                    RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp))
                HoldButton("choke_manual_inc", 1, stringResource(R.string.choke_manual_increase), onManualStep, onManualFlush, onManualRelease, onManualCancel, "›", isActive && capabilities.choke,
                    RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp))
            }
            Spacer(Modifier.weight(1f))
            Button(onClick = onTest, enabled = capabilities.choke, modifier = Modifier.testTag("choke_test"), colors = ButtonDefaults.buttonColors(
                containerColor = if (values.testing) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            )) {
                Text(if (values.testing) stringResource(R.string.choke_test_active) else stringResource(R.string.test))
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
        ParameterSection(stringResource(R.string.choke_use_closed_loop_rpm_regulator_title)) {
        NumberRow(ChokeNumber.REGULATOR_FACTOR, values, onEdit, capabilities.carburetor)
        listOf(ChokeNumber.CLOSED_LOOP, ChokeNumber.DISABLE_ON_GAS).forEachIndexed { i, n ->
            ParameterSwitchRow(stringResource(n.title), values.flags[i], "choke_${n.name.lowercase()}", enabled = capabilities.carburetor) { onToggle(n, it) }
        }
        }
        ParameterSection(stringResource(R.string.choke_settings)) {
        NumberRow(ChokeNumber.COLD_DURATION, values, onEdit, capabilities.carburetor)
        NumberRow(ChokeNumber.HOT_DURATION, values, onEdit, capabilities.carburetor)
        listOf(ChokeNumber.THROTTLE_INIT, ChokeNumber.MAX_FREQUENCY_INIT).forEachIndexed { i, n ->
            ParameterSwitchRow(stringResource(n.title), values.flags[i + 2], "choke_${n.name.lowercase()}") { onToggle(n, it) }
        }
        FrequencySelector(values.frequency, onFrequency)
        NumberRow(ChokeNumber.CRANK_TO_RUN, values, onEdit, capabilities.carburetor)
        }
        }
}

@Composable private fun NumberRow(n: ChokeNumber, values: ChokeValues, onEdit: (ChokeNumber) -> Unit, enabled: Boolean = true) {
    ParameterValueRow(stringResource(n.title), if (n.units == R.string.units_sec) stringResource(n.units) else "",
        values.value(n), n.precision, "choke_${n.name.lowercase()}", enabled = enabled) { onEdit(n) }
}

@Composable private fun FrequencySelector(index: Int, onSelect: (Int) -> Unit) {
    val options = listOf("300", "150", "100", "75")
    var expanded by remember { mutableStateOf(false) }
    Box {
        ParameterSelectionRow(stringResource(R.string.choke_frequency_of_pulses), options.getOrNull(index) ?: "Code $index", "choke_frequency") { expanded = true }
        if (expanded) ParameterChoiceDialog(
            title = stringResource(R.string.choke_frequency_of_pulses),
            choices = options.mapIndexed { i, option -> ParameterChoice(i, option, "choke_frequency_option_$i") },
            onDismiss = { expanded = false },
            onSelect = onSelect,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable private fun HoldButton(tag: String, direction: Int, label: String, onStep: (Int) -> Unit, onFlush: () -> Unit, onRelease: () -> Unit, onCancel: () -> Unit, icon: String, isActive: Boolean, shape: RoundedCornerShape) {
    var pressed by remember(tag) { mutableStateOf(false) }
    val currentOnStep by rememberUpdatedState(onStep)
    val currentOnFlush by rememberUpdatedState(onFlush)
    val currentOnRelease by rememberUpdatedState(onRelease)
    val currentOnCancel by rememberUpdatedState(onCancel)
    val timerScope = rememberCoroutineScope()
    val baseColor = MaterialTheme.colorScheme.primary
    val pressedColor = MaterialTheme.colorScheme.primaryContainer
    val contentColor = if (pressed) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onPrimary
    Surface(color = if (pressed) pressedColor else baseColor, contentColor = contentColor,
        shape = shape, modifier = Modifier.testTag(tag).size(width = 64.dp, height = 48.dp)
            .semantics {
                role = Role.Button
                onClick(label) { if (isActive) { currentOnStep(direction); currentOnRelease() }; true }
            }.pointerInput(direction, isActive) {
                if (!isActive) return@pointerInput
        awaitEachGesture {
            awaitFirstDown(false)
            pressed = true
            var repeatJob: kotlinx.coroutines.Job? = null
            var flushJob: kotlinx.coroutines.Job? = null
            try {
                currentOnStep(direction)
                repeatJob = timerScope.launch {
                    delay(400L)
                    while (true) {
                        currentOnStep(direction)
                        delay(80L)
                    }
                }
                flushJob = timerScope.launch {
                    while (true) {
                        delay(250L)
                        currentOnFlush()
                    }
                }
                val up = waitForUpOrCancellation()
                repeatJob.cancel()
                flushJob.cancel()
                if (up == null) currentOnCancel() else currentOnRelease()
            } finally {
                repeatJob?.cancel()
                flushJob?.cancel()
                pressed = false
            }
        }
    }) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(icon, style = MaterialTheme.typography.headlineSmall) }
    }
}
