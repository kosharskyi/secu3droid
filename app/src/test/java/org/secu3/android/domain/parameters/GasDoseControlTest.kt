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
import org.secu3.android.models.packets.out.params.GasDoseParamPacket

class GasDoseControlTest {
    private val fixtures = mutableListOf<Fixture>()

    @After
    fun cleanup() = fixtures.forEach(Fixture::close)

    @Test
    fun editsClampFiniteValuesAndSendSnapshotsForEveryValidAction() {
        val f = fixture()
        f.control.receive(GasDoseParamPacket())

        assertTrue(f.control.editSteps(4_000f))
        assertTrue(f.control.editStoichiometricRatio(Float.NEGATIVE_INFINITY).not())
        assertTrue(f.control.editStoichiometricRatio(24f))
        assertTrue(f.control.editFuelCutClosing(-1f))
        assertTrue(f.control.editCorrectionPositive(.1f))
        assertTrue(f.control.editCorrectionNegative(150f))
        assertFalse(f.control.selectFrequency(4))
        assertTrue(f.control.selectFrequency(2))
        assertTrue(f.control.setMaximumFrequency(true))
        assertTrue(f.control.toggleTest())

        val values = requireNotNull(f.control.values.value)
        assertEquals(2_000f, values.steps)
        assertEquals(22f, values.stoichiometricRatio)
        assertEquals(0f, values.fuelCutClosing)
        assertEquals(.5f, values.correctionPositive)
        assertEquals(100f, values.correctionNegative)
        assertEquals(2, values.frequency)
        assertTrue(values.maxFrequencyAtInit)
        assertTrue(values.testing)
        assertEquals(8, f.sent.size)
        f.sent.forEach { assertEquals(0, it.pack()[4]) }
    }

    @Test
    fun firmwareLossDropsPendingMovementAndBlocksCommandsUntilSupportReturns() {
        val f = fixture()
        val packet = GasDoseParamPacket(steps = 100)
        f.control.receive(packet)
        f.control.manualStep(1)
        f.firmware.value = FirmwareInfoPacket(options = 0)

        assertFalse(f.control.editSteps(500f))
        assertFalse(f.control.selectFrequency(3))
        assertFalse(f.control.setMaximumFrequency(true))
        assertFalse(f.control.toggleTest())
        assertTrue(f.sent.isEmpty())

        f.firmware.value = FirmwareInfoPacket(options = 1 shl 29)
        assertFalse(f.control.finishManual())
        assertTrue(f.control.editSteps(101f))
        assertEquals(101, packet.steps)
        f.control.manualStep(-1)
        assertTrue(f.control.finishManual())
        assertEquals(255, f.sent.last().pack()[4])
    }

    @Test
    fun firmwareLossClearsPendingMovementEvenIfSupportReturnsBeforeFlush() {
        val f = fixture()
        f.control.receive(GasDoseParamPacket(steps = 100))
        f.control.manualStep(1)

        f.firmware.value = FirmwareInfoPacket(options = 0)
        f.firmware.value = FirmwareInfoPacket(options = 1 shl 29)

        assertFalse(f.control.finishManual())
        assertTrue(f.sent.isEmpty())
    }

    @Test
    fun manualFlushConsumesBoundedDeltaAndQueuedSnapshotsDoNotChange() {
        val f = fixture()
        f.control.receive(GasDoseParamPacket(steps = 123, manualPositionD = 9))
        repeat(200) { f.control.manualStep(1) }
        assertTrue(f.control.flushManual())
        val positive = f.sent.single()
        assertEquals(127, positive.pack()[4])

        repeat(200) { f.control.manualStep(-1) }
        assertTrue(f.control.finishManual())
        assertEquals(129, f.sent.last().pack()[4])
        assertEquals(127, positive.pack()[4])
        f.control.cancelManual()
        assertFalse(f.control.finishManual())
        assertTrue(f.control.editSteps(400f))
        assertEquals(0, f.sent.last().pack()[4])
    }

    @Test
    fun receiveAndResetClearPendingMovementAndPreserveUnknownTail() {
        val f = fixture()
        val rawPacket = GasDoseParamPacket(steps = 100, manualPositionD = 40).pack()
        val packet = GasDoseParamPacket().parse(intArrayOf('@'.code, '*'.code) + rawPacket.drop(1).toIntArray() + intArrayOf(0xD1, 0xE2)) as GasDoseParamPacket
        f.control.receive(packet)
        assertEquals(0, packet.manualPositionD)
        assertEquals(0xD1, packet.pack()[14])
        f.control.manualStep(1)
        f.control.receive(GasDoseParamPacket(steps = 200))
        assertFalse(f.control.finishManual())
        assertTrue(f.sent.isEmpty())

        f.control.manualStep(-1)
        f.control.reset()
        assertNull(f.control.values.value)
        assertFalse(f.control.finishManual())
        assertFalse(f.control.editSteps(300f))
    }

    @Test
    fun reportsUnderlyingSendAdmissionBoolean() {
        val f = fixture()
        f.control.receive(GasDoseParamPacket(steps = 22))
        f.admitted = false

        assertFalse(f.control.editSteps(23f))
        assertEquals(1, f.sent.size)
        assertEquals(0, f.sent.single().pack()[4])

        f.control.manualStep(1)
        assertFalse(f.control.flushManual())
        assertEquals(2, f.sent.size)
        assertEquals(1, f.sent.last().pack()[4])
        f.admitted = true
        assertFalse(f.control.finishManual())
        assertEquals(2, f.sent.size)
    }

    private fun fixture(): Fixture = Fixture().also(fixtures::add)

    private class Fixture {
        val firmware = MutableStateFlow<FirmwareInfoPacket?>(FirmwareInfoPacket(options = 1 shl 29))
        val sent = mutableListOf<OutputPacket>()
        var admitted = true
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val control = GasDoseControl(firmware) { packet ->
            sent += packet
            admitted
        }

        init {
            control.start(scope)
        }

        fun close() = scope.cancel()
    }
}
