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
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.secu3.android.models.packets.out.params.DbwParamPacket

class DbwStateHolderTest {
    @Test fun loadingAndReceiveDoNotSendAndResetRestoresLoading() {
        val sent = mutableListOf<DbwParamPacket>()
        val allowed = mutableListOf<Boolean>()
        val holder = DbwStateHolder(sent::add, allowed::add)
        assertEquals(DbwUiState.Loading, holder.uiState)
        holder.edit(DbwNumber.ETC_P, 1.25f)
        assertTrue(sent.isEmpty())

        val packet = DbwParamPacket(homePosition_edited = 31.25f)
        holder.receive(packet)
        assertEquals(31.25f, (holder.uiState as DbwUiState.Data).values.homePosition, .0001f)
        assertTrue(sent.isEmpty())
        assertEquals(listOf(false, true), allowed)

        holder.reset()
        assertEquals(DbwUiState.Loading, holder.uiState)
        holder.edit(DbwNumber.ETC_I, 1f)
        assertTrue(sent.isEmpty())
        assertEquals("Resetting an unloaded neighbor must not block the active page", listOf(false, true), allowed)
    }

    @Test fun eachEditableNumberUpdatesExpectedPacketAndSendsSamePacketOnce() {
        val sent = mutableListOf<DbwParamPacket>()
        val packet = DbwParamPacket(homePosition_edited = 47.5f)
        val holder = DbwStateHolder(sent::add)
        holder.receive(packet)

        DbwNumber.entries.forEachIndexed { index, number ->
            val value = index + 1.125f
            holder.edit(number, value)
            assertEquals("${number.name} send count", 1, sent.size)
            assertSame("${number.name} packet identity", packet, sent.single())
            assertEquals("${number.name} displayed value", value,
                (holder.uiState as DbwUiState.Data).values.value(number), .0001f)
            when (number) {
                DbwNumber.ETC_P -> assertEquals(value, packet.etc_p, .0001f)
                DbwNumber.ETC_I -> assertEquals(value, packet.etc_i, .0001f)
                DbwNumber.ETC_D -> assertEquals(value, packet.etc_d, .0001f)
                DbwNumber.NMAX_DUTY -> assertEquals(value, packet.etc_nmax_duty, .0001f)
                DbwNumber.PMAX_DUTY -> assertEquals(value, packet.etc_pmax_duty, .0001f)
                DbwNumber.PID_PERIOD -> assertEquals(value, packet.pid_period, .0001f)
                DbwNumber.FRICTION_TORQUE_OPEN -> assertEquals(value, packet.frictorq_open, .0001f)
                DbwNumber.FRICTION_TORQUE_CLOSE -> assertEquals(value, packet.frictorq_close, .0001f)
                DbwNumber.FRICTION_THRD -> assertEquals(value, packet.frictorq_thrd, .0001f)
                DbwNumber.FRICTORQ_IDLE_ADD_MAX -> assertEquals(value, packet.frictorq_idleadd_max, .0001f)
            }
            assertEquals("${number.name} read-only home position", 47.5f,
                (holder.uiState as DbwUiState.Data).values.homePosition, .0001f)
            assertEquals(47.5f, packet.homePosition_edited, .0001f)
            sent.clear()
        }
    }

    @Test fun editsPreservePacketIdentityAndUnknownProtocolTail() {
        val rawHomePosition = (47.5f * 64).toInt()
        val homePositionBytes = intArrayOf(rawHomePosition shr 8, rawHomePosition and 0xFF)
        val unknownTail = intArrayOf(0xA5, 0x5A)
        val wireWithHeaderAndTail = intArrayOf(0x40, 0x40) + DbwParamPacket().pack().drop(1) + homePositionBytes + unknownTail
        val received = DbwParamPacket().parse(wireWithHeaderAndTail) as DbwParamPacket
        val sent = mutableListOf<DbwParamPacket>()
        val holder = DbwStateHolder(sent::add)
        holder.receive(received)
        assertEquals(47.5f, (holder.uiState as DbwUiState.Data).values.homePosition, .0001f)
        holder.edit(DbwNumber.ETC_I, 2.75f)

        assertSame(received, sent.single())
        val repackedPayload = received.pack().drop(1)
        assertEquals(unknownTail.toList(), repackedPayload.takeLast(unknownTail.size))
        assertEquals(DbwParamPacket().pack().size - 1 + unknownTail.size, repackedPayload.size)
        assertEquals(47.5f, received.homePosition_edited, .0001f)
        assertEquals(2.75f, received.etc_i, .0001f)
    }
}
