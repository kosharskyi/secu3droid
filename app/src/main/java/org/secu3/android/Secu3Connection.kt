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
package org.secu3.android

import android.bluetooth.BluetoothManager
import android.hardware.usb.UsbDevice
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.secu3.android.connection.BtConnection
import org.secu3.android.connection.UsbConnection
import org.secu3.android.models.packets.base.BaseOutputPacket
import org.secu3.android.models.packets.base.BaseSecu3Packet
import org.secu3.android.models.packets.input.FirmwareInfoPacket
import org.secu3.android.models.packets.input.SensorsPacket
import org.secu3.android.utils.UserPrefs
import org.secu3.android.utils.SecuLogger
import org.secu3.android.utils.Task
import org.threeten.bp.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Secu3Connection @Inject constructor(private val usbConnection: UsbConnection,
                                          private val btConnection: BtConnection,
                                          private val mPrefs: UserPrefs,
                                          private val bluetoothManager: BluetoothManager,
                                          private val secuLogger: SecuLogger,
) {


    private val connectionScope = CoroutineScope(Dispatchers.Default + Job())


    private var lastPacketReceivedTimetamp = LocalDateTime.now().minusMinutes(1)

    val isConnectedFlow = flow {
        while (lastPacketReceivedTimetamp.isAfter(LocalDateTime.now().minusMinutes(10))) {
            delay(500)

            if (lastPacketReceivedTimetamp.isBefore(LocalDateTime.now().minusSeconds(5))) {
                emit(false)
            } else {
                emit(true)
            }
        }

        emit(false)
    }

    val isConnectedLiveData = isConnectedFlow.asLiveData()

    var fwInfo: FirmwareInfoPacket? = null

    val receivedPacketFlow: Flow<BaseSecu3Packet> = merge(usbConnection.receivedPacketFlow, btConnection.receivedPacketFlow).map {
        withContext(Dispatchers.IO) {
            it.parse(fwInfo)
        }
    }.filterNotNull()

    val firmwareLiveData: LiveData<FirmwareInfoPacket> = receivedPacketFlow.filter { it is FirmwareInfoPacket }
        .map { it as FirmwareInfoPacket }
        .asLiveData()


    val isConnectionRunning: Boolean
        get() = usbConnection.isRunning || btConnection.isRunning

    val isBtRunning: Boolean
        get() = btConnection.isRunning

    val isUsbRunning: Boolean
        get() = usbConnection.isRunning

    val isConnected: Boolean
        get() = usbConnection.isConnected || btConnection.isConnected

    val isUsbConnected: Boolean
        get() = usbConnection.isConnected

    val isBtConnected: Boolean
        get() = btConnection.isConnected

    fun sendNewTask(task: Task) {
        if (isUsbConnected) {
            usbConnection.sendData(task.getPacket())
        }

        if (isBtConnected) {
            btConnection.sendData(task.getPacket())
        }
    }

    fun sendOutPacket(packet: BaseOutputPacket) {
        if (isUsbConnected) {
            usbConnection.sendData(packet)
        }

        if (isBtConnected) {
            btConnection.sendData(packet)
        }
    }


    fun startConnect() {
        mPrefs.bluetoothDeviceName?.takeIf {  it.isNotEmpty() }?.let {
            if (bluetoothManager.adapter.isEnabled.not()) {
                return
            }
            startBtConnection()
        }
    }

    fun startUsbConnection(device: UsbDevice) {
        btConnection.stopConnection()
        usbConnection.stopConnection()
        usbConnection.startConnection(device)
    }

    fun startBtConnection() {
        usbConnection.stopConnection()
        btConnection.stopConnection()
        btConnection.startConnection()
    }

    fun disable() {
        usbConnection.stopConnection()
        btConnection.stopConnection()
    }

    init {

        connectionScope.launch {
            receivedPacketFlow.collect {
                lastPacketReceivedTimetamp = LocalDateTime.now()
            }
        }

        connectionScope.launch {
            receivedPacketFlow.filter { it is SensorsPacket }
                .map { it as SensorsPacket }
                .collect {
                    if (mPrefs.isSensorLoggerEnabled) {
                        secuLogger.logPacket(it)
                    }
                }
        }

        connectionScope.launch {
            fwInfo = receivedPacketFlow.first { it is FirmwareInfoPacket } as FirmwareInfoPacket
        }

//        repositoryScope.launch {
//            while (lastPacketReceivedTimetamp.isAfter(LocalDateTime.now().minusMinutes(10))) {
//                delay(1000)
//
//                if (tryToConnect.not()) {
//                    continue
//                }
//
//                if (mPrefs.bluetoothDeviceName.isNullOrBlank()) {
//                    continue
//                }
//
//                if (bluetoothManager.adapter.isEnabled.not()) {
//                    continue
//                }
//
//                if (usbConnection.isRunning.not() && usbConnection.connectionAttempts == 0) {
//                    usbConnection.startConnection()
//                    delay(2000)
//                    continue
//                }
//
//                delay(2000)
//            }
//        }
    }
}
