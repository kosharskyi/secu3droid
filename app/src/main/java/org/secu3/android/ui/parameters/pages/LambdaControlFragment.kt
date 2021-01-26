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
import org.secu3.android.databinding.FragmentLambdaControlBinding
import org.secu3.android.ui.parameters.ParamsViewModel
import org.secu3.android.utils.gone
import org.secu3.android.utils.visible


class LambdaControlFragment : Fragment() {

    private val mViewModel: ParamsViewModel by activityViewModels()
    private lateinit var mBinding: FragmentLambdaControlBinding

    private val sensorTypesList: List<String> by lazy {
        resources.getStringArray(R.array.lambda_sensors_types).toList()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mBinding = FragmentLambdaControlBinding.inflate(inflater, container, false)
        return mBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mBinding.sensorType.inputType = InputType.TYPE_NULL
        ArrayAdapter(requireContext(), R.layout.list_item, sensorTypesList).also {
            mBinding.sensorType.setAdapter(it)
        }

        mViewModel.fwInfoLiveData.observe(viewLifecycleOwner) {
            if (it.isFuelInjectEnabled.not() && it.isCarbAfrEnabled.not() && it.isGdControlEnabled.not()) {
                mBinding.lambdaParamGroup.gone()
                mBinding.lambdaEmptyText.visible()
            }
        }

        mViewModel.lambdaLiveData.observe(viewLifecycleOwner) {
            mBinding.apply {
                sensorType.setText(sensorTypesList[it.senstype], false)
                numberOfStrokesPerStep.text = it.strPerStp.toString()
                numberOfMsPerStep.text = it.msPerStp.toString()

                sizePositiveCorrectionStep.text = "%.2f".format(it.stepSizeP)
                sizeNegativeCorrectionStep.text = "%.2f".format(it.stepSizeM)

                correctionLimitPositive.text = "%.2f".format(it.corrLimitP)
                correctionLimitNegative.text = "%.2f".format(it.corrLimitM)

                switchPoint.text = it.swtPoint.toString()

                ctsActivationThreshold.text = it.tempThrd.toString()
                rpmActivationThreshold.text = it.rpmThrd.toString()

                activationAfterStartIn.text = it.activDelay.toString()
                switchPointDeadband.text = it.deadBand.toString()

                determineHeatingUsingVoltage.isChecked = it.determineLambdaHeatingByVoltage
                lambdaCorrectioinOnIdling.isChecked = it.lambdaCorrectionOnIdling

                stoichiomRatioFor2Fuel.text = it.gdStoichval.toString()


                heatingTimeWithoutPwmOnCold.text = it.heatingTime0.toString()
                heatingTimeWithoutPwmOnHot.text = it.heatingTime1.toString()

                coldHotTemperatureThreshold.text = it.temperThrd.toString()

                turnOnTimeInPwmMode.text = it.heatingAct.toString()

                airFlowThresholdForTurningHeatingOff.text = it.aflowThrd.toString()

                heatingBeforeCranking.isChecked = it.heatingBeforeCranking
            }
        }
    }
}