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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import org.secu3.android.R
import org.secu3.android.models.packets.out.params.DbwParamPacket
import org.secu3.android.ui.parameters.compose.ParameterLoadingIndicator
import org.secu3.android.ui.parameters.compose.ParameterReadOnlyValueRow
import org.secu3.android.ui.parameters.compose.ParameterPage
import org.secu3.android.ui.parameters.compose.ParameterSection
import org.secu3.android.ui.parameters.compose.ParameterValueRow

enum class DbwNumber(
    @StringRes val titleRes: Int,
    @StringRes val unitsRes: Int,
    val precision: Int,
    val min: Float,
    val max: Float,
    val step: Float,
) {
    ETC_P(R.string.el_throttle_pid_p_title, R.string.units_empty, 5, 0f, 5f, .00025f),
    ETC_I(R.string.el_throttle_pid_i_title, R.string.units_empty, 5, 0f, 5f, .00025f),
    ETC_D(R.string.el_throttle_pid_d_title, R.string.units_empty, 5, 0f, 5f, .00025f),
    NMAX_DUTY(R.string.el_throttle_negative_max_title, R.string.units_percents, 1, 0f, 100f, .5f),
    PMAX_DUTY(R.string.el_throttle_positive_max_title, R.string.units_percents, 1, 0f, 100f, .5f),
    PID_PERIOD(R.string.el_throttle_period_pid_calling_title, R.string.units_sec, 2, 0f, .2f, .01f),
    FRICTION_TORQUE_OPEN(R.string.el_throttle_friction_torque_open_title, R.string.units_percents, 1, 0f, 15.9f, .1f),
    FRICTION_TORQUE_CLOSE(R.string.el_throttle_friction_torque_close_title, R.string.units_percents, 1, 0f, 15.9f, .1f),
    FRICTION_THRD(R.string.el_throttle_friqtion_torque_thrd_title, R.string.units_percent_per_second, 0, 0f, 25f, 1f),
    FRICTORQ_IDLE_ADD_MAX(R.string.el_throttle_max_addition_from_idl_reg_title, R.string.units_percents, 2, 0f, 15f, .1f),
}

data class DbwValues(val numbers: Map<DbwNumber, Float>, val homePosition: Float) {
    fun value(number: DbwNumber) = numbers.getValue(number)
    fun withValue(number: DbwNumber, value: Float) = copy(numbers = numbers + (number to value))

    companion object {
        fun from(packet: DbwParamPacket) = DbwValues(
            mapOf(
                DbwNumber.ETC_P to packet.etc_p,
                DbwNumber.ETC_I to packet.etc_i,
                DbwNumber.ETC_D to packet.etc_d,
                DbwNumber.NMAX_DUTY to packet.etc_nmax_duty,
                DbwNumber.PMAX_DUTY to packet.etc_pmax_duty,
                DbwNumber.PID_PERIOD to packet.pid_period,
                DbwNumber.FRICTION_TORQUE_OPEN to packet.frictorq_open,
                DbwNumber.FRICTION_TORQUE_CLOSE to packet.frictorq_close,
                DbwNumber.FRICTION_THRD to packet.frictorq_thrd,
                DbwNumber.FRICTORQ_IDLE_ADD_MAX to packet.frictorq_idleadd_max,
            ), packet.homePosition_edited,
        )
    }
}

fun applyDbwValue(packet: DbwParamPacket, number: DbwNumber, value: Float) {
    when (number) {
        DbwNumber.ETC_P -> packet.etc_p = value
        DbwNumber.ETC_I -> packet.etc_i = value
        DbwNumber.ETC_D -> packet.etc_d = value
        DbwNumber.NMAX_DUTY -> packet.etc_nmax_duty = value
        DbwNumber.PMAX_DUTY -> packet.etc_pmax_duty = value
        DbwNumber.PID_PERIOD -> packet.pid_period = value
        DbwNumber.FRICTION_TORQUE_OPEN -> packet.frictorq_open = value
        DbwNumber.FRICTION_TORQUE_CLOSE -> packet.frictorq_close = value
        DbwNumber.FRICTION_THRD -> packet.frictorq_thrd = value
        DbwNumber.FRICTORQ_IDLE_ADD_MAX -> packet.frictorq_idleadd_max = value
    }
}

sealed interface DbwUiState {
    data object Loading : DbwUiState
    data class Data(val values: DbwValues) : DbwUiState
}

@Composable
fun DbwScreen(state: DbwUiState, onEdit: (DbwNumber) -> Unit, onRefresh: (() -> Unit)? = null) {
    val pageScrollState = rememberScrollState()
    when (state) {
        DbwUiState.Loading -> ParameterLoadingIndicator("dbw_loading")
        is DbwUiState.Data -> ParameterPage(Modifier.testTag("dbw_content"), scrollState = pageScrollState) {
            ParameterSection {
                DbwNumber.entries.forEach { number ->
                    ParameterValueRow(
                        stringResource(number.titleRes), stringResource(number.unitsRes), state.values.value(number), number.precision,
                        "dbw_${number.name.lowercase()}", { onEdit(number) },
                    )
                }
                ParameterReadOnlyValueRow(
                    stringResource(R.string.el_throttle_home_position_title), stringResource(R.string.units_percents),
                    state.values.homePosition, 2, "dbw_home_position", stringResource(R.string.sec_read_only),
                )
                if (onRefresh != null) TextButton(onClick = onRefresh, modifier = Modifier.testTag("dbw_refresh")) {
                    Text(stringResource(R.string.refresh))
                }
            }
        }
    }
}
