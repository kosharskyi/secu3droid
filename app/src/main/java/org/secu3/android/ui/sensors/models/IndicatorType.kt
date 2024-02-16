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

enum class IndicatorType (val title: Int){

    GAS_VALVE(R.string.sensors_frag_status_gas_valve_label),
    THROTTLE(R.string.sensors_frag_status_throttle_label),
    FI_FUEL(R.string.sensors_frag_status_fl_fuel_label),
    POWER_VALVE(R.string.sensors_frag_status_power_valve_label),
    STARTER_BLOCKING(R.string.sensors_frag_status_starter_blocking_label),
    AE(R.string.sensors_frag_status_ae_label),
    COOLING_FAN(R.string.sensors_frag_status_cooling_fan_label),
    CHECK_ENGINE(R.string.sensors_frag_status_check_engine_label),
    REV_LIM_FUEL_CUT(R.string.sensors_frag_status_rev_lim_fuel_cut_label),
    FLOOD_CLEAR_MODE(R.string.sensors_frag_status_flood_clear_mode_label),
    SYSTEM_LOCKED(R.string.sensors_frag_status_system_locked_label),
    IGN_I_INPUT(R.string.sensors_frag_status_input_ign_label),
    COND_I_INPUT(R.string.sensors_frag_status_input_cond_label),
    EPAS_I_INPUT(R.string.sensors_frag_status_input_epas_label),
    AFTERSTART_ENR(R.string.sensors_frag_status_afterstart_enr_label),
    IAC_CLOSED_LOOP(R.string.sensors_frag_status_iac_closed_loop_label),

    UNIV_OUT1(R.string.sensors_frag_status_univ_out_1_label),
    UNIV_OUT2(R.string.sensors_frag_status_univ_out_2_label),
    UNIV_OUT3(R.string.sensors_frag_status_univ_out_3_label),
    UNIV_OUT4(R.string.sensors_frag_status_univ_out_4_label),
    UNIV_OUT5(R.string.sensors_frag_status_univ_out_5_label),
    UNIV_OUT6(R.string.sensors_frag_status_univ_out_6_label)

}