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
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.os.PowerManager.WakeLock
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import org.secu3.android.R
import org.secu3.android.databinding.FragmentDashboardBinding
import org.secu3.android.models.packets.input.SensorsPacket
import org.secu3.android.utils.LifeTimePrefs
import org.secu3.android.utils.Task
import javax.inject.Inject

@AndroidEntryPoint
class DashBoardFragment : Fragment() {

    private var time: Long = 0
    private var delta = 0f

    private lateinit var mBinding: FragmentDashboardBinding

    private val mViewModel: DashboardViewModel by viewModels()

    override fun onAttach(context: Context) {
        super.onAttach(context)
//        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mBinding = FragmentDashboardBinding.inflate(layoutInflater)
        return mBinding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mBinding.toolbar.apply {
            setNavigationIcon(R.drawable.ic_arrow_back_24)
            setNavigationOnClickListener { activity?.onBackPressed() }
        }

        if (mViewModel.isBluetoothDeviceAddressNotSelected()) {
            Toast.makeText(context, getString(R.string.choose_bluetooth_adapter), Toast.LENGTH_LONG).show()
        }


        mViewModel.connectionStatusLiveData.observe(viewLifecycleOwner) {
            if (it) {
                mBinding.connectionStatus.text = getString(R.string.status_online)
            } else {
                mBinding.connectionStatus.text = getString(R.string.status_offline)
            }
        }

        mViewModel.packetLiveData.observe(viewLifecycleOwner) {
            updatePacket(it)
        }

        mViewModel.statusLiveData.observe(viewLifecycleOwner) {
            mBinding.ledOnline.isVisible = it
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

        mBinding.speed.text = "%.0f km/h".format(packet.speed)
        mBinding.odometer.text = "%.2f km".format(packet.distance)

        mBinding.manometer.text = "%.2f kPa".format(packet.map)

        mBinding.temperature.text = "%.2f C".format(packet.temperature)

        mBinding.voltage.text = "%.1f V".format(packet.voltage)

        mBinding.rpm.text = "${packet.rpm} RPM"


        mBinding.ledCheckEngine.isVisible = packet.checkEngineBit > 0
        mBinding.ledGasoline.isVisible = packet.gasBit > 0
        mBinding.ledEco.isVisible = packet.ephhValveBit > 0
        mBinding.ledPower.isVisible = packet.epmValveBit > 0
        mBinding.ledChoke.isVisible = packet.carbBit > 0
        mBinding.ledFan.isVisible = packet.coolFanBit > 0
    }

    override fun onDestroy() {
        super.onDestroy()
//        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }
}