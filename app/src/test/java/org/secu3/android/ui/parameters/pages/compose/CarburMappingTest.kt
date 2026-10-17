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
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 *    contacts: http://secu-3.org; vetalkosharskiy@gmail.com
 */
package org.secu3.android.ui.parameters.pages.compose

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.secu3.android.models.packets.out.params.CarburParamPacket

class CarburMappingTest {
    @Test fun readsAndWritesAllElevenValuesAndPreservesOtherPacketData() {
        val source = CarburParamPacket(ieLot = 1000, ieHit = 1200, carbInvers = 1, feOnThresholds = 12.3f,
            ieLotG = 1100, ieHitG = 1300, shutoffDelay = .55f, tpsThreshold = 31.5f,
            fuelcutMapThrd = 88.5f, fuelcutCtsThrd = -10.5f, revlimLot = 5000, revlimHit = 5500,
            fuelcut_uni = 15, igncut_uni = 6)
        val packet = source
        val withExtras = CarburParamPacket().parse(source.pack().drop(1).toIntArray() + intArrayOf(222, 111)) as CarburParamPacket
        val state = CarburValues.from(source)
        assertEquals(11, state.numbers.size)
        assertEquals(88.5f, state.numbers.getValue(CarburNumber.MAP))
        CarburNumber.entries.forEachIndexed { i, number -> applyCarburValue(packet, number, 20f + i) }
        assertEquals(20, packet.ieLot); assertEquals(21, packet.ieHit)
        assertEquals(22, packet.ieLotG); assertEquals(23, packet.ieHitG)
        assertEquals(24f, packet.shutoffDelay); assertEquals(25f, packet.feOnThresholds)
        assertEquals(26f, packet.tpsThreshold); assertEquals(27f, packet.fuelcutMapThrd)
        assertEquals(28f, packet.fuelcutCtsThrd); assertEquals(29, packet.revlimLot); assertEquals(30, packet.revlimHit)
        assertEquals(1, packet.carbInvers); assertEquals(15, packet.fuelcut_uni); assertEquals(6, packet.igncut_uni)
        applyCarburValue(withExtras, CarburNumber.MAP, 15f)
        assertTrue(withExtras.pack().takeLast(2).toIntArray().contentEquals(intArrayOf(222, 111)))
    }

    @Test fun selectorCodesAndUnknownIncomingValuesAreSafeUntilExplicitSelection() {
        assertEquals(listOf(1, 2, 3, 4, 5, 6, 15), CarburValues.selectorCodes)
        val packet = CarburParamPacket(fuelcut_uni = 41, igncut_uni = 42)
        val original = CarburValues.from(packet)
        assertFalse(applyCarburSelector(packet, false, 7))
        assertFalse(applyCarburSelector(packet, true, -1))
        assertEquals(original, CarburValues.from(packet))
        assertTrue(applyCarburSelector(packet, false, 6)); assertEquals(15, packet.fuelcut_uni)
        assertTrue(applyCarburSelector(packet, true, 0)); assertEquals(1, packet.igncut_uni)
    }

    @Test fun rangesPrecisionAndTypesMatchLegacyLayout() {
        assertEquals(11, CarburNumber.entries.size)
        assertEquals(listOf(6, 5), listOf(CarburNumber.entries.count { it.integer }, CarburNumber.entries.count { !it.integer }))
        assertEquals(listOf(30000f, 30000f, 30000f, 30000f), listOf(CarburNumber.IDLE_LOW.max, CarburNumber.IDLE_HIGH.max, CarburNumber.IDLE_LOW_GAS.max, CarburNumber.IDLE_HIGH_GAS.max))
        assertEquals(250f, CarburNumber.IDLE_LOW.min); assertEquals(10f, CarburNumber.IDLE_LOW.step)
        assertEquals(500f, CarburNumber.VALVE_PRESSURE.max); assertEquals(.1f, CarburNumber.VALVE_PRESSURE.step)
        assertEquals(-40f, CarburNumber.CTS.min); assertEquals(300f, CarburNumber.CTS.max); assertEquals(.25f, CarburNumber.CTS.step)
        assertEquals(2, CarburNumber.CUTOFF_DELAY.precision); assertEquals(1, CarburNumber.VALVE_PRESSURE.precision)
        assertEquals(.5f, CarburNumber.TPS.step); assertEquals(30000f, CarburNumber.REV_HIGH.max)
    }
}
