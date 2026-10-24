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

package org.secu3.android.ui

import android.hardware.usb.UsbDevice
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import org.secu3.android.utils.UserPrefs
import org.secu3.android.connection.Secu3ConnectionManager
import org.secu3.android.connection.UsbAttachmentAttemptGate
import javax.inject.Inject

@HiltViewModel
class MainActivityViewModel @Inject constructor(
    val prefs: UserPrefs,
    val connectionManager: Secu3ConnectionManager,
) : ViewModel() {
    private val usbAttemptGate = UsbAttachmentAttemptGate()
    private var usbPermissionDevice: UsbDevice? = null
    private var usbPermissionToken = 0L
    private var handledUsbGeneration: Long? = null
    private var pendingStartNavigation = false
    val hasPendingUsbPermission: Boolean get() = usbPermissionDevice != null

    @Synchronized fun beginUsbAttempt(device: UsbDevice): Long? {
        val token = usbAttemptGate.begin(device.deviceId) ?: return null
        usbPermissionDevice = device
        usbPermissionToken = token
        return token
    }

    @Synchronized fun isUsbAttempted(deviceId: Int): Boolean = usbAttemptGate.hasAttempted(deviceId)

    @Synchronized fun hasOpenedUsb(deviceId: Int): Boolean = usbAttemptGate.hasOpened(deviceId)

    @Synchronized fun claimUsbOpen(deviceId: Int): Boolean {
        if (!usbAttemptGate.claimOpen(deviceId)) return false
        if (usbPermissionDevice?.deviceId == deviceId) usbPermissionDevice = null
        return true
    }

    @Synchronized fun consumePermissionDevice(token: Long, deviceId: Int): UsbDevice? {
        val device = usbPermissionDevice?.takeIf {
            usbPermissionToken == token && it.deviceId == deviceId && usbAttemptGate.accepts(deviceId, token)
        } ?: return null
        usbPermissionDevice = null
        return device
    }

    @Synchronized fun usbDeviceDetached(deviceId: Int) {
        usbAttemptGate.detach(deviceId)
        if (usbPermissionDevice?.deviceId == deviceId) {
            usbPermissionDevice = null
            usbPermissionToken += 1
        }
    }

    @Synchronized fun reconcileUsbAttachments(attachedDeviceIds: Set<Int>) {
        usbAttemptGate.reconcile(attachedDeviceIds)
        if (usbPermissionDevice?.let { it.deviceId !in attachedDeviceIds } == true) {
            usbPermissionDevice = null
            usbPermissionToken += 1
        }
    }

    @Synchronized fun claimUsbHomeNavigation(generation: Long): Boolean {
        if (handledUsbGeneration == generation) return false
        handledUsbGeneration = generation
        pendingStartNavigation = false
        return true
    }

    @Synchronized fun requestStartNavigation() { pendingStartNavigation = true }
    @Synchronized fun consumeStartNavigation(): Boolean = pendingStartNavigation.also { pendingStartNavigation = false }
}
