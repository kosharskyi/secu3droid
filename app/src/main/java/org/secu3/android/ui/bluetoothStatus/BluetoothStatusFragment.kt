/*
 *    SecuDroid  - An open source, free manager for SECU-3 engine control unit
 *    Copyright (C) 2021 Vitaliy O. Kosharskiy. Ukraine, Kharkiv
 *
 *    SECU-3  - An open source, free engine control unit
 *    Copyright (C) 2007 Alexey A. Shabelnikov. Ukraine, Kyiv
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

package org.secu3.android.ui.bluetoothStatus

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.withResumed
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.secu3.android.R
import org.secu3.android.databinding.FragmentBluetoothStatusBinding
import org.secu3.android.ui.settings.SettingsActivity
import org.secu3.android.utils.gone
import org.secu3.android.utils.visible


@AndroidEntryPoint
class BluetoothStatusFragment : Fragment() {

    private lateinit var mBinding: FragmentBluetoothStatusBinding

    private val mViewModel: BluetoothStatusViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mBinding = FragmentBluetoothStatusBinding.inflate(inflater, container, false)
        return mBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mBinding.apply {

            btDisabledButton.setOnClickListener {
                checkBtConfig()
            }

            btSettingsButton.setOnClickListener {
                startActivity(Intent(context, SettingsActivity::class.java))
            }

            btUuidButton.setOnClickListener {
                startActivity(Intent(context, SettingsActivity::class.java))
            }
        }

        lifecycleScope.launch {
            lifecycle.withResumed {
                checkBluetoothPermissions()
            }
        }
    }

    private val permissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        val isDeclined = it.values.any { isGranted -> isGranted.not() }

        if (isDeclined.not()) {
            checkBtConfig()
        }
    }

    private fun checkBluetoothPermissions() {
        val permissions = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
            permissions.add(Manifest.permission.BLUETOOTH_SCAN)
        } else {
            permissions.add(Manifest.permission.BLUETOOTH)
        }

        val deniedPermissions = permissions.filter {
            ContextCompat.checkSelfPermission(requireContext(),
                it) == PackageManager.PERMISSION_DENIED
        }

        if (deniedPermissions.isEmpty()) {
            checkBtConfig()
            return
        }

        if (permissions.any{ shouldShowRequestPermissionRationale(it) }) {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.permission_rationale_title)
                .setMessage(R.string.permission_rationale_message)
                .setPositiveButton(android.R.string.ok) { _, _ -> permissionRequest.launch(permissions.toTypedArray()) }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
            return
        }

        permissionRequest.launch(permissions.toTypedArray())
    }

    private fun checkBtConfig() {
        mBinding.apply {
            btDisabledGroup.gone()
            btSettingsGroup.gone()
            btUuidGroup.gone()

            when {
                mViewModel.isBtEnabled().not() -> btDisabledGroup.visible()
                mViewModel.isBtDeviceAddressNotSelected() -> btSettingsGroup.visible()
                mViewModel.isBtDeviceNotExist() -> btUuidGroup.visible()
            }

        }

        if (mViewModel.isBtConfigured) {
            findNavController().navigate(BluetoothStatusFragmentDirections.actionOpenSensors())
        }
    }
}