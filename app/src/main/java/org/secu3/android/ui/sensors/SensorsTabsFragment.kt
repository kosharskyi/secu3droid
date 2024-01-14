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

package org.secu3.android.ui.sensors

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayout.OnTabSelectedListener
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import org.secu3.android.R
import org.secu3.android.databinding.FragmentSensorsTabsBinding
import org.secu3.android.utils.Task
import org.secu3.android.utils.gone
import org.secu3.android.utils.visible

@AndroidEntryPoint
class SensorsTabsFragment : Fragment() {

    private lateinit var mBinding: FragmentSensorsTabsBinding

    private val mViewModel: SensorsViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mBinding = FragmentSensorsTabsBinding.inflate(layoutInflater)
        return mBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        init()

    }

    override fun onResume() {
        super.onResume()
        mBinding.apply {
            toolbar.setNavigationOnClickListener { findNavController().popBackStack() }
            logStart.isVisible = mViewModel.isLoggerEnabled && mViewModel.isLoggerStarted.not()
            logMarkBtnsGroup.isVisible = mViewModel.isLoggerEnabled && mViewModel.isLoggerStarted
        }
    }

    private fun init() {

        mBinding.apply {
            toolbar.apply {
                inflateMenu(R.menu.activity_main)

                setOnMenuItemClickListener { onMenuItemSelected(it) }
            }

            viewPager.adapter = SensorsPagerAdapter(this@SensorsTabsFragment)

            TabLayoutMediator(tabLayout, viewPager) { tab, position ->
                when (position) {
                    0 -> {
                        tab.text = getString(R.string.sensors_tab_title)
                    }
                    else -> {
                        tab.text = getString(R.string.menu_raw_sensors)
                    }
                }
            }.attach()

            tabLayout.addOnTabSelectedListener(object : OnTabSelectedListener {
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

            logStart.setOnClickListener {
                logMarkBtnsGroup.visible()
                logStart.gone()
                mViewModel.startWriteLog()
            }

            logMark1.setOnClickListener {
                mViewModel.logMark1()
            }

            logMark2.setOnClickListener {
                mViewModel.logMark2()
            }

            logMark3.setOnClickListener {
                mViewModel.logMark3()
            }

            logStop.setOnClickListener {
                logMarkBtnsGroup.gone()
                logStart.visible()
                mViewModel.stopWriteLog()
            }
        }
    }

    private fun onMenuItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {

            R.id.menu_errors -> {
                findNavController().navigate(SensorsTabsFragmentDirections.actionSensorsToErrors())
                true
            }

            R.id.menu_diagnostics -> {
                MaterialAlertDialogBuilder(requireContext()).setTitle(android.R.string.dialog_alert_title)
                    .setIcon(android.R.drawable.ic_dialog_alert).setMessage(R.string.menu_diagnostics_warning_title)
                    .setPositiveButton(android.R.string.ok) { _, _ ->

                        if (mViewModel.firmware?.isDiagnosticsEnabled == true) {
                            findNavController().navigate(SensorsTabsFragmentDirections.actionSensorsToDiagnostics())
                            return@setPositiveButton
                        }

                        Toast.makeText(context, R.string.diagnostics_not_supported_title, Toast.LENGTH_LONG).show()

                    }.setNegativeButton(android.R.string.cancel, null).create().show()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }
}