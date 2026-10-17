package org.secu3.android.models.packets.out.params

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class CkpsParamPacketSignedAnglesTest {
    private fun frame(widthWord: Int, degreesWord: Int, tail: IntArray = intArrayOf()): IntArray {
        val payload = IntArray(12 + tail.size)
        payload[6] = widthWord ushr 8
        payload[7] = widthWord and 0xFF
        payload[8] = degreesWord ushr 8
        payload[9] = degreesWord and 0xFF
        tail.copyInto(payload, 12)
        return intArrayOf('@'.code, 't'.code) + payload
    }

    private fun parse(widthWord: Int, degreesWord: Int, tail: IntArray = intArrayOf()) =
        CkpsParamPacket().parse(frame(widthWord, degreesWord, tail)) as CkpsParamPacket

    @Test fun hallAnglesDecodeSignedFractionalWordsAndKeepThemAfterUnrelatedEdit() {
        val packet = parse(0xFFFF, 0xFFE0, intArrayOf(0xCA, 0xFE))
        assertEquals(-.03125f, packet.hallWndWidth, 0f)
        assertEquals(-1f, packet.hallDegreesBtdc, 0f)

        packet.hallFlags = packet.hallFlags or 0x04
        assertArrayEquals(intArrayOf(0xFF, 0xFF, 0xFF, 0xE0), packet.pack().sliceArray(7..10))
        assertArrayEquals(intArrayOf(0xCA, 0xFE), packet.pack().takeLast(2).toIntArray())
    }

    @Test fun signedAngleBoundariesDecodeAndRepack() {
        for ((word, expected) in listOf(0x8000 to -1024f, 0x7FFF to 1023.96875f)) {
            val packet = parse(word, word)
            assertEquals(expected, packet.hallWndWidth, 0f)
            assertEquals(expected, packet.hallDegreesBtdc, 0f)
            assertArrayEquals(intArrayOf(word ushr 8, word and 0xFF, word ushr 8, word and 0xFF), packet.pack().sliceArray(7..10))
        }
    }

    @Test fun bothHallFieldsUseProtocolHalfAwayFromZeroRounding() {
        val negative = CkpsParamPacket(hallWndWidth = -.015625f, hallDegreesBtdc = -.015625f)
        assertArrayEquals(intArrayOf(0xFF, 0xFF, 0xFF, 0xFF), negative.pack().sliceArray(7..10))
        val positive = CkpsParamPacket(hallWndWidth = .015625f, hallDegreesBtdc = .015625f)
        assertArrayEquals(intArrayOf(0x00, 0x01, 0x00, 0x01), positive.pack().sliceArray(7..10))
    }
}
