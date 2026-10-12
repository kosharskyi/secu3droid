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
import org.junit.Test
import org.secu3.android.models.packets.out.params.DbwParamPacket

class DbwPacketMappingTest {
    @Test
    fun packetValuesMapInXmlOrderAndEditableFieldsApplyWithoutChangingHomePosition() {
        val packet = DbwParamPacket(.1f, .2f, .3f, 40f, 50f, .12f, 6f, 7f, 8f, 9f, 43.21f)
        val values = DbwValues.from(packet)
        assertEquals(DbwNumber.entries, values.numbers.keys.toList())
        assertEquals(listOf(.1f, .2f, .3f, 40f, 50f, .12f, 6f, 7f, 8f, 9f), DbwNumber.entries.map(values::value))
        assertEquals(43.21f, values.homePosition, 0f)

        DbwNumber.entries.forEach { applyDbwValue(packet, it, 11f + it.ordinal) }
        assertEquals(43.21f, packet.homePosition_edited, 0f)
        assertEquals(DbwNumber.entries.map { 11f + it.ordinal }, DbwNumber.entries.map { DbwValues.from(packet).value(it) })
    }

    @Test
    fun metadataMatchesLegacyDbwXml() {
        assertEquals(listOf(5, 5, 5, 1, 1, 2, 1, 1, 0, 2), DbwNumber.entries.map { it.precision })
        assertEquals(listOf(.00025f, .00025f, .00025f, .5f, .5f, .01f, .1f, .1f, 1f, .1f), DbwNumber.entries.map { it.step })
        assertEquals(listOf(5f, 5f, 5f, 100f, 100f, .2f, 15.9f, 15.9f, 25f, 15f), DbwNumber.entries.map { it.max })
    }
}
