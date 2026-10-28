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
import org.secu3.android.domain.parameters.StarterControl
import org.secu3.android.domain.parameters.StarterValues

import androidx.compose.foundation.rememberScrollState
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.secu3.android.ui.parameters.compose.ParameterLoadingIndicator
import org.secu3.android.ui.parameters.*
import org.secu3.android.R
import org.secu3.android.ui.parameters.compose.ParameterPage

enum class StarterUnit(@StringRes val resourceId: Int? = null, val text: String? = null) {
    RPM(R.string.units_rpm),
    SECONDS(R.string.units_sec),
    MILLISECONDS(R.string.units_ms),
    PERCENT(R.string.units_percents),
    NUM_LITERAL(text = "num"),
    STR_LITERAL(text = "str"),
}

enum class StarterNumber(
    @StringRes val titleRes: Int,
    val unit: StarterUnit,
    val precision: Int,
    val min: Float,
    val max: Float,
    val step: Float,
) {
    STARTER_OFF(R.string.starter_off_title, StarterUnit.RPM, 0, StarterControl.STARTER_OFF_MIN.toFloat(), StarterControl.STARTER_OFF_MAX.toFloat(), 10f),
    SMAP_ABANDON(R.string.starter_map_abandon_title, StarterUnit.RPM, 0, StarterControl.SMAP_ABANDON_MIN.toFloat(), StarterControl.SMAP_ABANDON_MAX.toFloat(), 10f),
    CRANK_TO_RUN_COLD(R.string.starter_dat_crank_to_run_time_cold_title, StarterUnit.SECONDS, 2, StarterControl.CRANK_TIME_MIN, StarterControl.CRANK_TIME_MAX, .01f),
    CRANK_TO_RUN_HOT(R.string.starter_dat_crank_to_run_time_hot_title, StarterUnit.SECONDS, 2, StarterControl.CRANK_TIME_MIN, StarterControl.CRANK_TIME_MAX, .01f),
    AFTERSTART_PETROL(R.string.starter_dat_afterstart_enrichment_strokes_petrol_title, StarterUnit.NUM_LITERAL, 0, StarterControl.AFTERSTART_MIN.toFloat(), StarterControl.AFTERSTART_MAX.toFloat(), 4f),
    AFTERSTART_GAS(R.string.starter_dat_afterstart_enrichment_strokes_gas_title, StarterUnit.NUM_LITERAL, 0, StarterControl.AFTERSTART_MIN.toFloat(), StarterControl.AFTERSTART_MAX.toFloat(), 4f),
    PRIME_COLD(R.string.starter_dat_prime_pulse_cold_title, StarterUnit.MILLISECONDS, 1, StarterControl.PRIME_MIN, StarterControl.PRIME_MAX, .1f),
    PRIME_HOT(R.string.starter_dat_prime_pulse_hot_title, StarterUnit.MILLISECONDS, 1, StarterControl.PRIME_MIN, StarterControl.PRIME_MAX, .1f),
    PRIME_DELAY(R.string.starter_dat_prime_pulse_delay_title, StarterUnit.SECONDS, 1, StarterControl.PRIME_DELAY_MIN, StarterControl.PRIME_DELAY_MAX, .1f),
    FLOOD_CLEAR_THRESHOLD(R.string.starter_dat_flod_clear_mode_threshold_title, StarterUnit.PERCENT, 1, StarterControl.FLOOD_CLEAR_MIN, StarterControl.FLOOD_CLEAR_MAX, .5f),
    STBL_COUNTDOWN(R.string.starter_dat_starters_blocking_timer_title, StarterUnit.STR_LITERAL, 0, StarterControl.STBL_MIN.toFloat(), StarterControl.STBL_MAX.toFloat(), 1f),

    ;

    val integer: Boolean get() = when (this) {
        STARTER_OFF, SMAP_ABANDON, AFTERSTART_PETROL, AFTERSTART_GAS, STBL_COUNTDOWN -> true
        else -> false
    }

    fun value(values: StarterValues): Float = when (this) {
        STARTER_OFF -> values.starterOff.toFloat()
        SMAP_ABANDON -> values.smapAbandon.toFloat()
        CRANK_TO_RUN_COLD -> values.crankToRunCold
        CRANK_TO_RUN_HOT -> values.crankToRunHot
        AFTERSTART_PETROL -> values.afterstartPetrol.toFloat()
        AFTERSTART_GAS -> values.afterstartGas.toFloat()
        PRIME_COLD -> values.primeCold
        PRIME_HOT -> values.primeHot
        PRIME_DELAY -> values.primeDelay
        FLOOD_CLEAR_THRESHOLD -> values.floodClearThreshold
        STBL_COUNTDOWN -> values.stblCountdown.toFloat()
    }
}

enum class StarterFlag(@StringRes val titleRes: Int) {
    ALLOW_FLOOD_CLEAR_START(R.string.starter_dat_allow_start_flood_clear_mode_title),
    LIMIT_CRANKING_PW(R.string.starter_dat_limit_max_inj_pw_on_cranking_title),
}

@Composable
fun StarterScreen(
    values: StarterValues?,
    onEdit: (StarterNumber) -> Unit,
    onFlagChanged: (StarterFlag, Boolean) -> Unit,
    capabilities: ParameterCapabilities = ParameterCapabilities.All,
) {
    val pageScrollState = rememberScrollState()
    if (values == null) {
        ParameterLoadingIndicator("starter_loading")
    } else {
        ParameterPage(scrollState = pageScrollState) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Column {
                    StarterNumber.entries.forEach { number ->
                        val enabled = when (number) {
                            StarterNumber.CRANK_TO_RUN_COLD, StarterNumber.CRANK_TO_RUN_HOT,
                            StarterNumber.PRIME_COLD, StarterNumber.PRIME_HOT, StarterNumber.PRIME_DELAY -> capabilities.fuelInjection
                            StarterNumber.AFTERSTART_PETROL, StarterNumber.AFTERSTART_GAS -> capabilities.fuelOrGasDose
                            StarterNumber.FLOOD_CLEAR_THRESHOLD -> capabilities.floodClearThreshold
                            else -> true
                        }
                        StarterNumberRow(number, number.value(values), enabled) { onEdit(number) }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                    StarterFlag.entries.forEachIndexed { index, flag ->
                        val checked = if (flag == StarterFlag.ALLOW_FLOOD_CLEAR_START) values.allowFloodClearStart else values.limitCrankingPw
                        StarterFlagRow(flag, checked, if (flag == StarterFlag.LIMIT_CRANKING_PW) capabilities.fuelInjection else capabilities.floodClearThreshold) {
                            onFlagChanged(flag, it)
                        }
                        if (index < StarterFlag.entries.lastIndex) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StarterNumberRow(number: StarterNumber, value: Float, enabled: Boolean, onClick: () -> Unit) {
    val testTag = "starter_${number.name.lowercase()}"
    val locale = LocalConfiguration.current.locales[0]
    val units = number.unit.text ?: stringResource(number.unit.resourceId!!)
    val formattedValue = buildAnnotatedString {
        append(String.format(locale, "%.${number.precision}f", value))
        withStyle(
            SpanStyle(
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal,
            ),
        ) {
            append(" $units")
        }
    }
    Row(
        Modifier.fillMaxWidth().heightIn(min = 66.dp)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick).testTag(testTag)
            .padding(horizontal = 14.dp, vertical = 11.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            stringResource(number.titleRes),
            Modifier.weight(0.62f).testTag("$testTag-label"),
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            formattedValue,
            Modifier.weight(0.38f).testTag("$testTag-value"),
            style = MaterialTheme.typography.titleLarge.copy(
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                fontFeatureSettings = "tnum",
            ),
            textAlign = TextAlign.End,
        )
    }
}

@Composable
private fun StarterFlagRow(flag: StarterFlag, checked: Boolean, enabled: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val testTag = "starter_flag_row_${flag.name.lowercase()}"
    Row(
        Modifier.fillMaxWidth().heightIn(min = 66.dp).testTag(testTag)
            .toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(horizontal = 14.dp, vertical = 11.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(stringResource(flag.titleRes), Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Switch(checked, onCheckedChange, enabled = enabled, modifier = Modifier.testTag("$testTag-switch"))
    }
}
