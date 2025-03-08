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


import android.content.Context
import androidx.core.content.ContextCompat
import com.github.anastr.speedviewlib.components.Section
import org.secu3.android.R
import org.secu3.android.models.packets.input.SensorsPacket

enum class GaugeType (val title: Int, val minValue: Float, val maxValue: Float, val units: Int, val isInteger: Boolean, val tickCount: Int, private val sections: List<Section> = listOf()) {

    RPM(R.string.sensors_frag_rpm_label, 0f, 8000f, R.string.units_rpm, true, 9, listOf(
        Section(0f, .5f, R.color.gauge_green, -1f),
        Section(.5f, .75f, R.color.gauge_yellow, -1f),
        Section(.75f, 1f, R.color.gauge_red, -1f)
    )),

    MAP(R.string.sensors_frag_absolute_pressure_label, 0f, 110f, R.string.units_pressure_kpa, false, 11, listOf(
        Section(0f, .25f, R.color.gauge_gray, -1f),
        Section(.25f, .75f, R.color.gauge_green, -1f),
        Section(.75f, 1f, R.color.gauge_gray, -1f),
    )),

    MAP_TURBO(R.string.sensors_frag_absolute_pressure_turbo_label, 0f, 250f, R.string.units_pressure_kpa, false, 11, listOf(
        Section(0f, .25f, R.color.gauge_gray, -1f),
        Section(.25f, .75f, R.color.gauge_green, -1f),
        Section(.75f, 1f, R.color.gauge_gray, -1f),
    )),

    VOLTAGE(R.string.sensors_frag_voltage_label, 2.0f, 18.0f, R.string.units_volts, false, 9, listOf(
        Section(0f, 0.5f, R.color.gauge_red, -1f),
        Section(0.5f, 0.81f, R.color.gauge_green, -1f),
        Section(0.81f, 1f, R.color.gauge_red, -1f),
    )),

    CURRENT_ANGLE(R.string.sensors_frag_advance_angle_label, -15.0f, 65.0f, R.string.units_degree, false, 9, listOf(
        Section(0f, 0.1875f, R.color.gauge_lt_gray, -1f),
        Section(0.1875f, 0.75f, R.color.gauge_yellow, -1f),
        Section(0.75f, 1f, R.color.gauge_red, -1f),
    )),

    TEMPERATURE(R.string.sensors_frag_coolant_temperature_label, -40.0f, 99.0f, R.string.units_degrees_celcius, false, 9, listOf(
        Section(0f, 0.5625f, R.color.gauge_lt_purple, -1f),
        Section(0.5625f, 0.875f, R.color.gauge_gray, -1f),
        Section(0.875f, 1.0f, R.color.gauge_red, -1f),
    )),

    ADD1(R.string.sensors_frag_input_1_label, 0f, 5.0f, R.string.units_volts, false, 11, listOf(
        Section(0f, 1f, R.color.gauge_lt_purple, -1f)
    )),

    ADD2(R.string.sensors_frag_input_2_label, 0f, 5.0f, R.string.units_volts, false, 11, listOf(
        Section(0f, 1f, R.color.gauge_lt_purple, -1f)
    )),

    INJ_PW(R.string.sensors_frag_injection_pw_label, 0f, 24.0f, R.string.units_ms, false, 13, listOf(
        Section(0f, 0.0416f, R.color.gauge_red, -1f),
        Section(0.0416f, 0.4166f, R.color.gauge_green, -1f),
        Section(0.4166f, 1.0f, R.color.gauge_lt_purple, -1f),
        )),

    IAT(R.string.sensors_frag_intake_air_temp_label, -40f, 120.0f, R.string.units_degrees_celcius, false, 9, listOf(
        Section(0f, 0.5625f, R.color.gauge_lt_purple, -1f),
        Section(0.5625f, 0.875f, R.color.gauge_gray, -1f),
        Section(0.875f, 1.0f, R.color.gauge_lt_red, -1f),
    )),

    EGO_CORR(R.string.sensors_frag_ego_correction_label, -50f, 50.0f, R.string.units_percents, false, 9, listOf(
        Section(0f, 0.3f, R.color.gauge_red, -1f),
        Section(0.3f, 0.7f, R.color.gauge_green, -1f),
        Section(0.7f, 1.0f, R.color.gauge_red, -1f),
    )),

    CHOKE_POSITION(R.string.sensors_frag_iac_valve_label, 0f, 100.0f, R.string.units_percents, false, 9, listOf(
        Section(0f, 0.05f, R.color.gauge_lt_purple, -1f),
        Section(0.05f, 0.75f, R.color.gauge_dark_green, -1f),
        Section(0.75f, 1.0f, R.color.gauge_lt_red, -1f),
    )),

    AIR_FLOW(R.string.sensors_frag_air_flow_label, 0f, 16.0f, R.string.units_curve_n, true, 9, listOf(
        Section(0f, 1f, R.color.gauge_green, -1f)
    )),

    VEHICLE_SPEED(R.string.sensors_frag_vehicle_speed_label, 0f, 220f, R.string.units_km_h, true, 12, listOf(
        Section(0f, 0.2857f, R.color.gauge_green, -1f),
        Section(0.2857f, 0.5714f, R.color.gauge_green, -1f),
        Section(0.5714f, 0.8571f, R.color.gauge_lt_red, -1f),
        Section(0.8571f, 1.0f, R.color.gauge_red, -1f),
    )),

    TPS_DOT(R.string.sensors_frag_tps_dot_label, -500.0f, 500f, R.string.units_percent_per_second, true, 6, listOf(
        Section(0f, 0.3f, R.color.gauge_red, -1f),
        Section(0.3f, 0.45f, R.color.gauge_green, -1f),
        Section(0.45f, 0.55f, R.color.gauge_blue, -1f),
        Section(0.55f, 0.7f, R.color.gauge_green, -1f),
        Section(0.7f, 1.0f, R.color.gauge_red, -1f),
    )),

    MAP2(R.string.sensors_frag_map2_label, 0.0f, 400f, R.string.units_pressure_kpa, false, 11, listOf(
        Section(0f, .25f, R.color.gauge_gray, -1f),
        Section(.25f, .75f, R.color.gauge_green, -1f),
        Section(.75f, 1f, R.color.gauge_gray, -1f),
    )),

    DIFF_PRESSURE(R.string.sensors_frag_diff_pressure_label, 0.0f, 400f, R.string.units_pressure_kpa, false, 11, listOf(
        Section(0f, .25f, R.color.gauge_gray, -1f),
        Section(.25f, .75f, R.color.gauge_green, -1f),
        Section(.75f, 1f, R.color.gauge_gray, -1f),
    )),

    IAT2(R.string.sensors_frag_iat2_label, -40.0f, 120f, R.string.units_degrees_celcius, false, 9, listOf(
        Section(0f, 0.5625f, R.color.gauge_lt_purple, -1f),
        Section(0.5625f, 0.875f, R.color.gauge_gray, -1f),
        Section(0.875f, 1.0f, R.color.gauge_lt_red, -1f),
    )),

    FUEL_CONSUMPTION(R.string.sensors_frag_fuel_consumption_label, 0.0f, 50f, R.string.units_l_per_100km, false, 13, listOf(
        Section(0f, 0.21f, R.color.gauge_green, -1f),
        Section(0.21f, 0.4f, R.color.gauge_dark_yellow, -1f),
        Section(0.4f, 1.0f, R.color.gauge_red, -1f),
    )),

    KNOCK_RETARD(R.string.sensors_frag_knock_retard_label, 0.0f, 30f, R.string.units_degree, false, 8, listOf(
        Section(0f, 0.333f, R.color.gauge_dark_yellow, -1f),
        Section(0.333f, 1.0f, R.color.gauge_lt_red, -1f),
    )),

    KNOCK_SIGNAL(R.string.sensors_frag_knock_signal_label, 0.0f, 5.0f, R.string.units_volts, false, 11, listOf(
        Section(0f, 1f, R.color.gauge_green, -1f)
    )),

    WBO_AFR(R.string.sensors_frag_wbo_afr_label, 6.0f, 24.0f, R.string.units_afr, false, 10, listOf(
        Section(0f, 0.222f, R.color.gauge_lt_red, -1f),
        Section(0.222f, 0.8f, R.color.gauge_lt_yellow, -1f),
        Section(0.8f, 1.0f, R.color.gauge_lt_red, -1f),
    )),

    IAC_VALVE(R.string.sensors_frag_throttle_gate_label, 0.0f, 100.0f, R.string.units_percents, false, 11, listOf(
        Section(0f, 1f, R.color.gauge_cyan, -1f)
    )),

    GAS_DISPENSER(R.string.sensors_frag_gas_dispenser_label, 0.0f, 100.0f, R.string.units_percents, false, 11, listOf(
        Section(0f, 1f, R.color.gauge_cyan, -1f)
    )),

    SYNTHETIC_LOAD(R.string.sensors_frag_synthetic_load_label, 0.0f, 250.0f, R.string.units_empty, false, 11, listOf(
        Section(0f, 0.4f, R.color.gauge_green, -1f),
        Section(0.4f, 1.0f, R.color.gauge_lt_red, -1f)
    )),

    BEGIN_INJ_PHASE(R.string.sensors_frag_begin_inj_phase_label, 0.0f, 720.0f, R.string.units_degree_word, false, 9, listOf(
        Section(0f, 1f, R.color.gauge_lt_gray, -1f)
    )),

    END_INJ_PHASE(R.string.sensors_frag_end_inj_phase_label, 0.0f, 720.0f, R.string.units_degree_word, false, 9, listOf(
        Section(0f, 1f, R.color.gauge_lt_gray, -1f)
    )),

    FUEL_CONSUMPTION_HZ(R.string.sensors_frag_fuel_consumption_hz_label, 0.0f, 250.0f, R.string.units_hertz, false, 11, listOf(
        Section(0f, 0.04f, R.color.gauge_gray, -1f),
        Section(0.04f, 0.3f, R.color.gauge_green, -1f),
        Section(0.3f, 0.6f, R.color.gauge_lt_yellow, -1f),
        Section(0.6f, 1.0f, R.color.gauge_lt_red, -1f),
    )),

    GRTS(R.string.sensors_frag_grts_label, -40.0f, 120.0f, R.string.units_degrees_celcius, false, 9, listOf(
        Section(0f, 0.55f, R.color.gauge_lt_purple, -1f),
        Section(0.55f, 0.9f, R.color.gauge_lt_gray, -1f),
        Section(0.9f, 1.0f, R.color.gauge_lt_red, -1f),
    )),

    FUEL_LEVEL(R.string.sensors_frag_fuel_level_label, 0.0f, 100.0f, R.string.units_liter, false, 11, listOf(
        Section(0f, 0.1f, R.color.gauge_lt_red, -1f),
        Section(0.1f, 0.5f, R.color.gauge_lt_yellow, -1f),
        Section(0.5f, 1.0f, R.color.gauge_green, -1f),
    )),

    EXHAUST_GAS_TEMP(R.string.sensors_frag_exhaust_gas_temp_label, 0.0f, 1100.0f, R.string.units_degrees_celcius, false, 9, listOf(
        Section(0f, 0.37f, R.color.gauge_dark_blue, -1f),
        Section(0.37f, 0.86f, R.color.gauge_dark_yellow, -1f),
        Section(0.86f, 1.0f, R.color.gauge_lt_red, -1f),
    )),

    OIL_PRESSURE(R.string.sensors_frag_oil_pressure_label, 0.0f, 6.0f, R.string.units_degrees_celcius, false, 7, listOf(
        Section(0f, 0.16f, R.color.gauge_lt_purple, -1f),
        Section(0.16f, 0.58f, R.color.gauge_green, -1f),
        Section(0.58f, 0.83f, R.color.gauge_yellow, -1f),
        Section(0.83f, 1.0f, R.color.gauge_lt_red, -1f),
    )),

    INJ_DUTY(R.string.sensors_frag_injector_duty_label, 0.0f, 100.0f, R.string.units_percents, false, 11, listOf(
        Section(0f, 0.1f, R.color.gauge_dark_blue, -1f),
        Section(0.1f, 0.7f, R.color.gauge_green, -1f),
        Section(0.7f, 0.9f, R.color.gauge_yellow, -1f),
        Section(0.9f, 1.0f, R.color.gauge_lt_red, -1f),
    )),

    MAF(R.string.sensors_frag_maf_label, 0.0f, 600.0f, R.string.units_gram_per_second, false, 11, listOf(
        Section(0f, 0.16f, R.color.gauge_lt_purple, -1f),
        Section(0.16f, 0.5f, R.color.gauge_lt_green, -1f),
        Section(0.5f, 0.83f, R.color.gauge_lt_yellow, -1f),
        Section(0.83f, 1.0f, R.color.gauge_lt_red, -1f),
    )),

    FAN_DUTY(R.string.sensors_frag_fan_duty_label, 0.0f, 100.0f, R.string.units_percents, false, 11, listOf(
        Section(0f, 0.1f, R.color.gauge_blue, -1f),
        Section(0.1f, 0.7f, R.color.gauge_green, -1f),
        Section(0.7f, 0.9f, R.color.gauge_lt_yellow, -1f),
        Section(0.9f, 1.0f, R.color.gauge_lt_red, -1f),
    )),

    MAP_DOT(R.string.sensors_frag_map_dot_label, -500.0f, 500.0f, R.string.units_kpa_per_sec, true, 6, listOf(
        Section(0f, 0.3f, R.color.gauge_red, -1f),
        Section(0.3f, 0.45f, R.color.gauge_green, -1f),
        Section(0.45f, 0.55f, R.color.gauge_blue, -1f),
        Section(0.55f, 0.7f, R.color.gauge_green, -1f),
        Section(0.7f, 1.0f, R.color.gauge_red, -1f),
    )),

    FUEL_TEMP(R.string.sensors_frag_fuel_temp_label, -40.0f, 120.0f, R.string.units_degrees_celcius, false, 9, listOf(
        Section(0f, 0.56f, R.color.gauge_dark_blue, -1f),
        Section(0.56f, 0.87f, R.color.gauge_gray, -1f),
        Section(0.87f, 1.0f, R.color.gauge_red, -1f),
    )),

    EGO_CORR2(R.string.sensors_frag_ego_correction2_label, -50.0f, 50.0f, R.string.units_percents, false, 9, listOf(
        Section(0f, 0.3f, R.color.gauge_red, -1f),
        Section(0.3f, 0.7f, R.color.gauge_green, -1f),
        Section(0.7f, 1.0f, R.color.gauge_red, -1f),
    )),

    WBO_AFR2(R.string.sensors_frag_wbo_afr_2_label, 6.0f, 24.0f, R.string.units_afr, false, 10, listOf(
        Section(0f, 0.222f, R.color.gauge_lt_red, -1f),
        Section(0.222f, 0.8f, R.color.gauge_lt_yellow, -1f),
        Section(0.8f, 1.0f, R.color.gauge_lt_red, -1f),
    )),

    WBO_AFR_TABL(R.string.wbo_afr_tabl, 6.0f, 24.0f, R.string.units_afr, false, 10, listOf(
        Section(0f, 0.222f, R.color.gauge_lt_red, -1f),
        Section(0.222f, 0.8f, R.color.gauge_lt_yellow, -1f),
        Section(0.8f, 1.0f, R.color.gauge_lt_red, -1f),
    )),

    AFR_DIFF(R.string.sensors_frag_afr_difference_label, -9.0f, 9.0f, R.string.units_afr, false, 10, listOf(
        Section(0f, 0.361f, R.color.gauge_blue, -1f),
        Section(0.361f, 0.45f, R.color.gauge_yellow, -1f),
        Section(0.45f, 0.55f, R.color.gauge_green, -1f),
        Section(0.55f, 0.639f, R.color.gauge_yellow, -1f),
        Section(0.639f, 1.0f, R.color.gauge_red, -1f),
    )),

    AFR_DIFF2(R.string.sensors_frag_afr_difference2_label, -9.0f, 9.0f, R.string.units_afr, false, 10, listOf(
        Section(0f, 0.361f, R.color.gauge_blue, -1f),
        Section(0.361f, 0.45f, R.color.gauge_yellow, -1f),
        Section(0.45f, 0.55f, R.color.gauge_green, -1f),
        Section(0.55f, 0.639f, R.color.gauge_yellow, -1f),
        Section(0.639f, 1.0f, R.color.gauge_red, -1f),
    )),

    GAS_PRESSURE_SENS(R.string.sensors_frag_gas_pressure_label,  0.0f, 400f, R.string.units_pressure_kpa, false, 11, listOf(
        Section(0f, .25f, R.color.gauge_gray, -1f),
        Section(.25f, .75f, R.color.gauge_green, -1f),
        Section(.75f, 1f, R.color.gauge_gray, -1f),
    )),

    FUEL_PRESSURE_SENS(R.string.sensors_frag_fuel_pressure_label,  0.0f, 700f, R.string.units_pressure_kpa, false, 20, listOf(
        Section(0f, .4f, R.color.gauge_lt_red, -1f),
        Section(.4f, 1f, R.color.gauge_green, -1f),
    )),

    APPS1(R.string.sensors_frag_acceleration_pedal_sensor_label, 0.0f, 100.0f, R.string.units_percents, false, 16, listOf(
        Section(0f, 0.1f, R.color.gauge_purple, -1f),
        Section(0.1f, 0.9f, R.color.gauge_green, -1f),
        Section(0.9f, 1.0f, R.color.gauge_red, -1f),
    ));

    fun getSections(context: Context, width: Float): List<Section> {
        return sections.map {
            Section(it.startOffset, it.endOffset, ContextCompat.getColor(context, it.color), width)
        }
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
            FUEL_CONSUMPTION -> packet.cons_fuel
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
        }
    }
}