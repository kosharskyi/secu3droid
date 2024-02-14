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

package org.secu3.android.ui.bluetoothStatus

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import org.secu3.android.utils.UserPrefs
import javax.inject.Inject

@HiltViewModel
class BluetoothStatusViewModel @Inject constructor(
    private val mPrefs: UserPrefs,
    private val bluetoothManager: BluetoothManager
): ViewModel() {

    private val bluetoothAdapter: BluetoothAdapter by lazy { bluetoothManager.adapter }

    fun isBtEnabled(): Boolean {
        return bluetoothAdapter.isEnabled
    }

    fun isBtDeviceAddressNotSelected(): Boolean {
        return mPrefs.bluetoothDeviceName.isNullOrBlank()
    }

    fun isBtDeviceNotExist(): Boolean {

        val btAddress = mPrefs.bluetoothDeviceName ?: return false

        val bluetoothDevice: BluetoothDevice? = bluetoothAdapter.bondedDevices.firstOrNull { it.name == btAddress }

        return bluetoothDevice == null
    }

    val isBtConfigured: Boolean
        get() {
            return isBtEnabled() && !isBtDeviceAddressNotSelected() && isBtDeviceNotExist().not()
        }

}