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
import org.secu3.android.models.packets.out.params.IdlingParamPacket

class IdlingStateHolderTest {
    @Test
    fun loadingDoesNotSendAndAcceptedEditUpdatesAndSendsPacket() {
        val sent = mutableListOf<IdlingParamPacket>()
        var mapping = 0
        var mapped = 0
        lateinit var holder: IdlingStateHolder
        holder = IdlingStateHolder(
            sendPacket = sent::add,
            onPacketMapping = {
                mapping++
                assertEquals(IdlingUiState.Loading, holder.state)
            },
            onPacketMapped = {
                mapped++
                assertTrue(holder.state is IdlingUiState.Data)
            },
        )

        assertEquals(IdlingUiState.Loading, holder.state)
        holder.updateValue(IdlingNumber.IFAC1, 2.5f)
        assertTrue(sent.isEmpty())

        val packet = IdlingParamPacket()
        holder.onPacketReceived(packet)
        assertEquals(1, mapping)
        assertEquals(1, mapped)
        assertEquals(IdlingUiState.Data(IdlingValues.from(packet)), holder.state)
        assertTrue("mapping a received packet must not echo it", sent.isEmpty())

        holder.updateValue(IdlingNumber.IFAC1, 2.5f)

        assertEquals(2.5f, packet.iFac1)
        assertEquals(1, sent.size)
        assertTrue(sent.single() === packet)
        assertEquals(2.5f, (holder.state as IdlingUiState.Data).values.value(IdlingNumber.IFAC1))
    }

    @Test
    fun flagChangeUpdatesStateAndSendsOnce() {
        val sent = mutableListOf<IdlingParamPacket>()
        val holder = IdlingStateHolder(sent::add)
        val packet = IdlingParamPacket()
        holder.onPacketReceived(packet)

        holder.updateFlag(IdlingFlag.REGULATOR, true)

        assertEquals(1, sent.size)
        assertTrue(sent.single() === packet)
        assertTrue(packet.useRegulator)
        assertTrue((holder.state as IdlingUiState.Data).values.flags.getValue(IdlingFlag.REGULATOR))
    }

    @Test
    fun resetClearsPacketAndReturnsToLoadingWithoutSending() {
        val sent = mutableListOf<IdlingParamPacket>()
        val holder = IdlingStateHolder(sent::add)
        holder.onPacketReceived(IdlingParamPacket())

        holder.reset()
        holder.updateValue(IdlingNumber.IFAC1, 4f)
        holder.updateFlag(IdlingFlag.REGULATOR, true)

        assertEquals(IdlingUiState.Loading, holder.state)
        assertTrue(sent.isEmpty())
    }

}
