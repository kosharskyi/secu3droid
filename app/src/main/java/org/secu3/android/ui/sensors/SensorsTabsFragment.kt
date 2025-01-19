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

import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayout.OnTabSelectedListener
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import org.secu3.android.R
import org.secu3.android.connection.Connected
import org.secu3.android.connection.Disconnected
import org.secu3.android.connection.InProgress
import org.secu3.android.databinding.FragmentSensorsTabsBinding
import org.secu3.android.utils.Task
import org.secu3.android.utils.UserPrefs
import org.secu3.android.utils.gone
import org.secu3.android.utils.visible
import javax.inject.Inject

@AndroidEntryPoint
class SensorsTabsFragment : Fragment() {

    private var mBinding: FragmentSensorsTabsBinding? = null

    private val mViewModel: SensorsViewModel by viewModels()

    @Inject
    internal lateinit var mPrefs: UserPrefs

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mBinding = FragmentSensorsTabsBinding.inflate(layoutInflater)
        return mBinding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        init()

        mViewModel.connectionStatusLiveData.observe(viewLifecycleOwner) {
            val color = when (it) {
                Connected -> {
                    ContextCompat.getColor(requireContext(), R.color.gauge_dark_green)
                }
                InProgress -> {
                    ContextCompat.getColor(requireContext(), R.color.gauge_dark_yellow)
                }
                Disconnected -> {
                    ContextCompat.getColor(requireContext(), R.color.gauge_red)
                }
                else -> {
                    return@observe
                }
            }

            mBinding?.toolbar?.menu?.findItem(R.id.connection_status)?.apply {
                val iconDrawable = icon ?: return@apply

                // Tint the icon with the desired color
                DrawableCompat.setTint(iconDrawable, color)
                DrawableCompat.setTintMode(iconDrawable, PorterDuff.Mode.SRC_IN)

                // Set the tinted icon to the menu item
                icon = iconDrawable
            }
        }

    }

    override fun onResume() {
        super.onResume()
        mBinding?.apply {
            viewPager.adapter = SensorsPagerAdapter(this@SensorsTabsFragment, mPrefs.oldSensorViewEnabled)

            toolbar.setNavigationOnClickListener { findNavController().navigateUp() }

            if (mViewModel.isLoggerEnabled && mViewModel.isLoggerStarted.not()) {
                logStart.show()
            } else {
                logStart.hide()
            }

            if (mViewModel.isLoggerEnabled && mViewModel.isLoggerStarted) {
                showLogMarks()
            } else {
                hideLogMarks()
            }
        }
    }

    private fun hideLogMarks() {
        mBinding?.apply {
            logMark1.hide()
            logMark2.hide()
            logMark3.hide()
            logStop.hide()
        }
    }

    private fun showLogMarks() {
        mBinding?.apply {
            logMark1.show()
            logMark2.show()
            logMark3.show()
            logStop.show()
        }
    }

    private fun init() {

        mBinding?.apply {

            toolbar.apply {
                inflateMenu(R.menu.fragment_sensors_tabs_menu)
                setOnMenuItemClickListener {
                    when (it.itemId) {
                        R.id.settings -> {
                            findNavController().navigate(SensorsTabsFragmentDirections.actionOpenSettings())
                            true
                        }
                        else -> false
                    }
                }
            }

            viewPager.adapter = SensorsPagerAdapter(this@SensorsTabsFragment, mPrefs.oldSensorViewEnabled)

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
                logMark1.show()
                logMark2.show()
                logMark3.show()
                logStop.show()
                logStart.hide()
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
                logMark1.hide()
                logMark2.hide()
                logMark3.hide()
                logStop.hide()
                logStart.show()
                mViewModel.stopWriteLog()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mBinding = null
    }
}