package org.secu3.android.models

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.secu3.android.R
import org.secu3.android.models.packets.input.FirmwareInfoPacket
import org.secu3.android.models.packets.input.CheckEngineErrorsPacket
import org.secu3.android.models.packets.input.DiagInputPacket
import org.secu3.android.models.packets.input.FnNameDatPacket
import org.secu3.android.models.packets.out.ChangeModePacket
import org.secu3.android.models.packets.out.OpCompNc
import org.secu3.android.models.packets.out.params.StarterParamPacket
import org.secu3.android.ui.parameters.parameterCapabilities
import org.secu3.android.ui.parameters.parameterPageSpecs
import org.secu3.android.utils.Task

class PacketTest {

    @Test
    fun changeModePacket_packsDescriptorNextDescriptorAndStubByte() {
        val packet = ChangeModePacket.getPacket(Task.Secu3ReadFirmwareInfo)

        assertArrayEquals(intArrayOf('h'.code, 'y'.code, 0), packet.pack())
    }

    @Test
    fun opCompNc_packsAndParsesSaveEepromCommand() {
        val packet = OpCompNc.getSaveEepromCommand()

        assertArrayEquals(intArrayOf('u'.code, 0, 1), packet.pack())

        val parsed = OpCompNc().parse(intArrayOf('@'.code, 'u'.code, 0, 1)) as OpCompNc

        assertTrue(parsed.isEepromParamSave)
    }

    @Test
    fun rawPacket_parsesCheckEngineErrorsPacket() {
        val parsed = RawPacket(
            intArrayOf('@'.code, 'v'.code, 0x01, 0x02, 0x03, 0x04, 0, 0)
        ).parse(null) as CheckEngineErrorsPacket

        assertEquals(0x01020304, parsed.errors)
        assertTrue(parsed.isError(2))
        assertFalse(parsed.isError(3))
    }

    @Test
    fun rawPacket_parsesFirmwareSignatureOptionsAndVersionAtProtocolOffsets() {
        val signature = "SECU-3 firmware v5.0. Build [Aug 17 2026 19:55:3".padEnd(52, 'Q').take(52)
        val signatureBytes = signature.map { it.code }.toIntArray()
        val frame = intArrayOf('@'.code, 'y'.code) + signatureBytes +
            intArrayOf(0x00, 0x02, 0x00, 0x00, 0x50, 0, 0)

        val parsed = RawPacket(frame).parse(null) as FirmwareInfoPacket

        assertEquals(signature, parsed.tag)
        assertEquals(1 shl 17, parsed.options)
        assertEquals(0, parsed.version[0])
        assertEquals(5, parsed.version[1])
        assertTrue(parsed.isSecu3T)
        assertFalse(parameterCapabilities(parsed).dbw)
        assertFalse(parameterPageSpecs(parsed).any { it.titleRes == R.string.params_tab_el_throttle_title })
    }

    @Test
    fun rawPacket_returnsNullForUnknownDescriptor() {
        val parsed = RawPacket(intArrayOf('@'.code, '?'.code, 0, 0)).parse(null)

        assertNull(parsed)
    }

    @Test
    fun rawPacket_returnsNullWhenKnownPacketIsTooShort() {
        val parsed = RawPacket(intArrayOf('@'.code, 'v'.code, 0x01, 0, 0)).parse(null)

        assertNull(parsed)
    }

    @Test
    fun rawPacket_returnsNullForEmptyAndShortOperationPackets() {
        assertNull(RawPacket(intArrayOf()).parse(null))
        assertNull(RawPacket(intArrayOf('@'.code, 'u'.code)).parse(null))
        assertNull(RawPacket(intArrayOf('@'.code, 'u'.code, 0)).parse(null))
    }

    @Test
    fun rawPacket_rejectsInvalidEnvelopeBytes() {
        assertNull(RawPacket(intArrayOf('!'.code, 'v'.code, 0, 0, 0, 0)).parse(null))
        assertNull(RawPacket(intArrayOf('@'.code, 'v'.code, -1, 0, 0, 0)).parse(null))
        assertNull(RawPacket(intArrayOf('@'.code, 'v'.code, 256, 0, 0, 0)).parse(null))
    }

    @Test
    fun rawPacket_returnsNullForFunctionNameCountZeroOrIndexOutsideCount() {
        val name = IntArray(16) { 'x'.code }
        val zeroCount = intArrayOf('@'.code, 'p'.code, 0, 0) + name + intArrayOf(0, 0)
        val invalidIndex = intArrayOf('@'.code, 'p'.code, 2, 2) + name + intArrayOf(0, 0)

        assertNull(RawPacket(zeroCount).parse(null))
        assertNull(RawPacket(invalidIndex).parse(null))
    }

    @Test
    fun fnNameDatPacket_rejectsInvalidCountAndIndex() {
        val name = IntArray(16) { 'x'.code }

        org.junit.Assert.assertThrows(IllegalArgumentException::class.java) {
            FnNameDatPacket().parse(intArrayOf('@'.code, 'p'.code, 0, 0) + name)
        }
        org.junit.Assert.assertThrows(IllegalArgumentException::class.java) {
            FnNameDatPacket().parse(intArrayOf('@'.code, 'p'.code, 1, 1) + name)
        }
    }

    @Test
    fun rawPacket_allKnownDescriptorsRejectEveryTruncationAndParseValidFrameAfterward() {
        // Payload sizes are the required field lengths from each packet's wire layout.
        val requiredPayloadBytes = linkedMapOf(
            'q' to 116, 'y' to 57, 's' to 26, 'v' to 4, 'x' to 4, '=' to 31,
            'o' to 19, 'm' to 14, 'l' to 42, 'n' to 47, 'j' to 15, 'k' to 24,
            'r' to 73, 't' to 12, 'w' to 19, 'z' to 22, '%' to 14, '#' to 16,
            '&' to 67, ';' to 51, '-' to 28, '|' to 11, '*' to 13, 'O' to 24,
            'P' to 16, 'p' to 18, 'u' to 2
        )

        for ((descriptor, payloadSize) in requiredPayloadBytes) {
            val payload = IntArray(payloadSize)
            if (descriptor == 'p') payload[0] = 1 // one name, entry zero
            val validFrame = intArrayOf('@'.code, descriptor.code) + payload + intArrayOf(0, 0)

            for (presentPayloadBytes in 0 until payloadSize) {
                val truncatedFrame = intArrayOf('@'.code, descriptor.code) +
                    payload.copyOf(presentPayloadBytes) + intArrayOf(0, 0)
                assertNull("descriptor=$descriptor payload=$presentPayloadBytes", RawPacket(truncatedFrame).parse(null))
            }

            assertTrue("valid frame after truncations for descriptor=$descriptor", RawPacket(validFrame).parse(null) != null)
        }
    }

    @Test
    fun rawPacket_preservesOptionalExtensionBytesOnSupportedPacket() {
        val payload = intArrayOf(
            0x03, 0xE8, 0x00, 0xC8, 0x00, 0x96, 0x03, 0x04, 0xE2, 0x09,
            0xC4, 0x0C, 0x0C, 0x80, 0x02, 0x07, 0x03, 0x01, 0x13, 0xAA, 0xBB
        )
        val parsed = RawPacket(intArrayOf('@'.code, 'o'.code) + payload + intArrayOf(0, 0))
            .parse(null) as StarterParamPacket

        assertArrayEquals(intArrayOf('o'.code) + payload, parsed.pack())
    }

    @Test
    fun diagInputPacket_parsesScaledValuesAndBitFlags() {
        val parsed = DiagInputPacket().parse(
            intArrayOf(
                '@'.code,
                '='.code,
                0x5A,
                0x01, 0xF4,
                0x03, 0xE8,
                0xFF, 0x38,
                0x00, 0x64,
                0x00, 0xC8,
                0x01, 0x2C,
                0x01, 0x90,
                0x01, 0xF4,
                0x02, 0x58,
                0x02, 0xBC,
                0x03, 0x20,
                0x03, 0x84,
                0x03, 0xE8,
                0x04, 0x4C,
                0x02, 0x25
            )
        ) as DiagInputPacket

        assertEquals(0x5A, parsed.flags)
        assertEquals(1.25f, parsed.voltage)
        assertEquals(2.5f, parsed.map)
        assertEquals(-0.5f, parsed.temperature)
        assertEquals(0.25f, parsed.addI1)
        assertEquals(0.5f, parsed.addI2)
        assertEquals(0.75f, parsed.addI3)
        assertEquals(1.0f, parsed.addI4)
        assertEquals(1.25f, parsed.addI5)
        assertEquals(1.5f, parsed.addI6)
        assertEquals(1.75f, parsed.addI7)
        assertEquals(2.0f, parsed.addI8)
        assertEquals(2.25f, parsed.carb)
        assertEquals(2.5f, parsed.ks1)
        assertEquals(2.75f, parsed.ks2)
        assertTrue(parsed.gasV)
        assertFalse(parsed.ckps)
        assertTrue(parsed.refS)
        assertFalse(parsed.ps)
        assertFalse(parsed.bl)
        assertTrue(parsed.de)
        assertFalse(parsed.ign_i)
        assertFalse(parsed.cond_i)
        assertFalse(parsed.epas_i)
        assertTrue(parsed.gpa4_i)
    }

    @Test
    fun starterParamPacket_roundTripsParsedValuesIncludingUnhandledParams() {
        val rawPacket = intArrayOf(
            '@'.code,
            'o'.code,
            0x03, 0xE8,
            0x00, 0xC8,
            0x00, 0x96,
            0x03,
            0x04, 0xE2,
            0x09, 0xC4,
            0x0C,
            0x0C, 0x80,
            0x02,
            0x07,
            0x03,
            0x01, 0x13,
            0xAA, 0xBB
        )

        val parsed = StarterParamPacket().parse(rawPacket) as StarterParamPacket

        assertEquals(1000, parsed.starterOff)
        assertEquals(200, parsed.smapAbandon)
        assertEquals(1.5f, parsed.crankToRunTime)
        assertEquals(12, parsed.injAftstrStroke)
        assertEquals(4.0f, parsed.injPrimeCold)
        assertEquals(8.0f, parsed.injPrimeHot)
        assertEquals(1.2f, parsed.injPrimeDelay)
        assertEquals(50.0f, parsed.injFloodclearTps)
        assertEquals(8, parsed.injAftStrokes1)
        assertEquals(7, parsed.stblStrCnt)
        assertTrue(parsed.allowStartOnClearFlood)
        assertTrue(parsed.limitMaxInjPwOnCranking)
        assertEquals(2.75f, parsed.injCrankToRun_time1)
        assertArrayEquals(rawPacket.sliceArray(1 until rawPacket.size), parsed.pack())
    }
}
