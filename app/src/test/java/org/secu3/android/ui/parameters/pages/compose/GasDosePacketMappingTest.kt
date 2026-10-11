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
import org.junit.Assert.assertTrue
import org.junit.Test
import org.secu3.android.models.packets.base.OutputPacket
import org.secu3.android.models.packets.out.params.GasDoseParamPacket

class GasDosePacketMappingTest {
    @Test fun mapsScaledFieldsAndClampsManualDelta() {
        val p = GasDoseParamPacket()
        GasDoseNumber.entries.forEach { n -> applyGasDoseNumber(p, n, when(n) {
            GasDoseNumber.STEPS -> 900f
            GasDoseNumber.STOICHIOMETRIC_RATIO -> 14.7f
            GasDoseNumber.FUEL_CUT_CLOSING -> 35.5f
            else -> 12.5f
        }) }
        p.freq = 2
        p.maxFreqInit = 1
        val mapped = GasDoseValues.from(p)
        assertEquals(listOf(900f, 14.7f, 35.5f, 12.5f, 12.5f), GasDoseNumber.entries.map(mapped::value))
        assertEquals(2, mapped.frequency)
        assertTrue(mapped.maxFrequencyAtInit)
        val delta = GasDoseManualDelta()
        repeat(200) { delta.step(-1) }
        assertEquals(-127, delta.value)
    }

    @Test fun scalesAndConsumesPlaceholderAndSnapshotsSignedDelta() {
        val p = GasDoseParamPacket(steps = 321, testing = true, manualPositionD = -2, fcClosing = 30f,
            lambdaCorrLimitP = 12.5f, lambdaCorrLimitM = 20f, lambdaStoichval = 14.7f, freq = 3, maxFreqInit = 1)
        val bytes = p.pack()
        assertEquals(254, bytes[4])
        assertEquals(60, bytes[5])
        assertEquals(0, bytes[6])
        assertEquals(64, bytes[7])
        assertEquals(7, bytes[10])
        assertEquals(90, bytes[11])
        assertEquals(3, bytes[12])
        val receivedPayload = bytes.drop(1).toIntArray()
        receivedPayload[3] = 0
        val withExtra = intArrayOf('@'.code, '*'.code) + receivedPayload + intArrayOf(0xD1, 0xE2)
        val parsed = GasDoseParamPacket(manualPositionD = -17).parse(withExtra) as GasDoseParamPacket
        assertEquals(0, parsed.manualPositionD)
        assertEquals(30f, parsed.fcClosing)
        assertEquals(12.5f, parsed.lambdaCorrLimitP)
        assertEquals(20f, parsed.lambdaCorrLimitM, .1f)
        assertEquals(14.7f, parsed.lambdaStoichval, .01f)
        assertEquals(0xD1, parsed.pack()[bytes.size])
        assertEquals(0xE2, parsed.pack()[bytes.size + 1])
        val snapshot: OutputPacket = parsed.snapshotWithManualPosition(-127)
        assertEquals(0, parsed.manualPositionD)
        assertEquals(129, snapshot.pack()[4])
        val copy = snapshot.pack(); copy[4] = 0
        assertEquals(129, snapshot.pack()[4])
    }
}
