/*
 *    SecuDroid  - An open source, free manager for SECU-3 engine control unit
 *    Copyright (C) 2026 Vitalii O. Kosharskyi. Ukraine, Kyiv
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
package org.secu3.android.ui.sensors

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.assert
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.secu3.android.R
import org.secu3.android.models.packets.input.SensorsPacket
import org.secu3.android.models.InjectionTimingMode
import org.secu3.android.ui.compose.SecuComposeTheme
import org.secu3.android.ui.sensors.models.SensorPresentationCatalog

class OldSensorsScreenTest {
    private val presentationCatalog = SensorPresentationCatalog()
    @get:Rule val compose = createComposeRule()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val labels = listOf(
        R.string.sensors_frag_rpm_label, R.string.sensors_frag_absolute_pressure_label, R.string.sensors_frag_voltage_label,
        R.string.sensors_frag_coolant_temperature_label, R.string.sensors_frag_advance_angle_label,
        R.string.sensors_frag_map2_label, R.string.sensors_frag_diff_pressure_label, R.string.sensors_frag_map_dot_label,
        R.string.sensors_frag_knock_signal_label, R.string.sensors_frag_knock_retard_label, R.string.sensors_frag_maf_label,
        R.string.sensors_frag_air_flow_label, R.string.sensors_frag_intake_air_temp_label, R.string.sensors_frag_throttle_gate_label,
        R.string.sensors_frag_tps_dot_label, R.string.sensors_frag_input_1_label, R.string.sensors_frag_input_2_label,
        R.string.sensors_frag_iac_valve_label, R.string.sensors_frag_gas_dispenser_label, R.string.sensors_frag_grts_label,
        R.string.sensors_frag_synthetic_load_label, R.string.sensors_frag_vehicle_speed_label, R.string.sensors_frag_distance_label,
        R.string.sensors_frag_fuel_consumption_hz_label, R.string.sensors_frag_fuel_consumption_label, R.string.sensors_frag_injection_pw_label,
        R.string.sensors_frag_injector_duty_label, R.string.sensors_frag_begin_inj_phase_label, R.string.sensors_frag_end_inj_phase_label,
        R.string.sensors_frag_ego_correction_label, R.string.sensors_frag_ego_correction2_label, R.string.sensors_frag_wbo_afr_label,
        R.string.sensors_frag_wbo_afr_2_label, R.string.wbo_afr_tabl, R.string.sensors_frag_iat2_label,
        R.string.sensors_frag_fuel_level_label, R.string.sensors_frag_exhaust_gas_temp_label, R.string.sensors_frag_oil_pressure_label,
        R.string.sensors_frag_fan_duty_label, R.string.sensors_frag_fuel_temp_label, R.string.sensors_frag_afr_difference_label,
        R.string.sensors_frag_afr_difference2_label, R.string.sensors_frag_gas_pressure_label,
        R.string.sensors_frag_fuel_pressure_label, R.string.sensors_frag_acceleration_pedal_sensor_label,
        R.string.sensors_frag_oil_temperature_sensor_label, R.string.sensors_frag_estimated_torque_label,
        R.string.sensors_frag_requested_torque_label, R.string.sensors_frag_fuel_consumption_hourly_label,
        R.string.sensors_frag_consumed_fuel_label,
    )

    private val units = listOf(
        R.string.units_rpm, R.string.units_pressure_kpa, R.string.units_volts, R.string.units_degrees_celcius, R.string.units_degree,
        R.string.units_pressure_kpa, R.string.units_pressure_kpa,
        R.string.units_kpa_per_sec, R.string.units_volts, R.string.units_degree,
        R.string.units_gram_per_second, R.string.units_curve_n, R.string.units_degrees_celcius,
        R.string.units_percents, R.string.units_percent_per_second, R.string.units_volts, R.string.units_volts,
        R.string.units_percents, R.string.units_percents, R.string.units_degrees_celcius, null, R.string.units_km_h,
        R.string.units_km, R.string.units_hertz, R.string.units_l_per_100km, R.string.units_ms,
        R.string.units_percents, R.string.units_degree_word, R.string.units_degree_word, R.string.units_percents, R.string.units_percents,
        R.string.units_afr, R.string.units_afr, R.string.units_afr, R.string.units_degrees_celcius, R.string.units_liter, R.string.units_degrees_celcius,
        R.string.units_kg_per_cm2, R.string.units_percents, R.string.units_degrees_celcius,
        R.string.units_afr, R.string.units_afr, R.string.units_pressure_kpa,
        R.string.units_pressure_kpa, R.string.units_percents, R.string.units_degrees_celcius,
        R.string.units_torque_nm, R.string.units_torque_nm, R.string.units_l_per_hour, R.string.units_liter,
    )
    private val statusLabels = listOf(
        listOf(R.string.sensors_frag_status_gas_valve_label, R.string.sensors_frag_status_throttle_label, R.string.sensors_frag_status_fl_fuel_label),
        listOf(R.string.sensors_frag_status_power_valve_label, R.string.sensors_frag_status_starter_blocking_label, R.string.sensors_frag_status_ae_label),
        listOf(R.string.sensors_frag_status_cooling_fan_label, R.string.sensors_frag_status_check_engine_label, R.string.sensors_frag_status_rev_lim_fuel_cut_label),
        listOf(R.string.sensors_frag_status_flood_clear_mode_label, R.string.sensors_frag_status_system_locked_label, R.string.sensors_frag_status_input_ign_label),
        listOf(R.string.sensors_frag_status_input_cond_label, R.string.sensors_frag_status_input_epas_label, R.string.sensors_frag_status_afterstart_enr_label),
        listOf(R.string.sensors_frag_status_iac_closed_loop_label, null, null),
        listOf(R.string.sensors_frag_status_univ_out_1_label, R.string.sensors_frag_status_univ_out_2_label, R.string.sensors_frag_status_univ_out_3_label),
        listOf(R.string.sensors_frag_status_univ_out_4_label, R.string.sensors_frag_status_univ_out_5_label, R.string.sensors_frag_status_univ_out_6_label),
        listOf(R.string.sensors_frag_status_gpa4_input_label, R.string.sensors_frag_status_input1_label, R.string.sensors_frag_status_input2_label),
        listOf(R.string.sensors_frag_status_auto_i_label, R.string.sensors_frag_status_mapsel0_label, R.string.sensors_frag_status_refprs_i_label),
        listOf(R.string.sensors_frag_status_altrn_i_label, null, null),
    )

    @Test fun rendersAllFiftyRowsInStableOrderWithUnitsAndUsFormatting() {
        compose.setContent { SecuComposeTheme { OldSensorsScreen(fixture(), presentationCatalog = presentationCatalog) } }
        assertEquals(50, labels.size)
        assertEquals(labels.size, units.size)
        labels.indices.forEach { index ->
            compose.onNodeWithTag("old-sensors-list").performScrollToNode(hasTestTag("old-sensor-row-$index"))
            compose.onNodeWithTag("old-sensor-label-$index").assertTextEquals(context.getString(labels[index]))
            compose.onNodeWithTag("old-sensor-value-$index").assertExists()
            units[index]?.let { unit ->
                compose.onNodeWithTag("old-sensor-unit-$index").assertTextEquals(context.getString(unit))
            } ?: compose.onAllNodesWithTag("old-sensor-unit-$index").assertCountEquals(0)
        }
        val expectedValues = listOf(
            "1234", "1.23", "2.35", "3.5", "4.57", "5.67", "6.78", "7", "8.125", "9.3", "10.10", "12",
            "13.4", "13.4", "15", "15.679", "16.789", "18.9", "19.0", "19.8", "20.30", "22.3", "23.5",
            "24.50", "8.37", "25.68", "26.9", "27.8", "28.9", "29.88", "30.4", "31.20", "32.30",
            "33.40", "35.6", "35.6", "36.7", "37.80", "38.0", "39.2", "-2.20", "-1.10", "42.50",
            "43.60", "44.7", "45.8", "46.90", "47.10", "9.50", "25.430",
        )
        expectedValues.forEachIndexed { index, value ->
            compose.onNodeWithTag("old-sensors-list").performScrollToNode(hasTestTag("old-sensor-row-$index"))
            compose.onNodeWithTag("old-sensor-value-$index").assertTextEquals(value)
        }
    }

    private val statusEnabled = listOf(
        listOf(true, false, true), listOf(false, true, false), listOf(false, true, true),
        listOf(false, true, false), listOf(false, true, false), listOf(true, null, null),
        listOf(true, false, true), listOf(false, true, false),
        listOf(true, true, true), listOf(false, true, false), listOf(true, null, null),
    )

    @Test fun startsBlankAndUpdatesValuesAndAllElevenStatusGroups() {
        val packet = mutableStateOf<SensorsPacket?>(null)
        compose.setContent { SecuComposeTheme { OldSensorsScreen(packet.value, presentationCatalog = presentationCatalog) } }
        compose.onNodeWithTag("old-sensors-list").assertExists()
        for (index in labels.indices) {
            compose.onNodeWithTag("old-sensor-label-$index").assertTextEquals(context.getString(labels[index]))
            compose.onNodeWithTag("old-sensor-value-$index").assertTextEquals("—")
            units[index]?.let { unit ->
                compose.onNodeWithTag("old-sensor-unit-$index").assertTextEquals(context.getString(unit))
            } ?: compose.onAllNodesWithTag("old-sensor-unit-$index").assertCountEquals(0)
        }
        for (group in statusLabels.indices) for (cell in 0..2) {
            val label = statusLabels[group][cell]?.let(context::getString).orEmpty()
            compose.onNodeWithTag("old-status-$group-$cell").assertTextEquals(label)
            if (label.isNotEmpty()) assertStatusState(group, cell, context.getString(R.string.sensors_status_accessibility_unavailable))
        }
        val mixedFlags = 0xA555
        val mixedOutputs = 0x15
        compose.runOnIdle { packet.value = fixture().copy(sensorsFlags = mixedFlags, uniOutput = mixedOutputs, additionalFlags = 0xAE) }
        compose.onNodeWithTag("old-sensor-value-0").assertTextEquals("1234")
        for (index in statusLabels.indices) compose.onNodeWithTag("old-status-group-$index").assertExists()
        statusLabels.forEachIndexed { group, cells ->
            compose.onNodeWithTag("old-sensors-list").performScrollToNode(hasTestTag("old-status-group-$group"))
            cells.forEachIndexed { cell, label ->
                compose.onNodeWithTag("old-status-$group-$cell").assertTextEquals(label?.let(context::getString).orEmpty())
            }
        }
        assertStatusStates(statusEnabled)
        compose.runOnIdle {
            packet.value = fixture().copy(sensorsFlags = mixedFlags xor 0xFFFF, uniOutput = mixedOutputs xor 0x3F, additionalFlags = 0x51)
        }
        assertStatusStates(statusEnabled.map { group -> group.map { it?.not() } })
    }

    @Test fun scrollReachesFinalValueAndStatusGroup() {
        compose.setContent { SecuComposeTheme { OldSensorsScreen(fixture(), presentationCatalog = presentationCatalog) } }
        compose.onNodeWithTag("old-sensors-list").performScrollToNode(hasTestTag("old-sensor-row-49"))
        compose.onNodeWithTag("old-sensor-value-49").assertIsDisplayed()
        compose.onNodeWithTag("old-sensors-list").performScrollToNode(hasTestTag("old-status-group-10"))
        compose.onNodeWithTag("old-status-10-0").assertExists()
    }

    @Test fun injectionPhasesUseSelectedDisplayConvention() {
        compose.setContent {
            SecuComposeTheme {
                OldSensorsScreen(fixture().copy(injTimBegin = 360f), injectionTimingMode = InjectionTimingMode.Btdc360, presentationCatalog = presentationCatalog)
            }
        }
        compose.onNodeWithTag("old-sensors-list").performScrollToNode(hasTestTag("old-sensor-row-27"))
        compose.onNodeWithTag("old-sensor-value-27").assertTextEquals("-360.0")
        compose.onNodeWithTag("old-sensor-unit-27").assertTextEquals(context.getString(R.string.units_degree_word))
    }

    private fun assertStatusStates(expected: List<List<Boolean?>>) {
        expected.forEachIndexed { group, cells ->
            compose.onNodeWithTag("old-sensors-list").performScrollToNode(hasTestTag("old-status-group-$group"))
            cells.forEachIndexed { cell, enabled ->
                if (enabled != null) {
                    val description = if (enabled) R.string.sensors_status_accessibility_on else R.string.sensors_status_accessibility_off
                    assertStatusState(group, cell, context.getString(description))
                }
            }
        }
    }

    private fun assertStatusState(group: Int, cell: Int, expected: String) {
        compose.onNodeWithTag("old-status-$group-$cell")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, expected))
    }

    private fun fixture() = SensorsPacket(
        rpm = 1234, map = 1.234f, voltage = 2.345f, temperature = 3.45f, currentAngle = 4.567f,
        knockValue = 8.125f, knockRetard = 9.25f, airflow = 12, tps = 13.45f, addI1 = 15.6789f,
        addI2 = 16.7891f, chokePosition = 18.9f, gasDosePosition = 19f, speed = 22.34f,
        distance = 23.45f, fuelFlowFrequency = 24.5f, airtempSensor = 13.45f, lambdaCorr = 29.876f,
        injPw = 25.678f, tpsdot = 15, map2 = 5.67f, mapd = 6.78f, tmp2 = 35.6f, afr = 31.2f,
        load = 20.3f, injTimBegin = 27.8f, injTimEnd = 28.9f, grts = 19.8f, ftls = 35.6f,
        egts = 36.7f, ops = 37.8f, sens_injDuty = 26.9f, maf = 10.1f, ventDuty = 38f,
        mapdot = 7, fts = 39.2f, cons_fuel = 25.43f, afr2 = 32.3f, lambdaCorr2 = 30.4f,
        afrMap = 33.4f, gasPressureSensor = 42.5f, fuelPressureSensor = 43.6f,
        apps1 = 44.7f, ots = 45.8f, estTorque = 46.9f, reqTorque = 47.1f,
    ).apply { inj_ffd = 8.37f; inj_ffh = 9.5f }
}
