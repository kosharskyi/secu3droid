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
import org.secu3.android.databinding.FragmentFuelCutoffkBinding
import org.secu3.android.models.packets.params.CarburParamPacket
import org.secu3.android.ui.parameters.ParamsViewModel
import org.secu3.android.ui.parameters.views.FloatParamView
import org.secu3.android.ui.parameters.views.IntParamView

class FuelCutoffkFragment : BaseParamFragment() {

    private val mViewModel: ParamsViewModel by activityViewModels()
    private lateinit var mBinding: FragmentFuelCutoffkBinding

    private var packet: CarburParamPacket? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mBinding = FragmentFuelCutoffkBinding.inflate(inflater, container, false)
        return mBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mViewModel.carburLiveData.observe(viewLifecycleOwner) {
            packet = it

            mBinding.apply {
                idleCutoffLowerThrd.value = it.ieLot
                idleCutoffUpperThrd.value = it.ieHit

                idleCutoffLowerThrdGas.value = it.ieLotG
                idleCutoffUpperThrdGas.value = it.ieHitG

                cutoffDelay.value = it.shutoffDelay
                powerValveTurnOnThrd.value = it.feOnThresholds
                tpsThreshold.value = it.tpsThreshold

                inversionOfThrottlePositionSwitch.isChecked = it.carbInvers > 0

                fuelCutMapThreshold.value = it.fuelcutMapThrd
                fuelCutCtsThreshold.value = it.fuelcutCtsThrd

                revLimitingLowerThrd.value = it.revlimLot
                revLimitingUpperThrd.value = it.revlimHit
            }

            initViews()
        }
    }


    private fun initViews() {

        mBinding.apply {
            idleCutoffLowerThrd.addOnValueChangeListener {
                packet?.ieLot = it
                packet?.let { it1 -> mViewModel.sendPacket(it1) }
            }
            idleCutoffUpperThrd.addOnValueChangeListener {
                packet?.ieHit = it
                packet?.let { it1 -> mViewModel.sendPacket(it1) }
            }

            idleCutoffLowerThrdGas.addOnValueChangeListener {
                packet?.ieLotG = it
                packet?.let { it1 -> mViewModel.sendPacket(it1) }
            }
            idleCutoffUpperThrdGas.addOnValueChangeListener {
                packet?.ieHitG = it
                packet?.let { it1 -> mViewModel.sendPacket(it1) }
            }
            cutoffDelay.addOnValueChangeListener {
                packet?.shutoffDelay = it
                packet?.let { it1 -> mViewModel.sendPacket(it1) }
            }
            powerValveTurnOnThrd.addOnValueChangeListener {
                packet?.feOnThresholds = it
                packet?.let { it1 -> mViewModel.sendPacket(it1) }
            }
            tpsThreshold.addOnValueChangeListener {
                packet?.tpsThreshold = it
                packet?.let { it1 -> mViewModel.sendPacket(it1) }
            }

            inversionOfThrottlePositionSwitch.setOnCheckedChangeListener { _, isChecked ->
                packet?.carbInvers = if (isChecked) 1 else 0
                packet?.let { it1 -> mViewModel.sendPacket(it1) }
            }

            fuelCutMapThreshold.addOnValueChangeListener {
                packet?.fuelcutMapThrd = it
                packet?.let { it1 -> mViewModel.sendPacket(it1) }
            }

            fuelCutCtsThreshold.addOnValueChangeListener {
                packet?.fuelcutCtsThrd = it
                packet?.let { it1 -> mViewModel.sendPacket(it1) }
            }

            revLimitingLowerThrd.addOnValueChangeListener {
                packet?.revlimLot = it
                packet?.let { it1 -> mViewModel.sendPacket(it1) }
            }

            revLimitingUpperThrd.addOnValueChangeListener {
                packet?.revlimHit = it
                packet?.let { it1 -> mViewModel.sendPacket(it1) }
            }

            idleCutoffLowerThrd.setOnClickListener { intParamClick(it as IntParamView) }
            idleCutoffUpperThrd.setOnClickListener { intParamClick(it as IntParamView) }
            idleCutoffLowerThrdGas.setOnClickListener { intParamClick(it as IntParamView) }
            idleCutoffUpperThrdGas.setOnClickListener { intParamClick(it as IntParamView) }
            cutoffDelay.setOnClickListener { floatParamClick(it as FloatParamView) }
            powerValveTurnOnThrd.setOnClickListener { floatParamClick(it as FloatParamView) }
            tpsThreshold.setOnClickListener { floatParamClick(it as FloatParamView) }

            fuelCutMapThreshold.setOnClickListener { floatParamClick(it as FloatParamView) }
            fuelCutCtsThreshold.setOnClickListener { floatParamClick(it as FloatParamView) }
            revLimitingLowerThrd.setOnClickListener { intParamClick(it as IntParamView) }
            revLimitingUpperThrd.setOnClickListener { intParamClick(it as IntParamView) }
        }
    }
}