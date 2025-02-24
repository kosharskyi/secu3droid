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
            }
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

            useUnioutCond.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
                packet?.apply {
                    fuelcut_uni = uniOutItems.keys.elementAt(position)
                    mViewModel.sendPacket(this)
                }
            }

            useUnioutCondForIgnCutoff.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
                packet?.apply {
                    igncut_uni = uniOutItems.keys.elementAt(position)
                    mViewModel.sendPacket(this)
                }
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

    override fun tabTitle(): Int {
        return R.string.params_tab_fuel_cutoff_title
    }
}