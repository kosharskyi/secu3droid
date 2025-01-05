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
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.secu3.android.connection.ConnectionState
import org.secu3.android.connection.Secu3Connection
import org.secu3.android.utils.UserPrefs
import javax.inject.Inject

@HiltViewModel
class StartScreenViewModel @Inject constructor(
    private val mPrefs: UserPrefs,
    private val bluetoothManager: BluetoothManager,
    val usbManager: UsbManager,
    private val secu3Connection: Secu3Connection,
): ViewModel() {

    private val isConnectionInProgressFlow = MutableStateFlow(false)
    val isConnectionInProgressLiveData: LiveData<Boolean>
        get() = isConnectionInProgressFlow.asLiveData()

    val isConnectedLiveData: LiveData<ConnectionState>
        get() = secu3Connection.connectionStateFlow.asLiveData()

    private val bluetoothAdapter: BluetoothAdapter by lazy { bluetoothManager.adapter }

    var usbDevice: UsbDevice? = null
    private val mUsbDeviceAttachedFlow = MutableStateFlow<UsbDevice?>(null)
    val mUsbDeviceAttachedLiveData: LiveData<UsbDevice?>
        get() = mUsbDeviceAttachedFlow.asLiveData()


    init {
        usbManager.deviceList.values.firstOrNull()?.let { newUsbDeviceAttached(it) }
    }

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

    fun startConnection(device: UsbDevice?) {
        viewModelScope.launch {
            isConnectionInProgressFlow.emit(true)

            if (device != null) {
                secu3Connection.startUsbConnection(device)
            } else {
                secu3Connection.startBtConnection()
            }

            while (secu3Connection.isConnectionRunning && secu3Connection.isConnected.not() && secu3Connection.fwInfo == null) {

                delay(2000)
            }

            delay(1000) // to prevent change button state too fast in case of success

            isConnectionInProgressFlow.emit(false)
        }
    }

    fun newUsbDeviceAttached(device: UsbDevice) {
        viewModelScope.launch {
            usbDevice = device
            mUsbDeviceAttachedFlow.emit(usbDevice)
        }
    }

    fun newUsbDeviceDetached() {
        viewModelScope.launch {
            usbDevice = null
            mUsbDeviceAttachedFlow.emit(null)
        }
    }

}