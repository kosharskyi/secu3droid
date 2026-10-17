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
import org.secu3.android.models.packets.out.params.CkpsParamPacket

class CkpsStateHolderTest {
    @Test fun loadingAndAcceptedNumberFlagAndChoiceEditsSendOnceEach() {
        val sent = mutableListOf<CkpsParamPacket>()
        var mapping = 0
        var mapped = 0
        val holder = CkpsStateHolder(sent::add, { mapping++ }, { mapped++ })
        assertEquals(CkpsUiState.Loading, holder.state)
        holder.updateNumber(CkpsNumber.WHEEL, 36f)
        holder.updateFlag(CkpsFlag.CKPS_EDGE, true)
        holder.updateChoice(true, 20)
        assertTrue(sent.isEmpty())

        val packet = CkpsParamPacket(ckpsCogsNum = 60, ckpsEngineCyl = 4)
        holder.onPacketReceived(packet)
        assertEquals(1, mapping); assertEquals(1, mapped)
        assertTrue("mapping a received packet must not echo it", sent.isEmpty())
        holder.updateNumber(CkpsNumber.MISSING, 2f)
        assertEquals(1, sent.size)
        assertEquals(2, packet.ckpsMissNum)
        assertEquals(2.5f, packet.mttf)
        holder.updateFlag(CkpsFlag.CKPS_EDGE, true)
        assertEquals(2, sent.size)
        assertTrue(packet.ckpsEdge)
        holder.updateChoice(true, 20)
        assertEquals(3, sent.size)
        assertEquals(20, packet.ckpsCogsBtdc)
        holder.updateChoice(false, 6)
        assertEquals(4, sent.size)
        assertEquals(6, packet.ckpsEngineCyl)
        assertTrue(sent.all { it === packet })
        val values = (holder.state as CkpsUiState.Data).values
        assertEquals(CkpsValues.from(packet), values)
    }

    @Test fun resetClearsPacketAndIgnoresEdits() {
        val sent = mutableListOf<CkpsParamPacket>()
        val holder = CkpsStateHolder(sent::add)
        holder.onPacketReceived(CkpsParamPacket())
        holder.reset()
        holder.updateNumber(CkpsNumber.WHEEL, 36f)
        holder.updateFlag(CkpsFlag.CKPS_EDGE, true)
        holder.updateChoice(true, 20)
        assertEquals(CkpsUiState.Loading, holder.state)
        assertTrue(sent.isEmpty())
    }

    @Test fun invalidChoicesLeavePacketAndStateUnchangedAndDoNotSend() {
        val sent = mutableListOf<CkpsParamPacket>()
        val holder = CkpsStateHolder(sent::add)
        val packet = CkpsParamPacket(ckpsCogsNum = 60, ckpsCogsBtdc = 20, ckpsEngineCyl = 4)
        holder.onPacketReceived(packet)
        val before = holder.state
        holder.updateChoice(true, 10)
        holder.updateChoice(false, 9)
        assertEquals(before, holder.state)
        assertEquals(20, packet.ckpsCogsBtdc)
        assertEquals(4, packet.ckpsEngineCyl)
        assertFalse(sent.isNotEmpty())
    }
}
