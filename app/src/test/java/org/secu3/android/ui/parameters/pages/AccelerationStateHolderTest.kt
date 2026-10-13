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
package org.secu3.android.ui.parameters.pages

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.secu3.android.models.packets.out.params.AccelerationParamPacket
import org.secu3.android.ui.parameters.pages.compose.AccelerationNumber

class AccelerationStateHolderTest {
    @Test
    fun loadingAndResetBlockEditsUntilPacketReceived() {
        val sent = mutableListOf<AccelerationParamPacket>()
        val allowed = mutableListOf<Boolean>()
        val holder = AccelerationStateHolder(sent::add, allowed::add)

        holder.updateNumber(AccelerationNumber.AE_TIME, 12f)
        holder.selectAeType(2)
        holder.selectWallwetModel(1)
        holder.setBalance(33f)
        assertNull(holder.values)
        assertTrue(sent.isEmpty())

        holder.onPacketReceived(AccelerationParamPacket())
        assertEquals(listOf(false, true), allowed)
        holder.reset()
        holder.updateNumber(AccelerationNumber.AE_TIME, 12f)
        assertNull(holder.values)
        assertTrue(sent.isEmpty())
        assertEquals("Resetting an unloaded neighbor must not block the active page", listOf(false, true), allowed)
    }

    @Test
    fun eachNumericEditMapsIntoAndSendsTheSamePacketOnce() {
        val sent = mutableListOf<AccelerationParamPacket>()
        val holder = AccelerationStateHolder(sent::add)
        val packet = AccelerationParamPacket()
        holder.onPacketReceived(packet)
        val editedValues = listOf(41f, 250f, 21f, 17f, 88f, -37f, -19f)

        AccelerationNumber.entries.forEachIndexed { index, number ->
            holder.updateNumber(number, editedValues[index])
            assertSame(packet, sent[index])
            assertEquals(editedValues[index], holder.values!!.value(number))
        }

        assertEquals(7, sent.size)
        assertEquals(41, packet.injAeTpsdotThrd)
        assertEquals(250, packet.injAeColdaccMult)
        assertEquals(21, packet.injAeDecayTime)
        assertEquals(17, packet.injAeTime)
        assertEquals(88, packet.injAeMapdotThrd)
        assertEquals(-37f, packet.injXtauSThrd)
        assertEquals(-19f, packet.injXtauFThrd)
    }

    @Test
    fun selectorsAndBalanceUpdateModelAndSendOnceEach() {
        val sent = mutableListOf<AccelerationParamPacket>()
        val holder = AccelerationStateHolder(sent::add)
        val packet = AccelerationParamPacket()
        holder.onPacketReceived(packet)

        holder.selectAeType(4)
        holder.selectWallwetModel(3)
        holder.setBalance(52.5f)

        assertEquals(3, sent.size)
        assertTrue(sent.all { it === packet })
        assertEquals(4, packet.injAeType)
        assertEquals(3, packet.wallwetModel)
        assertEquals(52.5f, packet.injAeBallance)
        assertEquals(4, holder.values?.aeType)
        assertEquals(3, holder.values?.wallwetModel)
        assertEquals(52.5f, holder.values?.balance)
    }

    @Test
    fun editsKeepUnknownPacketTailAndUseReceivedPacketIdentity() {
        val sent = mutableListOf<AccelerationParamPacket>()
        val holder = AccelerationStateHolder(sent::add)
        val packet = AccelerationParamPacket().also { it.injAeType = 9 }
        val raw = packet.pack().drop(1).toIntArray() + intArrayOf(0xA5, 0x5A)
        val received = AccelerationParamPacket().parse(intArrayOf(0x7C, 0x20) + raw) as AccelerationParamPacket
        holder.onPacketReceived(received)

        holder.updateNumber(AccelerationNumber.TPS_THRESHOLD, 73f)

        assertSame(received, sent.single())
        assertEquals(9, received.injAeType)
        assertEquals(listOf(0xA5, 0x5A), received.pack().takeLast(2))
        assertFalse(sent.isEmpty())
    }
}
