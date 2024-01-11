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

package org.secu3.android.ui.main

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import org.secu3.android.R
import org.secu3.android.databinding.FragmentMainMenuBinding
import org.secu3.android.ui.settings.SettingsActivity

@AndroidEntryPoint
class MainFragment : Fragment() {

    private var mBinding: FragmentMainMenuBinding? = null

    private val mViewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // This callback will only be called when MainMenuFragment is at least Started.
        requireActivity().onBackPressedDispatcher.addCallback(this) {
            exit()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mBinding = FragmentMainMenuBinding.inflate(inflater, container, false)
        return mBinding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mBinding?.apply {

            dashboard.setOnClickListener {
                findNavController().navigate(MainFragmentDirections.actionOpenDashboard())
            }

            sensors.setOnClickListener {
                findNavController().navigate(MainFragmentDirections.actionOpenSensors())
            }

            ecuParams.setOnClickListener {
                findNavController().navigate(MainFragmentDirections.actionOpenParameters())
            }

            secuLogs.setOnClickListener {
                findNavController().navigate(MainFragmentDirections.actionOpenSecuLogs())
            }

            appSettings.setOnClickListener {
                startActivity(Intent(context, SettingsActivity::class.java))
            }
        }

        mViewModel.connectionStatusLiveData.observe(viewLifecycleOwner) {
            if (it) {
                mBinding?.connectionStatus?.text = getString(R.string.status_online)
            } else {
                mBinding?.connectionStatus?.text = getString(R.string.status_offline)
            }
        }

        mViewModel.firmwareLiveData.observe(viewLifecycleOwner) {
            mBinding?.apply {
                if (fwInfo.text.isEmpty()) {
                    fwInfo.text = it.tag
                }
            }
        }
    }

    private fun exit() {
        mViewModel.closeConnection()
        activity?.finishAndRemoveTask()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mBinding = null
    }

    override fun onDestroy() {
        super.onDestroy()
        exit()
    }
}