/*
 *    SecuDroid  - An open source, free manager for SECU-3 engine control unit
 *    Copyright (C) 2026 Vitalii O. Kosharskyi. Ukraine, Kyiv
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 */
package org.secu3.android.domain.ecu

import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withTimeout
import org.secu3.android.connection.Secu3ConnectionManager
import org.secu3.android.models.packets.base.InputPacket
import java.util.concurrent.TimeoutException
import javax.inject.Inject
import javax.inject.Singleton

/** Owns subscribe-before-command ordering and the deadline for one ECU response exchange. */
@Singleton
class EcuExchange internal constructor(
    private val receivedPackets: (onSubscribed: () -> Unit) -> Flow<InputPacket>,
) {
    @Inject
    constructor(connectionManager: Secu3ConnectionManager) : this(connectionManager::receivedPacketFlow)

    suspend fun <T> request(
        timeoutMillis: Long,
        sendRequest: () -> Unit,
        receive: suspend (Flow<InputPacket>) -> T,
    ): T {
        try {
            return withTimeout(timeoutMillis) {
                receive(receivedPackets { sendRequest() })
            }
        } catch (timeout: TimeoutCancellationException) {
            currentCoroutineContext().ensureActive()
            throw TimeoutException("ECU response timed out").apply { initCause(timeout) }
        }
    }
}
