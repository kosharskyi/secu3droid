package org.secu3.android.models.packets.out.params

import org.junit.Assert.assertEquals
import org.junit.Test

class AccelerationParamPacketTest {
    private fun frame(coldWord: Int, tail: IntArray = intArrayOf()): IntArray {
        val payload = IntArray(11 + tail.size)
        payload[1] = (coldWord ushr 8) and 0xFF
        payload[2] = coldWord and 0xFF
        tail.copyInto(payload, 11)
        return intArrayOf('@'.code, '|'.code) + payload
    }

    private fun packedColdWord(packet: AccelerationParamPacket): Int =
        (packet.pack()[2] shl 8) or packet.pack()[3]

    @Test fun coldMultiplierUsesUnsignedBaselineAndFractionalDiscrete() {
        for ((word, expected) in listOf(
            0x0000 to 100f,
            0x0001 to 100.78125f,
            0x0040 to 150f,
            0x0080 to 200f,
            0x0280 to 600f,
            0x8000 to 25700f,
            0xFFFF to 51299.21875f,
        )) {
            val packet = AccelerationParamPacket().parse(frame(word)) as AccelerationParamPacket
            assertEquals("raw 0x${word.toString(16)}", expected, packet.injAeColdaccMult, .0001f)
            assertEquals("raw 0x${word.toString(16)}", word, packedColdWord(packet))
        }
    }

    @Test fun unrelatedEditPreservesFractionalWordAndUnknownTail() {
        val packet = AccelerationParamPacket().parse(frame(1, intArrayOf(0xCA, 0xFE))) as AccelerationParamPacket
        packet.injAeTpsdotThrd = 75
        assertEquals(100.78125f, packet.injAeColdaccMult, 0f)
        assertEquals(1, packedColdWord(packet))
        assertEquals(listOf(0xCA, 0xFE), packet.pack().takeLast(2))
    }
}
