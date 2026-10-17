package org.secu3.android.models.packets.out.params

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class IdlingParamPacketSignedRoundingTest {
    private data class Boundary(val word: Int, val factor: Float, val angle: Float, val temperature: Float)

    private fun packet(counts: Float): IdlingParamPacket = IdlingParamPacket(
        iFac1 = counts / 256f,
        iFac2 = counts / 256f,
        idlregMinAngle = counts / 32f,
        idlregMaxAngle = counts / 32f,
        idlregTurnOnTemp = counts / 4f,
    )

    private fun targetBytes(packet: IdlingParamPacket): IntArray {
        val encoded = packet.pack()
        return encoded.sliceArray(2..5) + encoded.sliceArray(10..15)
    }

    private fun frame(words: IntArray, tail: IntArray = intArrayOf()): IntArray {
        val payload = IdlingParamPacket().pack()
        val offsets = intArrayOf(2, 4, 10, 12, 14)
        words.forEachIndexed { index, word ->
            payload[offsets[index]] = word ushr 8
            payload[offsets[index] + 1] = word and 0xFF
        }
        return intArrayOf('@'.code) + payload + tail
    }

    @Test fun allFiveSignedFieldsUseProtocolRoundingForPositiveNegativeHalvesAndNearbyValue() {
        val negativeHalfBytes = targetBytes(packet(-1.5f))
        assertArrayEquals(IntArray(10) { if (it % 2 == 0) 0xFF else 0xFE }, negativeHalfBytes)
        val positiveHalfBytes = targetBytes(packet(1.5f))
        assertArrayEquals(IntArray(10) { if (it % 2 == 0) 0x00 else 0x02 }, positiveHalfBytes)
        val nearbyBytes = targetBytes(packet(-1.49f))
        assertArrayEquals(IntArray(10) { 0xFF }, nearbyBytes)
    }

    @Test fun signedBoundariesDecodeAndRepackForAllFiveFields() {
        for ((word, fac, angle, temp) in listOf(
            Boundary(0x8000, -128f, -1024f, -8192f),
            Boundary(0x7FFF, 127.99609375f, 1023.96875f, 8191.75f),
        )) {
            val packet = IdlingParamPacket().parse(frame(IntArray(5) { word })) as IdlingParamPacket
            assertEquals(fac, packet.iFac1, 0f)
            assertEquals(fac, packet.iFac2, 0f)
            assertEquals(angle, packet.idlregMinAngle, 0f)
            assertEquals(angle, packet.idlregMaxAngle, 0f)
            assertEquals(temp, packet.idlregTurnOnTemp, 0f)
            assertArrayEquals(IntArray(10) { if (it % 2 == 0) word ushr 8 else word and 0xFF }, targetBytes(packet))
        }
    }

    @Test fun signedWordsAndUnknownTailSurviveAnUnrelatedFlagEdit() {
        val words = intArrayOf(0xFFFF, 0xFFE0, 0xFFFF, 0x0005, 0xFFFC)
        val packet = IdlingParamPacket().parse(frame(words, intArrayOf(0xCA, 0xFE))) as IdlingParamPacket
        assertEquals(-1f / 256f, packet.iFac1, 0f)
        assertEquals(-.125f, packet.iFac2, 0f)
        assertEquals(-.03125f, packet.idlregMinAngle, 0f)
        assertEquals(.15625f, packet.idlregMaxAngle, 0f)
        assertEquals(-1f, packet.idlregTurnOnTemp, 0f)

        packet.idlFlags = 0x55
        assertArrayEquals(words.flatMap { listOf(it ushr 8, it and 0xFF) }.toIntArray(), targetBytes(packet))
        assertArrayEquals(intArrayOf(0xCA, 0xFE), packet.pack().takeLast(2).toIntArray())
    }
}
