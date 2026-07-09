package org.secu3.android.models

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.secu3.android.models.packets.input.CheckEngineErrorsPacket
import org.secu3.android.models.packets.input.DiagInputPacket
import org.secu3.android.models.packets.out.ChangeModePacket
import org.secu3.android.models.packets.out.OpCompNc
import org.secu3.android.models.packets.out.params.StarterParamPacket
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
