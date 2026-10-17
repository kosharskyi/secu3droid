package org.secu3.android.models.packets.out.params

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class InjctrParPacketFffConstTest {
    private fun frame(word: Int, payloadSize: Int = 51): IntArray {
        val payload = IntArray(payloadSize)
        payload[27] = word ushr 8
        payload[28] = word and 0xFF
        return intArrayOf('@'.code, ';'.code) + payload
    }

    private fun encodedWord(packet: InjctrParPacket): Int =
        (packet.pack()[28] shl 8) or packet.pack()[29]

    @Test fun retainsPhysicalFloatAndRawUnsignedWordAcrossUnrelatedEdits() {
        val packet = InjctrParPacket().parse(frame(0x2226, 53).also {
            it[it.lastIndex - 1] = 0xCA
            it[it.lastIndex] = 0xFE
        }) as InjctrParPacket
        assertEquals(8003.540039f, packet.fffConst, .0001f)
        assertEquals(0x2226, encodedWord(packet))

        packet.flags = packet.flags or 0x80
        packet.angleSpec = 1
        packet.minPw = 1234
        assertEquals(8003.540039f, packet.fffConst, .0001f)
        assertEquals(0x2226, encodedWord(packet))
        assertArrayEquals(intArrayOf(0xCA, 0xFE), packet.pack().takeLast(2).toIntArray())
    }

    @Test fun zeroAndUnsignedBoundaryWordsRemainLossless() {
        for (word in listOf(0x0000, 0x0001, 0x8000, 0xFFFF)) {
            val packet = InjctrParPacket().parse(frame(word)) as InjctrParPacket
            assertEquals("word 0x${word.toString(16)}", word, encodedWord(packet))
        }
        assertEquals(0f, (InjctrParPacket().parse(frame(0)) as InjctrParPacket).fffConst, 0f)
        assertEquals(30000f, (InjctrParPacket().parse(frame(0x8000)) as InjctrParPacket).fffConst, .001f)
    }

    @Test fun explicitPhysicalValue10000UsesManagerRoundedUnsignedWord() {
        val packet = InjctrParPacket(fffConst = 10000f)
        assertEquals(10923, encodedWord(packet))
        assertArrayEquals(intArrayOf(0x2A, 0xAB), packet.pack().sliceArray(28..29))
    }

    @Test fun equalityAndHashUseFloatSemanticsForThePhysicalValue() {
        val parsed = InjctrParPacket().parse(frame(0x2226)) as InjctrParPacket
        val sameValue = parsed.copy()
        assertEquals(parsed, sameValue)
        assertEquals(parsed.hashCode(), sameValue.hashCode())

        val positiveZero = InjctrParPacket(fffConst = 0f)
        val negativeZero = InjctrParPacket(fffConst = -0f)
        assertNotEquals(positiveZero, negativeZero)
        assertNotEquals(positiveZero.hashCode(), negativeZero.hashCode())
    }
}
