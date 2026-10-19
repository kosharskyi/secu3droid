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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.secu3.android.connection.Connected
import org.secu3.android.connection.ConnectionState
import org.secu3.android.models.packets.base.InputPacket
import org.secu3.android.models.packets.input.CheckEngineErrorsPacket
import org.secu3.android.models.packets.input.FirmwareInfoPacket
import org.secu3.android.models.packets.out.CheckEngineSavedErrorsPacket

internal data class ErrorsSnapshot(
    val saved: CheckEngineSavedErrorsPacket? = null,
    val active: CheckEngineErrorsPacket? = null,
)

/** Owns error packets and reads only while the route is resumed and its transport is connected. */
internal class ErrorsSessionController(
    private val scope: CoroutineScope,
    private val packets: (onSubscribed: () -> Unit) -> Flow<InputPacket>,
    private val sendSavedRead: () -> Unit,
    private val sendActiveRead: () -> Unit,
    private val clearSavedErrors: () -> Unit,
    private val onSessionInvalidated: () -> Unit = {},
) {
    private val _snapshot = MutableStateFlow(ErrorsSnapshot())
    val snapshot = _snapshot.asStateFlow()

    private var connected = false
    private var resumed = false
    private var generation = 0L
    private var sessionJob: Job? = null

    val canClearErrors: Boolean get() = connected && resumed

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
        stopSession()
        startSessionIfEligible(afterSavedRequest = clearSavedErrors)
    }

    fun close() {
        resumed = false
        connected = false
        stopSession()
    }

    private fun startSessionIfEligible(afterSavedRequest: () -> Unit = {}) {
        if (!connected || !resumed || sessionJob != null) return
        val session = ++generation
        sessionJob = scope.launch {
            try {
                val saved = packets {
                    if (isCurrent(session)) {
                        sendSavedRead()
                        afterSavedRequest()
                    }
                }.filterIsInstance<CheckEngineSavedErrorsPacket>().first()
                if (!isCurrent(session)) return@launch
                _snapshot.value = _snapshot.value.copy(saved = saved)

                packets {
                    if (isCurrent(session)) sendActiveRead()
                }.collect { packet ->
                    if (!isCurrent(session)) return@collect
                    when (packet) {
                        is CheckEngineErrorsPacket -> _snapshot.value = _snapshot.value.copy(active = packet)
                        // Connection setup can send h/y concurrently with this read. Reassert v
                        // after its firmware packet so the ECU returns to the active-error mode.
                        is FirmwareInfoPacket -> sendActiveRead()
                    }
                }
            } finally {
                if (generation == session) sessionJob = null
            }
        }
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
