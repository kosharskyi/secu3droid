/*
 *    SecuDroid  - An open source, free manager for SECU-3 engine control unit
 *    Copyright (C) 2026 Vitalii O. Kosharskyi. Ukraine, Kyiv
 *
 *    SECU-3  - An open source, free engine control unit
 *    Copyright (C) 2007-2026 Alexey A. Shabelnikov. Ukraine, Kyiv
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License
 *    as published by the Free Software Foundation, either version 3 of the
 *    License, or (at your option) any later version.
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
import org.secu3.android.models.packets.out.params.UniOutParamPacket

class UniversalOutputsStateHolderTest {
    @Test
    fun startsAndResetsLoadingWithoutSending() {
        val sent = mutableListOf<UniOutParamPacket>()
        val allowed = mutableListOf<Boolean>()
        val holder = UniversalOutputsStateHolder(sent::add, allowed::add)

        assertNull(holder.values)
        holder.updateOutput(1, UniversalOutputValues(true, 0, 0, false, false, false, 0, 0f, 0f, 0f, 0f))
        holder.updatePairFunction(3)
        assertTrue(sent.isEmpty())

        holder.onPacketReceived(UniOutParamPacket())
        assertEquals(6, holder.values?.outputs?.size)
        holder.reset()
        assertNull(holder.values)
        holder.updatePairFunction(4)
        assertTrue(sent.isEmpty())
        assertEquals("Resetting an unloaded neighbor must not block the active page", listOf(false, true), allowed)
    }

    @Test
    fun receivesWithoutSendingAndEditsEachOutputAndPairOnceOnSamePacket() {
        val sent = mutableListOf<UniOutParamPacket>()
        val holder = UniversalOutputsStateHolder(sent::add)
        val packet = UniOutParamPacket().also {
            it.output6Use = true
            it.output6LogicFunc = 9
            it.logicFunction_1_2 = 2
        }
        holder.onPacketReceived(packet)
        assertTrue(sent.isEmpty())
        assertEquals(6, holder.values?.outputs?.size)

        val oldOutput = holder.values!!.outputs[4]
        val changed = oldOutput.copy(enabled = true, inversion = true, logicFunction = 7)
        holder.updateOutput(5, changed)
        assertEquals(1, sent.size)
        assertSame(packet, sent.single())
        assertTrue(packet.output5Use)
        assertTrue(packet.output5Inversion)
        assertEquals(7, packet.output5LogicFunc)
        assertTrue(packet.output6Use)
        assertEquals(9, packet.output6LogicFunc)

        holder.updatePairFunction(15)
        assertEquals(2, sent.size)
        assertSame(packet, sent.last())
        assertEquals(15, packet.logicFunction_1_2)
        assertEquals(15, holder.values?.logicFunction12)
    }

    @Test
    fun invalidOutputIndicesDoNotMutateOrSend() {
        val sent = mutableListOf<UniOutParamPacket>()
        val holder = UniversalOutputsStateHolder(sent::add)
        val packet = UniOutParamPacket()
        holder.onPacketReceived(packet)
        val original = packet.pack()
        val edited = holder.values!!.outputs.first().copy(enabled = true)

        holder.updateOutput(0, edited)
        holder.updateOutput(7, edited)

        assertTrue(sent.isEmpty())
        assertTrue(original.contentEquals(packet.pack()))
    }

    @Test
    fun editsPreserveUnknownPacketBitsAndTail() {
        val sent = mutableListOf<UniOutParamPacket>()
        val holder = UniversalOutputsStateHolder(sent::add)
        val payload = UniOutParamPacket().also {
            it.output2Use = true
            it.output6Cond2Inversion = true
        }.pack().drop(1).toIntArray()
        payload[1] = 250 // Unrecognized output 1 condition ID.
        payload[3] = 0xFE // Raw threshold byte for that unknown condition.
        payload[4] = 0xDC
        val packet = UniOutParamPacket().parse(intArrayOf(0x40, 0x21) + payload + intArrayOf(0xA5, 0x5A)) as UniOutParamPacket
        holder.onPacketReceived(packet)

        holder.updateOutput(3, holder.values!!.outputs[2].copy(enabled = true))

        val packed = packet.pack()
        assertEquals(250, packet.output1Condition1)
        assertTrue(packet.output2Use)
        assertTrue(packet.output6Cond2Inversion)
        assertEquals(listOf(0xA5, 0x5A), packed.takeLast(2))
        assertTrue(sent.single() === packet)
    }
}
