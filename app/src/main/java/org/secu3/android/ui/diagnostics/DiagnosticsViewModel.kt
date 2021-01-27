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

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.secu3.android.Secu3Repository
import org.secu3.android.models.packets.DiagInputPacket
import org.secu3.android.models.packets.DiagOutputPacket
import org.secu3.android.models.packets.FirmwareInfoPacket
import org.secu3.android.models.packets.OpCompNc
import org.secu3.android.utils.Task

class DiagnosticsViewModel @ViewModelInject constructor(private val secu3Repository: Secu3Repository) : ViewModel() {

    val outputPacket = DiagOutputPacket(secu3Repository.fwInfo)

    var isDiagModeActive = false

    init {
        secu3Repository.receivedPacketLiveData.observeForever {
            if (it is OpCompNc) {
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

    private val mDiagInputLiveData = MediatorLiveData<DiagInputPacket>().also {
        it.addSource(secu3Repository.receivedPacketLiveData) { packet ->
            if (packet is DiagInputPacket) {
                isDiagModeActive = true
                it.value = packet
            }
        }
    }
    val diagInputLiveData: LiveData<DiagInputPacket>
        get() = mDiagInputLiveData


    fun sendDiagOutPacket() {
        secu3Repository.sendOutPacket(outputPacket)
    }
}