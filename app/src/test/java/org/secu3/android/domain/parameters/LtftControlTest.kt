package org.secu3.android.domain.parameters

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.secu3.android.models.packets.base.OutputPacket
import org.secu3.android.models.packets.input.FirmwareInfoPacket
import org.secu3.android.models.packets.out.params.LtftParamPacket

class LtftControlTest {
    @Test fun receiveIsPassiveAndAllFourModesAndOffModeEditsAreAccepted() {
        val sent = mutableListOf<IntArray>()
        val control = LtftControl({ null }, { packet -> sent += packet.pack(); true })
        val packet = LtftParamPacket(mode = 99, learnClt = -12.25f, min = -25f, max = -1f)
        control.receive(packet)
        assertEquals(99, control.values.value?.mode)
        assertEquals(-12.25f, control.values.value?.lowerClt)
        assertEquals(-25f, control.values.value?.bottomLimit)
        assertTrue(sent.isEmpty())
        assertTrue(control.selectMode(0))
        assertTrue(control.editLowerClt(90.25f))
        for (mode in 1..3) assertTrue(control.selectMode(mode))
        assertEquals(5, sent.size)
        assertEquals(90.25f, packet.learnClt)
        assertEquals(-25f, packet.min)
    }

    @Test fun mapsAllFieldsAndAcceptsUnsnappedIntsWithoutPairCorrection() {
        val packet = LtftParamPacket(mode = 2, learnClt = 12.5f, learnCltUp = 90f, learnIatUp = 70f,
            learnGrad = .25f, learnGpa = 100f, learnGpd = 12f, min = -10f, max = 15f,
            learnRpm0 = 800, learnRpm1 = 5000, learnLoad0 = 20f, learnLoad1 = 300f,
            deadBand0 = 1.2f, deadBand1 = 3.4f)
        val control = LtftControl({ null }, { true })
        control.receive(packet)
        val cases = listOf<() -> Boolean>(
            { control.editLowerClt(100.25f) }, { control.editUpperClt(10.75f) },
            { control.editUpperIat(80.25f) }, { control.editLearningGradient(.123f) },
            { control.editAbsoluteGasPressure(123.45f) }, { control.editDifferentialGasPressure(20.25f) },
            { control.editBottomLimit(-20.25f) }, { control.editTopLimit(22.25f) },
            { control.editLowerRpm(1234) }, { control.editUpperRpm(4321) },
            { control.editLowerLoad(321.25f) }, { control.editUpperLoad(12.25f) },
            { control.editNegativeInsensitivity(2.25f) }, { control.editPositiveInsensitivity(4.25f) },
        )
        cases.forEach { assertTrue(it()) }
        val values = control.values.value!!
        assertEquals(LtftValues(2, 100.25f, 10.75f, 80.25f, .123f, 123.45f, 20.25f,
            -20.25f, 22.25f, 1234, 4321, 321.25f, 12.25f, 2.25f, 4.25f), values)
        assertEquals(100.25f, values.lowerClt)
        assertEquals(10.75f, values.upperClt)
        assertEquals(.123f, values.learningGradient)
        assertEquals(1234, values.lowerRpm)
        assertEquals(4321, values.upperRpm)
        assertEquals(321.25f, values.lowerLoad)
        assertEquals(12.25f, values.upperLoad)
        assertEquals(2.25f, values.negativeInsensitivity)
        assertEquals(4.25f, values.positiveInsensitivity)
    }

    @Test fun rejectsInvalidExplicitInputsAndUnknownModeCodes() {
        val sent = mutableListOf<OutputPacket>()
        val control = LtftControl({ null }, { sent += it; true })
        control.receive(LtftParamPacket())
        assertFalse(control.editLowerClt(Float.NaN))
        assertFalse(control.editLearningGradient(1f))
        assertFalse(control.editBottomLimit(.1f))
        assertFalse(control.editTopLimit(24.61f))
        assertFalse(control.editLowerRpm(25001))
        assertFalse(control.editUpperLoad(-.1f))
        listOf(-1, 4, Int.MIN_VALUE, Int.MAX_VALUE).forEach { assertFalse(control.selectMode(it)) }
        assertTrue(sent.isEmpty())
    }

    @Test fun admissionUsesCurrentFiCapabilityAndNullFirmwareRemainsPermissive() {
        var firmware: FirmwareInfoPacket? = FirmwareInfoPacket(options = 1 shl 28)
        var sends = 0
        val control = LtftControl({ firmware }, { sends++; false })
        val packet = LtftParamPacket()
        control.receive(packet)
        assertFalse(control.editLowerClt(20f)) // sender refusal rolls back
        assertEquals(0f, packet.learnClt)
        firmware = FirmwareInfoPacket(options = 0)
        assertFalse(control.selectMode(0))
        assertFalse(control.editLowerRpm(500))
        assertEquals(1, sends)
        firmware = null
        assertFalse(control.editLowerClt(20f)) // send callback refusal
        assertEquals(0f, packet.learnClt)
        assertEquals(2, sends)
    }

    @Test fun refusedCommandRestoresEveryFieldAndPublishedState() {
        val control = LtftControl({ null }, { false })
        val packet = LtftParamPacket(mode = 88, learnClt = -2.5f, learnCltUp = 999f, learnIatUp = 1.125f,
            learnGrad = .777f, learnGpa = 600f, learnGpd = -4f, min = -25f, max = 99f,
            learnRpm0 = 65535, learnRpm1 = 12, learnLoad0 = -2f, learnLoad1 = 600f,
            deadBand0 = 255f, deadBand1 = -1f)
        control.receive(packet)
        val before = packet.pack()
        val stateBefore = control.values.value
        assertFalse(control.editLowerClt(10f))
        assertArrayEquals(before, packet.pack())
        assertEquals(stateBefore, control.values.value)
    }

    @Test fun packedCommandsAreImmutableAndKeepUnknownTail() {
        val outputs = mutableListOf<OutputPacket>()
        var reject = false
        val control = LtftControl({ null }, { output ->
            val snapshot = output.pack()
            outputs += output
            snapshot[0] = 0
            assertEquals('O'.code, output.pack()[0])
            !reject
        })
        val original = LtftParamPacket()
        val incoming = LtftParamPacket().parse(intArrayOf('@'.code) + original.pack() + intArrayOf(0xCA, 0xFE)) as LtftParamPacket
        control.receive(incoming)
        assertTrue(control.editUpperClt(44.5f))
        val first = outputs.single().pack()
        assertEquals('O'.code, first[0])
        first[0] = 0
        assertEquals('O'.code, outputs.single().pack()[0])
        assertEquals(listOf(0xCA, 0xFE), incoming.pack().takeLast(2))
        reject = true
        assertFalse(control.editUpperIat(50.25f))
        assertArrayEquals(outputs.first().pack(), first.also { it[0] = 'O'.code })
        assertEquals(44.5f, incoming.learnCltUp)
        assertEquals(0f, incoming.learnIatUp)
    }

    @Test fun unrelatedEditPreservesRawSignedBottomBytesAndExplicitHalfCountRoundsAwayFromZero() {
        val packed = LtftParamPacket().pack()
        packed[13] = 0x80 // signed lower-limit byte -128 -> -25 percent under the current codec.
        packed[14] = 0xFF // existing Android top-limit decoding remains unsigned.
        val packet = LtftParamPacket().parse(intArrayOf('@'.code) + packed + intArrayOf(0xCA, 0xFE)) as LtftParamPacket
        val control = LtftControl({ null }, { true })
        control.receive(packet)
        assertEquals(-25f, control.values.value?.bottomLimit)
        assertEquals(255f * (100f / 512f), control.values.value?.topLimit!!, .0001f)
        assertTrue(control.editUpperClt(12.25f))
        assertEquals(0x80, packet.pack()[13])
        assertEquals(0xFF, packet.pack()[14])
        assertEquals(listOf(0xCA, 0xFE), packet.pack().takeLast(2))

        val halfNegativeCount = LtftParamPacket(min = -.09765625f)
        assertEquals(0xFF, halfNegativeCount.pack()[13] and 0xff)
    }

    @Test fun resetDropsPacketAndValues() {
        val control = LtftControl({ null }, { true })
        control.receive(LtftParamPacket())
        control.reset()
        assertNull(control.values.value)
        assertFalse(control.editLowerRpm(1))
    }
}
