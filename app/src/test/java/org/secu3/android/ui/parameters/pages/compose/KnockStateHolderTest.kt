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
import org.secu3.android.models.packets.out.params.KnockParamPacket

class KnockStateHolderTest {
    private val bpfOptions = listOf("1.22", "1.26", "1.31")
    private val integrationOptions = listOf("40", "45", "50", "55")

    private fun holder(sent: MutableList<KnockParamPacket>) = KnockStateHolder(
        bpfOptions = bpfOptions,
        integrationOptions = integrationOptions,
        sendPacket = sent::add,
    )

    @Test
    fun loadingAndPacketMappingDoNotSendAndEnabledIsDerivedFromPacket() {
        val sent = mutableListOf<KnockParamPacket>()
        val holder = holder(sent)
        assertEquals(KnockUiState.Loading, holder.state)
        holder.updateValue(KnockNumber.BEGIN, 5f)
        holder.updateEnabled(true)
        holder.updateChannel(0, true)
        holder.updateSelector(KnockSelector.BPF, 1)
        assertTrue(sent.isEmpty())

        val packet = KnockParamPacket(useKnockChannel = 2, bpfFrequency = 1, intTimeCost = 3)
        holder.onPacketReceived(packet)
        val state = holder.state as KnockUiState.Data
        assertTrue(state.enabled)
        assertEquals("1.26", state.values.selector(KnockSelector.BPF))
        assertEquals("55", state.values.selector(KnockSelector.INTEGRATION))
        assertTrue(sent.isEmpty())

        packet.useKnockChannel = 0
        holder.onPacketReceived(packet)
        assertFalse((holder.state as KnockUiState.Data).enabled)
        assertTrue(sent.isEmpty())
    }

    @Test
    fun eachNumericEditMapsPacketAndSendsOnce() {
        val sent = mutableListOf<KnockParamPacket>()
        val holder = holder(sent)
        val packet = KnockParamPacket()
        holder.onPacketReceived(packet)
        val edits = KnockNumber.entries.mapIndexed { index, number -> number to (index + 1.25f) }
        edits.forEach { (number, value) ->
            holder.updateValue(number, value)
            assertEquals(sent.size, 1 + edits.takeWhile { it.first != number }.size)
            assertSame(packet, sent.last())
            val expected = if (number == KnockNumber.DELAY) value.toInt().toFloat() else value
            assertEquals(expected, (holder.state as KnockUiState.Data).values.value(number))
        }
        assertEquals(edits.size, sent.size)
        assertEquals(1.25f, packet.kWndBeginAngle)
        assertEquals(2.25f, packet.kWndEndAngle)
        assertEquals(3.25f, packet.retardStep)
        assertEquals(4.25f, packet.advanceStep)
        assertEquals(5.25f, packet.maxRetard)
        assertEquals(6.25f, packet.threshold)
        assertEquals(7, packet.recoveryDelay)
        assertEquals(8.25f, packet.knkctlThrd)
    }

    @Test
    fun enableChannelsAndValidSelectorsEachSendOnceAndInvalidSelectionsAreIgnored() {
        val sent = mutableListOf<KnockParamPacket>()
        val holder = holder(sent)
        val packet = KnockParamPacket(selectedChanels = 0x80)
        holder.onPacketReceived(packet)

        holder.updateEnabled(true)
        assertEquals(1, packet.useKnockChannel)
        assertTrue((holder.state as KnockUiState.Data).enabled)
        assertEquals(1, sent.size)
        holder.updateEnabled(false)
        assertEquals(0, packet.useKnockChannel)
        assertFalse((holder.state as KnockUiState.Data).enabled)
        assertEquals(2, sent.size)

        holder.updateChannel(0, true)
        assertEquals(0x81, packet.selectedChanels)
        assertTrue((holder.state as KnockUiState.Data).values.channels()[0])
        assertEquals(3, sent.size)
        holder.updateChannel(0, false)
        assertEquals(0x80, packet.selectedChanels)
        assertFalse((holder.state as KnockUiState.Data).values.channels()[0])
        assertEquals(4, sent.size)

        holder.updateSelector(KnockSelector.BPF, 2)
        assertEquals(2, packet.bpfFrequency)
        assertEquals("1.31", (holder.state as KnockUiState.Data).values.selector(KnockSelector.BPF))
        assertEquals(5, sent.size)
        holder.updateSelector(KnockSelector.INTEGRATION, 3)
        assertEquals(3, packet.intTimeCost)
        assertEquals("55", (holder.state as KnockUiState.Data).values.selector(KnockSelector.INTEGRATION))
        assertEquals(6, sent.size)

        holder.updateChannel(8, true)
        holder.updateSelector(KnockSelector.BPF, bpfOptions.size)
        holder.updateSelector(KnockSelector.INTEGRATION, -1)
        assertEquals(6, sent.size)
    }

    @Test
    fun packetMappingPreservesUnknownSelectorCodesForDisplay() {
        val holder = holder(mutableListOf())
        holder.onPacketReceived(KnockParamPacket(bpfFrequency = 99, intTimeCost = -1))
        val values = (holder.state as KnockUiState.Data).values
        assertEquals("Code 99", values.selector(KnockSelector.BPF))
        assertEquals("Code -1", values.selector(KnockSelector.INTEGRATION))
    }
}
