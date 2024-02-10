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
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.withResumed
import androidx.lifecycle.withStarted
import kotlinx.coroutines.launch
import org.secu3.android.R
import org.secu3.android.databinding.FragmentFuelCutoffkBinding
import org.secu3.android.models.packets.out.params.CarburParamPacket
import org.secu3.android.ui.parameters.views.FloatParamView
import org.secu3.android.ui.parameters.views.IntParamView
import org.secu3.android.utils.gone
import org.secu3.android.utils.visible

class FuelCutoffkFragment : BaseParamFragment() {

    private val uniOutItems = mapOf(1 to "1", 2 to "2", 3 to "3", 4 to "4", 5 to "5", 6 to "6", 15 to "no")

    private lateinit var mBinding: FragmentFuelCutoffkBinding

    private var packet: CarburParamPacket? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mBinding = FragmentFuelCutoffkBinding.inflate(inflater, container, false)
        return mBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mBinding.apply {
            val adapter = ArrayAdapter(requireContext(), R.layout.list_item, uniOutItems.values.toList())

            useUnioutCond.inputType = InputType.TYPE_NULL
            useUnioutCond.setAdapter(adapter)

            useUnioutCondForIgnCutoff.inputType = InputType.TYPE_NULL
            useUnioutCondForIgnCutoff.setAdapter(adapter)
        }

        lifecycleScope.launch {
            withResumed {

                mViewModel.carburLiveData.observe(viewLifecycleOwner) {

                    mViewModel.isSendAllowed = false

                    packet = it

                    mBinding.apply {

                        progressBar.gone()
                        params.visible()

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

                        useUnioutCond.setText(uniOutItems[it.fuelcut_uni], false)
                        useUnioutCondForIgnCutoff.setText(uniOutItems[it.igncut_uni], false)

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
            idleCutoffLowerThrd.addOnValueChangeListener {
                packet?.ieLot = it
            }
            idleCutoffUpperThrd.addOnValueChangeListener {
                packet?.ieHit = it
            }

            idleCutoffLowerThrdGas.addOnValueChangeListener {
                packet?.ieLotG = it
            }
            idleCutoffUpperThrdGas.addOnValueChangeListener {
                packet?.ieHitG = it
            }
            cutoffDelay.addOnValueChangeListener {
                packet?.shutoffDelay = it
            }
            powerValveTurnOnThrd.addOnValueChangeListener {
                packet?.feOnThresholds = it
            }
            tpsThreshold.addOnValueChangeListener {
                packet?.tpsThreshold = it
            }

            inversionOfThrottlePositionSwitch.setOnCheckedChangeListener { _, isChecked ->
                packet?.carbInvers = if (isChecked) 1 else 0
            }

            fuelCutMapThreshold.addOnValueChangeListener {
                packet?.fuelcutMapThrd = it
            }

            fuelCutCtsThreshold.addOnValueChangeListener {
                packet?.fuelcutCtsThrd = it
            }

            revLimitingLowerThrd.addOnValueChangeListener {
                packet?.revlimLot = it
            }

            revLimitingUpperThrd.addOnValueChangeListener {
                packet?.revlimHit = it
            }

            useUnioutCond.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
                packet?.fuelcut_uni = uniOutItems.keys.elementAt(position)
            }

            useUnioutCondForIgnCutoff.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
                packet?.igncut_uni = uniOutItems.keys.elementAt(position)
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