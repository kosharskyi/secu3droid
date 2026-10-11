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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import org.secu3.android.R
import org.secu3.android.models.packets.out.params.GasDoseParamPacket
import org.secu3.android.ui.parameters.compose.ParameterLoadingIndicator
import org.secu3.android.ui.parameters.compose.ParameterSwitchRow
import org.secu3.android.ui.parameters.compose.ParameterValueRow

private val gasDoseFrequencies = listOf("300", "150", "100", "75")

enum class GasDoseNumber(@StringRes val title: Int, @StringRes val units: Int, val precision: Int, val min: Float, val max: Float, val step: Float, val integer: Boolean = false) {
    STEPS(R.string.choke_steps_title, R.string.units_str, 0, 10f, 2000f, 1f, true),
    STOICHIOMETRIC_RATIO(R.string.gasdose_par_stoich_ratio, R.string.units_afr, 1, 8f, 22f, .1f),
    FUEL_CUT_CLOSING(R.string.gasdose_par_closing_on_fuel_cut, R.string.units_percents, 1, 0f, 100f, .5f),
    CORRECTION_POSITIVE(R.string.lambda_par_correction_positive_limit_title, R.string.units_percents, 2, .5f, 100f, .5f),
    CORRECTION_NEGATIVE(R.string.lambda_par_correction_negative_limit_title, R.string.units_percents, 2, .5f, 100f, .5f),
}

data class GasDoseValues(val steps: Float, val stoichiometricRatio: Float, val fuelCutClosing: Float, val correctionPositive: Float, val correctionNegative: Float, val testing: Boolean, val frequency: Int, val maxFrequencyAtInit: Boolean) {
    fun value(number: GasDoseNumber) = when (number) {
        GasDoseNumber.STEPS -> steps
        GasDoseNumber.STOICHIOMETRIC_RATIO -> stoichiometricRatio
        GasDoseNumber.FUEL_CUT_CLOSING -> fuelCutClosing
        GasDoseNumber.CORRECTION_POSITIVE -> correctionPositive
        GasDoseNumber.CORRECTION_NEGATIVE -> correctionNegative
    }
    companion object {
        fun from(p: GasDoseParamPacket) = GasDoseValues(p.steps.toFloat(), p.lambdaStoichval, p.fcClosing, p.lambdaCorrLimitP, p.lambdaCorrLimitM, p.testing, p.freq, p.maxFreqInit > 0)
    }
}

fun applyGasDoseNumber(p: GasDoseParamPacket, n: GasDoseNumber, value: Float) {
    when (n) {
        GasDoseNumber.STEPS -> p.steps = value.toInt().coerceIn(10, 2000)
        GasDoseNumber.STOICHIOMETRIC_RATIO -> p.lambdaStoichval = value.coerceIn(8f, 22f)
        GasDoseNumber.FUEL_CUT_CLOSING -> p.fcClosing = value.coerceIn(0f, 100f)
        GasDoseNumber.CORRECTION_POSITIVE -> p.lambdaCorrLimitP = value.coerceIn(.5f, 100f)
        GasDoseNumber.CORRECTION_NEGATIVE -> p.lambdaCorrLimitM = value.coerceIn(.5f, 100f)
    }
}

class GasDoseManualDelta {
    var value: Int = 0
        private set
    fun step(direction: Int) { value = (value + direction.coerceIn(-1, 1)).coerceIn(-127, 127) }
    fun clear() { value = 0 }
}

@Composable
fun GasDoseScreen(values: GasDoseValues?, firmwareAvailable: Boolean, onEdit: (GasDoseNumber) -> Unit, onFrequency: (Int) -> Unit, onMaximumFrequency: (Boolean) -> Unit, onManualStep: (Int) -> Unit, onManualRelease: () -> Unit, onManualCancel: () -> Unit, onTest: () -> Unit) {
    if (!firmwareAvailable) {
        Box(Modifier.fillMaxSize().testTag("gas_dose_unavailable"), contentAlignment = androidx.compose.ui.Alignment.Center) {
            Text(stringResource(R.string.gas_dose_unavailable), modifier = Modifier.padding(24.dp))
        }
        return
    }
    if (values == null) { ParameterLoadingIndicator("gas_dose_loading"); return }
    androidx.compose.runtime.DisposableEffect(Unit) { onDispose(onManualCancel) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.params_tab_gas_dose_title), style = MaterialTheme.typography.titleMedium)
        NumberRow(GasDoseNumber.STEPS, values, onEdit)
        Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                ManualHoldButton("gas_dose_manual_dec", -1, stringResource(R.string.gas_dose_manual_decrease), "‹", onManualStep, onManualRelease, onManualCancel, true)
                ManualHoldButton("gas_dose_manual_inc", 1, stringResource(R.string.gas_dose_manual_increase), "›", onManualStep, onManualRelease, onManualCancel, false)
            }
            Spacer(Modifier.weight(1f))
            Button(onClick = onTest, modifier = Modifier.testTag("gas_dose_test"), colors = ButtonDefaults.buttonColors(containerColor = if (values.testing) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary, contentColor = androidx.compose.ui.graphics.Color.White)) {
                Text(stringResource(if (values.testing) R.string.choke_test_active else R.string.test), color = androidx.compose.ui.graphics.Color.White)
            }
        }
        HorizontalDivider()
        NumberRow(GasDoseNumber.STOICHIOMETRIC_RATIO, values, onEdit)
        NumberRow(GasDoseNumber.FUEL_CUT_CLOSING, values, onEdit)
        NumberRow(GasDoseNumber.CORRECTION_POSITIVE, values, onEdit)
        NumberRow(GasDoseNumber.CORRECTION_NEGATIVE, values, onEdit)
        Column {
            var expanded = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
            OutlinedButton(onClick = { expanded.value = true }, Modifier.fillMaxWidth().testTag("gas_dose_frequency"), shape = DropdownShape) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.choke_frequency_of_pulses), style = MaterialTheme.typography.labelMedium)
                    Text(gasDoseFrequencies.getOrNull(values.frequency) ?: "Code ${values.frequency}")
                }
                Text("⌄")
            }
            DropdownMenu(expanded.value, { expanded.value = false }, shape = DropdownShape) {
                gasDoseFrequencies.forEachIndexed { i, option -> DropdownMenuItem(text = { Text(option) }, onClick = { expanded.value = false; onFrequency(i) }, modifier = Modifier.testTag("gas_dose_frequency_option_$i")) }
            }
        }
        ParameterSwitchRow(stringResource(R.string.maximum_step_frequency_at_init), values.maxFrequencyAtInit, "gas_dose_max_frequency") { onMaximumFrequency(it) }
    }
}

@Composable private fun NumberRow(n: GasDoseNumber, values: GasDoseValues, onEdit: (GasDoseNumber) -> Unit) {
    ParameterValueRow(stringResource(n.title), if (n.units == R.string.units_str) "" else stringResource(n.units), values.value(n), n.precision, "gas_dose_${n.name.lowercase()}") { onEdit(n) }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable private fun ManualHoldButton(tag: String, direction: Int, label: String, icon: String, onStep: (Int) -> Unit, onRelease: () -> Unit, onCancel: () -> Unit, start: Boolean) {
    val pressed = androidx.compose.runtime.remember(tag) { androidx.compose.runtime.mutableStateOf(false) }
    val color = androidx.compose.ui.graphics.lerp(MaterialTheme.colorScheme.primary, androidx.compose.ui.graphics.Color.Black, if (pressed.value) .25f else 0f)
    Surface(color = color, contentColor = androidx.compose.ui.graphics.Color.White, shape = androidx.compose.foundation.shape.RoundedCornerShape(topStart = if (start) 24.dp else 0.dp, bottomStart = if (start) 24.dp else 0.dp, topEnd = if (start) 0.dp else 24.dp, bottomEnd = if (start) 0.dp else 24.dp), modifier = Modifier.testTag(tag).size(64.dp, 48.dp).semantics {
        role = Role.Button
        onClick(label) { onStep(direction); onRelease(); true }
    }.pointerInput(direction, onStep, onRelease, onCancel) {
        awaitEachGesture {
            val down = awaitFirstDown(false)
            pressed.value = true
            try {
                onStep(direction)
                val up = waitForUpOrCancellation()
                if (up == null) onCancel() else {
                    val held = (up.uptimeMillis - down.uptimeMillis).coerceAtLeast(0)
                    if (held >= 400) repeat(1 + ((held - 400) / 80).toInt()) { onStep(direction) }
                    onRelease()
                }
            } finally { pressed.value = false }
        }
    }) { Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) { Text(icon, color = androidx.compose.ui.graphics.Color.White, style = MaterialTheme.typography.headlineSmall) } }
}
