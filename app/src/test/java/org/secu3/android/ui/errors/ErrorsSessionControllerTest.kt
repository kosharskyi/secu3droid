package org.secu3.android.ui.errors

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onSubscription
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.secu3.android.connection.Connected
import org.secu3.android.connection.ConnectionAttempt
import org.secu3.android.connection.Disconnected
import org.secu3.android.models.packets.base.InputPacket
import org.secu3.android.models.packets.input.CheckEngineErrorsPacket
import org.secu3.android.models.packets.input.FirmwareInfoPacket
import org.secu3.android.models.packets.out.CheckEngineSavedErrorsPacket
import org.secu3.android.models.packets.out.OpCompNc

class ErrorsSessionControllerTest {
    @Test
    fun initiallyDisconnectedDoesNothingAndConnectStartsOnceDespiteDuplicateConnectedEvents() = runBlocking {
        val fixture = Fixture(this)
        fixture.controller.onConnectionState(Disconnected)
        fixture.controller.onRouteResumed()
        assertTrue(fixture.commands.isEmpty())

        fixture.controller.onConnectionState(Connected)
        fixture.controller.onConnectionState(Connected)
        fixture.awaitSubscriptions(1)
        fixture.awaitCommands(listOf("saved"))
        assertEquals(listOf("saved"), fixture.commands)

        fixture.close()
    }

    @Test
    fun disconnectClearsBothPacketsAndReconnectReadsSavedBeforeActiveAgain() = runBlocking {
        val fixture = Fixture(this)
        fixture.resumeConnected()
        fixture.awaitSubscriptions(1)
        fixture.packets.emit(CheckEngineSavedErrorsPacket(errors = 3))
        fixture.awaitSnapshot { it.saved?.errors == 3 }
        fixture.awaitCommands(listOf("saved", "active"))
        fixture.packets.emit(CheckEngineErrorsPacket(errors = 5))
        fixture.awaitSnapshot { it.active?.errors == 5 }

        fixture.controller.onConnectionState(ConnectionAttempt(1))
        assertEquals(ErrorsSnapshot(), fixture.controller.snapshot.value)
        fixture.awaitSubscriptions(0)
        fixture.controller.onConnectionState(Connected)
        fixture.awaitSubscriptions(1)
        fixture.awaitCommands(listOf("saved", "active", "saved"))
        assertEquals(listOf("saved", "active", "saved"), fixture.commands)
        assertEquals(ErrorsSnapshot(), fixture.controller.snapshot.value)

        fixture.packets.emit(CheckEngineSavedErrorsPacket(errors = 7))
        fixture.awaitSnapshot { it.saved?.errors == 7 }
        fixture.awaitCommands(listOf("saved", "active", "saved", "active"))
        assertEquals(7, fixture.controller.snapshot.value.saved?.errors)
        assertEquals(listOf("saved", "active", "saved", "active"), fixture.commands)
        fixture.close()
    }

    @Test
    fun disconnectDuringSavedWaitCancelsOldCollectorAndFreshConnectionCanComplete() = runBlocking {
        val fixture = Fixture(this)
        fixture.resumeConnected()
        fixture.awaitSubscriptions(1)
        fixture.controller.onConnectionState(Disconnected)
        fixture.awaitSubscriptions(0)
        fixture.packets.emit(CheckEngineSavedErrorsPacket(errors = 1))
        assertEquals(ErrorsSnapshot(), fixture.controller.snapshot.value)

        fixture.controller.onConnectionState(Connected)
        fixture.awaitSubscriptions(1)
        val fresh = CheckEngineSavedErrorsPacket(errors = 9)
        fixture.packets.emit(fresh)
        fixture.awaitSnapshot { it.saved == fresh }
        assertEquals(fresh, fixture.controller.snapshot.value.saved)
        fixture.close()
    }

    @Test
    fun rapidDisconnectReconnectWhileSavedReadPendingStartsOnlyFreshActiveRead() = runBlocking {
        val fixture = Fixture(this)
        fixture.resumeConnected()
        fixture.awaitSubscriptions(1)
        fixture.awaitCommands(listOf("saved"))

        // Do not yield between the loss and reconnect events.
        fixture.controller.onConnectionState(ConnectionAttempt(1))
        fixture.controller.onConnectionState(Connected)
        fixture.awaitCommands(listOf("saved", "saved"))
        fixture.awaitSubscriptions(1)

        fixture.packets.emit(CheckEngineErrorsPacket(errors = 0x7))
        assertEquals(null, fixture.controller.snapshot.value.active)
        fixture.packets.emit(CheckEngineSavedErrorsPacket(errors = 9))
        fixture.awaitSnapshot { it.saved?.errors == 9 }
        fixture.awaitCommands(listOf("saved", "saved", "active"))
        assertEquals(null, fixture.controller.snapshot.value.active)

        fixture.packets.emit(CheckEngineErrorsPacket(errors = 0x2))
        fixture.awaitSnapshot { it.active?.errors == 0x2 }
        assertEquals(listOf("saved", "saved", "active"), fixture.commands)
        fixture.close()
    }

    @Test
    fun inactiveRouteDoesNotReadAndResumeRefreshesSession() = runBlocking {
        val fixture = Fixture(this)
        fixture.controller.onConnectionState(Connected)
        assertTrue(fixture.commands.isEmpty())

        fixture.controller.onRouteResumed()
        fixture.awaitSubscriptions(1)
        assertEquals(listOf("saved"), fixture.commands)
        fixture.controller.onRoutePaused()
        fixture.awaitSubscriptions(0)
        assertEquals(ErrorsSnapshot(), fixture.controller.snapshot.value)

        fixture.controller.onRouteResumed()
        fixture.awaitSubscriptions(1)
        assertEquals(listOf("saved", "saved"), fixture.commands)
        fixture.controller.onRoutePaused()
        fixture.awaitSubscriptions(0)
        fixture.controller.onConnectionState(Disconnected)
        fixture.controller.onConnectionState(Connected)
        assertEquals(listOf("saved", "saved"), fixture.commands)
        fixture.controller.onRouteResumed()
        fixture.awaitSubscriptions(1)
        fixture.awaitCommands(listOf("saved", "saved", "saved"))
        fixture.close()
    }

    @Test
    fun immediateSavedResponseIsRetainedAndFirmwarePacketReassertsActiveMode() = runBlocking {
        val fixture = Fixture(this)
        val immediate = CheckEngineSavedErrorsPacket(errors = 0x1234)
        fixture.onSavedRead = {
            assertEquals(1, fixture.packets.subscriptionCount.value)
            launch(start = CoroutineStart.UNDISPATCHED) {
                fixture.packets.emit(CheckEngineErrorsPacket(errors = 0x7))
                fixture.packets.emit(immediate)
            }
        }
        fixture.resumeConnected()
        fixture.awaitSubscriptions(1)
        fixture.awaitSnapshot { it.saved == immediate }
        assertEquals(null, fixture.controller.snapshot.value.active)
        fixture.awaitCommands(listOf("saved", "active"))
        fixture.awaitSubscriptions(1)

        fixture.packets.emit(FirmwareInfoPacket(tag = "startup"))
        fixture.awaitCommands(listOf("saved", "active", "active"))
        assertEquals(listOf("saved", "active", "active"), fixture.commands)
        fixture.close()
    }

    @Test
    fun clearReadIsSessionOwnedAndCannotSendAfterDisconnect() = runBlocking {
        val fixture = Fixture(this)
        fixture.resumeConnected()
        fixture.awaitSubscriptions(1)
        fixture.awaitCommands(listOf("saved"))
        fixture.packets.emit(CheckEngineSavedErrorsPacket(errors = 3))
        fixture.awaitCommands(listOf("saved", "active"))
        fixture.controller.clearErrors()
        fixture.awaitCommands(listOf("saved", "active", "clear"))
        fixture.controller.clearErrors()
        assertEquals(listOf("saved", "active", "clear"), fixture.commands)
        fixture.packets.emit(OpCompNc(opData = 0, opCode = 2))
        fixture.awaitCommands(listOf("saved", "active", "clear", "saved"))
        fixture.packets.emit(CheckEngineSavedErrorsPacket(errors = 1))
        fixture.awaitCommands(listOf("saved", "active", "clear", "saved", "active"))
        assertEquals(ErrorsClearStatus.Succeeded, fixture.controller.snapshot.value.clearStatus)

        fixture.controller.onConnectionState(Disconnected)
        fixture.awaitSubscriptions(0)
        fixture.controller.clearErrors()
        assertFalse(fixture.commands.drop(5).contains("clear"))
        fixture.close()
    }

    @Test
    fun clearWaitsForMatchingAcknowledgementThenFreshSavedReadBeforeActiveMode() = runBlocking {
        val fixture = Fixture(this)
        fixture.resumeConnected()
        fixture.awaitSubscriptions(1)
        fixture.awaitCommands(listOf("saved"))
        fixture.packets.emit(CheckEngineSavedErrorsPacket(errors = 3))
        fixture.awaitCommands(listOf("saved", "active"))
        fixture.packets.emit(CheckEngineErrorsPacket(errors = 0x80))
        fixture.awaitSnapshot { it.active?.errors == 0x80 }
        fixture.controller.clearErrors()
        fixture.awaitCommands(listOf("saved", "active", "clear"))
        assertEquals(ErrorsClearStatus.Clearing, fixture.controller.snapshot.value.clearStatus)
        assertEquals(3, fixture.controller.snapshot.value.saved?.errors)
        assertEquals(0x80, fixture.controller.snapshot.value.active?.errors)

        fixture.packets.emit(CheckEngineSavedErrorsPacket(errors = 0)) // x is not the write ACK
        fixture.packets.emit(OpCompNc(opData = 1, opCode = 2))
        fixture.packets.emit(OpCompNc(opData = 0, opCode = 1))
        assertEquals(listOf("saved", "active", "clear"), fixture.commands)
        assertEquals(3, fixture.controller.snapshot.value.saved?.errors)
        assertEquals(0x80, fixture.controller.snapshot.value.active?.errors)

        fixture.packets.emit(OpCompNc(opData = 0, opCode = 2))
        fixture.awaitCommands(listOf("saved", "active", "clear", "saved"))
        val refreshed = CheckEngineSavedErrorsPacket(errors = 0x40)
        fixture.packets.emit(refreshed)
        fixture.awaitCommands(listOf("saved", "active", "clear", "saved", "active"))
        assertEquals(refreshed, fixture.controller.snapshot.value.saved)
        assertEquals(ErrorsClearStatus.Succeeded, fixture.controller.snapshot.value.clearStatus)
        fixture.close()
    }

    @Test
    fun clearBeforeInitialSavedReadCompletesSendsNoWrite() = runBlocking {
        val fixture = Fixture(this)
        fixture.resumeConnected()
        fixture.awaitSubscriptions(1)
        fixture.awaitCommands(listOf("saved"))
        fixture.controller.clearErrors()
        assertEquals(listOf("saved"), fixture.commands)
        assertEquals(ErrorsClearStatus.Idle, fixture.controller.snapshot.value.clearStatus)
        fixture.close()
    }

    @Test
    fun immediateAcknowledgementAndReadbackAreAcceptedWithoutTreatingNonzeroMaskAsFailure() = runBlocking {
        val fixture = Fixture(this)
        fixture.onClearWrite = {
            launch(start = CoroutineStart.UNDISPATCHED) {
                fixture.packets.emit(OpCompNc(opData = 0, opCode = 2))
            }
        }
        fixture.onSavedRead = {
            if (fixture.commands.count { it == "saved" } > 1) {
                launch(start = CoroutineStart.UNDISPATCHED) {
                    fixture.packets.emit(CheckEngineSavedErrorsPacket(errors = 0x20))
                }
            }
        }
        fixture.resumeConnected()
        fixture.awaitSubscriptions(1)
        fixture.packets.emit(CheckEngineSavedErrorsPacket(errors = 2))
        fixture.awaitCommands(listOf("saved", "active"))
        fixture.controller.clearErrors()
        fixture.awaitSnapshot { it.clearStatus == ErrorsClearStatus.Succeeded }
        fixture.awaitCommands(listOf("saved", "active", "clear", "saved", "active"))
        assertEquals(0x20, fixture.controller.snapshot.value.saved?.errors)
        fixture.close()
    }

    @Test
    fun acknowledgementTimeoutReportsUncertaintyDoesNotRetryWriteAndRestoresActiveRead() = runBlocking {
        val fixture = Fixture(this, ackTimeoutMillis = 20)
        fixture.startWithSavedErrors()
        fixture.controller.clearErrors()
        fixture.awaitSnapshot { it.clearStatus == ErrorsClearStatus.AckTimeout }
        fixture.awaitCommands(listOf("saved", "active", "clear", "active"))
        assertEquals(1, fixture.commands.count { it == "clear" })
        assertEquals(3, fixture.controller.snapshot.value.saved?.errors)
        fixture.close()
    }

    @Test
    fun readbackTimeoutReportsUncertaintyDoesNotRetryWriteAndRestoresActiveRead() = runBlocking {
        val fixture = Fixture(this, readbackTimeoutMillis = 20)
        fixture.startWithSavedErrors()
        fixture.controller.clearErrors()
        fixture.awaitCommands(listOf("saved", "active", "clear"))
        fixture.packets.emit(OpCompNc(opData = 0, opCode = 2))
        fixture.awaitSnapshot { it.clearStatus == ErrorsClearStatus.ReadbackTimeout }
        fixture.awaitCommands(listOf("saved", "active", "clear", "saved", "active"))
        assertEquals(1, fixture.commands.count { it == "clear" })
        assertEquals(3, fixture.controller.snapshot.value.saved?.errors)
        fixture.close()
    }

    @Test
    fun disconnectDuringClearAckWaitCancelsWorkerAndFreshSessionIsUnaffected() = runBlocking {
        val fixture = Fixture(this)
        fixture.startWithSavedErrors()
        fixture.controller.clearErrors()
        fixture.awaitCommands(listOf("saved", "active", "clear"))
        fixture.controller.onConnectionState(Disconnected)
        fixture.awaitSubscriptions(0)
        fixture.packets.emit(OpCompNc(opData = 0, opCode = 2))
        fixture.controller.onConnectionState(Connected)
        fixture.awaitCommands(listOf("saved", "active", "clear", "saved"))
        assertEquals(ErrorsSnapshot(), fixture.controller.snapshot.value)
        fixture.close()
    }

    @Test
    fun routePauseDuringReadbackWaitCancelsWorkerAndFreshResumeReadsNormally() = runBlocking {
        val fixture = Fixture(this)
        fixture.startWithSavedErrors()
        fixture.controller.clearErrors()
        fixture.awaitCommands(listOf("saved", "active", "clear"))
        fixture.packets.emit(OpCompNc(opData = 0, opCode = 2))
        fixture.awaitCommands(listOf("saved", "active", "clear", "saved"))
        fixture.controller.onRoutePaused()
        fixture.awaitSubscriptions(0)
        fixture.packets.emit(CheckEngineSavedErrorsPacket(errors = 0))
        assertEquals(ErrorsSnapshot(), fixture.controller.snapshot.value)

        fixture.controller.onRouteResumed()
        fixture.awaitCommands(listOf("saved", "active", "clear", "saved", "saved"))
        fixture.packets.emit(CheckEngineSavedErrorsPacket(errors = 8))
        fixture.awaitSnapshot { it.saved?.errors == 8 }
        fixture.awaitCommands(listOf("saved", "active", "clear", "saved", "saved", "active"))
        assertEquals(ErrorsClearStatus.Idle, fixture.controller.snapshot.value.clearStatus)
        fixture.close()
    }

    private class Fixture(
        private val scope: kotlinx.coroutines.CoroutineScope,
        ackTimeoutMillis: Long = 1_000,
        readbackTimeoutMillis: Long = 1_000,
    ) {
        val packets = MutableSharedFlow<InputPacket>()
        val commands = mutableListOf<String>()
        var onSavedRead: (() -> Unit)? = null
        var onClearWrite: (() -> Unit)? = null
        val controller = ErrorsSessionController(
            scope = scope,
            packets = { onSubscribed -> packets.onSubscription { onSubscribed() } },
            sendSavedRead = { commands += "saved"; onSavedRead?.invoke() },
            sendActiveRead = { commands += "active" },
            clearSavedErrors = { commands += "clear"; onClearWrite?.invoke() },
            ackTimeoutMillis = ackTimeoutMillis,
            readbackTimeoutMillis = readbackTimeoutMillis,
        )

        fun resumeConnected() {
            controller.onRouteResumed()
            controller.onConnectionState(Connected)
        }

        suspend fun startWithSavedErrors() {
            resumeConnected()
            awaitSubscriptions(1)
            awaitCommands(listOf("saved"))
            packets.emit(CheckEngineSavedErrorsPacket(errors = 3))
            awaitCommands(listOf("saved", "active"))
        }

        suspend fun awaitSubscriptions(expected: Int) {
            withTimeout(1_000) { packets.subscriptionCount.first { it == expected } }
        }

        suspend fun awaitSnapshot(expected: ErrorsSnapshot) {
            awaitSnapshot { it == expected }
        }

        suspend fun awaitSnapshot(predicate: (ErrorsSnapshot) -> Boolean) {
            withTimeout(1_000) { controller.snapshot.first(predicate) }
        }

        suspend fun awaitCommands(expected: List<String>) {
            withTimeout(1_000) {
                while (commands != expected) kotlinx.coroutines.yield()
            }
        }

        fun close() = controller.close()
    }
}
