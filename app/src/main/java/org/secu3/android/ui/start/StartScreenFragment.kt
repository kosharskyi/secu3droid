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

package org.secu3.android.ui.start

import android.Manifest
import android.app.Activity.RESULT_OK
import android.app.PendingIntent
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.content.ContextCompat
import androidx.core.content.IntentCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import dagger.hilt.android.AndroidEntryPoint
import org.secu3.android.connection.Connected
import org.secu3.android.ui.MainActivity

@AndroidEntryPoint
class StartScreenFragment : Fragment() {

    private var showBluetoothRationale by mutableStateOf(false)

    private val viewModel: StartScreenViewModel by viewModels()

    private val usbDeviceActionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (ACTION_USB_ATTACHED == intent.action) {
                val usbDevice = IntentCompat.getParcelableExtra(intent, UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
                usbDevice?.let { viewModel.newUsbDeviceAttached(it) }
            }
            if (ACTION_USB_DETACHED == intent.action) {
                val usbDevice = IntentCompat.getParcelableExtra(intent, UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
                usbDevice?.let { viewModel.newUsbDeviceDetached() }
            }

            if (ACTION_USB_PERMISSION == intent.action) {
                Log.d(this.javaClass.simpleName, "ACTION_USB_PERMISSION RECEIVED")
                val device = IntentCompat.getParcelableExtra(intent, UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
                if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                    device?.let { viewModel.startConnection(it) }
                } else {
                    Log.d(this.javaClass.simpleName, "Permission denied for device $device")
                }
            }
        }
    }

    private val btDeviceDiscoveryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log.e(this.javaClass.simpleName, "onReceive")
            if (intent.action == BluetoothDevice.ACTION_FOUND) {
                Log.e(this.javaClass.simpleName, "ACTION_FOUND")
                val device: BluetoothDevice? =
                    IntentCompat.getParcelableExtra(intent, BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                device?.takeIf { viewModel.getBluetoothDeviceName(it).isNullOrEmpty().not() }?.let {
                    Log.e(this.javaClass.simpleName, "Device found: ${viewModel.getBluetoothDeviceName(it)}")
                    viewModel.discoveredBtDevices.find { device -> device.address == it.address } ?: viewModel.discoveredBtDevices.add(it)
                }
            }

            if (intent.action == BluetoothDevice.ACTION_BOND_STATE_CHANGED) {
                val device: BluetoothDevice? =
                    IntentCompat.getParcelableExtra(intent, BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)

                device?.let {

                    when (viewModel.getBluetoothBondState(it)) {
                        BluetoothDevice.BOND_BONDED -> {
                            viewModel.setBtDevice(it)
                            viewModel.startConnection(null)
                        }
                        BluetoothDevice.BOND_BONDING -> {
                            viewModel.enableProgress()
                        }
                        else -> {
                            viewModel.disableProgress()
                        }
                    }

                }
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                SideEffect {
                    WindowInsetsControllerCompat(requireActivity().window, requireView()).isAppearanceLightStatusBars = true
                }
                val connecting by viewModel.isConnectionInProgressLiveData.observeAsState(false)
                val usbDevice by viewModel.mUsbDeviceAttachedLiveData.observeAsState()
                val devices = viewModel.discoveredBtDevices.map { device ->
                    StartBluetoothDevice(
                        id = device.address,
                        name = viewModel.getBluetoothDeviceName(device).orEmpty(),
                    )
                }
                StartScreenContent(
                    state = StartScreenState(
                        connecting = connecting,
                        usbDeviceDetected = usbDevice != null,
                        showBluetoothDevices = viewModel.showBottomSheet.value,
                        bluetoothDevices = devices,
                        showBluetoothRationale = showBluetoothRationale,
                    ),
                    actions = StartScreenActions(
                        onConnectOrCancel = { inProgress -> connectBtnClicked(inProgress) },
                        onLogs = { findNavController().navigate(StartScreenFragmentDirections.openSecuLogsFragment()) },
                        onSettings = { (requireActivity() as MainActivity).openSettings() },
                        onBluetoothDeviceSelected = ::onBluetoothDeviceSelected,
                        onBluetoothSheetDismissed = {
                            viewModel.showBottomSheet.value = false
                            viewModel.discoveredBtDevices.clear()
                            viewModel.cancelBtDiscovery()
                        },
                        onRationaleConfirmed = {
                            showBluetoothRationale = false
                            permissionRequest.launch(pendingBluetoothPermissions)
                        },
                        onRationaleCancelled = { showBluetoothRationale = false },
                    ),
                )
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.isConnectedLiveData.observe(viewLifecycleOwner) {
            if (it is Connected) {
                findNavController().navigate(StartScreenFragmentDirections.openHomeFragment())
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (viewModel.isUsbHostSupported) {
            requireContext().registerReceiver(usbDeviceActionReceiver, IntentFilter(ACTION_USB_ATTACHED))
            requireContext().registerReceiver(usbDeviceActionReceiver, IntentFilter(ACTION_USB_DETACHED))
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                requireContext().registerReceiver(
                    usbDeviceActionReceiver,
                    IntentFilter(ACTION_USB_PERMISSION),
                    Context.RECEIVER_NOT_EXPORTED
                )
            }
        }

        requireContext().registerReceiver(btDeviceDiscoveryReceiver, IntentFilter(BluetoothDevice.ACTION_FOUND))
        requireContext().registerReceiver(btDeviceDiscoveryReceiver, IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED))
    }

    override fun onPause() {
        super.onPause()
        if (viewModel.isUsbHostSupported) {
            requireContext().unregisterReceiver(usbDeviceActionReceiver)
        }

        requireContext().unregisterReceiver(btDeviceDiscoveryReceiver)
    }

    private fun onBluetoothDeviceSelected(address: String) {
        val device = viewModel.discoveredBtDevices.firstOrNull { it.address == address } ?: return
        if (viewModel.getBluetoothBondState(device) == BluetoothDevice.BOND_BONDED) {
            viewModel.setBtDevice(device)
            viewModel.startConnection(null)
        } else {
            viewModel.enableProgress()
            viewModel.createBluetoothBond(device)
        }
        viewModel.showBottomSheet.value = false
    }

    private fun connectBtnClicked(isInProgress: Boolean) {
        if (isInProgress) {
            viewModel.cancelConnection()
            return
        }

        viewModel.usbDevice?.let {
            checkUsbPermissionsAndConnect(it)
            return
        }

        checkBluetoothPermissionsAndConnect()
    }

    private fun checkUsbPermissionsAndConnect(usbDevice: UsbDevice) {
        if (viewModel.usbManager.hasPermission(usbDevice)) {
            Log.d(this.javaClass.simpleName, "Permission already granted for USB device: ${usbDevice.deviceName}")
            viewModel.startConnection(usbDevice)
        } else {
            Log.d(this.javaClass.simpleName, "Requesting permission for USB device: ${usbDevice.deviceName}")
            val permissionIntent = PendingIntent.getBroadcast(requireContext(),0, Intent(ACTION_USB_PERMISSION), PendingIntent.FLAG_IMMUTABLE)
            viewModel.usbManager.requestPermission(usbDevice, permissionIntent)
        }
    }

    private val permissionRequest = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        val isGranted = it.values.all { isGranted -> isGranted }

        if (isGranted.not()) {
            return@registerForActivityResult
        }

        checkBtConfig()
    }

    private var pendingBluetoothPermissions: Array<String> = emptyArray()

    private val enableBtRequest = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == RESULT_OK) {
            checkBluetoothPermissionsAndConnect()
        }
    }

    private fun checkBluetoothPermissionsAndConnect() {
        val permissions = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
            permissions.add(Manifest.permission.BLUETOOTH_SCAN)

            if (viewModel.isXiaomi && Build.VERSION.SDK_INT == Build.VERSION_CODES.S) {
                permissions.add(Manifest.permission.BLUETOOTH)
            }

        } else {
            permissions.add(Manifest.permission.BLUETOOTH)
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
            permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }

        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
            // required only for Android 10
            permissions.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }

        val deniedPermissions = permissions.filter { ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_DENIED }

        if (deniedPermissions.isEmpty()) {
            checkBtConfig()
            return
        }

        pendingBluetoothPermissions = permissions.toTypedArray()

        if (permissions.any{ shouldShowRequestPermissionRationale(it) }) {
            showBluetoothRationale = true
            return
        }

        permissionRequest.launch(permissions.toTypedArray())
    }

    private fun checkBtConfig() {

        when {
            viewModel.isBtEnabled().not() -> enableBt()
            viewModel.isLocationEnabled().not() -> enableLocation()
            viewModel.isBtDeviceAddressNotSelected() -> {
                viewModel.showBottomSheet.value = true
                viewModel.startBtDiscovery()
            }
            else -> viewModel.startConnection(null)
        }

    }

    private fun enableLocation() {
        val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
        startActivity(intent)
    }

    private fun enableBt() {
        val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        enableBtRequest.launch(enableBtIntent)
    }

    companion object {
        private const val ACTION_USB_ATTACHED = "android.hardware.usb.action.USB_DEVICE_ATTACHED"
        private const val ACTION_USB_DETACHED = "android.hardware.usb.action.USB_DEVICE_DETACHED"
        private const val ACTION_USB_PERMISSION = "org.secu3.android.USB_PERMISSION"
    }
}
