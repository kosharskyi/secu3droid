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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import org.secu3.android.ui.parameters.compose.ParameterLoadingIndicator
import org.secu3.android.R
import org.secu3.android.models.packets.out.params.ChokeControlParPacket
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

data class ChokeValues(val numbers: Map<ChokeNumber, Float>, val flags: List<Boolean>, val testing: Boolean, val frequency: Int) {
    fun value(number: ChokeNumber) = numbers.getValue(number)
    companion object {
        fun from(p: ChokeControlParPacket) = ChokeValues(
            mapOf(ChokeNumber.STEPS to p.smSteps.toFloat(), ChokeNumber.REGULATOR_FACTOR to p.rpmIf,
                ChokeNumber.COLD_DURATION to p.corrTime0, ChokeNumber.HOT_DURATION to p.corrTime1,
                ChokeNumber.CRANK_TO_RUN to p.injCrankToRunTime),
            listOf(p.useClosedLoopRmpRegulator, p.dontUseRpmRegOnGas, p.useThrottlePosInChokeInit, p.maxSTEPfreqAtInit),
            p.testing, p.smFreq,
        )
    }
}

fun applyChokeNumber(p: ChokeControlParPacket, n: ChokeNumber, value: Float) {
    when (n) {
        ChokeNumber.STEPS -> p.smSteps = value.toInt().coerceIn(50, 2000)
        ChokeNumber.REGULATOR_FACTOR -> p.rpmIf = value.coerceIn(.01f, 1f)
        ChokeNumber.COLD_DURATION -> p.corrTime0 = value.coerceIn(.1f, 10f)
        ChokeNumber.HOT_DURATION -> p.corrTime1 = value.coerceIn(.1f, 10f)
        ChokeNumber.CRANK_TO_RUN -> p.injCrankToRunTime = value.coerceIn(.1f, 99f)
        else -> Unit
    }
}

/** A pending manual movement delta that is only copied into a packet on release. */
class ChokeManualDelta {
    var value: Int = 0
        private set
    fun step(direction: Int) { value = (value + direction.coerceIn(-1, 1)).coerceIn(-127, 127) }
    fun clear() { value = 0 }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChokeScreen(
    values: ChokeValues?, onEdit: (ChokeNumber) -> Unit,
    onToggle: (ChokeNumber, Boolean) -> Unit, onFrequency: (Int) -> Unit,
    onManualStep: (Int) -> Unit, onManualRelease: () -> Unit, onManualCancel: () -> Unit,
    onTest: () -> Unit,
) {
    if (values == null) {
        ParameterLoadingIndicator("choke_loading")
        return
    }
    DisposableEffect(Unit) { onDispose(onManualCancel) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.choke_manual_control), style = MaterialTheme.typography.titleMedium)
        NumberRow(ChokeNumber.STEPS, values, onEdit)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                HoldButton("choke_manual_dec", -1, stringResource(R.string.choke_manual_decrease), onManualStep, onManualRelease, onManualCancel, "‹",
                    RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp))
                HoldButton("choke_manual_inc", 1, stringResource(R.string.choke_manual_increase), onManualStep, onManualRelease, onManualCancel, "›",
                    RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp))
            }
            Spacer(Modifier.weight(1f))
            Button(onClick = onTest, modifier = Modifier.testTag("choke_test"), colors = ButtonDefaults.buttonColors(
                containerColor = if (values.testing) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                contentColor = androidx.compose.ui.graphics.Color.White
            )) {
                Text(if (values.testing) stringResource(R.string.choke_test_active) else stringResource(R.string.test), color = androidx.compose.ui.graphics.Color.White)
            }
        }
        HorizontalDivider()
        Text(stringResource(R.string.choke_use_closed_loop_rpm_regulator_title), style = MaterialTheme.typography.titleMedium)
        NumberRow(ChokeNumber.REGULATOR_FACTOR, values, onEdit)
        listOf(ChokeNumber.CLOSED_LOOP, ChokeNumber.DISABLE_ON_GAS).forEachIndexed { i, n ->
            ParameterSwitchRow(stringResource(n.title), values.flags[i], "choke_${n.name.lowercase()}") { onToggle(n, it) }
        }
        HorizontalDivider()
        Text(stringResource(R.string.choke_settings), style = MaterialTheme.typography.titleMedium)
        NumberRow(ChokeNumber.COLD_DURATION, values, onEdit)
        NumberRow(ChokeNumber.HOT_DURATION, values, onEdit)
        listOf(ChokeNumber.THROTTLE_INIT, ChokeNumber.MAX_FREQUENCY_INIT).forEachIndexed { i, n ->
            ParameterSwitchRow(stringResource(n.title), values.flags[i + 2], "choke_${n.name.lowercase()}") { onToggle(n, it) }
        }
        FrequencySelector(values.frequency, onFrequency)
        NumberRow(ChokeNumber.CRANK_TO_RUN, values, onEdit)
    }
}

@Composable private fun NumberRow(n: ChokeNumber, values: ChokeValues, onEdit: (ChokeNumber) -> Unit) {
    ParameterValueRow(stringResource(n.title), if (n.units == R.string.units_sec) stringResource(n.units) else "",
        values.value(n), n.precision, "choke_${n.name.lowercase()}") { onEdit(n) }
}

@Composable private fun FrequencySelector(index: Int, onSelect: (Int) -> Unit) {
    val options = listOf("300", "150", "100", "75")
    var expanded by remember { mutableStateOf(false) }
    Column {
        OutlinedButton(onClick = { expanded = true }, Modifier.fillMaxWidth().testTag("choke_frequency")) {
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.Start) {
                Text(stringResource(R.string.choke_frequency_of_pulses), style = MaterialTheme.typography.labelMedium)
                Text(options.getOrNull(index) ?: "Code $index")
            }
            Text("⌄", style = MaterialTheme.typography.titleLarge)
        }
        DropdownMenu(expanded, { expanded = false }) {
            options.forEachIndexed { i, option -> DropdownMenuItem(text = { Text(option) }, onClick = { expanded = false; onSelect(i) }, modifier = Modifier.testTag("choke_frequency_option_$i")) }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable private fun HoldButton(tag: String, direction: Int, label: String, onStep: (Int) -> Unit, onRelease: () -> Unit, onCancel: () -> Unit, icon: String, shape: RoundedCornerShape) {
    var pressed by remember(tag) { mutableStateOf(false) }
    val baseColor = MaterialTheme.colorScheme.primary
    val pressedColor = lerp(baseColor, Color.Black, .25f)
    Surface(color = if (pressed) pressedColor else baseColor, contentColor = Color.White,
        shape = shape, modifier = Modifier.testTag(tag).size(width = 64.dp, height = 48.dp)
            .semantics {
                role = Role.Button
                onClick(label) { onStep(direction); onRelease(); true }
            }.pointerInput(direction, onStep, onRelease, onCancel) {
        awaitEachGesture {
            val down = awaitFirstDown(false)
            pressed = true
            try {
                onStep(direction)
                val up = waitForUpOrCancellation()
                if (up == null) {
                    onCancel()
                } else {
                    val heldMillis = (up.uptimeMillis - down.uptimeMillis).coerceAtLeast(0L)
                    if (heldMillis >= 400L) {
                        repeat(1 + ((heldMillis - 400L) / 80L).toInt()) { onStep(direction) }
                    }
                    onRelease()
                }
            } finally {
                pressed = false
            }
        }
    }) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(icon, color = Color.White, style = MaterialTheme.typography.headlineSmall) }
    }
}
