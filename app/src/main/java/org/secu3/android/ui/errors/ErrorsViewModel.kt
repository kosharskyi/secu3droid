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
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.secu3.android.connection.ConnectionState
import org.secu3.android.connection.Secu3ConnectionManager
import org.secu3.android.models.packets.input.CheckEngineErrorsPacket
import org.secu3.android.models.packets.out.CheckEngineSavedErrorsPacket
import org.secu3.android.utils.Task
import javax.inject.Inject

internal suspend fun readSavedErrors(
    packets: (onSubscribed: () -> Unit) -> Flow<org.secu3.android.models.packets.base.InputPacket>,
    sendRequest: () -> Unit,
    onReceived: (CheckEngineSavedErrorsPacket) -> Unit,
): CheckEngineSavedErrorsPacket {
    val savedErrors = packets { sendRequest() }
        .filterIsInstance<CheckEngineSavedErrorsPacket>()
        .first()
    onReceived(savedErrors)
    return savedErrors
}

@HiltViewModel
class ErrorsViewModel @Inject constructor(private val secu3ConnectionManager: Secu3ConnectionManager) : ViewModel() {

    val connectionStatusLiveData: LiveData<ConnectionState> = secu3ConnectionManager.connectionStateFlow.asLiveData()

    private val _checkEngineSavedLiveData = MutableLiveData<CheckEngineSavedErrorsPacket>()
    val checkEngineSavedLiveData: LiveData<CheckEngineSavedErrorsPacket> = _checkEngineSavedLiveData

    val checkEngineLiveData: LiveData<CheckEngineErrorsPacket> = secu3ConnectionManager.receivedPacketFlow
        .filterIsInstance<CheckEngineErrorsPacket>().distinctUntilChangedBy { it.errors }.asLiveData()

    private val _showClearConfirmation = MutableStateFlow(false)
    val showClearConfirmation: StateFlow<Boolean> = _showClearConfirmation.asStateFlow()

    init {
        waitSavedErrors()
    }

    private fun waitSavedErrors() {
        viewModelScope.launch {
            readSavedErrors(
                packets = { onSubscribed -> secu3ConnectionManager.receivedPacketFlow(onSubscribed) },
                sendRequest = { secu3ConnectionManager.sendNewTask(Task.Secu3ReadEcuSavedErrors) },
                onReceived = { _checkEngineSavedLiveData.value = it },
            )
            secu3ConnectionManager.sendNewTask(Task.Secu3ReadEcuErrors)
        }
    }

    fun clearErrors() {
        waitSavedErrors()
        secu3ConnectionManager.sendOutPacket(CheckEngineSavedErrorsPacket())
    }

    fun requestClearConfirmation() {
        _showClearConfirmation.value = true
    }

    fun dismissClearConfirmation() {
        _showClearConfirmation.value = false
    }

    fun confirmClearErrors() {
        _showClearConfirmation.value = false
        clearErrors()
    }

}
