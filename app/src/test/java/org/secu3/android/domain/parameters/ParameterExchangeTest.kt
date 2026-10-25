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
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.onSubscription
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.TimeoutCancellationException
import org.secu3.android.domain.ecu.EcuExchange
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.secu3.android.models.FnName
import org.secu3.android.models.packets.base.InputPacket
import org.secu3.android.models.packets.input.FnNameDatPacket
import org.secu3.android.models.packets.out.OpCompNc
import org.secu3.android.models.packets.out.params.StarterParamPacket
import org.secu3.android.utils.Task
import java.util.concurrent.TimeoutException

class ParameterExchangeTest {
    @Test
    fun immediateReplyIsReceivedAndSensorsAreRestoredAfterRequest() = runBlocking {
        val harness = ExchangeHarness(this)
        val expected = StarterParamPacket(starterOff = 42)
        harness.onSend = { task ->
            if (task == Task.Secu3ReadStarterParam) {
                assertTrue(harness.packets.subscriptionCount.value > 0)
                launch(start = CoroutineStart.UNDISPATCHED) { harness.packets.emit(expected) }
            }
        }

        val result = harness.exchange.readPacket(Task.Secu3ReadStarterParam, StarterParamPacket::class.java)

        assertEquals(expected, result)
        assertEquals(listOf(Task.Secu3ReadStarterParam, Task.Secu3ReadSensors), harness.requests)
    }

    @Test
    fun timeoutRestoresSensorsAndParentCancellationRemainsCancellation() = runBlocking {
        val harness = ExchangeHarness(this, timeoutMillis = 25)

        val timeout = runCatching {
            harness.exchange.readPacket(Task.Secu3ReadStarterParam, StarterParamPacket::class.java)
        }.exceptionOrNull()
        assertTrue(timeout is TimeoutException)
        assertEquals(listOf(Task.Secu3ReadStarterParam, Task.Secu3ReadSensors), harness.requests)

        harness.requests.clear()
        val longerWait = ExchangeHarness(this, timeoutMillis = 1_000)
        val cancelled = runCatching {
            withTimeout(25) {
                longerWait.exchange.readPacket(Task.Secu3ReadStarterParam, StarterParamPacket::class.java)
            }
        }.exceptionOrNull()
        assertTrue(cancelled is TimeoutCancellationException)
        assertEquals(listOf(Task.Secu3ReadStarterParam, Task.Secu3ReadSensors), longerWait.requests)
    }

    @Test
    fun functionReadCollectsEveryFragmentBeforeReadingSettings() = runBlocking {
        val harness = ExchangeHarness(this)
        val result = async { harness.exchange.readFunctions() }

        assertEquals(Task.Secu3ReadFnNameDat, withTimeout(1_000) { harness.sentTasks.receive() })
        harness.packets.emit(functionName(1, "second"))
        assertFalse(result.isCompleted)
        harness.packets.emit(functionName(0, "first"))
        assertEquals(Task.Secu3ReadSensors, withTimeout(1_000) { harness.sentTasks.receive() })
        assertEquals(Task.Secu3ReadFunsetParam, withTimeout(1_000) { harness.sentTasks.receive() })

        val settings = org.secu3.android.models.packets.out.params.FunSetParamPacket()
        harness.packets.emit(settings)
        val parameters = withTimeout(1_000) { result.await() }

        assertEquals(listOf(FnName(0, "first"), FnName(1, "second")), parameters.names.fnNameList)
        assertTrue(parameters.settings === settings)
        assertEquals(Task.Secu3ReadSensors, harness.sentTasks.receive())
    }

    @Test
    fun saveIgnoresOtherOperationAcksAndDoesNotRestoreSensors() = runBlocking {
        val harness = ExchangeHarness(this)
        val started = CompletableDeferred<Unit>()
        val otherAckSent = CompletableDeferred<Unit>()
        harness.onSend = { task ->
            if (task == Task.Secu3OpComSaveEeprom) {
                assertTrue(harness.packets.subscriptionCount.value > 0)
                launch(start = CoroutineStart.UNDISPATCHED) {
                    harness.packets.emit(OpCompNc(opCode = 2))
                    otherAckSent.complete(Unit)
                }
            }
        }
        val save = async { harness.exchange.saveEeprom { started.complete(Unit) } }

        assertEquals(Task.Secu3OpComSaveEeprom, withTimeout(1_000) { harness.sentTasks.receive() })
        withTimeout(1_000) { started.await() }
        withTimeout(1_000) { otherAckSent.await() }
        assertFalse(save.isCompleted)
        harness.packets.emit(OpCompNc(opCode = 1))

        assertEquals(EepromSaveResult.Confirmed, withTimeout(1_000) { save.await() })
        assertEquals(listOf(Task.Secu3OpComSaveEeprom), harness.requests)
    }

    @Test
    fun saveTimeoutIsUnconfirmedAndReleasesSingleFlightGuard() = runBlocking {
        val harness = ExchangeHarness(this, timeoutMillis = 25)
        var startedSignals = 0

        assertEquals(EepromSaveResult.Unconfirmed, harness.exchange.saveEeprom { startedSignals++ })
        assertEquals(1, startedSignals)
        assertEquals(listOf(Task.Secu3OpComSaveEeprom), harness.requests)

        harness.onSend = { task ->
            if (task == Task.Secu3OpComSaveEeprom) {
                launch(start = CoroutineStart.UNDISPATCHED) {
                    harness.packets.emit(OpCompNc(opCode = 1))
                }
            }
        }
        assertEquals(EepromSaveResult.Confirmed, harness.exchange.saveEeprom { startedSignals++ })
        assertEquals(2, startedSignals)
        assertEquals(listOf(Task.Secu3OpComSaveEeprom, Task.Secu3OpComSaveEeprom), harness.requests)
    }

    @Test
    fun disconnectedAndDuplicateSaveRequestsDoNotSendAndCancellationDoesNotConfirm() = runBlocking {
        val harness = ExchangeHarness(this)
        var startedSignals = 0
        harness.connected = false
        assertEquals(EepromSaveResult.NotStarted, harness.exchange.saveEeprom { startedSignals++ })
        assertEquals(0, startedSignals)
        assertTrue(harness.requests.isEmpty())

        harness.connected = true
        val save = async(start = CoroutineStart.UNDISPATCHED) { harness.exchange.saveEeprom { startedSignals++ } }
        assertEquals(Task.Secu3OpComSaveEeprom, withTimeout(1_000) { harness.sentTasks.receive() })
        assertEquals(EepromSaveResult.NotStarted, harness.exchange.saveEeprom { startedSignals++ })
        assertEquals(1, startedSignals)
        save.cancelAndJoin()
        assertEquals(listOf(Task.Secu3OpComSaveEeprom), harness.requests)

        val nextSave = async(start = CoroutineStart.UNDISPATCHED) { harness.exchange.saveEeprom { startedSignals++ } }
        assertEquals(Task.Secu3OpComSaveEeprom, withTimeout(1_000) { harness.sentTasks.receive() })
        assertEquals(2, startedSignals)
        assertFalse(nextSave.isCompleted)
        nextSave.cancelAndJoin()
        assertEquals(listOf(Task.Secu3OpComSaveEeprom, Task.Secu3OpComSaveEeprom), harness.requests)
    }

    @Test
    fun cancelledRequestIsNotReportedAsUnconfirmed() = runBlocking {
        val harness = ExchangeHarness(this)
        val result = async(start = CoroutineStart.UNDISPATCHED) { harness.exchange.saveEeprom() }
        assertEquals(Task.Secu3OpComSaveEeprom, withTimeout(1_000) { harness.sentTasks.receive() })
        result.cancel()
        val failure = runCatching { result.await() }.exceptionOrNull()
        assertTrue(failure is CancellationException)
    }

    private fun functionName(index: Int, name: String) = FnNameDatPacket(
        tablesNumber = 2,
        fnName = FnName(index, name),
    )

    private class ExchangeHarness(
        private val scope: kotlinx.coroutines.CoroutineScope,
        timeoutMillis: Long = 1_000,
    ) {
        val packets = MutableSharedFlow<InputPacket>()
        val requests = mutableListOf<Task>()
        val sentTasks = kotlinx.coroutines.channels.Channel<Task>(kotlinx.coroutines.channels.Channel.UNLIMITED)
        var connected = true
        var onSend: ((Task) -> Unit)? = null

        val exchange = ParameterExchange(
            ecuExchange = EcuExchange { onSubscribed -> packets.onSubscription { onSubscribed() } },
            sendTask = { task ->
                requests += task
                sentTasks.trySend(task)
                onSend?.invoke(task)
            },
            isConnected = { connected },
            timeoutMillis = timeoutMillis,
        )
    }
}
