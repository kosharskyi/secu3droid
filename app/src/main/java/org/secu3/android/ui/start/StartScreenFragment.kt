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
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import org.secu3.android.R
import org.secu3.android.connection.Connected
import org.secu3.android.ui.bluetoothStatus.StartScreenViewModel
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
                val device = intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE) ?: return
                val permissionGranted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
                if (permissionGranted) {
                    Toast.makeText(context, "Permission granted for device: ${device.deviceName}", Toast.LENGTH_SHORT).show()
                    viewModel.startConnection(device)
                } else {
                    Toast.makeText(context, "Permission denied for USB device", Toast.LENGTH_SHORT).show()
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
        requireContext().registerReceiver(usbDeviceActionReceiver, IntentFilter(ACTION_USB_ATTACHED))
        requireContext().registerReceiver(usbDeviceActionReceiver, IntentFilter(ACTION_USB_DETACHED))
//        requireContext().registerReceiver(usbDeviceActionReceiver, IntentFilter(ACTION_USB_PERMISSION))
    }

    override fun onPause() {
        super.onPause()
        requireContext().unregisterReceiver(usbDeviceActionReceiver)
    }

    @Composable
    fun StartScreen(viewModel: StartScreenViewModel = viewModel()) {

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
                            textNormal = "Connect",
                            textInProgress = "Connecting...",
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

                        val device = viewModel.mUsbDeviceAttachedLiveData.observeAsState()
                        device.value?.let {
                            Text("USB device detected", modifier = Modifier.padding(16.dp).align(Alignment.BottomCenter))
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
            val usbManager = requireContext().getSystemService(Context.USB_SERVICE) as UsbManager
            if (usbManager.hasPermission(usbDevice)) {
                viewModel.startConnection(usbDevice)
            } else {
                val permissionIntent = PendingIntent.getBroadcast(requireContext(),0, Intent(ACTION_USB_PERMISSION), PendingIntent.FLAG_IMMUTABLE)
                usbManager.requestPermission(usbDevice, permissionIntent)
            }
        } else {
            viewModel.startConnection(usbDevice)
        }
    }

    private val permissionRequest = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        val isDeclined = it.values.any { isGranted -> isGranted.not() }

        if (isDeclined.not()) {
            checkBtConfig()
        }
    }

    private fun checkBluetoothPermissionsAndConnect() {
        val permissions = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
            permissions.add(Manifest.permission.BLUETOOTH_SCAN)
        } else {
            permissions.add(Manifest.permission.BLUETOOTH)
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
            viewModel.isBtDeviceAddressNotSelected() -> showNoBtDeviceDialog()
            viewModel.isBtDeviceNotExist() -> showBtDeviceNotValidDialog()
            else -> viewModel.startConnection(null)
        }

    }

    private fun showBtDeviceNotValidDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.select_bluetooth_device)
            .setMessage(R.string.bluetooth_device_not_valid)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                startActivity(Intent(context, SettingsActivity::class.java))
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showNoBtDeviceDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.select_bluetooth_device)
            .setMessage(R.string.choose_bluetooth_adapter)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                startActivity(Intent(context, SettingsActivity::class.java))
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
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