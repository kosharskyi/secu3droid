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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.sample
import org.secu3.android.connection.BluetoothDeviceAddress
import org.secu3.android.connection.Connected
import org.secu3.android.connection.ConnectionState
import org.secu3.android.connection.Secu3ConnectionManager
import org.secu3.android.models.packets.base.InputPacket
import org.secu3.android.models.packets.input.SensorsPacket
import org.secu3.android.utils.UserPrefs
import org.secu3.android.utils.Task
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(private val secu3ConnectionManager: Secu3ConnectionManager,
                                             private val mPrefs: UserPrefs) : ViewModel() {

    val packetLiveData: LiveData<DashboardViewData?> = dashboardPacketFlow(
        secu3ConnectionManager.connectionStateFlow,
        secu3ConnectionManager.receivedPacketFlow,
    ).asLiveData()

    val statusLiveData: LiveData<ConnectionState> = secu3ConnectionManager.connectionStateFlow.asLiveData()

    val isKeepScreenAliveActive: Boolean
        get() = mPrefs.isKeepScreenAliveActive

    fun isBluetoothDeviceAddressNotSelected(): Boolean {
        return BluetoothDeviceAddress.normalize(mPrefs.bluetoothDeviceAddress) == null
    }

    fun setTask(task: Task) {
        secu3ConnectionManager.sendNewTask(task)
    }

}

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class, kotlinx.coroutines.FlowPreview::class)
internal fun dashboardPacketFlow(
    connectionStates: Flow<ConnectionState>,
    packets: Flow<InputPacket>,
): Flow<DashboardViewData?> = connectionStates.flatMapLatest { state ->
    if (state != Connected) {
        flowOf(null)
    } else {
        packets
            .filter { it is SensorsPacket }
            .sample(300)
            .map { DashboardViewData.inflate(it as SensorsPacket) }
    }
}
