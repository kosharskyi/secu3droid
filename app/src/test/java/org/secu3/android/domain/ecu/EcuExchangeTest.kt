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

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onSubscription
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.secu3.android.models.packets.base.InputPacket
import org.secu3.android.models.packets.out.OpCompNc
import org.secu3.android.models.packets.out.params.StarterParamPacket
import java.util.concurrent.TimeoutException

class EcuExchangeTest {
    @Test
    fun subscribesBeforeSendingAndAcceptsImmediateMatchingReply() = runBlocking {
        val packets = MutableSharedFlow<InputPacket>()
        val exchange = EcuExchange { onSubscribed -> packets.onSubscription { onSubscribed() } }
        val expected = StarterParamPacket(starterOff = 42)
        var sent = false

        val result = exchange.request(
            timeoutMillis = 1_000,
            sendRequest = {
                assertTrue(packets.subscriptionCount.value > 0)
                sent = true
                launch(start = CoroutineStart.UNDISPATCHED) {
                    packets.emit(OpCompNc(opCode = 2))
                    packets.emit(expected)
                }
            },
            receive = { response -> response.filterIsInstance<StarterParamPacket>().first() },
        )

        assertTrue(sent)
        assertEquals(expected, result)
    }

    @Test
    fun ownDeadlineBecomesTimeoutExceptionButParentCancellationPropagates() = runBlocking {
        val packets = MutableSharedFlow<InputPacket>()
        val exchange = EcuExchange { onSubscribed -> packets.onSubscription { onSubscribed() } }
        val ownTimeout = runCatching {
            exchange.request(20, sendRequest = {}) { it.first() }
        }.exceptionOrNull()
        assertTrue(ownTimeout is TimeoutException)

        val parentCancellation = runCatching {
            withTimeout(20) {
                exchange.request(1_000, sendRequest = {}) { it.first() }
            }
        }.exceptionOrNull()
        assertTrue(parentCancellation is TimeoutCancellationException)
    }
}
