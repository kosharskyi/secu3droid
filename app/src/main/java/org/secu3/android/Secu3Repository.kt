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
import androidx.lifecycle.asLiveData
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
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

    private val repositoryJob = Job()
    private val repositoryScope = CoroutineScope(Dispatchers.Default + repositoryJob)


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

        emit(false)
    }

    val connectionStatusLiveData = connectionStatus.asLiveData()

    var fwInfo: FirmwareInfoPacket? = null

    val firmwareLiveData: LiveData<FirmwareInfoPacket> = secu3Manager.receivedPacketFlow.filter { it is FirmwareInfoPacket }
        .map { it as FirmwareInfoPacket }
        .asLiveData()

    val receivedPacketLiveData: Flow<BaseSecu3Packet> = secu3Manager.receivedPacketFlow


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


    init {

        repositoryScope.launch {
            secu3Manager.receivedPacketFlow.collect {
                lastPacketReceivedTimetamp = LocalDateTime.now()
            }
        }

        repositoryScope.launch {
            fwInfo = secu3Manager.receivedPacketFlow.first { it is FirmwareInfoPacket } as FirmwareInfoPacket
        }

        repositoryScope.launch {
            while (lastPacketReceivedTimetamp.isAfter(LocalDateTime.now().minusMinutes(10))) {
                delay(1000)

                if (tryToConnect.not()) {
                    continue
                }

                if (mPrefs.bluetoothDeviceName.isNullOrBlank()) {
                    continue
                }

                if (bluetoothAdapter.isEnabled.not()) {
                    continue
                }

                if (secu3Manager.connectedThread == null) {
                    secu3Manager.start()
                    delay(2000)
                    continue
                }

                secu3Manager.connectedThread?.mmSocket?.let {
                    if (it.isConnected.not()) {
                        secu3Manager.disable()
                        secu3Manager.start()
                    }
                }
                delay(2000)
            }
        }
    }
}