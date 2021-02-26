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

package org.secu3.android.ui.main

import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayout
import dagger.hilt.android.AndroidEntryPoint
import org.secu3.android.R
import org.secu3.android.databinding.FragmentSensorsBinding
import org.secu3.android.ui.settings.SettingsActivity
import org.secu3.android.utils.Task
import java.util.*

@AndroidEntryPoint
class SensorsFragment : Fragment() {

    private lateinit var mBinding: FragmentSensorsBinding

    private val mViewModel: SensorsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // This callback will only be called when MyFragment is at least Started.
        requireActivity().onBackPressedDispatcher.addCallback(this) {
            exit()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mBinding = FragmentSensorsBinding.inflate(layoutInflater)
        return mBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        init()

        mViewModel.connectionStatusLiveData.observe(viewLifecycleOwner) {
            if (it) {
                mBinding.connectionStatus.text = getString(R.string.status_online)
            } else {
                mBinding.connectionStatus.text = getString(R.string.status_offline)
            }
        }

    }

    override fun onResume() {
        super.onResume()
        when {
            mViewModel.isBluetoothDeviceAddressNotSelected() -> {
                Toast.makeText(context, R.string.choose_bluetooth_adapter, Toast.LENGTH_LONG).show()
            }
            !BluetoothAdapter.getDefaultAdapter().isEnabled -> {
                Toast.makeText(context, R.string.msg_bluetooth_disabled, Toast.LENGTH_LONG).show()
            }
            else -> {
                mViewModel.start()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mViewModel.closeConnection()
    }

    private fun init() {

        mBinding.toolbar.apply {
            inflateMenu(R.menu.activity_main)

            setOnMenuItemClickListener { onMenuItemSelected(it) }
        }

        mBinding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> mViewModel.sendNewTask(Task.Secu3ReadSensors)
                    else -> mViewModel.sendNewTask(Task.Secu3ReadRawSensors)
                }

            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {
            }
        })


        mViewModel.firmwareLiveData.observe(viewLifecycleOwner, {
            mBinding.fwInfo.text = it.tag
            mViewModel.sendNewTask(Task.Secu3ReadSensors)
        })

        mViewModel.sensorsLiveData.observe(viewLifecycleOwner, {
            var result = ""
            result += String.format(Locale.US, getString(R.string.status_rpm_title), it.rpm)
            result += String.format(Locale.US, getString(R.string.status_map_title), it.map)
            result += String.format(Locale.US, getString(R.string.status_voltage_title), it.voltage)
            result += String.format(Locale.US, getString(R.string.status_temperature_title), it.temperature)
            result += String.format(Locale.US, getString(R.string.status_angle_correction_title), it.currentAngle)
            result += String.format(Locale.US, getString(R.string.status_knock_title), it.knockValue)
            result += String.format(Locale.US, getString(R.string.status_knock_retard_title), it.knockRetard)
            result += String.format(Locale.US, getString(R.string.status_air_flow_title), it.airflow)

            result += String.format(Locale.US, getString(R.string.status_fi_valve_title), it.ephhValveBit)
            result += String.format(Locale.US, getString(R.string.status_carb_status_title), it.carbBit)
            result += String.format(Locale.US, getString(R.string.status_gas_valve_title), it.gasBit)
            result += String.format(Locale.US, getString(R.string.status_power_valve_title), it.epmValveBit)
            result += String.format(Locale.US, getString(R.string.status_ecf_title), it.coolFanBit)
            result += String.format(Locale.US, getString(R.string.status_starter_block_title), it.stBlockBit)

            result += String.format(Locale.US, getString(R.string.status_addi1_voltage_title), it.addI1)
            result += String.format(Locale.US, getString(R.string.status_addi2_voltage_title), it.addI2)
            result += String.format(Locale.US, getString(R.string.status_tps_title), it.tps)
            result += String.format(Locale.US, getString(R.string.status_choke_position_title), it.chokePosition)

            mBinding.sensorsData.text = result
        })

        mViewModel.rawSensorsLiveData.observe(viewLifecycleOwner, {
            var result = ""
            result += String.format(Locale.US, getString(R.string.raw_status_map_title), it.map)
            result += String.format(Locale.US, getString(R.string.raw_status_voltage_title), it.voltage)
            result += String.format(Locale.US, getString(R.string.raw_status_temperature_title), it.temperature)
            result += String.format(Locale.US, getString(R.string.raw_status_knock_title), it.knockValue)
            result += String.format(Locale.US, getString(R.string.raw_status_tps_title), it.tps)
            result += String.format(Locale.US, getString(R.string.raw_status_addi1_title), it.addI1)
            result += String.format(Locale.US, getString(R.string.raw_status_addi2_title), it.addI2)

            mBinding.sensorsData.text = result
        })
    }

    override fun onStart() {
        super.onStart()
        mViewModel.sendNewTask(Task.Secu3ReadFirmwareInfo)
    }

    private fun onMenuItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_dashboard -> {
                findNavController().navigate(SensorsFragmentDirections.actionSensorsToDashboard())
                true
            }
            R.id.menu_preferences -> {
                startActivity(Intent(context, SettingsActivity::class.java))
                true
            }
            R.id.menu_params -> {
                findNavController().navigate(SensorsFragmentDirections.actionSensorsToParameters())
                true
            }
            R.id.menu_errors -> {
                findNavController().navigate(SensorsFragmentDirections.actionSensorsToErrors())
                true
            }
            R.id.menu_exit -> {
                exit()
                true
            }
            R.id.menu_diagnostics -> {
                MaterialAlertDialogBuilder(requireContext()).setTitle(android.R.string.dialog_alert_title)
                    .setIcon(android.R.drawable.ic_dialog_alert).setMessage(R.string.menu_diagnostics_warning_title)
                    .setPositiveButton(android.R.string.ok) { _, _ ->

                        if (mViewModel.firmware.isDiagnosticsEnabled) {
                            findNavController().navigate(SensorsFragmentDirections.actionSensorsToDiagnostics())
                            return@setPositiveButton
                        }

                        Toast.makeText(context, R.string.diagnostics_not_supported_title, Toast.LENGTH_LONG).show()

                    }.setNegativeButton(android.R.string.cancel, null).create().show()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun exit() {
        mViewModel.closeConnection()
        activity?.finishAndRemoveTask()
    }
}