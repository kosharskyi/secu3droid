package org.secu3.android.domain.parameters

import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.*
import org.junit.Test
import org.secu3.android.models.packets.base.OutputPacket
import org.secu3.android.models.packets.input.FirmwareInfoPacket
import org.secu3.android.models.packets.out.params.InjctrParPacket

class FuelInjectionControlTest {
    private val sends = mutableListOf<OutputPacket>()
    private val firmware = MutableStateFlow<FirmwareInfoPacket?>(FirmwareInfoPacket(options = (1 shl 28)))
    private var admitSend = true
    private val control = FuelInjectionControl(firmware) { packet ->
        sends += packet
        admitSend
    }

    private fun packet() = InjctrParPacket(
        flags = 0x80, config = intArrayOf(0x11, 0x11), flowRate = floatArrayOf(300f, 400f),
        sdIglConst = intArrayOf(123, 456), ckpsEngineCyl = 4, timing = floatArrayOf(10f, 20f),
        timingCrk = floatArrayOf(30f, 40f), injMaxPw = floatArrayOf(10f, 11f),
        injPwDecSpeed = 1f, injPwIncSpeed = 2f,
    ).also { it.engineDisp = 2f }

    @Test fun receiveDoesNotEchoAndResetClearsOnlyOwnedValues() {
        control.changeAngleSpec(0, 1)
        assertTrue(sends.isEmpty())
        control.receive(packet())
        assertTrue(sends.isEmpty())
        assertNotNull(control.values.value)
        control.reset()
        assertNull(control.values.value)
        control.editFlow(0, 600f)
        assertTrue(sends.isEmpty())
    }

    @Test fun numericEditsUpdateBothBanksAndPublishRawPhysicalValues() {
        val p = packet()
        control.receive(p)
        val edits = listOf<() -> Boolean>(
            { control.editEngineDisplacement(3f) },
            { control.editFlow(0, 301f) }, { control.editFlow(1, 401f) },
            { control.editTiming(0, 11f) }, { control.editTiming(1, 21f) },
            { control.editCrankingTiming(0, 31f) }, { control.editCrankingTiming(1, 41f) },
            { control.editMinimumPulseWidth(0, .8f) }, { control.editMinimumPulseWidth(1, .9f) },
            { control.editMaximumPulseWidth(0, 12f) }, { control.editMaximumPulseWidth(1, 13f) },
            { control.editPulsesPerLiter(10000f) }, { control.editDecreaseRate(3f) }, { control.editIncreaseRate(4f) },
        )
        edits.forEach { assertTrue(it()) }
        assertEquals(edits.size, sends.size)
        assertEquals(3f, p.engineDisp, .0001f)
        assertArrayEquals(floatArrayOf(301f, 401f), p.flowRate, .001f)
        assertArrayEquals(floatArrayOf(11f, 21f), p.timing, 0f)
        assertArrayEquals(floatArrayOf(31f, 41f), p.timingCrk, 0f)
        assertEquals(10000f, p.fffConst, 0f)
        val values = control.values.value!!
        assertEquals(3f, values.engineDisplacement, .001f)
        assertEquals(401f, values.banks[1].flow, .001f)
        assertEquals(10000f, values.pulsesPerLiter, 0f)
    }

    @Test fun fractionalSignedTimingAndPhysicalPulsesSurviveUnrelatedEditsAndSnapshotsAreImmutable() {
        val source = packet().also {
            it.timing = floatArrayOf(1.5f, -1f)
            it.timingCrk = floatArrayOf(.0625f, 720f)
        }
        val incomingFrame = intArrayOf('@'.code) + source.pack() + intArrayOf(0xCA, 0xFE)
        incomingFrame[19] = source.ckpsEngineCyl
        val incoming = InjctrParPacket().parse(incomingFrame) as InjctrParPacket
        control.receive(incoming)
        assertEquals(1.5f, control.values.value!!.banks[0].timing, 0f)
        val timingWords = incoming.pack().sliceArray(19..26)
        assertTrue(control.setAirDensity(true))
        assertEquals(0x88, incoming.flags)
        val queuedSnapshot = sends.last()
        control.setUseMap(0, true)
        assertArrayEquals(timingWords, queuedSnapshot.pack().sliceArray(19..26))
        assertArrayEquals(timingWords, incoming.pack().sliceArray(19..26))
        assertArrayEquals(intArrayOf(0xCA, 0xFE), incoming.pack().takeLast(2).toIntArray())
        sends.clear()

        control.setUseMap(0, false)
        control.editTiming(0, 2f)
        control.editTiming(1, 3.25f)
        control.editCrankingTiming(0, 4.5f)
        control.editCrankingTiming(1, -.03125f)
        assertArrayEquals(intArrayOf(0x00, 0x20, 0x00, 0x34, 0x00, 0x48, 0xFF, 0xFF), incoming.pack().sliceArray(19..26))
    }

    @Test fun pulsesPerLiterRemainsPhysicalFloatAcrossUnrelatedEdit() {
        val payload = IntArray(53).also {
            it[27] = 0x22
            it[28] = 0x26
            it[51] = 0xCA
            it[52] = 0xFE
        }
        val received = InjctrParPacket().parse(intArrayOf('@'.code, ';'.code) + payload) as InjctrParPacket
        control.receive(received)
        assertEquals(8003.540039f, control.values.value!!.pulsesPerLiter, .0001f)
        assertTrue(control.setAirDensity(true))
        assertEquals(0x2226, (received.pack()[28] shl 8) or received.pack()[29])
        assertArrayEquals(intArrayOf(0xCA, 0xFE), received.pack().takeLast(2).toIntArray())
        assertTrue(control.editPulsesPerLiter(10000f))
        assertEquals(10923, (received.pack()[28] shl 8) or received.pack()[29])
    }

    @Test fun derivedConstantsRecalculateAndConfigRetainsOrReplacesPulseCountBeforeCalculation() {
        val p = packet()
        control.receive(p)
        val expected = packet().also {
            it.flowRate[0] = 600f
            it.flowRate[1] = 500f
            it.recalculateDerivedConstants()
        }
        control.editFlow(0, 600f)
        control.editFlow(1, 500f)
        assertArrayEquals(expected.sdIglConst, p.sdIglConst)
        assertArrayEquals(expected.injMafConst, p.injMafConst)
        assertEquals(expected.mafloadConst, p.mafloadConst)

        control.changeConfig(0, 2)
        assertEquals(2, p.config0Pulses)
        assertEquals(2, p.config0)
        control.changeConfig(1, 4)
        assertEquals(4, p.config1Pulses)
        assertEquals(4, p.config1)
        val sentCount = sends.size
        control.changeConfig(-1, 0)
        control.changeConfig(2, 0)
        control.changeConfig(0, 99)
        control.changePulses(0, 3)
        control.changeAngleSpec(0, 3)
        assertEquals(sentCount, sends.size)
    }

    @Test fun firmwareMapAndInputGuardsRejectCommandsAndSendAdmissionIsReturned() {
        val p = packet()
        control.receive(p)
        control.setUseMap(0, true)
        val count = sends.size
        assertFalse(control.editTiming(0, 12f))
        assertFalse(control.editTiming(2, 12f))
        assertFalse(control.editFlow(0, Float.NaN))
        assertFalse(control.changeAngleSpec(2, 1))
        assertEquals(count, sends.size)
        assertTrue(control.editTiming(1, -721f)) // Protocol values remain wider than the editor range.

        firmware.value = FirmwareInfoPacket(options = (1 shl 28) or (1 shl 17))
        val beforeRestricted = sends.size
        assertFalse(control.setAdditionalCorrections(true))
        assertFalse(control.setDifferentialPressure(true))
        assertFalse(control.setSwitchRows(true))
        assertEquals(beforeRestricted, sends.size)

        firmware.value = FirmwareInfoPacket(options = 0)
        assertFalse(control.setAirDensity(false))
        assertEquals(beforeRestricted, sends.size)

        firmware.value = FirmwareInfoPacket(options = 1 shl 28)
        admitSend = false
        assertFalse(control.setAirDensity(true))
        assertTrue(control.values.value!!.airDensity)
    }

}
