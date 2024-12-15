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

package org.secu3.android.ui.dashboard

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.sample
import org.secu3.android.Secu3Repository
import org.secu3.android.models.packets.input.SensorsPacket
import org.secu3.android.ui.sensors.models.GaugeType
import org.secu3.android.utils.UserPrefs
import org.secu3.android.utils.Task
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(private val secu3Repository: Secu3Repository,
                                             private val mPrefs: UserPrefs) : ViewModel() {

    val packetLiveData: LiveData<DashboardViewData>
        get() = secu3Repository.receivedPacketFlow
            .filter { it is SensorsPacket }
            .sample(500)
            .map { (it as SensorsPacket) }
            .map {
                DashboardViewData(
                    it.rpm.toFloat(), it.temperature, it.speed, it.map, it.voltage,
                    it.checkEngineBit > 0,
                    it.gasBit > 0,
                    it.ephhValveBit > 0,
                    it.epmValveBit > 0,
                    it.carbBit > 0,
                    it.coolFanBit > 0,

                )
            }.asLiveData()

    val statusLiveData: LiveData<Boolean>
        get() = secu3Repository.connectionStatusLiveData

    val dashboardConfig: DashboardConfig = mPrefs.dashboardConfig ?: DashboardConfig(
        GaugeConfig(GaugeType.RPM, ),
        GaugeConfig(GaugeType.TEMPERATURE),
        GaugeConfig(GaugeType.VEHICLE_SPEED),
        GaugeConfig(GaugeType.MAP),
        GaugeConfig(GaugeType.VOLTAGE)
    )


    fun isBluetoothDeviceAddressNotSelected(): Boolean {
        return mPrefs.bluetoothDeviceName.isNullOrBlank()
    }

    fun setTask(task: Task) {
        secu3Repository.sendNewTask(task)
    }

}