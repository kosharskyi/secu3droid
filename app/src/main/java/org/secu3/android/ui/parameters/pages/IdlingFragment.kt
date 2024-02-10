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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.withResumed
import androidx.lifecycle.withStarted
import kotlinx.coroutines.launch
import org.secu3.android.databinding.FragmentIdlingBinding
import org.secu3.android.models.packets.out.params.IdlingParamPacket
import org.secu3.android.ui.parameters.views.FloatParamView
import org.secu3.android.ui.parameters.views.IntParamView
import org.secu3.android.utils.gone
import org.secu3.android.utils.visible

class IdlingFragment : BaseParamFragment() {

    private lateinit var mBinding: FragmentIdlingBinding

    private var packet: IdlingParamPacket? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mBinding = FragmentIdlingBinding.inflate(inflater, container, false)
        return mBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        lifecycleScope.launch {
            withResumed {
                mViewModel.idlingLiveData.observe(viewLifecycleOwner) {

                    mViewModel.isSendAllowed = false

                    packet = it

                    mBinding.apply {

                        progressBar.gone()
                        params.visible()

                        positiveRegFactor.value = it.iFac1
                        negativeRegFactor.value = it.iFac2

                        maxRegLimit.value = it.idlregMaxAngle
                        minRegLimit.value = it.idlregMinAngle

                        goalRpm.value = it.idlingRpm
                        rpmDeadBand.value = it.minefr

                        regulatorOnTemp.value = it.idlregTurnOnTemp

                        useRegulator.isChecked = it.useRegulator
                        useRegulatorOnGas.isChecked = it.useRegulatorOnGas
                        proportionalRegulator.isChecked = it.pRegMode
                        useClosedLoop.isChecked = it.useClosedLoop

                        iacAddAfterExit.value = it.idlToRunAdd
                        useThrassmap.isChecked = it.useThrassmap
                        rpmAddOnRun.value = it.rpmOnRunAdd

                        proportionalPositive.value = it.idlRegP0
                        integralPositive.value = it.idlRegI0
                        proportionalNegative.value = it.idlRegP1
                        integralNegative.value = it.idlRegI1

                        transientThreshold1.value = it.coefThrd1
                        transientThreshold2.value = it.coefThrd2

                        integratorRpmLimit.value = it.integratorRpmLim
                        pressureLoadOnIdling.value = it.mapValue

                        minIacPosition.value = it.iacMinPos
                        maxIacPosition.value = it.iacMaxPos

                        iacDeadBand.value = it.iacRegDb

                        useClosedLoopOnGas.isChecked = it.useClosedLoopOnGas
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
            positiveRegFactor.addOnValueChangeListener {
                packet?.iFac1 = it
            }
            negativeRegFactor.addOnValueChangeListener {
                packet?.iFac2 = it
            }
            maxRegLimit.addOnValueChangeListener {
                packet?.idlregMaxAngle = it
            }
            minRegLimit.addOnValueChangeListener {
                packet?.idlregMinAngle = it
            }
            goalRpm.addOnValueChangeListener {
                packet?.idlingRpm = it
            }
            rpmDeadBand.addOnValueChangeListener {
                packet?.minefr = it
            }
            regulatorOnTemp.addOnValueChangeListener {
                packet?.idlregTurnOnTemp = it
            }

            useRegulator.setOnCheckedChangeListener { _, isChecked ->
                packet?.useRegulator = isChecked
            }
            useRegulatorOnGas.setOnCheckedChangeListener { _, isChecked ->
                packet?.useRegulatorOnGas = isChecked
            }
            proportionalRegulator.setOnCheckedChangeListener { _, isChecked ->
                packet?.pRegMode = isChecked
            }
            useClosedLoop.setOnCheckedChangeListener { _, isChecked ->
                packet?.useClosedLoop = isChecked
            }

            iacAddAfterExit.addOnValueChangeListener {
                packet?.idlToRunAdd = it
            }

            useThrassmap.setOnCheckedChangeListener { _, isChecked ->
                packet?.useThrassmap = isChecked
            }

            rpmAddOnRun.addOnValueChangeListener {
                packet?.rpmOnRunAdd = it
            }

            proportionalPositive.addOnValueChangeListener {
                packet?.idlRegP0 = it
            }

            integralPositive.addOnValueChangeListener {
                packet?.idlRegI0 = it
            }
            proportionalNegative.addOnValueChangeListener {
                packet?.idlRegP1 = it
            }

            integralNegative.addOnValueChangeListener {
                packet?.idlRegI1 = it
            }

            transientThreshold1.addOnValueChangeListener {
                packet?.coefThrd1 = it
            }

            transientThreshold2.addOnValueChangeListener {
                packet?.coefThrd2 = it
            }

            integratorRpmLimit.addOnValueChangeListener {
                packet?.integratorRpmLim = it
            }

            pressureLoadOnIdling.addOnValueChangeListener {
                packet?.mapValue = it
            }

            minIacPosition.addOnValueChangeListener {
                packet?.iacMinPos = it
            }

            maxIacPosition.addOnValueChangeListener {
                packet?.iacMaxPos = it
            }

            iacDeadBand.addOnValueChangeListener {
                packet?.iacRegDb = it
            }

            useClosedLoopOnGas.setOnCheckedChangeListener { _, isChecked ->
                packet?.useClosedLoopOnGas = isChecked
            }

            positiveRegFactor.setOnClickListener { floatParamClick(it as FloatParamView) }
            negativeRegFactor.setOnClickListener { floatParamClick(it as FloatParamView) }
            maxRegLimit.setOnClickListener { floatParamClick(it as FloatParamView) }
            minRegLimit.setOnClickListener { floatParamClick(it as FloatParamView) }
            goalRpm.setOnClickListener { intParamClick(it as IntParamView) }
            rpmDeadBand.setOnClickListener { intParamClick(it as IntParamView) }
            regulatorOnTemp.setOnClickListener { floatParamClick(it as FloatParamView) }

            iacAddAfterExit.setOnClickListener { floatParamClick(it as FloatParamView) }
            rpmAddOnRun.setOnClickListener { intParamClick(it as IntParamView) }
            proportionalPositive.setOnClickListener { floatParamClick(it as FloatParamView) }
            integralPositive.setOnClickListener { floatParamClick(it as FloatParamView) }
            proportionalNegative.setOnClickListener { floatParamClick(it as FloatParamView) }
            integralNegative.setOnClickListener { floatParamClick(it as FloatParamView) }
            transientThreshold1.setOnClickListener { floatParamClick(it as FloatParamView) }
            transientThreshold2.setOnClickListener { floatParamClick(it as FloatParamView) }
            integratorRpmLimit.setOnClickListener { intParamClick(it as IntParamView) }
            pressureLoadOnIdling.setOnClickListener { floatParamClick(it as FloatParamView) }
            minIacPosition.setOnClickListener { floatParamClick(it as FloatParamView) }
            maxIacPosition.setOnClickListener { floatParamClick(it as FloatParamView) }
        }
    }

}