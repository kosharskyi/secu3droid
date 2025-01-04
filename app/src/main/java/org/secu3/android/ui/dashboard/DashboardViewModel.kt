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
import org.secu3.android.connection.ConnectionState
import org.secu3.android.connection.Secu3Connection
import org.secu3.android.models.packets.input.SensorsPacket
import org.secu3.android.ui.sensors.models.GaugeType
import org.secu3.android.utils.UserPrefs
import org.secu3.android.utils.Task
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(private val secu3Connection: Secu3Connection,
                                             private val mPrefs: UserPrefs) : ViewModel() {

    val packetLiveData: LiveData<DashboardViewData>
        get() = secu3Connection.receivedPacketFlow
            .filter { it is SensorsPacket }
            .sample(300)
            .map { (it as SensorsPacket) }
            .map {
                DashboardViewData.inflate(dashboardConfig, it)
            }
            .asLiveData()

    val statusLiveData: LiveData<ConnectionState>
        get() = secu3Connection.connectionStateFlow.asLiveData()

    val dashboardConfig: DashboardConfig = mPrefs.dashboardConfig ?: DashboardConfig(
        GaugeConfig(GaugeType.RPM, ),
        GaugeConfig(GaugeType.TEMPERATURE),
        GaugeConfig(GaugeType.VEHICLE_SPEED),
        GaugeConfig(GaugeType.MAP),
        GaugeConfig(GaugeType.VOLTAGE)
    )

    fun saveConfig() {
        mPrefs.dashboardConfig = dashboardConfig
    }

    fun isBluetoothDeviceAddressNotSelected(): Boolean {
        return mPrefs.bluetoothDeviceName.isNullOrBlank()
    }

    fun setTask(task: Task) {
        secu3Connection.sendNewTask(task)
    }

}