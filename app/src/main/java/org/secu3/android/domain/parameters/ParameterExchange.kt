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

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withTimeout
import org.secu3.android.connection.Secu3ConnectionManager
import org.secu3.android.models.FnName
import org.secu3.android.models.packets.base.InputPacket
import org.secu3.android.models.packets.input.FnNameDatPacket
import org.secu3.android.models.packets.out.OpCompNc
import org.secu3.android.models.packets.out.params.FunSetParamPacket
import org.secu3.android.utils.Task
import java.util.concurrent.TimeoutException
import javax.inject.Inject
import javax.inject.Singleton

data class FunctionParameters(
    val names: FnNameDatPacket,
    val settings: FunSetParamPacket,
)

enum class EepromSaveResult {
    Confirmed,
    Unconfirmed,
    NotStarted,
}

@Singleton
class ParameterExchange internal constructor(
    private val sendTask: (Task) -> Unit,
    private val receivedPackets: (onSubscribed: () -> Unit) -> Flow<InputPacket>,
    private val isConnected: () -> Boolean,
    private val timeoutMillis: Long = RESPONSE_TIMEOUT_MILLIS,
) {
    @Inject
    constructor(connectionManager: Secu3ConnectionManager) : this(
        sendTask = connectionManager::sendNewTask,
        receivedPackets = connectionManager::receivedPacketFlow,
        isConnected = { connectionManager.isConnected },
    )

    private val saveRequestMutex = Mutex()

    suspend fun <T : InputPacket> readPacket(task: Task, packetType: Class<T>): T = request(
        task = task,
        restoreSensors = true,
    ) { packets ->
        packetType.cast(packets.first { packetType.isInstance(it) })
            ?: error("Received packet was not an instance of ${packetType.simpleName}")
    }

    suspend fun readFunctions(): FunctionParameters {
        val names = request(Task.Secu3ReadFnNameDat, restoreSensors = true) { packets ->
            var namesPacket: FnNameDatPacket? = null
            packets.filterIsInstance<FnNameDatPacket>().first { packet ->
                val accumulated = namesPacket ?: packet.also {
                    it.fnNameList = MutableList(packet.tablesNumber) { FnName(-1, "placeholder name") }
                    namesPacket = it
                }
                accumulated.fnNameList[packet.fnName.index] = packet.fnName
                accumulated.isAllFnNamesReceived
            }
            checkNotNull(namesPacket)
        }
        val settings = readPacket(Task.Secu3ReadFunsetParam, FunSetParamPacket::class.java)
        return FunctionParameters(names, settings)
    }

    suspend fun saveEeprom(onStarted: () -> Unit = {}): EepromSaveResult {
        if (!saveRequestMutex.tryLock()) return EepromSaveResult.NotStarted
        try {
            if (!isConnected()) return EepromSaveResult.NotStarted

            request(
                task = Task.Secu3OpComSaveEeprom,
                restoreSensors = false,
                onRequestStarted = onStarted,
            ) { packets ->
                packets.filterIsInstance<OpCompNc>().first { it.isEepromParamSave }
            }
            return EepromSaveResult.Confirmed
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            return EepromSaveResult.Unconfirmed
        } finally {
            saveRequestMutex.unlock()
        }
    }

    private suspend fun <T> request(
        task: Task,
        restoreSensors: Boolean,
        onRequestStarted: () -> Unit = {},
        receive: suspend (Flow<InputPacket>) -> T,
    ): T {
        try {
            return try {
                withTimeout(timeoutMillis) {
                    receive(receivedPackets {
                        sendTask(task)
                        onRequestStarted()
                    })
                }
            } catch (timeout: TimeoutCancellationException) {
                currentCoroutineContext().ensureActive()
                throw TimeoutException("ECU parameter response timed out").apply { initCause(timeout) }
            }
        } finally {
            if (restoreSensors) sendTask(Task.Secu3ReadSensors)
        }
    }

    private companion object {
        const val RESPONSE_TIMEOUT_MILLIS = 10_000L
    }
}
