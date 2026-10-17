package org.secu3.android.models.packets.out.params

import org.junit.Assert.assertEquals
import org.junit.Assert.assertArrayEquals
import org.junit.Test

class IdlingRpmEncodingTest {
    private fun receiveFrame(rpmOnRunAddCount: Int, integratorRpmCount: Int, vararg tail: Int): IntArray {
        val frame = intArrayOf('@'.code, IdlingParamPacket.DESCRIPTOR.code) + IntArray(42) + tail
        frame[18] = rpmOnRunAddCount
        frame[29] = integratorRpmCount
        return frame
    }

    @Test fun bothRpmValuesRoundToNearestUnsignedTenRpmCount() {
        val cases = listOf(0 to 0, 10 to 1, 14 to 1, 15 to 2, 19 to 2)
        for ((rpm, count) in cases) {
            val packet = IdlingParamPacket(rpmOnRunAdd = rpm, integratorRpmLim = rpm)
            val wire = packet.pack()
            assertEquals("rpmOnRunAdd=$rpm", count, wire[17])
            assertEquals("integratorRpmLim=$rpm", count, wire[28])
        }

        val maximums = IdlingParamPacket(rpmOnRunAdd = 2000, integratorRpmLim = 1200).pack()
        assertEquals(200, maximums[17])
        assertEquals(120, maximums[28])
    }

    @Test fun fifteenRpmEncodesAsTwoCountsAndParsesBackAsTwentyRpm() {
        val encoded = IdlingParamPacket(rpmOnRunAdd = 15, integratorRpmLim = 15).pack()
        assertEquals(2, encoded[17])
        assertEquals(2, encoded[28])

        val parsed = IdlingParamPacket().parse(intArrayOf('@'.code) + encoded) as IdlingParamPacket
        assertEquals(20, parsed.rpmOnRunAdd)
        assertEquals(20, parsed.integratorRpmLim)
    }

    @Test fun incomingUnsignedCountsAndUnknownTailSurviveAnUnrelatedFlagEdit() {
        val packet = IdlingParamPacket().parse(receiveFrame(15, 25, 0xCA, 0xFE)) as IdlingParamPacket
        assertEquals(150, packet.rpmOnRunAdd)
        assertEquals(250, packet.integratorRpmLim)

        packet.idlFlags = 0x55
        val wire = packet.pack()
        assertEquals(15, wire[17])
        assertEquals(25, wire[28])
        assertArrayEquals(intArrayOf(0xCA, 0xFE), wire.takeLast(2).toIntArray())
    }
}
