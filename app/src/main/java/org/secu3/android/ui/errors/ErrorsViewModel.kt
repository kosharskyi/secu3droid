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

package org.secu3.android.ui.errors

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import org.secu3.android.connection.ConnectionState
import org.secu3.android.connection.Secu3ConnectionManager
import org.secu3.android.models.packets.input.CheckEngineErrorsPacket
import org.secu3.android.models.packets.input.CheckEngineSavedErrorsPacket
import org.secu3.android.utils.Task
import javax.inject.Inject

@HiltViewModel
class ErrorsViewModel @Inject constructor(private val secu3ConnectionManager: Secu3ConnectionManager) : ViewModel() {

    val connectionStatusLiveData: LiveData<ConnectionState>
        get() = secu3ConnectionManager.connectionStateFlow.asLiveData()

    val checkEngineSavedLiveData: LiveData<CheckEngineSavedErrorsPacket>
        get() = flow {
            secu3ConnectionManager.sendNewTask(Task.Secu3ReadEcuSavedErrors)

            val packet = secu3ConnectionManager.receivedPacketFlow.first { it is CheckEngineSavedErrorsPacket } as CheckEngineSavedErrorsPacket

            emit(packet)
            secu3ConnectionManager.sendNewTask(Task.Secu3ReadEcuErrors)
        }.asLiveData()

    val checkEngineLiveData: LiveData<CheckEngineErrorsPacket>
        get() = secu3ConnectionManager.receivedPacketFlow.filter { it is CheckEngineErrorsPacket }
            .map { it as CheckEngineErrorsPacket }.asLiveData()
}
