package org.secu3.android.models.packets.out.params

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class CarburLambdaTemperatureRoundingTest {
    private fun frame(packet: IntArray, vararg tail: Int): IntArray =
        intArrayOf('@'.code) + packet + tail

    @Test fun carburThresholdRoundsSignedHalfCountsAwayFromZero() {
        fun bytes(value: Float) = CarburParamPacket(fuelcutCtsThrd = value).pack().sliceArray(17..18)

        assertArrayEquals(intArrayOf(0, 1), bytes(.125f))
        assertArrayEquals(intArrayOf(0xFF, 0xFF), bytes(-.125f))
        assertArrayEquals(intArrayOf(0, 2), bytes(.375f))
        assertArrayEquals(intArrayOf(0xFF, 0xFE), bytes(-.375f))
        assertArrayEquals(intArrayOf(0, 0), bytes(.124f))
        assertArrayEquals(intArrayOf(0, 0), bytes(-.124f))
    }

    @Test fun lambdaThresholdRoundsSignedHalfCountsAwayFromZero() {
        fun bytes(value: Float) = LambdaParamPacket(tempThrd = value).pack().sliceArray(10..11)

        assertArrayEquals(intArrayOf(0, 1), bytes(.125f))
        assertArrayEquals(intArrayOf(0xFF, 0xFF), bytes(-.125f))
        assertArrayEquals(intArrayOf(0, 2), bytes(.375f))
        assertArrayEquals(intArrayOf(0xFF, 0xFE), bytes(-.375f))
        assertArrayEquals(intArrayOf(0, 0), bytes(.124f))
        assertArrayEquals(intArrayOf(0, 0), bytes(-.124f))
    }

    @Test fun carburSignedBoundariesAndUnknownTailSurviveAnUnrelatedEdit() {
        val encoded = CarburParamPacket().pack()
        encoded[17] = 0x80; encoded[18] = 0x00
        val parsed = CarburParamPacket().parse(frame(encoded, 0xCA, 0xFE)) as CarburParamPacket
        assertEquals(-8192f, parsed.fuelcutCtsThrd, 0f)
        parsed.carbInvers = 7
        assertArrayEquals(intArrayOf(0x80, 0x00), parsed.pack().sliceArray(17..18))
        assertArrayEquals(intArrayOf(0xCA, 0xFE), parsed.pack().takeLast(2).toIntArray())

        encoded[17] = 0x7F; encoded[18] = 0xFF
        val upper = CarburParamPacket().parse(frame(encoded)) as CarburParamPacket
        assertEquals(8191.75f, upper.fuelcutCtsThrd, 0f)
        assertArrayEquals(intArrayOf(0x7F, 0xFF), upper.pack().sliceArray(17..18))
    }

    @Test fun carburEnrichmentThresholdDecodesSignedWordsAndSurvivesUnrelatedEdits() {
        val expected = listOf(0 to 0f, 0x7FFF to (32767f / 64f), 0x8000 to (-512f), 0xFFFF to (-1f / 64f))
        for ((raw, value) in expected) {
            val incoming = frame(CarburParamPacket().pack(), 0xCA, 0xFE)
            incoming[7] = raw ushr 8
            incoming[8] = raw and 255

            val packet = CarburParamPacket().parse(incoming) as CarburParamPacket
            assertEquals(value, packet.feOnThresholds, 0f)
            packet.carbInvers = 1
            assertArrayEquals(intArrayOf(raw ushr 8, raw and 255), packet.pack().sliceArray(6..7))
            assertArrayEquals(intArrayOf(0xCA, 0xFE), packet.pack().takeLast(2).toIntArray())
        }
    }

    @Test fun carburEnrichmentThresholdRoundsSignedHalfCountsAwayFromZero() {
        fun bytes(value: Float) = CarburParamPacket(feOnThresholds = value).pack().sliceArray(6..7)

        assertArrayEquals(intArrayOf(0, 1), bytes(1f / 128f))
        assertArrayEquals(intArrayOf(0xFF, 0xFF), bytes(-1f / 128f))
        assertArrayEquals(intArrayOf(0xFF, 0xFE), bytes(-3f / 128f))
        assertArrayEquals(intArrayOf(0, 0), bytes(0.49f / 64f))
    }

    @Test fun lambdaSignedBoundariesAndUnknownTailSurviveAnUnrelatedEdit() {
        val encoded = LambdaParamPacket().pack()
        encoded[10] = 0x80; encoded[11] = 0x00
        val parsed = LambdaParamPacket().parse(frame(encoded, 0xCA, 0xFE)) as LambdaParamPacket
        assertEquals(-8192f, parsed.tempThrd, 0f)
        parsed.flags = 0x05
        assertArrayEquals(intArrayOf(0x80, 0x00), parsed.pack().sliceArray(10..11))
        assertArrayEquals(intArrayOf(0xCA, 0xFE), parsed.pack().takeLast(2).toIntArray())

        encoded[10] = 0x7F; encoded[11] = 0xFF
        val upper = LambdaParamPacket().parse(frame(encoded)) as LambdaParamPacket
        assertEquals(8191.75f, upper.tempThrd, 0f)
        assertArrayEquals(intArrayOf(0x7F, 0xFF), upper.pack().sliceArray(10..11))
    }
}
