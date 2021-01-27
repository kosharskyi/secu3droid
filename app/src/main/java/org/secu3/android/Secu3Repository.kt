/* SecuDroid  - An open source, free manager for SECU-3 engine control unit
   Copyright (C) 2020 Vitaliy O. Kosharskiy. Ukraine, Kharkiv

   SECU-3  - An open source, free engine control unit
   Copyright (C) 2007 Alexey A. Shabelnikov. Ukraine, Kyiv

   This program is free software: you can redistribute it and/or modify
   it under the terms of the GNU General Public License as published by
   the Free Software Foundation, either version 3 of the License, or
   (at your option) any later version.

   This program is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.

   You should have received a copy of the GNU General Public License
   along with this program.  If not, see <http://www.gnu.org/licenses/>.

   contacts:
              http://secu-3.org
              email: vetalkosharskiy@gmail.com
*/
package org.secu3.android

import android.bluetooth.BluetoothAdapter
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.asLiveData
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import org.secu3.android.models.packets.BaseOutputPacket
import org.secu3.android.models.packets.BaseSecu3Packet
import org.secu3.android.models.packets.FirmwareInfoPacket
import org.secu3.android.utils.LifeTimePrefs
import org.secu3.android.utils.Task
import org.threeten.bp.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Secu3Repository @Inject constructor(private val secu3Manager: Secu3Manager,
                                          private val mPrefs: LifeTimePrefs
) {
    private var lastPacketReceivedTimetamp = LocalDateTime.now().minusMinutes(1)
    private var tryToConnect = false

    private val bluetoothAdapter: BluetoothAdapter by lazy { BluetoothAdapter.getDefaultAdapter() }

    private val connectionStatus = flow {
        while (lastPacketReceivedTimetamp.isAfter(LocalDateTime.now().minusMinutes(10))) {
            delay(500)

            if (lastPacketReceivedTimetamp.isBefore(LocalDateTime.now().minusSeconds(5))) {
                emit(false)
            } else {
                emit(true)
            }
        }
    }

    private val connectionWatchdog = GlobalScope.launch {
        while (lastPacketReceivedTimetamp.isAfter(LocalDateTime.now().minusMinutes(10))) {
            delay(1000)

            if (tryToConnect.not()) {
                continue
            }

            if (mPrefs.bluetoothDeviceAddress.isNullOrBlank()) {
                continue
            }

            if (bluetoothAdapter.isEnabled.not()) {
                continue
            }

            if (secu3Manager.connectedThread == null) {
                secu3Manager.start()
                continue
            }

            secu3Manager.connectedThread?.mmSocket?.let {
                if (it.isConnected.not()) {
                    secu3Manager.disable()
                    secu3Manager.start()
                }
            }
        }
    }

    val connectionStatusLiveData = connectionStatus.asLiveData()


    private val mPacketsLiveData = MediatorLiveData<BaseSecu3Packet>().also {
        it.addSource(secu3Manager.receivedPacketLiveData) { packet ->

            lastPacketReceivedTimetamp = LocalDateTime.now()

            if (packet is FirmwareInfoPacket) {
                fwInfo = packet
                mFirmwareLiveData.value = packet
            } else {
                it.value = packet
            }
        }
    }



    lateinit var fwInfo: FirmwareInfoPacket

    private val mFirmwareLiveData = MediatorLiveData<FirmwareInfoPacket>()
    val firmwareLiveData: LiveData<FirmwareInfoPacket>
        get() = mFirmwareLiveData

    val receivedPacketLiveData: LiveData<BaseSecu3Packet>
        get() = mPacketsLiveData


    fun sendNewTask(task: Task) {
        secu3Manager.addPacket(task.getPacket())
    }

    fun sendOutPacket(packet: BaseOutputPacket) {
        secu3Manager.addPacket(packet)
    }

    fun startConnect() {
        tryToConnect = true
    }

    fun disable() {
        tryToConnect = false
        secu3Manager.disable()
    }

}