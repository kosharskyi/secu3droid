/*
 *    SecuDroid  - An open source, free manager for SECU-3 engine control unit
 *    Copyright (C) 2024 Vitalii O. Kosharskyi. Ukraine, Kyiv
 *
 *    SECU-3  - An open source, free engine control unit
 *    Copyright (C) 2007-2024 Alexey A. Shabelnikov. Ukraine, Kyiv
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

package org.secu3.android.ui.sensors.models

import org.secu3.android.R
import org.secu3.android.models.sensors.IndicatorType

val IndicatorType.title: Int
    get() = when (this) {
        IndicatorType.GAS_VALVE -> R.string.sensors_frag_status_gas_valve_label
        IndicatorType.THROTTLE -> R.string.sensors_frag_status_throttle_label
        IndicatorType.FI_FUEL -> R.string.sensors_frag_status_fl_fuel_label
        IndicatorType.POWER_VALVE -> R.string.sensors_frag_status_power_valve_label
        IndicatorType.STARTER_BLOCKING -> R.string.sensors_frag_status_starter_blocking_label
        IndicatorType.AE -> R.string.sensors_frag_status_ae_label
        IndicatorType.COOLING_FAN -> R.string.sensors_frag_status_cooling_fan_label
        IndicatorType.CHECK_ENGINE -> R.string.sensors_frag_status_check_engine_label
        IndicatorType.REV_LIM_FUEL_CUT -> R.string.sensors_frag_status_rev_lim_fuel_cut_label
        IndicatorType.FLOOD_CLEAR_MODE -> R.string.sensors_frag_status_flood_clear_mode_label
        IndicatorType.SYSTEM_LOCKED -> R.string.sensors_frag_status_system_locked_label
        IndicatorType.IGN_I_INPUT -> R.string.sensors_frag_status_input_ign_label
        IndicatorType.COND_I_INPUT -> R.string.sensors_frag_status_input_cond_label
        IndicatorType.EPAS_I_INPUT -> R.string.sensors_frag_status_input_epas_label
        IndicatorType.AFTERSTART_ENR -> R.string.sensors_frag_status_afterstart_enr_label
        IndicatorType.IAC_CLOSED_LOOP -> R.string.sensors_frag_status_iac_closed_loop_label
        IndicatorType.UNIV_OUT1 -> R.string.sensors_frag_status_univ_out_1_label
        IndicatorType.UNIV_OUT2 -> R.string.sensors_frag_status_univ_out_2_label
        IndicatorType.UNIV_OUT3 -> R.string.sensors_frag_status_univ_out_3_label
        IndicatorType.UNIV_OUT4 -> R.string.sensors_frag_status_univ_out_4_label
        IndicatorType.UNIV_OUT5 -> R.string.sensors_frag_status_univ_out_5_label
        IndicatorType.UNIV_OUT6 -> R.string.sensors_frag_status_univ_out_6_label
        IndicatorType.GPA4_I -> R.string.sensors_frag_status_gpa4_input_label
        IndicatorType.INPUT1 -> R.string.sensors_frag_status_input1_label
        IndicatorType.INPUT2 -> R.string.sensors_frag_status_input2_label
        IndicatorType.AUTO_I -> R.string.sensors_frag_status_auto_i_label
        IndicatorType.MAPSEL0 -> R.string.sensors_frag_status_mapsel0_label
        IndicatorType.REFPRS_I -> R.string.sensors_frag_status_refprs_i_label
        IndicatorType.ALTRN_I -> R.string.sensors_frag_status_altrn_i_label
    }
