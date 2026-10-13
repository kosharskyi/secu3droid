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
import org.secu3.android.models.packets.out.params.TemperatureParamPacket

class TemperatureStateHolderTest {
    @Test
    fun loadingDoesNotSendAndAcceptedNumericAndFlagEditsUpdateAndSendOnce() {
        val sent = mutableListOf<TemperatureParamPacket>()
        var mapping = 0
        var mapped = 0
        val holder = TemperatureStateHolder(sent::add, { mapping++ }, { mapped++ })

        assertEquals(TemperatureUiState.Loading, holder.state)
        holder.updateValue(TemperatureNumber.FAN_ON, 88.5f)
        holder.updateFlag(TemperatureFlag.COOLANT_SENSOR, true)
        assertTrue(sent.isEmpty())

        val packet = TemperatureParamPacket()
        holder.onPacketReceived(packet)
        assertEquals(1, mapping)
        assertEquals(1, mapped)
        assertEquals(TemperatureUiState.Data(TemperatureValues.from(packet)), holder.state)
        assertTrue("mapping a received packet must not echo it", sent.isEmpty())

        holder.updateValue(TemperatureNumber.FAN_ON, 88.5f)
        holder.updateValue(TemperatureNumber.PWM_FREQUENCY, 270f)
        holder.updateFlag(TemperatureFlag.COOLANT_SENSOR, true)

        assertEquals(3, sent.size)
        assertTrue(sent.all { it === packet })
        assertEquals(88.5f, packet.ventOn)
        assertEquals(270, packet.ventPwmFrq)
        assertTrue(packet.coolantUse)
        val state = (holder.state as TemperatureUiState.Data).values
        assertEquals(88.5f, state.value(TemperatureNumber.FAN_ON))
        assertEquals(270f, state.value(TemperatureNumber.PWM_FREQUENCY))
        assertTrue(state.flags.getValue(TemperatureFlag.COOLANT_SENSOR))
    }

    @Test
    fun resetClearsPacketAndReturnsToLoadingWithoutSending() {
        val sent = mutableListOf<TemperatureParamPacket>()
        val holder = TemperatureStateHolder(sent::add)
        holder.onPacketReceived(TemperatureParamPacket())

        holder.reset()
        holder.updateValue(TemperatureNumber.FAN_ON, 88f)
        holder.updateFlag(TemperatureFlag.COOLANT_SENSOR, true)

        assertEquals(TemperatureUiState.Loading, holder.state)
        assertTrue(sent.isEmpty())
    }
}
