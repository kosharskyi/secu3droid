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

package org.secu3.android.ui.main

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import org.secu3.android.R
import org.secu3.android.databinding.FragmentSensorsBinding
import java.util.Locale

class SensorsFragment : Fragment() {

    private val mViewModel: SensorsViewModel by viewModels( ownerProducer = { requireParentFragment() } )

    private var mBinding: FragmentSensorsBinding? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mBinding = FragmentSensorsBinding.inflate(inflater, container, false)
        return mBinding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mViewModel.sensorsLiveData.observe(viewLifecycleOwner) {
            var result = ""
            result += String.format(Locale.US, getString(R.string.status_rpm_title), it.rpm)
            result += String.format(Locale.US, getString(R.string.status_map_title), it.map)
            result += String.format(Locale.US, getString(R.string.status_voltage_title), it.voltage)
            result += String.format(Locale.US, getString(R.string.status_temperature_title), it.temperature)
            result += String.format(Locale.US, getString(R.string.status_angle_correction_title), it.currentAngle)
            result += String.format(Locale.US, getString(R.string.status_knock_title), it.knockValue)
            result += String.format(Locale.US, getString(R.string.status_knock_retard_title), it.knockRetard)
            result += String.format(Locale.US, getString(R.string.status_air_flow_title), it.airflow)

            result += String.format(Locale.US, getString(R.string.status_fi_valve_title), it.ephhValveBit)
            result += String.format(Locale.US, getString(R.string.status_carb_status_title), it.carbBit)
            result += String.format(Locale.US, getString(R.string.status_gas_valve_title), it.gasBit)
            result += String.format(Locale.US, getString(R.string.status_power_valve_title), it.epmValveBit)
            result += String.format(Locale.US, getString(R.string.status_ecf_title), it.coolFanBit)
            result += String.format(Locale.US, getString(R.string.status_starter_block_title), it.stBlockBit)

            result += String.format(Locale.US, getString(R.string.status_addi1_voltage_title), it.addI1)
            result += String.format(Locale.US, getString(R.string.status_addi2_voltage_title), it.addI2)
            result += String.format(Locale.US, getString(R.string.status_tps_title), it.tps)
            result += String.format(Locale.US, getString(R.string.status_choke_position_title), it.chokePosition)

            mBinding?.sensorsData?.text = result
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mBinding = null
    }
}