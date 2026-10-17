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
import org.secu3.android.models.packets.out.params.TemperatureParamPacket

class TemperaturePacketMappingTest {
    @Test
    fun mapsEveryNumberAndFlagAndPreservesUnknownFlagBits() {
        val packet = TemperatureParamPacket(
            tmpFlags = 0b1000_0000,
            ventOn = 95.4f,
            ventOff = 88.2f,
            ventPwmFrq = 100,
            condPvtOn = 1.234f,
            condPvtOff = 2.345f,
            condMinRpm = 900,
            ventTmr = 20f,
        )
        TemperatureFlag.entries.forEach { applyTemperatureFlag(packet, it, it.ordinal % 2 == 0) }
        val values = TemperatureValues.from(packet)
        assertEquals(listOf(95.4f, 88.2f, 100f, 1.234f, 2.345f, 900f, 20f), TemperatureNumber.entries.map(values::value))
        assertEquals(listOf(true, false, true), TemperatureFlag.entries.map { values.flags.getValue(it) })

        val original = packet
        val edits = listOf(99.2f, 90.6f, 250f, 3.21f, 4.32f, 1200f, 30f)
        TemperatureNumber.entries.forEachIndexed { index, number -> applyTemperatureValue(packet, number, edits[index]) }
        assertSame(original, packet)
        assertEquals(edits, listOf(packet.ventOn, packet.ventOff, packet.ventPwmFrq.toFloat(), packet.condPvtOn, packet.condPvtOff, packet.condMinRpm.toFloat(), packet.ventTmr))
        assertEquals(0b1000_0000, packet.tmpFlags and 0b1000_0000)
        TemperatureFlag.entries.forEach { applyTemperatureFlag(packet, it, true) }
        assertEquals(0b1000_0000, packet.tmpFlags and 0b1000_0000)
        assertEquals(0b1000_0111, packet.tmpFlags)
    }

    @Test
    fun metadataMatchesLegacyXmlAndManagerAcRangesWithThreeIntegerEditors() {
        assertEquals(listOf(TemperatureNumber.PWM_FREQUENCY, TemperatureNumber.AC_MIN_RPM, TemperatureNumber.FAN_TIMER), TemperatureNumber.entries.filter { it.integer })
        assertEquals(listOf(2, 2, 0, 4, 4, 0, 0), TemperatureNumber.entries.map { it.precision })
        assertEquals(listOf(-10f, -10f, 10f, 0f, 0f, 500f, 0f), TemperatureNumber.entries.map { it.min })
        assertEquals(listOf(125f, 125f, 5000f, 5f, 5f, 3000f, 600f), TemperatureNumber.entries.map { it.max })
        assertEquals(listOf(.2f, .2f, 1f, .01f, .01f, 10f, 1f), TemperatureNumber.entries.map { it.step })
        assertEquals(3, TemperatureFlag.entries.size)
    }

    @Test
    fun fanTimerMappingRetainsCentisecondsAndEncodesRoundedWireValue() {
        val packet = TemperatureParamPacket(ventTmr = 1.75f)
        assertEquals(1.75f, TemperatureValues.from(packet).value(TemperatureNumber.FAN_TIMER), 0f)
        applyTemperatureValue(packet, TemperatureNumber.FAN_TIMER, 30f)
        assertEquals(30f, packet.ventTmr, 0f)
        assertEquals(0x0B, packet.pack()[14])
        assertEquals(0xB8, packet.pack()[15])
        assertEquals(128, TemperatureParamPacket(ventTmr = 1.275f).pack()[15])
    }
}
