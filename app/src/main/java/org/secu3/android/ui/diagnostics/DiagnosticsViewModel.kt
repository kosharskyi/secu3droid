/*
 *    SecuDroid  - An open source, free manager for SECU-3 engine control unit
 *    Copyright (C) 2024 Vitalii O. Kosharskyi. Ukraine, Kyiv
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

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.secu3.android.connection.ConnectionState
import org.secu3.android.connection.Secu3ConnectionManager
import org.secu3.android.models.packets.input.DiagInputPacket
import org.secu3.android.models.packets.out.DiagOutputPacket
import org.secu3.android.models.packets.input.FirmwareInfoPacket
import org.secu3.android.models.packets.out.OpCompNc
import org.secu3.android.utils.Task
import javax.inject.Inject

@HiltViewModel
class DiagnosticsViewModel @Inject constructor(private val secu3ConnectionManager: Secu3ConnectionManager) : ViewModel() {

    private val outputPacket = DiagOutputPacket()
    private val outputController = DiagnosticOutputController(outputPacket, ::sendDiagOutPacket)

    private val session = DiagnosticsSessionController(secu3ConnectionManager::sendNewTask)

    val firmwareInfo: FirmwareInfoPacket?
        get() = secu3ConnectionManager.fwInfo

    val firmwareLiveData: LiveData<FirmwareInfoPacket>
        get() = secu3ConnectionManager.firmwareLiveData

    data class UiState(
        val outputBits: Int = 0,
        val channel: Int = 0,
        val frequency: Float = 10f,
        val duty: Float = 50f,
        val enableBlDe: Boolean = false,
        val enableTachO: Boolean = false,
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        secu3ConnectionManager.fwInfo?.let { firmware ->
            outputController.initializeIgnitionOutputs(firmware.isSecu3T)
            publishOutputState()
        }
        viewModelScope.launch {
            secu3ConnectionManager.receivedPacketFlow.filter { it is OpCompNc }.map {
                it as OpCompNc
            }.collect {
                session.onOpCode(it.opCode)
            }
        }
        session.start()
    }

    // Each asLiveData() call creates a Flow collector; keep one instance for this
    // view model so Compose recompositions and view recreation do not multiply them.
    val connectionStatusLiveData: LiveData<ConnectionState> by lazy {
        secu3ConnectionManager.connectionStateFlow.asLiveData()
    }

    val diagInputLiveData: LiveData<DiagInputPacket> by lazy {
        secu3ConnectionManager.receivedPacketFlow.filter { it is DiagInputPacket }.map {
            val input = it as DiagInputPacket
            outputController.onDiagnosticInput(isSecu3T = input.flags != 0)
            publishOutputState()
            session.onInputReceived()
            input
        }.asLiveData()
    }


    fun enableTachO(enable: Boolean) {
        outputController.enableTachO(enable)

        _uiState.value = _uiState.value.copy(enableTachO = enable, outputBits = outputPacket.out)
    }

    fun enableBlDe(enable: Boolean) {
        outputController.enableBlDe(enable)

        _uiState.value = _uiState.value.copy(enableBlDe = enable, outputBits = outputPacket.out)
    }

    fun sendDiagOutPacket() {
        secu3ConnectionManager.sendOutPacket(outputPacket)
    }

    internal fun setOutput(output: DiagnosticOutput, enabled: Boolean) {
        outputController.setOutput(output, enabled)
        publishOutputState()
    }

    fun selectChannel(index: Int) {
        outputController.selectChannel(index)
        publishOutputState()
    }

    fun editFrequency(value: Float) {
        outputController.editFrequency(value)
        publishOutputState()
    }

    fun editDuty(value: Float) {
        outputController.editDuty(value)
        publishOutputState()
    }

    fun applyAdditionalOptions(enableBlDe: Boolean, enableTachO: Boolean) {
        outputController.applyAdditionalOptions(enableBlDe, enableTachO)
        _uiState.value = _uiState.value.copy(enableBlDe = enableBlDe, enableTachO = enableTachO, outputBits = outputPacket.out)
    }

    private fun publishOutputState() {
        _uiState.value = _uiState.value.copy(
            outputBits = outputPacket.out,
            channel = outputPacket.chan,
            frequency = outputPacket.frq,
            duty = outputPacket.duty,
        )
    }

    override fun onCleared() {
        session.close()
    }
}

internal class DiagnosticsSessionController(private val sendTask: (Task) -> Unit) {
    var isActive: Boolean = false
        private set
    private var started = false
    private var closed = false

    fun start() {
        if (started || closed) return
        started = true
        sendTask(Task.Secu3OpComEnterDiagnostics)
    }

    fun onOpCode(opCode: Int) {
        if (!started || closed) return
        when (opCode) {
            6 -> { isActive = true; sendTask(Task.Secu3DiagInput) }
            7 -> isActive = false
        }
    }

    fun onInputReceived() {
        if (started && !closed) isActive = true
    }

    fun close() {
        if (closed) return
        closed = true
        isActive = false
        if (!started) return
        sendTask(Task.Secu3OpComLeaveDiagnostics)
        sendTask(Task.Secu3ReadSensors)
    }
}
