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
import org.secu3.android.databinding.FragmentAnglesBinding
import org.secu3.android.models.packets.params.AnglesParamPacket
import org.secu3.android.ui.parameters.ParamsViewModel
import org.secu3.android.ui.parameters.views.FloatParamView


class AnglesFragment : BaseParamFragment() {

    private val mViewModel: ParamsViewModel by activityViewModels()

    private lateinit var mBinding: FragmentAnglesBinding

    private var packet: AnglesParamPacket? = null


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mBinding = FragmentAnglesBinding.inflate(inflater, container, false)
        return mBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mViewModel.anglesLiveData.observe(viewLifecycleOwner) {

            packet = it

            mBinding.apply {
                maxAdvanceAngle.value = it.maxAngle
                minAdvanceAngle.value = it.minAngle

                angleDecreaseSpeed.value = it.angleDecSpeed
                angleIncreaseSpeed.value = it.angleIncSpeed

                octaneCorrection.value = it.angleCorrection

                zeroAdvAngle.isChecked = it.zeroAdvAngle > 0
            }

            initViews()
        }
    }

    private fun initViews() {

        mBinding.apply {
            maxAdvanceAngle.addOnValueChangeListener {
                packet?.maxAngle = it
                packet?.let { it1 -> mViewModel.sendPacket(it1) }
            }
            minAdvanceAngle.addOnValueChangeListener {
                packet?.minAngle = it
                packet?.let { it1 -> mViewModel.sendPacket(it1) }
            }
            angleDecreaseSpeed.addOnValueChangeListener {
                packet?.angleDecSpeed = it
                packet?.let { it1 -> mViewModel.sendPacket(it1) }
            }
            angleIncreaseSpeed.addOnValueChangeListener {
                packet?.angleIncSpeed = it
                packet?.let { it1 -> mViewModel.sendPacket(it1) }
            }
            octaneCorrection.addOnValueChangeListener {
                packet?.angleCorrection = it
                packet?.let { it1 -> mViewModel.sendPacket(it1) }
            }
            zeroAdvAngle.setOnCheckedChangeListener { _, isChecked ->
                packet?.zeroAdvAngle = if (isChecked) 1 else 0
                packet?.let { it1 -> mViewModel.sendPacket(it1) }
            }

            maxAdvanceAngle.setOnClickListener { floatParamClick(it as FloatParamView) }
            minAdvanceAngle.setOnClickListener { floatParamClick(it as FloatParamView) }
            angleDecreaseSpeed.setOnClickListener { floatParamClick(it as FloatParamView) }
            angleIncreaseSpeed.setOnClickListener { floatParamClick(it as FloatParamView) }
            octaneCorrection.setOnClickListener { floatParamClick(it as FloatParamView) }
        }
    }
}