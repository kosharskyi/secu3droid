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
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import org.secu3.android.R
import org.secu3.android.connection.Connected
import org.secu3.android.ui.settings.SettingsActivity

@AndroidEntryPoint
class StartScreenFragment : Fragment() {

    private val viewModel: StartScreenViewModel by viewModels()

    private val usbDeviceActionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (ACTION_USB_ATTACHED == intent.action) {
                val usbDevice = intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE)
                usbDevice?.let { viewModel.newUsbDeviceAttached(it) }
            }
            if (ACTION_USB_DETACHED == intent.action) {
                val usbDevice = intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE)
                usbDevice?.let { viewModel.newUsbDeviceDetached() }
            }

            if (ACTION_USB_PERMISSION == intent.action) {
                Log.d(this.javaClass.simpleName, "ACTION_USB_PERMISSION RECEIVED")
                val device = intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE)
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
                    intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                device?.takeIf { it.name.isNullOrEmpty().not() }?.let {
                    Log.e(this.javaClass.simpleName, "Device found: ${it.name}")
                    viewModel.discoveredBtDevices.find { device -> device.address == it.address } ?: viewModel.discoveredBtDevices.add(it)
                }
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        return ComposeView(requireContext()).apply {
            setContent {
                StartScreen(viewModel)
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
    }

    override fun onPause() {
        super.onPause()
        if (viewModel.isUsbHostSupported) {
            requireContext().unregisterReceiver(usbDeviceActionReceiver)
        }

        requireContext().unregisterReceiver(btDeviceDiscoveryReceiver)
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun StartScreen(viewModel: StartScreenViewModel = viewModel()) {

        var showBottomSheet by remember { viewModel.showBottomSheet }

        MaterialTheme {
            Surface {
                Box(modifier = Modifier.fillMaxSize()) {

                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings icon",
                        modifier = Modifier
                            .size(64.dp)
                            .padding(16.dp)
                            .align(Alignment.TopEnd)
                            .clickable {
                                startActivity(Intent(requireContext(), SettingsActivity::class.java))
                            },
                        tint = Color.Black
                    )

                    val isInProgress = viewModel.isConnectionInProgressLiveData.observeAsState()
                    isInProgress.value?.let {
                        CircularButton(
                            textNormal = stringResource(R.string.connect),
                            textInProgress = stringResource(R.string.status_connecting),
                            isInProgress = it,
                            onClick = {
                                connectBtnClicked(it)
                            },
                            size = 200.dp,
                            borderColor = Color.Gray,
                            progressSegmentColor = Color.Cyan,
                            textColor = Color.Black,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }

                    val device = viewModel.mUsbDeviceAttachedLiveData.observeAsState()
                    device.value?.let {
                        Text(
                            stringResource(R.string.usb_device_detected), modifier = Modifier
                                .padding(16.dp)
                                .align(Alignment.BottomCenter))
                    }

                    if (showBottomSheet) {

                        ModalBottomSheet(
                            onDismissRequest = {
                                showBottomSheet = false
                                viewModel.discoveredBtDevices.clear()
                                viewModel.cancelBtDiscovery()
                            }
                        ) {
                            androidx.compose.material3.Text(
                                text = stringResource(R.string.select_a_device_from_the_list),
                                style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
                                modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 16.dp)
                            )
                            HorizontalDivider()

                            val devices = remember { viewModel.discoveredBtDevices }

                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                            ) {
                                items(devices, key = { it.address }) { device ->
                                    ListItem(
                                        headlineContent = {
                                            Row {
                                                Image(Icons.Default.Bluetooth, contentDescription = "Bluetooth icon",
                                                    modifier = Modifier
                                                        .padding(end = 8.dp)
                                                        .align(Alignment.CenterVertically))
                                                Column {
                                                    androidx.compose.material3.Text(device.name ?: device.alias ?: stringResource(R.string.unknown_device))
                                                    androidx.compose.material3.Text(device.address, fontSize = 12.sp, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)) // Відображення MAC-адреси()
                                                }
                                            }
                                        },
                                        modifier = Modifier.clickable {
                                            viewModel.setBtDevice(device)
                                            viewModel.startConnection(null)
                                            viewModel.showBottomSheet.value = false
                                        }
                                    )
                                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun connectBtnClicked(isInProgress: Boolean) {
        if (isInProgress) return

        viewModel.usbDevice?.let {
            checkUsbPermissionsAndConnect(it)
            return
        }

        checkBluetoothPermissionsAndConnect()
    }

    private fun checkUsbPermissionsAndConnect(usbDevice: UsbDevice) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (viewModel.usbManager.hasPermission(usbDevice)) {
                Log.d(this.javaClass.simpleName, "Permission already granted for USB device: ${usbDevice.deviceName}")
                viewModel.startConnection(usbDevice)
            } else {
                Log.d(this.javaClass.simpleName, "Requesting permission for USB device: ${usbDevice.deviceName}")
                val permissionIntent = PendingIntent.getBroadcast(requireContext(),0, Intent(ACTION_USB_PERMISSION), PendingIntent.FLAG_IMMUTABLE)
                viewModel.usbManager.requestPermission(usbDevice, permissionIntent)
            }
        } else {
            viewModel.startConnection(usbDevice)
        }
    }

    private val permissionRequest = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        val isGranted = it.values.all { isGranted -> isGranted }

        if (isGranted.not()) {
            return@registerForActivityResult
        }

        checkBtConfig()
    }

    private fun checkBluetoothPermissionsAndConnect() {
        val permissions = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
            permissions.add(Manifest.permission.BLUETOOTH_SCAN)
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
        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                // Bluetooth enabled
                checkBluetoothPermissionsAndConnect()
            } else {
                // User refused to enable Bluetooth
            }
        }
    }

    companion object {
        private const val REQUEST_ENABLE_BT = 243135
        private const val ACTION_USB_ATTACHED = "android.hardware.usb.action.USB_DEVICE_ATTACHED"
        private const val ACTION_USB_DETACHED = "android.hardware.usb.action.USB_DEVICE_DETACHED"
        private const val ACTION_USB_PERMISSION = "org.secu3.android.USB_PERMISSION"
    }
}