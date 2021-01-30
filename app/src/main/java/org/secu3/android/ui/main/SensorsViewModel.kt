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
package org.secu3.android.ui.main

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import org.secu3.android.Secu3Repository
import org.secu3.android.models.packets.AdcRawDatPacket
import org.secu3.android.models.packets.FirmwareInfoPacket
import org.secu3.android.models.packets.SensorsPacket
import org.secu3.android.utils.LifeTimePrefs
import org.secu3.android.utils.Task
import org.threeten.bp.Duration
import org.threeten.bp.LocalDateTime
import javax.inject.Inject

@HiltViewModel
class SensorsViewModel @Inject constructor(private val secu3Repository: Secu3Repository,
                                           private val mPrefs: LifeTimePrefs
) : ViewModel() {


    fun isBluetoothDeviceAddressNotSelected(): Boolean {
        return mPrefs.bluetoothDeviceAddress.isNullOrBlank()
    }

    var lastPacketReceivedStamp: LocalDateTime = LocalDateTime.now()

    val connectionStatusLiveData: LiveData<Boolean>
        get() = secu3Repository.connectionStatusLiveData


    val firmwareLiveData: LiveData<FirmwareInfoPacket>
        get() = secu3Repository.firmwareLiveData


    private val mSensorsDataLiveData = MediatorLiveData<SensorsPacket>().also {
        it.addSource(secu3Repository.receivedPacketLiveData) { packet ->
            if (packet is SensorsPacket) {
                it.value = packet
            }
        }
    }
    val sensorsLiveData: LiveData<SensorsPacket>
        get() = mSensorsDataLiveData



    private val mRawSensorsDataLiveData = MediatorLiveData<AdcRawDatPacket>().also {
        it.addSource(secu3Repository.receivedPacketLiveData) { packet ->
            if (packet is AdcRawDatPacket) {
                if (Duration.between(lastPacketReceivedStamp, LocalDateTime.now()).toMillis() > 500) {
                    it.value = packet
                    lastPacketReceivedStamp = LocalDateTime.now()
                }
            }
        }
    }
    val rawSensorsLiveData: LiveData<AdcRawDatPacket>
        get() = mRawSensorsDataLiveData


    fun start() {
        secu3Repository.startConnect()
    }

    fun sendNewTask(task: Task) {
        secu3Repository.sendNewTask(task)
    }

    fun closeConnection() {
        secu3Repository.disable()
    }
}
