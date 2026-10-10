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
import org.secu3.android.models.packets.out.params.AnglesParamPacket

class AnglesPacketMappingTest {
    @Test fun packetMapsAllDisplayedValuesAndFlags() {
        val packet = AnglesParamPacket(45f, -12f, -5.5f, 1.2f, 2.3f, 2, 0b101, -3.4f)
        val values = AnglesValues.from(packet)
        assertEquals(45f, values.value(AnglesNumber.MAX))
        assertEquals(-12f, values.value(AnglesNumber.MIN))
        assertEquals(1.2f, values.value(AnglesNumber.DECREASE))
        assertEquals(2.3f, values.value(AnglesNumber.INCREASE))
        assertEquals(-5.5f, values.value(AnglesNumber.OCTANE))
        assertEquals(-3.4f, values.value(AnglesNumber.SHIFT))
        assertEquals(true, values.flags.getValue(AnglesFlag.ZERO))
        assertEquals(true, values.flags.getValue(AnglesFlag.WORKING_MAP))
        assertEquals(false, values.flags.getValue(AnglesFlag.MANUAL_CORRECTION))
        assertEquals(true, values.flags.getValue(AnglesFlag.ZERO_WITH_OCTANE))
    }

    @Test fun editsMutateTheOriginalPacketAndFlagBitsPreserveUnrelatedBits() {
        val packet = AnglesParamPacket(igntimFlags = 0b10101000)
        val original = packet
        AnglesNumber.entries.forEach { applyAnglesValue(packet, it, -2f - it.ordinal) }
        assertEquals(-2f, packet.maxAngle)
        assertEquals(-3f, packet.minAngle)
        assertEquals(-4f, packet.angleDecSpeed)
        assertEquals(-5f, packet.angleIncSpeed)
        assertEquals(-6f, packet.angleCorrection)
        assertEquals(-7f, packet.shift_ingtim)
        AnglesFlag.WORKING_MAP.let { applyAnglesFlag(packet, it, true) }
        assertEquals(0b10101001, packet.igntimFlags)
        applyAnglesFlag(packet, AnglesFlag.MANUAL_CORRECTION, true)
        assertEquals(0b10101011, packet.igntimFlags)
        applyAnglesFlag(packet, AnglesFlag.ZERO_WITH_OCTANE, true)
        assertEquals(0b10101111, packet.igntimFlags)
        applyAnglesFlag(packet, AnglesFlag.ZERO, false)
        assertEquals(0, packet.zeroAdvAngle)
        applyAnglesFlag(packet, AnglesFlag.ZERO, true)
        assertEquals(1, packet.zeroAdvAngle)
        assertSame(original, packet)
    }
}
