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
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.secu3.android.models.packets.out.params.LtftParamPacket

class LtftStateHolderTest {
    @Test fun startsAndResetsLoadingAndReceiveDoesNotSend() {
        val sent = mutableListOf<LtftParamPacket>()
        val holder = LtftStateHolder(sent::add)
        assertFalse(holder.isSendAllowed)
        assertEquals(LtftUiState.Loading, holder.uiState)
        holder.edit(LtftNumber.LOWER_CLT, 12f)
        holder.selectMode(2)
        assertTrue(sent.isEmpty())

        val packet = LtftParamPacket(mode = 3)
        holder.receive(packet)
        assertTrue(holder.isSendAllowed)
        assertEquals(3, (holder.uiState as LtftUiState.Data).values.mode)
        assertTrue(sent.isEmpty())
        holder.reset()
        holder.edit(LtftNumber.LOWER_CLT, 12f)
        holder.selectMode(2)
        assertFalse(holder.isSendAllowed)
        assertEquals(LtftUiState.Loading, holder.uiState)
        assertTrue(sent.isEmpty())
    }

    @Test fun eachNumericEditUpdatesExpectedPacketFieldAndSendsSamePacketOnce() {
        val sent = mutableListOf<LtftParamPacket>()
        val packet = LtftParamPacket()
        val holder = LtftStateHolder(sent::add)
        holder.receive(packet)
        LtftNumber.entries.forEachIndexed { index, number ->
            val value = if (number.integer) index * 10f else index * 2.25f + .75f
            holder.edit(number, value)
            assertEquals(1, sent.size)
            assertSame(packet, sent.single())
            val expected = if (number.integer) value.toInt().toFloat() else value
            assertEquals(expected, (holder.uiState as LtftUiState.Data).values.value(number), .0001f)
            sent.clear()
        }
        assertEquals(0, packet.learnClt.compareTo(.75f))
        assertEquals(80, packet.learnRpm0)
        assertEquals(90, packet.learnRpm1)
        assertEquals(0, packet.deadBand1.compareTo(30f))
    }

    @Test fun modeRequiresValidIndexAndSendsSamePacketOnce() {
        val sent = mutableListOf<LtftParamPacket>()
        val packet = LtftParamPacket(mode = 1)
        val holder = LtftStateHolder(sent::add)
        holder.receive(packet)
        listOf(-1, 4, Int.MIN_VALUE, Int.MAX_VALUE).forEach(holder::selectMode)
        assertEquals(1, packet.mode)
        assertEquals(1, (holder.uiState as LtftUiState.Data).values.mode)
        assertTrue(sent.isEmpty())
        holder.selectMode(3)
        assertEquals(3, packet.mode)
        assertEquals(3, (holder.uiState as LtftUiState.Data).values.mode)
        assertEquals(1, sent.size)
        assertSame(packet, sent.single())
    }

    @Test fun editsRetainPacketIdentityAndUnknownProtocolTail() {
        val original = LtftParamPacket()
        val wireWithHeaderAndTail = intArrayOf(0x40, 0x40) + original.pack() + intArrayOf(0xA5, 0x5A)
        val received = LtftParamPacket().parse(wireWithHeaderAndTail) as LtftParamPacket
        val sent = mutableListOf<LtftParamPacket>()
        val holder = LtftStateHolder(sent::add)
        holder.receive(received)
        holder.edit(LtftNumber.UPPER_CLT, 44.5f)
        assertSame(received, sent.single())
        assertEquals(listOf(0xA5, 0x5A), received.pack().takeLast(2))
        assertEquals(44.5f, received.learnCltUp, .0001f)
    }
}
