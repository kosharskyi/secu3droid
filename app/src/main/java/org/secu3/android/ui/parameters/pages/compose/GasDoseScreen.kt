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
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.secu3.android.R
import org.secu3.android.domain.parameters.GasDoseValues
import org.secu3.android.domain.parameters.GasDoseControl
import org.secu3.android.domain.parameters.ParameterCapabilities
import org.secu3.android.ui.parameters.compose.ParameterLoadingIndicator
import org.secu3.android.ui.parameters.compose.ParameterPage
import org.secu3.android.ui.parameters.compose.ParameterSection
import org.secu3.android.ui.parameters.compose.ParameterSelectionRow
import org.secu3.android.ui.parameters.compose.ParameterTextValueRow
import org.secu3.android.ui.parameters.compose.ParameterSwitchRow
import org.secu3.android.ui.parameters.compose.ParameterValueRow
import org.secu3.android.ui.parameters.compose.ManualPositionButton
import org.secu3.android.ui.parameters.dialogs.ParameterChoice
import org.secu3.android.ui.parameters.dialogs.ParameterChoiceDialog

enum class GasDoseNumber(@StringRes val title: Int, @StringRes val units: Int, val precision: Int, val min: Float, val max: Float, val step: Float, val integer: Boolean = false) {
    STEPS(R.string.choke_steps_title, R.string.units_str, 0, GasDoseControl.STEPS_MIN, GasDoseControl.STEPS_MAX, 1f, true),
    STOICHIOMETRIC_RATIO(R.string.gasdose_par_stoich_ratio, R.string.units_afr, 1, GasDoseControl.STOICHIOMETRIC_RATIO_MIN, GasDoseControl.STOICHIOMETRIC_RATIO_MAX, .1f),
    FUEL_CUT_CLOSING(R.string.gasdose_par_closing_on_fuel_cut, R.string.units_percents, 1, GasDoseControl.FUEL_CUT_CLOSING_MIN, GasDoseControl.FUEL_CUT_CLOSING_MAX, .5f),
    CORRECTION_POSITIVE(R.string.lambda_par_correction_positive_limit_title, R.string.units_percents, 2, GasDoseControl.CORRECTION_MIN, GasDoseControl.CORRECTION_MAX, .5f),
    CORRECTION_NEGATIVE(R.string.lambda_par_correction_negative_limit_title, R.string.units_percents, 2, GasDoseControl.CORRECTION_MIN, GasDoseControl.CORRECTION_MAX, .5f);

    fun value(values: GasDoseValues) = when (this) {
        STEPS -> values.steps
        STOICHIOMETRIC_RATIO -> values.stoichiometricRatio
        FUEL_CUT_CLOSING -> values.fuelCutClosing
        CORRECTION_POSITIVE -> values.correctionPositive
        CORRECTION_NEGATIVE -> values.correctionNegative
    }
}

@Composable
fun GasDoseScreen(values: GasDoseValues?, capabilities: ParameterCapabilities, onEdit: (GasDoseNumber) -> Unit, onFrequency: (Int) -> Unit, onMaximumFrequency: (Boolean) -> Unit, onManualStep: (Int) -> Unit, onManualRelease: () -> Unit, onManualCancel: () -> Unit, onTest: () -> Unit, onManualFlush: () -> Unit = {}, isActive: Boolean = true) {
    val frequencies = listOf("300", "150", "100", "75")
    val pageScrollState = rememberScrollState()
    if (!capabilities.gasDose) {
        Box(Modifier.fillMaxSize().testTag("gas_dose_unavailable"), contentAlignment = androidx.compose.ui.Alignment.Center) {
            Text(stringResource(R.string.gas_dose_unavailable), modifier = Modifier.padding(24.dp))
        }
        return
    }
    if (values == null) { ParameterLoadingIndicator("gas_dose_loading"); return }
    androidx.compose.runtime.DisposableEffect(isActive) { if (!isActive) onManualCancel(); onDispose(onManualCancel) }
    ParameterPage(scrollState = pageScrollState) {
        ParameterSection {
        NumberRow(GasDoseNumber.STEPS, values, onEdit)
        Row(Modifier.fillMaxWidth().heightIn(min = 66.dp).padding(horizontal = 14.dp, vertical = 9.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                ManualPositionButton(
                    testTag = "gas_dose_manual_dec", direction = -1,
                    label = stringResource(R.string.gas_dose_manual_decrease), icon = "‹", leading = true,
                    enabled = isActive, onStep = onManualStep, onFlush = onManualFlush,
                    onRelease = onManualRelease, onCancel = onManualCancel,
                )
                ManualPositionButton(
                    testTag = "gas_dose_manual_inc", direction = 1,
                    label = stringResource(R.string.gas_dose_manual_increase), icon = "›", leading = false,
                    enabled = isActive, onStep = onManualStep, onFlush = onManualFlush,
                    onRelease = onManualRelease, onCancel = onManualCancel,
                )
            }
            Spacer(Modifier.weight(1f))
            Button(onClick = onTest, modifier = Modifier.testTag("gas_dose_test"), colors = ButtonDefaults.buttonColors(containerColor = if (values.testing) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)) {
                Text(stringResource(if (values.testing) R.string.choke_test_active else R.string.test))
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
        ParameterSection {
        NumberRow(GasDoseNumber.STOICHIOMETRIC_RATIO, values, onEdit)
        NumberRow(GasDoseNumber.FUEL_CUT_CLOSING, values, onEdit)
        NumberRow(GasDoseNumber.CORRECTION_POSITIVE, values, onEdit)
        NumberRow(GasDoseNumber.CORRECTION_NEGATIVE, values, onEdit)
        val frequencyDialogOpen = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
        Box {
            ParameterSelectionRow(stringResource(R.string.choke_frequency_of_pulses), frequencies.getOrNull(values.frequency) ?: "Code ${values.frequency}", "gas_dose_frequency") { frequencyDialogOpen.value = true }
        }
        if (frequencyDialogOpen.value) ParameterChoiceDialog(
            title = stringResource(R.string.choke_frequency_of_pulses),
            choices = frequencies.mapIndexed { i, option -> ParameterChoice(i, option, "gas_dose_frequency_option_$i") },
            onDismiss = { frequencyDialogOpen.value = false },
            onSelect = onFrequency,
        )
        ParameterSwitchRow(stringResource(R.string.maximum_step_frequency_at_init), values.maxFrequencyAtInit, "gas_dose_max_frequency") { onMaximumFrequency(it) }
        }
        }
}

@Composable private fun NumberRow(n: GasDoseNumber, values: GasDoseValues, onEdit: (GasDoseNumber) -> Unit) {
    ParameterValueRow(stringResource(n.title), if (n.units == R.string.units_str) "" else stringResource(n.units), n.value(values), n.precision, "gas_dose_${n.name.lowercase()}") { onEdit(n) }
}
