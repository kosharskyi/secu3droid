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
package org.secu3.android.ui

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.AndroidEntryPoint
import org.secu3.android.R
import org.secu3.android.connection.SecuConnectionService

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val viewModel: MainActivityViewModel by viewModels()

    private val usbDeviceActionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (ACTION_USB_ATTACHED == intent.action) {
                val usbDevice = intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE)
                usbDevice?.let { deviceAttached(it) }
            }

            if (ACTION_USB_PERMISSION == intent.action) {
                val device = intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE) ?: return
                val permissionGranted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
                if (permissionGranted) {
                    Toast.makeText(context, "Permission granted for device: ${device.deviceName}", Toast.LENGTH_SHORT).show()
                    viewModel.newUsbDeviceAttached(device)
                } else {
                    Toast.makeText(context, "Permission denied for USB device", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        registerReceiver(usbDeviceActionReceiver, IntentFilter(ACTION_USB_ATTACHED))
    }

    override fun onResume() {
        super.onResume()

        handleKeepScreenOn()
        handleSecuConnectionService()
    }

    private fun deviceAttached(device: UsbDevice) {
        Toast.makeText(this, "USB Device Attached: ${device.deviceName}", Toast.LENGTH_SHORT).show()
        val hasPermission = viewModel.usbManager.hasPermission(device)

        if (hasPermission) {
            viewModel.newUsbDeviceAttached(device)
        } else {
            val permissionIntent = PendingIntent.getBroadcast(this,0, Intent(ACTION_USB_PERMISSION),PendingIntent.FLAG_IMMUTABLE)
            viewModel.usbManager.requestPermission(device, permissionIntent)
        }
    }

    private fun handleKeepScreenOn() {
        if (viewModel.prefs.isKeepScreenAliveActive) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    private fun handleSecuConnectionService() {
        val intent = Intent(this, SecuConnectionService::class.java)
        if (viewModel.prefs.isWakeLockEnabled) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
        } else {
            stopService(intent)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(usbDeviceActionReceiver)
    }

    companion object {
        private const val ACTION_USB_ATTACHED = "android.hardware.usb.action.USB_DEVICE_ATTACHED"
        private const val ACTION_USB_PERMISSION = "org.secu3.android.USB_PERMISSION"
    }
}