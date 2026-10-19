/*
 *    SecuDroid  - An open source, free manager for SECU-3 engine control unit
 *    Copyright (C) 2026 Vitaliy O. Kosharskyi. Ukraine, Kyiv
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 */
package org.secu3.android.ui.errors

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import org.secu3.android.connection.Connected
import org.secu3.android.connection.ConnectionState
import org.secu3.android.models.packets.base.InputPacket
import org.secu3.android.models.packets.input.CheckEngineErrorsPacket
import org.secu3.android.models.packets.input.FirmwareInfoPacket
import org.secu3.android.models.packets.out.CheckEngineSavedErrorsPacket
import org.secu3.android.models.packets.out.OpCompNc

internal enum class ErrorsClearStatus {
    Idle,
    Clearing,
    Succeeded,
    AckTimeout,
    ReadbackTimeout,
}

internal enum class ErrorsReadStatus { Idle, SavedTimeout, ActiveTimeout }

internal data class ErrorsSnapshot(
    val saved: CheckEngineSavedErrorsPacket? = null,
    val active: CheckEngineErrorsPacket? = null,
    val clearStatus: ErrorsClearStatus = ErrorsClearStatus.Idle,
    val readStatus: ErrorsReadStatus = ErrorsReadStatus.Idle,
)

/** Owns error packets and reads only while the route is resumed and its transport is connected. */
internal class ErrorsSessionController(
    private val scope: CoroutineScope,
    private val packets: (onSubscribed: () -> Unit) -> Flow<InputPacket>,
    private val sendSavedRead: () -> Unit,
    private val sendActiveRead: () -> Unit,
    private val clearSavedErrors: () -> Unit,
    private val onSessionInvalidated: () -> Unit = {},
    private val ackTimeoutMillis: Long = 10_000,
    private val readbackTimeoutMillis: Long = 10_000,
    private val initialReadTimeoutMillis: Long = 10_000,
) {
    private val _snapshot = MutableStateFlow(ErrorsSnapshot())
    val snapshot = _snapshot.asStateFlow()

    private var connected = false
    private var resumed = false
    private var generation = 0L
    private var sessionJob: Job? = null

    val canClearErrors: Boolean
        get() = connected && resumed && _snapshot.value.saved != null &&
            _snapshot.value.clearStatus != ErrorsClearStatus.Clearing

    fun onConnectionState(state: ConnectionState) {
        if (state != Connected) {
            connected = false
            stopSession()
            return
        }
        if (connected) return
        connected = true
        startSessionIfEligible()
    }

    fun onRouteResumed() {
        if (resumed) return
        resumed = true
        startSessionIfEligible()
    }

    fun onRoutePaused() {
        if (!resumed && sessionJob == null) return
        resumed = false
        stopSession()
    }

    fun clearErrors() {
        if (!canClearErrors) return
        cancelWorkerPreservingSnapshot()
        _snapshot.value = _snapshot.value.copy(clearStatus = ErrorsClearStatus.Clearing, readStatus = ErrorsReadStatus.Idle)
        val session = generation
        sessionJob = scope.launch {
            try {
                performClear(session)
            } finally {
                if (generation == session) sessionJob = null
            }
        }
    }

    fun retryRead() {
        if (!connected || !resumed || _snapshot.value.readStatus == ErrorsReadStatus.Idle ||
            _snapshot.value.clearStatus == ErrorsClearStatus.Clearing) return
        cancelWorkerPreservingSnapshot()
        _snapshot.value = _snapshot.value.copy(saved = null, active = null, readStatus = ErrorsReadStatus.Idle)
        startSessionIfEligible()
    }

    fun close() {
        resumed = false
        connected = false
        stopSession()
    }

    private fun startSessionIfEligible() {
        if (!connected || !resumed || sessionJob != null) return
        val session = ++generation
        sessionJob = scope.launch {
            try {
                val saved = try {
                    withTimeout(initialReadTimeoutMillis) {
                        packets { if (isCurrent(session)) sendSavedRead() }
                            .filterIsInstance<CheckEngineSavedErrorsPacket>().first()
                    }
                } catch (_: TimeoutCancellationException) {
                    currentCoroutineContext().ensureActive()
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
            withTimeout(ackTimeoutMillis) {
                packets { if (isCurrent(session)) clearSavedErrors() }
                    .filterIsInstance<OpCompNc>()
                    .first { it.isSavedErrorsWriteAcknowledgement }
            }
        } catch (_: TimeoutCancellationException) {
            currentCoroutineContext().ensureActive()
            updateClearStatus(session, ErrorsClearStatus.AckTimeout)
            collectActiveErrors(session)
            return
        }
        if (!isCurrent(session)) return

        val refreshedSavedErrors = try {
            withTimeout(readbackTimeoutMillis) {
                packets { if (isCurrent(session)) sendSavedRead() }
                    .filterIsInstance<CheckEngineSavedErrorsPacket>()
                    .first()
            }
        } catch (_: TimeoutCancellationException) {
            currentCoroutineContext().ensureActive()
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
                    // Connection setup can send h/y concurrently with this read. Reassert v
                    // after its firmware packet so the ECU returns to the active-error mode.
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
        onSessionInvalidated()
    }

    private fun isCurrent(session: Long): Boolean = connected && resumed && generation == session
}
