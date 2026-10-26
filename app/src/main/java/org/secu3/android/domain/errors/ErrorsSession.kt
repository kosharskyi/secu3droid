/*
 *    SecuDroid  - An open source, free manager for SECU-3 engine control unit
 *    Copyright (C) 2026 Vitalii O. Kosharskyi. Ukraine, Kyiv
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 */
package org.secu3.android.domain.errors

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.secu3.android.connection.Connected
import org.secu3.android.connection.ConnectionState
import org.secu3.android.connection.Secu3ConnectionManager
import org.secu3.android.domain.ecu.EcuExchange
import org.secu3.android.models.packets.base.InputPacket
import org.secu3.android.models.packets.input.CheckEngineErrorsPacket
import org.secu3.android.models.packets.input.FirmwareInfoPacket
import org.secu3.android.models.packets.out.CheckEngineSavedErrorsPacket
import org.secu3.android.models.packets.out.OpCompNc
import org.secu3.android.utils.Task
import java.util.concurrent.TimeoutException
import javax.inject.Inject

enum class ErrorsClearStatus {
    Idle,
    Clearing,
    Succeeded,
    AckTimeout,
    ReadbackTimeout,
}

enum class ErrorsReadStatus { Idle, SavedTimeout, ActiveTimeout }

data class ErrorsSnapshot(
    val saved: CheckEngineSavedErrorsPacket? = null,
    val active: CheckEngineErrorsPacket? = null,
    val clearStatus: ErrorsClearStatus = ErrorsClearStatus.Idle,
    val readStatus: ErrorsReadStatus = ErrorsReadStatus.Idle,
) {
    val canClearErrors: Boolean get() = saved != null && clearStatus != ErrorsClearStatus.Clearing
    val canRetryRead: Boolean get() = readStatus != ErrorsReadStatus.Idle && clearStatus != ErrorsClearStatus.Clearing
}

/** Owns ECU error reads and clear transactions while the route is resumed and connected. */
class ErrorsSession internal constructor(
    private val connectionStates: Flow<ConnectionState>,
    private val ecuExchange: EcuExchange,
    private val packets: (onSubscribed: () -> Unit) -> Flow<InputPacket>,
    private val sendSavedRead: () -> Unit,
    private val sendActiveRead: () -> Unit,
    private val clearSavedErrors: () -> Unit,
    private val ackTimeoutMillis: Long = 10_000,
    private val readbackTimeoutMillis: Long = 10_000,
    private val initialReadTimeoutMillis: Long = 10_000,
) {
    @Inject
    constructor(connectionManager: Secu3ConnectionManager, ecuExchange: EcuExchange) : this(
        connectionStates = connectionManager.connectionStateFlow,
        ecuExchange = ecuExchange,
        packets = connectionManager::receivedPacketFlow,
        sendSavedRead = { connectionManager.sendNewTask(Task.Secu3ReadEcuSavedErrors) },
        sendActiveRead = { connectionManager.sendNewTask(Task.Secu3ReadEcuErrors) },
        clearSavedErrors = { connectionManager.sendOutPacket(CheckEngineSavedErrorsPacket()) },
    )

    private val _snapshot = MutableStateFlow(ErrorsSnapshot())
    val snapshot = _snapshot.asStateFlow()

    private var connected = false
    private var resumed = false
    private var closed = false
    private var generation = 0L
    private var sessionJob: Job? = null
    private var connectionJob: Job? = null

    fun start(scope: CoroutineScope) {
        if (closed || connectionJob != null) return
        sessionScope = scope
        connectionJob = scope.launch(start = kotlinx.coroutines.CoroutineStart.UNDISPATCHED) {
            connectionStates.collect(::onConnectionState)
        }
    }

    fun onRouteResumed() {
        if (closed || resumed) return
        resumed = true
        startSessionIfEligible()
    }

    fun onRoutePaused() {
        if (!resumed && sessionJob == null) return
        resumed = false
        stopSession()
    }

    fun clearErrors() {
        if (!connected || !resumed || !_snapshot.value.canClearErrors) return
        cancelWorkerPreservingSnapshot()
        _snapshot.value = _snapshot.value.copy(clearStatus = ErrorsClearStatus.Clearing, readStatus = ErrorsReadStatus.Idle)
        val session = generation
        sessionJob = sessionScope?.launch {
            try {
                performClear(session)
            } finally {
                if (generation == session) sessionJob = null
            }
        }
    }

    fun retryRead() {
        if (!connected || !resumed || !_snapshot.value.canRetryRead) return
        cancelWorkerPreservingSnapshot()
        _snapshot.value = _snapshot.value.copy(saved = null, active = null, readStatus = ErrorsReadStatus.Idle)
        startSessionIfEligible()
    }

    fun close() {
        if (closed) return
        closed = true
        resumed = false
        connected = false
        stopSession()
        connectionJob?.cancel()
        connectionJob = null
        sessionScope = null
    }

    private var sessionScope: CoroutineScope? = null

    private fun onConnectionState(state: ConnectionState) {
        if (closed) return
        if (state != Connected) {
            connected = false
            stopSession()
            return
        }
        if (connected) return
        connected = true
        startSessionIfEligible()
    }

    private fun startSessionIfEligible() {
        val scope = sessionScope ?: return
        if (!connected || !resumed || sessionJob != null) return
        val session = ++generation
        sessionJob = scope.launch {
            try {
                val saved = try {
                    ecuExchange.request(
                        timeoutMillis = initialReadTimeoutMillis,
                        sendRequest = { if (isCurrent(session)) sendSavedRead() },
                    ) { responsePackets ->
                        responsePackets.filterIsInstance<CheckEngineSavedErrorsPacket>().first()
                    }
                } catch (_: TimeoutException) {
                    if (isCurrent(session)) _snapshot.value = _snapshot.value.copy(readStatus = ErrorsReadStatus.SavedTimeout)
                    return@launch
                }
                if (!isCurrent(session)) return@launch
                _snapshot.value = _snapshot.value.copy(saved = saved)
                collectActiveErrors(session)
            } finally {
                if (generation == session) sessionJob = null
            }
        }
    }

    private suspend fun performClear(session: Long) {
        try {
            ecuExchange.request(
                timeoutMillis = ackTimeoutMillis,
                sendRequest = { if (isCurrent(session)) clearSavedErrors() },
            ) { responsePackets ->
                responsePackets.filterIsInstance<OpCompNc>()
                    .first { it.isSavedErrorsWriteAcknowledgement }
            }
        } catch (_: TimeoutException) {
            updateClearStatus(session, ErrorsClearStatus.AckTimeout)
            collectActiveErrors(session)
            return
        }
        if (!isCurrent(session)) return

        val refreshedSavedErrors = try {
            ecuExchange.request(
                timeoutMillis = readbackTimeoutMillis,
                sendRequest = { if (isCurrent(session)) sendSavedRead() },
            ) { responsePackets ->
                responsePackets.filterIsInstance<CheckEngineSavedErrorsPacket>().first()
            }
        } catch (_: TimeoutException) {
            updateClearStatus(session, ErrorsClearStatus.ReadbackTimeout)
            collectActiveErrors(session)
            return
        }
        if (!isCurrent(session)) return

        _snapshot.value = _snapshot.value.copy(
            saved = refreshedSavedErrors,
            clearStatus = ErrorsClearStatus.Succeeded,
        )
        collectActiveErrors(session)
    }

    private suspend fun collectActiveErrors(session: Long) {
        coroutineScope {
            var receivedActive = _snapshot.value.active != null
            val timer = if (receivedActive) null else launch {
                delay(initialReadTimeoutMillis)
                if (
                    isCurrent(session) &&
                    _snapshot.value.active == null &&
                    _snapshot.value.clearStatus != ErrorsClearStatus.Clearing
                ) {
                    _snapshot.value = _snapshot.value.copy(readStatus = ErrorsReadStatus.ActiveTimeout)
                }
            }
            packets { if (isCurrent(session)) sendActiveRead() }.collect { packet ->
                if (!isCurrent(session)) return@collect
                when (packet) {
                    is CheckEngineErrorsPacket -> {
                        _snapshot.value = _snapshot.value.copy(active = packet, readStatus = ErrorsReadStatus.Idle)
                        if (!receivedActive) {
                            receivedActive = true
                            timer?.cancel()
                        }
                    }
                    // Firmware setup can overtake the active read; reselect v mode after its reply.
                    is FirmwareInfoPacket -> sendActiveRead()
                }
            }
        }
    }

    private fun updateClearStatus(session: Long, status: ErrorsClearStatus) {
        if (isCurrent(session)) _snapshot.value = _snapshot.value.copy(clearStatus = status)
    }

    private fun cancelWorkerPreservingSnapshot() {
        generation++
        sessionJob?.cancel()
        sessionJob = null
    }

    private fun stopSession() {
        generation++
        sessionJob?.cancel()
        sessionJob = null
        _snapshot.value = ErrorsSnapshot()
    }

    private fun isCurrent(session: Long): Boolean = !closed && connected && resumed && generation == session
}
