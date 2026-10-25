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
import org.secu3.android.domain.ecu.EcuExchange
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.secu3.android.domain.parameters.ParameterExchange
import org.secu3.android.models.packets.base.InputPacket
import org.secu3.android.models.packets.input.FnNameDatPacket
import org.secu3.android.models.packets.out.params.AnglesParamPacket
import org.secu3.android.models.packets.out.params.FunSetParamPacket
import org.secu3.android.models.packets.out.params.StarterParamPacket
import org.secu3.android.utils.Task

class ParameterPageReadGateTest {
    @Test
    fun disconnectedReadIsCancelledAndReconnectWaitsForFreshPacket() = runBlocking {
        val gate = ParameterPageReadGate()
        val packets = MutableSharedFlow<InputPacket>()
        val requests = Channel<Task>(Channel.UNLIMITED)
        val exchange = parameterExchange(requests, packets)
        suspend fun read() = gate.read(0) {
            exchange.readPacket(Task.Secu3ReadStarterParam, StarterParamPacket::class.java)
        }.first()
        val old = async(start = CoroutineStart.UNDISPATCHED) { read() }
        gate.activate(0)
        assertEquals(Task.Secu3ReadStarterParam, withTimeout(1_000) { requests.receive() })
        gate.deactivate()
        old.cancel()
        old.join()
        assertEquals(Task.Secu3ReadSensors, withTimeout(1_000) { requests.receive() })
        packets.emit(StarterParamPacket(starterOff = 99)) // A reply from the cancelled read has no active subscriber.
        val fresh = async(start = CoroutineStart.UNDISPATCHED) { read() }
        gate.activate(0)
        assertEquals(Task.Secu3ReadStarterParam, withTimeout(1_000) { requests.receive() })
        assertFalse(fresh.isCompleted)
        val freshPacket = StarterParamPacket(starterOff = 1)
        packets.emit(freshPacket)
        assertEquals(freshPacket, withTimeout(1_000) { fresh.await() })
        assertEquals(Task.Secu3ReadSensors, requests.receive())
    }

    @Test
    fun completedPageCanReadChangedSharedFieldsOnReactivation() = runBlocking {
        val gate = ParameterPageReadGate()
        val sent = mutableListOf<IntArray>()
        val holder = org.secu3.android.ui.parameters.pages.compose.FunctionsStateHolder { sent += it.pack() }
        holder.onNamesReceived(org.secu3.android.models.packets.input.FnNameDatPacket())
        var volumePerCylinder = .5f
        var requests = 0
        suspend fun reload() {
            val packet = gate.read(4) {
                requests++
                org.secu3.android.models.packets.out.params.FunSetParamPacket(ckpsEngineCyl = 4, injCylDisp = volumePerCylinder)
            }.first()
            holder.onPacketReceived(packet, emptyList(), emptyList(), emptyList())
        }
        gate.activate(4)
        reload()
        gate.activate(14) // Injection changes a value also present in FUNSET.
        volumePerCylinder = .75f
        gate.activate(4)
        reload()
        holder.updateFlag(true)
        assertEquals(2, requests)
        assertEquals(listOf(0x30, 0x00), sent.single().slice(26..27))
    }

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
            val requests = Channel<Task>(Channel.UNLIMITED)
            val packets = MutableSharedFlow<InputPacket>()
            val exchange = parameterExchange(requests, packets)
            val pages = (0..1).map { page ->
                async(start = CoroutineStart.UNDISPATCHED) {
                    gate.read(page) {
                        if (page == 0) {
                            exchange.readPacket(Task.Secu3ReadStarterParam, StarterParamPacket::class.java)
                        } else {
                            exchange.readPacket(Task.Secu3ReadAnglesParam, AnglesParamPacket::class.java)
                        }
                    }.first()
                }
            }
            gate.activate(0)
            assertEquals(Task.Secu3ReadStarterParam, withTimeout(1_000) { requests.receive() })
            gate.activate(1)
            assertEquals(Task.Secu3ReadSensors, withTimeout(1_000) { requests.receive() })
            assertEquals(Task.Secu3ReadAnglesParam, withTimeout(1_000) { requests.receive() })
            packets.emit(StarterParamPacket(starterOff = 99)) // The second page must ignore the old packet type.
            assertFalse(pages[0].isCompleted)
            val secondPage = AnglesParamPacket()
            packets.emit(secondPage)
            assertEquals(secondPage, withTimeout(1_000) { pages[1].await() })
            assertEquals(Task.Secu3ReadSensors, requests.receive())

            gate.activate(0)
            assertEquals(Task.Secu3ReadStarterParam, withTimeout(1_000) { requests.receive() })
            val freshPacket = StarterParamPacket(starterOff = 1)
            packets.emit(freshPacket)
            assertEquals(freshPacket, withTimeout(1_000) { pages[0].await() })
            assertEquals(Task.Secu3ReadSensors, requests.receive())
        }
    }

    @Test
    fun pauseCancelsPendingReadAndDoesNotRestartUntilResume() {
        runBlocking {
            val gate = ParameterPageReadGate()
            val requests = Channel<Task>(Channel.UNLIMITED)
            val packets = MutableSharedFlow<InputPacket>()
            val exchange = parameterExchange(requests, packets)
            val page = async(start = CoroutineStart.UNDISPATCHED) {
                gate.read(0) {
                    exchange.readPacket(Task.Secu3ReadStarterParam, StarterParamPacket::class.java)
                }.first()
            }
            gate.activate(0)
            assertEquals(Task.Secu3ReadStarterParam, withTimeout(1_000) { requests.receive() })
            gate.deactivate()
            assertEquals(Task.Secu3ReadSensors, withTimeout(1_000) { requests.receive() })
            yield()
            assertFalse(page.isCompleted)
            assertTrue(requests.tryReceive().isFailure)
            gate.activate(0)
            assertEquals(Task.Secu3ReadStarterParam, withTimeout(1_000) { requests.receive() })
            val resumed = StarterParamPacket(starterOff = 2)
            packets.emit(resumed)
            assertEquals(resumed, withTimeout(1_000) { page.await() })
            assertEquals(Task.Secu3ReadSensors, requests.receive())
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
    fun functionsReadCancelsBeforeSecondStageAndRetriesBothStagesOnReturn() {
        runBlocking {
            val gate = ParameterPageReadGate()
            val requests = Channel<Task>(Channel.UNLIMITED)
            val packets = MutableSharedFlow<InputPacket>()
            val exchange = parameterExchange(requests, packets)
            val functionsPage = 10
            val read = async(start = CoroutineStart.UNDISPATCHED) {
                gate.read(functionsPage) { exchange.readFunctions() }.first()
            }

            gate.activate(functionsPage)
            assertEquals(Task.Secu3ReadFnNameDat, withTimeout(1_000) { requests.receive() })
            gate.activate(functionsPage + 1)
            assertEquals(Task.Secu3ReadSensors, withTimeout(1_000) { requests.receive() })
            yield()
            assertTrue("Cancelled names stage must not issue the funset request", requests.tryReceive().isFailure)

            gate.activate(functionsPage)
            assertEquals(Task.Secu3ReadFnNameDat, withTimeout(1_000) { requests.receive() })
            packets.emit(FnNameDatPacket(tablesNumber = 1, fnName = org.secu3.android.models.FnName(0, "table")))
            assertEquals(Task.Secu3ReadSensors, withTimeout(1_000) { requests.receive() })
            assertEquals(Task.Secu3ReadFunsetParam, withTimeout(1_000) { requests.receive() })
            val settings = FunSetParamPacket()
            packets.emit(settings)
            val result = withTimeout(1_000) { read.await() }
            assertEquals(listOf(org.secu3.android.models.FnName(0, "table")), result.names.fnNameList)
            assertTrue(result.settings === settings)
            assertEquals(Task.Secu3ReadSensors, withTimeout(1_000) { requests.receive() })
        }
    }

    private fun parameterExchange(requests: Channel<Task>, packets: MutableSharedFlow<InputPacket>) =
        ParameterExchange(
            ecuExchange = EcuExchange { onSubscribed -> packets.onSubscription { onSubscribed() } },
            sendTask = { requests.trySend(it) },
            isConnected = { true },
            timeoutMillis = 1_000,
        )
}
