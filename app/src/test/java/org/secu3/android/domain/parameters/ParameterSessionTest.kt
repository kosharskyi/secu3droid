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

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.onSubscription
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.After
import org.junit.Test
import org.secu3.android.connection.Connected
import org.secu3.android.connection.Disconnected
import org.secu3.android.connection.ConnectionState
import org.secu3.android.domain.ecu.EcuExchange
import org.secu3.android.models.packets.base.InputPacket
import org.secu3.android.models.packets.base.OutputPacket
import org.secu3.android.models.packets.out.OpCompNc
import org.secu3.android.models.packets.out.params.AnglesParamPacket
import org.secu3.android.models.packets.out.params.StarterParamPacket
import org.secu3.android.utils.Task

class ParameterSessionTest {
    private val activeHarnesses = mutableListOf<SessionHarness>()

    @After
    fun cleanup() {
        activeHarnesses.forEach(SessionHarness::close)
        activeHarnesses.clear()
    }

    @Test
    fun successfulPagesAreCachedAndWritesInvalidateOnlyOtherPages() = runBlocking {
        val harness = newHarness()
        harness.start()
        yield()
        harness.session.activatePage(ParameterPage.STARTER)
        harness.awaitLoaded(ParameterPage.STARTER)
        harness.session.deactivatePage()

        harness.session.activatePage(ParameterPage.ANGLES)
        harness.awaitLoaded(ParameterPage.ANGLES)
        harness.session.deactivatePage()

        harness.session.activatePage(ParameterPage.STARTER)
        yield()
        assertEquals(1, harness.count(Task.Secu3ReadStarterParam))
        harness.session.sendPacket(object : OutputPacket { override fun pack() = intArrayOf('x'.code) })
        harness.session.deactivatePage()

        harness.session.activatePage(ParameterPage.STARTER)
        yield()
        assertEquals("The active page remains cached after a write", 1, harness.count(Task.Secu3ReadStarterParam))
        harness.session.deactivatePage()
        harness.session.activatePage(ParameterPage.ANGLES)
        harness.awaitLoaded(ParameterPage.ANGLES, expectedCount = 2)
        assertEquals(2, harness.count(Task.Secu3ReadAnglesParam))

        harness.session.refreshPage(ParameterPage.ANGLES)
        harness.awaitLoaded(ParameterPage.ANGLES, expectedCount = 3)
        assertEquals(3, harness.count(Task.Secu3ReadAnglesParam))
    }

    @Test
    fun pauseRejectsLateReplyAndResumeStartsFreshRead() = runBlocking {
        val harness = newHarness(replyImmediately = false)
        harness.start()
        yield()
        harness.session.activatePage(ParameterPage.STARTER)
        harness.awaitRequest(Task.Secu3ReadStarterParam)
        harness.session.pause()
        yield()
        harness.packets.emit(StarterParamPacket(starterOff = 77))
        harness.repliesEnabled = true
        harness.session.activatePage(ParameterPage.STARTER)
        harness.awaitRequest(Task.Secu3ReadStarterParam, expectedCount = 2)
        harness.awaitLoaded(ParameterPage.STARTER)

        assertEquals(2, harness.count(Task.Secu3ReadStarterParam))
        assertEquals(1, harness.events.filterIsInstance<ParameterSessionEvent.PageLoaded>().count { it.page == ParameterPage.STARTER })
        assertTrue(harness.requests.contains(Task.Secu3ReadSensors))
    }

    @Test
    fun connectionLossInvalidatesAndBlocksOperationsUntilConnectedEvent() = runBlocking {
        val harness = newHarness()
        harness.start()
        yield()
        harness.session.activatePage(ParameterPage.STARTER)
        harness.awaitLoaded(ParameterPage.STARTER)
        val commandCount = harness.requests.size
        harness.transportConnected = false
        assertFalse(harness.session.sendPacket(object : OutputPacket { override fun pack() = intArrayOf('x'.code) }))
        harness.session.activatePage(ParameterPage.ANGLES)
        yield()
        assertEquals(commandCount, harness.requests.size)

        harness.connectionStates.emit(Disconnected)
        harness.transportConnected = true
        harness.connectionStates.emit(Connected)
        yield()
        val commandsAfterLoss = harness.requests.size
        assertEquals(commandCount, commandsAfterLoss)
        harness.session.activatePage(ParameterPage.STARTER)
        harness.awaitLoaded(ParameterPage.STARTER, expectedCount = 2)
        assertEquals(2, harness.count(Task.Secu3ReadStarterParam))
    }

    @Test
    fun pendingSaveIsSingleFlightAndConnectionLossCancelsItsConfirmation() = runBlocking {
        val harness = newHarness(replyImmediately = false)
        harness.start()
        yield()
        harness.session.save()
        harness.awaitRequest(Task.Secu3OpComSaveEeprom)
        harness.session.save()
        yield()
        assertEquals(1, harness.count(Task.Secu3OpComSaveEeprom))
        assertEquals(1, harness.events.count { it == ParameterSessionEvent.SaveStarted })

        harness.session.pause()
        yield()
        assertFalse(harness.events.any { it is ParameterSessionEvent.SaveFinished })
        harness.session.save()
        assertEquals("Save admission is closed while paused", 1, harness.count(Task.Secu3OpComSaveEeprom))
        harness.repliesEnabled = true
        harness.session.activatePage(ParameterPage.STARTER)
        harness.awaitLoaded(ParameterPage.STARTER)

        harness.session.save()
        harness.awaitRequest(Task.Secu3OpComSaveEeprom, expectedCount = 2)
        harness.connectionStates.emit(Disconnected)
        yield()
        harness.packets.emit(OpCompNc(opCode = 1))
        harness.connectionStates.emit(Connected)
        yield()
        assertFalse(harness.events.any { it is ParameterSessionEvent.SaveFinished })

        harness.session.save()
        harness.awaitRequest(Task.Secu3OpComSaveEeprom, expectedCount = 3)
        harness.packets.emit(OpCompNc(opCode = 1))
        withTimeout(1_000) {
            while (harness.events.filterIsInstance<ParameterSessionEvent.SaveFinished>().isEmpty()) yield()
        }
        assertEquals(EepromSaveResult.Confirmed, harness.events.filterIsInstance<ParameterSessionEvent.SaveFinished>().single().result)
    }

    @Test
    fun nextPageCapturesWriteRevisionAfterPreviousReadCleanup() = runBlocking {
        val harness = newHarness(replyImmediately = false, holdReadCleanup = true)
        harness.start()
        yield()
        harness.session.activatePage(ParameterPage.STARTER)
        harness.awaitRequest(Task.Secu3ReadStarterParam)
        harness.session.activatePage(ParameterPage.ANGLES)
        withTimeout(1_000) { harness.cleanupStarted.await() }

        harness.session.sendPacket(object : OutputPacket { override fun pack() = intArrayOf('x'.code) })
        harness.releaseCleanup.complete(Unit)
        harness.awaitRequest(Task.Secu3ReadAnglesParam)
        harness.packets.emit(AnglesParamPacket(maxAngle = 9f))
        harness.awaitLoaded(ParameterPage.ANGLES)
        harness.session.deactivatePage()
        harness.session.activatePage(ParameterPage.ANGLES)
        yield()
        assertEquals("The read waited for cleanup and began after the write", 1, harness.count(Task.Secu3ReadAnglesParam))
    }

    private fun newHarness(replyImmediately: Boolean = true, holdReadCleanup: Boolean = false) =
        SessionHarness(replyImmediately, holdReadCleanup).also(activeHarnesses::add)

    private class SessionHarness(replyImmediately: Boolean = true, holdReadCleanup: Boolean = false) {
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val connectionStates = MutableSharedFlow<ConnectionState>(replay = 1, extraBufferCapacity = 8).also { it.tryEmit(Connected) }
        val packets = MutableSharedFlow<InputPacket>()
        val requests = mutableListOf<Task>()
        val events = mutableListOf<ParameterSessionEvent>()
        var transportConnected = true
        var repliesEnabled = replyImmediately
        val cleanupStarted = kotlinx.coroutines.CompletableDeferred<Unit>()
        val releaseCleanup = kotlinx.coroutines.CompletableDeferred<Unit>()

        private val exchange = ParameterExchange(
            ecuExchange = EcuExchange { onSubscribed ->
                packets.onSubscription { onSubscribed() }.onCompletion { cause ->
                    if (holdReadCleanup && cause is kotlinx.coroutines.CancellationException) {
                        withContext(NonCancellable) {
                            cleanupStarted.complete(Unit)
                            releaseCleanup.await()
                        }
                    }
                }
            },
            sendTask = { task ->
                requests += task
                if (repliesEnabled) {
                    when (task) {
                        Task.Secu3ReadStarterParam -> scope.launch { packets.emit(StarterParamPacket(starterOff = 3)) }
                        Task.Secu3ReadAnglesParam -> scope.launch { packets.emit(AnglesParamPacket(maxAngle = 5f)) }
                        else -> Unit
                    }
                }
            },
            isConnected = { transportConnected },
            timeoutMillis = 1_000,
        )

        val session = ParameterSession(
            connectionStates = connectionStates,
            isTransportConnected = { transportConnected },
            firmware = { null },
            exchange = exchange,
            sendPacketToEcu = { },
        )

        fun start() = session.start(scope) { events += it }

        fun close() {
            releaseCleanup.complete(Unit)
            session.closePageSession()
            scope.cancel()
        }

        fun count(task: Task) = requests.count { it == task }

        suspend fun awaitRequest(task: Task, expectedCount: Int = 1) {
            withTimeout(1_000) {
                while (count(task) < expectedCount) yield()
            }
        }

        suspend fun awaitLoaded(page: ParameterPage, expectedCount: Int = 1) {
            val loaded = withTimeoutOrNull(1_000) {
                while (events.filterIsInstance<ParameterSessionEvent.PageLoaded>().count { it.page == page } < expectedCount) yield()
                true
            }
            assertTrue("No loaded $page event; requests=$requests events=$events", loaded == true)
        }
    }
}
