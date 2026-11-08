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
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
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
import java.util.ArrayDeque
import java.util.Collections
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
    val diagnosticSamples: List<DiagnosticSample> = emptyList(),
    val outputBits: Int = 0,
    val channel: Int = 0,
    val frequency: Float = 10f,
    val duty: Float = 50f,
    val generatorOutput: DiagnosticOutput? = null,
    val enableBlDe: Boolean = false,
    val enableTachO: Boolean = false,
    val controlsEnabled: Boolean = false,
    val isAutoTestingOutputs: Boolean = false,
) {
    val canStartOutputAutoTest: Boolean
        get() = controlsEnabled && !isAutoTestingOutputs && channel == 0

    fun canSetOutput(output: DiagnosticOutput): Boolean =
        controlsEnabled && !isAutoTestingOutputs && output != generatorOutput && when (output) {
            DiagnosticOutput.BL, DiagnosticOutput.DE -> enableBlDe
            DiagnosticOutput.TACH_O -> enableTachO
            else -> true
        }
}

data class DiagnosticSample(
    val timestampNanos: Long,
    val ks1: Float,
    val ks2: Float,
)

/** Owns the diagnostic protocol session and its ECU output packet. */
class DiagnosticsSession internal constructor(
    private val connectionStates: Flow<ConnectionState>,
    private val packets: (onSubscribed: () -> Unit) -> Flow<InputPacket>,
    private val isTransportConnected: () -> Boolean,
    private val initialFirmwareFamily: () -> Boolean?,
    private val sendTask: (Task) -> Unit,
    private val sendPacket: (DiagOutputPacket) -> Unit,
    private val monotonicNowNanos: () -> Long = System::nanoTime,
    private val waitForAutoTestTick: suspend () -> Unit = { delay(AUTO_TEST_TICK_MILLIS) },
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
    private val diagnosticSamples = ArrayDeque<DiagnosticSample>()
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
    private var autoTestJob: Job? = null
    private var autoTestRunning = false
    private var autoTestPhase = AutoTestPhase.SEQUENTIAL
    private var autoTestOutputIndex = 0
    private var autoTestGroupStep = 0
    private var autoTestGeneration = 0L

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
        if (!canControl() || autoTestRunning) return
        val channels = channels() ?: return
        if (index !in 0..channels.size) return
        packet.chan = index
        sendIfInitialized()
        publishOutputState()
    }

    fun editFrequency(value: Float) {
        if (!canControl() || autoTestRunning || !value.isFinite() || value !in MIN_FREQUENCY..MAX_FREQUENCY) return
        packet.frq = value
        if (packet.chan > 0) sendIfInitialized()
        publishOutputState()
    }

    fun editDuty(value: Float) {
        if (!canControl() || autoTestRunning || !value.isFinite() || value !in MIN_DUTY..MAX_DUTY) return
        packet.duty = value
        if (packet.chan > 0) sendIfInitialized()
        publishOutputState()
    }

    fun setOutput(output: DiagnosticOutput, enabled: Boolean) {
        if (!canControl() || !_state.value.canSetOutput(output)) return
        when (output) {
            DiagnosticOutput.BL -> packet.bl = enabled
            DiagnosticOutput.DE -> packet.de = enabled
            DiagnosticOutput.TACH_O -> packet.tachO = enabled
            else -> packet.out = if (enabled) packet.out or (1 shl output.bit) else packet.out and (1 shl output.bit).inv()
        }
        sendIfInitialized()
        publishOutputState()
    }

    fun enableBlDe(enabled: Boolean) {
        if (!canControl() || autoTestRunning) return
        if (packet.blOutputEnabled == enabled && packet.deOutputEnabled == enabled) return
        packet.bl = false
        packet.de = false
        packet.blOutputEnabled = enabled
        packet.deOutputEnabled = enabled
        sendIfInitialized()
        publishOutputState()
    }

    fun enableTachO(enabled: Boolean) {
        if (!canControl() || autoTestRunning || packet.tachOOutputEnabled == enabled) return
        packet.tachO = false
        packet.tachOOutputEnabled = enabled
        sendIfInitialized()
        publishOutputState()
    }

    fun applyAdditionalOptions(enableBlDe: Boolean, enableTachO: Boolean) {
        if (!canControl() || autoTestRunning) return
        // Each changed option enqueues its reset packet before the next option is applied.
        enableBlDe(enableBlDe)
        enableTachO(enableTachO)
    }

    fun startOutputAutoTest(scope: CoroutineScope) {
        if (!scope.isActive || !canControl() || !_state.value.canStartOutputAutoTest) return
        autoTestRunning = true
        val generation = ++autoTestGeneration
        autoTestPhase = AutoTestPhase.SEQUENTIAL
        autoTestOutputIndex = 0
        autoTestGroupStep = 0
        setAllAutoTestOutputs(false)
        sendIfInitialized()
        publishOutputState()
        val job = scope.launch(start = CoroutineStart.LAZY) { runOutputAutoTest() }
        job.invokeOnCompletion {
            if (autoTestGeneration == generation && autoTestRunning) {
                finishOutputAutoTest(resetOutputs = canControl(), cancelJob = false)
            }
        }
        autoTestJob = job
        if (!job.start()) finishOutputAutoTest(resetOutputs = false, cancelJob = false)
    }

    fun stopOutputAutoTest() {
        finishOutputAutoTest(resetOutputs = canControl(), cancelJob = true)
    }

    fun close() {
        if (closed) return
        finishOutputAutoTest(resetOutputs = canControl(), cancelJob = true)
        val shouldLeave = connectionAvailable && entered && isTransportConnected()
        closed = true
        connectionAvailable = false
        active = false
        diagnosticSamples.clear()
        connectionJob?.cancel()
        connectionJob = null
        if (shouldLeave) {
            sendTask(Task.Secu3OpComLeaveDiagnostics)
            sendTask(Task.Secu3ReadSensors)
        }
        publishOutputState()
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
                    finishOutputAutoTest(resetOutputs = false, cancelJob = true)
                    active = false
                    diagnosticSamples.clear()
                    publishOutputState()
                }
            }
            is DiagInputPacket -> {
                active = true
                recordDiagnosticSample(packet)
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
        finishOutputAutoTest(resetOutputs = false, cancelJob = true)
        connectionAvailable = false
        entered = false
        inputRequested = false
        active = false
        diagnosticSamples.clear()
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
            diagnosticSamples = immutableDiagnosticSampleSnapshot(),
            outputBits = packet.out,
            channel = packet.chan,
            frequency = packet.frq,
            duty = packet.duty,
            generatorOutput = channelOutput(packet.chan),
            enableBlDe = packet.blOutputEnabled && packet.deOutputEnabled,
            enableTachO = packet.tachOOutputEnabled,
            controlsEnabled = connectionAvailable && isTransportConnected() && active && input != null,
            isAutoTestingOutputs = autoTestRunning,
        )
    }

    private suspend fun runOutputAutoTest() {
        while (autoTestRunning) {
            waitForAutoTestTick()
            if (!autoTestRunning) return
            if (!canControl()) {
                finishOutputAutoTest(resetOutputs = false, cancelJob = false)
                return
            }
            when (autoTestPhase) {
                AutoTestPhase.SEQUENTIAL -> {
                    val outputs = outputAutoTestOrder()
                    if (autoTestOutputIndex == outputs.size) {
                        setAllAutoTestOutputs(false)
                        autoTestPhase = AutoTestPhase.GROUP
                        autoTestGroupStep = 0
                    } else {
                        val previous = outputs.getOrNull(autoTestOutputIndex - 1) ?: outputs.last()
                        setAutoTestOutput(previous, false)
                        setAutoTestOutput(outputs[autoTestOutputIndex], true)
                        autoTestOutputIndex++
                    }
                    sendIfInitialized()
                }
                AutoTestPhase.GROUP -> {
                    if (autoTestGroupStep == AUTO_TEST_GROUP_STEPS) {
                        setAllAutoTestOutputs(false)
                        autoTestPhase = AutoTestPhase.SEQUENTIAL
                        autoTestOutputIndex = 0
                    } else {
                        setAllAutoTestOutputs(autoTestGroupStep % 2 == 0)
                        autoTestGroupStep++
                    }
                    sendIfInitialized()
                }
            }
            publishOutputState()
        }
    }

    private fun finishOutputAutoTest(resetOutputs: Boolean, cancelJob: Boolean) {
        if (!autoTestRunning) return
        autoTestRunning = false
        autoTestGeneration++
        val runningJob = autoTestJob
        autoTestJob = null
        if (cancelJob) runningJob?.cancel()
        if (resetOutputs && canControl()) {
            setAllAutoTestOutputs(false)
            sendIfInitialized()
        }
        publishOutputState()
    }

    private fun outputAutoTestOrder(): List<DiagnosticOutput> = when (familyIsSecu3T) {
        true -> listOf(
            DiagnosticOutput.FE, DiagnosticOutput.IE, DiagnosticOutput.ST_BLOCK,
            DiagnosticOutput.IGN_OUT1, DiagnosticOutput.IGN_OUT2,
            DiagnosticOutput.IGN_OUT3, DiagnosticOutput.IGN_OUT4,
            DiagnosticOutput.CE, DiagnosticOutput.ECF, DiagnosticOutput.ADD_IO1,
            DiagnosticOutput.ADD_IO2, DiagnosticOutput.BL, DiagnosticOutput.DE,
        )
        false -> listOf(
            DiagnosticOutput.IGN_OUT1, DiagnosticOutput.IGN_OUT2,
            DiagnosticOutput.IGN_OUT3, DiagnosticOutput.IGN_OUT4,
            DiagnosticOutput.IGN_OUT5, DiagnosticOutput.INJ_O1,
            DiagnosticOutput.INJ_O2, DiagnosticOutput.INJ_O3,
            DiagnosticOutput.INJ_O4, DiagnosticOutput.INJ_O5,
            DiagnosticOutput.ECF0, DiagnosticOutput.ADD_O2,
            DiagnosticOutput.EVAP_O, DiagnosticOutput.O2SH_O,
            DiagnosticOutput.COND_O, DiagnosticOutput.STBL_O,
            DiagnosticOutput.CEL_O, DiagnosticOutput.FPMP_O,
            DiagnosticOutput.PWRR_O, DiagnosticOutput.BL, DiagnosticOutput.DE,
            DiagnosticOutput.TACH_O, DiagnosticOutput.GPA6_O,
        )
        null -> emptyList()
    }

    private fun setAllAutoTestOutputs(enabled: Boolean) {
        outputAutoTestOrder().forEach { output -> setAutoTestOutput(output, enabled) }
    }

    private fun setAutoTestOutput(output: DiagnosticOutput, enabled: Boolean) {
        when (output) {
            DiagnosticOutput.BL -> if (packet.blOutputEnabled) packet.bl = enabled
            DiagnosticOutput.DE -> if (packet.deOutputEnabled) packet.de = enabled
            DiagnosticOutput.TACH_O -> if (packet.tachOOutputEnabled) packet.tachO = enabled
            else -> {
                val mask = 1 shl output.bit
                packet.out = if (enabled) packet.out or mask else packet.out and mask.inv()
            }
        }
    }

    private fun recordDiagnosticSample(input: DiagInputPacket) {
        val now = monotonicNowNanos()
        diagnosticSamples.addLast(DiagnosticSample(now, input.ks1, input.ks2))
        val oldestRetained = now - DIAGNOSTIC_SAMPLE_WINDOW_NANOS
        while (diagnosticSamples.isNotEmpty() && diagnosticSamples.first().timestampNanos < oldestRetained) {
            diagnosticSamples.removeFirst()
        }
        while (diagnosticSamples.size > MAX_DIAGNOSTIC_SAMPLES) diagnosticSamples.removeFirst()
    }

    private fun immutableDiagnosticSampleSnapshot(): List<DiagnosticSample> =
        Collections.unmodifiableList(ArrayList(diagnosticSamples))

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
        const val DIAGNOSTIC_SAMPLE_WINDOW_NANOS = 10_000_000_000L
        const val MAX_DIAGNOSTIC_SAMPLES = 512
        const val AUTO_TEST_TICK_MILLIS = 400L
        private const val AUTO_TEST_GROUP_STEPS = 5
        private const val IGNITION_OUTPUT_MASK = 0b11
    }

    private enum class AutoTestPhase { SEQUENTIAL, GROUP }
}
