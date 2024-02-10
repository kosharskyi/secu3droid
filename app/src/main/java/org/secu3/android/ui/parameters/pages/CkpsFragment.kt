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
import org.secu3.android.databinding.FragmentCkpsBinding
import org.secu3.android.models.packets.out.params.CkpsParamPacket
import org.secu3.android.ui.parameters.views.FloatParamView
import org.secu3.android.ui.parameters.views.IntParamView
import org.secu3.android.utils.gone
import org.secu3.android.utils.visible

class CkpsFragment : BaseParamFragment() {

    private lateinit var mBinding: FragmentCkpsBinding

    private var packet: CkpsParamPacket? = null

    private val teethBeforeTdcList = IntRange(12, 55).toList()
    private val numberOfCylinderList = listOf(2,4,6,8)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mBinding = FragmentCkpsBinding.inflate(inflater, container, false)
        return mBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initTeethBeforeTdc()
        initNumberOfCylinders()

        lifecycleScope.launch {
            withResumed {
                mViewModel.ckpsLiveData.observe(viewLifecycleOwner) {

                    mViewModel.isSendAllowed = false

                    packet = it

                    mBinding.apply {

                        progressBar.gone()
                        params.visible()

                        if (it.ckpsEdge) {
                            ckpsEdgeRising.isChecked = true
                        } else {
                            ckpsEdgeFalling.isChecked = true
                        }

                        if (it.refsEdge) {
                            refSEdgeRising.isChecked = true
                        } else {
                            refSEdgeFalling.isChecked = true
                        }

                        sparkRisingEdgeForCdi.isChecked = it.risingSpark
                        mergeSignalsToSingleOutput.isChecked = it.mergeOuts

                        numberOfWheelsTeeth.value = it.ckpsCogsNum
                        numberOfMissingTeeth.value = it.ckpsMissNum

                        teethBeforeTdc.setText(it.ckpsCogsBtdc.toString(), false)
                        numberOfCylinders.setText(it.ckpsEngineCyl.toString(), false)

                        durationIngDriverPulseTeeth.value = it.ckpsIgnitCogs
                        hallSensorInterrupterDegree.value = it.hallWndWidth
                        degreesBeforeTDC.value = it.hallDegreesBtdc

                        useCamSensorAsReference.isChecked = it.useCamRef
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

    private fun initTeethBeforeTdc() {
        mBinding.teethBeforeTdc.inputType = InputType.TYPE_NULL
        val adapter = ArrayAdapter(requireContext(), R.layout.list_item, teethBeforeTdcList.map { it.toString() })
        mBinding.teethBeforeTdc.setAdapter(adapter)
    }

    private fun initNumberOfCylinders() {
        mBinding.numberOfCylinders.inputType = InputType.TYPE_NULL
        val adapter = ArrayAdapter(requireContext(), R.layout.list_item, numberOfCylinderList.map { it.toString() })
        mBinding.numberOfCylinders.setAdapter(adapter)
    }

    private fun initViews() {

        mBinding.apply {

            ckpsEdgeGroup.setOnCheckedChangeListener { _, checkedId ->
                packet?.ckpsEdge = checkedId == R.id.ckps_edge_rising
            }

            refSEdgeGroup.setOnCheckedChangeListener { _, checkedId ->
                packet?.refsEdge = checkedId == R.id.ref_s_edge_rising
            }

            sparkRisingEdgeForCdi.setOnCheckedChangeListener { _, isChecked ->
                packet?.risingSpark = isChecked
            }
            mergeSignalsToSingleOutput.setOnCheckedChangeListener { _, isChecked ->
                packet?.mergeOuts = isChecked
            }

            numberOfWheelsTeeth.addOnValueChangeListener {
                packet?.ckpsCogsNum = it
            }
            numberOfMissingTeeth.addOnValueChangeListener {
                packet?.ckpsMissNum = it
            }

            teethBeforeTdc.setOnItemClickListener { _, _, position, _ ->
                packet?.ckpsCogsBtdc = teethBeforeTdcList[position]
            }
            numberOfCylinders.setOnItemClickListener { _, _, position, _ ->
                packet?.ckpsEngineCyl = numberOfCylinderList[position]
            }
            durationIngDriverPulseTeeth.addOnValueChangeListener {
                packet?.ckpsIgnitCogs = it
            }
            hallSensorInterrupterDegree.addOnValueChangeListener {
                packet?.hallWndWidth = it
            }
            degreesBeforeTDC.addOnValueChangeListener {
                packet?.hallDegreesBtdc = it
            }

            useCamSensorAsReference.setOnCheckedChangeListener { _, isChecked ->
                packet?.useCamRef = isChecked
            }

            numberOfWheelsTeeth.setOnClickListener { intParamClick(it as IntParamView) }
            numberOfMissingTeeth.setOnClickListener { intParamClick(it as IntParamView) }
            durationIngDriverPulseTeeth.setOnClickListener { intParamClick(it as IntParamView) }
            hallSensorInterrupterDegree.setOnClickListener { floatParamClick(it as FloatParamView) }
            degreesBeforeTDC.setOnClickListener { floatParamClick(it as FloatParamView) }
        }
    }
}