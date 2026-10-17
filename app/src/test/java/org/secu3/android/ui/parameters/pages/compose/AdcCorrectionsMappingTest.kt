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

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test
import org.secu3.android.models.packets.out.params.AdcCorrectionsParamPacket

class AdcCorrectionsMappingTest {
    @Test
    fun mapsAndWritesAllTwentyFourValuesOnOriginalPacket() {
        val packet = AdcCorrectionsParamPacket(adcFlags = 0b1010_1100)
        val values = AdcCorrectionsValues.from(packet)
        assertEquals(24, values.numbers.size)
        assertEquals(List(24) { 0f }, AdcNumber.entries.map(values::value))
        val originalPacket = packet
        AdcNumber.entries.forEachIndexed { index, field -> packet.setAdcValue(field, (index + 1) / 10f) }
        assertEquals(AdcNumber.entries.map { (it.ordinal + 1) / 10f }, AdcNumber.entries.map(packet::adcValue))
        assertSame(originalPacket, packet)
        assertEquals(0b1010_1100, packet.adcFlags and 0b1111_1110)
        assertEquals(listOf(3, 3, 3, 4) + List(20) { if (it % 2 == 0) 3 else 4 }, AdcNumber.entries.map { it.precision })
        assertEquals(AdcNumber.entries.map { if (it.name.endsWith("FACTOR")) 0f else -2f }, AdcNumber.entries.map { it.min })
        assertEquals(AdcNumber.entries.map { if (it.name.endsWith("FACTOR")) 3.999f else 1.999f }, AdcNumber.entries.map { it.max })
        assertEquals(AdcNumber.entries.map { if (it.name.endsWith("CORRECTION")) .0025f else .001f }, AdcNumber.entries.map { it.step })
        assertEquals(AdcNumber.entries.map { if (it.name.endsWith("CORRECTION")) "V" else "" }, AdcNumber.entries.map { it.units })
    }

    @Test
    fun switchChangesOnlyCompensationBitAndPacketRetainsTrailingData() {
        val packet = AdcCorrectionsParamPacket(adcFlags = 0b1010_1100)
        val before = packet.adcFlags and 0b1111_1110
        packet.adcCompMode = true
        assertEquals(before, packet.adcFlags and 0b1111_1110)
        assertEquals(1, packet.adcFlags and 1)
        packet.adcCompMode = false
        assertEquals(before, packet.adcFlags)

        val withUnknown = IntArray(77) { 0 }.also { it[75] = 0x55; it[76] = 0x66 }
        packet.parse(withUnknown)
        assertEquals(listOf(0x55, 0x66), packet.pack().takeLast(2))
    }
}
