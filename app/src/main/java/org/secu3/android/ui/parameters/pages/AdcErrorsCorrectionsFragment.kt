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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import org.secu3.android.databinding.FragmentAdcErrorsCorrectionsBinding
import org.secu3.android.models.packets.params.AdcCorrectionsParamPacket
import org.secu3.android.ui.parameters.ParamsViewModel
import org.secu3.android.ui.parameters.views.FloatParamView

class AdcErrorsCorrectionsFragment : BaseParamFragment() {

    private val mViewModel: ParamsViewModel by activityViewModels()
    private lateinit var mBinding: FragmentAdcErrorsCorrectionsBinding

    private var packet: AdcCorrectionsParamPacket? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mBinding = FragmentAdcErrorsCorrectionsBinding.inflate(inflater)
        return mBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mViewModel.adcCorrectionsLiveData.observe(viewLifecycleOwner) {

            packet = it

            mBinding.apply {
                mapFactor.value = it.mapAdcFactor
                mapCorrection.value = it.mapAdcCorrection

                voltageFactor.value = it.ubatAdcFactor
                voltageCorrection.value = it.ubatAdcCorrection

                ctsFactor.value = it.tempAdcFactor
                ctsCorrection.value = it.tempAdcCorrection

                tpsFactor.value = it.tpsAdcFactor
                tpsCorrection.value = it.tpsAdcCorrection

                add1Factor.value = it.ai1AdcFactor
                add1Correction.value = it.ai1AdcCorrection

                add2Factor.value = it.ai2AdcFactor
                add2Correction.value = it.ai2AdcCorrection

                add3Factor.value = it.ai3AdcFactor
                add3Correction.value = it.ai3AdcCorrection

                add4Factor.value = it.ai4AdcFactor
                add4Correction.value = it.ai4AdcCorrection

                add5Factor.value = it.ai5AdcFactor
                add5Correction.value = it.ai5AdcCorrection

                add6Factor.value = it.ai6AdcFactor
                add6Correction.value = it.ai6AdcCorrection

                add7Factor.value = it.ai7AdcFactor
                add7Correction.value = it.ai7AdcCorrection

                add8Factor.value = it.ai8AdcFactor
                add8Correction.value = it.ai8AdcCorrection

            }

            initViews()
        }
    }

    private fun initViews() {

        mBinding.apply {
            mapFactor.addOnValueChangeListener {
                packet?.mapAdcFactor = it
                packet?.let { it1 -> mViewModel.sendPacket(it1) }
            }
            mapCorrection.addOnValueChangeListener {
                packet?.mapAdcCorrection = it
                packet?.let { it1 -> mViewModel.sendPacket(it1) }
            }

            voltageFactor.addOnValueChangeListener {
                packet?.ubatAdcFactor = it
                packet?.let { it1 -> mViewModel.sendPacket(it1) }
            }
            voltageCorrection.addOnValueChangeListener {
                packet?.ubatAdcCorrection = it
                packet?.let { it1 -> mViewModel.sendPacket(it1) }
            }
            ctsFactor.addOnValueChangeListener {
                packet?.tempAdcFactor = it
                packet?.let { it1 -> mViewModel.sendPacket(it1) }
            }
            ctsCorrection.addOnValueChangeListener {
                packet?.tempAdcCorrection = it
                packet?.let { it1 -> mViewModel.sendPacket(it1) }
            }
            tpsFactor.addOnValueChangeListener {
                packet?.tpsAdcFactor = it
                packet?.let { it1 -> mViewModel.sendPacket(it1) }
            }

            tpsCorrection.addOnValueChangeListener {
                packet?.tpsAdcCorrection = it
                packet?.let { it1 -> mViewModel.sendPacket(it1) }
            }

            add1Factor.addOnValueChangeListener {
                packet?.ai1AdcFactor = it
                packet?.let { it1 -> mViewModel.sendPacket(it1) }
            }

            add1Correction.addOnValueChangeListener {
                packet?.ai1AdcCorrection = it
                packet?.let { it1 -> mViewModel.sendPacket(it1) }
            }

            add2Factor.addOnValueChangeListener {
                packet?.ai2AdcFactor = it
                packet?.let { it1 -> mViewModel.sendPacket(it1) }
            }
            add2Correction.addOnValueChangeListener {
                packet?.ai2AdcCorrection = it
                packet?.let { it1 -> mViewModel.sendPacket(it1) }
            }

            add3Factor.addOnValueChangeListener {
                packet?.ai3AdcFactor = it
                packet?.let { it1 -> mViewModel.sendPacket(it1) }
            }
            add3Correction.addOnValueChangeListener {
                packet?.ai3AdcCorrection = it
                packet?.let { it1 -> mViewModel.sendPacket(it1) }
            }

            add4Factor.addOnValueChangeListener {
                packet?.ai4AdcFactor = it
                packet?.let { it1 -> mViewModel.sendPacket(it1) }
            }
            add4Correction.addOnValueChangeListener {
                packet?.ai4AdcCorrection = it
                packet?.let { it1 -> mViewModel.sendPacket(it1) }
            }

            add5Factor.addOnValueChangeListener {
                packet?.ai5AdcFactor = it
                packet?.let { it1 -> mViewModel.sendPacket(it1) }
            }
            add5Correction.addOnValueChangeListener {
                packet?.ai5AdcCorrection = it
                packet?.let { it1 -> mViewModel.sendPacket(it1) }
            }

            add6Factor.addOnValueChangeListener {
                packet?.ai6AdcFactor = it
                packet?.let { it1 -> mViewModel.sendPacket(it1) }
            }
            add6Correction.addOnValueChangeListener {
                packet?.ai6AdcCorrection = it
                packet?.let { it1 -> mViewModel.sendPacket(it1) }
            }

            add7Factor.addOnValueChangeListener {
                packet?.ai7AdcFactor = it
                packet?.let { it1 -> mViewModel.sendPacket(it1) }
            }
            add7Correction.addOnValueChangeListener {
                packet?.ai7AdcCorrection = it
                packet?.let { it1 -> mViewModel.sendPacket(it1) }
            }

            add8Factor.addOnValueChangeListener {
                packet?.ai8AdcFactor = it
                packet?.let { it1 -> mViewModel.sendPacket(it1) }
            }
            add8Correction.addOnValueChangeListener {
                packet?.ai8AdcCorrection = it
                packet?.let { it1 -> mViewModel.sendPacket(it1) }
            }

            mapFactor.setOnClickListener { floatParamClick(it as FloatParamView) }
            mapCorrection.setOnClickListener { floatParamClick(it as FloatParamView) }
            voltageFactor.setOnClickListener { floatParamClick(it as FloatParamView) }
            voltageCorrection.setOnClickListener { floatParamClick(it as FloatParamView) }
            ctsFactor.setOnClickListener { floatParamClick(it as FloatParamView) }
            ctsCorrection.setOnClickListener { floatParamClick(it as FloatParamView) }
            tpsFactor.setOnClickListener { floatParamClick(it as FloatParamView) }
            tpsCorrection.setOnClickListener { floatParamClick(it as FloatParamView) }

            add1Factor.setOnClickListener { floatParamClick(it as FloatParamView) }
            add1Correction.setOnClickListener { floatParamClick(it as FloatParamView) }
            add2Factor.setOnClickListener { floatParamClick(it as FloatParamView) }
            add2Correction.setOnClickListener { floatParamClick(it as FloatParamView) }
            add3Factor.setOnClickListener { floatParamClick(it as FloatParamView) }
            add3Correction.setOnClickListener { floatParamClick(it as FloatParamView) }
            add4Factor.setOnClickListener { floatParamClick(it as FloatParamView) }
            add4Correction.setOnClickListener { floatParamClick(it as FloatParamView) }
            add5Factor.setOnClickListener { floatParamClick(it as FloatParamView) }
            add5Correction.setOnClickListener { floatParamClick(it as FloatParamView) }
            add6Factor.setOnClickListener { floatParamClick(it as FloatParamView) }
            add6Correction.setOnClickListener { floatParamClick(it as FloatParamView) }
            add7Factor.setOnClickListener { floatParamClick(it as FloatParamView) }
            add7Correction.setOnClickListener { floatParamClick(it as FloatParamView) }
            add8Factor.setOnClickListener { floatParamClick(it as FloatParamView) }
            add8Correction.setOnClickListener { floatParamClick(it as FloatParamView) }
        }
    }
}