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
            var result = ""
            result += String.format(Locale.US, getString(R.string.raw_status_map_title), it.map)
            result += String.format(Locale.US, getString(R.string.raw_status_voltage_title), it.voltage)
            result += String.format(Locale.US, getString(R.string.raw_status_temperature_title), it.temperature)
            result += String.format(Locale.US, getString(R.string.raw_status_knock_title), it.knockValue)
            result += String.format(Locale.US, getString(R.string.raw_status_tps_title), it.tps)
            result += String.format(Locale.US, getString(R.string.raw_status_addi1_title), it.addI1)
            result += String.format(Locale.US, getString(R.string.raw_status_addi2_title), it.addI2)

            mBinding?.sensorsData?.text = result
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mBinding = null
    }
}