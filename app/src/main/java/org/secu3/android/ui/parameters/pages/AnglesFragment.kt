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
import kotlinx.coroutines.launch
import org.secu3.android.databinding.FragmentAnglesBinding
import org.secu3.android.models.packets.out.params.AnglesParamPacket
import org.secu3.android.ui.parameters.views.FloatParamView
import org.secu3.android.utils.gone
import org.secu3.android.utils.visible


class AnglesFragment : BaseParamFragment() {

    private lateinit var mBinding: FragmentAnglesBinding

    private var packet: AnglesParamPacket? = null


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mBinding = FragmentAnglesBinding.inflate(inflater, container, false)
        return mBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        lifecycleScope.launch {
            withResumed {
                mViewModel.anglesLiveData.observe(viewLifecycleOwner) {
                    mViewModel.isSendAllowed = false

                    packet = it

                    mBinding.apply {

                        progressBar.gone()
                        params.visible()

                        maxAdvanceAngle.value = it.maxAngle
                        minAdvanceAngle.value = it.minAngle

                        angleDecreaseSpeed.value = it.angleDecSpeed
                        angleIncreaseSpeed.value = it.angleIncSpeed

                        octaneCorrection.value = it.angleCorrection

                        zeroAdvAngle.isChecked = it.zeroAdvAngle > 0

                        ignTimingWhenShifting.value = it.shift_ingtim

                        alwaysUseWorkingModeAngleMap.isChecked = it.alwaysUseIgnitionMap
                        applyManualTimingCorr.isChecked = it.applyManualTimingCorrOnIdl
                        zeroAdvOctaneCorr.isChecked = it.zeroAdvAngleWithCorr
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
            maxAdvanceAngle.addOnValueChangeListener {
                packet?.maxAngle = it
            }
            minAdvanceAngle.addOnValueChangeListener {
                packet?.minAngle = it
            }
            angleDecreaseSpeed.addOnValueChangeListener {
                packet?.angleDecSpeed = it
            }
            angleIncreaseSpeed.addOnValueChangeListener {
                packet?.angleIncSpeed = it
            }
            octaneCorrection.addOnValueChangeListener {
                packet?.angleCorrection = it
            }
            zeroAdvAngle.setOnCheckedChangeListener { _, isChecked ->
                packet?.zeroAdvAngle = if (isChecked) 1 else 0
            }

            ignTimingWhenShifting.addOnValueChangeListener {
                packet?.shift_ingtim = it
            }

            alwaysUseWorkingModeAngleMap.setOnCheckedChangeListener { _, isChecked ->
                packet?.alwaysUseIgnitionMap = isChecked
            }

            applyManualTimingCorr.setOnCheckedChangeListener { _, isChecked ->
                packet?.applyManualTimingCorrOnIdl = isChecked
            }

            zeroAdvOctaneCorr.setOnCheckedChangeListener { _, isChecked ->
                packet?.zeroAdvAngleWithCorr = isChecked
            }

            maxAdvanceAngle.setOnClickListener { floatParamClick(it as FloatParamView) }
            minAdvanceAngle.setOnClickListener { floatParamClick(it as FloatParamView) }
            angleDecreaseSpeed.setOnClickListener { floatParamClick(it as FloatParamView) }
            angleIncreaseSpeed.setOnClickListener { floatParamClick(it as FloatParamView) }
            octaneCorrection.setOnClickListener { floatParamClick(it as FloatParamView) }
            ignTimingWhenShifting.setOnClickListener { floatParamClick(it as FloatParamView) }
        }
    }
}