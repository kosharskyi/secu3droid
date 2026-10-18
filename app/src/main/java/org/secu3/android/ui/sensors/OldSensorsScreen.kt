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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
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
import org.secu3.android.models.InjectionTimingMode
import org.secu3.android.ui.sensors.models.GaugeType
import org.secu3.android.ui.sensors.models.IndicatorType

@Composable
internal fun OldSensorsScreen(
    packet: SensorsPacket?,
    modifier: Modifier = Modifier,
    injectionTimingMode: InjectionTimingMode = InjectionTimingMode.Default,
) {
    val p = packet ?: SensorsPacket()
    val rows = listOf(
        typedRow(R.string.sensors_frag_rpm_label, GaugeType.RPM, p.rpm.toFloat()),
        typedRow(R.string.sensors_frag_absolute_pressure_label, GaugeType.MAP, p.map),
        typedRow(R.string.sensors_frag_voltage_label, GaugeType.VOLTAGE, p.voltage),
        typedRow(R.string.sensors_frag_coolant_temperature_label, GaugeType.TEMPERATURE, p.temperature),
        typedRow(R.string.sensors_frag_advance_angle_label, GaugeType.CURRENT_ANGLE, p.currentAngle),
        typedRow(R.string.sensors_frag_map2_label, GaugeType.MAP2, p.map2),
        typedRow(R.string.sensors_frag_diff_pressure_label, GaugeType.DIFF_PRESSURE, p.mapd),
        typedRow(R.string.sensors_frag_map_dot_label, GaugeType.MAP_DOT, p.mapdot.toFloat()),
        typedRow(R.string.sensors_frag_knock_signal_label, GaugeType.KNOCK_SIGNAL, p.knockValue),
        typedRow(R.string.sensors_frag_knock_retard_label, GaugeType.KNOCK_RETARD, p.knockRetard),
        typedRow(R.string.sensors_frag_maf_label, GaugeType.MAF, p.maf),
        typedRow(R.string.sensors_frag_air_flow_label, GaugeType.AIR_FLOW, p.airflow.toFloat()),
        typedRow(R.string.sensors_frag_intake_air_temp_label, GaugeType.IAT, p.airtempSensor),
        typedRow(R.string.sensors_frag_throttle_gate_label, GaugeType.IAC_VALVE, p.tps),
        typedRow(R.string.sensors_frag_tps_dot_label, GaugeType.TPS_DOT, p.tpsdot.toFloat()),
        typedRow(R.string.sensors_frag_input_1_label, GaugeType.ADD1, p.addI1),
        typedRow(R.string.sensors_frag_input_2_label, GaugeType.ADD2, p.addI2),
        typedRow(R.string.sensors_frag_iac_valve_label, GaugeType.CHOKE_POSITION, p.chokePosition),
        typedRow(R.string.sensors_frag_gas_dispenser_label, GaugeType.GAS_DISPENSER, p.gasDosePosition),
        typedRow(R.string.sensors_frag_grts_label, GaugeType.GRTS, p.grts),
        typedRow(R.string.sensors_frag_synthetic_load_label, GaugeType.SYNTHETIC_LOAD, p.load, includeUnit = false),
        typedRow(R.string.sensors_frag_vehicle_speed_label, GaugeType.VEHICLE_SPEED, p.speed),
        typedRow(R.string.sensors_frag_distance_label, GaugeType.DISTANCE, p.distance),
        typedRow(R.string.sensors_frag_fuel_consumption_hz_label, GaugeType.FUEL_CONSUMPTION_HZ, p.fuelFlowFrequency),
        typedRow(R.string.sensors_frag_fuel_consumption_label, GaugeType.FUEL_CONSUMPTION, p.inj_ffd),
        typedRow(R.string.sensors_frag_injection_pw_label, GaugeType.INJ_PW, p.injPw),
        typedRow(R.string.sensors_frag_injector_duty_label, GaugeType.INJ_DUTY, p.sens_injDuty),
        typedRow(R.string.sensors_frag_begin_inj_phase_label, GaugeType.BEGIN_INJ_PHASE, injectionTimingMode.transform(p.injTimBegin)),
        typedRow(R.string.sensors_frag_end_inj_phase_label, GaugeType.END_INJ_PHASE, injectionTimingMode.transform(p.injTimEnd)),
        typedRow(R.string.sensors_frag_ego_correction_label, GaugeType.EGO_CORR, p.lambdaCorr),
        typedRow(R.string.sensors_frag_ego_correction2_label, GaugeType.EGO_CORR2, p.lambdaCorr2),
        typedRow(R.string.sensors_frag_wbo_afr_label, GaugeType.WBO_AFR, p.afr),
        typedRow(R.string.sensors_frag_wbo_afr_2_label, GaugeType.WBO_AFR2, p.afr2),
        typedRow(R.string.wbo_afr_tabl, GaugeType.WBO_AFR_TABL, p.afrMap),
        typedRow(R.string.sensors_frag_iat2_label, GaugeType.IAT2, p.tmp2),
        typedRow(R.string.sensors_frag_fuel_level_label, GaugeType.FUEL_LEVEL, p.ftls),
        typedRow(R.string.sensors_frag_exhaust_gas_temp_label, GaugeType.EXHAUST_GAS_TEMP, p.egts),
        typedRow(R.string.sensors_frag_oil_pressure_label, GaugeType.OIL_PRESSURE, p.ops),
        typedRow(R.string.sensors_frag_fan_duty_label, GaugeType.FAN_DUTY, p.ventDuty),
        typedRow(R.string.sensors_frag_fuel_temp_label, GaugeType.FUEL_TEMP, p.fts),
        typedRow(R.string.sensors_frag_afr_difference_label, GaugeType.AFR_DIFF, p.afr - p.afrMap),
        typedRow(R.string.sensors_frag_afr_difference2_label, GaugeType.AFR_DIFF2, p.afr2 - p.afrMap),
        typedRow(R.string.sensors_frag_gas_pressure_label, GaugeType.GAS_PRESSURE_SENS, p.gasPressureSensor),
        // Keep the original row indexes stable; newly covered values follow the original 43 rows.
        typedRow(R.string.sensors_frag_fuel_pressure_label, GaugeType.FUEL_PRESSURE_SENS, p.fuelPressureSensor),
        typedRow(R.string.sensors_frag_acceleration_pedal_sensor_label, GaugeType.APPS1, p.apps1),
        typedRow(R.string.sensors_frag_oil_temperature_sensor_label, GaugeType.OTS, p.ots),
        typedRow(R.string.sensors_frag_estimated_torque_label, GaugeType.EST_TORQUE, p.estTorque),
        typedRow(R.string.sensors_frag_requested_torque_label, GaugeType.REQ_TORQUE, p.reqTorque),
        typedRow(R.string.sensors_frag_fuel_consumption_hourly_label, GaugeType.FUEL_CONSUMPTION_HOURLY, p.inj_ffh),
        typedRow(R.string.sensors_frag_consumed_fuel_label, GaugeType.CONSUMED_FUEL, p.cons_fuel),
    )
    val statuses = listOf(
        listOf(IndicatorType.GAS_VALVE, IndicatorType.THROTTLE, IndicatorType.FI_FUEL),
        listOf(IndicatorType.POWER_VALVE, IndicatorType.STARTER_BLOCKING, IndicatorType.AE),
        listOf(IndicatorType.COOLING_FAN, IndicatorType.CHECK_ENGINE, IndicatorType.REV_LIM_FUEL_CUT),
        listOf(IndicatorType.FLOOD_CLEAR_MODE, IndicatorType.SYSTEM_LOCKED, IndicatorType.IGN_I_INPUT),
        listOf(IndicatorType.COND_I_INPUT, IndicatorType.EPAS_I_INPUT, IndicatorType.AFTERSTART_ENR),
        listOf(IndicatorType.IAC_CLOSED_LOOP, null, null),
        listOf(IndicatorType.UNIV_OUT1, IndicatorType.UNIV_OUT2, IndicatorType.UNIV_OUT3),
        listOf(IndicatorType.UNIV_OUT4, IndicatorType.UNIV_OUT5, IndicatorType.UNIV_OUT6),
        listOf(IndicatorType.GPA4_I, IndicatorType.INPUT1, IndicatorType.INPUT2),
        listOf(IndicatorType.AUTO_I, IndicatorType.MAPSEL0, IndicatorType.REFPRS_I),
        listOf(IndicatorType.ALTRN_I, null, null),
    )
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp).testTag("old-sensors-list")) {
        SensorGroupSurface(Modifier.fillMaxWidth().testTag("old-sensors-measurements")) {
            rows.forEachIndexed { index, item ->
                SensorReadoutRow(
                    label = stringResource(item.label),
                    value = if (packet == null) "—" else item.value,
                    unit = item.unit?.let { stringResource(it) }.orEmpty(),
                    rowTag = "old-sensor-row-$index",
                    labelTag = "old-sensor-label-$index",
                    valueTag = "old-sensor-value-$index",
                    unitTag = "old-sensor-unit-$index",
                )
                if (index != rows.lastIndex) SensorRowDivider()
            }
        }
        Spacer(Modifier.height(12.dp))
        statuses.forEachIndexed { group, entries ->
            Row(Modifier.fillMaxWidth().padding(top = 8.dp).testTag("old-status-group-$group")) {
                entries.forEachIndexed { cell, entry ->
                    val label = entry?.let { stringResource(it.title) }.orEmpty()
                    val palette = org.secu3.android.ui.compose.LocalSecuPalette.current
                    val activeState = entry?.isActive(p) == true && packet != null
                    val background = when {
                        entry == null -> Color.Transparent
                        packet == null -> MaterialTheme.colorScheme.background
                        activeState -> palette.successContainer
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                    val foreground = when {
                        activeState -> palette.onSuccessContainer
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    val state = when {
                        entry == null -> ""
                        packet == null -> stringResource(R.string.sensors_status_accessibility_unavailable)
                        activeState -> stringResource(R.string.sensors_status_accessibility_on)
                        else -> stringResource(R.string.sensors_status_accessibility_off)
                    }
                    Text(
                        if (entry == null) "" else label,
                        Modifier.weight(1f).padding(horizontal = 2.dp).background(background).padding(4.dp).testTag("old-status-$group-$cell")
                            .semantics { stateDescription = state },
                        color = foreground,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

private data class OldSensorRow(val label: Int, val value: String, val unit: Int?)
private fun typedRow(label: Int, type: GaugeType, value: Float, includeUnit: Boolean = true) =
    OldSensorRow(label, type.formatValue(value), type.units.takeIf { includeUnit })
