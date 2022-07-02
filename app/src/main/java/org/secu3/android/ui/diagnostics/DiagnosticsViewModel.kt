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
package org.secu3.android.ui.diagnostics

import androidx.lifecycle.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.secu3.android.Secu3Repository
import org.secu3.android.models.packets.DiagInputPacket
import org.secu3.android.models.packets.DiagOutputPacket
import org.secu3.android.models.packets.FirmwareInfoPacket
import org.secu3.android.models.packets.OpCompNc
import org.secu3.android.utils.Task
import javax.inject.Inject

@HiltViewModel
class DiagnosticsViewModel @Inject constructor(private val secu3Repository: Secu3Repository) : ViewModel() {

    val outputPacket = DiagOutputPacket(secu3Repository.fwInfo!!)

    private var isDiagModeActive = false

    init {
        viewModelScope.launch {
            secu3Repository.receivedPacketLiveData.filter { it is OpCompNc }.map {
                it as OpCompNc
            }.collect {
                if (it.opCode == 7) {
                    isDiagModeActive = false
                    mConfirmExit.value = true
                }

                if (it.opCode == 6) {
                    isDiagModeActive = true
                    secu3Repository.sendNewTask(Task.Secu3DiagInput)
                }
            }
        }

        secu3Repository.sendNewTask(Task.Secu3EnterDiagnostics)
    }

    val firmwareLiveData: LiveData<FirmwareInfoPacket>
        get() = secu3Repository.firmwareLiveData

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
        secu3Repository.sendNewTask(Task.Secu3LeaveDiagnostics)
        secu3Repository.sendNewTask(Task.Secu3ReadSensors)
    }

    val connectionStatusLiveData: LiveData<Boolean>
        get() = secu3Repository.connectionStatusLiveData

    val diagInputLiveData: LiveData<DiagInputPacket>
        get() = secu3Repository.receivedPacketLiveData.filter { it is DiagInputPacket }.map {
            isDiagModeActive = true
            it as DiagInputPacket
        }.asLiveData()


    fun sendDiagOutPacket() {
        secu3Repository.sendOutPacket(outputPacket)
    }
}