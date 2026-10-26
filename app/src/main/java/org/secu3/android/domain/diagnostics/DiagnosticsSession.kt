/*
 *    SecuDroid  - An open source, free manager for SECU-3 engine control unit
 *    Copyright (C) 2026 Vitalii O. Kosharskyi. Ukraine, Kyiv
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 */
package org.secu3.android.domain.diagnostics

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.secu3.android.connection.Connected
import org.secu3.android.connection.ConnectionState
import org.secu3.android.connection.Secu3ConnectionManager
import org.secu3.android.models.packets.base.InputPacket
import org.secu3.android.models.packets.input.DiagInputPacket
import org.secu3.android.models.packets.out.DiagOutputPacket
import org.secu3.android.models.packets.out.OpCompNc
import org.secu3.android.utils.Task
import javax.inject.Inject

enum class DiagnosticOutput(val bit: Int) {
    IGN_OUT1(0), IGN_OUT2(1), IGN_OUT3(2), IGN_OUT4(3),
    IE(4), FE(5), ECF(6), CE(7), ST_BLOCK(8), ADD_IO1(9), ADD_IO2(10),
    IGN_OUT5(4), ECF0(5), INJ_O1(6), INJ_O2(7), INJ_O3(8),
    INJ_O4(9), INJ_O5(10), BL(11), DE(13), STBL_O(15),
    CEL_O(16), FPMP_O(17), PWRR_O(18), EVAP_O(19), O2SH_O(20),
    COND_O(21), ADD_O2(22), TACH_O(23), GPA6_O(25);

    companion object {
        val secu3TChannels = listOf(IGN_OUT1, IGN_OUT2, IGN_OUT3, IGN_OUT4, IE, FE, ECF, CE, ST_BLOCK, ADD_IO1, ADD_IO2, BL, DE)
        val secu3IChannels = listOf(IGN_OUT1, IGN_OUT2, IGN_OUT3, IGN_OUT4, IGN_OUT5, ECF0, INJ_O1, INJ_O2, INJ_O3, INJ_O4, INJ_O5, BL, DE, STBL_O, CEL_O, FPMP_O, PWRR_O, EVAP_O, O2SH_O, COND_O, ADD_O2, TACH_O, GPA6_O)
    }
}

data class DiagnosticsState(
    val input: DiagInputPacket? = null,
    val outputBits: Int = 0,
    val channel: Int = 0,
    val frequency: Float = 10f,
    val duty: Float = 50f,
    val generatorOutput: DiagnosticOutput? = null,
    val enableBlDe: Boolean = false,
    val enableTachO: Boolean = false,
    val controlsEnabled: Boolean = false,
)

/** Owns the diagnostic protocol session and its ECU output packet. */
class DiagnosticsSession internal constructor(
    private val connectionStates: Flow<ConnectionState>,
    private val packets: (onSubscribed: () -> Unit) -> Flow<InputPacket>,
    private val isTransportConnected: () -> Boolean,
    private val initialFirmwareFamily: () -> Boolean?,
    private val sendTask: (Task) -> Unit,
    private val sendPacket: (DiagOutputPacket) -> Unit,
) {
    @Inject
    constructor(connectionManager: Secu3ConnectionManager) : this(
        connectionStates = connectionManager.connectionStateFlow,
        packets = connectionManager::receivedPacketFlow,
        isTransportConnected = { connectionManager.isConnected },
        initialFirmwareFamily = { connectionManager.fwInfo?.isSecu3T },
        sendTask = connectionManager::sendNewTask,
        sendPacket = connectionManager::sendOutPacket,
    )

    private val packet = DiagOutputPacket()
    private val _state = MutableStateFlow(DiagnosticsState())
    val state: StateFlow<DiagnosticsState> = _state.asStateFlow()
    private var connectionJob: Job? = null
    private var started = false
    private var closed = false
    private var connectionAvailable = false
    private var entered = false
    private var inputRequested = false
    private var active = false
    private var familyIsSecu3T: Boolean? = null
    private var initialized = false
    private var initialStateSent = false

    fun start(scope: CoroutineScope) {
        if (connectionJob != null || closed) return
        started = true
        initialFirmwareFamily()?.let(::initializeOutputs)
        publishOutputState()
        connectionJob = scope.launch(start = CoroutineStart.UNDISPATCHED) {
            connectionStates.collectLatest { connection ->
                if (connection == Connected && isTransportConnected()) {
                    packets({
                        if (!closed && isTransportConnected()) {
                            connectionAvailable = true
                            requestEnterIfNeeded()
                            publishReadiness()
                        } else {
                            onConnectionLost()
                        }
                    }).collect(::onPacket)
                } else {
                    onConnectionLost()
                }
            }
        }
    }

    fun selectChannel(index: Int) {
        if (!canControl()) return
        val channels = channels() ?: return
        if (index !in 0..channels.size) return
        packet.chan = index
        sendIfInitialized()
        publishOutputState()
    }

    fun editFrequency(value: Float) {
        if (!canControl() || !value.isFinite() || value !in MIN_FREQUENCY..MAX_FREQUENCY) return
        packet.frq = value
        if (packet.chan > 0) sendIfInitialized()
        publishOutputState()
    }

    fun editDuty(value: Float) {
        if (!canControl() || !value.isFinite() || value !in MIN_DUTY..MAX_DUTY) return
        packet.duty = value
        if (packet.chan > 0) sendIfInitialized()
        publishOutputState()
    }

    fun setOutput(output: DiagnosticOutput, enabled: Boolean) {
        if (!canControl() || channelOutput(packet.chan) == output) return
        when (output) {
            DiagnosticOutput.BL -> if (!packet.blOutputEnabled) return else packet.bl = enabled
            DiagnosticOutput.DE -> if (!packet.deOutputEnabled) return else packet.de = enabled
            DiagnosticOutput.TACH_O -> if (!packet.tachOOutputEnabled) return else packet.tachO = enabled
            else -> packet.out = if (enabled) packet.out or (1 shl output.bit) else packet.out and (1 shl output.bit).inv()
        }
        sendIfInitialized()
        publishOutputState()
    }

    fun enableBlDe(enabled: Boolean) {
        if (!canControl()) return
        if (packet.blOutputEnabled == enabled && packet.deOutputEnabled == enabled) return
        packet.bl = false
        packet.de = false
        packet.blOutputEnabled = enabled
        packet.deOutputEnabled = enabled
        sendIfInitialized()
        publishOutputState()
    }

    fun enableTachO(enabled: Boolean) {
        if (!canControl() || packet.tachOOutputEnabled == enabled) return
        packet.tachO = false
        packet.tachOOutputEnabled = enabled
        sendIfInitialized()
        publishOutputState()
    }

    fun applyAdditionalOptions(enableBlDe: Boolean, enableTachO: Boolean) {
        if (!canControl()) return
        // Each changed option enqueues its reset packet before the next option is applied.
        enableBlDe(enableBlDe)
        enableTachO(enableTachO)
    }

    fun close() {
        if (closed) return
        val shouldLeave = connectionAvailable && entered && isTransportConnected()
        closed = true
        connectionAvailable = false
        active = false
        connectionJob?.cancel()
        connectionJob = null
        if (shouldLeave) {
            sendTask(Task.Secu3OpComLeaveDiagnostics)
            sendTask(Task.Secu3ReadSensors)
        }
        publishReadiness()
    }

    private fun onPacket(packet: InputPacket) {
        if (closed) return
        if (!connectionAvailable || !isTransportConnected()) {
            onConnectionLost()
            return
        }
        when (packet) {
            is OpCompNc -> when (packet.opCode) {
                6 -> if (!inputRequested && entered) {
                    inputRequested = true
                    sendTask(Task.Secu3DiagInput)
                }
                7 -> {
                    active = false
                    publishReadiness()
                }
            }
            is DiagInputPacket -> {
                active = true
                initializeOutputs(packet.flags != 0)
                if (!initialStateSent) {
                    initialStateSent = true
                    sendIfInitialized()
                }
                publishOutputState(packet)
            }
        }
    }

    private fun requestEnterIfNeeded() {
        if (closed || !started || !connectionAvailable || entered) return
        entered = true
        inputRequested = false
        sendTask(Task.Secu3OpComEnterDiagnostics)
    }

    private fun onConnectionLost() {
        if (closed) return
        connectionAvailable = false
        entered = false
        inputRequested = false
        active = false
        val retainedFamily = familyIsSecu3T ?: initialFirmwareFamily()
        familyIsSecu3T = retainedFamily
        packet.out = if (retainedFamily == true) IGNITION_OUTPUT_MASK else 0
        packet.chan = 0
        initialized = false
        initialStateSent = false
        publishOutputState(input = null)
    }

    private fun initializeOutputs(isSecu3T: Boolean) {
        if (initialized) return
        familyIsSecu3T = isSecu3T
        packet.out = (packet.out and IGNITION_OUTPUT_MASK.inv()) or if (isSecu3T) IGNITION_OUTPUT_MASK else 0
        initialized = true
    }

    private fun canControl() = connectionAvailable && isTransportConnected() && active && _state.value.input != null

    private fun publishReadiness() {
        _state.value = _state.value.copy(controlsEnabled = canControl())
    }

    private fun publishOutputState(input: DiagInputPacket? = _state.value.input) {
        _state.value = _state.value.copy(
            input = input,
            outputBits = packet.out,
            channel = packet.chan,
            frequency = packet.frq,
            duty = packet.duty,
            generatorOutput = channelOutput(packet.chan),
            enableBlDe = packet.blOutputEnabled && packet.deOutputEnabled,
            enableTachO = packet.tachOOutputEnabled,
            controlsEnabled = connectionAvailable && isTransportConnected() && active && input != null,
        )
    }

    private fun channels(): List<DiagnosticOutput>? = when (familyIsSecu3T) {
        true -> DiagnosticOutput.secu3TChannels
        false -> DiagnosticOutput.secu3IChannels
        null -> null
    }

    private fun channelOutput(channel: Int): DiagnosticOutput? =
        if (channel > 0) channels()?.getOrNull(channel - 1) else null

    private fun sendIfInitialized() {
        if (initialized) sendPacket(packet)
    }

    companion object {
        const val MIN_FREQUENCY = 10f
        const val MAX_FREQUENCY = 5000f
        const val MIN_DUTY = 0f
        const val MAX_DUTY = 100f
        private const val IGNITION_OUTPUT_MASK = 0b11
    }
}
