/*
 *    SecuDroid  - An open source, free manager for SECU-3 engine control unit
 *    Copyright (C) 2026 Vitalii O. Kosharskyi. Ukraine, Kyiv
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 */
package org.secu3.android.domain.ltft

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.timeout
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.launch
import org.secu3.android.connection.Connected
import org.secu3.android.connection.ConnectionState
import org.secu3.android.connection.Secu3ConnectionManager
import org.secu3.android.domain.ecu.EcuExchange
import org.secu3.android.models.packets.base.InputPacket
import org.secu3.android.models.packets.input.FirmwareInfoPacket
import org.secu3.android.models.packets.input.LoadGridPacket
import org.secu3.android.models.packets.input.LtftMapChunkPacket
import org.secu3.android.models.packets.input.RpmGridPacket
import org.secu3.android.models.packets.input.SensorsPacket
import org.secu3.android.models.packets.out.params.FunSetParamPacket
import org.secu3.android.utils.Task
import java.util.concurrent.TimeoutException
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

internal sealed interface LtftMapsState {
    data object Disconnected : LtftMapsState
    data object Unsupported : LtftMapsState
    data object Reading : LtftMapsState
    data object TimedOut : LtftMapsState

    data class Ready(
        val tables: List<List<Float>>,
        val rpmAxis: List<Int>,
        val loadAxis: List<Float>?,
        val isLoadPressure: Boolean,
        val workingPoint: LtftWorkingPoint? = null,
    ) : LtftMapsState
}

internal data class LtftWorkingPoint(
    val rpm: Int,
    val load: Float,
    val rpmIndex: Float,
    val loadIndex: Float,
)

internal class LtftMapsSession(
    private val connectionStates: Flow<ConnectionState>,
    private val firmwareInfo: () -> FirmwareInfoPacket?,
    private val isTransportConnected: () -> Boolean,
    private val ecuExchange: EcuExchange,
    private val packets: (onSubscribed: () -> Unit) -> Flow<InputPacket>,
    private val sendTask: (Task) -> Unit,
    private val timeoutMillis: Long = 10_000,
) {
    @Inject
    constructor(connectionManager: Secu3ConnectionManager, ecuExchange: EcuExchange) : this(
        connectionStates = connectionManager.connectionStateFlow,
        firmwareInfo = { connectionManager.fwInfo },
        isTransportConnected = { connectionManager.isConnected },
        ecuExchange = ecuExchange,
        packets = connectionManager::receivedPacketFlow,
        sendTask = connectionManager::sendNewTask,
    )

    private val _state = MutableStateFlow<LtftMapsState>(LtftMapsState.Disconnected)
    val state = _state.asStateFlow()

    private var scope: CoroutineScope? = null
    private var connectionJob: Job? = null
    private var readJob: Job? = null
    private var connected = false
    private var resumed = false
    private var readModeSelected = false
    private var generation = 0L

    fun start(scope: CoroutineScope) {
        if (connectionJob != null) return
        this.scope = scope
        connectionJob = scope.launch(start = CoroutineStart.UNDISPATCHED) {
            connectionStates.collect(::onConnectionState)
        }
    }

    fun onRouteResumed() {
        if (resumed) return
        resumed = true
        startRead()
    }

    fun onRoutePaused() {
        if (!resumed) return
        resumed = false
        val restoreSensors = connected && readModeSelected && isTransportConnected()
        cancelRead()
        if (restoreSensors) sendTask(Task.Secu3ReadSensors)
        _state.value = LtftMapsState.Disconnected
    }

    fun refresh() {
        if (_state.value !is LtftMapsState.Ready && _state.value != LtftMapsState.TimedOut) return
        cancelRead()
        startRead()
    }

    fun close() {
        onRoutePaused()
        connectionJob?.cancel()
        connectionJob = null
        scope = null
        connected = false
    }

    private fun onConnectionState(state: ConnectionState) {
        if (state != Connected) {
            connected = false
            cancelRead()
            _state.value = LtftMapsState.Disconnected
        } else if (!connected) {
            connected = true
            startRead()
        }
    }

    private fun startRead() {
        val scope = scope ?: return
        if (!connected || !resumed || readJob != null) return
        val session = ++generation
        _state.value = LtftMapsState.Reading
        val job = scope.launch(start = CoroutineStart.LAZY) {
            try {
                readMaps(session)
            } catch (_: TimeoutException) {
                if (isCurrent(session)) {
                    restoreSensorsIfReading()
                    _state.value = LtftMapsState.TimedOut
                }
            } finally {
                if (readJob === currentCoroutineContext()[Job]) readJob = null
            }
        }
        readJob = job
        job.start()
    }

    private suspend fun readMaps(session: Long) {
        val firmware = firmwareInfo() ?: readPacket<FirmwareInfoPacket>(session, Task.Secu3ReadFirmwareInfo)
        if (!isCurrent(session)) return
        if (!firmware.isFuelInjectEnabled) {
            restoreSensorsIfReading()
            _state.value = LtftMapsState.Unsupported
            return
        }

        val rpm = readPacket<RpmGridPacket>(session, Task.Secu3ReadRpmGrid).values
        val functions = readPacket<FunSetParamPacket>(session, Task.Secu3ReadFunsetParam)
        val usesBarometricLimit = !functions.useLoadGrid && functions.loadSrcCfg == MAP_BAROMETRIC_SOURCE
        val loadAxis = when {
            functions.useLoadGrid -> readPacket<LoadGridPacket>(session, Task.Secu3ReadLoadGrid).values
            usesBarometricLimit -> null
            else -> linearAxis(functions.loadLower, functions.loadUpper)
        }
        val tables = readTables(session)
        if (!isCurrent(session)) return

        val ready = LtftMapsState.Ready(
            tables = tables,
            rpmAxis = rpm,
            loadAxis = loadAxis,
            isLoadPressure = functions.loadSrcCfg <= MAP_BAROMETRIC_SOURCE,
        )
        monitorSensors(session, ready, functions.loadLower.takeIf { usesBarometricLimit })
    }

    private suspend inline fun <reified T : InputPacket> readPacket(session: Long, task: Task): T =
        request(session, task) { it.filterIsInstance<T>().first() }

    @OptIn(FlowPreview::class)
    private suspend fun readTables(session: Long): List<List<Float>> {
        val tables = Array(TABLE_COUNT) { FloatArray(CELL_COUNT) }
        val received = Array(TABLE_COUNT) { BooleanArray(CELL_COUNT) }
        var remaining = CELL_COUNT * TABLE_COUNT
        request(session, Task.Secu3ReadLtftMap, deadlineMillis = timeoutMillis * remaining) { responses ->
            responses.filterIsInstance<LtftMapChunkPacket>()
                .transform { chunk ->
                    val before = remaining
                    chunk.values.forEachIndexed { offset, value ->
                        val index = chunk.address + offset
                        val table = chunk.tableIndex
                        tables[table][index] = value
                        if (!received[table][index]) {
                            received[table][index] = true
                            remaining--
                        }
                    }
                    if (remaining < before) emit(remaining)
                }
                .timeout(timeoutMillis.milliseconds)
                .first { it == 0 }
        }
        return tables.map { it.toList() }
    }

    private suspend fun <T> request(
        session: Long,
        task: Task,
        deadlineMillis: Long = timeoutMillis,
        receive: suspend (Flow<InputPacket>) -> T,
    ): T =
        ecuExchange.request(
            timeoutMillis = deadlineMillis,
            sendRequest = {
                if (isCurrent(session)) {
                    readModeSelected = true
                    sendTask(task)
                }
            },
            receive = receive,
        )

    private suspend fun monitorSensors(session: Long, initial: LtftMapsState.Ready, barometricLower: Float?) {
        var ready = initial
        var barometricUpper: Float? = null
        val rpmBins = initial.rpmAxis.map(Int::toFloat)
        packets {
            if (isCurrent(session)) {
                restoreSensorsIfReading()
                _state.value = initial
            }
        }
            .filterIsInstance<SensorsPacket>()
            .collect { sensors ->
                if (!isCurrent(session)) return@collect
                val loadAxis = if (barometricLower != null &&
                    sensors.baroPress > barometricLower && sensors.baroPress != barometricUpper
                ) {
                    barometricUpper = sensors.baroPress
                    linearAxis(barometricLower, sensors.baroPress)
                } else {
                    ready.loadAxis
                }
                ready = ready.copy(
                    loadAxis = loadAxis,
                    workingPoint = if (sensors.strtUse) null else workingPoint(sensors, rpmBins, loadAxis),
                )
                _state.value = ready
            }
    }

    private fun workingPoint(sensors: SensorsPacket, rpmBins: List<Float>, loadAxis: List<Float>?): LtftWorkingPoint? {
        val rpmIndex = axisIndex(sensors.rpm.toFloat(), rpmBins)
        val loadIndex = loadAxis?.let { axisIndex(sensors.load, it) }
        if (rpmIndex == null || loadIndex == null) return null
        return LtftWorkingPoint(sensors.rpm, sensors.load, rpmIndex, loadIndex)
    }

    private fun axisIndex(value: Float, axis: List<Float>): Float? {
        if (!value.isFinite() || (1..axis.lastIndex).any { axis[it - 1] > axis[it] }) return null
        if (value <= axis.first()) return 0f
        if (value >= axis.last()) return axis.lastIndex.toFloat()
        val lower = axis.indexOfLast { it <= value }
        return lower + (value - axis[lower]) / (axis[lower + 1] - axis[lower])
    }

    private fun linearAxis(lower: Float, upper: Float): List<Float> =
        List(AXIS_SIZE) { lower + (upper - lower) * it / (AXIS_SIZE - 1) }

    private fun restoreSensorsIfReading() {
        if (!readModeSelected) return
        readModeSelected = false
        sendTask(Task.Secu3ReadSensors)
    }

    private fun cancelRead() {
        generation++
        readJob?.cancel()
        readJob = null
        readModeSelected = false
    }

    private fun isCurrent(session: Long): Boolean =
        generation == session && connected && resumed && isTransportConnected()

    companion object {
        const val AXIS_SIZE = 16
        const val TABLE_COUNT = 2
        private const val CELL_COUNT = AXIS_SIZE * AXIS_SIZE
        private const val MAP_BAROMETRIC_SOURCE = 1
    }
}
