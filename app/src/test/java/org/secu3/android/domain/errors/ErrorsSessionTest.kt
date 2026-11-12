package org.secu3.android.domain.errors

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onSubscription
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.secu3.android.connection.Connected
import org.secu3.android.connection.ConnectionAttempt
import org.secu3.android.connection.Disconnected
import org.secu3.android.domain.ecu.EcuExchange
import org.secu3.android.models.packets.base.InputPacket
import org.secu3.android.models.packets.input.CheckEngineErrorsPacket
import org.secu3.android.models.packets.input.FirmwareInfoPacket
import org.secu3.android.models.packets.out.CheckEngineSavedErrorsPacket
import org.secu3.android.models.packets.out.OpCompNc

class ErrorsSessionTest {
    @Test
    fun activeErrorsStartWithoutWaitingForSavedPreferenceOrResponse() = runBlocking {
        val fixture = Fixture(this, autoReadSaved = false)
        fixture.session.onRouteResumed()
        fixture.emitState(Connected)
        fixture.awaitCommands(listOf("active"))
        fixture.packets.emit(CheckEngineErrorsPacket(errors = 0x5))
        fixture.awaitSnapshot { it.activeMask == 0x5 && it.activeReadStatus == ErrorsReadStatus.Idle }
        assertNull(fixture.session.snapshot.value.savedMask)

        fixture.autoRead.value = true
        delay(30)
        assertEquals(listOf("active"), fixture.commands)
        fixture.session.onRoutePaused()
        fixture.session.onRouteResumed()
        fixture.awaitCommands(listOf("active", "active", "saved"))
        assertEquals(ErrorsReadStatus.Reading, fixture.session.snapshot.value.savedReadStatus)
        fixture.packets.emit(CheckEngineErrorsPacket(errors = 0x5))
        fixture.packets.emit(CheckEngineSavedErrorsPacket(errors = 0x2))
        fixture.awaitSnapshot { it.savedMask == 0x2 && it.activeMask == 0x5 }
        fixture.close()
    }

    @Test
    fun savedReadTimeoutDoesNotHideActiveErrorsAndManualReadRecoversSavedState() = runBlocking {
        val fixture = Fixture(this, initialReadTimeoutMillis = 80)
        fixture.startConnected()
        fixture.awaitCommands(listOf("active", "saved"))
        fixture.packets.emit(CheckEngineErrorsPacket(errors = 0x4))
        fixture.awaitSnapshot { it.activeMask == 0x4 }
        fixture.awaitSnapshot { it.savedReadStatus == ErrorsReadStatus.Timeout }
        assertEquals(0x4, fixture.session.snapshot.value.activeMask)

        fixture.session.readSavedErrors()
        fixture.awaitCommands(listOf("active", "saved", "saved"))
        fixture.packets.emit(CheckEngineSavedErrorsPacket(errors = 0x8))
        fixture.awaitSnapshot { it.savedMask == 0x8 && it.savedReadStatus == ErrorsReadStatus.Idle }
        fixture.close()
    }

    @Test
    fun savedReadCollectorIsSubscribedBeforeAnImmediateResponse() = runBlocking {
        val fixture = Fixture(this)
        fixture.onSavedRead = {
            assertTrue(fixture.packets.subscriptionCount.value >= 2)
            launch(start = CoroutineStart.UNDISPATCHED) {
                fixture.packets.emit(CheckEngineSavedErrorsPacket(errors = 0x40))
            }
        }
        fixture.startConnected()
        fixture.awaitSnapshot { it.savedMask == 0x40 }
        assertEquals(listOf("active", "saved"), fixture.commands)
        fixture.close()
    }

    @Test
    fun activeTimeoutKeepsCollectorForLatePacketsAndCanBeRetried() = runBlocking {
        val fixture = Fixture(this, autoReadSaved = false, initialReadTimeoutMillis = 80)
        fixture.startConnected()
        fixture.awaitSnapshot { it.activeReadStatus == ErrorsReadStatus.Timeout }
        fixture.session.retryActiveRead()
        fixture.awaitCommands(listOf("active", "active"))
        fixture.packets.emit(CheckEngineErrorsPacket(errors = 0x2))
        fixture.awaitSnapshot { it.activeMask == 0x2 && it.activeReadStatus == ErrorsReadStatus.Idle }
        fixture.close()
    }

    @Test
    fun liveReadingRestartsAfterPauseAndReconnectAndRetainsHoldSetting() = runBlocking {
        val fixture = Fixture(this, autoReadSaved = false)
        fixture.startConnected()
        fixture.awaitCommands(listOf("active"))
        fixture.session.setHoldActiveErrors(true)
        fixture.packets.emit(CheckEngineErrorsPacket(errors = 0x20))
        fixture.awaitSnapshot { it.activeMask == 0x20 }

        fixture.session.onRoutePaused()
        fixture.awaitSubscriptions(0)
        assertNull(fixture.session.snapshot.value.activeMask)
        fixture.session.onRouteResumed()
        fixture.awaitCommands(listOf("active", "active"))
        assertTrue(fixture.session.snapshot.value.holdActiveErrors)
        fixture.packets.emit(CheckEngineErrorsPacket(errors = 0x40))
        fixture.awaitSnapshot { it.activeMask == 0x40 }

        fixture.emitState(ConnectionAttempt(1))
        fixture.awaitSubscriptions(0)
        assertNull(fixture.session.snapshot.value.activeMask)
        fixture.emitState(Connected)
        fixture.awaitCommands(listOf("active", "active", "active"))
        assertTrue(fixture.session.snapshot.value.holdActiveErrors)
        fixture.close()
    }

    @Test
    fun firmwareInformationReassertsLiveModeWhileTheRouteIsResumed() = runBlocking {
        val fixture = Fixture(this, autoReadSaved = false)
        fixture.startConnected()
        fixture.awaitCommands(listOf("active"))
        fixture.packets.emit(FirmwareInfoPacket(tag = "startup"))
        fixture.awaitCommands(listOf("active", "active"))
        fixture.session.onRoutePaused()
        fixture.awaitSubscriptions(0)
        fixture.packets.emit(FirmwareInfoPacket(tag = "later"))
        assertEquals(listOf("active", "active"), fixture.commands)
        fixture.close()
    }

    @Test
    fun savedErrorDraftOnlyRemovesKnownSavedBitsAndWritesOnlyAfterUserRequest() = runBlocking {
        val unknownBit = 1 shl 30
        val fixture = Fixture(this)
        fixture.startConnected()
        fixture.awaitCommands(listOf("active", "saved"))
        fixture.packets.emit(CheckEngineSavedErrorsPacket(errors = unknownBit or 0x3))
        fixture.packets.emit(CheckEngineErrorsPacket(errors = 0x4))
        fixture.awaitSnapshot { it.savedMask == (unknownBit or 0x3) && it.activeMask == 0x4 }

        fixture.session.beginEditing()
        fixture.session.setDraftError(2, true)
        fixture.session.setDraftError(3, true)
        fixture.session.setDraftError(30, false)
        assertEquals(unknownBit or 0x3, fixture.session.snapshot.value.draftMask)
        assertFalse(fixture.session.snapshot.value.canWriteDraft)
        fixture.session.setDraftError(1, false)
        assertEquals(unknownBit or 0x1, fixture.session.snapshot.value.draftMask)
        assertTrue(fixture.session.snapshot.value.canWriteDraft)
        fixture.session.setDraftError(1, true)
        assertFalse(fixture.session.snapshot.value.canWriteDraft)
        fixture.session.setDraftError(0, false)
        fixture.session.setDraftError(1, false)
        assertEquals(unknownBit, fixture.session.snapshot.value.draftMask)
        assertFalse(fixture.session.snapshot.value.canReadSaved)
        assertEquals(listOf("active", "saved"), fixture.commands)

        fixture.session.writeDraft()
        fixture.awaitCommands(listOf("active", "saved", "write:${unknownBit}"))
        assertEquals(ErrorsWriteStatus.Writing, fixture.session.snapshot.value.writeStatus)
        fixture.packets.emit(OpCompNc(opData = 0, opCode = 2))
        fixture.awaitCommands(listOf("active", "saved", "write:${unknownBit}", "saved"))
        fixture.packets.emit(CheckEngineSavedErrorsPacket(errors = unknownBit))
        fixture.awaitSnapshot { it.writeStatus == ErrorsWriteStatus.Succeeded && it.savedMask == unknownBit }
        assertFalse(fixture.session.snapshot.value.canEditSaved)
        assertTrue(fixture.session.snapshot.value.canClearErrors)
        fixture.close()
    }

    @Test
    fun clearWritesZeroAfterConfirmationCallbackAndUsesAcknowledgementReadback() = runBlocking {
        val fixture = Fixture(this)
        fixture.startConnected()
        fixture.awaitCommands(listOf("active", "saved"))
        fixture.packets.emit(CheckEngineSavedErrorsPacket(errors = 0x100))
        fixture.awaitSnapshot { it.canClearErrors }
        fixture.session.clearErrors()
        fixture.awaitCommands(listOf("active", "saved", "write:0"))
        fixture.packets.emit(CheckEngineSavedErrorsPacket(errors = 0))
        assertEquals(0x100, fixture.session.snapshot.value.savedMask)
        fixture.packets.emit(OpCompNc(opData = 0, opCode = 2))
        fixture.awaitCommands(listOf("active", "saved", "write:0", "saved"))
        fixture.packets.emit(CheckEngineSavedErrorsPacket(errors = 0x100))
        fixture.awaitSnapshot { it.writeStatus == ErrorsWriteStatus.Succeeded && it.savedMask == 0x100 }
        fixture.close()
    }

    @Test
    fun writeAcknowledgementMustMatchAndTimeoutRequiresFreshReadBeforeNextWrite() = runBlocking {
        val fixture = Fixture(this, ackTimeoutMillis = 50)
        fixture.startConnected()
        fixture.awaitCommands(listOf("active", "saved"))
        fixture.packets.emit(CheckEngineSavedErrorsPacket(errors = 0x1))
        fixture.awaitSnapshot { it.canEditSaved }
        fixture.session.beginEditing()
        fixture.session.setDraftError(0, false)
        fixture.session.writeDraft()
        fixture.awaitCommands(listOf("active", "saved", "write:0"))
        fixture.packets.emit(OpCompNc(opData = 1, opCode = 2))
        fixture.packets.emit(OpCompNc(opData = 0, opCode = 1))
        fixture.awaitSnapshot { it.writeStatus == ErrorsWriteStatus.AckTimeout }
        assertEquals(1, fixture.commands.count { it.startsWith("write:") })
        assertTrue(fixture.session.snapshot.value.canReadSaved)
        assertFalse(fixture.session.snapshot.value.canClearErrors)

        fixture.session.readSavedErrors()
        fixture.awaitCommands(listOf("active", "saved", "write:0", "saved"))
        fixture.packets.emit(CheckEngineSavedErrorsPacket(errors = 0x1))
        fixture.awaitSnapshot { it.savedMask == 0x1 && it.writeStatus == ErrorsWriteStatus.Idle }
        assertEquals(1, fixture.commands.count { it.startsWith("write:") })
        fixture.close()
    }

    @Test
    fun readbackTimeoutDoesNotRepeatWrite() = runBlocking {
        val fixture = Fixture(this, readbackTimeoutMillis = 50)
        fixture.startConnected()
        fixture.awaitCommands(listOf("active", "saved"))
        fixture.packets.emit(CheckEngineSavedErrorsPacket(errors = 0x3))
        fixture.awaitSnapshot { it.canEditSaved }
        fixture.session.beginEditing()
        fixture.session.setDraftError(1, false)
        fixture.session.writeDraft()
        fixture.awaitCommands(listOf("active", "saved", "write:1"))
        fixture.packets.emit(OpCompNc(opData = 0, opCode = 2))
        fixture.awaitCommands(listOf("active", "saved", "write:1", "saved"))
        fixture.awaitSnapshot { it.writeStatus == ErrorsWriteStatus.ReadbackTimeout }
        assertTrue(fixture.session.snapshot.value.canReadSaved)
        assertEquals(1, fixture.commands.count { it.startsWith("write:") })
        fixture.close()
    }

    @Test
    fun routePauseCancelsWriteReadbackAndResumeStartsFreshReads() = runBlocking {
        val fixture = Fixture(this)
        fixture.startConnected()
        fixture.awaitCommands(listOf("active", "saved"))
        fixture.packets.emit(CheckEngineSavedErrorsPacket(errors = 0x3))
        fixture.awaitSnapshot { it.canEditSaved }
        fixture.session.beginEditing()
        fixture.session.setDraftError(0, false)
        fixture.session.writeDraft()
        fixture.awaitCommands(listOf("active", "saved", "write:2"))
        fixture.packets.emit(OpCompNc(opData = 0, opCode = 2))
        fixture.awaitCommands(listOf("active", "saved", "write:2", "saved"))

        fixture.session.onRoutePaused()
        fixture.awaitSubscriptions(0)
        fixture.packets.emit(CheckEngineSavedErrorsPacket(errors = 0))
        assertEquals(null, fixture.session.snapshot.value.savedMask)
        assertEquals(1, fixture.commands.count { it.startsWith("write:") })

        fixture.session.onRouteResumed()
        fixture.awaitCommands(listOf("active", "saved", "write:2", "saved", "active", "saved"))
        fixture.packets.emit(CheckEngineSavedErrorsPacket(errors = 0x9))
        fixture.awaitSnapshot { it.savedMask == 0x9 && it.writeStatus == ErrorsWriteStatus.Idle }
        fixture.close()
    }

    @Test
    fun disconnectDuringSavedReadIgnoresOldReplyAndUsesFreshConnection() = runBlocking {
        val fixture = Fixture(this, autoReadSaved = false)
        fixture.startConnected()
        fixture.awaitCommands(listOf("active"))
        fixture.session.readSavedErrors()
        fixture.awaitCommands(listOf("active", "saved"))
        fixture.emitState(ConnectionAttempt(1))
        fixture.awaitSubscriptions(0)
        fixture.packets.emit(CheckEngineSavedErrorsPacket(errors = 0x2))
        assertNull(fixture.session.snapshot.value.savedMask)

        fixture.emitState(Connected)
        fixture.awaitCommands(listOf("active", "saved", "active"))
        fixture.session.readSavedErrors()
        fixture.awaitCommands(listOf("active", "saved", "active", "saved"))
        fixture.packets.emit(CheckEngineSavedErrorsPacket(errors = 0x8))
        fixture.awaitSnapshot { it.savedMask == 0x8 }
        fixture.close()
    }

    @Test
    fun heldLiveErrorsRemainVisibleForFiveHundredMilliseconds() = runBlocking {
        val fixture = Fixture(this, autoReadSaved = false)
        fixture.startConnected()
        fixture.awaitCommands(listOf("active"))
        fixture.session.setHoldActiveErrors(true)
        fixture.session.setHoldActiveErrors(false)
        assertNull(fixture.session.snapshot.value.activeMask)
        fixture.session.setHoldActiveErrors(true)
        fixture.packets.emit(CheckEngineErrorsPacket(errors = 0x1))
        fixture.awaitSnapshot { it.activeMask == 0x1 }
        fixture.packets.emit(CheckEngineErrorsPacket(errors = 0))
        delay(250)
        assertEquals(0x1, fixture.session.snapshot.value.activeMask)
        fixture.session.setHoldActiveErrors(false)
        assertEquals(0, fixture.session.snapshot.value.activeMask)
        fixture.awaitSnapshot { it.activeMask == 0 }
        fixture.close()
    }

    @Test
    fun disconnectAndRoutePauseClearMasksDraftsAndCancelPendingWrites() = runBlocking {
        val fixture = Fixture(this)
        fixture.startConnected()
        fixture.awaitCommands(listOf("active", "saved"))
        fixture.packets.emit(CheckEngineSavedErrorsPacket(errors = 3))
        fixture.packets.emit(CheckEngineErrorsPacket(errors = 4))
        fixture.awaitSnapshot { it.savedMask == 3 && it.activeMask == 4 }
        fixture.session.beginEditing()
        fixture.session.setDraftError(0, false)
        fixture.session.writeDraft()
        fixture.awaitCommands(listOf("active", "saved", "write:2"))
        fixture.emitState(ConnectionAttempt(1))
        fixture.awaitSubscriptions(0)
        fixture.packets.emit(OpCompNc(opData = 0, opCode = 2))
        assertEquals(ErrorsSnapshot(autoReadSavedErrors = true), fixture.session.snapshot.value)
        assertEquals(1, fixture.commands.count { it.startsWith("write:") })

        fixture.emitState(Connected)
        fixture.session.onRoutePaused()
        assertNull(fixture.session.snapshot.value.savedMask)
        fixture.close()
    }

    private class Fixture(
        private val scope: CoroutineScope,
        autoReadSaved: Boolean = true,
        ackTimeoutMillis: Long = 1_000,
        readbackTimeoutMillis: Long = 1_000,
        initialReadTimeoutMillis: Long = 1_000,
    ) {
        val connectionStates = MutableSharedFlow<org.secu3.android.connection.ConnectionState>(
            replay = 1,
            extraBufferCapacity = 32,
        ).also { it.tryEmit(Disconnected) }
        val packets = MutableSharedFlow<InputPacket>()
        val commands = mutableListOf<String>()
        val autoRead = MutableStateFlow(autoReadSaved)
        var onSavedRead: (() -> Unit)? = null
        val session = ErrorsSession(
            connectionStates = connectionStates,
            ecuExchange = EcuExchange { onSubscribed -> packets.onSubscription { onSubscribed() } },
            packets = { onSubscribed -> packets.onSubscription { onSubscribed() } },
            savedReadSettings = autoRead,
            sendSavedRead = { commands += "saved"; onSavedRead?.invoke() },
            sendActiveRead = { commands += "active" },
            writeSavedErrors = { mask -> commands += "write:$mask" },
            ackTimeoutMillis = ackTimeoutMillis,
            readbackTimeoutMillis = readbackTimeoutMillis,
            initialReadTimeoutMillis = initialReadTimeoutMillis,
        )

        init { session.start(scope) }

        fun emitState(state: org.secu3.android.connection.ConnectionState) {
            connectionStates.tryEmit(state)
        }

        fun startConnected() {
            session.onRouteResumed()
            emitState(Connected)
        }

        suspend fun awaitSubscriptions(expected: Int) {
            withTimeout(1_000) { packets.subscriptionCount.first { it == expected } }
        }

        suspend fun awaitSnapshot(predicate: (ErrorsSnapshot) -> Boolean) {
            withTimeout(1_000) { session.snapshot.first(predicate) }
        }

        suspend fun awaitCommands(expected: List<String>) {
            withTimeout(1_000) {
                while (commands != expected) kotlinx.coroutines.yield()
            }
        }

        fun close() = session.close()
    }
}
