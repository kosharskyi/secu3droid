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
package org.secu3.android.ui.parameters.pages

import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.withResumed
import androidx.lifecycle.withStarted
import kotlinx.coroutines.launch
import org.secu3.android.R
import org.secu3.android.databinding.FragmentFuelInjectionBinding
import org.secu3.android.models.packets.out.params.InjctrParPacket
import org.secu3.android.ui.parameters.pages.injection.FuelInjectionViewModel
import org.secu3.android.ui.parameters.views.FloatParamView
import org.secu3.android.ui.parameters.views.IntParamView
import org.secu3.android.utils.gone
import org.secu3.android.utils.visible

class FuelInjectionFragment : BaseParamFragment() {

    private val mInjViewModel: FuelInjectionViewModel by viewModels()
    private lateinit var mBinding: FragmentFuelInjectionBinding

    private val mInjTimingSpecifiesList: List<String> by lazy {
        resources.getStringArray(R.array.inj_timing_specifies).toList()
    }

    private lateinit var packet: InjctrParPacket

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mBinding = FragmentFuelInjectionBinding.inflate(inflater, container, false)
        return mBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initDropdowns()

        mViewModel.fwInfoLiveData.observe(viewLifecycleOwner) {
            if (it.isFuelInjectEnabled.not()) {
                mBinding.fuelInjectParamGroup.gone()
                mBinding.fuelInjectEmptyText.visible()
            }
        }

        mInjViewModel.sqrnum[0].observe(viewLifecycleOwner) {
            mBinding.numOfSquirtsCycle.apply {
                inputType = InputType.TYPE_NULL
                ArrayAdapter(requireContext(), R.layout.list_item, it.map { it.second }).also {
                    setAdapter(it)
                }

                setText(packet.config0Pulses.toString(), false)
            }
        }

        mInjViewModel.sqrnum[1].observe(viewLifecycleOwner) {
            mBinding.numOfSquirtsCycleG.apply {
                inputType = InputType.TYPE_NULL
                ArrayAdapter(requireContext(), R.layout.list_item, it.map { it.second }).also {
                    setAdapter(it)
                }
                setText(packet.config1Pulses.toString(), false)
            }
        }

        lifecycleScope.launch {
            withResumed {
                mViewModel.fuelInjectionLiveData.observe(viewLifecycleOwner) {

                    mViewModel.isSendAllowed = false

                    packet = it

                    mInjViewModel.generateSquirtsDropDown(packet, 0)
                    mInjViewModel.generateSquirtsDropDown(packet, 1)

                    mBinding.apply {
                        progressBar.gone()
                        params.visible()

                        engineDisplacement.value = it.engineDisp

                        injectorFlowRate.value = it.flowRate[0]

                        val configText = InjctrParPacket.InjConfig.entries.first { injConfig ->
                            injConfig.id == it.config0
                        }.let { injConfig ->
                            getString(injConfig.strId)
                        }
                        injectionConfiguration.setText(configText, false)

                        injectorTiming.value = it.timing[0]
                        crankingInjectionTiming.value = it.timingCrk[0]

                        useMapInjectionTiming.isChecked = it.useTimingMap
                        injTimingSpecifies.setText(mInjTimingSpecifiesList[it.angleSpec0], false)

                        minInjectionPw.value = it.minPw0
                        maxInjectionPw.value = it.injMaxPw[0]



                        injectorFlowRateG.value = it.flowRate[1]

                        val configTextG = InjctrParPacket.InjConfig.entries.first { injConfig ->
                            injConfig.id == it.config1
                        }.let { injConfig ->
                            getString(injConfig.strId)
                        }
                        injectionConfigurationG.setText(configTextG, false)

                        injectorTimingG.value = it.timing[1]
                        crankingInjectionTimingG.value = it.timingCrk[1]

                        useMapInjectionTimingG.isChecked = it.useTimingMapG
                        injTimingSpecifiesG.setText(mInjTimingSpecifiesList[it.angleSpec1], false)

                        minInjectionPwG.value = it.minPw1
                        maxInjectionPwG.value = it.injMaxPw[1]

                        pulsesPerLitterOfFuel.value = it.fffConst

                        additionalCorrectionsGasEq.isChecked = it.useAdditionalCorrections
                        useAirDensityCorrectionMap.isChecked = it.useAirDensity
                        diffPressForPwCorrGps.isChecked = it.useDifferentialPressure
                        switchBetweenInjectorsRows.isChecked = it.switchSecondInjRow
                    }

                    initViews()

                    mViewModel.isSendAllowed = true
                }
            }
        }
    }

    private fun initDropdowns() {
        val injConfigList = InjctrParPacket.InjConfig.entries.map { it.strId }

        mBinding.injectionConfiguration.apply {
            ArrayAdapter(requireContext(), R.layout.list_item, injConfigList).also {
                setAdapter(it)
            }
            inputType = InputType.TYPE_NULL
        }

        mBinding.numOfSquirtsCycle.apply {
            inputType = InputType.TYPE_NULL
        }

        mBinding.injTimingSpecifies.apply {
            inputType = InputType.TYPE_NULL
            ArrayAdapter(requireContext(), R.layout.list_item, mInjTimingSpecifiesList).also {
                setAdapter(it)
            }
        }


        mBinding.injectionConfigurationG.apply {
            ArrayAdapter(requireContext(), R.layout.list_item, injConfigList).also {
                setAdapter(it)
            }
            inputType = InputType.TYPE_NULL
        }

        mBinding.numOfSquirtsCycleG.apply {
            inputType = InputType.TYPE_NULL
        }

        mBinding.injTimingSpecifiesG.apply {
            inputType = InputType.TYPE_NULL
            ArrayAdapter(requireContext(), R.layout.list_item, mInjTimingSpecifiesList).also {
                setAdapter(it)
            }
        }
    }

    private fun initViews() {

        mBinding.apply {

            engineDisplacement.addOnValueChangeListener {
                packet.engineDisp = it
                packet.let { it1 -> mViewModel.sendPacket(it1) }
            }

            injectorFlowRate.addOnValueChangeListener {
                packet.flowRate[0] = it
                packet.let { it1 -> mViewModel.sendPacket(it1) }
            }

            injectionConfiguration.setOnItemClickListener { _, _, position, _ ->
                packet.apply {
                    config0 = InjctrParPacket.InjConfig.entries[position].id
                    mViewModel.sendPacket(this)
                }
            }

            numOfSquirtsCycle.setOnItemClickListener { _, _, position, _ ->
                packet.config0Pulses = mInjViewModel.sqrnum[0].value?.get(position)?.first!!
                packet.let { it1 -> mViewModel.sendPacket(it1) }
            }

            injectorTiming.addOnValueChangeListener {
                packet.timing[0] = it
                packet.let { it1 -> mViewModel.sendPacket(it1) }
            }

            crankingInjectionTiming.addOnValueChangeListener {
                packet.timingCrk[0] = it
                packet.let { it1 -> mViewModel.sendPacket(it1) }
            }

            useMapInjectionTiming.setOnCheckedChangeListener { _, isChecked ->
                packet.useTimingMap = isChecked
                packet.let { it1 -> mViewModel.sendPacket(it1) }
            }

            injTimingSpecifies.setOnItemClickListener { _, _, position, _ ->
                packet.angleSpec0 = position
                packet.let { it1 -> mViewModel.sendPacket(it1) }
            }

            minInjectionPw.addOnValueChangeListener {
                packet.minPw0 = it
                packet.let { it1 -> mViewModel.sendPacket(it1) }
            }

            maxInjectionPw.addOnValueChangeListener {
                packet.apply {
                    injMaxPw[0] = it
                    mViewModel.sendPacket(this)
                }
            }









            injectorFlowRateG.addOnValueChangeListener {
                packet.flowRate[1] = it
                packet.let { it1 -> mViewModel.sendPacket(it1) }
            }

            injectionConfigurationG.setOnItemClickListener { _, _, position, _ ->
                packet.apply {
                    config1 = InjctrParPacket.InjConfig.entries[position].id
                    mViewModel.sendPacket(this)
                }
            }

            numOfSquirtsCycleG.setOnItemClickListener { _, _, position, _ ->
                packet.config1Pulses = mInjViewModel.sqrnum[1].value?.get(position)?.first!!
                packet.let { it1 -> mViewModel.sendPacket(it1) }
            }

            injectorTimingG.addOnValueChangeListener {
                packet.timing[1] = it
                packet.let { it1 -> mViewModel.sendPacket(it1) }
            }

            crankingInjectionTimingG.addOnValueChangeListener {
                packet.timingCrk[1] = it
                packet.let { it1 -> mViewModel.sendPacket(it1) }
            }

            useMapInjectionTimingG.setOnCheckedChangeListener { _, isChecked ->
                packet.useTimingMapG = isChecked
                packet.let { it1 -> mViewModel.sendPacket(it1) }
            }

            injTimingSpecifiesG.setOnItemClickListener { _, _, position, _ ->
                packet.angleSpec1 = position
                packet.let { it1 -> mViewModel.sendPacket(it1) }
            }

            minInjectionPwG.addOnValueChangeListener {
                packet.minPw1 = it
                packet.let { it1 -> mViewModel.sendPacket(it1) }
            }

            maxInjectionPwG.addOnValueChangeListener {
                packet.apply {
                    injMaxPw[1] = it
                    mViewModel.sendPacket(this)
                }
            }





            pulsesPerLitterOfFuel.addOnValueChangeListener {
                packet.fffConst = it
                packet.let { it1 -> mViewModel.sendPacket(it1) }
            }

            additionalCorrectionsGasEq.setOnCheckedChangeListener { _, isChecked ->
                packet.useAdditionalCorrections = isChecked
                packet.let { it1 -> mViewModel.sendPacket(it1) }
            }
            useAirDensityCorrectionMap.setOnCheckedChangeListener { _, isChecked ->
                packet.useAirDensity = isChecked
                packet.let { it1 -> mViewModel.sendPacket(it1) }
            }
            diffPressForPwCorrGps.setOnCheckedChangeListener { _, isChecked ->
                packet.useDifferentialPressure = isChecked
                packet.let { it1 -> mViewModel.sendPacket(it1) }
            }
            switchBetweenInjectorsRows.setOnCheckedChangeListener { _, isChecked ->
                packet.switchSecondInjRow = isChecked
                packet.let { it1 -> mViewModel.sendPacket(it1) }
            }



            engineDisplacement.setOnClickListener { floatParamClick(it as FloatParamView) }
            injectorFlowRate.setOnClickListener { floatParamClick(it as FloatParamView) }
            injectorTiming.setOnClickListener { intParamClick(it as IntParamView) }
            crankingInjectionTiming.setOnClickListener { intParamClick(it as IntParamView) }
            minInjectionPw.setOnClickListener { floatParamClick(it as FloatParamView) }
            maxInjectionPw.setOnClickListener { floatParamClick(it as FloatParamView) }
            injectorFlowRateG.setOnClickListener { floatParamClick(it as FloatParamView) }
            injectorTimingG.setOnClickListener { intParamClick(it as IntParamView) }
            crankingInjectionTimingG.setOnClickListener { intParamClick(it as IntParamView) }
            minInjectionPwG.setOnClickListener { floatParamClick(it as FloatParamView) }
            maxInjectionPwG.setOnClickListener { floatParamClick(it as FloatParamView) }
            pulsesPerLitterOfFuel.setOnClickListener { intParamClick(it as IntParamView) }
        }
    }
}