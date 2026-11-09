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

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
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
import org.secu3.android.utils.prefs.UserPrefs
import java.util.concurrent.TimeoutException
import javax.inject.Inject

enum class ErrorsReadStatus { Idle, Reading, Timeout }

enum class ErrorsWriteStatus { Idle, Writing, Succeeded, AckTimeout, ReadbackTimeout }

data class ErrorsSnapshot(
    val savedMask: Int? = null,
    val activeMask: Int? = null,
    val draftMask: Int? = null,
    val activeReadStatus: ErrorsReadStatus = ErrorsReadStatus.Idle,
    val savedReadStatus: ErrorsReadStatus = ErrorsReadStatus.Idle,
    val writeStatus: ErrorsWriteStatus = ErrorsWriteStatus.Idle,
    val liveEnabled: Boolean = true,
    val holdActiveErrors: Boolean = false,
    val autoReadSavedErrors: Boolean? = null,
) {
    private val canWriteSavedMask: Boolean
        get() = writeStatus == ErrorsWriteStatus.Idle || writeStatus == ErrorsWriteStatus.Succeeded

    val canReadSaved: Boolean
        get() = savedReadStatus != ErrorsReadStatus.Reading && draftMask == null && writeStatus != ErrorsWriteStatus.Writing

    val canEditSaved: Boolean
        get() = savedMask != null && savedReadStatus == ErrorsReadStatus.Idle && draftMask == null &&
            canWriteSavedMask

    val canWriteDraft: Boolean
        get() = draftMask != null && draftMask != savedMask && savedMask != null &&
            savedReadStatus == ErrorsReadStatus.Idle && canWriteSavedMask

    val canClearErrors: Boolean
        get() = canEditSaved
}

/** Owns live and EEPROM error reads, editable saved masks, and ECU write transactions. */
class ErrorsSession internal constructor(
    private val connectionStates: Flow<ConnectionState>,
    private val ecuExchange: EcuExchange,
    private val packets: (onSubscribed: () -> Unit) -> Flow<InputPacket>,
    private val savedReadSettings: Flow<Boolean>,
    private val sendSavedRead: () -> Unit,
    private val sendActiveRead: () -> Unit,
    private val sendSensorsMode: () -> Unit,
    private val writeSavedErrors: (Int) -> Unit,
    private val ackTimeoutMillis: Long = 10_000,
    private val readbackTimeoutMillis: Long = 10_000,
    private val initialReadTimeoutMillis: Long = 10_000,
) {
    @Inject
    constructor(
        connectionManager: Secu3ConnectionManager,
        ecuExchange: EcuExchange,
        userPrefs: UserPrefs,
    ) : this(
        connectionStates = connectionManager.connectionStateFlow,
        ecuExchange = ecuExchange,
        packets = connectionManager::receivedPacketFlow,
        savedReadSettings = userPrefs.observeSettings {
            Log.e("ErrorsSession", "Could not read saved-errors preference", it)
        }.map { it.autoReadSavedErrors },
        sendSavedRead = { connectionManager.sendNewTask(Task.Secu3ReadEcuSavedErrors) },
        sendActiveRead = { connectionManager.sendNewTask(Task.Secu3ReadEcuErrors) },
        sendSensorsMode = { connectionManager.sendNewTask(Task.Secu3ReadSensors) },
        writeSavedErrors = { mask -> connectionManager.sendOutPacket(CheckEngineSavedErrorsPacket(mask)) },
    )

    private val _snapshot = MutableStateFlow(ErrorsSnapshot())
    val snapshot = _snapshot.asStateFlow()

    private var connected = false
    private var resumed = false
    private var closed = false
    private var generation = 0L
    private var activeGeneration = 0L
    private var lastAutoReadSetting: Boolean? = null
    private var liveEnabled = true
    private var holdActiveErrors = false
    private var holdSettingsGeneration = 0L
    private var sessionScope: CoroutineScope? = null
    private var connectionJob: Job? = null
    private var settingsJob: Job? = null
    private var activeJob: Job? = null
    private var activeTimeoutJob: Job? = null
    private var savedJob: Job? = null
    private var writeJob: Job? = null
    private var holdExpiryJob: Job? = null

    fun start(scope: CoroutineScope) {
        if (closed || connectionJob != null) return
        sessionScope = scope
        connectionJob = scope.launch(start = kotlinx.coroutines.CoroutineStart.UNDISPATCHED) {
            connectionStates.collect(::onConnectionState)
        }
        settingsJob = scope.launch(start = kotlinx.coroutines.CoroutineStart.UNDISPATCHED) {
            savedReadSettings.collect(::onAutoReadSetting)
        }
    }

    fun onRouteResumed() {
        if (closed || resumed) return
        resumed = true
        _snapshot.value = ErrorsSnapshot(
            liveEnabled = liveEnabled,
            holdActiveErrors = holdActiveErrors,
            autoReadSavedErrors = lastAutoReadSetting,
        )
        startSessionIfEligible()
    }

    fun onRoutePaused() {
        if (!resumed && activeJob == null && savedJob == null && writeJob == null) return
        resumed = false
        stopSession()
    }

    fun setLiveEnabled(enabled: Boolean) {
        if (!connected || !resumed || closed || _snapshot.value.liveEnabled == enabled) return
        liveEnabled = enabled
        _snapshot.value = _snapshot.value.copy(
            activeMask = null,
            activeReadStatus = if (enabled) ErrorsReadStatus.Reading else ErrorsReadStatus.Idle,
            liveEnabled = enabled,
        )
        stopActiveRead()
        if (enabled) {
            startActiveRead(generation)
        } else {
            sendSensorsMode()
        }
    }

    fun setHoldActiveErrors(enabled: Boolean) {
        if (_snapshot.value.holdActiveErrors == enabled) return
        holdActiveErrors = enabled
        holdSettingsGeneration++
        _snapshot.value = _snapshot.value.copy(
            holdActiveErrors = enabled,
            activeMask = if (enabled) _snapshot.value.activeMask else rawActiveMask,
        )
        if (!enabled) {
            holdExpiryJob?.cancel()
            holdExpiryJob = null
        }
    }

    fun readSavedErrors() {
        if (!connected || !resumed || closed || !_snapshot.value.canReadSaved || writeJob != null) return
        startSavedRead(generation)
    }

    fun retryActiveRead() {
        if (!connected || !resumed || closed || !_snapshot.value.liveEnabled ||
            _snapshot.value.activeReadStatus != ErrorsReadStatus.Timeout
        ) return
        _snapshot.value = _snapshot.value.copy(activeReadStatus = ErrorsReadStatus.Reading)
        stopActiveRead()
        startActiveRead(generation)
    }

    fun beginEditing() {
        if (!_snapshot.value.canEditSaved) return
        _snapshot.value = _snapshot.value.copy(draftMask = _snapshot.value.savedMask)
    }

    fun setDraftError(bit: Int, selected: Boolean) {
        if (bit !in 0 until ERROR_COUNT) return
        updateDraft { draft -> if (selected) draft or (1 shl bit) else draft and (1 shl bit).inv() }
    }

    fun setAllDraftErrors(selected: Boolean) {
        val knownErrorsMask = ((1L shl ERROR_COUNT) - 1L).toInt()
        updateDraft { draft -> if (selected) draft or knownErrorsMask else draft and knownErrorsMask.inv() }
    }

    fun discardDraft() {
        if (_snapshot.value.draftMask == null || writeJob != null) return
        _snapshot.value = _snapshot.value.copy(draftMask = null)
    }

    fun writeDraft() {
        if (!connected || !resumed || closed || !_snapshot.value.canWriteDraft || writeJob != null) return
        startSavedWrite(requireNotNull(_snapshot.value.draftMask))
    }

    fun clearErrors() {
        if (!connected || !resumed || closed || !_snapshot.value.canClearErrors || writeJob != null) return
        startSavedWrite(0)
    }

    fun close() {
        if (closed) return
        closed = true
        resumed = false
        connected = false
        stopSession()
        connectionJob?.cancel()
        settingsJob?.cancel()
        connectionJob = null
        settingsJob = null
        sessionScope = null
    }

    private var rawActiveMask: Int? = null

    private fun onAutoReadSetting(enabled: Boolean) {
        val firstValue = lastAutoReadSetting == null
        lastAutoReadSetting = enabled
        _snapshot.value = _snapshot.value.copy(autoReadSavedErrors = enabled)
        if (firstValue && enabled && connected && resumed && _snapshot.value.canReadSaved) {
            readSavedErrors()
        }
    }

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
        if (sessionScope == null) return
        if (!connected || !resumed || closed) return
        if (_snapshot.value.liveEnabled && activeJob == null) {
            _snapshot.value = _snapshot.value.copy(activeReadStatus = ErrorsReadStatus.Reading)
            startActiveRead(generation)
        }
        if (lastAutoReadSetting == true && _snapshot.value.savedMask == null && savedJob == null) {
            startSavedRead(generation)
        }
    }

    private fun startActiveRead(session: Long) {
        if (activeJob != null || !isCurrent(session) || !_snapshot.value.liveEnabled) return
        val activeSession = ++activeGeneration
        val operation = sessionScope?.launch(start = CoroutineStart.LAZY) {
            try {
                coroutineScope {
                    var receivedActive = false
                    val visibleUntil = LongArray(Int.SIZE_BITS)
                    var handledHoldSettingsGeneration = holdSettingsGeneration

                    fun renderActiveMask(): Long? {
                        val now = System.nanoTime()
                        var displayedMask = rawActiveMask ?: return null
                        var nextExpiry: Long? = null
                        for (bit in 0 until Int.SIZE_BITS) {
                            val bitMask = 1 shl bit
                            val expiry = visibleUntil[bit]
                            if (expiry != 0L && expiry - now > 0L && displayedMask and bitMask == 0) {
                                displayedMask = displayedMask or bitMask
                                if (nextExpiry == null || expiry < nextExpiry) nextExpiry = expiry
                            }
                        }
                        if (isActiveCurrent(session, activeSession)) {
                            _snapshot.value = _snapshot.value.copy(activeMask = displayedMask)
                        }
                        return nextExpiry
                    }

                    fun scheduleHoldExpiry() {
                        holdExpiryJob?.cancel()
                        holdExpiryJob = null
                        if (!_snapshot.value.holdActiveErrors) return
                        if (renderActiveMask() == null) return
                        holdExpiryJob = launch {
                            while (isActiveCurrent(session, activeSession) && _snapshot.value.holdActiveErrors) {
                                val nextExpiry = renderActiveMask() ?: break
                                val remainingNanos = nextExpiry - System.nanoTime()
                                if (remainingNanos > 0) delay((remainingNanos + 999_999L) / 1_000_000L)
                            }
                        }
                    }

                    packets { if (isActiveCurrent(session, activeSession)) sendActiveRead() }.collect { packet ->
                        if (!isActiveCurrent(session, activeSession)) return@collect
                        when (packet) {
                            is CheckEngineErrorsPacket -> {
                                receivedActive = true
                                if (handledHoldSettingsGeneration != holdSettingsGeneration) {
                                    visibleUntil.fill(0L)
                                    handledHoldSettingsGeneration = holdSettingsGeneration
                                }
                                val now = System.nanoTime()
                                rawActiveMask = packet.errors
                                if (_snapshot.value.holdActiveErrors) {
                                    for (bit in 0 until Int.SIZE_BITS) {
                                        val bitMask = 1 shl bit
                                        if (packet.errors and bitMask != 0) {
                                            visibleUntil[bit] = now + HOLD_DURATION_NANOS
                                        }
                                    }
                                    scheduleHoldExpiry()
                                } else {
                                    _snapshot.value = _snapshot.value.copy(activeMask = packet.errors)
                                }
                                if (_snapshot.value.activeReadStatus == ErrorsReadStatus.Reading ||
                                    _snapshot.value.activeReadStatus == ErrorsReadStatus.Timeout
                                ) {
                                    _snapshot.value = _snapshot.value.copy(activeReadStatus = ErrorsReadStatus.Idle)
                                }
                                activeTimeoutJob?.cancel()
                            }
                            is FirmwareInfoPacket -> sendActiveReadIfCurrent(session, activeSession)
                        }
                    }
                    if (!receivedActive && isActiveCurrent(session, activeSession)) {
                        _snapshot.value = _snapshot.value.copy(activeMask = null)
                    }
                }
            } finally {
                if (activeJob === currentCoroutineContext()[Job]) activeJob = null
            }
        }
        activeJob = operation
        operation?.start()
        activeTimeoutJob?.cancel()
        activeTimeoutJob = sessionScope?.launch {
            delay(initialReadTimeoutMillis)
            if (isActiveCurrent(session, activeSession) && _snapshot.value.activeReadStatus == ErrorsReadStatus.Reading) {
                _snapshot.value = _snapshot.value.copy(activeReadStatus = ErrorsReadStatus.Timeout)
            }
        }
    }

    private suspend fun sendActiveReadIfCurrent(session: Long, activeSession: Long) {
        if (isActiveCurrent(session, activeSession)) sendActiveRead()
    }

    private fun startSavedRead(session: Long) {
        if (savedJob != null || !isCurrent(session) || !_snapshot.value.canReadSaved) return
        _snapshot.value = _snapshot.value.copy(savedReadStatus = ErrorsReadStatus.Reading)
        val operation = sessionScope?.launch(start = CoroutineStart.LAZY) {
            try {
                val saved = try {
                    ecuExchange.request(
                        timeoutMillis = initialReadTimeoutMillis,
                        sendRequest = { if (isCurrent(session)) sendSavedRead() },
                    ) { responsePackets ->
                        responsePackets.filterIsInstance<CheckEngineSavedErrorsPacket>().first()
                    }
                } catch (_: TimeoutException) {
                    if (isCurrent(session)) {
                        _snapshot.value = _snapshot.value.copy(
                            savedReadStatus = ErrorsReadStatus.Timeout,
                        )
                    }
                    return@launch
                }
                if (!isCurrent(session)) return@launch
                _snapshot.value = _snapshot.value.copy(
                    savedMask = saved.errors,
                    draftMask = null,
                    savedReadStatus = ErrorsReadStatus.Idle,
                    writeStatus = if (_snapshot.value.writeStatus == ErrorsWriteStatus.AckTimeout ||
                        _snapshot.value.writeStatus == ErrorsWriteStatus.ReadbackTimeout
                    ) ErrorsWriteStatus.Idle else _snapshot.value.writeStatus,
                )
            } finally {
                if (savedJob === currentCoroutineContext()[Job]) savedJob = null
            }
        }
        savedJob = operation
        operation?.start()
    }

    private fun startSavedWrite(mask: Int) {
        val session = generation
        savedJob?.cancel()
        savedJob = null
        _snapshot.value = _snapshot.value.copy(
            draftMask = null,
            writeStatus = ErrorsWriteStatus.Writing,
            savedReadStatus = ErrorsReadStatus.Idle,
        )
        val operation = sessionScope?.launch(start = CoroutineStart.LAZY) {
            try {
                try {
                    ecuExchange.request(
                        timeoutMillis = ackTimeoutMillis,
                        sendRequest = { if (isCurrent(session)) writeSavedErrors(mask) },
                    ) { responsePackets ->
                        responsePackets.filterIsInstance<OpCompNc>()
                            .first { it.isSavedErrorsWriteAcknowledgement }
                    }
                } catch (_: TimeoutException) {
                    updateWriteStatus(session, ErrorsWriteStatus.AckTimeout)
                    return@launch
                }
                if (!isCurrent(session)) return@launch

                val refreshedSavedErrors = try {
                    ecuExchange.request(
                        timeoutMillis = readbackTimeoutMillis,
                        sendRequest = { if (isCurrent(session)) sendSavedRead() },
                    ) { responsePackets ->
                        responsePackets.filterIsInstance<CheckEngineSavedErrorsPacket>().first()
                    }
                } catch (_: TimeoutException) {
                    updateWriteStatus(session, ErrorsWriteStatus.ReadbackTimeout)
                    return@launch
                }
                if (!isCurrent(session)) return@launch
                _snapshot.value = _snapshot.value.copy(
                    savedMask = refreshedSavedErrors.errors,
                    writeStatus = ErrorsWriteStatus.Succeeded,
                    savedReadStatus = ErrorsReadStatus.Idle,
                )
            } finally {
                if (writeJob === currentCoroutineContext()[Job]) writeJob = null
            }
        }
        writeJob = operation
        operation?.start()
    }

    private fun updateWriteStatus(session: Long, status: ErrorsWriteStatus) {
        if (isCurrent(session)) _snapshot.value = _snapshot.value.copy(writeStatus = status)
    }

    private fun updateDraft(transform: (Int) -> Int) {
        if (_snapshot.value.draftMask == null || writeJob != null) return
        _snapshot.value = _snapshot.value.copy(draftMask = transform(requireNotNull(_snapshot.value.draftMask)))
    }

    private fun stopActiveRead() {
        activeGeneration++
        activeJob?.cancel()
        activeJob = null
        activeTimeoutJob?.cancel()
        activeTimeoutJob = null
        holdExpiryJob?.cancel()
        holdExpiryJob = null
        rawActiveMask = null
    }

    private fun stopSession() {
        generation++
        stopActiveRead()
        savedJob?.cancel()
        savedJob = null
        writeJob?.cancel()
        writeJob = null
        _snapshot.value = ErrorsSnapshot(
            liveEnabled = liveEnabled,
            holdActiveErrors = holdActiveErrors,
            autoReadSavedErrors = lastAutoReadSetting,
        )
    }

    private fun isCurrent(session: Long): Boolean = !closed && connected && resumed && generation == session

    private fun isActiveCurrent(session: Long, activeSession: Long): Boolean =
        isCurrent(session) && activeGeneration == activeSession && _snapshot.value.liveEnabled

    companion object {
        const val ERROR_COUNT = 26
        const val HOLD_DURATION_MILLIS = 500L
        private const val HOLD_DURATION_NANOS = HOLD_DURATION_MILLIS * 1_000_000L
    }
}
