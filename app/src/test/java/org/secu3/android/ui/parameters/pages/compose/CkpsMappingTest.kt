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
package org.secu3.android.ui.parameters.pages.compose

import org.junit.Assert.*
import org.junit.Test
import org.secu3.android.models.packets.out.params.CkpsParamPacket

class CkpsMappingTest {
    @Test
    fun everyNumericFieldMapsAndPreservesUnknownPacketData() {
        val source = CkpsParamPacket(
            ckpsCogsBtdc = 24,
            ckpsIgnitCogs = 7,
            ckpsEngineCyl = 4,
            ckpsCogsNum = 60,
            ckpsMissNum = 2,
            hallFlags = 0xE0,
            hallWndWidth = 12.3f,
            hallDegreesBtdc = 45.6f,
            mttf = 2.5f,
        )
        source.ckpsEdge = true
        source.risingSpark = true

        val encodedPacket = source.pack()
        val trailingBytes = intArrayOf(231, 232)
        val response = intArrayOf(64, encodedPacket.first()) +
            encodedPacket.drop(1).toIntArray() + trailingBytes
        val packet = CkpsParamPacket().parse(response) as CkpsParamPacket

        val initialValues = CkpsValues.from(packet)
        assertEquals(60f, initialValues.numbers.getValue(CkpsNumber.WHEEL))
        assertEquals(2f, initialValues.numbers.getValue(CkpsNumber.MISSING))
        assertEquals(7f, initialValues.numbers.getValue(CkpsNumber.PULSE))
        assertEquals(12.3f, initialValues.numbers.getValue(CkpsNumber.HALL_WIDTH), 0.02f)
        assertEquals(45.6f, initialValues.numbers.getValue(CkpsNumber.DEGREES), 0.02f)
        assertEquals(2.5f, initialValues.numbers.getValue(CkpsNumber.MTTF))
        assertTrue(initialValues.ckpsEdge)
        assertTrue(initialValues.risingSpark)

        CkpsNumber.entries.forEach { number ->
            val value = when (number) {
                CkpsNumber.WHEEL -> 80f
                CkpsNumber.MISSING -> 3f
                CkpsNumber.PULSE -> 9f
                CkpsNumber.HALL_WIDTH -> 13.4f
                CkpsNumber.DEGREES -> 46.7f
                CkpsNumber.MTTF -> 7.25f
            }
            applyCkpsNumber(packet, number, value)
        }

        val updatedValues = CkpsValues.from(packet)
        assertEquals(80f, updatedValues.numbers.getValue(CkpsNumber.WHEEL))
        assertEquals(3f, updatedValues.numbers.getValue(CkpsNumber.MISSING))
        assertEquals(9f, updatedValues.numbers.getValue(CkpsNumber.PULSE))
        assertEquals(13.4f, updatedValues.numbers.getValue(CkpsNumber.HALL_WIDTH))
        assertEquals(46.7f, updatedValues.numbers.getValue(CkpsNumber.DEGREES))
        assertEquals(7.25f, updatedValues.numbers.getValue(CkpsNumber.MTTF))
        assertEquals(0xE5, packet.hallFlags)
        assertArrayEquals(trailingBytes, packet.pack().takeLast(2).toIntArray())
    }

    @Test
    fun allFlagsPreserveUnknownBitsAndCamSetterChangesPackedBit() {
        val packet = CkpsParamPacket(hallFlags = 0xA0)
        applyCkpsFlag(packet, CkpsFlag.CKPS_EDGE, true)
        applyCkpsFlag(packet, CkpsFlag.REFERENCE_EDGE, true)
        applyCkpsFlag(packet, CkpsFlag.SPARK_RISING, true)
        applyCkpsFlag(packet, CkpsFlag.MERGE_OUTPUTS, true)
        applyCkpsFlag(packet, CkpsFlag.USE_CAM_REFERENCE, true)

        assertEquals(0xBF, packet.hallFlags)
        assertTrue(packet.useCamRef)
        assertEquals(0xBF, packet.pack()[6])

        packet.useCamRef = false
        assertEquals(0xBD, packet.hallFlags)
        assertEquals(0xA0, packet.hallFlags and 0xE0)
    }

    @Test
    fun choicesKeepUnknownValuesUntilValidExplicitSelection() {
        val packet = CkpsParamPacket(ckpsCogsBtdc = 56, ckpsEngineCyl = 3)
        assertEquals("Code 56", selectorValue(packet.ckpsCogsBtdc, (12..55).toList()))

        assertFalse(applyCkpsChoice(packet, true, 11))
        assertFalse(applyCkpsChoice(packet, false, 3))
        assertEquals(56, packet.ckpsCogsBtdc)
        assertEquals(3, packet.ckpsEngineCyl)

        assertTrue(applyCkpsChoice(packet, true, 55))
        assertTrue(applyCkpsChoice(packet, false, 8))
        assertEquals(55, packet.ckpsCogsBtdc)
        assertEquals(8, packet.ckpsEngineCyl)
    }

    @Test
    fun missingTeethFactorUsesLegacyLookupButZeroPreservesValue() {
        val expected = listOf(1.5f, 2.5f, 3f, 4f, 5f, 6f)
        expected.forEachIndexed { index, value ->
            val packet = CkpsParamPacket(mttf = 7f)
            applyCkpsNumber(packet, CkpsNumber.MISSING, (index + 1).toFloat())
            assertEquals(value, packet.mttf)
        }

        val zeroMissing = CkpsParamPacket(mttf = 3.75f)
        applyCkpsNumber(zeroMissing, CkpsNumber.MISSING, 0f)
        assertEquals(3.75f, zeroMissing.mttf)
    }

    @Test
    fun valuesExposeAllRangesTypesAndCurrentFields() {
        val packet = CkpsParamPacket(
            ckpsCogsNum = 40,
            ckpsMissNum = 2,
            ckpsIgnitCogs = 5,
            hallWndWidth = 12f,
            hallDegreesBtdc = 40f,
            mttf = 2f,
        )

        assertEquals(6, CkpsValues.from(packet).numbers.size)
        assertEquals(16f, CkpsNumber.WHEEL.min)
        assertEquals(200f, CkpsNumber.WHEEL.max)
        assertEquals(0f, CkpsNumber.MISSING.min)
        assertEquals(6f, CkpsNumber.MISSING.max)
        assertEquals(.1f, CkpsNumber.HALL_WIDTH.step)
        assertEquals(1, CkpsNumber.DEGREES.precision)
    }

    private fun selectorValue(value: Int, options: List<Int>): String =
        value.toString().takeIf { value in options } ?: "Code $value"
}
