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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.secu3.android.R
import org.secu3.android.models.packets.input.SensorsPacket
import java.util.Locale

@Composable
internal fun OldSensorsScreen(packet: SensorsPacket?, modifier: Modifier = Modifier) {
    val p = packet ?: SensorsPacket()
    val rows = listOf(
        row(R.string.sensors_frag_rpm_label, p.rpm.toString()),
        row(R.string.sensors_frag_absolute_pressure_label, fmt(p.map, "%.2f")),
        row(R.string.sensors_frag_voltage_label, fmt(p.voltage, "%.1f")),
        row(R.string.sensors_frag_coolant_temperature_label, fmt(p.temperature, "%.1f")),
        row(R.string.sensors_frag_advance_angle_label, fmt(p.currentAngle, "%.1f")),
        row(R.string.sensors_frag_map2_label, fmt(p.map2, "%.1f"), R.string.units_pressure_kpa),
        row(R.string.sensors_frag_diff_pressure_label, fmt(p.mapd, "%.1f"), R.string.units_pressure_kpa),
        row(R.string.sensors_frag_map_dot_label, p.mapdot.toString(), R.string.units_percent_per_second),
        row(R.string.sensors_frag_knock_signal_label, fmt(p.knockValue, "%.3f"), R.string.units_curve_n),
        row(R.string.sensors_frag_knock_retard_label, fmt(p.knockRetard, "%.1f"), R.string.units_degree_word),
        row(R.string.sensors_frag_maf_label, fmt(p.maf, "%.1f"), R.string.units_gram_per_second),
        row(R.string.sensors_frag_air_flow_label, p.airflow.toString(), R.string.units_curve_n),
        row(R.string.sensors_frag_intake_air_temp_label, fmt(p.airtempSensor, "%.1f"), R.string.units_degrees_celcius),
        row(R.string.sensors_frag_throttle_gate_label, fmt(p.tps, "%.1f"), R.string.units_percents),
        row(R.string.sensors_frag_tps_dot_label, p.tpsdot.toString(), R.string.units_percent_per_second),
        row(R.string.sensors_frag_input_1_label, fmt(p.addI1, "%.3f"), R.string.units_volts),
        row(R.string.sensors_frag_input_2_label, fmt(p.addI2, "%.3f"), R.string.units_volts),
        row(R.string.sensors_frag_iac_valve_label, fmt(p.chokePosition, "%.1f")),
        row(R.string.sensors_frag_gas_dispenser_label, p.gasDosePosition.toString(), R.string.units_percents),
        row(R.string.sensors_frag_grts_label, fmt(p.grts, "%.1f"), R.string.units_degrees_celcius),
        row(R.string.sensors_frag_synthetic_load_label, fmt(p.load, "%.1f")),
        row(R.string.sensors_frag_vehicle_speed_label, fmt(p.speed, "%.1f"), R.string.units_km_h),
        row(R.string.sensors_frag_distance_label, fmt(p.distance, "%.1f"), R.string.units_km),
        row(R.string.sensors_frag_fuel_consumption_hz_label, p.fuelFlowFrequency.toString(), R.string.units_hertz),
        row(R.string.sensors_frag_fuel_consumption_label, fmt(p.cons_fuel, "%.2f"), R.string.units_l_per_100km),
        row(R.string.sensors_frag_injection_pw_label, fmt(p.injPw, "%.2f"), R.string.units_ms),
        row(R.string.sensors_frag_injector_duty_label, fmt(p.sens_injDuty, "%.1f"), R.string.units_percents),
        row(R.string.sensors_frag_begin_inj_phase_label, fmt(p.injTimBegin, "%.1f")),
        row(R.string.sensors_frag_end_inj_phase_label, fmt(p.injTimEnd, "%.1f")),
        row(R.string.sensors_frag_ego_correction_label, fmt(p.lambdaCorr, "%.2f"), R.string.units_percents),
        row(R.string.sensors_frag_ego_correction2_label, fmt(p.lambdaCorr2, "%.1f"), R.string.units_percents),
        row(R.string.sensors_frag_wbo_afr_label, fmt(p.afr, "%.1f")),
        row(R.string.sensors_frag_wbo_afr_2_label, fmt(p.afr2, "%.1f")),
        row(R.string.wbo_afr_tabl, fmt(p.afrMap, "%.2f")),
        row(R.string.sensors_frag_iat2_label, fmt(p.tmp2, "%.1f"), R.string.units_afr),
        row(R.string.sensors_frag_fuel_level_label, fmt(p.ftls, "%.1f"), R.string.units_liter),
        row(R.string.sensors_frag_exhaust_gas_temp_label, fmt(p.egts, "%.1f"), R.string.units_degrees_celcius),
        row(R.string.sensors_frag_oil_pressure_label, fmt(p.ops, "%.1f"), R.string.units_kg_per_cm2),
        row(R.string.sensors_frag_fan_duty_label, p.ventDuty.toString(), R.string.units_percents),
        row(R.string.sensors_frag_fuel_temp_label, fmt(p.fts, "%.1f"), R.string.units_degrees_celcius),
        row(R.string.sensors_frag_afr_difference_label, fmt(p.afr - p.afrMap, "%.2f")),
        row(R.string.sensors_frag_afr_difference2_label, fmt(p.afr2 - p.afrMap, "%.2f")),
        row(R.string.sensors_frag_gas_pressure_label, fmt(p.gasPressureSensor, "%.1f"), R.string.units_pressure_kpa),
    )
    val statuses = listOf(
        listOf(R.string.sensors_frag_status_gas_valve_label to (p.gasBit > 0), R.string.sensors_frag_status_throttle_label to (p.carbBit > 0), R.string.sensors_frag_status_fl_fuel_label to (p.ephhValveBit > 0)),
        listOf(R.string.sensors_frag_status_power_valve_label to (p.epmValveBit > 0), R.string.sensors_frag_status_starter_blocking_label to (p.starterBlockBit > 0), R.string.sensors_frag_status_ae_label to (p.accelerationEnrichment > 0)),
        listOf(R.string.sensors_frag_status_cooling_fan_label to (p.coolFanBit > 0), R.string.sensors_frag_status_check_engine_label to (p.checkEngineBit > 0), R.string.sensors_frag_status_rev_lim_fuel_cut_label to (p.fc_revlim > 0)),
        listOf(R.string.sensors_frag_status_flood_clear_mode_label to (p.floodclear > 0), R.string.sensors_frag_status_system_locked_label to (p.sys_locked > 0), R.string.sensors_frag_status_input_ign_label to (p.ign_i > 0)),
        listOf(R.string.sensors_frag_status_input_cond_label to (p.cond_i > 0), R.string.sensors_frag_status_input_epas_label to (p.epas_i > 0), R.string.sensors_frag_status_afterstart_enr_label to (p.aftstr_enr > 0)),
        listOf(R.string.sensors_frag_status_iac_closed_loop_label to (p.iac_closed_loop > 0), null, null),
        listOf(R.string.sensors_frag_status_univ_out_1_label to (p.uniOut0Bit > 0), R.string.sensors_frag_status_univ_out_2_label to (p.uniOut1Bit > 0), R.string.sensors_frag_status_univ_out_3_label to (p.uniOut2Bit > 0)),
        listOf(R.string.sensors_frag_status_univ_out_4_label to (p.uniOut3Bit > 0), R.string.sensors_frag_status_univ_out_5_label to (p.uniOut4Bit > 0), R.string.sensors_frag_status_univ_out_6_label to (p.uniOut5Bit > 0)),
    )
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 8.dp).testTag("old-sensors-list")) {
        rows.forEachIndexed { index, item ->
            val label = if (packet == null) "" else stringResource(item.label)
            val text = if (packet == null || item.unit == null) label else "$label, ${stringResource(item.unit)}:"
            Row(Modifier.fillMaxWidth().testTag("old-sensor-row-$index"), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text, Modifier.weight(1f).padding(8.dp).testTag("old-sensor-label-$index"), color = Color.Black, fontSize = 16.sp)
                Text(if (packet == null) "" else item.value, Modifier.padding(8.dp).testTag("old-sensor-value-$index"), color = Color.Black, fontSize = 16.sp)
            }
            HorizontalDivider(color = Color(0xFFEDEDED), thickness = 1.dp)
        }
        statuses.forEachIndexed { group, entries ->
            Row(Modifier.fillMaxWidth().padding(top = 8.dp).testTag("old-status-group-$group")) {
                entries.forEachIndexed { cell, entry ->
                    val label = if (packet == null) "" else entry?.let { stringResource(it.first) }.orEmpty()
                    val background = when { packet == null || entry == null -> Color.Transparent; entry.second -> Color(0xFF00FF00); else -> Color(0xFFCCCCCC) }
                    val state = when {
                        packet == null || entry == null -> stringResource(R.string.sensors_status_accessibility_unavailable)
                        entry.second -> stringResource(R.string.sensors_status_accessibility_on)
                        else -> stringResource(R.string.sensors_status_accessibility_off)
                    }
                    Text(
                        label,
                        Modifier.weight(1f).background(background).testTag("old-status-$group-$cell")
                            .semantics { stateDescription = state },
                        color = Color.Black,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

private data class OldSensorRow(val label: Int, val value: String, val unit: Int?)
private fun row(label: Int, value: String, unit: Int? = null) = OldSensorRow(label, value, unit)
private fun fmt(value: Number, pattern: String) = String.format(Locale.US, pattern, value)
