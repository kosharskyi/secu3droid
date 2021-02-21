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
import org.secu3.android.databinding.FragmentChokeControlBinding
import org.secu3.android.models.packets.params.ChokeControlParPacket
import org.secu3.android.ui.parameters.ParamsViewModel
import org.secu3.android.ui.parameters.views.FloatParamView
import org.secu3.android.ui.parameters.views.IntParamView


class ChokeControlFragment : BaseParamFragment() {

    private val mViewModel: ParamsViewModel by activityViewModels()
    private lateinit var mBinding: FragmentChokeControlBinding

    private val stepperPulses = listOf("300", "150", "100", "75")

    private var packet: ChokeControlParPacket? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mBinding = FragmentChokeControlBinding.inflate(inflater, container, false)
        return mBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mBinding.freqOfPulses.apply {
            inputType = InputType.TYPE_NULL
            ArrayAdapter(requireContext(), R.layout.list_item, stepperPulses).also {
                setAdapter(it)
            }
        }

        mViewModel.chokeLiveData.observe(viewLifecycleOwner) {

            packet = it

            mBinding.apply {
                numSmSteps.value = it.smSteps
                regulatorFactor.value = it.rpmIf

                crankingMapLastingCold.value = it.corrTime0
                crankingMapLastingHot.value = it.corrTime1

                useClosedLoopRpmRegulator.isChecked = it.useClosedLoopRmpRegulator
                dontUseRpmRegulatorOnGas.isChecked = it.dontUseRpmRegOnGas
                useThrottlePosInChokeInit.isChecked = it.useThrottlePosInChokeInit
                maximumSTEPFrequencyAtInit.isChecked = it.maxSTEPfreqAtInit

                freqOfPulses.setText(stepperPulses[it.smFreq], false)
                timeFromCrankToRun.value = it.injCrankToRunTime
            }

            initViews()
        }
    }

    private fun initViews() {

        mBinding.apply {

            numSmSteps.addOnValueChangeListener {
                packet?.smSteps = it
                packet?.let { it1 -> mViewModel.sendPacket(it1) }
            }

            regulatorFactor.addOnValueChangeListener {
                packet?.rpmIf = it
                packet?.let { it1 -> mViewModel.sendPacket(it1) }
            }
            crankingMapLastingCold.addOnValueChangeListener {
                packet?.corrTime0 = it
                packet?.let { it1 -> mViewModel.sendPacket(it1) }
            }

            crankingMapLastingHot.addOnValueChangeListener {
                packet?.corrTime1 = it
                packet?.let { it1 -> mViewModel.sendPacket(it1) }
            }

            useClosedLoopRpmRegulator.setOnCheckedChangeListener { _, isChecked ->
                packet?.useClosedLoopRmpRegulator = isChecked
                packet?.let { it1 -> mViewModel.sendPacket(it1) }
            }
            dontUseRpmRegulatorOnGas.setOnCheckedChangeListener { _, isChecked ->
                packet?.dontUseRpmRegOnGas = isChecked
                packet?.let { it1 -> mViewModel.sendPacket(it1) }
            }
            useThrottlePosInChokeInit.setOnCheckedChangeListener { _, isChecked ->
                packet?.useThrottlePosInChokeInit = isChecked
                packet?.let { it1 -> mViewModel.sendPacket(it1) }
            }
            maximumSTEPFrequencyAtInit.setOnCheckedChangeListener { _, isChecked ->
                packet?.maxSTEPfreqAtInit = isChecked
                packet?.let { it1 -> mViewModel.sendPacket(it1) }
            }

            freqOfPulses.setOnItemClickListener { _, _, position, _ ->
                packet?.smFreq = position
                packet?.let { it1 -> mViewModel.sendPacket(it1) }
            }

            timeFromCrankToRun.addOnValueChangeListener {
                packet?.injCrankToRunTime = it
                packet?.let { it1 -> mViewModel.sendPacket(it1) }
            }


            numSmSteps.setOnClickListener { intParamClick(it as IntParamView) }
            regulatorFactor.setOnClickListener { floatParamClick(it as FloatParamView) }
            crankingMapLastingCold.setOnClickListener { floatParamClick(it as FloatParamView) }

            crankingMapLastingHot.setOnClickListener { floatParamClick(it as FloatParamView) }
            timeFromCrankToRun.setOnClickListener { floatParamClick(it as FloatParamView) }
        }
    }
}