package org.secu3.android.models.packets.out.params

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class AnglesParamPacketRoundingTest {
    private fun packet(counts: Float): AnglesParamPacket = AnglesParamPacket(
        maxAngle = counts / 32f,
        minAngle = counts / 32f,
        angleCorrection = counts / 32f,
        angleDecSpeed = counts / 32f,
        angleIncSpeed = counts / 32f,
        shift_ingtim = counts / 32f,
    )

    private fun expectedBytes(word: Int): IntArray {
        val oneWord = intArrayOf((word ushr 8) and 0xFF, word and 0xFF)
        return ((0 until 5).flatMap { oneWord.toList() } + listOf(0, 0) + oneWord.toList()).toIntArray()
    }

    @Test fun allSixAngleFieldsUseProtocolRoundingForPositiveNegativeHalvesAndNearbyValue() {
        assertArrayEquals(expectedBytes(-2), packet(-1.5f).pack().sliceArray(1..14))
        assertArrayEquals(expectedBytes(2), packet(1.5f).pack().sliceArray(1..14))
        assertArrayEquals(expectedBytes(-1), packet(-1.49f).pack().sliceArray(1..14))
    }

    @Test fun signedAngleWordsRemainExactAfterUnrelatedEditAndUnknownTail() {
        val words = intArrayOf(0xFFFF, 0xFFE0, 0x8000, 0x7FFF, 0xFFF1, 0x0005)
        val payload = IntArray(16)
        val offsets = intArrayOf(0, 2, 4, 6, 8, 12)
        words.forEachIndexed { index, word ->
            payload[offsets[index]] = word ushr 8
            payload[offsets[index] + 1] = word and 0xFF
        }
        payload[14] = 0xCA
        payload[15] = 0xFE
        val packet = AnglesParamPacket().parse(intArrayOf('@'.code, 'm'.code) + payload) as AnglesParamPacket

        assertEquals(-.03125f, packet.maxAngle, 0f)
        assertEquals(-1f, packet.minAngle, 0f)
        assertEquals(-1024f, packet.angleCorrection, 0f)
        assertEquals(1023.96875f, packet.angleDecSpeed, 0f)
        assertEquals(-.46875f, packet.angleIncSpeed, 0f)
        assertEquals(.15625f, packet.shift_ingtim, 0f)

        packet.igntimFlags = 1
        assertArrayEquals(words.flatMap { listOf(it ushr 8, it and 0xFF) }.toIntArray(), packet.pack().sliceArray(1..10) + packet.pack().sliceArray(13..14))
        assertArrayEquals(intArrayOf(0xCA, 0xFE), packet.pack().takeLast(2).toIntArray())
    }
}
