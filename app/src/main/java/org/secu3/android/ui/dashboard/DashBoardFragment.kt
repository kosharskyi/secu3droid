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
package org.secu3.android.ui.dashboard

import android.content.Context
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import org.secu3.android.R
import org.secu3.android.databinding.FragmentDashboardBinding
import org.secu3.android.utils.Task

@AndroidEntryPoint
class DashBoardFragment : Fragment() {

    private var mBinding: FragmentDashboardBinding? = null

    private val mViewModel: DashboardViewModel by viewModels()

    override fun onAttach(context: Context) {
        super.onAttach(context)
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mBinding = FragmentDashboardBinding.inflate(layoutInflater)
        return mBinding?.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        mBinding?.apply {

            with(mViewModel.dashboardConfig.center) {
                center.clearSections()
                center.tickNumber = type.tickCount
                center.minSpeed = min
                center.maxSpeed = max
                center.unit = getString(type.units)
                center.addSections(
                    type.getSections(requireContext(), center.speedometerWidth)
                )
            }

            with(mViewModel.dashboardConfig.topLeft) {
                topLeft.clearSections()
                topLeft.minSpeed = min
                topLeft.tickNumber = type.tickCount
                topLeft.maxSpeed = max
                topLeft.unit = getString(type.units)
                topLeft.addSections(
                    type.getSections(requireContext(), center.speedometerWidth)
                )
            }

            with(mViewModel.dashboardConfig.bottomRight) {
                bottomRight.clearSections()
                bottomRight.minSpeed = min
                bottomRight.tickNumber = type.tickCount
                bottomRight.maxSpeed = max
                bottomRight.unit = getString(type.units)
                bottomRight.addSections(
                    type.getSections(requireContext(), center.speedometerWidth)
                )
            }

             with(mViewModel.dashboardConfig.topRight) {
                 topRigth.clearSections()
                 topRigth.minSpeed = min
                 topRigth.tickNumber = type.tickCount
                 topRigth.maxSpeed = max
                 topRigth.unit = getString(type.units)
                 topRigth.addSections(
                     type.getSections(requireContext(), center.speedometerWidth)
                 )
             }

            with(mViewModel.dashboardConfig.bottomLeft) {
                bottomLeft.clearSections()
                bottomLeft.minSpeed = min
                bottomLeft.tickNumber = type.tickCount
                bottomLeft.maxSpeed = max
                bottomLeft.unit = getString(type.units)
                bottomLeft.addSections(
                    type.getSections(requireContext(), center.speedometerWidth)
                )
            }
        }

        if (mViewModel.isBluetoothDeviceAddressNotSelected()) {
            Toast.makeText(context, getString(R.string.choose_bluetooth_adapter), Toast.LENGTH_LONG).show()
        }

        mViewModel.packetLiveData.observe(viewLifecycleOwner) {
            updatePacket(it)
        }

        mViewModel.statusLiveData.observe(viewLifecycleOwner) {
            val color = if (it) {
                ContextCompat.getColor(requireContext(), R.color.gauge_green)
            } else {
                ContextCompat.getColor(requireContext(), R.color.gauge_red)
            }

            mBinding?.ledOnline?.setColorFilter(color)
        }
    }

    @Synchronized
    override fun onResume() {
        super.onResume()
        mViewModel.setTask(Task.Secu3ReadSensors)
    }

    private fun updatePacket(data: DashboardViewData) {

        mBinding?.apply {

            topLeft.setSpeedAt(data.topLeft)
            topRigth.setSpeedAt(data.topRight)

            center.setSpeedAt(data.center)

            bottomLeft.setSpeedAt(data.bottomLeft)
            bottomRight.setSpeedAt(data.bottomRight)

//            odometer.text = "%.2f km".format(data.distance)

            ledCheckEngine.isVisible = data.ledCheckEngine
            ledGasoline.isVisible = data.ledGasoline
            ledEco.isVisible = data.ledEco
            ledPower.isVisible = data.ledPower
            ledChoke.isVisible = data.ledChoke
            ledFan.isVisible = data.ledFan
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mBinding = null
    }

    override fun onDestroy() {
        super.onDestroy()
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }
}