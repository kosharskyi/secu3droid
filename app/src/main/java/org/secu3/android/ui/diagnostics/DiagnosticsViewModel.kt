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
package org.secu3.android.ui.diagnostics

import androidx.lifecycle.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.secu3.android.connection.Secu3Connection
import org.secu3.android.models.packets.input.DiagInputPacket
import org.secu3.android.models.packets.out.DiagOutputPacket
import org.secu3.android.models.packets.input.FirmwareInfoPacket
import org.secu3.android.models.packets.out.OpCompNc
import org.secu3.android.utils.Task
import javax.inject.Inject

@HiltViewModel
class DiagnosticsViewModel @Inject constructor(private val secu3Connection: Secu3Connection) : ViewModel() {

    val outputPacket = DiagOutputPacket(secu3Connection.fwInfo!!)

    private var isDiagModeActive = false

    init {
        viewModelScope.launch {
            secu3Connection.receivedPacketFlow.filter { it is OpCompNc }.map {
                it as OpCompNc
            }.collect {
                if (it.opCode == 7) {
                    isDiagModeActive = false
                    mConfirmExit.value = true
                }

                if (it.opCode == 6) {
                    isDiagModeActive = true
                    secu3Connection.sendNewTask(Task.Secu3DiagInput)
                }
            }
        }

        secu3Connection.sendNewTask(Task.Secu3OpComEnterDiagnostics)
    }

    val firmwareLiveData: LiveData<FirmwareInfoPacket>
        get() = secu3Connection.firmwareLiveData

    private val mConfirmExit = MutableLiveData<Boolean>()
    val confirmExit: LiveData<Boolean>
        get() = mConfirmExit


    fun toggleBlDe() {
        mEnableBlDe.value = true
    }

    private val mEnableBlDe = MutableLiveData<Boolean>()
    val enableBlDe: LiveData<Boolean>
        get() = mEnableBlDe


    fun leaveDiagnostic() {
        secu3Connection.sendNewTask(Task.Secu3OpComLeaveDiagnostics)
        secu3Connection.sendNewTask(Task.Secu3ReadSensors)
    }

    val connectionStatusLiveData: LiveData<Boolean>
        get() = secu3Connection.isConnectedLiveData

    val diagInputLiveData: LiveData<DiagInputPacket>
        get() = secu3Connection.receivedPacketFlow.filter { it is DiagInputPacket }.map {
            isDiagModeActive = true
            it as DiagInputPacket
        }.asLiveData()


    fun sendDiagOutPacket() {
        secu3Connection.sendOutPacket(outputPacket)
    }
}