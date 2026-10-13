/*
 *    SecuDroid  - An open source, free manager for SECU-3 engine control unit
 *    Copyright (C) 2026 Vitalii O. Kosharskyi. Ukraine, Kyiv
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 */
package org.secu3.android.ui.parameters

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onSubscription
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ParameterPageReadGateTest {
    @Test
    fun preloadedAndSkippedPagesDoNotRead() {
        runBlocking {
            val gate = ParameterPageReadGate()
            val commands = Channel<Int>(Channel.UNLIMITED)
            val pages = (0..2).map { page ->
                async(start = CoroutineStart.UNDISPATCHED) {
                    gate.read(page) { commands.send(page); page }.first()
                }
            }
            gate.activate(0)
            assertEquals(0, withTimeout(1_000) { commands.receive() })
            assertEquals(0, pages[0].await())
            assertFalse(pages[1].isCompleted)
            assertFalse(pages[2].isCompleted)
            assertTrue(commands.tryReceive().isFailure)

            gate.activate(2)
            assertEquals(2, withTimeout(1_000) { commands.receive() })
            assertEquals(2, pages[2].await())
            assertFalse("The skipped neighbor must not read", pages[1].isCompleted)
            assertTrue(commands.tryReceive().isFailure)
            pages[1].cancel()
        }
    }

    @Test
    fun switchingCancelsOldReadBeforeChangingModeAndRetriesOnReturn() {
        runBlocking {
            val gate = ParameterPageReadGate()
            val events = Channel<String>(Channel.UNLIMITED)
            val packets = MutableSharedFlow<Pair<Int, String>>()
            val pages = (0..1).map { page ->
                async(start = CoroutineStart.UNDISPATCHED) {
                    gate.read(page) {
                        readParameterPacket(
                            sendRequest = { events.trySend("read$page") },
                            restoreSensors = { events.trySend("sensors$page") },
                        ) { ready -> packets.onSubscription { ready() }.first { it.first == page }.second }
                    }.first()
                }
            }
            gate.activate(0)
            assertEquals("read0", withTimeout(1_000) { events.receive() })
            gate.activate(1)
            assertEquals("sensors0", withTimeout(1_000) { events.receive() })
            assertEquals("read1", withTimeout(1_000) { events.receive() })
            packets.emit(0 to "late old response")
            assertFalse(pages[0].isCompleted)
            packets.emit(1 to "second page")
            assertEquals("second page", withTimeout(1_000) { pages[1].await() })
            assertEquals("sensors1", events.receive())

            gate.activate(0)
            assertEquals("read0", withTimeout(1_000) { events.receive() })
            packets.emit(0 to "fresh response")
            assertEquals("fresh response", withTimeout(1_000) { pages[0].await() })
            assertEquals("sensors0", events.receive())
        }
    }

    @Test
    fun pauseCancelsPendingReadAndDoesNotRestartUntilResume() {
        runBlocking {
            val gate = ParameterPageReadGate()
            val events = Channel<String>(Channel.UNLIMITED)
            val response = CompletableDeferred<String>()
            val page = async(start = CoroutineStart.UNDISPATCHED) {
                gate.read(0) {
                    readParameterPacket(
                        sendRequest = { events.trySend("read") },
                        restoreSensors = { events.trySend("sensors") },
                    ) { ready -> ready(); response.await() }
                }.first()
            }
            gate.activate(0)
            assertEquals("read", withTimeout(1_000) { events.receive() })
            gate.deactivate()
            assertEquals("sensors", withTimeout(1_000) { events.receive() })
            yield()
            assertFalse(page.isCompleted)
            assertTrue(events.tryReceive().isFailure)
            gate.activate(0)
            assertEquals("read", withTimeout(1_000) { events.receive() })
            response.complete("resumed")
            assertEquals("resumed", withTimeout(1_000) { page.await() })
            assertEquals("sensors", events.receive())
        }
    }

    @Test
    fun newPageWaitsForCancellationCleanup() {
        runBlocking {
            val gate = ParameterPageReadGate()
            val started = CompletableDeferred<Unit>()
            val cleaning = CompletableDeferred<Unit>()
            val releaseCleanup = CompletableDeferred<Unit>()
            val first = async(start = CoroutineStart.UNDISPATCHED) {
                gate.read(0) {
                    try {
                        started.complete(Unit)
                        CompletableDeferred<Unit>().await()
                    } finally {
                        withContext(NonCancellable) {
                            cleaning.complete(Unit)
                            releaseCleanup.await()
                        }
                    }
                }.first()
            }
            val secondStarted = CompletableDeferred<Unit>()
            val second = async(start = CoroutineStart.UNDISPATCHED) {
                gate.read(1) { secondStarted.complete(Unit) }.first()
            }
            gate.activate(0)
            withTimeout(1_000) { started.await() }
            gate.activate(1)
            withTimeout(1_000) { cleaning.await() }
            yield()
            assertFalse(secondStarted.isCompleted)
            releaseCleanup.complete(Unit)
            withTimeout(1_000) { second.await() }
            assertTrue(secondStarted.isCompleted)
            first.cancel()
        }
    }

    @Test
    fun activeCollectorIsReadyBeforeRequestAndImmediateResponseIsReceived() {
        runBlocking {
            val packets = MutableSharedFlow<String>()
            var sensorRestores = 0
            val result = readParameterPacket(
                sendRequest = {
                    assertEquals(1, packets.subscriptionCount.value)
                    launch(start = CoroutineStart.UNDISPATCHED) { packets.emit("immediate") }
                },
                restoreSensors = { sensorRestores++ },
            ) { ready -> packets.onSubscription { ready() }.first() }
            assertEquals("immediate", result)
            assertEquals(1, sensorRestores)
        }
    }
}
