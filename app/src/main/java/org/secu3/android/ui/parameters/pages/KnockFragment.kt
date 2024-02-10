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
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.withResumed
import androidx.lifecycle.withStarted
import kotlinx.coroutines.launch
import org.secu3.android.R
import org.secu3.android.databinding.FragmentKnockBinding
import org.secu3.android.models.packets.out.params.KnockParamPacket
import org.secu3.android.ui.parameters.views.FloatParamView
import org.secu3.android.ui.parameters.views.IntParamView
import org.secu3.android.utils.gone
import org.secu3.android.utils.visible


class KnockFragment : BaseParamFragment() {

    private lateinit var mBinding: FragmentKnockBinding

    private var packet: KnockParamPacket? = null

    private val bpfList: List<String> by lazy {
        resources.getStringArray(R.array.knock_filter_frequencies).toList()
    }

    private val integrationTimeConstatList: List<String> by lazy {
        resources.getStringArray(R.array.knock_integration_timeouts).toList()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mBinding = FragmentKnockBinding.inflate(inflater, container, false)
        return mBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mBinding.bpfFrequency.apply {
            inputType = InputType.TYPE_NULL
            ArrayAdapter(requireContext(), R.layout.list_item, bpfList).also {
                setAdapter(it)
            }
        }

        mBinding.integrationTimeConstant.apply {
            inputType = InputType.TYPE_NULL
            ArrayAdapter(requireContext(), R.layout.list_item, integrationTimeConstatList).also {
                setAdapter(it)
            }
        }

        lifecycleScope.launch {
            withResumed {
                mViewModel.knockLiveData.observe(viewLifecycleOwner) {

                    mViewModel.isSendAllowed = false

                    packet = it

                    mBinding.apply {
                        progressBar.gone()
                        params.visible()

                        enableSensor.isChecked = it.useKnockChannel > 0
                        phaseWindowBegin.value = it.kWndBeginAngle
                        phaseWindowEnd.value = it.kWndEndAngle

                        bpfFrequency.setText(bpfList[it.bpfFrequency], false)
                        integrationTimeConstant.setText(integrationTimeConstatList[it.intTimeCost], false)

                        angleDisplacementStep.value = it.retardStep
                        angleRecoveryStep.value = it.advanceStep
                        maxAngleDisplacement.value = it.maxRetard

                        knockThreshold.value = it.threshold
                        angleRecoveryDelay.value = it.recoveryDelay

                        knockCh1.isChecked = it.isKnockChanelSelected(0)
                        knockCh2.isChecked = it.isKnockChanelSelected(1)
                        knockCh3.isChecked = it.isKnockChanelSelected(2)
                        knockCh4.isChecked = it.isKnockChanelSelected(3)
                        knockCh5.isChecked = it.isKnockChanelSelected(4)
                        knockCh6.isChecked = it.isKnockChanelSelected(5)
                        knockCh7.isChecked = it.isKnockChanelSelected(6)
                        knockCh8.isChecked = it.isKnockChanelSelected(7)

                        knockControlThreshold.value = it.knkctlThrd
                    }

                    initViews()

                    mViewModel.isSendAllowed = true
                }

                mViewModel.savePacketLiveData.observe(viewLifecycleOwner) { isSendClicked ->
                    if (isSendClicked.not()) {
                        return@observe
                    }

                    if (isResumed.not()) {
                        return@observe
                    }

                    packet?.let {
                        mViewModel.savePacket(false)
                        mViewModel.sendPacket(it)
                    }
                }
            }
        }
    }

    private fun initViews() {

        mBinding.apply {

            enableSensor.setOnCheckedChangeListener { _, checkedId ->
                packet?.useKnockChannel = if (checkedId) 1 else 0
            }

            phaseWindowBegin.addOnValueChangeListener {
                packet?.kWndBeginAngle = it
            }
            phaseWindowEnd.addOnValueChangeListener {
                packet?.kWndEndAngle = it
            }

            bpfFrequency.setOnItemClickListener { _, _, position, _ ->
                packet?.bpfFrequency = position
            }
            integrationTimeConstant.setOnItemClickListener { _, _, position, _ ->
                packet?.intTimeCost = position
            }

            angleDisplacementStep.addOnValueChangeListener {
                packet?.retardStep = it
            }
            angleRecoveryStep.addOnValueChangeListener {
                packet?.advanceStep = it
            }
            maxAngleDisplacement.addOnValueChangeListener {
                packet?.maxRetard = it
            }

            knockThreshold.addOnValueChangeListener {
                packet?.threshold = it
            }

            angleRecoveryDelay.addOnValueChangeListener {
                packet?.recoveryDelay = it
            }

            knockCh1.setOnCheckedChangeListener { _, isChecked ->
                packet?.selectKnockChanel(0, isChecked)
            }

            knockCh2.setOnCheckedChangeListener { _, isChecked ->
                packet?.selectKnockChanel(1, isChecked)
            }

            knockCh3.setOnCheckedChangeListener { _, isChecked ->
                packet?.selectKnockChanel(2, isChecked)
            }

            knockCh4.setOnCheckedChangeListener { _, isChecked ->
                packet?.selectKnockChanel(3, isChecked)
            }

            knockCh5.setOnCheckedChangeListener { _, isChecked ->
                packet?.selectKnockChanel(4, isChecked)
            }

            knockCh6.setOnCheckedChangeListener { _, isChecked ->
                packet?.selectKnockChanel(5, isChecked)
            }

            knockCh7.setOnCheckedChangeListener { _, isChecked ->
                packet?.selectKnockChanel(6, isChecked)
            }

            knockCh8.setOnCheckedChangeListener { _, isChecked ->
                packet?.selectKnockChanel(7, isChecked)
            }

            knockControlThreshold.addOnValueChangeListener {
                packet?.knkctlThrd = it
            }


            phaseWindowBegin.setOnClickListener { floatParamClick(it as FloatParamView) }
            phaseWindowEnd.setOnClickListener { floatParamClick(it as FloatParamView) }

            angleDisplacementStep.setOnClickListener { floatParamClick(it as FloatParamView) }
            angleRecoveryStep.setOnClickListener { floatParamClick(it as FloatParamView) }
            maxAngleDisplacement.setOnClickListener { floatParamClick(it as FloatParamView) }

            knockThreshold.setOnClickListener { floatParamClick(it as FloatParamView) }
            angleRecoveryDelay.setOnClickListener { intParamClick(it as IntParamView) }
            knockControlThreshold.setOnClickListener { floatParamClick(it as FloatParamView) }
        }
    }
}