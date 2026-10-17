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
import org.secu3.android.models.packets.out.params.IdlingParamPacket

class IdlingPacketMappingTest {
    @Test
    fun mapsEveryNumberAndFlagAndEditsSamePacketPreservingUnknownFlagBits() {
        val packet = IdlingParamPacket(
            idlFlags = 0b10000000,
            iFac1 = 1.1f,
            iFac2 = 1.2f,
            minefr = 30,
            idlingRpm = 800,
            idlregMinAngle = -5f,
            idlregMaxAngle = 20f,
            idlregTurnOnTemp = 50f,
            idlToRunAdd = 10f,
            rpmOnRunAdd = 100,
            idlRegP0 = .1f,
            idlRegI0 = .2f,
            idlRegP1 = .3f,
            idlRegI1 = .4f,
            coefThrd1 = 1.2f,
            coefThrd2 = 1.3f,
            integratorRpmLim = 500,
            mapValue = 40f,
            iacMinPos = 10f,
            iacMaxPos = 90f,
            iacRegDb = 20,
            idlRegD = .5f,
            irrKLoad = 3f,
            irrKRpm = 4f,
            loadIdlThrd = 5f,
        )
        IdlingFlag.entries.forEach { applyIdlingFlag(packet, it, it.ordinal % 2 == 0) }

        val values = IdlingValues.from(packet)
        assertEquals(
            listOf(1.1f, 1.2f, 20f, -5f, 800f, 30f, 50f, 10f, 100f, .1f, .2f, .3f, .4f, 1.2f, 1.3f, 500f, 40f, 10f, 90f, 20f, .5f, 3f, 4f, 5f),
            IdlingNumber.entries.map(values::value),
        )
        assertEquals(
            listOf(true, false, true, false, true, false, true),
            IdlingFlag.entries.map { values.flags.getValue(it) },
        )

        val originalPacket = packet
        val edits = IdlingNumber.entries.associateWith { 10f + it.ordinal }
        edits.forEach { (number, value) -> applyIdlingValue(packet, number, value) }
        assertSame(originalPacket, packet)
        assertEquals(10f, packet.iFac1)
        assertEquals(11f, packet.iFac2)
        assertEquals(12f, packet.idlregMaxAngle)
        assertEquals(13f, packet.idlregMinAngle)
        assertEquals(14, packet.idlingRpm)
        assertEquals(15, packet.minefr)
        assertEquals(16f, packet.idlregTurnOnTemp)
        assertEquals(17f, packet.idlToRunAdd)
        assertEquals(18, packet.rpmOnRunAdd)
        assertEquals(19f, packet.idlRegP0)
        assertEquals(20f, packet.idlRegI0)
        assertEquals(21f, packet.idlRegP1)
        assertEquals(22f, packet.idlRegI1)
        assertEquals(23f, packet.coefThrd1)
        assertEquals(24f, packet.coefThrd2)
        assertEquals(25, packet.integratorRpmLim)
        assertEquals(26f, packet.mapValue)
        assertEquals(27f, packet.iacMinPos)
        assertEquals(28f, packet.iacMaxPos)
        assertEquals(29, packet.iacRegDb)
        assertEquals(30f, packet.idlRegD)
        assertEquals(31f, packet.irrKLoad)
        assertEquals(32f, packet.irrKRpm)
        assertEquals(33f, packet.loadIdlThrd)

        assertEquals(0b10000000, packet.idlFlags and 0b10000000)
        IdlingFlag.entries.forEach { applyIdlingFlag(packet, it, true) }
        assertEquals(0b10000000, packet.idlFlags and 0b10000000)
        assertEquals(true, packet.useClosedLoopOnGas)
        assertEquals(true, packet.idlRegWorksWithIAC)
    }

    @Test
    fun metadataMatchesLegacyParameterLimitsAndIntegerDialogFields() {
        assertEquals(24, IdlingNumber.entries.size)
        val integerFields = listOf(
            IdlingNumber.RPM,
            IdlingNumber.DEAD_BAND,
            IdlingNumber.RPM_ADD,
            IdlingNumber.INTEGRATOR_RPM,
            IdlingNumber.IAC_DB,
        )
        assertEquals(integerFields, IdlingNumber.entries.filter { it.integer })
        assertEquals(listOf(0f, 0f, 0f, 0f, 0f), integerFields.map { it.min })
        assertEquals(listOf(1800f, 500f, 2000f, 1200f, 500f), integerFields.map { it.max })
        assertEquals(
            listOf(2, 2, 3, 3, 0, 0, 2, 1, 0, 3, 3, 3, 3, 2, 2, 0, 1, 1, 1, 0, 3, 2, 2, 1),
            IdlingNumber.entries.map { it.precision },
        )
        assertEquals(
            listOf(.01f, .01f, .025f, .025f, 5f, 1f, .2f, .5f, 10f, .005f, .005f, .005f, .005f, .01f, .01f, 10f, .1f, .5f, .5f, 1f, .005f, .05f, .05f, .1f),
            IdlingNumber.entries.map { it.step },
        )
        assertEquals(
            listOf(0f, 0f, -15f, -15f, 0f, 0f, -40f, 0f, 0f, 0f, 0f, 0f, 0f, 1f, 1f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f),
            IdlingNumber.entries.map { it.min },
        )
        assertEquals(
            listOf(10f, 10f, 30f, 30f, 1800f, 500f, 300f, 100f, 2000f, 5f, 5f, 5f, 5f, 2.99f, 2.99f, 1200f, 100f, 100f, 100f, 500f, 5f, 48f, 48f, 500f),
            IdlingNumber.entries.map { it.max },
        )
        assertEquals(3, IdlingNumber.MAX_ANGLE.precision)
        assertEquals(3, IdlingNumber.MIN_ANGLE.precision)
    }
}
