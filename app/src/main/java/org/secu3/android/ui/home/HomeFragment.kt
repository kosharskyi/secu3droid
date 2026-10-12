/*
 *    SecuDroid  - An open source, free manager for SECU-3 engine control unit
 *    Copyright (C) 2024 Vitalii O. Kosharskyi. Ukraine, Kyiv
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import org.secu3.android.ui.compose.ConfirmationDialog
import dagger.hilt.android.AndroidEntryPoint
import org.secu3.android.R
import org.secu3.android.connection.Connected
import org.secu3.android.connection.Disconnected
import org.secu3.android.connection.InProgress
import org.secu3.android.connection.SecuConnectionService
import org.secu3.android.models.packets.input.FirmwareInfoPacket
import org.secu3.android.network.models.GitHubRelease
import org.secu3.android.ui.settings.SettingsActivity

@AndroidEntryPoint
class HomeFragment : Fragment() {
    private val mViewModel: HomeViewModel by viewModels()
    private var connectionStatus by mutableStateOf(HomeConnectionStatus.DISCONNECTED)
    private var firmware by mutableStateOf<FirmwareInfoPacket?>(null)
    private var release by mutableStateOf<GitHubRelease?>(null)
    private var exitPending = false
    private var showExitConfirmation by mutableStateOf(false)
    private var showDownloadPermissionRationale by mutableStateOf(false)
    private val releasePermissionFlow = PendingReleasePermissionFlow()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requireActivity().onBackPressedDispatcher.addCallback(this) { exit() }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                    HomeScreen(
                        state = HomeScreenState(
                            connectionStatus = connectionStatus,
                            firmwareAvailable = firmware != null,
                            connectionRunning = mViewModel.isConnectionRunning,
                            showExitConfirmation = showExitConfirmation,
                            releaseName = release?.displayName,
                            releaseMessage = release?.let { getString(R.string.the_version_is_available_do_you_want_to_download, it.displayName) },
                        ),
                        actions = HomeScreenActions(
                            onDashboard = { findNavController().navigate(HomeFragmentDirections.actionOpenDashboard()) },
                            onSensors = { findNavController().navigate(HomeFragmentDirections.actionOpenSensors()) },
                            onParameters = { findNavController().navigate(HomeFragmentDirections.actionOpenParameters()) },
                            onErrors = { findNavController().navigate(HomeFragmentDirections.actionOpenSecuErrors()) },
                            onDiagnosticsConfirmed = {
                                if (mViewModel.firmware?.isDiagnosticsEnabled == true) {
                                    findNavController().navigate(HomeFragmentDirections.actionOpenSecuDiagnostic())
                                } else {
                                    Toast.makeText(requireContext(), R.string.diagnostics_not_supported_title, Toast.LENGTH_LONG).show()
                                }
                            },
                            onLogs = { findNavController().navigate(HomeFragmentDirections.actionOpenSecuLogs()) },
                            onSettings = { startActivity(Intent(requireContext(), SettingsActivity::class.java)) },
                            onFirmware = { findNavController().navigate(HomeFragmentDirections.actionOpenFirmwareDialog()) },
                            onExitImmediately = { stopServiceAndNavigate() },
                            onExitCancelled = { showExitConfirmation = false },
                            onExitConfirmed = { showExitConfirmation = false; exitPending = true; requireActivity().stopService(serviceIntent()); mViewModel.closeConnection() },
                            onDownloadRelease = { release?.let(::checkPermissionsAndDownload) },
                            onReleaseNotes = { release?.let { CustomTabsIntent.Builder().build().launchUrl(requireContext(), it.htmlUrl.toUri()) } },
                        ),
                    )
                    if (showDownloadPermissionRationale) {
                        ConfirmationDialog(
                            title = R.string.download_manager_permission_rationale_title,
                            message = R.string.download_manager_permission_rationale_message,
                            onConfirm = {
                                showDownloadPermissionRationale = false
                                permissionRequest.launch(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                            },
                            onCancel = { showDownloadPermissionRationale = false; releasePermissionFlow.cancel() },
                        )
                    }
            }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        firmware = mViewModel.firmware
        mViewModel.firmwareLiveData.observe(viewLifecycleOwner) {
            if (mViewModel.isConnectionRunning) firmware = it
        }
        mViewModel.newReleaseAvailable.observe(viewLifecycleOwner) { release = it }
        mViewModel.connectionStatusLiveData.observe(viewLifecycleOwner) {
            connectionStatus = when (it) {
                Connected -> HomeConnectionStatus.CONNECTED
                InProgress -> HomeConnectionStatus.CONNECTING
                Disconnected -> HomeConnectionStatus.DISCONNECTED
                else -> HomeConnectionStatus.DISCONNECTED
            }
            if (it == Disconnected && mViewModel.isUserTapExit && exitPending) {
                exitPending = false
                findNavController().navigateUp()
            }
            if (it == Disconnected) firmware = null
        }
    }

    override fun onResume() {
        super.onResume()
        handleSecuConnectionService()
    }

    private val permissionRequest = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        releasePermissionFlow.complete(granted)?.let(mViewModel::downloadRelease)
    }

    private fun checkPermissionsAndDownload(item: GitHubRelease) {
        releasePermissionFlow.begin(item)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            val permission = android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            if (ContextCompat.checkSelfPermission(requireContext(), permission) == PackageManager.PERMISSION_GRANTED) {
                mViewModel.downloadRelease(item)
                releasePermissionFlow.cancel()
                return
            }
            if (shouldShowRequestPermissionRationale(permission)) {
                showDownloadPermissionRationale = true
                return
            }
            permissionRequest.launch(permission)
        } else {
            releasePermissionFlow.cancel()
            mViewModel.downloadRelease(item)
        }
    }

    private fun serviceIntent() = Intent(requireContext(), SecuConnectionService::class.java)

    private fun handleSecuConnectionService() {
        if (mViewModel.prefs.isWakeLockEnabled) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) requireActivity().startForegroundService(serviceIntent())
            else requireActivity().startService(serviceIntent())
        } else requireActivity().stopService(serviceIntent())
    }

    private fun stopServiceAndNavigate() {
        requireActivity().stopService(serviceIntent())
        findNavController().navigateUp()
    }

    private fun exit() {
        if (!mViewModel.isConnectionRunning) {
            stopServiceAndNavigate()
            return
        }
        // The Compose exit dialog's confirmation requests shutdown and navigation occurs on Disconnected.
        showExitConfirmation = true
    }
}

internal class PendingReleasePermissionFlow {
    private var requestedRelease: GitHubRelease? = null

    fun begin(release: GitHubRelease) {
        requestedRelease = release
    }

    fun complete(granted: Boolean): GitHubRelease? {
        val release = requestedRelease
        requestedRelease = null
        return release.takeIf { granted }
    }

    fun cancel() {
        requestedRelease = null
    }
}
