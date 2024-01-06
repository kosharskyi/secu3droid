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
import androidx.fragment.app.activityViewModels
import org.secu3.android.R
import org.secu3.android.databinding.FragmentAccelerationBinding
import org.secu3.android.models.packets.params.AccelerationParamPacket
import org.secu3.android.ui.parameters.ParamsViewModel
import org.secu3.android.ui.parameters.views.IntParamView

class AccelerationFragment : BaseParamFragment() {

    private val mViewModel: ParamsViewModel by activityViewModels()
    private lateinit var mBinding: FragmentAccelerationBinding

    private val accelEnrichmentTypesList: List<String> by lazy {
        resources.getStringArray(R.array.accel_enrichment_types).toList()
    }

    private var packet: AccelerationParamPacket? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mBinding = FragmentAccelerationBinding.inflate(inflater, container, false)
        return mBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mBinding.aeType.inputType = InputType.TYPE_NULL
        ArrayAdapter(requireContext(), R.layout.list_item, accelEnrichmentTypesList).also {
            mBinding.aeType.setAdapter(it)
        }

        mViewModel.accelerationLiveData.observe(viewLifecycleOwner) {

            packet = it

            mBinding.apply {
                accelTpsdotThreshold.value = it.injAeTpsdotThrd
                coldAccelMultiplier.value = it.injAeColdaccMult
                aeDecayTime.value = it.injAeDecayTime

                aeType.setText(accelEnrichmentTypesList[it.injAeType], false)

                aeTime.value = it.injAeTime
            }
        }

        initViews()
    }

    private fun initViews() {

        mBinding.apply {

            accelTpsdotThreshold.addOnValueChangeListener {
                packet?.injAeTpsdotThrd = it
                packet?.let { it1 -> mViewModel.sendPacket(it1) }
            }

            coldAccelMultiplier.addOnValueChangeListener {
                packet?.injAeColdaccMult = it
                packet?.let { it1 -> mViewModel.sendPacket(it1) }
            }

            aeDecayTime.addOnValueChangeListener {
                packet?.injAeDecayTime = it
                packet?.let { it1 -> mViewModel.sendPacket(it1) }
            }

            aeType.setOnItemClickListener { _, _, position, _ ->
                packet?.apply {
                    injAeType = position
                    mViewModel.sendPacket(this)
                }
            }

            aeTime.addOnValueChangeListener {
                packet?.apply {
                    injAeTime = it
                    mViewModel.sendPacket(this)
                }
            }


            accelTpsdotThreshold.setOnClickListener { intParamClick(it as IntParamView) }
            coldAccelMultiplier.setOnClickListener { intParamClick(it as IntParamView) }
            aeDecayTime.setOnClickListener { intParamClick(it as IntParamView) }
            aeTime.setOnClickListener { intParamClick(it as IntParamView) }
        }
    }
}