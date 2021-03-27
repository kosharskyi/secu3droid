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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.activityViewModels
import org.secu3.android.R
import org.secu3.android.databinding.FragmentMiscellaneousBinding
import org.secu3.android.models.packets.params.MiscellaneousParamPacket
import org.secu3.android.ui.parameters.ParamsViewModel
import org.secu3.android.ui.parameters.views.FloatParamView
import org.secu3.android.ui.parameters.views.IntParamView


class MiscellaneousFragment : BaseParamFragment() {

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

    private var packet: MiscellaneousParamPacket? = null

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

            packet = it

            mBinding.apply {
                dataTransmitionSpeed.setText(baudRateList[it.uartDivisor], false)
                dataPacketsTransmissionPeriod.value = it.uartPeriodTms

                enableCutoffOfIgnitionCheckbox.isChecked = it.ignCutoff > 0
                enableCutoffOfIgnition.value = it.ignCutoffThrd

                startRelToTdc.value = it.hopStartAng
                duration.value = it.hopDuratAng

                turnOffFuelPumpAfterGas.isChecked = it.offPumpOnGas
                turnOffInjectorsAfterGas.isChecked = it.offInjOnGas
                turnOffInjectorsAfterPetrol.isChecked = it.offInjOnPetrol

                evapStartingAirFlow.value = it.evapAfbegin
                evapEndingAirFlow.value = it.evapAfEnd

                fuelPumpWorkingTime.value = it.fpTimeoutStrt

                pwmfrq0.value = it.pwmFrq0
                pwmfrq1.value = it.pwmFrq1
            }

            initViews()
        }
    }

    private fun initViews() {

        mBinding.apply {

            dataTransmitionSpeed.setOnItemClickListener { _, _, position, _ ->
                packet?.uartDivisor = position
                packet?.let { it1 -> mViewModel.sendPacket(it1) }
            }

            dataPacketsTransmissionPeriod.addOnValueChangeListener {
                packet?.uartPeriodTms = it
                packet?.let { it1 -> mViewModel.sendPacket(it1) }
            }

            enableCutoffOfIgnitionCheckbox.setOnCheckedChangeListener { _, checkedId ->
                packet?.ignCutoff = if (checkedId) 1 else 0
                packet?.let { it1 -> mViewModel.sendPacket(it1) }
            }

            enableCutoffOfIgnition.addOnValueChangeListener {
                packet?.ignCutoffThrd = it
                packet?.let { it1 -> mViewModel.sendPacket(it1) }
            }
            startRelToTdc.addOnValueChangeListener {
                packet?.hopStartAng = it
                packet?.let { it1 -> mViewModel.sendPacket(it1) }
            }

            duration.addOnValueChangeListener {
                packet?.hopDuratAng = it
                packet?.let { it1 -> mViewModel.sendPacket(it1) }
            }

            turnOffFuelPumpAfterGas.setOnCheckedChangeListener { _, isChecked ->
                packet?.offPumpOnGas = isChecked
                packet?.let { it1 -> mViewModel.sendPacket(it1) }
            }
            turnOffInjectorsAfterGas.setOnCheckedChangeListener { _, isChecked ->
                packet?.offInjOnGas = isChecked
                packet?.let { it1 -> mViewModel.sendPacket(it1) }
            }
            turnOffInjectorsAfterPetrol.setOnCheckedChangeListener { _, isChecked ->
                packet?.offInjOnPetrol = isChecked
                packet?.let { it1 -> mViewModel.sendPacket(it1) }
            }

            evapStartingAirFlow.addOnValueChangeListener {
                packet?.evapAfbegin = it
                packet?.let { it1 -> mViewModel.sendPacket(it1) }
            }

            evapEndingAirFlow.addOnValueChangeListener {
                packet?.evapAfEnd = it
                packet?.let { it1 -> mViewModel.sendPacket(it1) }
            }

            fuelPumpWorkingTime.addOnValueChangeListener {
                packet?.fpTimeoutStrt = it
                packet?.let { it1 -> mViewModel.sendPacket(it1) }
            }

            pwmfrq0.addOnValueChangeListener {
                packet?.pwmFrq0 = it
                packet?.let { it1 -> mViewModel.sendPacket(it1) }
            }

            pwmfrq1.addOnValueChangeListener {
                packet?.pwmFrq1 = it
                packet?.let { it1 -> mViewModel.sendPacket(it1) }
            }


            dataPacketsTransmissionPeriod.setOnClickListener { intParamClick(it as IntParamView) }
            enableCutoffOfIgnition.setOnClickListener { intParamClick(it as IntParamView) }
            startRelToTdc.setOnClickListener { intParamClick(it as IntParamView) }

            duration.setOnClickListener { intParamClick(it as IntParamView) }
            evapStartingAirFlow.setOnClickListener { intParamClick(it as IntParamView) }
            evapEndingAirFlow.setOnClickListener { intParamClick(it as IntParamView) }

            fuelPumpWorkingTime.setOnClickListener { floatParamClick(it as FloatParamView) }

            pwmfrq0.setOnClickListener { intParamClick(it as IntParamView) }
            pwmfrq1.setOnClickListener { intParamClick(it as IntParamView) }

        }
    }
}