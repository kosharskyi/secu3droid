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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.secu3.android.models.packets.out.params.CarburParamPacket

class CarburStateHolderTest {
    @Test
    fun loadingDoesNotSendAndAcceptedEditsUpdateAndSendOnce() {
        val sent = mutableListOf<CarburParamPacket>()
        var mapping = 0
        var mapped = 0
        val holder = CarburStateHolder(sent::add, { mapping++ }, { mapped++ })

        assertEquals(CarburUiState.Loading, holder.state)
        holder.updateValue(CarburNumber.TPS, 24f)
        holder.updateValue(CarburNumber.IDLE_LOW, 950f)
        holder.updateInvers(true)
        holder.updateSelector(false, 6)
        assertTrue(sent.isEmpty())

        val packet = CarburParamPacket()
        holder.onPacketReceived(packet)
        assertEquals(1, mapping)
        assertEquals(1, mapped)
        assertEquals(CarburUiState.Data(CarburValues.from(packet)), holder.state)
        assertTrue("mapping a received packet must not echo it", sent.isEmpty())

        holder.updateValue(CarburNumber.TPS, 24f)
        holder.updateValue(CarburNumber.IDLE_LOW, 950f)
        holder.updateInvers(true)
        holder.updateSelector(false, 6)
        holder.updateSelector(true, 0)

        assertEquals(5, sent.size)
        assertTrue(sent.all { it === packet })
        assertEquals(24f, packet.tpsThreshold)
        assertEquals(950, packet.ieLot)
        assertEquals(1, packet.carbInvers)
        assertEquals(15, packet.fuelcut_uni)
        assertEquals(1, packet.igncut_uni)
        val values = (holder.state as CarburUiState.Data).values
        assertEquals(24f, values.numbers.getValue(CarburNumber.TPS))
        assertEquals(950f, values.numbers.getValue(CarburNumber.IDLE_LOW))
        assertTrue(values.invers)
        assertEquals(15, values.fuelCutUni)
        assertEquals(1, values.ignitionCutUni)
    }

    @Test
    fun invalidSelectorDoesNotChangeStatePacketOrSend() {
        val sent = mutableListOf<CarburParamPacket>()
        val holder = CarburStateHolder(sent::add)
        val packet = CarburParamPacket(fuelcut_uni = 2, igncut_uni = 4)
        holder.onPacketReceived(packet)
        val before = holder.state

        holder.updateSelector(ignition = false, index = 7)
        holder.updateSelector(ignition = true, index = -1)

        assertEquals(before, holder.state)
        assertEquals(2, packet.fuelcut_uni)
        assertEquals(4, packet.igncut_uni)
        assertFalse(sent.isNotEmpty())
    }

    @Test
    fun resetClearsPacketAndReturnsToLoadingWithoutSending() {
        val sent = mutableListOf<CarburParamPacket>()
        val holder = CarburStateHolder(sent::add)
        holder.onPacketReceived(CarburParamPacket())

        holder.reset()
        holder.updateValue(CarburNumber.TPS, 24f)
        holder.updateInvers(true)
        holder.updateSelector(false, 6)

        assertEquals(CarburUiState.Loading, holder.state)
        assertTrue(sent.isEmpty())
    }
}
