/*
 *    SecuDroid  - An open source, free manager for SECU-3 engine control unit
 *    Copyright (C) 2026 Vitalii O. Kosharskyi. Ukraine, Kyiv
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 */
package org.secu3.android.domain.parameters

import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.secu3.android.connection.Connected
import org.secu3.android.connection.ConnectionState
import org.secu3.android.connection.Secu3ConnectionManager
import org.secu3.android.models.packets.base.InputPacket
import org.secu3.android.models.packets.base.OutputPacket
import org.secu3.android.models.packets.input.FirmwareInfoPacket
import org.secu3.android.models.packets.out.params.AccelerationParamPacket
import org.secu3.android.models.packets.out.params.AdcCorrectionsParamPacket
import org.secu3.android.models.packets.out.params.AnglesParamPacket
import org.secu3.android.models.packets.out.params.CarburParamPacket
import org.secu3.android.models.packets.out.params.ChokeControlParPacket
import org.secu3.android.models.packets.out.params.CkpsParamPacket
import org.secu3.android.models.packets.out.params.DbwParamPacket
import org.secu3.android.models.packets.out.params.GasDoseParamPacket
import org.secu3.android.models.packets.out.params.IdlingParamPacket
import org.secu3.android.models.packets.out.params.InjctrParPacket
import org.secu3.android.models.packets.out.params.KnockParamPacket
import org.secu3.android.models.packets.out.params.LambdaParamPacket
import org.secu3.android.models.packets.out.params.LtftParamPacket
import org.secu3.android.models.packets.out.params.MiscellaneousParamPacket
import org.secu3.android.models.packets.out.params.SecurityParamPacket
import org.secu3.android.models.packets.out.params.StarterParamPacket
import org.secu3.android.models.packets.out.params.TemperatureParamPacket
import org.secu3.android.models.packets.out.params.UniOutParamPacket
import org.secu3.android.utils.Task
import java.util.concurrent.TimeoutException
import javax.inject.Inject
import kotlin.coroutines.coroutineContext

sealed interface ParameterReadResult {
    data class Packet(val packet: InputPacket) : ParameterReadResult
    data class Functions(val parameters: FunctionParameters) : ParameterReadResult
}

sealed interface ParameterSessionEvent {
    data class Invalidated(val sessionId: Int) : ParameterSessionEvent
    data class PageLoading(val page: ParameterPage) : ParameterSessionEvent
    data class PageLoaded(val page: ParameterPage, val result: ParameterReadResult) : ParameterSessionEvent
    data class PageFailed(val page: ParameterPage) : ParameterSessionEvent
    data object SaveStarted : ParameterSessionEvent
    data class SaveFinished(val result: EepromSaveResult) : ParameterSessionEvent
}

/** Owns parameter page reads, cache validity, command ordering, and session cleanup. */
@ViewModelScoped
class ParameterSession internal constructor(
    private val connectionStates: Flow<ConnectionState>,
    private val isTransportConnected: () -> Boolean,
    private val firmware: () -> FirmwareInfoPacket?,
    private val exchange: ParameterExchange,
    private val sendPacketToEcu: (OutputPacket) -> Unit,
) {
    @Inject
    constructor(connectionManager: Secu3ConnectionManager, exchange: ParameterExchange) : this(
        connectionStates = connectionManager.connectionStateFlow,
        isTransportConnected = { connectionManager.isConnected },
        firmware = { connectionManager.fwInfo },
        exchange = exchange,
        sendPacketToEcu = connectionManager::sendOutPacket,
    )

    private val readGate = ParameterPageReadGate()
    private val readJobs = mutableMapOf<ParameterPage, Job>()
    private val loadedPages = mutableSetOf<ParameterPage>()
    private var scope: CoroutineScope? = null
    private var eventHandler: ((ParameterSessionEvent) -> Unit)? = null
    private var connectionJob: Job? = null
    private var saveJob: Job? = null
    private var connected = false
    private var paused = false
    private var activePage: ParameterPage? = null
    private var sessionId = 0
    private var writeRevision = 0L

    fun start(scope: CoroutineScope, eventHandler: (ParameterSessionEvent) -> Unit) {
        if (connectionJob != null) return
        this.scope = scope
        this.eventHandler = eventHandler
        connectionJob = scope.launch(start = CoroutineStart.UNDISPATCHED) {
            connectionStates.collect(::onConnectionState)
        }
    }

    fun activatePage(page: ParameterPage) {
        if (!connected || !isTransportConnected()) return
        paused = false
        activePage = page
        readGate.activate(page)
        loadPage(page)
    }

    fun deactivatePage() {
        readGate.deactivate()
    }

    /** A host pause cancels pending work and forces a fresh read when the route resumes. */
    fun pause() {
        paused = true
        sessionId++
        activePage = null
        readGate.deactivate()
        loadedPages.clear()
        writeRevision++
        cancelPendingWork()
    }

    fun refreshPage(page: ParameterPage) {
        if (!canOperate()) return
        loadedPages.remove(page)
        loadPage(page)
    }

    fun sendPacket(packet: OutputPacket): Boolean {
        if (!canOperate()) return false
        sendPacketToEcu(packet)
        writeRevision++
        loadedPages.removeAll { it != activePage }
        return true
    }

    fun save() {
        val scope = scope ?: return
        if (!canOperate() || saveJob?.isActive == true) return
        val session = sessionId
        saveJob = scope.launch {
            try {
                val result = exchange.saveEeprom {
                    if (isCurrent(session)) eventHandler?.invoke(ParameterSessionEvent.SaveStarted)
                }
                if (isCurrent(session)) eventHandler?.invoke(ParameterSessionEvent.SaveFinished(result))
            } catch (cancelled: CancellationException) {
                throw cancelled
            } finally {
                if (saveJob == coroutineContext[Job]) saveJob = null
            }
        }
    }

    /** Ends a route-owned parameter view and emits a final UI invalidation. */
    fun closePageSession() {
        paused = true
        sessionId++
        activePage = null
        readGate.deactivate()
        loadedPages.clear()
        writeRevision++
        cancelPendingWork()
        eventHandler?.invoke(ParameterSessionEvent.Invalidated(sessionId))
    }

    private fun onConnectionState(state: ConnectionState) {
        if (state == Connected) {
            connected = true
        } else {
            sessionId++
            connected = false
            activePage = null
            readGate.deactivate()
            loadedPages.clear()
            writeRevision++
            cancelPendingWork()
            eventHandler?.invoke(ParameterSessionEvent.Invalidated(sessionId))
        }
    }

    private fun cancelPendingWork() {
        saveJob?.cancel()
        saveJob = null
        val jobsToCancel = readJobs.values.toList()
        readJobs.clear()
        jobsToCancel.forEach(Job::cancel)
    }

    private fun loadPage(page: ParameterPage) {
        if (page in loadedPages || readJobs[page]?.isActive == true) return
        val scope = scope ?: return
        val session = sessionId
        eventHandler?.invoke(ParameterSessionEvent.PageLoading(page))
        var readRevision = writeRevision
        lateinit var job: Job
        job = scope.launch {
            try {
                val result = readGate.read(page, onStarted = { readRevision = writeRevision }) {
                    if (isCurrent(session) && isTransportConnected()) readPage(page) else null
                }.first()
                if (!isCurrent(session)) return@launch
                if (result == null) return@launch
                if (writeRevision == readRevision) loadedPages += page
                eventHandler?.invoke(ParameterSessionEvent.PageLoaded(page, result))
            } catch (_: TimeoutException) {
                if (isCurrent(session)) eventHandler?.invoke(ParameterSessionEvent.PageFailed(page))
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                if (isCurrent(session)) eventHandler?.invoke(ParameterSessionEvent.PageFailed(page))
            } finally {
                if (readJobs[page] === coroutineContext[Job]) readJobs.remove(page)
            }
        }
        readJobs[page] = job
    }

    private suspend fun readPage(page: ParameterPage): ParameterReadResult? {
        return when (page) {
            ParameterPage.STARTER -> packet(Task.Secu3ReadStarterParam, StarterParamPacket::class.java)
            ParameterPage.ANGLES -> packet(Task.Secu3ReadAnglesParam, AnglesParamPacket::class.java)
            ParameterPage.IDLING -> packet(Task.Secu3ReadIdlingParam, IdlingParamPacket::class.java)
            ParameterPage.TEMPERATURE -> packet(Task.Secu3ReadTemperatureParam, TemperatureParamPacket::class.java)
            ParameterPage.FUEL_CUTOFF -> packet(Task.Secu3ReadCarburParam, CarburParamPacket::class.java)
            ParameterPage.CKPS -> packet(Task.Secu3ReadCkpsParam, CkpsParamPacket::class.java)
            ParameterPage.ADC_ERRORS -> packet(Task.Secu3ReadAdcErrorsCorrectionsParam, AdcCorrectionsParamPacket::class.java)
            ParameterPage.KNOCK -> packet(Task.Secu3ReadKnockParam, KnockParamPacket::class.java)
            ParameterPage.LAMBDA_CONTROL -> {
                val info = firmware() ?: return null
                if (!info.isFuelInjectEnabled && !info.isCarbAfrEnabled && !info.isGdControlEnabled) null
                else packet(Task.Secu3ReadLambdaParam, LambdaParamPacket::class.java)
            }
            ParameterPage.ACCELERATION -> {
                val info = firmware() ?: return null
                if (!info.isFuelInjectEnabled && !info.isGdControlEnabled) null
                else packet(Task.Secu3ReadAccelerationParam, AccelerationParamPacket::class.java)
            }
            ParameterPage.LTFT -> packet(Task.Secu3ReadLtftParam, LtftParamPacket::class.java)
            ParameterPage.FUNCTIONS -> ParameterReadResult.Functions(exchange.readFunctions())
            ParameterPage.MISCELLANEOUS -> packet(Task.Secu3ReadMiscellaneousParam, MiscellaneousParamPacket::class.java)
            ParameterPage.SECURITY -> packet(Task.Secu3ReadSecurityParam, SecurityParamPacket::class.java)
            ParameterPage.CHOKE_CONTROL -> packet(Task.Secu3ReadChokeControlParam, ChokeControlParPacket::class.java)
            ParameterPage.GAS_DOSE -> {
                if (firmware()?.isGdControlEnabled != true) null
                else packet(Task.Secu3ReadGasDoseParam, GasDoseParamPacket::class.java)
            }
            ParameterPage.EL_THROTTLE -> packet(Task.Secu3ReadDbwParam, DbwParamPacket::class.java)
            ParameterPage.FUEL_INJECTION -> {
                if (firmware()?.isFuelInjectEnabled != true) null
                else packet(Task.Secu3ReadFuelInjectionParam, InjctrParPacket::class.java)
            }
            ParameterPage.UNIVERSAL_OUTPUTS -> packet(Task.Secu3ReadUniversalOutputsParam, UniOutParamPacket::class.java)
        }
    }

    private suspend fun <T : InputPacket> packet(task: Task, type: Class<T>): ParameterReadResult =
        ParameterReadResult.Packet(exchange.readPacket(task, type))

    private fun canOperate(): Boolean = !paused && connected && isTransportConnected()

    private fun isCurrent(session: Int): Boolean = connected && sessionId == session
}
