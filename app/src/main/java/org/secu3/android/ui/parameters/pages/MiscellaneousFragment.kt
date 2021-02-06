/* SecuDroid  - An open source, free manager for SECU-3 engine control unit
   Copyright (C) 2020 Vitaliy O. Kosharskiy. Ukraine, Kharkiv

   SECU-3  - An open source, free engine control unit
   Copyright (C) 2007 Alexey A. Shabelnikov. Ukraine, Kyiv

   This program is free software: you can redistribute it and/or modify
   it under the terms of the GNU General Public License as published by
   the Free Software Foundation, either version 3 of the License, or
   (at your option) any later version.

   This program is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.

   You should have received a copy of the GNU General Public License
   along with this program.  If not, see <http://www.gnu.org/licenses/>.

   contacts:
              http://secu-3.org
              email: vetalkosharskiy@gmail.com
*/
package org.secu3.android.ui.parameters.pages

import android.os.Bundle
import android.text.InputType
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.activityViewModels
import org.secu3.android.R
import org.secu3.android.databinding.FragmentMiscellaneousBinding
import org.secu3.android.ui.parameters.ParamsViewModel


class MiscellaneousFragment : Fragment() {

    private val mViewModel: ParamsViewModel by activityViewModels()
    private lateinit var mBinding: FragmentMiscellaneousBinding

    private val baudRateList = mapOf(
        0x0411 to "2400",
        0x0208 to "4800",
        0x0103 to "9600",
        0x00AD to "14400",
        0x0081 to "19200",
        0x0056 to "28800",
        0x0040 to "38400",
        0x002A to "57600",
        0x0015 to "115200",
        0x0009 to "250000"
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mBinding = FragmentMiscellaneousBinding.inflate(inflater, container, false)
        return mBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mBinding.dataTransmitionSpeed.inputType = InputType.TYPE_NULL
        ArrayAdapter(requireContext(), R.layout.list_item, baudRateList.values.toList()).also {
            mBinding.dataTransmitionSpeed.setAdapter(it)
        }

        mViewModel.miscellaneousLiveData.observe(viewLifecycleOwner) {
            mBinding.apply {
                dataTransmitionSpeed.setText(baudRateList[it.uartDivisor], false)
                dataPacketsTransmissionPeriod.value = it.uartPeriodTms

                enableCutoffOfIgnitionCheckbox.isChecked = it.ignCutoff > 0
                enableCutoffOfIgnition.value = it.ignCutoffThrd

                startRelToTdc.value = it.hopStartCogs
                duration.value = it.hopDuratCogs

                turnOffFuelPumpAfterGas.isChecked = it.offPumpOnGas
                turnOffInjectorsAfterGas.isChecked = it.offInjOnGas
                turnOffInjectorsAfterPetrol.isChecked = it.offInjOnPetrol

                evapStartingAirFlow.value = it.evapAfbegin
                evapEndingAirFlow.value = it.evapAfEnd

                fuelPumpWorkingTime.value = it.fpTimeoutStrt
            }
        }
    }
}