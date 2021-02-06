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

package org.secu3.android.ui.errors

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import org.secu3.android.Secu3Repository
import org.secu3.android.models.packets.CheckEngineErrorsPacket
import org.secu3.android.models.packets.CheckEngineSavedErrorsPacket
import org.secu3.android.utils.Task
import javax.inject.Inject

@HiltViewModel
class ErrorsViewModel @Inject constructor(private val secu3Repository: Secu3Repository) : ViewModel() {

    init {
        sendNewTask(Task.Secu3ReadEcuSavedErrors)
    }

    val connectionStatusLiveData: LiveData<Boolean>
        get() = secu3Repository.connectionStatusLiveData

    val checkEngineLiveData: LiveData<CheckEngineErrorsPacket>
        get() = secu3Repository.receivedPacketLiveData.filter { it is CheckEngineErrorsPacket }
            .map { it as CheckEngineErrorsPacket }.asLiveData()

    val checkEngineSavedLiveData: LiveData<CheckEngineSavedErrorsPacket>
        get() = secu3Repository.receivedPacketLiveData.filter { it is CheckEngineSavedErrorsPacket }
            .map { it as CheckEngineSavedErrorsPacket }.asLiveData()


    fun sendNewTask(task: Task) {
        secu3Repository.sendNewTask(task)
    }
}
