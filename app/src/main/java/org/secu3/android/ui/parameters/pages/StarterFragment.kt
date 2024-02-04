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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import org.secu3.android.databinding.FragmentStarterBinding
import org.secu3.android.models.packets.out.params.StarterParamPacket
import org.secu3.android.ui.parameters.views.FloatParamView
import org.secu3.android.ui.parameters.views.IntParamView
import org.secu3.android.utils.gone
import org.secu3.android.utils.visible

class StarterFragment : BaseParamFragment() {

    private lateinit var mBinding: FragmentStarterBinding

    private var packet: StarterParamPacket? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mBinding = FragmentStarterBinding.inflate(inflater, container, false)
        return mBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mViewModel.starterLiveData.observe(viewLifecycleOwner) {
            mViewModel.isSendAllowed = false

            packet = it

            mBinding.apply {

                progressBar.gone()
                params.visible()

                starterBlockingRpm.value = it.starterOff
                switchCrankMapRpm.value = it.smapAbandon
                timeCrankToRunPositionCold.value = it.crankToRunTime
                timeCrankToRunPositionHot.value = it.injCrankToRun_time1
                afterstartEnrichmentTimePetrol.value = it.injAftstrStroke
                afterstartEnrichmentTimeGas.value = it.injAftStrokes1
                primePulseCold.value = it.injPrimeCold
                primePulseHot.value = it.injPrimeHot
                primePulseDelay.value = it.injPrimeDelay
                floodClearModeThreshold.value = it.injFloodclearTps
                stblStrCountdown.value = it.stblStrCnt
                allowStartOnFloodClearMode.isChecked = it.allowStartOnClearFlood
                limitMaxInjPwOnCranking.isChecked = it.limitMaxInjPwOnCranking
            }

            initViews()

            mViewModel.isSendAllowed = true
        }
    }


    private fun initViews() {

        mBinding.apply {
            starterBlockingRpm.addOnValueChangeListener {
                packet?.starterOff = it
                packet?.let { it1 -> mViewModel.sendPacket(it1) }
            }
            switchCrankMapRpm.addOnValueChangeListener {
                packet?.smapAbandon = it
                packet?.let { it1 -> mViewModel.sendPacket(it1) }
            }
            timeCrankToRunPositionCold.addOnValueChangeListener {
                packet?.crankToRunTime = it
                packet?.let { it1 -> mViewModel.sendPacket(it1) }
            }
            timeCrankToRunPositionHot.addOnValueChangeListener {
                packet?.injCrankToRun_time1 = it
                packet?.let { it1 -> mViewModel.sendPacket(it1) }
            }
            afterstartEnrichmentTimePetrol.addOnValueChangeListener {
                packet?.injAftstrStroke = it
                packet?.let { it1 -> mViewModel.sendPacket(it1) }
            }
            afterstartEnrichmentTimeGas.addOnValueChangeListener {
                packet?.injAftStrokes1 = it
                packet?.let { it1 -> mViewModel.sendPacket(it1) }
            }
            primePulseCold.addOnValueChangeListener {
                packet?.injPrimeCold = it
                packet?.let { it1 -> mViewModel.sendPacket(it1) }
            }
            primePulseHot.addOnValueChangeListener {
                packet?.injPrimeHot = it
                packet?.let { it1 -> mViewModel.sendPacket(it1) }
            }
            primePulseDelay.addOnValueChangeListener {
                packet?.injPrimeDelay = it
                packet?.let { it1 -> mViewModel.sendPacket(it1) }
            }
            floodClearModeThreshold.addOnValueChangeListener {
                packet?.injFloodclearTps = it
                packet?.let { it1 -> mViewModel.sendPacket(it1) }
            }
            stblStrCountdown.addOnValueChangeListener {
                packet?.apply {
                    stblStrCnt = it
                    mViewModel.sendPacket(this)
                }
            }
            allowStartOnFloodClearMode.setOnCheckedChangeListener { _, isChecked ->
                packet?.apply {
                    allowStartOnClearFlood = isChecked
                    mViewModel.sendPacket(this)
                }
            }
            limitMaxInjPwOnCranking.setOnCheckedChangeListener { _, isChecked ->
                packet?.apply {
                    limitMaxInjPwOnCranking = isChecked
                    mViewModel.sendPacket(this)
                }
            }

            starterBlockingRpm.setOnClickListener { intParamClick(it as IntParamView) }
            switchCrankMapRpm.setOnClickListener { intParamClick(it as IntParamView) }
            timeCrankToRunPositionCold.setOnClickListener { floatParamClick(it as FloatParamView) }
            timeCrankToRunPositionHot.setOnClickListener { floatParamClick(it as FloatParamView) }
            afterstartEnrichmentTimePetrol.setOnClickListener { intParamClick(it as IntParamView) }
            afterstartEnrichmentTimeGas.setOnClickListener { intParamClick(it as IntParamView) }
            primePulseCold.setOnClickListener { floatParamClick(it as FloatParamView) }
            primePulseHot.setOnClickListener { floatParamClick(it as FloatParamView) }
            primePulseDelay.setOnClickListener { floatParamClick(it as FloatParamView) }
            floodClearModeThreshold.setOnClickListener { floatParamClick(it as FloatParamView) }
            stblStrCountdown.setOnClickListener { intParamClick(it as IntParamView) }
        }
    }
}