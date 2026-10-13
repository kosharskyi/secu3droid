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
import org.secu3.android.models.packets.out.params.AnglesParamPacket

class AnglesStateHolderTest {
    @Test
    fun loadingDoesNotSendAndAcceptedEditUpdatesAndSendsPacket() {
        val sent = mutableListOf<AnglesParamPacket>()
        var mapping = 0
        var mapped = 0
        lateinit var holder: AnglesStateHolder
        holder = AnglesStateHolder(
            sendPacket = sent::add,
            onPacketMapping = {
                mapping++
                assertEquals(AnglesUiState.Loading, holder.state)
            },
            onPacketMapped = {
                mapped++
                assertTrue(holder.state is AnglesUiState.Data)
            },
        )

        assertEquals(AnglesUiState.Loading, holder.state)
        holder.updateValue(AnglesNumber.OCTANE, 4.5f)
        assertTrue(sent.isEmpty())

        val packet = AnglesParamPacket()
        holder.onPacketReceived(packet)
        assertEquals(1, mapping)
        assertEquals(1, mapped)
        assertEquals(AnglesUiState.Data(AnglesValues.from(packet)), holder.state)
        assertTrue("Receiving a packet must not echo it to the ECU", sent.isEmpty())

        holder.updateValue(AnglesNumber.OCTANE, 4.5f)

        assertEquals(4.5f, packet.angleCorrection)
        assertEquals(1, sent.size)
        assertTrue(sent.single() === packet)
        assertEquals(4.5f, (holder.state as AnglesUiState.Data).values.value(AnglesNumber.OCTANE))
    }

    @Test
    fun flagChangeUpdatesStateAndSendsOnce() {
        val sent = mutableListOf<AnglesParamPacket>()
        val holder = AnglesStateHolder(sent::add)
        val packet = AnglesParamPacket()
        holder.onPacketReceived(packet)

        holder.updateFlag(AnglesFlag.WORKING_MAP, true)

        assertEquals(1, sent.size)
        assertTrue(sent.single() === packet)
        assertTrue(packet.alwaysUseIgnitionMap)
        assertTrue((holder.state as AnglesUiState.Data).values.flags.getValue(AnglesFlag.WORKING_MAP))
    }

    @Test
    fun resetDiscardsOldPacketBeforeAnotherRead() {
        val sent = mutableListOf<AnglesParamPacket>()
        val holder = AnglesStateHolder(sent::add)
        holder.onPacketReceived(AnglesParamPacket())

        holder.reset()
        holder.updateValue(AnglesNumber.OCTANE, 4.5f)

        assertEquals(AnglesUiState.Loading, holder.state)
        assertTrue(sent.isEmpty())
    }

}
