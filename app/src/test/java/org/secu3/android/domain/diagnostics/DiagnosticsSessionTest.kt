package org.secu3.android.domain.diagnostics

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.onSubscription
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import org.secu3.android.connection.Connected
import org.secu3.android.connection.ConnectionState
import org.secu3.android.connection.Disconnected
import org.secu3.android.models.packets.base.InputPacket
import org.secu3.android.models.packets.input.DiagInputPacket
import org.secu3.android.models.packets.out.DiagOutputPacket
import org.secu3.android.models.packets.out.OpCompNc
import org.secu3.android.utils.Task

class DiagnosticsSessionTest {
    @Test
    fun startSubscribesBeforeEnterAndRequestsInputOnceForRepeatedAck() = runBlocking {
        val fixture = Fixture(initiallyConnected = true, acknowledgeEnterImmediately = true)
        fixture.session.start(fixture.scope)
        fixture.await { fixture.commands.contains(Task.Secu3DiagInput) }
        assertEquals(listOf(Task.Secu3OpComEnterDiagnostics, Task.Secu3DiagInput), fixture.commands)
        fixture.emit(OpCompNc().apply { opCode = 6 })
        fixture.emit(OpCompNc().apply { opCode = 6 })
        assertEquals(1, fixture.commands.count { it == Task.Secu3DiagInput })
        assertFalse(fixture.session.state.value.controlsEnabled)
        fixture.close()
    }

    @Test
    fun inputInitializesSafeBitsAndLossClearsStateRetainingGeneratorSettings() = runBlocking {
        val fixture = Fixture(initiallyConnected = true)
        fixture.session.start(fixture.scope)
        fixture.await { fixture.commands.contains(Task.Secu3OpComEnterDiagnostics) }
        fixture.emit(DiagInputPacket(flags = 1))
        fixture.await { fixture.session.state.value.controlsEnabled && fixture.outputSnapshots.size == 1 }
        assertEquals(0b11, fixture.session.state.value.outputBits)
        fixture.session.selectChannel(3)
        assertEquals(DiagnosticOutput.IGN_OUT3, fixture.session.state.value.generatorOutput)
        fixture.session.editFrequency(321f)
        fixture.session.editDuty(37.5f)
        fixture.emit(OpCompNc().apply { opCode = 7 })
        assertFalse(fixture.session.state.value.controlsEnabled)
        fixture.emit(DiagInputPacket(flags = 1))
        fixture.await { fixture.session.state.value.controlsEnabled }
        fixture.session.close()

        val reconnect = Fixture(initiallyConnected = true)
        reconnect.session.start(reconnect.scope)
        reconnect.await { reconnect.commands.contains(Task.Secu3OpComEnterDiagnostics) }
        reconnect.emit(DiagInputPacket(flags = 1))
        reconnect.await { reconnect.session.state.value.controlsEnabled }
        reconnect.session.selectChannel(3)
        assertEquals(DiagnosticOutput.IGN_OUT3, reconnect.session.state.value.generatorOutput)
        reconnect.session.editFrequency(321f)
        reconnect.session.editDuty(37.5f)
        reconnect.connection.emit(Disconnected)
        reconnect.transportConnected = false
        reconnect.await { reconnect.session.state.value.input == null }
        assertFalse(reconnect.session.state.value.controlsEnabled)
        assertEquals(0, reconnect.session.state.value.channel)
        assertEquals(0b11, reconnect.session.state.value.outputBits)
        assertEquals(321f, reconnect.session.state.value.frequency)
        assertEquals(37.5f, reconnect.session.state.value.duty)
        reconnect.transportConnected = true
        reconnect.connection.emit(Connected)
        reconnect.await { reconnect.commands.count { it == Task.Secu3OpComEnterDiagnostics } == 2 }
        assertFalse(reconnect.session.state.value.controlsEnabled)
        reconnect.emit(OpCompNc().apply { opCode = 6 })
        reconnect.await { reconnect.commands.count { it == Task.Secu3DiagInput } == 1 }
        reconnect.emit(DiagInputPacket(flags = 1))
        reconnect.await { reconnect.session.state.value.controlsEnabled }
        assertEquals(321f, reconnect.session.state.value.frequency)
        assertEquals(37.5f, reconnect.session.state.value.duty)
        reconnect.close()
    }

    @Test
    fun optionsResetEachGroupInOrderAndOutputCommandsRespectGeneratorAndDirection() = runBlocking {
        val fixture = Fixture(initiallyConnected = true)
        fixture.session.start(fixture.scope)
        fixture.await { fixture.commands.contains(Task.Secu3OpComEnterDiagnostics) }
        fixture.emit(DiagInputPacket(flags = 0))
        fixture.await { fixture.session.state.value.controlsEnabled }
        fixture.session.selectChannel(1)
        fixture.session.setOutput(DiagnosticOutput.IGN_OUT1, true)
        assertEquals(0, fixture.session.state.value.outputBits and 1)
        val disabledDirectionSendCount = fixture.outputSnapshots.size
        fixture.session.setOutput(DiagnosticOutput.BL, true)
        fixture.session.setOutput(DiagnosticOutput.DE, true)
        fixture.session.setOutput(DiagnosticOutput.TACH_O, true)
        assertEquals(0, fixture.session.state.value.outputBits and ((1 shl 11) or (1 shl 13) or (1 shl 23)))
        assertEquals(disabledDirectionSendCount, fixture.outputSnapshots.size)
        fixture.session.applyAdditionalOptions(enableBlDe = true, enableTachO = true)
        val lowResetSnapshots = fixture.outputSnapshots.takeLast(2).map { it.out }
        assertEquals(listOf((1 shl 12) or (1 shl 14), (1 shl 12) or (1 shl 14) or (1 shl 24)), lowResetSnapshots)
        fixture.session.setOutput(DiagnosticOutput.BL, true)
        fixture.session.setOutput(DiagnosticOutput.DE, true)
        fixture.session.setOutput(DiagnosticOutput.TACH_O, true)
        val drivenHigh = fixture.session.state.value.outputBits
        val unchangedCount = fixture.outputSnapshots.size
        fixture.session.applyAdditionalOptions(enableBlDe = true, enableTachO = true)
        assertEquals(drivenHigh, fixture.session.state.value.outputBits)
        assertEquals(unchangedCount, fixture.outputSnapshots.size)
        val before = fixture.outputSnapshots.size
        fixture.session.applyAdditionalOptions(enableBlDe = false, enableTachO = false)
        assertEquals(listOf(1 shl 23 or (1 shl 24), 0), fixture.outputSnapshots.drop(before).map { it.out })
        fixture.session.close()
    }

    @Test
    fun repeatedSameChannelAndGeneratorValuesStillQueueCommands() = runBlocking {
        val fixture = Fixture(initiallyConnected = true)
        fixture.session.start(fixture.scope)
        fixture.await { fixture.commands.contains(Task.Secu3OpComEnterDiagnostics) }
        fixture.emit(DiagInputPacket(flags = 0))
        fixture.await { fixture.session.state.value.controlsEnabled }
        val initialCount = fixture.outputSnapshots.size
        fixture.session.selectChannel(1)
        fixture.session.selectChannel(1)
        fixture.session.editFrequency(10f)
        fixture.session.editDuty(50f)
        assertEquals(initialCount + 4, fixture.outputSnapshots.size)
        fixture.close()
    }

    @Test
    fun outputLimitsAndChannelIndicesAreValidatedByTheSession() = runBlocking {
        val fixture = Fixture(initiallyConnected = true)
        fixture.session.start(fixture.scope)
        fixture.await { fixture.commands.contains(Task.Secu3OpComEnterDiagnostics) }
        fixture.emit(DiagInputPacket(flags = 0))
        fixture.await { fixture.session.state.value.controlsEnabled }
        fixture.session.selectChannel(14)
        assertEquals(14, fixture.session.state.value.channel)
        val sendCount = fixture.outputSnapshots.size
        fixture.session.selectChannel(24)
        fixture.session.editFrequency(Float.NaN)
        fixture.session.editFrequency(9.9f)
        fixture.session.editFrequency(5000.1f)
        fixture.session.editDuty(Float.POSITIVE_INFINITY)
        fixture.session.editDuty(-0.1f)
        fixture.session.editDuty(100.1f)
        assertEquals(14, fixture.session.state.value.channel)
        assertEquals(10f, fixture.session.state.value.frequency)
        assertEquals(50f, fixture.session.state.value.duty)
        assertEquals(sendCount, fixture.outputSnapshots.size)
        fixture.session.close()
    }

    @Test
    fun familyCatalogsKeepFullProtocolOrderIncludingHiddenTachOutput() = runBlocking {
        assertEquals(13, DiagnosticOutput.secu3TChannels.size)
        assertEquals(23, DiagnosticOutput.secu3IChannels.size)
        assertEquals(DiagnosticOutput.TACH_O, DiagnosticOutput.secu3IChannels[21])
        assertEquals(DiagnosticOutput.GPA6_O, DiagnosticOutput.secu3IChannels[22])
        val fixture = Fixture(initiallyConnected = true)
        fixture.session.start(fixture.scope)
        fixture.await { fixture.commands.contains(Task.Secu3OpComEnterDiagnostics) }
        fixture.emit(DiagInputPacket(flags = 0))
        fixture.await { fixture.session.state.value.controlsEnabled }
        fixture.session.selectChannel(22)
        assertEquals(DiagnosticOutput.TACH_O, fixture.session.state.value.generatorOutput)
        fixture.close()
    }

    @Test
    fun everyOutputUsesItsExpectedWireMask() = runBlocking {
        val fixture = Fixture(initiallyConnected = true)
        fixture.session.start(fixture.scope)
        fixture.await { fixture.commands.contains(Task.Secu3OpComEnterDiagnostics) }
        fixture.emit(DiagInputPacket(flags = 0))
        fixture.await { fixture.session.state.value.controlsEnabled }
        fixture.session.applyAdditionalOptions(enableBlDe = true, enableTachO = true)
        val directionMask = (1 shl 12) or (1 shl 14) or (1 shl 24)
        val expectedOn = mapOf(
            DiagnosticOutput.IGN_OUT1 to (1 shl 0), DiagnosticOutput.IGN_OUT2 to (1 shl 1), DiagnosticOutput.IGN_OUT3 to (1 shl 2), DiagnosticOutput.IGN_OUT4 to (1 shl 3),
            DiagnosticOutput.IE to (1 shl 4), DiagnosticOutput.FE to (1 shl 5), DiagnosticOutput.ECF to (1 shl 6), DiagnosticOutput.CE to (1 shl 7), DiagnosticOutput.ST_BLOCK to (1 shl 8),
            DiagnosticOutput.ADD_IO1 to (1 shl 9), DiagnosticOutput.ADD_IO2 to (1 shl 10), DiagnosticOutput.IGN_OUT5 to (1 shl 4), DiagnosticOutput.ECF0 to (1 shl 5),
            DiagnosticOutput.INJ_O1 to (1 shl 6), DiagnosticOutput.INJ_O2 to (1 shl 7), DiagnosticOutput.INJ_O3 to (1 shl 8), DiagnosticOutput.INJ_O4 to (1 shl 9),
            DiagnosticOutput.INJ_O5 to (1 shl 10), DiagnosticOutput.BL to ((1 shl 11) or (1 shl 12)), DiagnosticOutput.DE to ((1 shl 13) or (1 shl 14)),
            DiagnosticOutput.STBL_O to (1 shl 15), DiagnosticOutput.CEL_O to (1 shl 16), DiagnosticOutput.FPMP_O to (1 shl 17), DiagnosticOutput.PWRR_O to (1 shl 18),
            DiagnosticOutput.EVAP_O to (1 shl 19), DiagnosticOutput.O2SH_O to (1 shl 20), DiagnosticOutput.COND_O to (1 shl 21), DiagnosticOutput.ADD_O2 to (1 shl 22),
            DiagnosticOutput.TACH_O to ((1 shl 23) or (1 shl 24)), DiagnosticOutput.GPA6_O to (1 shl 25),
        )
        DiagnosticOutput.entries.forEach { output ->
            fixture.session.setOutput(output, true)
            assertEquals(output.name, directionMask or expectedOn.getValue(output), fixture.session.state.value.outputBits)
            fixture.session.setOutput(output, false)
            assertEquals(output.name, directionMask, fixture.session.state.value.outputBits)
        }
        fixture.close()
    }

    @Test
    fun knownFirmwareInitializesSafeMaskImmediatelyAndRepeatedLossRetainsIt() = runBlocking {
        val fixture = Fixture(initiallyConnected = true, initialFirmwareFamily = true)
        fixture.session.start(fixture.scope)
        assertEquals(0b11, fixture.session.state.value.outputBits)
        fixture.await { fixture.commands.contains(Task.Secu3OpComEnterDiagnostics) }
        fixture.emit(DiagInputPacket(flags = 1))
        fixture.await { fixture.session.state.value.controlsEnabled }
        fixture.connection.emit(Disconnected)
        fixture.transportConnected = false
        fixture.await { fixture.session.state.value.input == null }
        fixture.connection.emit(Disconnected)
        assertEquals(0b11, fixture.session.state.value.outputBits)
        assertEquals(10f, fixture.session.state.value.frequency)
        assertEquals(50f, fixture.session.state.value.duty)
        fixture.close()

        val secu3i = Fixture(initialFirmwareFamily = false)
        secu3i.session.start(secu3i.scope)
        assertEquals(0, secu3i.session.state.value.outputBits)
        secu3i.close()
    }

    @Test
    fun closeLeavesOnlyAnEnteredLiveSession() = runBlocking {
        val offline = Fixture(initiallyConnected = false)
        offline.session.start(offline.scope)
        offline.session.close()
        offline.connection.emit(Connected)
        assertEquals(emptyList<Task>(), offline.commands)

        val live = Fixture(initiallyConnected = true)
        live.session.start(live.scope)
        live.await { live.commands.contains(Task.Secu3OpComEnterDiagnostics) }
        live.session.close()
        assertEquals(
            listOf(Task.Secu3OpComEnterDiagnostics, Task.Secu3OpComLeaveDiagnostics, Task.Secu3ReadSensors),
            live.commands,
        )
        assertNull(live.session.state.value.input)
        live.finish()
        offline.finish()
    }

    private class Fixture(
        initiallyConnected: Boolean = false,
        private val initialFirmwareFamily: Boolean? = null,
        private val acknowledgeEnterImmediately: Boolean = false,
    ) {
        val connection = MutableSharedFlow<ConnectionState>(replay = 1, extraBufferCapacity = 32).also {
            it.tryEmit(if (initiallyConnected) Connected else Disconnected)
        }
        val packets = MutableSharedFlow<InputPacket>(extraBufferCapacity = 8)
        val commands = mutableListOf<Task>()
        val outputSnapshots = mutableListOf<OutputSnapshot>()
        var transportConnected = initiallyConnected
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val session = DiagnosticsSession(
            connectionStates = connection,
            packets = { subscribed -> packets.onSubscription { subscribed() } },
            isTransportConnected = { transportConnected },
            initialFirmwareFamily = { initialFirmwareFamily },
            sendTask = { task ->
                commands += task
                if (acknowledgeEnterImmediately && task == Task.Secu3OpComEnterDiagnostics) {
                    packets.tryEmit(OpCompNc(opCode = 6))
                }
            },
            sendPacket = { packet -> outputSnapshots += OutputSnapshot(packet.out, packet.chan, packet.frq, packet.duty) },
        )

        suspend fun emit(packet: InputPacket) {
            packets.emit(packet)
            delay(1)
        }

        suspend fun await(condition: () -> Boolean) = withTimeout(2_000) {
            while (!condition()) delay(1)
        }

        fun close() {
            session.close()
            scope.cancel()
        }

        fun finish() = scope.cancel()
    }

    private data class OutputSnapshot(val out: Int, val channel: Int, val frequency: Float, val duty: Float)
}
