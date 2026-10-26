package org.secu3.android.domain.parameters

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.secu3.android.models.packets.base.OutputPacket
import org.secu3.android.models.packets.input.FirmwareInfoPacket
import org.secu3.android.models.packets.out.params.ChokeControlParPacket
import org.secu3.android.models.packets.out.params.GasDoseParamPacket

class ChokeControlTest {
    private val fixtures = mutableListOf<Fixture>()

    @After fun cleanup() = fixtures.forEach(Fixture::close)

    @Test fun resetAndReceiveClearValuesAndPendingMovement() {
        val f = fixture()
        val first = ChokeControlParPacket(smSteps = 777, manualPositionD = 23)
        f.control.receive(first)
        assertEquals(0, first.manualPositionD)
        f.control.manualStep(1)
        f.control.reset()
        assertNull(f.control.values.value)
        assertFalse(f.control.editSteps(900f))
        assertFalse(f.control.toggleTest())
        assertFalse(f.control.finishManual())
        assertTrue(f.sent.isEmpty())

        f.control.receive(ChokeControlParPacket(smSteps = 600))
        assertTrue(f.control.editSteps(900f))
        assertEquals(1, f.sent.size)
    }

    @Test fun manualMotionClampsFlushesOnceAndCancelDropsOnlyUnsentMovement() {
        val f = fixture()
        val packet = ChokeControlParPacket()
        f.control.receive(packet)
        repeat(150) { f.control.manualStep(1) }
        assertTrue(f.control.flushManual())
        repeat(3) { f.control.manualStep(-1) }
        assertTrue(f.control.flushManual())
        f.control.manualStep(1)
        f.control.cancelManual()
        assertFalse(f.control.finishManual())
        assertEquals(listOf(127, 253), f.sent.map { it.pack()[4] })
        assertEquals(0, packet.manualPositionD)

        repeat(150) { f.control.manualStep(-1) }
        assertTrue(f.control.finishManual())
        assertEquals(129, f.sent.last().pack()[4])
        assertFalse(f.control.finishManual())
    }

    @Test fun ordinaryEditsPreservePendingMovementAndSendIndependentZeroDeltaSnapshots() {
        val f = fixture()
        val packet = ChokeControlParPacket(smSteps = 600, flags = 0b10100000, manualPositionD = 9)
        f.control.receive(packet)
        f.control.manualStep(1)
        assertTrue(f.control.editSteps(900f))
        assertEquals(0, f.sent.single().pack()[4])
        assertEquals(0, packet.manualPositionD)
        assertTrue(f.control.finishManual())
        assertEquals(1, f.sent[1].pack()[4])
        assertEquals(900, f.sent[0].pack()[1] * 256 + f.sent[0].pack()[2])

        assertTrue(f.control.editRegulatorFactor(.75f))
        assertTrue(f.control.setClosedLoopRegulator(true))
        assertTrue(f.control.selectFrequency(3))
        assertTrue(f.control.toggleTest())
        assertFalse(f.control.selectFrequency(-1))
        assertFalse(f.control.selectFrequency(4))
        assertEquals(0b10100001, packet.flags)
        assertEquals(3, packet.smFreq)
        assertTrue(packet.testing)
        assertEquals(.75f, requireNotNull(f.control.values.value).regulatorFactor)
        assertEquals(0, f.sent[2].pack()[4])
        assertEquals(0, f.sent[3].pack()[4])
    }

    @Test fun editsClampFiniteValuesAndPreserveUnknownFrequencyForDisplay() {
        val f = fixture()
        f.control.receive(ChokeControlParPacket(smSteps = 600, smFreq = 8))
        assertFalse(f.control.editSteps(Float.NaN))
        assertTrue(f.control.editSteps(5f))
        assertEquals(50, requireNotNull(f.control.values.value).steps)
        assertEquals(8, requireNotNull(f.control.values.value).frequency)
        assertTrue(f.control.editRegulatorFactor(5f))
        assertTrue(f.control.editColdDuration(0f))
        assertTrue(f.control.editHotDuration(12f))
        assertTrue(f.control.editCrankToRunDuration(120f))
        val values = requireNotNull(f.control.values.value)
        assertEquals(1f, values.regulatorFactor)
        assertEquals(.1f, values.coldDuration)
        assertEquals(10f, values.hotDuration)
        assertEquals(99f, values.crankToRunDuration)
    }

    @Test fun firmwareLossClearsPendingMovementAndRejectsEveryChokeCommand() {
        val f = fixture()
        val packet = ChokeControlParPacket()
        f.control.receive(packet)
        f.control.manualStep(1)
        f.firmware.value = FirmwareInfoPacket(options = 1 shl 28) // Fuel injection but no Choke support.
        assertFalse(f.control.editSteps(700f))
        assertFalse(f.control.editRegulatorFactor(.5f))
        assertFalse(f.control.selectFrequency(2))
        assertFalse(f.control.setThrottlePositionAtInit(true))
        assertFalse(f.control.toggleTest())
        f.control.manualStep(1)
        assertFalse(f.control.finishManual())
        assertTrue(f.sent.isEmpty())

        f.firmware.value = FirmwareInfoPacket(options = (1 shl 22) or (1 shl 28))
        assertFalse(f.control.finishManual())
        assertTrue(f.control.editSteps(701f))
        assertEquals(701, packet.smSteps)
    }

    @Test fun carburetorSpecificControlsRemainGated() {
        val f = fixture(options = (1 shl 22) or (1 shl 28))
        f.control.receive(ChokeControlParPacket())
        assertFalse(f.control.editRegulatorFactor(.5f))
        assertFalse(f.control.setClosedLoopRegulator(true))
        assertTrue(f.control.editSteps(600f))
        assertTrue(f.control.selectFrequency(2))
    }

    @Test fun chokeAndGasDoseKeepIndependentPendingManualMovement() {
        val firmware = MutableStateFlow<FirmwareInfoPacket?>(null)
        val chokeSent = mutableListOf<OutputPacket>()
        val gasDoseSent = mutableListOf<OutputPacket>()
        val choke = ChokeControl(firmware, { chokeSent += it; true }, ManualPositionDelta())
        val gasDose = GasDoseControl(firmware, { gasDoseSent += it; true }, ManualPositionDelta())
        choke.receive(ChokeControlParPacket())
        gasDose.receive(GasDoseParamPacket())

        choke.manualStep(1)
        choke.manualStep(1)
        gasDose.manualStep(-1)
        choke.cancelManual()
        assertTrue(gasDose.finishManual())
        assertEquals(0, chokeSent.size)
        assertEquals(255, gasDoseSent.single().pack()[4])

        choke.manualStep(-1)
        gasDose.manualStep(1)
        assertTrue(choke.finishManual())
        gasDose.cancelManual()
        assertEquals(1, chokeSent.size)
        assertEquals(255, chokeSent.single().pack()[4])
        assertEquals(1, gasDoseSent.size)
    }

    private fun fixture(options: Int = 0): Fixture = Fixture(options).also(fixtures::add)

    private class Fixture(options: Int) {
        val firmware = MutableStateFlow<FirmwareInfoPacket?>(if (options == 0) null else FirmwareInfoPacket(options = options))
        val sent = mutableListOf<OutputPacket>()
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val control = ChokeControl(firmware, { sent += it; true }, ManualPositionDelta()).also { it.start(scope) }
        fun close() = scope.cancel()
    }
}
