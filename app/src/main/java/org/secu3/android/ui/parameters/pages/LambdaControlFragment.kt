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
import org.secu3.android.databinding.FragmentLambdaControlBinding
import org.secu3.android.models.packets.params.LambdaParamPacket
import org.secu3.android.ui.parameters.ParamsViewModel
import org.secu3.android.ui.parameters.views.FloatParamView
import org.secu3.android.ui.parameters.views.IntParamView
import org.secu3.android.utils.gone
import org.secu3.android.utils.visible


class LambdaControlFragment : BaseParamFragment() {

    private val mViewModel: ParamsViewModel by activityViewModels()
    private lateinit var mBinding: FragmentLambdaControlBinding

    private val sensorTypesList: List<String> by lazy {
        resources.getStringArray(R.array.lambda_sensors_types).toList()
    }

    private var packet: LambdaParamPacket? = null

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

            packet = it

            mBinding.apply {
                sensorType.setText(sensorTypesList[it.senstype], false)
                numberOfStrokesPerStep.value = it.strPerStp
                numberOfMsPerStep.value = it.msPerStp

                sizePositiveCorrectionStep.value = it.stepSizeP
                sizeNegativeCorrectionStep.value = it.stepSizeM

                correctionLimitPositive.value = it.corrLimitP
                correctionLimitNegative.value = it.corrLimitM

                switchPoint.value = it.swtPoint

                ctsActivationThreshold.value = it.tempThrd
                rpmActivationThreshold.value = it.rpmThrd

                activationAfterStartIn.value = it.activDelay
                switchPointDeadband.value = it.deadBand

                determineHeatingUsingVoltage.isChecked = it.determineLambdaHeatingByVoltage
                lambdaCorrectioinOnIdling.isChecked = it.lambdaCorrectionOnIdling

                stoichiomRatioFor2Fuel.value = it.gdStoichval


                heatingTimeWithoutPwmOnCold.value = it.heatingTime0
                heatingTimeWithoutPwmOnHot.value = it.heatingTime1

                coldHotTemperatureThreshold.value = it.temperThrd

                turnOnTimeInPwmMode.value = it.heatingAct

                airFlowThresholdForTurningHeatingOff.value = it.aflowThrd

                heatingBeforeCranking.isChecked = it.heatingBeforeCranking
            }
        }

        initViews()
    }

    private fun initViews() {

        mBinding.apply {

            sensorType.setOnItemClickListener { _, _, position, _ ->
                packet?.senstype = position
                packet?.let { it1 -> mViewModel.sendPacket(it1) }
            }

            numberOfStrokesPerStep.addOnValueChangeListener {
                packet?.strPerStp = it
                packet?.let { it1 -> mViewModel.sendPacket(it1) }
            }

            numberOfMsPerStep.addOnValueChangeListener {
                packet?.msPerStp = it
                packet?.let { it1 -> mViewModel.sendPacket(it1) }
            }

            sizePositiveCorrectionStep.addOnValueChangeListener {
                packet?.stepSizeP = it
                packet?.let { it1 -> mViewModel.sendPacket(it1) }
            }

            sizeNegativeCorrectionStep.addOnValueChangeListener {
                packet?.stepSizeM = it
                packet?.let { it1 -> mViewModel.sendPacket(it1) }
            }

            correctionLimitPositive.addOnValueChangeListener {
                packet?.corrLimitP = it
                packet?.let { it1 -> mViewModel.sendPacket(it1) }
            }

            correctionLimitNegative.addOnValueChangeListener {
                packet?.corrLimitM = it
                packet?.let { it1 -> mViewModel.sendPacket(it1) }
            }

            switchPoint.addOnValueChangeListener {
                packet?.swtPoint = it
                packet?.let { it1 -> mViewModel.sendPacket(it1) }
            }

            ctsActivationThreshold.addOnValueChangeListener {
                packet?.tempThrd = it
                packet?.let { it1 -> mViewModel.sendPacket(it1) }
            }

            rpmActivationThreshold.addOnValueChangeListener {
                packet?.rpmThrd = it
                packet?.let { it1 -> mViewModel.sendPacket(it1) }
            }

            activationAfterStartIn.addOnValueChangeListener {
                packet?.activDelay = it
                packet?.let { it1 -> mViewModel.sendPacket(it1) }
            }

            switchPointDeadband.addOnValueChangeListener {
                packet?.deadBand = it
                packet?.let { it1 -> mViewModel.sendPacket(it1) }
            }

            determineHeatingUsingVoltage.setOnCheckedChangeListener { _, isChecked ->
                packet?.determineLambdaHeatingByVoltage = isChecked
                packet?.let { it1 -> mViewModel.sendPacket(it1) }
            }

            lambdaCorrectioinOnIdling.setOnCheckedChangeListener { _, isChecked ->
                packet?.lambdaCorrectionOnIdling = isChecked
                packet?.let { it1 -> mViewModel.sendPacket(it1) }
            }

            stoichiomRatioFor2Fuel.addOnValueChangeListener {
                packet?.gdStoichval = it
                packet?.let { it1 -> mViewModel.sendPacket(it1) }
            }

            heatingTimeWithoutPwmOnCold.addOnValueChangeListener {
                packet?.heatingTime0 = it
                packet?.let { it1 -> mViewModel.sendPacket(it1) }
            }

            heatingTimeWithoutPwmOnHot.addOnValueChangeListener {
                packet?.heatingTime1 = it
                packet?.let { it1 -> mViewModel.sendPacket(it1) }
            }

            coldHotTemperatureThreshold.addOnValueChangeListener {
                packet?.temperThrd = it
                packet?.let { it1 -> mViewModel.sendPacket(it1) }
            }

            turnOnTimeInPwmMode.addOnValueChangeListener {
                packet?.heatingAct = it
                packet?.let { it1 -> mViewModel.sendPacket(it1) }
            }

            airFlowThresholdForTurningHeatingOff.addOnValueChangeListener {
                packet?.aflowThrd = it
                packet?.let { it1 -> mViewModel.sendPacket(it1) }
            }

            heatingBeforeCranking.setOnCheckedChangeListener { _, isChecked ->
                packet?.heatingBeforeCranking = isChecked
                packet?.let { it1 -> mViewModel.sendPacket(it1) }
            }



            numberOfStrokesPerStep.setOnClickListener { intParamClick(it as IntParamView) }
            numberOfMsPerStep.setOnClickListener { intParamClick(it as IntParamView) }
            sizePositiveCorrectionStep.setOnClickListener { floatParamClick(it as FloatParamView) }
            sizeNegativeCorrectionStep.setOnClickListener { floatParamClick(it as FloatParamView) }
            correctionLimitPositive.setOnClickListener { floatParamClick(it as FloatParamView) }
            correctionLimitNegative.setOnClickListener { floatParamClick(it as FloatParamView) }
            switchPoint.setOnClickListener { floatParamClick(it as FloatParamView) }
            ctsActivationThreshold.setOnClickListener { floatParamClick(it as FloatParamView) }
            rpmActivationThreshold.setOnClickListener { intParamClick(it as IntParamView) }
            activationAfterStartIn.setOnClickListener { intParamClick(it as IntParamView) }


            switchPointDeadband.setOnClickListener { floatParamClick(it as FloatParamView) }
            stoichiomRatioFor2Fuel.setOnClickListener { floatParamClick(it as FloatParamView) }
            heatingTimeWithoutPwmOnCold.setOnClickListener { intParamClick(it as IntParamView) }
            heatingTimeWithoutPwmOnHot.setOnClickListener { intParamClick(it as IntParamView) }
            coldHotTemperatureThreshold.setOnClickListener { intParamClick(it as IntParamView) }
            turnOnTimeInPwmMode.setOnClickListener { floatParamClick(it as FloatParamView) }
            airFlowThresholdForTurningHeatingOff.setOnClickListener { intParamClick(it as IntParamView) }
        }
    }
}