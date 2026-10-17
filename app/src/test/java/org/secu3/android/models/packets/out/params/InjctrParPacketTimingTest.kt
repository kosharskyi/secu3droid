package org.secu3.android.models.packets.out.params

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class InjctrParPacketTimingTest {
    private fun frame(payload: IntArray): IntArray = intArrayOf('@'.code, ';'.code) + payload

    @Test fun allFourTimingWordsAreSignedSixteenthsAndPreserveExtensions() {
        val payload = IntArray(53)
        // Golden payload offsets 18, 20, 22 and 24 are the timing/cranking words.
        val words = intArrayOf(0x0018, 0xFFF0, 0x0001, 0x2D00)
        words.forEachIndexed { index, word ->
            payload[18 + index * 2] = word ushr 8
            payload[19 + index * 2] = word and 0xFF
        }
        payload[51] = 0xCA
        payload[52] = 0xFE

        val packet = InjctrParPacket().parse(frame(payload)) as InjctrParPacket
        assertArrayEquals(floatArrayOf(1.5f, -1f), packet.timing, 0f)
        assertArrayEquals(floatArrayOf(.0625f, 720f), packet.timingCrk, 0f)
        assertArrayEquals(intArrayOf(0xCA, 0xFE), packet.pack().takeLast(2).toIntArray())
        packet.angleSpec = 1
        val expectedTimingBytes = words.flatMap { listOf(it ushr 8, it and 0xFF) }.toIntArray()
        assertArrayEquals(expectedTimingBytes, packet.pack().sliceArray(19..26))
    }

    @Test fun signedTimingBoundariesAndNegativeHalfPhaseUseManagerRounding() {
        for ((word, expected) in listOf(0x8000 to -2048f, 0x7FFF to 2047.9375f)) {
            val payload = IntArray(51)
            payload[18] = word ushr 8
            payload[19] = word and 0xFF
            val packet = InjctrParPacket().parse(frame(payload)) as InjctrParPacket
            assertEquals(expected, packet.timing[0], 0f)
            assertArrayEquals(intArrayOf(word ushr 8, word and 0xFF), packet.pack().sliceArray(19..20))
        }
        val packet = InjctrParPacket(timing = floatArrayOf(-.03125f, 0f))
        assertArrayEquals(intArrayOf(0xFF, 0xFF), packet.pack().sliceArray(19..20))
    }

    @Test fun receivedCylinderCountIsRetainedButTransmitUsesZeroStub() {
        val payload = IntArray(53)
        payload[17] = 6 // Read-only RX cylinder metadata.
        val words = intArrayOf(0x0018, 0xFFF0, 0x0001, 0x2D00)
        words.forEachIndexed { index, word ->
            payload[18 + index * 2] = word ushr 8
            payload[19 + index * 2] = word and 0xFF
        }
        payload[51] = 0xCA
        payload[52] = 0xFE

        val packet = InjctrParPacket().parse(frame(payload)) as InjctrParPacket
        assertEquals(6, packet.ckpsEngineCyl)
        val timingBytes = words.flatMap { listOf(it ushr 8, it and 0xFF) }.toIntArray()

        assertEquals(0, packet.pack()[18])
        assertArrayEquals(timingBytes, packet.pack().sliceArray(19..26))
        assertArrayEquals(intArrayOf(0xCA, 0xFE), packet.pack().takeLast(2).toIntArray())

        packet.flags = packet.flags or 0x80
        assertEquals(6, packet.ckpsEngineCyl)
        assertEquals(0, packet.pack()[18])
        assertArrayEquals(timingBytes, packet.pack().sliceArray(19..26))
        assertArrayEquals(intArrayOf(0xCA, 0xFE), packet.pack().takeLast(2).toIntArray())
    }
}
