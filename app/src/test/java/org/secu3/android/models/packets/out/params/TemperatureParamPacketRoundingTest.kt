package org.secu3.android.models.packets.out.params

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class TemperatureParamPacketRoundingTest {
    private fun packet(counts: Float): TemperatureParamPacket = TemperatureParamPacket(
        ventOn = counts / 4f,
        ventOff = counts / 4f,
    )

    private fun frame(onWord: Int, offWord: Int, tail: IntArray = intArrayOf()): IntArray {
        val encoded = TemperatureParamPacket().pack()
        encoded[2] = onWord ushr 8
        encoded[3] = onWord and 0xFF
        encoded[4] = offWord ushr 8
        encoded[5] = offWord and 0xFF
        return intArrayOf('@'.code) + encoded + tail
    }

    @Test fun bothFanThresholdsUsePositiveNegativeHalfAndNearbyNonHalfRounding() {
        assertArrayEquals(intArrayOf(0xFF, 0xFE, 0xFF, 0xFE), packet(-1.5f).pack().sliceArray(2..5))
        assertArrayEquals(intArrayOf(0x00, 0x02, 0x00, 0x02), packet(1.5f).pack().sliceArray(2..5))
        assertArrayEquals(intArrayOf(0xFF, 0xFF, 0xFF, 0xFF), packet(-1.49f).pack().sliceArray(2..5))
        assertArrayEquals(intArrayOf(0xFF, 0xFF, 0x00, 0x01), packet(-.5f).copy(ventOff = .125f).pack().sliceArray(2..5))
    }

    @Test fun signedWordBoundariesDecodeExactlyAndRepack() {
        for ((word, expected) in listOf(0x8000 to -8192f, 0x7FFF to 8191.75f)) {
            val packet = TemperatureParamPacket().parse(frame(word, word)) as TemperatureParamPacket
            assertEquals(expected, packet.ventOn, 0f)
            assertEquals(expected, packet.ventOff, 0f)
            assertArrayEquals(intArrayOf(word ushr 8, word and 0xFF, word ushr 8, word and 0xFF), packet.pack().sliceArray(2..5))
        }
    }

    @Test fun signedThresholdWordsAndUnknownTailSurviveAnUnrelatedFlagEdit() {
        val packet = TemperatureParamPacket().parse(frame(0xFFFF, 0xFFE0, intArrayOf(0xCA, 0xFE))) as TemperatureParamPacket
        assertEquals(-.25f, packet.ventOn, 0f)
        assertEquals(-8f, packet.ventOff, 0f)
        packet.tmpFlags = 0x05
        assertArrayEquals(intArrayOf(0xFF, 0xFF, 0xFF, 0xE0), packet.pack().sliceArray(2..5))
        assertArrayEquals(intArrayOf(0xCA, 0xFE), packet.pack().takeLast(2).toIntArray())
    }
}
