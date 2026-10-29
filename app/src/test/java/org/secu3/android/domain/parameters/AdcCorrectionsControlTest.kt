package org.secu3.android.domain.parameters

import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.secu3.android.models.packets.base.OutputPacket
import org.secu3.android.models.packets.input.FirmwareInfoPacket
import org.secu3.android.models.packets.out.params.AdcCorrectionsParamPacket

class AdcCorrectionsControlTest {
    private class Harness(
        firmware: FirmwareInfoPacket? = null,
        var admitted: Boolean = true,
    ) {
        val sent = mutableListOf<IntArray>()
        val captured = mutableListOf<OutputPacket>()
        val control = AdcCorrectionsControl(MutableStateFlow(firmware)) { packet: OutputPacket ->
            captured += packet
            sent += packet.pack()
            admitted
        }
    }

    private fun adcFrame(mode: Boolean, factor: IntArray, correction: IntArray, flags: Int = if (mode) 0xA1 else 0xA0) =
        intArrayOf(64, 'r'.code, flags) + (0 until 12).flatMap { channel ->
            listOf(factor[channel] ushr 8, factor[channel] and 255) + (0..3).map { byte ->
                correction[channel] ushr ((3 - byte) * 8) and 255
            }
        }.toIntArray() + intArrayOf(0xCA, 0xFE)

    private fun packet(mode: Boolean = false, factor: Int = 0x4000, correction: Int = 0x00322000): AdcCorrectionsParamPacket =
        AdcCorrectionsParamPacket().parse(adcFrame(mode, IntArray(12) { factor }, IntArray(12) { correction }).dropLast(2).toIntArray()) as AdcCorrectionsParamPacket

    private fun AdcCorrectionsValues.ordered() = listOf(
        mapFactor, mapCorrection, voltageFactor, voltageCorrection, ctsFactor, ctsCorrection,
        tpsFactor, tpsCorrection, addI1Factor, addI1Correction, addI2Factor, addI2Correction,
        addI3Factor, addI3Correction, addI4Factor, addI4Correction, addI5Factor, addI5Correction,
        addI6Factor, addI6Correction, addI7Factor, addI7Correction, addI8Factor, addI8Correction,
    )

    @Test fun allNamedEditsPublishTheCorrespondingOneOfTwentyFourFields() {
        val h = Harness()
        val packet = AdcCorrectionsParamPacket()
        h.control.receive(packet)
        val edits: List<(Float) -> Boolean> = listOf(
            h.control::editMapFactor, h.control::editMapCorrection,
            h.control::editVoltageFactor, h.control::editVoltageCorrection,
            h.control::editCtsFactor, h.control::editCtsCorrection,
            h.control::editTpsFactor, h.control::editTpsCorrection,
            h.control::editAddI1Factor, h.control::editAddI1Correction,
            h.control::editAddI2Factor, h.control::editAddI2Correction,
            h.control::editAddI3Factor, h.control::editAddI3Correction,
            h.control::editAddI4Factor, h.control::editAddI4Correction,
            h.control::editAddI5Factor, h.control::editAddI5Correction,
            h.control::editAddI6Factor, h.control::editAddI6Correction,
            h.control::editAddI7Factor, h.control::editAddI7Correction,
            h.control::editAddI8Factor, h.control::editAddI8Correction,
        )
        val expected = MutableList(24) { 0f }
        edits.forEachIndexed { index, edit ->
            val value = if (index % 2 == 0) (index + 1) / 100f else -(index + 1) / 100f
            assertTrue("field $index", edit(value))
            expected[index] = value
            assertEquals("full state after field $index", expected, h.control.values.value!!.ordered())
        }
        assertEquals(24, h.sent.size)
        assertTrue(h.control.setCompensationMode(true))
        assertTrue(h.control.values.value!!.adcCompMode)
    }

    @Test fun invalidBoundsAndNonFiniteValuesNeverReachAnAcceptingSender() {
        val h = Harness()
        h.control.receive(AdcCorrectionsParamPacket())
        val edits: List<(Float) -> Boolean> = listOf(
            h.control::editMapFactor, h.control::editMapCorrection,
            h.control::editVoltageFactor, h.control::editVoltageCorrection,
            h.control::editCtsFactor, h.control::editCtsCorrection,
            h.control::editTpsFactor, h.control::editTpsCorrection,
            h.control::editAddI1Factor, h.control::editAddI1Correction,
            h.control::editAddI2Factor, h.control::editAddI2Correction,
            h.control::editAddI3Factor, h.control::editAddI3Correction,
            h.control::editAddI4Factor, h.control::editAddI4Correction,
            h.control::editAddI5Factor, h.control::editAddI5Correction,
            h.control::editAddI6Factor, h.control::editAddI6Correction,
            h.control::editAddI7Factor, h.control::editAddI7Correction,
            h.control::editAddI8Factor, h.control::editAddI8Correction,
        )
        edits.forEachIndexed { index, edit ->
            val invalid = if (index % 2 == 0) listOf(Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY, -0.001f, 4f)
                else listOf(Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY, -2.001f, 2f)
            invalid.forEach { assertFalse("field $index value $it", edit(it)) }
        }
        assertTrue(h.sent.isEmpty())
        assertEquals(List(24) { 0f }, h.control.values.value!!.ordered())
    }

    @Test fun channelCapabilitiesKeepAddI4IndependentAndUnknownFirmwarePermissive() {
        val tpicT = FirmwareInfoPacket(options = (1 shl 17) or (1 shl 5))
        val tpicTControl = Harness(tpicT).control
        tpicTControl.receive(AdcCorrectionsParamPacket())
        assertTrue(tpicTControl.editAddI4Factor(1f))
        assertFalse(tpicTControl.editAddI3Factor(1f))
        assertFalse(tpicTControl.editAddI5Factor(1f))

        val plain = Harness(FirmwareInfoPacket()).control
        plain.receive(AdcCorrectionsParamPacket())
        assertTrue(plain.editAddI3Factor(1f))
        assertTrue(plain.editAddI8Correction(.25f))
        assertFalse(plain.editAddI4Factor(1f))

        val unknown = Harness().control
        unknown.receive(AdcCorrectionsParamPacket())
        assertTrue(unknown.editAddI4Factor(1f))
        assertTrue(unknown.editAddI8Factor(1f))
    }

    @Test fun refusedModeAndFieldCommandsRestoreHiddenRawFallbackBeforeLaterAcceptedEdit() {
        val factors = IntArray(12).also { it[1] = 0x8000 }
        val corrections = IntArray(12) { 0x12345670 + it }
        val original = adcFrame(true, factors, corrections)
        val h = Harness(admitted = false)
        val packet = AdcCorrectionsParamPacket().parse(original) as AdcCorrectionsParamPacket
        h.control.receive(packet)
        val before = h.control.values.value
        assertFalse(h.control.editMapCorrection(0f)) // same displayed value still retires fallback if accepted
        assertFalse(h.control.editMapFactor(1f))
        assertFalse(h.control.setCompensationMode(false))
        assertEquals(before, h.control.values.value)
        h.admitted = true
        assertTrue(h.control.editVoltageFactor(2.5f))
        val sent = h.sent.last()
        for (channel in 0 until 12) {
            val offset = 2 + channel * 6
            val expectedFactor = if (channel == 1) intArrayOf(0xA0, 0) else intArrayOf(0, 0)
            assertArrayEquals(expectedFactor, sent.sliceArray(offset until offset + 2))
            if (channel == 1) continue
            assertArrayEquals(
                intArrayOf(corrections[channel] ushr 24 and 255, corrections[channel] ushr 16 and 255,
                    corrections[channel] ushr 8 and 255, corrections[channel] and 255),
                sent.sliceArray(offset + 2 until offset + 6),
            )
        }
        assertEquals(0xA1, sent[1])
        assertEquals(0xCA, sent[sent.lastIndex - 1])
        assertEquals(0xFE, sent.last())
    }

    @Test fun admittedModeOffThenOnDoesNotResurrectZeroFactorRawFallback() {
        val raw = IntArray(12) { 0x12345670 + it }
        val h = Harness()
        val packet = AdcCorrectionsParamPacket().parse(adcFrame(true, IntArray(12), raw)) as AdcCorrectionsParamPacket
        h.control.receive(packet)
        assertTrue(h.control.setCompensationMode(false))
        assertTrue(h.control.setCompensationMode(true))
        for (channel in 0 until 12) assertArrayEquals(
            intArrayOf(0, 0, 0x20, 0), h.sent.last().sliceArray(4 + channel * 6..7 + channel * 6),
        )
    }

    @Test fun refusedCommandDoesNotPublishAndOutputPacketKeepsCapturedBytes() {
        val h = Harness(admitted = true)
        val packet = AdcCorrectionsParamPacket()
        h.control.receive(packet)
        assertTrue(h.control.editMapFactor(1f))
        val captured = h.captured.first()
        val first = captured.pack()
        assertTrue(h.control.editMapFactor(2f))
        assertArrayEquals(first, captured.pack())
        assertEquals(2f, h.control.values.value!!.mapFactor, 0f)
        h.admitted = false
        assertFalse(h.control.editMapFactor(3f))
        assertEquals(2f, h.control.values.value!!.mapFactor, 0f)
    }

    @Test fun unloadedAndResetControlsArePassive() {
        val h = Harness()
        assertFalse(h.control.editMapFactor(1f))
        assertFalse(h.control.setCompensationMode(true))
        h.control.receive(AdcCorrectionsParamPacket())
        h.control.reset()
        assertFalse(h.control.editMapFactor(1f))
        assertFalse(h.control.setCompensationMode(true))
        assertEquals(null, h.control.values.value)
        assertTrue(h.sent.isEmpty())
    }
}
