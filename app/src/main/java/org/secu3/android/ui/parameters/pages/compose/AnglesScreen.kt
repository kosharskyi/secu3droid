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
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import org.secu3.android.ui.parameters.compose.ParameterLoadingIndicator
import org.secu3.android.R
import org.secu3.android.ui.parameters.compose.ParameterSwitchRow
import org.secu3.android.ui.parameters.compose.ParameterValueRow
import org.secu3.android.ui.parameters.compose.ParameterPage
import org.secu3.android.ui.parameters.compose.ParameterSection
import org.secu3.android.domain.parameters.AnglesValues

enum class AnglesNumber(@StringRes val titleRes: Int, val unitRes: Int, val precision: Int, val min: Float, val max: Float, val step: Float) {
    MAX(R.string.angles_max_angle_title, R.string.units_degree, 2, -15f, 60f, .25f),
    MIN(R.string.angles_min_angle_title, R.string.units_degree, 2, -15f, 60f, .25f),
    DECREASE(R.string.angles_angle_decrement_step_title, R.string.units_degree_per_stroke, 3, 0f, 10f, .025f),
    INCREASE(R.string.angles_angle_increment_step_title, R.string.units_degree_per_stroke, 3, 0f, 10f, .025f),
    OCTANE(R.string.angles_octane_correction_title, R.string.units_degree, 2, -30f, 30f, .25f),
    SHIFT(R.string.angles_ign_timing_when_shifting_title, R.string.units_degree, 1, -15f, 55f, .1f);

    fun value(values: AnglesValues) = when (this) {
        MAX -> values.maxAngle
        MIN -> values.minAngle
        DECREASE -> values.angleDecSpeed
        INCREASE -> values.angleIncSpeed
        OCTANE -> values.angleCorrection
        SHIFT -> values.shiftIgnitionTiming
    }
}

enum class AnglesFlag(@StringRes val titleRes: Int) {
    ZERO(R.string.angles_zero_angle_title),
    WORKING_MAP(R.string.angles_always_use_ing_timing_map_title),
    MANUAL_CORRECTION(R.string.angles_apply_manual_timing_corr_title),
    ZERO_WITH_OCTANE(R.string.angles_zero_adv_correction_title);

    fun checked(values: AnglesValues) = when (this) {
        ZERO -> values.zeroAdvAngle > 0
        WORKING_MAP -> values.alwaysUseIgnitionMap
        MANUAL_CORRECTION -> values.applyManualTimingCorrOnIdl
        ZERO_WITH_OCTANE -> values.zeroAdvAngleWithCorr
    }
}

@Composable
fun AnglesScreen(values: AnglesValues?, onEdit: (AnglesNumber) -> Unit, onFlagChanged: (AnglesFlag, Boolean) -> Unit) {
    val pageScrollState = rememberScrollState()
    if (values == null) {
        ParameterLoadingIndicator("angles_loading")
        return
    }

    ParameterPage(scrollState = pageScrollState) {
        ParameterSection {
            AnglesNumber.entries.filter { it != AnglesNumber.SHIFT }.forEach { number ->
                ParameterValueRow(
                    stringResource(number.titleRes), stringResource(number.unitRes), number.value(values), number.precision,
                    "angles_${number.name.lowercase()}", enabled = number != AnglesNumber.OCTANE || values.zeroAdvAngle == 0,
                    onClick = { onEdit(number) },
                )
                if (number == AnglesNumber.OCTANE) {
                    val flag = AnglesFlag.ZERO
                    ParameterSwitchRow(stringResource(flag.titleRes), flag.checked(values), "angles_flag_${flag.name.lowercase()}") {
                        onFlagChanged(flag, it)
                    }
                }
            }
            AnglesNumber.SHIFT.let { number ->
                ParameterValueRow(stringResource(number.titleRes), stringResource(number.unitRes), number.value(values), number.precision,
                    "angles_${number.name.lowercase()}", onClick = { onEdit(number) })
            }
            AnglesFlag.entries.filter { it != AnglesFlag.ZERO }.forEach { flag ->
                ParameterSwitchRow(stringResource(flag.titleRes), flag.checked(values), "angles_flag_${flag.name.lowercase()}") {
                    onFlagChanged(flag, it)
                }
            }
        }
    }
}
