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
import org.secu3.android.models.packets.input.SensorsPacket
import java.util.Locale

data class GaugeSection(val start: Float, val end: Float, val color: Int)

enum class GaugeType (val title: Int, val minValue: Float, val maxValue: Float, val units: Int, val isInteger: Boolean, val tickCount: Int, val sections: List<GaugeSection> = listOf()) {

    RPM(R.string.sensors_frag_rpm_label, 0f, 8000f, R.string.units_rpm, true, 17, listOf(
        GaugeSection(0f, 0.5f, R.color.gauge_desktop_64ff64),
        GaugeSection(0.5f, 0.75f, R.color.gauge_desktop_ffff64),
        GaugeSection(0.75f, 1f, R.color.gauge_desktop_ff6464)
    )),

    MAP(R.string.sensors_frag_absolute_pressure_label, 10.0f, 110.0f, R.string.units_pressure_kpa, false, 21, listOf(
        GaugeSection(0f, 0.2f, R.color.gauge_desktop_b48282),
        GaugeSection(0.2f, 0.8f, R.color.gauge_desktop_78c878),
        GaugeSection(0.8f, 1f, R.color.gauge_desktop_b48282)
    )),

    MAP_TURBO(R.string.sensors_frag_absolute_pressure_turbo_label, 0f, 250f, R.string.units_pressure_kpa, false, 11, listOf(
        GaugeSection(0f, .25f, R.color.gauge_gray),
        GaugeSection(.25f, .75f, R.color.gauge_green),
        GaugeSection(.75f, 1f, R.color.gauge_gray),
    )),

    VOLTAGE(R.string.sensors_frag_voltage_label, 2.0f, 18.0f, R.string.units_volts, false, 17, listOf(
        GaugeSection(0f, 0.5f, R.color.gauge_desktop_ff8080),
        GaugeSection(0.5f, 0.8125f, R.color.gauge_desktop_80ff80),
        GaugeSection(0.8125f, 1f, R.color.gauge_desktop_ff0000)
    )),

    CURRENT_ANGLE(R.string.sensors_frag_advance_angle_label, -15.0f, 65.0f, R.string.units_degree, false, 17, listOf(
        GaugeSection(0f, 0.1875f, R.color.gauge_desktop_b48282),
        GaugeSection(0.1875f, 0.75f, R.color.gauge_desktop_dcdc78),
        GaugeSection(0.75f, 1f, R.color.gauge_desktop_ff6464)
    )),

    TEMPERATURE(R.string.sensors_frag_coolant_temperature_label, -40.0f, 120.0f, R.string.units_degrees_celcius, false, 17, listOf(
        GaugeSection(0f, 0.5625f, R.color.gauge_desktop_8282b4),
        GaugeSection(0.5625f, 0.875f, R.color.gauge_desktop_787878),
        GaugeSection(0.875f, 1f, R.color.gauge_desktop_e68282)
    )),

    ADD1(R.string.sensors_frag_input_1_label, 0f, 5.0f, R.string.units_volts, false, 21, listOf(
        GaugeSection(0f, 1f, R.color.gauge_desktop_b4b4e6)
    )),

    ADD2(R.string.sensors_frag_input_2_label, 0f, 5.0f, R.string.units_volts, false, 21, listOf(
        GaugeSection(0f, 1f, R.color.gauge_desktop_b4b4e6)
    )),

    INJ_PW(R.string.sensors_frag_injection_pw_label, 0f, 24.0f, R.string.units_ms, false, 25, listOf(
        GaugeSection(0f, 0.041666667f, R.color.gauge_desktop_ff6464),
        GaugeSection(0.041666667f, 0.41666667f, R.color.gauge_desktop_64ff64),
        GaugeSection(0.41666667f, 1f, R.color.gauge_desktop_9696fa)
    )),

    IAT(R.string.sensors_frag_intake_air_temp_label, -40f, 120.0f, R.string.units_degrees_celcius, false, 17, listOf(
        GaugeSection(0f, 0.5625f, R.color.gauge_desktop_8282b4),
        GaugeSection(0.5625f, 0.875f, R.color.gauge_desktop_787878),
        GaugeSection(0.875f, 1f, R.color.gauge_desktop_e68282)
    )),

    EGO_CORR(R.string.sensors_frag_ego_correction_label, -50f, 50.0f, R.string.units_percents, false, 17, listOf(
        GaugeSection(0f, 0.3f, R.color.gauge_desktop_e68282),
        GaugeSection(0.3f, 0.7f, R.color.gauge_desktop_78be78),
        GaugeSection(0.7f, 1f, R.color.gauge_desktop_e68282)
    )),

    CHOKE_POSITION(R.string.sensors_frag_iac_valve_label, 0f, 100.0f, R.string.units_percents, false, 21, listOf(
        GaugeSection(0f, 1f, R.color.gauge_desktop_00e6fa)
    )),

    AIR_FLOW(R.string.sensors_frag_air_flow_label, 0f, 16.0f, R.string.units_curve_n, true, 17, listOf(
        GaugeSection(0f, 0.0625f, R.color.gauge_desktop_be9696),
        GaugeSection(0.0625f, 0.125f, R.color.gauge_desktop_b496a0),
        GaugeSection(0.125f, 0.1875f, R.color.gauge_desktop_aa96aa),
        GaugeSection(0.1875f, 0.25f, R.color.gauge_desktop_a096b4),
        GaugeSection(0.25f, 0.3125f, R.color.gauge_desktop_9696be),
        GaugeSection(0.3125f, 0.375f, R.color.gauge_desktop_9696c8),
        GaugeSection(0.375f, 0.4375f, R.color.gauge_desktop_9696d2),
        GaugeSection(0.4375f, 0.5f, R.color.gauge_desktop_9696dc),
        GaugeSection(0.5f, 0.5625f, R.color.gauge_desktop_9696e6),
        GaugeSection(0.5625f, 0.625f, R.color.gauge_desktop_9696f0),
        GaugeSection(0.625f, 0.6875f, R.color.gauge_desktop_9696fa),
        GaugeSection(0.6875f, 0.75f, R.color.gauge_desktop_9696ff),
        GaugeSection(0.75f, 0.8125f, R.color.gauge_desktop_9696ff),
        GaugeSection(0.8125f, 0.875f, R.color.gauge_desktop_9696ff),
        GaugeSection(0.875f, 0.9375f, R.color.gauge_desktop_9696ff),
        GaugeSection(0.9375f, 1f, R.color.gauge_desktop_9696ff)
    )),

    VEHICLE_SPEED(R.string.sensors_frag_vehicle_speed_label, 0f, 220f, R.string.units_km_h, false, 23, listOf(
        GaugeSection(0f, 0.27272727f, R.color.gauge_desktop_64ff64),
        GaugeSection(0.27272727f, 0.54545455f, R.color.gauge_desktop_fae664),
        GaugeSection(0.54545455f, 0.81818182f, R.color.gauge_desktop_e68264),
        GaugeSection(0.81818182f, 1f, R.color.gauge_desktop_ff7878)
    )),

    TPS_DOT(R.string.sensors_frag_tps_dot_label, -500.0f, 500f, R.string.units_percent_per_second, true, 11, listOf(
        GaugeSection(0f, 0.3f, R.color.gauge_desktop_fa6464),
        GaugeSection(0.3f, 0.45f, R.color.gauge_desktop_64fa64),
        GaugeSection(0.45f, 0.5f, R.color.gauge_desktop_6464fa),
        GaugeSection(0.5f, 0.55f, R.color.gauge_desktop_6464fa),
        GaugeSection(0.55f, 0.7f, R.color.gauge_desktop_64fa64),
        GaugeSection(0.7f, 1f, R.color.gauge_desktop_fa6464)
    )),

    MAP2(R.string.sensors_frag_map2_label, 0.0f, 400f, R.string.units_pressure_kpa, false, 21, listOf(
        GaugeSection(0f, 0.25f, R.color.gauge_desktop_b48282),
        GaugeSection(0.25f, 1f, R.color.gauge_desktop_78c878)
    )),

    DIFF_PRESSURE(R.string.sensors_frag_diff_pressure_label, 0.0f, 400f, R.string.units_pressure_kpa, false, 21, listOf(
        GaugeSection(0f, 0.25f, R.color.gauge_desktop_b48282),
        GaugeSection(0.25f, 1f, R.color.gauge_desktop_78c878)
    )),

    IAT2(R.string.sensors_frag_iat2_label, -40.0f, 120f, R.string.units_degrees_celcius, false, 17, listOf(
        GaugeSection(0f, 0.5625f, R.color.gauge_desktop_8282b4),
        GaugeSection(0.5625f, 0.875f, R.color.gauge_desktop_787878),
        GaugeSection(0.875f, 1f, R.color.gauge_desktop_e68282)
    )),

    FUEL_CONSUMPTION(R.string.sensors_frag_fuel_consumption_label, 0.0f, 50f, R.string.units_l_per_100km, false, 25, listOf(
        GaugeSection(0f, 0.2f, R.color.gauge_desktop_64dc64),
        GaugeSection(0.2f, 0.4f, R.color.gauge_desktop_b4b464),
        GaugeSection(0.4f, 1f, R.color.gauge_desktop_dc6464)
    )),

    KNOCK_RETARD(R.string.sensors_frag_knock_retard_label, 0.0f, 30f, R.string.units_degree, false, 15, listOf(
        GaugeSection(0f, 0.33333333f, R.color.gauge_desktop_dcdc78),
        GaugeSection(0.33333333f, 1f, R.color.gauge_desktop_dc7878)
    )),

    KNOCK_SIGNAL(R.string.sensors_frag_knock_signal_label, 0.0f, 5.0f, R.string.units_volts, false, 21, listOf(
        GaugeSection(0f, 0.1f, R.color.gauge_desktop_2850dc),
        GaugeSection(0.1f, 0.2f, R.color.gauge_desktop_3c64b4),
        GaugeSection(0.2f, 0.3f, R.color.gauge_desktop_5078a0),
        GaugeSection(0.3f, 0.4f, R.color.gauge_desktop_648c8c),
        GaugeSection(0.4f, 0.5f, R.color.gauge_desktop_78a078),
        GaugeSection(0.5f, 0.6f, R.color.gauge_desktop_8cb464),
        GaugeSection(0.6f, 0.7f, R.color.gauge_desktop_a0a064),
        GaugeSection(0.7f, 0.8f, R.color.gauge_desktop_b48c64),
        GaugeSection(0.8f, 0.9f, R.color.gauge_desktop_c87864),
        GaugeSection(0.9f, 1f, R.color.gauge_desktop_e66464)
    )),

    WBO_AFR(R.string.sensors_frag_wbo_afr_label, 6.0f, 24.0f, R.string.units_afr, false, 19, listOf(
        GaugeSection(0f, 0.22222222f, R.color.gauge_desktop_ff8282),
        GaugeSection(0.22222222f, 0.475f, R.color.gauge_desktop_f0f082),
        GaugeSection(0.475f, 0.49166667f, R.color.gauge_desktop_82ff82),
        GaugeSection(0.49166667f, 0.525f, R.color.gauge_desktop_f0f078),
        GaugeSection(0.525f, 0.54166667f, R.color.gauge_desktop_82ff82),
        GaugeSection(0.54166667f, 0.77777778f, R.color.gauge_desktop_f0f082),
        GaugeSection(0.77777778f, 1f, R.color.gauge_desktop_ff8282)
    )),

    IAC_VALVE(R.string.sensors_frag_throttle_gate_label, 0.0f, 100.0f, R.string.units_percents, false, 17, listOf(
        GaugeSection(0f, 0.05f, R.color.gauge_desktop_8282b4),
        GaugeSection(0.05f, 0.75f, R.color.gauge_desktop_78b496),
        GaugeSection(0.75f, 1f, R.color.gauge_desktop_e68282)
    )),

    GAS_DISPENSER(R.string.sensors_frag_gas_dispenser_label, 0.0f, 100.0f, R.string.units_percents, false, 21, listOf(
        GaugeSection(0f, 1f, R.color.gauge_desktop_00e6fa)
    )),

    SYNTHETIC_LOAD(R.string.sensors_frag_synthetic_load_label, 0.0f, 250.0f, R.string.units_empty, false, 21, listOf(
        GaugeSection(0f, 0.4f, R.color.gauge_desktop_82fa82),
        GaugeSection(0.4f, 1f, R.color.gauge_desktop_fa8282)
    )),

    BEGIN_INJ_PHASE(R.string.sensors_frag_begin_inj_phase_label, 0.0f, 720.0f, R.string.units_degree_word, false, 17, listOf(
        GaugeSection(0f, 1f, R.color.gauge_desktop_fafafa)
    )),

    END_INJ_PHASE(R.string.sensors_frag_end_inj_phase_label, 0.0f, 720.0f, R.string.units_degree_word, false, 17, listOf(
        GaugeSection(0f, 1f, R.color.gauge_desktop_fafafa)
    )),

    FUEL_CONSUMPTION_HZ(R.string.sensors_frag_fuel_consumption_hz_label, 0.0f, 250.0f, R.string.units_hertz, false, 21, listOf(
        GaugeSection(0f, 0.04f, R.color.gauge_desktop_828296),
        GaugeSection(0.04f, 0.28f, R.color.gauge_desktop_64f064),
        GaugeSection(0.28f, 0.6f, R.color.gauge_desktop_e6e664),
        GaugeSection(0.6f, 1f, R.color.gauge_desktop_f06464)
    )),

    GRTS(R.string.sensors_frag_grts_label, -40.0f, 120.0f, R.string.units_degrees_celcius, false, 17, listOf(
        GaugeSection(0f, 0.5625f, R.color.gauge_desktop_8282b4),
        GaugeSection(0.5625f, 0.875f, R.color.gauge_desktop_787878),
        GaugeSection(0.875f, 1f, R.color.gauge_desktop_e68282)
    )),

    FUEL_LEVEL(R.string.sensors_frag_fuel_level_label, 0.0f, 100.0f, R.string.units_liter, false, 21, listOf(
        GaugeSection(0f, 0.1f, R.color.gauge_desktop_fa6464),
        GaugeSection(0.1f, 0.5f, R.color.gauge_desktop_dcdc64),
        GaugeSection(0.5f, 1f, R.color.gauge_desktop_64dc64)
    )),

    EXHAUST_GAS_TEMP(R.string.sensors_frag_exhaust_gas_temp_label, 0.0f, 1100.0f, R.string.units_degrees_celcius, false, 17, listOf(
        GaugeSection(0f, 0.36363636f, R.color.gauge_desktop_6464ff),
        GaugeSection(0.36363636f, 0.86363636f, R.color.gauge_desktop_c8c878),
        GaugeSection(0.86363636f, 1f, R.color.gauge_desktop_ff6464)
    )),

    OIL_PRESSURE(R.string.sensors_frag_oil_pressure_label, 0.0f, 6.0f, R.string.units_kg_per_cm2, false, 13, listOf(
        GaugeSection(0f, 0.16666667f, R.color.gauge_desktop_8282be),
        GaugeSection(0.16666667f, 0.58333333f, R.color.gauge_desktop_78f078),
        GaugeSection(0.58333333f, 0.83333333f, R.color.gauge_desktop_f0f078),
        GaugeSection(0.83333333f, 1f, R.color.gauge_desktop_ff8282)
    )),

    INJ_DUTY(R.string.sensors_frag_injector_duty_label, 0.0f, 100.0f, R.string.units_percents, false, 21, listOf(
        GaugeSection(0f, 0.1f, R.color.gauge_desktop_6464ff),
        GaugeSection(0.1f, 0.7f, R.color.gauge_desktop_64ff64),
        GaugeSection(0.7f, 0.9f, R.color.gauge_desktop_f0ff64),
        GaugeSection(0.9f, 1f, R.color.gauge_desktop_ff6464)
    )),

    MAF(R.string.sensors_frag_maf_label, 0.0f, 600.0f, R.string.units_gram_per_second, false, 21, listOf(
        GaugeSection(0f, 0.16666667f, R.color.gauge_desktop_8282b4),
        GaugeSection(0.16666667f, 0.5f, R.color.gauge_desktop_78c878),
        GaugeSection(0.5f, 0.83333333f, R.color.gauge_desktop_c8c878),
        GaugeSection(0.83333333f, 1f, R.color.gauge_desktop_c87878)
    )),

    FAN_DUTY(R.string.sensors_frag_fan_duty_label, 0.0f, 100.0f, R.string.units_percents, false, 21, listOf(
        GaugeSection(0f, 0.1f, R.color.gauge_desktop_6464ff),
        GaugeSection(0.1f, 0.7f, R.color.gauge_desktop_64ff64),
        GaugeSection(0.7f, 0.9f, R.color.gauge_desktop_f0ff64),
        GaugeSection(0.9f, 1f, R.color.gauge_desktop_ff6464)
    )),

    MAP_DOT(R.string.sensors_frag_map_dot_label, -500.0f, 500.0f, R.string.units_kpa_per_sec, true, 11, listOf(
        GaugeSection(0f, 0.3f, R.color.gauge_desktop_fa6464),
        GaugeSection(0.3f, 0.45f, R.color.gauge_desktop_64fa64),
        GaugeSection(0.45f, 0.5f, R.color.gauge_desktop_6464fa),
        GaugeSection(0.5f, 0.55f, R.color.gauge_desktop_6464fa),
        GaugeSection(0.55f, 0.7f, R.color.gauge_desktop_64fa64),
        GaugeSection(0.7f, 1f, R.color.gauge_desktop_fa6464)
    )),

    FUEL_TEMP(R.string.sensors_frag_fuel_temp_label, -40.0f, 120.0f, R.string.units_degrees_celcius, false, 17, listOf(
        GaugeSection(0f, 0.5625f, R.color.gauge_desktop_8282b4),
        GaugeSection(0.5625f, 0.875f, R.color.gauge_desktop_787878),
        GaugeSection(0.875f, 1f, R.color.gauge_desktop_e68282)
    )),

    EGO_CORR2(R.string.sensors_frag_ego_correction2_label, -50.0f, 50.0f, R.string.units_percents, false, 17, listOf(
        GaugeSection(0f, 0.3f, R.color.gauge_desktop_e68282),
        GaugeSection(0.3f, 0.7f, R.color.gauge_desktop_78be78),
        GaugeSection(0.7f, 1f, R.color.gauge_desktop_e68282)
    )),

    WBO_AFR2(R.string.sensors_frag_wbo_afr_2_label, 6.0f, 24.0f, R.string.units_afr, false, 19, listOf(
        GaugeSection(0f, 0.22222222f, R.color.gauge_desktop_ff8282),
        GaugeSection(0.22222222f, 0.475f, R.color.gauge_desktop_f0f082),
        GaugeSection(0.475f, 0.49166667f, R.color.gauge_desktop_82ff82),
        GaugeSection(0.49166667f, 0.525f, R.color.gauge_desktop_f0f078),
        GaugeSection(0.525f, 0.54166667f, R.color.gauge_desktop_82ff82),
        GaugeSection(0.54166667f, 0.77777778f, R.color.gauge_desktop_f0f082),
        GaugeSection(0.77777778f, 1f, R.color.gauge_desktop_ff8282)
    )),

    WBO_AFR_TABL(R.string.wbo_afr_tabl, 6.0f, 24.0f, R.string.units_afr, false, 19, listOf(
        GaugeSection(0f, 0.22222222f, R.color.gauge_desktop_ff8282),
        GaugeSection(0.22222222f, 0.475f, R.color.gauge_desktop_f0f082),
        GaugeSection(0.475f, 0.49166667f, R.color.gauge_desktop_82ff82),
        GaugeSection(0.49166667f, 0.525f, R.color.gauge_desktop_f0f078),
        GaugeSection(0.525f, 0.54166667f, R.color.gauge_desktop_82ff82),
        GaugeSection(0.54166667f, 0.77777778f, R.color.gauge_desktop_f0f082),
        GaugeSection(0.77777778f, 1f, R.color.gauge_desktop_ff8282)
    )),

    AFR_DIFF(R.string.sensors_frag_afr_difference_label, -10.0f, 10.0f, R.string.units_afr, false, 19, listOf(
        GaugeSection(0.05f, 0.4f, R.color.gauge_desktop_8282ff),
        GaugeSection(0.4f, 0.45f, R.color.gauge_desktop_f0f082),
        GaugeSection(0.45f, 0.55f, R.color.gauge_desktop_82ff82),
        GaugeSection(0.55f, 0.6f, R.color.gauge_desktop_f0f082),
        GaugeSection(0.6f, 0.95f, R.color.gauge_desktop_ff8282)
    )),

    AFR_DIFF2(R.string.sensors_frag_afr_difference2_label, -10.0f, 10.0f, R.string.units_afr, false, 19, listOf(
        GaugeSection(0.05f, 0.4f, R.color.gauge_desktop_8282ff),
        GaugeSection(0.4f, 0.45f, R.color.gauge_desktop_f0f082),
        GaugeSection(0.45f, 0.55f, R.color.gauge_desktop_82ff82),
        GaugeSection(0.55f, 0.6f, R.color.gauge_desktop_f0f082),
        GaugeSection(0.6f, 0.95f, R.color.gauge_desktop_ff8282)
    )),

    GAS_PRESSURE_SENS(R.string.sensors_frag_gas_pressure_label, 0.0f, 400f, R.string.units_pressure_kpa, false, 21, listOf(
        GaugeSection(0f, 0.25f, R.color.gauge_desktop_b48282),
        GaugeSection(0.25f, 1f, R.color.gauge_desktop_78c878)
    )),

    FUEL_PRESSURE_SENS(R.string.sensors_frag_fuel_pressure_label, 0.0f, 700f, R.string.units_pressure_kpa, false, 21, listOf(
        GaugeSection(0f, 0.42857143f, R.color.gauge_desktop_b48282),
        GaugeSection(0.42857143f, 1f, R.color.gauge_desktop_78c878)
    )),

    APPS1(R.string.sensors_frag_acceleration_pedal_sensor_label, 0.0f, 100.0f, R.string.units_percents, false, 17, listOf(
        GaugeSection(0f, 0.1f, R.color.gauge_desktop_b6b6d3),
        GaugeSection(0.1f, 0.9f, R.color.gauge_desktop_93ea79),
        GaugeSection(0.9f, 1f, R.color.gauge_desktop_fb5537)
    )),

    OTS(R.string.sensors_frag_oil_temperature_sensor_label, -50.0f, 140.0f, R.string.units_degrees_celcius, false, 17, listOf(
        GaugeSection(0.052631579f, 0.52631579f, R.color.gauge_desktop_8282b4),
        GaugeSection(0.52631579f, 0.78947368f, R.color.gauge_desktop_787878),
        GaugeSection(0.78947368f, 1f, R.color.gauge_desktop_e68282)
    )),

    EST_TORQUE(R.string.sensors_frag_estimated_torque_label, -50.0f, 200.0f, R.string.units_torque_nm, false, 21, listOf(
        GaugeSection(0f, 0.2f, R.color.gauge_desktop_8282b4),
        GaugeSection(0.2f, 0.7f, R.color.gauge_desktop_78bb78),
        GaugeSection(0.7f, 0.84f, R.color.gauge_desktop_ccce84),
        GaugeSection(0.84f, 1f, R.color.gauge_desktop_f87070)
    )),

    REQ_TORQUE(R.string.sensors_frag_requested_torque_label, -50.0f, 200.0f, R.string.units_torque_nm, false, 21, listOf(
        GaugeSection(0f, 0.2f, R.color.gauge_desktop_8282b4),
        GaugeSection(0.2f, 0.7f, R.color.gauge_desktop_78bb78),
        GaugeSection(0.7f, 0.84f, R.color.gauge_desktop_ccce84),
        GaugeSection(0.84f, 1f, R.color.gauge_desktop_f87070)
    )),

    DISTANCE(R.string.sensors_frag_distance_label, 0.0f, 1000.0f, R.string.units_km, false, 11),
    FUEL_CONSUMPTION_HOURLY(R.string.sensors_frag_fuel_consumption_hourly_label, 0.0f, 100.0f, R.string.units_l_per_hour, false, 11),
    CONSUMED_FUEL(R.string.sensors_frag_consumed_fuel_label, 0.0f, 1000.0f, R.string.units_liter, false, 11);

    val decimals: Int
        get() = when (this) {
            RPM, AIR_FLOW, TPS_DOT, MAP_DOT -> 0
            MAP, VOLTAGE, CURRENT_ANGLE, INJ_PW, EGO_CORR, FUEL_CONSUMPTION,
            WBO_AFR, WBO_AFR2, WBO_AFR_TABL, AFR_DIFF, AFR_DIFF2, MAP2,
            DIFF_PRESSURE, FUEL_CONSUMPTION_HZ, OIL_PRESSURE, MAF,
            GAS_PRESSURE_SENS, FUEL_PRESSURE_SENS, EST_TORQUE, REQ_TORQUE,
            SYNTHETIC_LOAD, FUEL_CONSUMPTION_HOURLY -> 2
            DISTANCE -> 1
            CONSUMED_FUEL -> 3
            ADD1, ADD2, KNOCK_SIGNAL -> 3
            else -> 1
        }

    val defaultNumericView: Boolean
        get() = this == DISTANCE || this == CONSUMED_FUEL

    fun formatValue(value: Float): String {
        if (!value.isFinite()) return "—"
        return String.format(Locale.US, "%.${decimals}f", value)
    }

    fun getGaugeItem(packet: SensorsPacket): Float {
        return when (this) {
            RPM -> packet.rpm.toFloat()
            MAP -> packet.map
            MAP_TURBO -> packet.map
            VOLTAGE -> packet.voltage
            CURRENT_ANGLE -> packet.currentAngle
            TEMPERATURE -> packet.temperature
            ADD1 -> packet.addI1
            ADD2 -> packet.addI2
            INJ_PW -> packet.injPw
            IAT -> packet.airtempSensor
            EGO_CORR -> packet.lambdaCorr
            CHOKE_POSITION -> packet.chokePosition
            AIR_FLOW -> packet.airflow.toFloat()
            VEHICLE_SPEED -> packet.speed
            TPS_DOT -> packet.tpsdot.toFloat()
            MAP2 ->packet.map2
            DIFF_PRESSURE -> packet.mapd
            IAT2 -> packet.tmp2
            FUEL_CONSUMPTION -> packet.inj_ffd
            KNOCK_RETARD -> packet.knockRetard
            KNOCK_SIGNAL -> packet.knockValue
            WBO_AFR -> packet.afr
            IAC_VALVE -> packet.tps
            GAS_DISPENSER -> packet.gasDosePosition
            SYNTHETIC_LOAD -> packet.load
            BEGIN_INJ_PHASE -> packet.injTimBegin
            END_INJ_PHASE -> packet.injTimEnd
            FUEL_CONSUMPTION_HZ -> packet.fuelFlowFrequency
            GRTS -> packet.grts
            FUEL_LEVEL -> packet.ftls
            EXHAUST_GAS_TEMP -> packet.egts
            OIL_PRESSURE -> packet.ops
            INJ_DUTY -> packet.sens_injDuty
            MAF -> packet.maf
            FAN_DUTY -> packet.ventDuty
            MAP_DOT -> packet.mapdot.toFloat()
            FUEL_TEMP -> packet.fts
            EGO_CORR2 -> packet.lambdaCorr2
            WBO_AFR2 -> packet.afr2
            WBO_AFR_TABL -> packet.afrMap
            AFR_DIFF -> {
                val difAfr = packet.afr - packet.afrMap
                difAfr
            }
            AFR_DIFF2 -> {
                val difAfr = packet.afr2 - packet.afrMap
                difAfr
            }

            GAS_PRESSURE_SENS -> packet.gasPressureSensor

            FUEL_PRESSURE_SENS -> packet.fuelPressureSensor
            APPS1 -> packet.apps1
            OTS -> packet.ots
            EST_TORQUE -> packet.estTorque
            REQ_TORQUE -> packet.reqTorque
            DISTANCE -> packet.distance
            FUEL_CONSUMPTION_HOURLY -> packet.inj_ffh
            CONSUMED_FUEL -> packet.cons_fuel
        }
    }
}
