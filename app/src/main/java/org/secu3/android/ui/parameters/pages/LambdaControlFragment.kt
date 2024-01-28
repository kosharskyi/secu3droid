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
package org.secu3.android.ui.parameters.pages

import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.withResumed
import kotlinx.coroutines.launch
import org.secu3.android.R
import org.secu3.android.databinding.FragmentLambdaControlBinding
import org.secu3.android.models.packets.out.params.LambdaParamPacket
import org.secu3.android.ui.parameters.views.FloatParamView
import org.secu3.android.ui.parameters.views.IntParamView
import org.secu3.android.utils.gone
import org.secu3.android.utils.visible


class LambdaControlFragment : BaseParamFragment() {

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

        mViewModel.fwInfoPacket?.let {
            if (it.isFuelInjectEnabled.not() && it.isCarbAfrEnabled.not() && it.isGdControlEnabled.not()) {
                mBinding.apply {
                    progressBar.gone()
                    params.gone()
                }
            }
        }

        lifecycleScope.launch {
            withResumed {
                mViewModel.lambdaLiveData.observe(viewLifecycleOwner) {

                    mViewModel.isSendAllowed = false

                    packet = it

                    mBinding.apply {

                        progressBar.gone()
                        params.visible()

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

                        airFlowThresholdForTurningHeatingOff.value = it.aflowThrd.toInt()

                        heatingBeforeCranking.isChecked = it.heatingBeforeCranking

                        lambdaChanel1.isChecked = it.lambdaChanel1
                        lambdaChanel2.isChecked = it.lambdaChanel2
                        lambdaChanel3.isChecked = it.lambdaChanel3
                        lambdaChanel4.isChecked = it.lambdaChanel4
                        lambdaChanel5.isChecked = it.lambdaChanel5
                        lambdaChanel6.isChecked = it.lambdaChanel6
                        lambdaChanel7.isChecked = it.lambdaChanel7
                        lambdaChanel8.isChecked = it.lambdaChanel8

                        mixSensorsValue.isChecked = it.mixSenorsValue
                    }

                    initViews()

                    mViewModel.isSendAllowed = true
                }
            }
        }
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
                packet?.aflowThrd = it.toFloat()
                packet?.let { it1 -> mViewModel.sendPacket(it1) }
            }

            heatingBeforeCranking.setOnCheckedChangeListener { _, isChecked ->
                packet?.heatingBeforeCranking = isChecked
                packet?.let { it1 -> mViewModel.sendPacket(it1) }
            }

            lambdaChanel1.setOnCheckedChangeListener { _, isChecked ->
                packet?.apply {
                    lambdaChanel1 = isChecked
                    mViewModel.sendPacket(this)
                }
            }

            lambdaChanel2.setOnCheckedChangeListener { _, isChecked ->
                packet?.apply {
                    lambdaChanel2 = isChecked
                    mViewModel.sendPacket(this)
                }
            }

            lambdaChanel3.setOnCheckedChangeListener { _, isChecked ->
                packet?.apply {
                    lambdaChanel3 = isChecked
                    mViewModel.sendPacket(this)
                }
            }

            lambdaChanel4.setOnCheckedChangeListener { _, isChecked ->
                packet?.apply {
                    lambdaChanel4 = isChecked
                    mViewModel.sendPacket(this)
                }
            }

            lambdaChanel5.setOnCheckedChangeListener { _, isChecked ->
                packet?.apply {
                    lambdaChanel5 = isChecked
                    mViewModel.sendPacket(this)
                }
            }

            lambdaChanel6.setOnCheckedChangeListener { _, isChecked ->
                packet?.apply {
                    lambdaChanel6 = isChecked
                    mViewModel.sendPacket(this)
                }
            }

            lambdaChanel7.setOnCheckedChangeListener { _, isChecked ->
                packet?.apply {
                    lambdaChanel7 = isChecked
                    mViewModel.sendPacket(this)
                }
            }

            lambdaChanel8.setOnCheckedChangeListener { _, isChecked ->
                packet?.apply {
                    lambdaChanel8 = isChecked
                    mViewModel.sendPacket(this)
                }
            }

            mixSensorsValue.setOnCheckedChangeListener { _, isChecked ->
                packet?.apply {
                    mixSenorsValue = isChecked
                    mViewModel.sendPacket(this)
                }
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