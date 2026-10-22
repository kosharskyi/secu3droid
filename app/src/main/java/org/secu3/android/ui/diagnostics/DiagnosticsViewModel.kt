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
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.secu3.android.connection.ConnectionState
import org.secu3.android.connection.Connected
import org.secu3.android.connection.Secu3ConnectionManager
import org.secu3.android.models.packets.input.DiagInputPacket
import org.secu3.android.models.packets.out.DiagOutputPacket
import org.secu3.android.models.packets.input.FirmwareInfoPacket
import org.secu3.android.models.packets.base.InputPacket
import org.secu3.android.models.packets.out.OpCompNc
import org.secu3.android.utils.Task
import javax.inject.Inject

@HiltViewModel
class DiagnosticsViewModel @Inject constructor(private val secu3ConnectionManager: Secu3ConnectionManager) : ViewModel() {

    private val outputPacket = DiagOutputPacket()
    private val outputController = DiagnosticOutputController(outputPacket, ::sendDiagOutPacket)

    private val session = DiagnosticsSessionController(secu3ConnectionManager::sendNewTask)
    private var connectionAvailable = false
    private var hasFreshInput = false
    private val _diagInput = MutableLiveData<DiagInputPacket?>(null)
    private val _connectionStatus = MutableLiveData<ConnectionState>()

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
        val controlsEnabled: Boolean = false,
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        secu3ConnectionManager.fwInfo?.let { firmware ->
            outputController.initializeIgnitionOutputs(firmware.isSecu3T)
            publishOutputState()
        }
        session.start()
        viewModelScope.launch {
            secu3ConnectionManager.connectionStateFlow.collectLatest { state ->
                _connectionStatus.value = state
                if (state == Connected && secu3ConnectionManager.isConnected) {
                    secu3ConnectionManager.receivedPacketFlow(onSubscribed = {
                        if (secu3ConnectionManager.isConnected) {
                            connectionAvailable = true
                            session.onConnectionChanged(true)
                            publishControlAvailability()
                        } else {
                            handleConnectionLoss()
                        }
                    }).collect(::onReceivedPacket)
                } else {
                    handleConnectionLoss()
                }
            }
        }
    }

    val connectionStatusLiveData: LiveData<ConnectionState>
        get() = _connectionStatus

    val diagInputLiveData: LiveData<DiagInputPacket?>
        get() = _diagInput

    private fun onReceivedPacket(packet: InputPacket) {
        if (!connectionAvailable || !secu3ConnectionManager.isConnected) {
            handleConnectionLoss()
            return
        }
        when (packet) {
            is OpCompNc -> {
                session.onOpCode(packet.opCode)
                publishControlAvailability()
            }
            is DiagInputPacket -> onDiagnosticInput(packet)
        }
    }

    private fun onDiagnosticInput(input: DiagInputPacket) {
        if (!connectionAvailable || !secu3ConnectionManager.isConnected) {
            handleConnectionLoss()
            return
        }
        hasFreshInput = true
        session.onInputReceived()
        _diagInput.value = input
        outputController.onDiagnosticInput(isSecu3T = input.flags != 0)
        publishOutputState()
    }


    fun enableTachO(enable: Boolean) {
        if (!canControl()) return
        outputController.enableTachO(enable)

        _uiState.value = _uiState.value.copy(enableTachO = enable, outputBits = outputPacket.out)
    }

    fun enableBlDe(enable: Boolean) {
        if (!canControl()) return
        outputController.enableBlDe(enable)

        _uiState.value = _uiState.value.copy(enableBlDe = enable, outputBits = outputPacket.out)
    }

    fun sendDiagOutPacket() {
        if (!canControl()) return
        secu3ConnectionManager.sendOutPacket(outputPacket)
    }

    internal fun setOutput(output: DiagnosticOutput, enabled: Boolean) {
        if (!canControl()) return
        outputController.setOutput(output, enabled)
        publishOutputState()
    }

    fun selectChannel(index: Int) {
        if (!canControl()) return
        outputController.selectChannel(index)
        publishOutputState()
    }

    fun editFrequency(value: Float) {
        if (!canControl()) return
        outputController.editFrequency(value)
        publishOutputState()
    }

    fun editDuty(value: Float) {
        if (!canControl()) return
        outputController.editDuty(value)
        publishOutputState()
    }

    fun applyAdditionalOptions(enableBlDe: Boolean, enableTachO: Boolean) {
        if (!canControl()) return
        outputController.applyAdditionalOptions(enableBlDe, enableTachO)
        _uiState.value = _uiState.value.copy(enableBlDe = enableBlDe, enableTachO = enableTachO, outputBits = outputPacket.out)
    }

    private fun publishOutputState() {
        _uiState.value = _uiState.value.copy(
            outputBits = outputPacket.out,
            channel = outputPacket.chan,
            frequency = outputPacket.frq,
            duty = outputPacket.duty,
            enableBlDe = outputPacket.blOutputEnabled && outputPacket.deOutputEnabled,
            enableTachO = outputPacket.tachOOutputEnabled,
            controlsEnabled = canControl(),
        )
    }

    private fun publishControlAvailability() {
        _uiState.value = _uiState.value.copy(controlsEnabled = canControl())
    }

    private fun canControl(): Boolean =
        connectionAvailable && secu3ConnectionManager.isConnected && hasFreshInput && session.isActive

    private fun handleConnectionLoss() {
        connectionAvailable = false
        hasFreshInput = false
        session.onConnectionChanged(false)
        _diagInput.value = null
        outputController.resetForConnectionLoss(secu3ConnectionManager.fwInfo?.isSecu3T)
        publishOutputState()
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
    private var connectionAvailable = false
    private var enterRequested = false
    private var inputRequested = false

    fun onConnectionChanged(available: Boolean) {
        if (closed) return
        connectionAvailable = available
        if (available) {
            requestEnterIfNeeded()
        } else {
            isActive = false
            enterRequested = false
            inputRequested = false
        }
    }

    fun start() {
        if (started || closed) return
        started = true
        requestEnterIfNeeded()
    }

    private fun requestEnterIfNeeded() {
        if (!started || closed || !connectionAvailable || enterRequested) return
        enterRequested = true
        inputRequested = false
        sendTask(Task.Secu3OpComEnterDiagnostics)
    }

    fun onOpCode(opCode: Int) {
        if (!started || closed || !connectionAvailable || !enterRequested) return
        when (opCode) {
            6 -> {
                if (!inputRequested) {
                    inputRequested = true
                    sendTask(Task.Secu3DiagInput)
                }
            }
            7 -> isActive = false
        }
    }

    fun onInputReceived() {
        if (started && !closed && connectionAvailable && enterRequested) isActive = true
    }

    fun close() {
        if (closed) return
        val shouldLeave = started && connectionAvailable && enterRequested
        closed = true
        connectionAvailable = false
        isActive = false
        if (shouldLeave) {
            sendTask(Task.Secu3OpComLeaveDiagnostics)
            sendTask(Task.Secu3ReadSensors)
        }
    }
}
