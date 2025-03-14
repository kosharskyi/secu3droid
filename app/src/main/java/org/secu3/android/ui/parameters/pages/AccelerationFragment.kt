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
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.withResumed
import kotlinx.coroutines.launch
import org.secu3.android.R
import org.secu3.android.databinding.FragmentAccelerationBinding
import org.secu3.android.models.packets.out.params.AccelerationParamPacket
import org.secu3.android.ui.parameters.views.IntParamView
import org.secu3.android.utils.gone
import org.secu3.android.utils.visible

class AccelerationFragment : BaseParamFragment() {

    private lateinit var mBinding: FragmentAccelerationBinding

    private val accelEnrichmentTypesList: List<String> by lazy {
        resources.getStringArray(R.array.accel_enrichment_types).toList()
    }
    private val wallwetModelList: List<String> by lazy {
        resources.getStringArray(R.array.accel_wallwet_types).toList()
    }

    private var packet: AccelerationParamPacket? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mBinding = FragmentAccelerationBinding.inflate(inflater, container, false)
        return mBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mBinding.apply {

            aeType.inputType = InputType.TYPE_NULL
            ArrayAdapter(requireContext(), R.layout.list_item, accelEnrichmentTypesList).also {
                aeType.setAdapter(it)
            }

            wallwetModel.inputType = InputType.TYPE_NULL
            ArrayAdapter(requireContext(), R.layout.list_item, wallwetModelList).also {
                wallwetModel.setAdapter(it)
            }
        }

        lifecycleScope.launch {
            withResumed {
                mViewModel.accelerationLiveData.observe(viewLifecycleOwner) {
                    mViewModel.isSendAllowed = false

                    packet = it

                    mBinding.apply {
                        progressBar.gone()
                        params.visible()

                        accelTpsdotThreshold.value = it.injAeTpsdotThrd
                        coldAccelMultiplier.value = it.injAeColdaccMult
                        aeDecayTime.value = it.injAeDecayTime

                        aeType.setText(accelEnrichmentTypesList[it.injAeType], false)

                        aeTime.isInvisible = it.injAeType == 0
                        aeTime.value = it.injAeTime

                        aeBalance.value = it.injAeBallance
                        aeMapdoeThrd.value = it.injAeMapdotThrd

                        wallwetModel.setText(wallwetModelList[it.wallwetModel], false)

                        xTauStartThrd.isVisible = it.wallwetModel > 0
                        xTauStartThrd.value = it.injXtauSThrd.toInt()

                        xTauFinishThrd.isVisible = it.wallwetModel > 0
                        xTauFinishThrd.value = it.injXtauFThrd.toInt()
                    }

                    initViews()

                    mViewModel.isSendAllowed = true
                }
            }
        }
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
                aeTime.isInvisible = position == 0

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

            aeBalance.addOnChangeListener { _, value, fromUser ->
                if (fromUser.not()) {
                    return@addOnChangeListener
                }

                packet?.apply {
                    injAeBallance = value
                    mViewModel.sendPacket(this)
                }
            }

            aeMapdoeThrd.addOnValueChangeListener {
                packet?.apply {
                    injAeMapdotThrd = it
                    mViewModel.sendPacket(this)
                }
            }

            wallwetModel.setOnItemClickListener { _, _, position, _ ->
                xTauStartThrd.isVisible = position > 0
                xTauFinishThrd.isVisible = position > 0

                packet?.apply {
                    wallwetModel = position
                    mViewModel.sendPacket(this)
                }
            }

            xTauStartThrd.addOnValueChangeListener {
                packet?.apply {
                    injXtauSThrd = it.toFloat()
                    mViewModel.sendPacket(this)
                }
            }

            xTauFinishThrd.addOnValueChangeListener {
                packet?.apply {
                    injXtauFThrd = it.toFloat()
                    mViewModel.sendPacket(this)
                }
            }

            accelTpsdotThreshold.setOnClickListener { intParamClick(it as IntParamView) }
            coldAccelMultiplier.setOnClickListener { intParamClick(it as IntParamView) }
            aeDecayTime.setOnClickListener { intParamClick(it as IntParamView) }
            aeTime.setOnClickListener { intParamClick(it as IntParamView) }
            aeMapdoeThrd.setOnClickListener { intParamClick(it as IntParamView) }
            xTauStartThrd.setOnClickListener { intParamClick(it as IntParamView) }
            xTauFinishThrd.setOnClickListener { intParamClick(it as IntParamView) }
        }
    }

    override fun tabTitle(): Int {
        return R.string.params_tab_acceleration_title
    }
}