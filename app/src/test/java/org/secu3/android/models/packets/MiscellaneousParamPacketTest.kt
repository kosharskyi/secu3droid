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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.secu3.android.models.packets.out.params.MiscellaneousParamPacket
import org.secu3.android.ui.parameters.pages.evapEditBounds
import org.secu3.android.ui.parameters.pages.MiscField
import org.secu3.android.ui.parameters.pages.MiscellaneousNumericMapping
import org.secu3.android.ui.parameters.pages.miscFieldEnabled

class MiscellaneousParamPacketTest {
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
        val source=MiscellaneousParamPacket(uartDivisor=0x0103,uartPeriodTms=20,ignCutoff=1,ignCutoffThrd=4200,hopStartAng=-2,hopDuratAng=5,flpmpFlags=0xA9,evapAfbegin=5000,evapAfslope=32f/170000f,fpTimeoutStrt=2.5f,pwmFrq0=100,pwmFrq1=200,vssPeriodDist=1000)
        val incoming=intArrayOf(0x40,0x21)+source.pack().drop(1).toIntArray()+intArrayOf(0xD1,0xE2)
        val parsed=MiscellaneousParamPacket().parse(incoming) as MiscellaneousParamPacket
        parsed.uartPeriodTms=30
        val repacked=parsed.pack()
        assertEquals(0x0103,(repacked[1] shl 8) or repacked[2])
        assertEquals(3,repacked[3])
        assertEquals(1,repacked[4])
        assertEquals(4200,(repacked[5] shl 8) or repacked[6])
        assertEquals(-2,parsed.hopStartAng)
        assertEquals(0xA9,parsed.flpmpFlags)
        assertEquals(25,repacked[16])
        assertEquals(100,parsed.pwmFrq0)
        assertEquals(200,parsed.pwmFrq1)
        assertEquals(1000,parsed.vssPeriodDist)
        assertEquals(0xD1,repacked[23])
        assertEquals(0xE2,repacked[24])
    }

    @Test fun evapBoundsStayInsideXmlRangeAndRejectImpossibleIncomingPair() {
        assertEquals(100 to 158500,evapEditBounds(MiscField.BEGIN,5000,175000))
        assertEquals(21500 to 2_000_000,evapEditBounds(MiscField.END,5000,175000))
        assertEquals(100 to 100,evapEditBounds(MiscField.BEGIN,50,16600))
        assertEquals(null,evapEditBounds(MiscField.BEGIN,100,16599))
        assertEquals(215500 to 2_000_000,evapEditBounds(MiscField.END,199000,200000))
    }

    @Test fun hallRangesAndExpandedEvapUpperBoundAcceptDesktopSupportedEdges() {
        val packet = MiscellaneousParamPacket(evapAfbegin = 100, evapAfslope = 32f / 20000f)
        MiscellaneousNumericMapping.applyInt(packet, MiscField.START, -36)
        assertEquals(-36, packet.hopStartAng)
        MiscellaneousNumericMapping.applyInt(packet, MiscField.START, 72)
        assertEquals(72, packet.hopStartAng)
        MiscellaneousNumericMapping.applyInt(packet, MiscField.DURATION, 6)
        assertEquals(6, packet.hopDuratAng)
        MiscellaneousNumericMapping.applyInt(packet, MiscField.DURATION, 180)
        assertEquals(180, packet.hopDuratAng)
        MiscellaneousNumericMapping.applyInt(packet, MiscField.END, 2_000_000)
        assertTrue(packet.evapAfEnd in 1_995_000..2_000_000)
        assertTrue(packet.evapAfEnd - packet.evapAfbegin >= MiscellaneousParamPacket.MIN_EVAP_AIRFLOW_DELTA)
        packet.pack()

        listOf(MiscField.START to -37, MiscField.START to 73, MiscField.DURATION to 5, MiscField.DURATION to 181).forEach { (field, value) ->
            var rejected = false
            try { MiscellaneousNumericMapping.applyInt(packet, field, value) } catch (_: IllegalArgumentException) { rejected = true }
            assertTrue("$field should reject $value", rejected)
        }
    }

    @Test fun cutoffThresholdIsDisabledUntilIgnitionCutoffIsEnabled() {
        assertFalse(miscFieldEnabled(MiscField.CUTOFF, false))
        assertTrue(miscFieldEnabled(MiscField.CUTOFF, true))
        assertTrue(miscFieldEnabled(MiscField.START, false))
    }

    @Test fun numericMappingUpdatesEachFieldAndPreservesUnrelatedFieldsAndTail() {
        MiscField.entries.forEach { field ->
            val source = MiscellaneousParamPacket(
                uartDivisor = 0x0103,
                uartPeriodTms = 20,
                ignCutoff = 1,
                ignCutoffThrd = 4200,
                hopStartAng = -2,
                hopDuratAng = 5,
                flpmpFlags = 0xA9,
                evapAfbegin = 5000,
                evapAfslope = 32f / 170000f,
                fpTimeoutStrt = 2.5f,
                pwmFrq0 = 100,
                pwmFrq1 = 200,
                vssPeriodDist = 1000,
            )
            val input = intArrayOf(0x40, 0x21) + source.pack().drop(1).toIntArray() + intArrayOf(0xD1, 0xE2)
            val packet = MiscellaneousParamPacket().parse(input) as MiscellaneousParamPacket
            val oldEvapBegin = packet.evapAfbegin
            val oldEvapEnd = packet.evapAfEnd
            val oldSlope = packet.evapAfslope

            if (field == MiscField.TIMEOUT) {
                MiscellaneousNumericMapping.applyFloat(packet, field, 3.4f)
            } else {
                val value = when (field) {
                    MiscField.PERIOD -> 30
                    MiscField.CUTOFF -> 4100
                    MiscField.START -> -4
                    MiscField.DURATION -> 10
                    MiscField.BEGIN -> 6000
                    MiscField.END -> 180000
                    MiscField.PWM0 -> 500
                    MiscField.PWM1 -> 600
                    MiscField.VSS -> 1200
                    MiscField.TIMEOUT -> error("Handled above")
                }
                MiscellaneousNumericMapping.applyInt(packet, field, value)
            }

            assertEquals(0x0103, packet.uartDivisor)
            assertEquals(if (field == MiscField.PERIOD) 30 else 20, packet.uartPeriodTms)
            assertEquals(1, packet.ignCutoff)
            assertEquals(if (field == MiscField.CUTOFF) 4100 else 4200, packet.ignCutoffThrd)
            assertEquals(if (field == MiscField.START) -4 else -2, packet.hopStartAng)
            assertEquals(if (field == MiscField.DURATION) 10 else 5, packet.hopDuratAng)
            assertEquals(0xA9, packet.flpmpFlags)
            assertEquals(if (field == MiscField.BEGIN) 6000 else oldEvapBegin, packet.evapAfbegin)
            assertTrue(packet.evapAfslope > 0f)
            if (field == MiscField.BEGIN) assertTrue(kotlin.math.abs(packet.evapAfEnd - oldEvapEnd) < 200)
            else if (field == MiscField.END) assertTrue(kotlin.math.abs(packet.evapAfEnd - 180000) < 200)
            else {
                assertEquals(oldEvapEnd, packet.evapAfEnd)
                assertEquals(oldSlope, packet.evapAfslope, 0f)
            }
            assertEquals(if (field == MiscField.TIMEOUT) 3.4f else 2.5f, packet.fpTimeoutStrt, 0.0001f)
            assertEquals(if (field == MiscField.PWM0) 500 else 100, packet.pwmFrq0)
            assertEquals(if (field == MiscField.PWM1) 600 else 200, packet.pwmFrq1)
            assertEquals(if (field == MiscField.VSS) 1200 else 1000, packet.vssPeriodDist)
            assertEquals(listOf(0xD1, 0xE2), packet.pack().takeLast(2))
        }
    }

    @Test fun validEvapEndEditRecoversZeroSlopePacketAndUnlocksOtherEdits() {
        val original = MiscellaneousParamPacket(
            uartDivisor = 0x0103,
            uartPeriodTms = 20,
            ignCutoff = 1,
            ignCutoffThrd = 4200,
            hopStartAng = -2,
            hopDuratAng = 5,
            flpmpFlags = 0xA9,
            evapAfbegin = 5000,
            evapAfslope = 32f / 170000f,
            fpTimeoutStrt = 2.5f,
            pwmFrq0 = 100,
            pwmFrq1 = 200,
            vssPeriodDist = 1000,
        )
        val incoming = intArrayOf(0x40, 0x21) + original.pack().drop(1).toIntArray() + intArrayOf(0xD1, 0xE2)
        incoming[15] = 0
        incoming[16] = 0
        val packet = MiscellaneousParamPacket().parse(incoming) as MiscellaneousParamPacket
        assertEquals(0f, packet.evapAfslope, 0f)

        MiscellaneousNumericMapping.applyInt(packet, MiscField.END, 30000)
        packet.pack()
        assertEquals(4992, packet.evapAfbegin)
        assertTrue(kotlin.math.abs(packet.evapAfEnd - 30000) < 20)
        assertEquals(20, packet.uartPeriodTms)
        assertEquals(4200, packet.ignCutoffThrd)
        assertEquals(-2, packet.hopStartAng)
        assertEquals(5, packet.hopDuratAng)
        assertEquals(0xA9, packet.flpmpFlags)
        assertEquals(2.5f, packet.fpTimeoutStrt, 0f)
        assertEquals(100, packet.pwmFrq0)
        assertEquals(200, packet.pwmFrq1)
        assertEquals(1000, packet.vssPeriodDist)
        assertEquals(listOf(0xD1, 0xE2), packet.pack().takeLast(2))

        MiscellaneousNumericMapping.applyInt(packet, MiscField.PERIOD, 30)
        assertEquals(30, packet.uartPeriodTms)
        packet.pack()
    }

    @Test fun minimumEvapDeltaRemainsSafeAfterWireQuantization() {
        listOf(17500, 200000).forEach { requestedEnd ->
            val packet = MiscellaneousParamPacket(evapAfbegin = 1000, evapAfslope = 32f / 20000f)
            MiscellaneousNumericMapping.applyInt(packet, MiscField.END, requestedEnd)
            val packed = packet.pack()
            val parsed = MiscellaneousParamPacket().parse(intArrayOf(0x40, 0x21) + packed.drop(1).toIntArray()) as MiscellaneousParamPacket
            assertTrue(parsed.evapAfEnd in 100..200000)
            assertTrue(parsed.evapAfEnd - parsed.evapAfbegin >= MiscellaneousParamPacket.MIN_EVAP_AIRFLOW_DELTA)
        }
    }
}
