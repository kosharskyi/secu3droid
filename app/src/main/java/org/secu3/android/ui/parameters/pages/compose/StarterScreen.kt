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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.secu3.android.R
import org.secu3.android.models.packets.out.params.StarterParamPacket
import org.secu3.android.ui.parameters.compose.ParameterSwitchRow
import org.secu3.android.ui.parameters.compose.ParameterValueRow

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
    STARTER_OFF(R.string.starter_off_title, StarterUnit.RPM, 0, 0f, 6000f, 10f),
    SMAP_ABANDON(R.string.starter_map_abandon_title, StarterUnit.RPM, 0, 0f, 6000f, 10f),
    CRANK_TO_RUN_COLD(R.string.starter_dat_crank_to_run_time_cold_title, StarterUnit.SECONDS, 2, .1f, 99f, .1f),
    CRANK_TO_RUN_HOT(R.string.starter_dat_crank_to_run_time_hot_title, StarterUnit.SECONDS, 2, .1f, 99f, .1f),
    AFTERSTART_PETROL(R.string.starter_dat_afterstart_enrichment_strokes_petrol_title, StarterUnit.NUM_LITERAL, 0, 1f, 510f, 2f),
    AFTERSTART_GAS(R.string.starter_dat_afterstart_enrichment_strokes_gas_title, StarterUnit.NUM_LITERAL, 0, 1f, 510f, 2f),
    PRIME_COLD(R.string.starter_dat_prime_pulse_cold_title, StarterUnit.MILLISECONDS, 1, 0f, 100f, .1f),
    PRIME_HOT(R.string.starter_dat_prime_pulse_hot_title, StarterUnit.MILLISECONDS, 1, 0f, 100f, .1f),
    PRIME_DELAY(R.string.starter_dat_prime_pulse_delay_title, StarterUnit.SECONDS, 1, 0f, 25f, .1f),
    FLOOD_CLEAR_THRESHOLD(R.string.starter_dat_flod_clear_mode_threshold_title, StarterUnit.PERCENT, 1, 0f, 100f, .5f),
    STBL_COUNTDOWN(R.string.starter_dat_starters_blocking_timer_title, StarterUnit.STR_LITERAL, 0, 0f, 255f, 1f),
}

enum class StarterFlag(@StringRes val titleRes: Int) {
    ALLOW_FLOOD_CLEAR_START(R.string.starter_dat_allow_start_flood_clear_mode_title),
    LIMIT_CRANKING_PW(R.string.starter_dat_limit_max_inj_pw_on_cranking_title),
}

data class StarterValues(
    val numbers: Map<StarterNumber, Float>,
    val flags: Map<StarterFlag, Boolean>,
) {
    fun value(number: StarterNumber): Float = numbers.getValue(number)
    fun withValue(number: StarterNumber, value: Float) = copy(numbers = numbers + (number to value))
    fun withFlag(flag: StarterFlag, checked: Boolean) = copy(flags = flags + (flag to checked))

    companion object {
        fun from(packet: StarterParamPacket) = StarterValues(
            numbers = mapOf(
                StarterNumber.STARTER_OFF to packet.starterOff.toFloat(),
                StarterNumber.SMAP_ABANDON to packet.smapAbandon.toFloat(),
                StarterNumber.CRANK_TO_RUN_COLD to packet.crankToRunTime,
                StarterNumber.CRANK_TO_RUN_HOT to packet.injCrankToRun_time1,
                StarterNumber.AFTERSTART_PETROL to packet.injAftstrStroke.toFloat(),
                StarterNumber.AFTERSTART_GAS to packet.injAftStrokes1.toFloat(),
                StarterNumber.PRIME_COLD to packet.injPrimeCold,
                StarterNumber.PRIME_HOT to packet.injPrimeHot,
                StarterNumber.PRIME_DELAY to packet.injPrimeDelay,
                StarterNumber.FLOOD_CLEAR_THRESHOLD to packet.injFloodclearTps,
                StarterNumber.STBL_COUNTDOWN to packet.stblStrCnt.toFloat(),
            ),
            flags = mapOf(
                StarterFlag.ALLOW_FLOOD_CLEAR_START to packet.allowStartOnClearFlood,
                StarterFlag.LIMIT_CRANKING_PW to packet.limitMaxInjPwOnCranking,
            ),
        )
    }
}

fun applyStarterValue(packet: StarterParamPacket, number: StarterNumber, value: Float) {
    when (number) {
        StarterNumber.STARTER_OFF -> packet.starterOff = value.toInt()
        StarterNumber.SMAP_ABANDON -> packet.smapAbandon = value.toInt()
        StarterNumber.CRANK_TO_RUN_COLD -> packet.crankToRunTime = value
        StarterNumber.CRANK_TO_RUN_HOT -> packet.injCrankToRun_time1 = value
        StarterNumber.AFTERSTART_PETROL -> packet.injAftstrStroke = value.toInt()
        StarterNumber.AFTERSTART_GAS -> packet.injAftStrokes1 = value.toInt()
        StarterNumber.PRIME_COLD -> packet.injPrimeCold = value
        StarterNumber.PRIME_HOT -> packet.injPrimeHot = value
        StarterNumber.PRIME_DELAY -> packet.injPrimeDelay = value
        StarterNumber.FLOOD_CLEAR_THRESHOLD -> packet.injFloodclearTps = value
        StarterNumber.STBL_COUNTDOWN -> packet.stblStrCnt = value.toInt()
    }
}

sealed interface StarterUiState {
    data object Loading : StarterUiState
    data class Data(val values: StarterValues) : StarterUiState
}

@Composable
fun StarterScreen(
    state: StarterUiState,
    onEdit: (StarterNumber) -> Unit,
    onFlagChanged: (StarterFlag, Boolean) -> Unit,
) {
    when (state) {
        StarterUiState.Loading -> Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(Modifier.testTag("starter_loading"))
        }
        is StarterUiState.Data -> Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            StarterNumber.entries.forEach { number ->
                ParameterValueRow(
                    title = stringResource(number.titleRes),
                    units = number.unit.text ?: stringResource(number.unit.resourceId!!),
                    value = state.values.value(number),
                    precision = number.precision,
                    testTag = "starter_${number.name.lowercase()}",
                    onClick = { onEdit(number) },
                )
            }
            StarterFlag.entries.forEach { flag ->
                ParameterSwitchRow(
                    title = stringResource(flag.titleRes),
                    checked = state.values.flags.getValue(flag),
                    testTag = "starter_flag_row_${flag.name.lowercase()}",
                    onCheckedChange = { onFlagChanged(flag, it) },
                )
            }
        }
    }
}
