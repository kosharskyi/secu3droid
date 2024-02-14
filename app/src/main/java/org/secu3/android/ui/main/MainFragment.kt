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
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import org.secu3.android.R
import org.secu3.android.SecuConnectionService
import org.secu3.android.databinding.FragmentMainMenuBinding
import org.secu3.android.network.models.GitHubRelease
import org.secu3.android.ui.settings.SettingsActivity

@AndroidEntryPoint
class MainFragment : Fragment() {

    private var mBinding: FragmentMainMenuBinding? = null

    private val mViewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // This callback will only be called when MainMenuFragment is at least Started.
        requireActivity().onBackPressedDispatcher.addCallback(this) {
            requireActivity().finish()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mBinding = FragmentMainMenuBinding.inflate(inflater, container, false)
        return mBinding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mBinding?.apply {

            exit.setOnClickListener {
                exit()
            }

            carStatus.setOnClickListener {
                mViewModel.firmware ?: return@setOnClickListener
                findNavController().navigate(MainFragmentDirections.actionOpenFirmwareDialog())
            }

            dashboard.setOnClickListener {
                findNavController().navigate(MainFragmentDirections.actionOpenDashboard())
            }

            sensors.setOnClickListener {
                findNavController().navigate(MainFragmentDirections.actionOpenSensors())
            }

            ecuParams.setOnClickListener {
                findNavController().navigate(MainFragmentDirections.actionOpenParameters())
            }

            secuCheckEngine.setOnClickListener {
                findNavController().navigate(MainFragmentDirections.actionOpenSecuErrors())
            }

            secuDiagnostics.setOnClickListener {
                showDiagnosticAlert()
            }

            secuLogs.setOnClickListener {
                findNavController().navigate(MainFragmentDirections.actionOpenSecuLogs())
            }

            appSettings.setOnClickListener {
                startActivity(Intent(context, SettingsActivity::class.java))
            }
        }

        mViewModel.newReleaseAvailable.observe(viewLifecycleOwner) {
            showNewVersionDialog(it)
        }

        mViewModel.connectionStatusLiveData.observe(viewLifecycleOwner) {
            if (it) {
                mBinding?.carStatus?.setColorFilter(Color.GREEN)
            } else {
                mBinding?.carStatus?.setColorFilter(Color.RED)
            }
        }
    }

    private fun showNewVersionDialog(release: GitHubRelease) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.new_version_available))
            .setMessage(getString(R.string.the_version_is_available_do_you_want_to_download, release.tagName))
            .setPositiveButton(getString(R.string.download)) { _, _ ->
                mViewModel.downloadRelease(release)
            }
            .setNeutralButton(getString(R.string.remind_me_later)) { _, _ ->
                // do nothing
            }.show()
    }

    private fun showDiagnosticAlert() {
        MaterialAlertDialogBuilder(requireContext()).setTitle(android.R.string.dialog_alert_title)
            .setIcon(android.R.drawable.ic_dialog_alert).setMessage(R.string.menu_diagnostics_warning_title)
            .setPositiveButton(android.R.string.ok) { _, _ ->

                if (mViewModel.firmware?.isDiagnosticsEnabled == true) {
                    findNavController().navigate(MainFragmentDirections.actionOpenSecuDiagnostic())
                    return@setPositiveButton
                }

                Toast.makeText(context, R.string.diagnostics_not_supported_title, Toast.LENGTH_LONG).show()

            }.setNegativeButton(android.R.string.cancel, null).create().show()
    }

    private fun exit() {
        requireActivity().stopService(Intent(requireContext(), SecuConnectionService::class.java))
        mViewModel.closeConnection()
        activity?.finishAndRemoveTask()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mBinding = null
    }
}