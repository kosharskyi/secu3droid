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
import android.hardware.usb.UsbManager
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import org.secu3.android.Secu3Connection
import org.secu3.android.utils.UserPrefs
import javax.inject.Inject

@HiltViewModel
class MainActivityViewModel @Inject constructor(
    private val secu3Connection: Secu3Connection,
    val usbManager: UsbManager,
    val prefs: UserPrefs,
) : ViewModel() {



    fun newUsbDeviceAttached(device: UsbDevice) {
        with(secu3Connection) {
            if (isConnectionRunning.not()) {
                startUsbConnection(device)
                return
            }

            if (isUsbRunning && isUsbConnected.not()) {
                startUsbConnection(device)
                return
            }

            if (isBtRunning && isBtConnected.not()) {
                startUsbConnection(device)
                return
            }

            if (isUsbConnected) {
                // TODO: make up this case
            }

            if (isBtConnected) {
                // TODO: make up this case
            }
        }

    }

}