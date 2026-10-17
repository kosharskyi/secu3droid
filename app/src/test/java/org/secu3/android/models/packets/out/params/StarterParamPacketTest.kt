package org.secu3.android.models.packets.out.params

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class StarterParamPacketTest {
    @Test
    fun afterstartStrokeWordsRoundToFourStrokeCounts() {
        val expectedCounts = intArrayOf(0, 0, 1, 1, 1, 1, 2)
        (0..6).forEachIndexed { index, strokes ->
            val packet = StarterParamPacket(injAftstrStroke = strokes, injAftStrokes1 = strokes)
            assertEquals(expectedCounts[index], packet.pack()[7])
            assertEquals(expectedCounts[index], packet.pack()[15])
        }

        assertEquals(255, StarterParamPacket(injAftstrStroke = 1020).pack()[7])
        assertEquals(255, StarterParamPacket(injAftStrokes1 = 1020).pack()[15])
    }

    @Test
    fun parsedFourCountValuesRemainExactAcrossUnrelatedEditAndUnknownTail() {
        val payload = IntArray(21)
        payload[6] = 3
        payload[14] = 2
        payload[19] = 0xCA
        payload[20] = 0xFE
        // The standard frame payload is 19 bytes; retain the two extension bytes after it.
        val frame = intArrayOf('@'.code, 'o'.code) + payload
        val packet = StarterParamPacket().parse(frame) as StarterParamPacket

        assertEquals(12, packet.injAftstrStroke)
        assertEquals(8, packet.injAftStrokes1)
        assertArrayEquals(intArrayOf(3, 2), intArrayOf(packet.pack()[7], packet.pack()[15]))
        assertArrayEquals(intArrayOf(0xCA, 0xFE), packet.pack().takeLast(2).toIntArray())

        packet.starterOff = 123
        assertArrayEquals(intArrayOf(3, 2), intArrayOf(packet.pack()[7], packet.pack()[15]))
        assertArrayEquals(intArrayOf(0xCA, 0xFE), packet.pack().takeLast(2).toIntArray())

        val oneCountFrame = frame.copyOf().apply {
            this[8] = 1
            this[16] = 1
        }
        val oneCountPacket = StarterParamPacket().parse(oneCountFrame) as StarterParamPacket
        assertEquals(4, oneCountPacket.injAftstrStroke)
        assertEquals(4, oneCountPacket.injAftStrokes1)
    }

    @Test
    fun editedTwoOrThreeStrokesAreRoundedByManagerRuleOnBothFields() {
        assertEquals(1, StarterParamPacket(injAftstrStroke = 2).pack()[7])
        assertEquals(1, StarterParamPacket(injAftStrokes1 = 2).pack()[15])
        assertEquals(1, StarterParamPacket(injAftstrStroke = 3).pack()[7])
        assertEquals(1, StarterParamPacket(injAftStrokes1 = 3).pack()[15])
    }
}
