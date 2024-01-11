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
                rawMap.title.text = "Датчик абсолютного давления, В:"
                rawMap.value.text = String.format(Locale.US, "%.3f", it.map)

                rawVoltage.title.text = "Напряжение бортовой сети, В:"
                rawVoltage.value.text = String.format(Locale.US, "%.3f", it.voltage)

                rawCoolantTemp.title.text = "Датчик температуры ОЖ, В:"
                rawCoolantTemp.value.text = String.format(Locale.US, "%.3f", it.temperature)

                rawKnockLvl.title.text = "Уровень сигнала детонации, В:"
                rawKnockLvl.value.text = String.format(Locale.US, "%.3f", it.knockValue)

                rawThrottle.title.text = "Датчик полож. дрос. заслонки, В:"
                rawThrottle.value.text = String.format(Locale.US, "%.3f", it.tps)

                rawAdd1.title.text = "Вход ADD_I1, В:"
                rawAdd1.value.text = String.format(Locale.US, "%.3f", it.addI1)

                rawAdd2.title.text = "Вход ADD_I2, В:"
                rawAdd2.value.text = String.format(Locale.US, "%.3f", it.addI2)

                rawAdd3.title.text = "Вход ADD_I3, В:"
                rawAdd3.value.text = String.format(Locale.US, "%.3f", it.addI3)

                rawAdd4.title.text = "Вход ADD_I4, В:"
                rawAdd4.value.text = String.format(Locale.US, "%.3f", it.addI4)

                rawAdd5.title.text = "Вход ADD_I5, В:"
                rawAdd5.value.text = String.format(Locale.US, "%.3f", it.addI5)

                rawAdd6.title.text = "Вход ADD_I6, В:"
                rawAdd6.value.text = String.format(Locale.US, "%.3f", it.addI6)

                rawAdd7.title.text = "Вход ADD_I7, В:"
                rawAdd7.value.text = String.format(Locale.US, "%.3f", it.addI7)

                rawAdd8.title.text = "Вход ADD_I1, В:"
                rawAdd8.value.text = String.format(Locale.US, "%.3f", it.addI8)
            }

        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mBinding = null
    }
}