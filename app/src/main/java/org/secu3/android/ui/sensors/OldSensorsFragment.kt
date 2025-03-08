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

package org.secu3.android.ui.sensors

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import org.secu3.android.R
import org.secu3.android.databinding.FragmentOldSensorsBinding
import org.secu3.android.utils.Task
import java.util.Locale

class OldSensorsFragment : Fragment() {

    private val mViewModel: SensorsViewModel by viewModels( ownerProducer = { requireParentFragment() } )

    private var mBinding: FragmentOldSensorsBinding? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mBinding = FragmentOldSensorsBinding.inflate(inflater, container, false)
        return mBinding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mViewModel.sensorsLiveData.observe(viewLifecycleOwner) {
            mBinding?.apply {
                rpm.title.text = getString(R.string.sensors_frag_rpm_label)
                rpm.value.text = it.rpm.toString()

                pressure.title.text = getString(R.string.sensors_frag_absolute_pressure_label)
                pressure.value.text = String.format(Locale.US, "%.2f", it.map)

                voltage.title.text = getString(R.string.sensors_frag_voltage_label)
                voltage.value.text = String.format(Locale.US, "%.1f", it.voltage)

                temperature.title.text = getString(R.string.sensors_frag_coolant_temperature_label)
                temperature.value.text =  String.format(Locale.US, "%.1f", it.temperature)

                advAngle.title.text = getString(R.string.sensors_frag_advance_angle_label)
                advAngle.value.text = String.format(Locale.US, "%.1f", it.currentAngle)

                knockRetard.title.text = "${getString(R.string.sensors_frag_knock_retard_label)}, ${getString(R.string.units_degree_word)}:"
                knockRetard.value.text = String.format(Locale.US, "%.1f", it.knockRetard)

                knockValue.title.text = "${getString(R.string.sensors_frag_knock_signal_label)}, ${getString(R.string.units_curve_n)}:"
                knockValue.value.text = String.format(Locale.US, "%.3f", it.knockValue)

                airFlow.title.text = "${getString(R.string.sensors_frag_air_flow_label)}, ${getString(R.string.units_curve_n)}:"
                airFlow.value.text = it.airflow.toString()

                tps.title.text = "${getString(R.string.sensors_frag_throttle_gate_label)}, ${getString(R.string.units_percents)}:"
                tps.value.text = String.format(Locale.US, "%.1f", it.tps)

                add1.title.text = "${getString(R.string.sensors_frag_input_1_label)}, ${getString(R.string.units_volts)}:"
                add1.value.text = String.format(Locale.US, "%.3f", it.addI1)

                add2.title.text = "${getString(R.string.sensors_frag_input_2_label)}, ${getString(R.string.units_volts)}:"
                add2.value.text = String.format(Locale.US, "%.3f", it.addI2)

                chokePosition.title.text = getString(R.string.sensors_frag_iac_valve_label)
                chokePosition.value.text = String.format(Locale.US, "%.1f", it.chokePosition)

                gasDosePosition.title.text = "${getString(R.string.sensors_frag_gas_dispenser_label)}, ${getString(R.string.units_percents)}:"
                gasDosePosition.value.text = it.gasDosePosition.toString()

                synthLoad.title.text = getString(R.string.sensors_frag_synthetic_load_label)
                synthLoad.value.text = String.format(Locale.US, "%.1f", it.load)

                speed.title.text = "${getString(R.string.sensors_frag_vehicle_speed_label)}, ${getString(R.string.units_km_h)}:"
                speed.value.text = String.format(Locale.US, "%.1f", it.speed)

                distance.title.text = "${getString(R.string.sensors_frag_distance_label)}, ${getString(R.string.units_km)}:"
                distance.value.text = String.format(Locale.US, "%.1f", it.distance)

                fuelInj.title.text = "${getString(R.string.sensors_frag_fuel_consumption_hz_label)}, ${getString(R.string.units_hertz)}:"
                fuelInj.value.text = it.fuelFlowFrequency.toString()

                airTemp.title.text = "${getString(R.string.sensors_frag_intake_air_temp_label)}, ${getString(R.string.units_degrees_celcius)}:"
                airTemp.value.text = String.format(Locale.US, "%.1f", it.airtempSensor)

                lambdaCorr.title.text = "${getString(R.string.sensors_frag_ego_correction_label)}, ${getString(R.string.units_percents)}:"
                lambdaCorr.value.text = String.format(Locale.US, "%.2f", it.lambdaCorr)

                injPw.title.text = "${getString(R.string.sensors_frag_injection_pw_label)}, ${getString(R.string.units_ms)}:"
                injPw.value.text = String.format(Locale.US, "%.2f", it.injPw)

                tpsDot.title.text = "${getString(R.string.sensors_frag_tps_dot_label)}, ${getString(R.string.units_percent_per_second)}:"
                tpsDot.value.text = it.tpsdot.toString()

                map2.title.text = "${getString(R.string.sensors_frag_map2_label)}, ${getString(R.string.units_pressure_kpa)}:"
                map2.value.text = String.format(Locale.US, "%.1f", it.map2)

                mapDiff.title.text = "${getString(R.string.sensors_frag_diff_pressure_label)}, ${getString(R.string.units_pressure_kpa)}:"
                mapDiff.value.text = String.format(Locale.US, "%.1f", it.mapd)

                tmp2.title.text = "${getString(R.string.sensors_frag_iat2_label)}, ${getString(R.string.units_afr)}:"
                tmp2.value.text = String.format(Locale.US, "%.1f", it.tmp2)

                afr.title.text = getString(R.string.sensors_frag_wbo_afr_label)
                afr.value.text = String.format(Locale.US, "%.1f", it.afr)

                consFuel.title.text = "${getString(R.string.sensors_frag_fuel_consumption_label)}, ${getString(R.string.units_l_per_100km)}:"
                consFuel.value.text = String.format(Locale.US, "%.2f", it.cons_fuel)

                grts.title.text = "${getString(R.string.sensors_frag_grts_label)}, ${getString(R.string.units_degrees_celcius)}:"
                grts.value.text = String.format(Locale.US, "%.1f", it.grts)

                ftls.title.text = "${getString(R.string.sensors_frag_fuel_level_label)}, ${getString(R.string.units_liter)}:"
                ftls.value.text = String.format(Locale.US, "%.1f", it.ftls)

                egts.title.text = "${getString(R.string.sensors_frag_exhaust_gas_temp_label)}, ${getString(R.string.units_degrees_celcius)}:"
                egts.value.text = String.format(Locale.US, "%.1f", it.egts)

                ops.title.text = "${getString(R.string.sensors_frag_oil_pressure_label)}, ${getString(R.string.units_kg_per_cm2)}:"
                ops.value.text = String.format(Locale.US, "%.1f", it.ops)

                injDuty.title.text = "${getString(R.string.sensors_frag_injector_duty_label)}, ${getString(R.string.units_percents)}:"
                injDuty.value.text = String.format(Locale.US, "%.1f", it.sens_injDuty)

                maf.title.text = "${getString(R.string.sensors_frag_maf_label)}, ${getString(R.string.units_gram_per_second)}:"
                maf.value.text = String.format(Locale.US, "%.1f", it.maf)

                ventDuty.title.text = "${getString(R.string.sensors_frag_fan_duty_label)}, ${getString(R.string.units_percents)}:"
                ventDuty.value.text = it.ventDuty.toString()

                mapDot.title.text = "${getString(R.string.sensors_frag_map_dot_label)}, ${getString(R.string.units_percent_per_second)}:"
                mapDot.value.text = it.mapdot.toString()

                fts.title.text = "${getString(R.string.sensors_frag_fuel_temp_label)}, ${getString(R.string.units_degrees_celcius)}:"
                fts.value.text = String.format(Locale.US, "%.1f", it.fts)

                lambdaCorr2.title.text = "${getString(R.string.sensors_frag_ego_correction2_label)}, ${getString(R.string.units_percents)}:"
                lambdaCorr2.value.text = String.format(Locale.US, "%.1f", it.lambdaCorr2)

                afrDifference.title.text = getString(R.string.sensors_frag_afr_difference_label)
                afrDifference.value.text = String.format(Locale.US, "%.2f", it.afr - it.afrMap)

                afrDifference2.title.text = getString(R.string.sensors_frag_afr_difference2_label)
                afrDifference2.value.text = String.format(Locale.US, "%.2f", it.afr2 - it.afrMap)

                beginInjPhase.title.text = getString(R.string.sensors_frag_begin_inj_phase_label)
                beginInjPhase.value.text = String.format(Locale.US, "%.1f", it.injTimBegin)

                endInjPhase.title.text = getString(R.string.sensors_frag_end_inj_phase_label)
                endInjPhase.value.text = String.format(Locale.US, "%.1f", it.injTimEnd)

                afrTable.title.text = getString(R.string.wbo_afr_tabl)
                afrTable.value.text = String.format(Locale.US, "%.2f", it.afrMap)

                afr2.title.text = getString(R.string.sensors_frag_wbo_afr_2_label)
                afr2.value.text = String.format(Locale.US, "%.1f", it.afr2)

                gasPressureSensor.title.text = "${getString(R.string.sensors_frag_gas_pressure_label)}, ${getString(R.string.units_pressure_kpa)}:"
                gasPressureSensor.value.text = String.format(Locale.US, "%.1f", it.gasPressureSensor)

                /**State sensors*/

                statusGasDosThrottleFlFuel.status1.text = getString(R.string.sensors_frag_status_gas_valve_label)
                statusGasDosThrottleFlFuel.status1.setBackgroundColor(if(it.gasBit > 0) Color.GREEN else Color.LTGRAY)

                statusGasDosThrottleFlFuel.status2.text = getString(R.string.sensors_frag_status_throttle_label)
                statusGasDosThrottleFlFuel.status2.setBackgroundColor(if(it.carbBit > 0) Color.GREEN else Color.LTGRAY)

                statusGasDosThrottleFlFuel.status3.text = getString(R.string.sensors_frag_status_fl_fuel_label)
                statusGasDosThrottleFlFuel.status3.setBackgroundColor(if(it.ephhValveBit > 0) Color.GREEN else Color.LTGRAY)

                statusPowerValveStarterAe.status1.text = getString(R.string.sensors_frag_status_power_valve_label)
                statusPowerValveStarterAe.status1.setBackgroundColor(if(it.epmValveBit > 0) Color.GREEN else Color.LTGRAY)
                statusPowerValveStarterAe.status2.text = getString(R.string.sensors_frag_status_starter_blocking_label)
                statusPowerValveStarterAe.status2.setBackgroundColor(if(it.starterBlockBit > 0) Color.GREEN else Color.LTGRAY)
                statusPowerValveStarterAe.status3.text = getString(R.string.sensors_frag_status_ae_label)
                statusPowerValveStarterAe.status3.setBackgroundColor(if(it.accelerationEnrichment > 0) Color.GREEN else Color.LTGRAY)

                statusCoolingFanCheckEngineRevLimFuelCut.status1.text =
                    getString(R.string.sensors_frag_status_cooling_fan_label)
                statusCoolingFanCheckEngineRevLimFuelCut.status1.setBackgroundColor(if(it.coolFanBit > 0) Color.GREEN else Color.LTGRAY)
                statusCoolingFanCheckEngineRevLimFuelCut.status2.text =
                    getString(R.string.sensors_frag_status_check_engine_label)
                statusCoolingFanCheckEngineRevLimFuelCut.status2.setBackgroundColor(if(it.checkEngineBit > 0) Color.GREEN else Color.LTGRAY)
                statusCoolingFanCheckEngineRevLimFuelCut.status3.text =
                    getString(R.string.sensors_frag_status_rev_lim_fuel_cut_label)
                statusCoolingFanCheckEngineRevLimFuelCut.status3.setBackgroundColor(if(it.fc_revlim > 0) Color.GREEN else Color.LTGRAY)

                statusFloodClearSysLockIgnInput.status1.text =
                    getString(R.string.sensors_frag_status_flood_clear_mode_label)
                statusFloodClearSysLockIgnInput.status1.setBackgroundColor(if(it.floodclear > 0) Color.GREEN else Color.LTGRAY)
                statusFloodClearSysLockIgnInput.status2.text =
                    getString(R.string.sensors_frag_status_system_locked_label)
                statusFloodClearSysLockIgnInput.status2.setBackgroundColor(if(it.sys_locked > 0) Color.GREEN else Color.LTGRAY)
                statusFloodClearSysLockIgnInput.status3.text = getString(R.string.sensors_frag_status_input_ign_label)
                statusFloodClearSysLockIgnInput.status3.setBackgroundColor(if(it.ign_i > 0) Color.GREEN else Color.LTGRAY)

                statusCondEpasAfterstrEnr.status1.text = getString(R.string.sensors_frag_status_input_cond_label)
                statusCondEpasAfterstrEnr.status1.setBackgroundColor(if(it.cond_i > 0) Color.GREEN else Color.LTGRAY)
                statusCondEpasAfterstrEnr.status2.text = getString(R.string.sensors_frag_status_input_epas_label)
                statusCondEpasAfterstrEnr.status2.setBackgroundColor(if(it.epas_i > 0) Color.GREEN else Color.LTGRAY)
                statusCondEpasAfterstrEnr.status3.text = getString(R.string.sensors_frag_status_afterstart_enr_label)
                statusCondEpasAfterstrEnr.status3.setBackgroundColor(if(it.aftstr_enr > 0) Color.GREEN else Color.LTGRAY)

                statusClosedLoopReservReserv.status1.text =
                    getString(R.string.sensors_frag_status_iac_closed_loop_label)
                statusClosedLoopReservReserv.status1.setBackgroundColor(if(it.iac_closed_loop > 0) Color.GREEN else Color.LTGRAY)

                statusUni1Uni2Uni3.status1.text = getString(R.string.sensors_frag_status_univ_out_1_label)
                statusUni1Uni2Uni3.status1.setBackgroundColor(if(it.uniOut0Bit > 0) Color.GREEN else Color.LTGRAY)
                statusUni1Uni2Uni3.status2.text = getString(R.string.sensors_frag_status_univ_out_2_label)
                statusUni1Uni2Uni3.status2.setBackgroundColor(if(it.uniOut1Bit > 0) Color.GREEN else Color.LTGRAY)
                statusUni1Uni2Uni3.status3.text = getString(R.string.sensors_frag_status_univ_out_3_label)
                statusUni1Uni2Uni3.status3.setBackgroundColor(if(it.uniOut2Bit > 0) Color.GREEN else Color.LTGRAY)

                statusUni4Uni5Uni6.status1.text = getString(R.string.sensors_frag_status_univ_out_4_label)
                statusUni4Uni5Uni6.status1.setBackgroundColor(if(it.uniOut3Bit > 0) Color.GREEN else Color.LTGRAY)
                statusUni4Uni5Uni6.status2.text = getString(R.string.sensors_frag_status_univ_out_5_label)
                statusUni4Uni5Uni6.status2.setBackgroundColor(if(it.uniOut4Bit > 0) Color.GREEN else Color.LTGRAY)
                statusUni4Uni5Uni6.status3.text = getString(R.string.sensors_frag_status_univ_out_6_label)
                statusUni4Uni5Uni6.status3.setBackgroundColor(if(it.uniOut5Bit > 0) Color.GREEN else Color.LTGRAY)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        mViewModel.sendNewTask(Task.Secu3ReadSensors)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mBinding = null
    }
}
