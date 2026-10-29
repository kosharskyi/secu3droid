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


import androidx.compose.foundation.rememberScrollState
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import org.secu3.android.R
import org.secu3.android.domain.parameters.LtftValues
import org.secu3.android.ui.parameters.compose.ParameterLoadingIndicator
import org.secu3.android.ui.parameters.compose.ParameterValueRow
import org.secu3.android.ui.parameters.compose.ParameterPage
import org.secu3.android.ui.parameters.compose.ParameterSection
import org.secu3.android.ui.parameters.compose.ParameterSelectionRow
import org.secu3.android.ui.parameters.dialogs.ParameterChoice
import org.secu3.android.ui.parameters.dialogs.ParameterChoiceDialog

enum class LtftNumber(
    @StringRes val titleRes: Int,
    @StringRes val unitRes: Int,
    val precision: Int,
    val min: Float,
    val max: Float,
    val step: Float,
    val integer: Boolean = false,
) {
    LOWER_CLT(R.string.ltft_par_lower_clt_threshold_title, R.string.units_degrees_celcius, 2, 0f, 270f, .25f),
    UPPER_CLT(R.string.ltft_par_upper_clt_threshold_title, R.string.units_degrees_celcius, 2, 0f, 270f, .25f),
    UPPER_IAT(R.string.ltft_par_upper_iat_threshold_title, R.string.units_degrees_celcius, 2, 0f, 270f, .25f),
    LEARNING_GRADIENT(R.string.ltft_par_learning__gradient_factor_title, R.string.units_empty, 3, 0f, .99f, .005f),
    ABS_GAS_PRESSURE(R.string.ltft_par_abs_gas_pressure_treshold_title, R.string.units_pressure_kpa, 1, 0f, 500f, .1f),
    DIFF_GAS_PRESSURE(R.string.ltft_par_diff_gas_pressure_treshold_title, R.string.units_pressure_kpa, 1, 0f, 500f, .1f),
    BOTTOM_LIMIT(R.string.ltft_par_bottom_limit_correction_title, R.string.units_percents, 1, -24.6f, 0f, .1f),
    TOP_LIMIT(R.string.ltft_par_top_limit_correction_title, R.string.units_percents, 1, 0f, 24.6f, .1f),
    LOWER_RPM(R.string.ltft_par_lower_rpm_threshold_title, R.string.units_rpm, 0, 0f, 25000f, 10f, true),
    UPPER_RPM(R.string.ltft_par_upper_rpm_threshold_title, R.string.units_rpm, 0, 0f, 25000f, 10f, true),
    LOWER_LOAD(R.string.ltft_par_lower_load_threshold_title, R.string.units_pressure_kpa, 2, 0f, 500f, 1f),
    UPPER_LOAD(R.string.ltft_par_upper_load_threshold_title, R.string.units_pressure_kpa, 2, 0f, 500f, 1f),
    NEGATIVE_INSENSITIVITY(R.string.ltft_par_negative_insensitivity_threshold_title, R.string.units_percents, 1, 0f, 10f, .1f),
    POSITIVE_INSENSITIVITY(R.string.ltft_par_positive_insensitivity_threshold_title, R.string.units_percents, 1, 0f, 10f, .1f);

    fun value(values: LtftValues): Float = when (this) {
        LOWER_CLT -> values.lowerClt
        UPPER_CLT -> values.upperClt
        UPPER_IAT -> values.upperIat
        LEARNING_GRADIENT -> values.learningGradient
        ABS_GAS_PRESSURE -> values.absoluteGasPressure
        DIFF_GAS_PRESSURE -> values.differentialGasPressure
        BOTTOM_LIMIT -> values.bottomLimit
        TOP_LIMIT -> values.topLimit
        LOWER_RPM -> values.lowerRpm.toFloat()
        UPPER_RPM -> values.upperRpm.toFloat()
        LOWER_LOAD -> values.lowerLoad
        UPPER_LOAD -> values.upperLoad
        NEGATIVE_INSENSITIVITY -> values.negativeInsensitivity
        POSITIVE_INSENSITIVITY -> values.positiveInsensitivity
    }
}

@Composable
fun LtftScreen(values: LtftValues?, onEdit: (LtftNumber) -> Unit, onMode: (Int) -> Unit) {
    val pageScrollState = rememberScrollState()
    if (values == null) {
        ParameterLoadingIndicator("ltft_loading")
    } else {
        val modes = stringArrayResource(R.array.ltft_mode_items)
        ParameterPage(scrollState = pageScrollState) {
            ParameterSection {
                LtftModeSelector(modes, values.mode, onMode)
                LtftNumber.entries.forEach { number ->
                    ParameterValueRow(
                        title = stringResource(number.titleRes),
                        units = stringResource(number.unitRes),
                        value = number.value(values),
                        precision = number.precision,
                        testTag = "ltft_${number.name.lowercase()}",
                        onClick = { onEdit(number) },
                    )
                }
            }
        }
    }
}

@Composable
private fun LtftModeSelector(modes: Array<String>, mode: Int, onMode: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val selected = modes.getOrNull(mode) ?: "Unknown ($mode)"
    Column {
        ParameterSelectionRow(
            title = stringResource(R.string.ltft_par_mode_title),
            value = selected,
            testTag = "ltft_mode",
            valueTestTag = "ltft_mode_value",
            onClick = { expanded = true },
        )
        if (expanded) ParameterChoiceDialog(
            title = stringResource(R.string.ltft_par_mode_title),
            choices = modes.mapIndexed { index, item -> ParameterChoice(index, item, "ltft_mode_option_$index") },
            onDismiss = { expanded = false },
            onSelect = onMode,
        )
    }
}
