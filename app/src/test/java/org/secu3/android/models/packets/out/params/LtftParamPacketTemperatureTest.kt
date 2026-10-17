package org.secu3.android.models.packets.out.params

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class LtftParamPacketTemperatureTest {
    private fun frame(words: IntArray, tail: IntArray = intArrayOf()): IntArray {
        val payload = IntArray(24 + tail.size)
        words.forEachIndexed { index, word ->
            val offset = 1 + index * 2
            payload[offset] = word ushr 8
            payload[offset + 1] = word and 0xFF
        }
        tail.copyInto(payload, 24)
        return intArrayOf('@'.code, 'O'.code) + payload
    }

    @Test fun threeLearnTemperaturesDecodeSignedQuarterDegreesAndPreserveWordAndTail() {
        val words = intArrayOf(0xFFFC, 0x0005, 0x8000)
        val packet = LtftParamPacket().parse(frame(words, intArrayOf(0xCA, 0xFE))) as LtftParamPacket
        assertEquals(-1f, packet.learnClt, 0f)
        assertEquals(1.25f, packet.learnCltUp, 0f)
        assertEquals(-8192f, packet.learnIatUp, 0f)

        packet.mode = 1
        assertArrayEquals(intArrayOf(0xFF, 0xFC, 0x00, 0x05, 0x80, 0x00), packet.pack().sliceArray(2..7))
        assertArrayEquals(intArrayOf(0xCA, 0xFE), packet.pack().takeLast(2).toIntArray())
    }

    @Test fun signedTemperatureBoundariesDecodeAndRoundTrip() {
        for ((word, expected) in listOf(0x8000 to -8192f, 0x7FFF to 8191.75f)) {
            val packet = LtftParamPacket().parse(frame(intArrayOf(word, 0, 0))) as LtftParamPacket
            assertEquals(expected, packet.learnClt, 0f)
            assertArrayEquals(intArrayOf(word ushr 8, word and 0xFF), packet.pack().sliceArray(2..3))
        }
    }

    @Test fun eachLearnTemperatureUsesHalfAwayFromZeroProtocolRounding() {
        val packet = LtftParamPacket(learnClt = -.125f, learnCltUp = .125f, learnIatUp = -.125f)
        assertArrayEquals(intArrayOf(0xFF, 0xFF, 0x00, 0x01, 0xFF, 0xFF), packet.pack().sliceArray(2..7))
    }
}
