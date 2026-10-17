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
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.secu3.android.models.packets.out.params.MiscellaneousParamPacket

class MiscellaneousStateHolderTest {
    private fun validPacket() = MiscellaneousParamPacket(
        uartDivisor = 0x0103,
        uartPeriodTms = 100,
        evapAfbegin = 32000,
        evapAfslope = 32f / 40000f,
        fpTimeoutStrt = 3f,
        pwmFrq0 = 100,
        pwmFrq1 = 200,
        vssPeriodDist = 1000f,
    )

    @Test
    fun validEditsUpdateStateAndSendTheSamePacketOnce() {
        val sent = mutableListOf<MiscellaneousParamPacket>()
        val holder = MiscellaneousStateHolder(sent::add)
        val packet = validPacket()
        holder.onPacketReceived(packet)
        assertTrue("mapping a received packet must not echo it", sent.isEmpty())

        holder.editInt(MiscField.PERIOD, 120)
        holder.toggle(MiscToggle.PUMP, true)

        assertEquals(2, sent.size)
        assertSame(packet, sent[0])
        assertSame(packet, sent[1])
        assertEquals(120, (holder.state!!).period)
        assertTrue((holder.state!!).pump)
        assertFalse(holder.hasValidationError)
    }

    @Test
    fun invalidIncomingPacketBlocksUnrelatedEditsUntilEvapPairIsRepaired() {
        val sent = mutableListOf<MiscellaneousParamPacket>()
        val holder = MiscellaneousStateHolder(sent::add)
        val retainedVss = 32768000f / 65275f
        val invalid = validPacket().apply { evapAfslope = 0f; vssPeriodDist = retainedVss }
        holder.onPacketReceived(invalid)

        holder.editInt(MiscField.PERIOD, 150)
        holder.selectBaud(0x0015)
        assertTrue(holder.hasValidationError)
        assertTrue(sent.isEmpty())
        assertEquals(100, invalid.uartPeriodTms)
        assertEquals(0x0103, invalid.uartDivisor)
        assertEquals(retainedVss, invalid.vssPeriodDist, 0f)
        assertEquals(502, holder.state!!.vss)

        holder.editInt(MiscField.END, 100000)
        assertFalse(holder.hasValidationError)
        assertEquals(1, sent.size)
        assertSame(invalid, sent.single())
        assertTrue(invalid.pack().isNotEmpty())
        assertEquals(retainedVss, invalid.vssPeriodDist, 0f)
        assertTrue(holder.state!!.end - holder.state!!.begin >= MiscellaneousParamPacket.MIN_EVAP_AIRFLOW_DELTA)
    }

    @Test
    fun receivingAndUnrelatedEditKeepPreciseVssWhileUiStateRoundsForDisplay() {
        val sent = mutableListOf<MiscellaneousParamPacket>()
        val holder = MiscellaneousStateHolder(sent::add)
        val packet = validPacket().apply { vssPeriodDist = 32768000f / 65275f }
        holder.onPacketReceived(packet)

        assertTrue(sent.isEmpty())
        assertEquals(502, holder.state!!.vss)
        holder.editInt(MiscField.PERIOD, 120)

        assertEquals(1, sent.size)
        assertEquals(32768000f / 65275f, packet.vssPeriodDist, 0f)
        assertEquals(0xFE, packet.pack()[21])
        assertEquals(0xFB, packet.pack()[22])
        assertEquals(502, holder.state!!.vss)

        holder.editInt(MiscField.VSS, 1200)
        assertEquals(2, sent.size)
        assertEquals(1200, holder.state!!.vss)
        assertEquals(1200f, packet.vssPeriodDist, 0f)
        assertEquals(0x6A, packet.pack()[21])
        assertEquals(0xAB, packet.pack()[22])
    }

    @Test
    fun invalidEvapCandidateDoesNotSendOrLeavePartialMutation() {
        val sent = mutableListOf<MiscellaneousParamPacket>()
        val holder = MiscellaneousStateHolder(sent::add)
        val packet = validPacket()
        holder.onPacketReceived(packet)
        val originalBegin = packet.evapAfbegin
        val originalSlope = packet.evapAfslope

        holder.editInt(MiscField.END, packet.evapAfbegin + MiscellaneousParamPacket.MIN_EVAP_AIRFLOW_DELTA - 1)

        assertTrue(holder.hasValidationError)
        assertTrue(sent.isEmpty())
        assertEquals(originalBegin, packet.evapAfbegin)
        assertEquals(originalSlope, packet.evapAfslope)
    }

    @Test
    fun resetClearsPacketStateAndValidationAndIgnoresEdits() {
        val sent = mutableListOf<MiscellaneousParamPacket>()
        val holder = MiscellaneousStateHolder(sent::add)
        holder.onPacketReceived(validPacket())
        holder.reset()

        holder.selectBaud(0x0015)
        holder.editInt(MiscField.PERIOD, 150)
        holder.toggle(MiscToggle.PUMP, true)

        assertEquals(null, holder.currentPacket)
        assertEquals(null, holder.state)
        assertFalse(holder.hasValidationError)
        assertTrue(sent.isEmpty())
    }
}
