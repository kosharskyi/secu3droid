/*
 *    SecuDroid  - An open source, free manager for SECU-3 engine control unit
 *    Copyright (C) 2026 Vitalii O. Kosharskyi. Ukraine, Kyiv
 *
 *    SECU-3  - An open source, free engine control unit
 *    Copyright (C) 2007-2026 Alexey A. Shabelnikov. Ukraine, Kyiv
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *    contacts:
 *                    http://secu-3.org
 *                    email: vetalkosharskiy@gmail.com
 */
package org.secu3.android.models.packets

import org.junit.Assert.assertEquals
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.secu3.android.models.packets.out.params.MiscellaneousParamPacket
import kotlin.math.roundToInt

class MiscellaneousParamPacketTest {
    private fun vssFrame(raw: Int): IntArray {
        val valid = MiscellaneousParamPacket(
            evapAfbegin = 5000,
            evapAfslope = 32f / 170000f,
            pwmFrq0 = 100,
            pwmFrq1 = 200,
            vssPeriodDist = 1000f,
        ).pack()
        return (intArrayOf(0x40, 'z'.code) + valid.drop(1).toIntArray() + intArrayOf(0xCA, 0xFE)).also {
            it[22] = raw ushr 8
            it[23] = raw and 255
        }
    }

    @Test fun ignitionCutoffThresholdDecodesSignedWordsAndSurvivesUnrelatedEdits() {
        val expected = listOf(0 to 0, 0x7FFF to 32767, 0x8000 to -32768, 0xFFFF to -1)
        for ((raw, value) in expected) {
            val incoming = vssFrame(32768).also {
                it[6] = raw ushr 8
                it[7] = raw and 255
            }.copyOfRange(0, 24) + intArrayOf(0xCA, 0xFE)

            val packet = MiscellaneousParamPacket().parse(incoming) as MiscellaneousParamPacket
            assertEquals(value, packet.ignCutoffThrd)
            packet.uartPeriodTms = 30
            assertArrayEquals(intArrayOf(raw ushr 8, raw and 255), packet.pack().sliceArray(5..6))
            assertArrayEquals(intArrayOf(0xCA, 0xFE), packet.pack().takeLast(2).toIntArray())
        }
    }

    @Test fun vssReciprocalRetainsIncomingWireWordAcrossUnrelatedEdits() {
        val decoded = mutableListOf<Float>()
        for (raw in listOf(65275, 65290, 1, 257, 32768, 65535)) {
            val packet = MiscellaneousParamPacket().parse(vssFrame(raw)) as MiscellaneousParamPacket
            assertEquals(32768000f / raw.toFloat(), packet.vssPeriodDist, 0f)
            packet.uartPeriodTms = 30
            packet.offPumpOnGas = true
            val packed = packet.pack()
            assertArrayEquals(intArrayOf(raw ushr 8, raw and 255), packed.sliceArray(21..22))
            assertArrayEquals(intArrayOf(0xCA, 0xFE), packed.takeLast(2).toIntArray())
            decoded += packet.vssPeriodDist
        }
        assertEquals(502, decoded[0].roundToInt())
        assertEquals(502, decoded[1].roundToInt())
        assertTrue(decoded[0] != decoded[1])
    }

    @Test fun hopAnglesRetainSignedAndUnsignedWirePrecisionAcrossUnrelatedEdits() {
        for ((raws, expected) in listOf(
            intArrayOf(0x0021, 0x0041) to (33f / 32f to 65f / 32f),
            intArrayOf(0xFFDF, 0xFFFF) to (-33f / 32f to 65535f / 32f),
        )) {
            val startRaw = raws[0]
            val durationRaw = raws[1]
            val incoming = vssFrame(32768).also {
                it[8] = startRaw ushr 8; it[9] = startRaw and 255
                it[10] = durationRaw ushr 8; it[11] = durationRaw and 255
            }
            val packet = MiscellaneousParamPacket().parse(incoming) as MiscellaneousParamPacket
            assertEquals(expected.first, packet.hopStartAng, 0f)
            assertEquals(expected.second, packet.hopDuratAng, 0f)

            packet.uartPeriodTms = 30
            val packed = packet.pack()
            assertArrayEquals(intArrayOf(startRaw ushr 8, startRaw and 255, durationRaw ushr 8, durationRaw and 255), packed.sliceArray(7..10))
            assertArrayEquals(intArrayOf(0xCA, 0xFE), packed.takeLast(2).toIntArray())
        }

        val negativeHalfTie = MiscellaneousParamPacket(
            evapAfbegin = 5000,
            evapAfslope = 32f / 170000f,
            pwmFrq0 = 100,
            pwmFrq1 = 200,
            vssPeriodDist = 1000f,
            hopStartAng = -.015625f,
        )
        assertArrayEquals(intArrayOf(0xFF, 0xFF), negativeHalfTie.pack().sliceArray(7..8))
    }

    @Test fun evapSlopeUsesExpectedRawWordAndRoundTrips() {
        val packet = MiscellaneousParamPacket(uartDivisor=0x0103, evapAfbegin=5000, evapAfslope=32f/170000f)
        val packed = packet.pack()
        assertEquals(0x18, packed[14])
        assertEquals(0xAC, packed[15])
        val incoming = intArrayOf(0x40,0x21) + packed.drop(1).toIntArray() + intArrayOf(0xA5,0x5A)
        val parsed = MiscellaneousParamPacket().parse(incoming) as MiscellaneousParamPacket
        assertEquals(6316f/33554432f, parsed.evapAfslope, 0.0000001f)
        assertEquals(0x0103, parsed.uartDivisor)
        assertTrue(parsed.pack().takeLast(2) == listOf(0xA5,0x5A))
    }

    @Test fun evapStartUsesManagerRoundingAtHalfCountsAndBoundaries() {
        val expected = listOf(100 to 3, 1000 to 31, 1007 to 31, 1008 to 32, 1016 to 32, 1024 to 32, 2_000_000 to 62_500)
        for ((begin, word) in expected) {
            val packet = MiscellaneousParamPacket(evapAfbegin = begin, evapAfslope = 32f / 20_000f)
            assertArrayEquals(intArrayOf(word ushr 8, word and 0xFF), packet.pack().sliceArray(12..13))
        }
    }

    @Test fun incomingEvapStartWordAndUnknownTailSurviveUnrelatedEdit() {
        val incoming = vssFrame(32768).also {
            it[13] = 0x12
            it[14] = 0x34
        }
        val packet = MiscellaneousParamPacket().parse(incoming) as MiscellaneousParamPacket
        assertEquals(0x1234 * 32, packet.evapAfbegin)
        packet.uartPeriodTms = 30
        assertArrayEquals(intArrayOf(0x12, 0x34), packet.pack().sliceArray(12..13))
        assertArrayEquals(intArrayOf(0xCA, 0xFE), packet.pack().takeLast(2).toIntArray())
    }

    @Test fun invalidEvapSlopeCannotBePackedAndEndRequiresSafeDelta() {
        val packet = MiscellaneousParamPacket(evapAfbegin=5000, evapAfslope=0f)
        var failed=false
        try { packet.pack() } catch (_: IllegalArgumentException) { failed=true }
        assertTrue(failed)
        failed=false
        try { packet.evapAfEnd=21499 } catch (_: IllegalArgumentException) { failed=true }
        assertTrue(failed)
        packet.evapAfEnd=21500
        assertTrue(packet.evapAfslope > 0f)
        assertFalse(packet.evapAfslope.isNaN())
        packet.pack()
        assertTrue(packet.evapAfEnd-packet.evapAfbegin >= MiscellaneousParamPacket.MIN_EVAP_AIRFLOW_DELTA)
    }

    @Test fun flagWritesPreserveUnknownBits() {
        val packet=MiscellaneousParamPacket(flpmpFlags=0xA8)
        packet.offPumpOnGas=true
        packet.offInjOnGas=true
        packet.offInjOnPetrol=false
        assertEquals(0xAB,packet.flpmpFlags)
    }

    @Test fun editingParsedPacketPreservesOtherParametersAndUnknownTail() {
        val source=MiscellaneousParamPacket(uartDivisor=0x0103,uartPeriodTms=20,ignCutoff=1,ignCutoffThrd=4200,hopStartAng=-2f,hopDuratAng=5f,flpmpFlags=0xA9,evapAfbegin=5000,evapAfslope=32f/170000f,fpTimeoutStrt=2.5f,pwmFrq0=100,pwmFrq1=200,vssPeriodDist=1000f)
        val incoming=intArrayOf(0x40,0x21)+source.pack().drop(1).toIntArray()+intArrayOf(0xD1,0xE2)
        val parsed=MiscellaneousParamPacket().parse(incoming) as MiscellaneousParamPacket
        parsed.uartPeriodTms=30
        val repacked=parsed.pack()
        assertEquals(0x0103,(repacked[1] shl 8) or repacked[2])
        assertEquals(3,repacked[3])
        assertEquals(1,repacked[4])
        assertEquals(4200,(repacked[5] shl 8) or repacked[6])
        assertEquals(-2f,parsed.hopStartAng, 0f)
        assertEquals(0xA9,parsed.flpmpFlags)
        assertEquals(25,repacked[16])
        assertEquals(100,parsed.pwmFrq0)
        assertEquals(200,parsed.pwmFrq1)
        assertEquals(1000f,parsed.vssPeriodDist, 0f)
        assertEquals(0xD1,repacked[23])
        assertEquals(0xE2,repacked[24])
    }

}
