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
import org.secu3.android.databinding.FragmentFuelInjectionBinding
import org.secu3.android.ui.parameters.ParamsViewModel
import org.secu3.android.utils.gone
import org.secu3.android.utils.visible

class FuelInjectionFragment : BaseParamFragment() {

    private val mViewModel: ParamsViewModel by activityViewModels()
    private lateinit var mBinding: FragmentFuelInjectionBinding

    private val mConfigList: List<String> by lazy {
        resources.getStringArray(R.array.fuel_inject_config_types).toList()
    }

    private val mInjTimingSpecifiesList: List<String> by lazy {
        resources.getStringArray(R.array.inj_timing_specifies).toList()
    }

    private val numOfSquirtsList = listOf("1","2","4")

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

        mViewModel.fuelInjectionLiveData.observe(viewLifecycleOwner) {
            mBinding.apply {
                engineDisplacement.value = it.cylDisp

                injectorFlowRate.value = it.flowRate0

                injectionConfiguration.setText(mConfigList[it.config0], false)
                numOfSquirtsCycle.setText(it.config0Pulses.toString(), false)

                injectorTiming.value = it.timing0
                crankingInjectionTiming.value = it.timingCrk0

                useMapInjectionTiming.isChecked = it.useTimingMap
                injTimingSpecifies.setText(mInjTimingSpecifiesList[it.angleSpec0], false)

                minInjectionPw.value = it.minPw0



                injectorFlowRateG.value = it.flowRate1

                injectionConfigurationG.setText(mConfigList[it.config1], false)
                numOfSquirtsCycleG.setText(it.config1Pulses.toString(), false)

                injectorTimingG.value = it.timing1
                crankingInjectionTimingG.value = it.timingCrk1

                useMapInjectionTimingG.isChecked = it.useTimingMapG
                injTimingSpecifiesG.setText(mInjTimingSpecifiesList[it.angleSpec1], false)

                minInjectionPwG.value = it.minPw1

                pulsesPerLitterOfFuel.value = it.fffConst

                additionalCorrectionsGasEq.isChecked = it.useAdditionalCorrections
                useAirDensityCorrectionMap.isChecked = it.useAirDensity
                diffPressForPwCorrGps.isChecked = it.useDifferentialPressure
                switchBetweenInjectorsRows.isChecked = it.switchSecondInjRow
            }
        }
    }

    private fun initDropdowns() {
        mBinding.injectionConfiguration.apply {
            inputType = InputType.TYPE_NULL
            ArrayAdapter(requireContext(), R.layout.list_item, mConfigList).also {
                setAdapter(it)
            }
        }

        mBinding.numOfSquirtsCycle.apply {
            inputType = InputType.TYPE_NULL
            ArrayAdapter(requireContext(), R.layout.list_item, numOfSquirtsList).also {
                setAdapter(it)
            }
        }

        mBinding.injTimingSpecifies.apply {
            inputType = InputType.TYPE_NULL
            ArrayAdapter(requireContext(), R.layout.list_item, mInjTimingSpecifiesList).also {
                setAdapter(it)
            }
        }
    }
}