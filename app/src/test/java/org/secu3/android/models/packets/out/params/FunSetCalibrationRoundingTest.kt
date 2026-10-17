package org.secu3.android.models.packets.out.params

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class FunSetCalibrationRoundingTest {
    private data class Calibration(
        val name: String,
        val rxStart: Int,
        val txStart: Int,
        val physicalPerCount: Float,
        val decode: (Int) -> Float,
        val set: (FunSetParamPacket, Float) -> Unit,
        val get: (FunSetParamPacket) -> Float,
    )

    private val calibrations = listOf(
        Calibration("MAP offset", 8, 7, .0025f, { it * .0025f }, { p, v -> p.mapCurveOffset = v }, { it.mapCurveOffset }),
        Calibration("MAP gradient", 10, 9, 1f / 20.48f, { it / 20.48f }, { p, v -> p.mapCurveGradient = v }, { it.mapCurveGradient }),
        Calibration("MAP2 offset", 12, 11, .0025f, { it * .0025f }, { p, v -> p.map2CurveOffset = v }, { it.map2CurveOffset }),
        Calibration("MAP2 gradient", 14, 13, 1f / 20.48f, { it / 20.48f }, { p, v -> p.map2CurveGradient = v }, { it.map2CurveGradient }),
        Calibration("TPS offset", 16, 15, .0025f, { it * .0025f }, { p, v -> p.tpsCurveOffset = v }, { it.tpsCurveOffset }),
        Calibration("TPS gradient", 18, 17, 1f / 40.96f, { it / 40.96f }, { p, v -> p.tpsCurveGradient = v }, { it.tpsCurveGradient }),
        Calibration("GPS offset", 35, 32, .0025f, { it * .0025f }, { p, v -> p.gpsCurveOffset = v }, { it.gpsCurveOffset }),
        Calibration("GPS gradient", 37, 34, 1f / 20.48f, { it / 20.48f }, { p, v -> p.gpsCurveGradient = v }, { it.gpsCurveGradient }),
        Calibration("FPS offset", 39, 36, .0025f, { it * .0025f }, { p, v -> p.fpsCurveOffset = v }, { it.fpsCurveOffset }),
        Calibration("FPS gradient", 41, 38, 1f / 20.48f, { it / 20.48f }, { p, v -> p.fpsCurveGradient = v }, { it.fpsCurveGradient }),
        Calibration("APPS1 offset", 43, 40, .0025f, { it * .0025f }, { p, v -> p.apps1CurveOffset = v }, { it.apps1CurveOffset }),
        Calibration("APPS1 gradient", 45, 42, 1f / 40.96f, { it / 40.96f }, { p, v -> p.apps1CurveGradient = v }, { it.apps1CurveGradient }),
    )

    private fun word(bytes: IntArray, start: Int): Int =
        ((bytes[start] and 0xFF) shl 8) or (bytes[start + 1] and 0xFF)

    private fun putWord(bytes: IntArray, start: Int, value: Int) {
        bytes[start] = value ushr 8 and 0xFF
        bytes[start + 1] = value and 0xFF
    }

    @Test fun allTwelveSignedCalibrationWordsUseProtocolHalfAwayRounding() {
        val cases = listOf(-.5f to -1, .5f to 1, -1.5f to -2, 1.5f to 2, -.4f to 0, .4f to 0)
        for (field in calibrations) {
            for ((counts, expected) in cases) {
                val packet = FunSetParamPacket()
                field.set(packet, counts * field.physicalPerCount)
                val actual = word(packet.pack(), field.txStart)
                assertEquals("${field.name}: $counts counts", expected and 0xFFFF, actual)
            }
        }
    }

    @Test fun receiveFixtureHasReadOnlyWordsAndPreservesAllCalibrationWordsAndTail() {
        val frame = IntArray(2 + 47 + 2)
        frame[0] = '@'.code
        frame[1] = FunSetParamPacket.DESCRIPTOR.code
        val rawWords = calibrations.mapIndexed { index, field ->
            val word = when (index) {
                0 -> 0x8000
                11 -> 0x7FFF
                else -> 0xFF00 + index * 3
            }
            putWord(frame, field.rxStart, word)
            word
        }
        frame[49] = 0xCA
        frame[50] = 0xFE

        assertEquals("RX carries 47 payload bytes plus two-byte tail", 51, frame.size)
        val packet = FunSetParamPacket().parse(frame) as FunSetParamPacket
        for ((index, field) in calibrations.withIndex()) {
            assertEquals(field.name, field.decode(rawWords[index].toShort().toInt()), field.get(packet), 0f)
        }

        packet.funcFlags = 0x05
        val transmitted = packet.pack()
        assertEquals("TX omits the four read-only ADC payload bytes", 46, transmitted.size)
        for ((index, field) in calibrations.withIndex()) {
            assertEquals(field.name, rawWords[index], word(transmitted, field.txStart))
        }
        assertArrayEquals(intArrayOf(0xCA, 0xFE), transmitted.takeLast(2).toIntArray())
    }
}
