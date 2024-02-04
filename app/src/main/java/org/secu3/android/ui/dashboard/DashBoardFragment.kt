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
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.github.anastr.speedviewlib.components.Section
import dagger.hilt.android.AndroidEntryPoint
import org.secu3.android.R
import org.secu3.android.databinding.FragmentDashboardBinding
import org.secu3.android.models.packets.input.SensorsPacket
import org.secu3.android.ui.sensors.models.GaugeType
import org.secu3.android.utils.Task

@AndroidEntryPoint
class DashBoardFragment : Fragment() {

    private var time: Long = 0
    private var delta = 0f

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
            speed.clearSections()
            speed.addSections(
                Section(0.0f, 1f, Color.GREEN, width = speed.speedometerWidth)
            )

            rpm.clearSections()
            rpm.addSections(GaugeType.RPM.getSections(requireContext(), speed.speedometerWidth))

            voltage.clearSections()
            voltage.addSections(
                Section(0.0f, 0.22f, Color.RED, width = speed.speedometerWidth),
                Section(0.22f, 0.67f, Color.GREEN, width = speed.speedometerWidth),
                Section(0.67f, 1f, Color.RED, width = speed.speedometerWidth)
            )

            temperature.clearSections()
            temperature.addSections(
                Section(0.0f, 0.8f, Color.GRAY, width = speed.speedometerWidth),
                Section(0.8f, 1f, Color.RED, width = speed.speedometerWidth)
            )

            map.clearSections()
            map.addSections(
                Section(0.0f, 0.2f, Color.GRAY, width = speed.speedometerWidth),
                Section(0.2f, 0.8f, Color.GREEN, width = speed.speedometerWidth),
                Section(0.8f, 1f, Color.RED, width = speed.speedometerWidth)
            )
        }

        if (mViewModel.isBluetoothDeviceAddressNotSelected()) {
            Toast.makeText(context, getString(R.string.choose_bluetooth_adapter), Toast.LENGTH_LONG).show()
        }

        mViewModel.packetLiveData.observe(viewLifecycleOwner) {
            updatePacket(it)
        }

        mViewModel.statusLiveData.observe(viewLifecycleOwner) {
            if (it) {
                mBinding?.ledOnline?.setColorFilter(Color.GREEN)
            } else {
                mBinding?.ledOnline?.setColorFilter(Color.RED)
            }
        }
    }

    @Synchronized
    override fun onResume() {
        super.onResume()
        mViewModel.setTask(Task.Secu3ReadSensors)
    }

    private fun updatePacket(packet: SensorsPacket) {
        if (time != 0L) {
            delta = ((System.currentTimeMillis() - time) / 1000.0).toFloat()
        }

        time = System.currentTimeMillis()

        mBinding?.apply {

            speed.setSpeedAt(packet.speed)
            odometer.text = "%.2f km".format(packet.distance)

            map.setSpeedAt(packet.map)

            temperature.setSpeedAt(packet.temperature)

            voltage.setSpeedAt(packet.voltage)

            rpm.setSpeedAt(packet.rpm.toFloat())

            ledCheckEngine.isVisible = packet.checkEngineBit > 0
            ledGasoline.isVisible = packet.gasBit > 0
            ledEco.isVisible = packet.ephhValveBit > 0
            ledPower.isVisible = packet.epmValveBit > 0
            ledChoke.isVisible = packet.carbBit > 0
            ledFan.isVisible = packet.coolFanBit > 0
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