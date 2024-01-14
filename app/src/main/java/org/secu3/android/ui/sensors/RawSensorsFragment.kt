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
import org.secu3.android.R
import org.secu3.android.databinding.FragmentRawSensorsBinding
import java.util.Locale


class RawSensorsFragment : Fragment() {

    private val mViewModel: SensorsViewModel by viewModels( ownerProducer = { requireParentFragment() } )

    private var mBinding: FragmentRawSensorsBinding? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mBinding = FragmentRawSensorsBinding.inflate(inflater, container, false)
        return mBinding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mViewModel.rawSensorsLiveData.observe(viewLifecycleOwner) {
            mBinding?.apply {
                rawMap.title.text = getString(R.string.raw_sensors_frag_map_sensor_label)
                rawMap.value.text = String.format(Locale.US, "%.3f", it.map)

                rawVoltage.title.text = getString(R.string.raw_sensors_frag_voltage_label)
                rawVoltage.value.text = String.format(Locale.US, "%.3f", it.voltage)

                rawCoolantTemp.title.text = getString(R.string.raw_sensors_frag_coolant_sensor_label)
                rawCoolantTemp.value.text = String.format(Locale.US, "%.3f", it.temperature)

                rawKnockLvl.title.text = getString(R.string.raw_sensors_frag_knock_signal_lavel_label)
                rawKnockLvl.value.text = String.format(Locale.US, "%.3f", it.knockValue)

                rawThrottle.title.text = getString(R.string.raw_sensors_frag_throttle_position_sensor_label)
                rawThrottle.value.text = String.format(Locale.US, "%.3f", it.tps)

                rawAdd1.title.text = getString(R.string.raw_sensors_frag_add_io1_input_label)
                rawAdd1.value.text = String.format(Locale.US, "%.3f", it.addI1)

                rawAdd2.title.text = getString(R.string.raw_sensors_frag_add_io2_input_label)
                rawAdd2.value.text = String.format(Locale.US, "%.3f", it.addI2)

                rawAdd3.title.text = getString(R.string.raw_sensors_frag_add_io3_input_label)
                rawAdd3.value.text = String.format(Locale.US, "%.3f", it.addI3)

                rawAdd4.title.text = getString(R.string.raw_sensors_frag_add_io4_input_label)
                rawAdd4.value.text = String.format(Locale.US, "%.3f", it.addI4)

                rawAdd5.title.text = getString(R.string.raw_sensors_frag_add_io5_input_label)
                rawAdd5.value.text = String.format(Locale.US, "%.3f", it.addI5)

                rawAdd6.title.text = getString(R.string.raw_sensors_frag_add_io6_input_label)
                rawAdd6.value.text = String.format(Locale.US, "%.3f", it.addI6)

                rawAdd7.title.text = getString(R.string.raw_sensors_frag_add_io7_input_label)
                rawAdd7.value.text = String.format(Locale.US, "%.3f", it.addI7)

                rawAdd8.title.text = getString(R.string.raw_sensors_frag_add_io8_input_label)
                rawAdd8.value.text = String.format(Locale.US, "%.3f", it.addI8)
            }

        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mBinding = null
    }
}