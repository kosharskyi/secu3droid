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
