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

package org.secu3.android.ui.home

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import org.secu3.android.R
import org.secu3.android.connection.Connected
import org.secu3.android.connection.Disconnected
import org.secu3.android.connection.InProgress
import org.secu3.android.connection.SecuConnectionService
import org.secu3.android.databinding.FragmentHomeBinding
import org.secu3.android.network.models.GitHubRelease
import org.secu3.android.ui.settings.SettingsActivity

@AndroidEntryPoint
class HomeFragment : Fragment() {

    private var mBinding: FragmentHomeBinding? = null

    private val mViewModel: HomeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // This callback will only be called when MainMenuFragment is at least Started.
        requireActivity().onBackPressedDispatcher.addCallback(this) {
            exit()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mBinding = FragmentHomeBinding.inflate(inflater, container, false)
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
                findNavController().navigate(HomeFragmentDirections.actionOpenFirmwareDialog())
            }

            dashboard.setOnClickListener {
                findNavController().navigate(HomeFragmentDirections.actionOpenDashboard())
            }

            sensors.setOnClickListener {
                findNavController().navigate(HomeFragmentDirections.actionOpenSensors())
            }

            ecuParams.setOnClickListener {
                findNavController().navigate(HomeFragmentDirections.actionOpenParameters())
            }

            secuCheckEngine.setOnClickListener {
                findNavController().navigate(HomeFragmentDirections.actionOpenSecuErrors())
            }

            secuDiagnostics.setOnClickListener {
                showDiagnosticAlert()
            }

            secuLogs.setOnClickListener {
                findNavController().navigate(HomeFragmentDirections.actionOpenSecuLogs())
            }

            appSettings.setOnClickListener {
                startActivity(Intent(context, SettingsActivity::class.java))
            }
        }

        mViewModel.newReleaseAvailable.observe(viewLifecycleOwner) {
            showNewVersionDialog(it)
        }

        mViewModel.connectionStatusLiveData.observe(viewLifecycleOwner) {
            when (it) {
                Connected -> {
                    mBinding?.carStatus?.setColorFilter(ContextCompat.getColor(requireContext(), R.color.gauge_dark_green))
                }
                InProgress -> {
                    mBinding?.carStatus?.setColorFilter(ContextCompat.getColor(requireContext(), R.color.gauge_dark_yellow))
                }
                Disconnected -> {
                    mBinding?.carStatus?.setColorFilter(ContextCompat.getColor(requireContext(), R.color.gauge_red))
                    if (mViewModel.isUserTapExit) {
                        findNavController().popBackStack()
                    }
                }
                else -> {
                    // do nothing
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()

        handleSecuConnectionService()
    }

    private fun showNewVersionDialog(release: GitHubRelease) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.new_version_available))
            .setMessage(getString(R.string.the_version_is_available_do_you_want_to_download, release.tagName))
            .setPositiveButton(getString(R.string.download)) { _, _ ->
                checkPermissionsAndDownload(release)
            }
            .setNegativeButton(getString(R.string.what_s_new)) { _, _ ->
                val intent = CustomTabsIntent.Builder().build()
                intent.launchUrl(requireContext(), release.htmlUrl.toUri())
            }
            .setNeutralButton(getString(R.string.remind_me_later)) { _, _ ->
                // do nothing
            }.show()
    }

    private val permissionRequest = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            mViewModel.newReleaseAvailable.value?.let { mViewModel.downloadRelease(it) }
        }
    }

    private fun checkPermissionsAndDownload(release: GitHubRelease) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            val permission = android.Manifest.permission.WRITE_EXTERNAL_STORAGE

            if (ContextCompat.checkSelfPermission(requireContext(), permission) == PackageManager.PERMISSION_GRANTED) {
                mViewModel.downloadRelease(release)
            }

            if (shouldShowRequestPermissionRationale(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.download_manager_permission_rationale_title)
                    .setMessage(R.string.download_manager_permission_rationale_message)
                    .setPositiveButton(android.R.string.ok) { _, _ -> permissionRequest.launch(permission) }
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()
                return
            }

            permissionRequest.launch(permission)
        } else {
            mViewModel.downloadRelease(release)
        }
    }

    private fun showDiagnosticAlert() {
        MaterialAlertDialogBuilder(requireContext()).setTitle(android.R.string.dialog_alert_title)
            .setIcon(android.R.drawable.ic_dialog_alert).setMessage(R.string.menu_diagnostics_warning_title)
            .setPositiveButton(android.R.string.ok) { _, _ ->

                if (mViewModel.firmware?.isDiagnosticsEnabled == true) {
                    findNavController().navigate(HomeFragmentDirections.actionOpenSecuDiagnostic())
                    return@setPositiveButton
                }

                Toast.makeText(context, R.string.diagnostics_not_supported_title, Toast.LENGTH_LONG).show()

            }.setNegativeButton(android.R.string.cancel, null).create().show()
    }

    private fun handleSecuConnectionService() {
        val intent = Intent(requireContext(), SecuConnectionService::class.java)
        if (mViewModel.prefs.isWakeLockEnabled) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                requireActivity().startForegroundService(intent)
            } else {
                requireActivity().startService(intent)
            }
        } else {
            requireActivity().stopService(intent)
        }
    }

    private fun exit() {
        MaterialAlertDialogBuilder(requireContext()).apply {
            setTitle(getString(R.string.disconnect_device))
            setMessage(getString(R.string.disconnect_device_msg))
            setPositiveButton(android.R.string.ok) { _, _ ->
                requireActivity().stopService(Intent(requireContext(), SecuConnectionService::class.java))
                mViewModel.closeConnection()
            }
            setNegativeButton(android.R.string.cancel, null)
            show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mBinding = null
    }
}