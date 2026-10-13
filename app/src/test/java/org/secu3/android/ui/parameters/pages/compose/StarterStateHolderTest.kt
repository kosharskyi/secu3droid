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
import org.secu3.android.models.packets.out.params.StarterParamPacket

class StarterStateHolderTest {
    @Test
    fun packetLoadPublishesValuesAndAcceptedDialogEditUpdatesAndSendsPacket() {
        val sent = mutableListOf<StarterParamPacket>()
        var loaded = 0
        val holder = StarterStateHolder(sent::add) { loaded++ }

        assertEquals(StarterUiState.Loading, holder.state)
        holder.updateValue(StarterNumber.STARTER_OFF, 750f)
        assertTrue(sent.isEmpty())

        val packet = StarterParamPacket()
        holder.onPacketReceived(packet)
        assertEquals(1, loaded)
        assertEquals(StarterUiState.Data(StarterValues.from(packet)), holder.state)

        holder.updateValue(StarterNumber.STARTER_OFF, 750f)

        assertEquals(750, packet.starterOff)
        assertEquals(1, sent.size)
        assertTrue(sent.single() === packet)
        assertEquals(750f, (holder.state as StarterUiState.Data).values.value(StarterNumber.STARTER_OFF))
    }

    @Test
    fun flagChangeUpdatesOnlyRequestedFlagAndSendsUpdatedPacket() {
        val sent = mutableListOf<StarterParamPacket>()
        val holder = StarterStateHolder(sent::add)
        val packet = StarterParamPacket().apply {
            allowStartOnClearFlood = false
            limitMaxInjPwOnCranking = true
        }
        holder.onPacketReceived(packet)

        holder.updateFlag(StarterFlag.ALLOW_FLOOD_CLEAR_START, true)

        assertTrue(packet.allowStartOnClearFlood)
        assertTrue(packet.limitMaxInjPwOnCranking)
        assertEquals(1, sent.size)
        val flags = (holder.state as StarterUiState.Data).values.flags
        assertTrue(flags.getValue(StarterFlag.ALLOW_FLOOD_CLEAR_START))
        assertTrue(flags.getValue(StarterFlag.LIMIT_CRANKING_PW))
    }
}
