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
import org.secu3.android.models.packets.base.OutputPacket
import org.secu3.android.models.packets.out.params.ChokeControlParPacket

class ChokePacketMappingTest {
    @Test fun mapsFieldsAndRetainsUnknownFlagBits() {
        val p = ChokeControlParPacket(flags = 0b10000000, smFreq = 2)
        ChokeNumber.entries.filter { it !in listOf(ChokeNumber.CLOSED_LOOP, ChokeNumber.DISABLE_ON_GAS, ChokeNumber.THROTTLE_INIT, ChokeNumber.MAX_FREQUENCY_INIT) }
            .forEach { applyChokeNumber(p, it, when (it) {
                ChokeNumber.STEPS -> 900f; ChokeNumber.REGULATOR_FACTOR -> .75f; ChokeNumber.COLD_DURATION -> 4f
                ChokeNumber.HOT_DURATION -> 5f; ChokeNumber.CRANK_TO_RUN -> 12f; else -> 0f
            }) }
        p.useClosedLoopRmpRegulator = true
        p.dontUseRpmRegOnGas = true
        p.useThrottlePosInChokeInit = true
        p.maxSTEPfreqAtInit = false
        assertEquals(0b10000111, p.flags)
        val mapped = ChokeValues.from(p)
        assertEquals(listOf(900f, .75f, 4f, 5f, 12f), ChokeNumber.entries.take(5).map(mapped::value))
        assertEquals(2, mapped.frequency)
    }

    @Test fun manualDeltaClampsAndNegativeDeltaPacksAsSignedByteWithoutTouchingFlags() {
        val delta = ChokeManualDelta()
        repeat(200) { delta.step(-1) }
        assertEquals(-127, delta.value)
        val p = ChokeControlParPacket(
            flags = 0b10100000,
            manualPositionD = delta.value,
            rpmIf = 42.5f,
            corrTime0 = 1.25f,
            corrTime1 = 2.5f,
            smFreq = 9,
            injCrankToRunTime = 3.75f
        )
        val packed = p.pack()
        assertEquals(129, packed[4])
        assertEquals(0b10100000, p.flags)
        val incomingPayload = packed.drop(1).toIntArray()
        incomingPayload[3] = 0 // Firmware reports zero for the outgoing-only manual command byte.
        val incoming = intArrayOf(0x40, 0x25) + incomingPayload + intArrayOf(0xD1, 0xE2)
        val parsed = ChokeControlParPacket().parse(incoming) as ChokeControlParPacket
        assertEquals(0, parsed.manualPositionD)
        assertEquals(42.5f, parsed.rpmIf)
        assertEquals(1.25f, parsed.corrTime0)
        assertEquals(2.5f, parsed.corrTime1)
        assertEquals(9, parsed.smFreq)
        assertEquals(3.75f, parsed.injCrankToRunTime)
        assertEquals(0b10100000, parsed.flags)
        assertEquals(0xD1, parsed.pack()[packed.size])
        assertEquals(0xE2, parsed.pack()[packed.size + 1])
        val asyncSnapshot: OutputPacket = parsed.snapshotWithManualPosition(-127)
        assertEquals(0, parsed.manualPositionD)
        parsed.manualPositionD = 0
        val snapshotBytes = asyncSnapshot.pack()
        assertEquals(129, snapshotBytes[4])
        assertEquals(0b10100000, snapshotBytes[11])
        assertEquals(0xD1, snapshotBytes[packed.size])
        assertEquals(0xE2, snapshotBytes[packed.size + 1])
        snapshotBytes[4] = 0
        assertEquals(129, asyncSnapshot.pack()[4])
        delta.clear()
        assertEquals(0, delta.value)
    }
}
