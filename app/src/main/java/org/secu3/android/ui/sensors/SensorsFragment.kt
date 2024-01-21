/*
 *    SecuDroid  - An open source, free manager for SECU-3 engine control unit
 *    Copyright (C) 2024 Vitaliy O. Kosharskyi. Ukraine, Kyiv
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

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.secu3.android.R
import org.secu3.android.databinding.FragmentSensorsBinding
import org.secu3.android.utils.Task

class SensorsFragment : Fragment() {

    private val mViewModel: SensorsViewModel by viewModels( ownerProducer = { requireParentFragment() } )

    private var mBinding: FragmentSensorsBinding? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mBinding = FragmentSensorsBinding.inflate(inflater, container, false)
        return mBinding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mBinding?.apply {
            fab.setOnClickListener { onFabClick() }

            gaugesList.adapter = GaugeAdapter {
                mViewModel.deleteGauge(it)
            }

            indicatorsList.adapter = IndicatorAdapter {
                mViewModel.deleteIndicator(it)
            }
        }


        mViewModel.gaugesLiveData.observe(viewLifecycleOwner) {
            (mBinding?.gaugesList?.adapter as? GaugeAdapter)?.submitList(it)
        }

        mViewModel.indicatorLiveData.observe(viewLifecycleOwner) {
            (mBinding?.indicatorsList?.adapter as? IndicatorAdapter)?.submitList(it)
        }

//        mViewModel.sensorsLiveData.observe(viewLifecycleOwner) {
//            mBinding?.apply {
//
//                distance.title.text = getString(R.string.sensors_frag_distance_label)
//                distance.value.text = String.format(Locale.US, "%.1f", it.distance)
//
//                /**State sensors*/
//
//                statusGasDosThrottleFlFuel.status1.text = getString(R.string.sensors_frag_status_gas_valve_label)
//                statusGasDosThrottleFlFuel.status1.setBackgroundColor(if(it.gasBit > 0) Color.GREEN else Color.LTGRAY)
//
//                statusGasDosThrottleFlFuel.status2.text = getString(R.string.sensors_frag_status_throttle_label)
//                statusGasDosThrottleFlFuel.status2.setBackgroundColor(if(it.carbBit > 0) Color.GREEN else Color.LTGRAY)
//
//                statusGasDosThrottleFlFuel.status3.text = getString(R.string.sensors_frag_status_fl_fuel_label)
//                statusGasDosThrottleFlFuel.status3.setBackgroundColor(if(it.ephhValveBit > 0) Color.GREEN else Color.LTGRAY)
//
//                statusPowerValveStarterAe.status1.text = getString(R.string.sensors_frag_status_power_valve_label)
//                statusPowerValveStarterAe.status1.setBackgroundColor(if(it.epmValveBit > 0) Color.GREEN else Color.LTGRAY)
//                statusPowerValveStarterAe.status2.text = getString(R.string.sensors_frag_status_starter_blocking_label)
//                statusPowerValveStarterAe.status2.setBackgroundColor(if(it.starterBlockBit > 0) Color.GREEN else Color.LTGRAY)
//                statusPowerValveStarterAe.status3.text = getString(R.string.sensors_frag_status_ae_label)
//                statusPowerValveStarterAe.status3.setBackgroundColor(if(it.accelerationEnrichment > 0) Color.GREEN else Color.LTGRAY)
//
//                statusCoolingFanCheckEngineRevLimFuelCut.status1.text =
//                    getString(R.string.sensors_frag_status_cooling_fan_label)
//                statusCoolingFanCheckEngineRevLimFuelCut.status1.setBackgroundColor(if(it.coolFanBit > 0) Color.GREEN else Color.LTGRAY)
//                statusCoolingFanCheckEngineRevLimFuelCut.status2.text =
//                    getString(R.string.sensors_frag_status_check_engine_label)
//                statusCoolingFanCheckEngineRevLimFuelCut.status2.setBackgroundColor(if(it.checkEngineBit > 0) Color.GREEN else Color.LTGRAY)
//                statusCoolingFanCheckEngineRevLimFuelCut.status3.text =
//                    getString(R.string.sensors_frag_status_rev_lim_fuel_cut_label)
//                statusCoolingFanCheckEngineRevLimFuelCut.status3.setBackgroundColor(if(it.fc_revlim > 0) Color.GREEN else Color.LTGRAY)
//
//                statusFloodClearSysLockIgnInput.status1.text =
//                    getString(R.string.sensors_frag_status_flood_clear_mode_label)
//                statusFloodClearSysLockIgnInput.status1.setBackgroundColor(if(it.floodclear > 0) Color.GREEN else Color.LTGRAY)
//                statusFloodClearSysLockIgnInput.status2.text =
//                    getString(R.string.sensors_frag_status_system_locked_label)
//                statusFloodClearSysLockIgnInput.status2.setBackgroundColor(if(it.sys_locked > 0) Color.GREEN else Color.LTGRAY)
//                statusFloodClearSysLockIgnInput.status3.text = getString(R.string.sensors_frag_status_input_ign_label)
//                statusFloodClearSysLockIgnInput.status3.setBackgroundColor(if(it.ign_i > 0) Color.GREEN else Color.LTGRAY)
//
//                statusCondEpasAfterstrEnr.status1.text = getString(R.string.sensors_frag_status_input_cond_label)
//                statusCondEpasAfterstrEnr.status1.setBackgroundColor(if(it.cond_i > 0) Color.GREEN else Color.LTGRAY)
//                statusCondEpasAfterstrEnr.status2.text = getString(R.string.sensors_frag_status_input_epas_label)
//                statusCondEpasAfterstrEnr.status2.setBackgroundColor(if(it.epas_i > 0) Color.GREEN else Color.LTGRAY)
//                statusCondEpasAfterstrEnr.status3.text = getString(R.string.sensors_frag_status_afterstart_enr_label)
//                statusCondEpasAfterstrEnr.status3.setBackgroundColor(if(it.aftstr_enr > 0) Color.GREEN else Color.LTGRAY)
//
//                statusClosedLoopReservReserv.status1.text =
//                    getString(R.string.sensors_frag_status_iac_closed_loop_label)
//                statusClosedLoopReservReserv.status1.setBackgroundColor(if(it.iac_closed_loop > 0) Color.GREEN else Color.LTGRAY)
//
//                statusUni1Uni2Uni3.status1.text = getString(R.string.sensors_frag_status_univ_out_1_label)
//                statusUni1Uni2Uni3.status1.setBackgroundColor(if(it.uniOut0Bit > 0) Color.GREEN else Color.LTGRAY)
//                statusUni1Uni2Uni3.status2.text = getString(R.string.sensors_frag_status_univ_out_2_label)
//                statusUni1Uni2Uni3.status2.setBackgroundColor(if(it.uniOut1Bit > 0) Color.GREEN else Color.LTGRAY)
//                statusUni1Uni2Uni3.status3.text = getString(R.string.sensors_frag_status_univ_out_3_label)
//                statusUni1Uni2Uni3.status3.setBackgroundColor(if(it.uniOut2Bit > 0) Color.GREEN else Color.LTGRAY)
//
//                statusUni4Uni5Uni6.status1.text = getString(R.string.sensors_frag_status_univ_out_4_label)
//                statusUni4Uni5Uni6.status1.setBackgroundColor(if(it.uniOut3Bit > 0) Color.GREEN else Color.LTGRAY)
//                statusUni4Uni5Uni6.status2.text = getString(R.string.sensors_frag_status_univ_out_5_label)
//                statusUni4Uni5Uni6.status2.setBackgroundColor(if(it.uniOut4Bit > 0) Color.GREEN else Color.LTGRAY)
//                statusUni4Uni5Uni6.status3.text = getString(R.string.sensors_frag_status_univ_out_6_label)
//                statusUni4Uni5Uni6.status3.setBackgroundColor(if(it.uniOut5Bit > 0) Color.GREEN else Color.LTGRAY)
//            }
//        }
    }

    private fun onFabClick() {

        val gauges = mViewModel.getGaugesAvaliableToAdd()
        val gaugesNames = gauges.map { it.title }.map { getString(it) }.toTypedArray()

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.select_gauge_to_add))
            .setItems(gaugesNames) { dialog, which ->
                mViewModel.addGauge(gauges[which])
            }.show()
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
