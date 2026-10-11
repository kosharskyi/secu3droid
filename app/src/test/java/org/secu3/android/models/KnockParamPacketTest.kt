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
package org.secu3.android.models

import org.junit.Assert.*
import org.junit.Test
import org.secu3.android.models.packets.out.params.KnockParamPacket
import org.secu3.android.ui.parameters.pages.compose.KnockNumber
import org.secu3.android.ui.parameters.pages.compose.KnockValues
import org.secu3.android.ui.parameters.pages.compose.applyKnockNumber

class KnockParamPacketTest {
    @Test fun selectionUpdatesSingleBitAndPreservesOtherBits() {
        val packet = KnockParamPacket(selectedChanels = 0xA5)
        for (bit in 0..7) {
            val before = packet.selectedChanels
            packet.selectKnockChanel(bit, true)
            assertEquals(before or (1 shl bit), packet.selectedChanels)
            packet.selectKnockChanel(bit, false)
            assertEquals(before and (1 shl bit).inv(), packet.selectedChanels)
        }
    }

    @Test fun numericFieldMetadataMatchesKnockControls() {
        with(org.secu3.android.ui.parameters.pages.compose.KnockNumber.BEGIN) { assertEquals(-12f, min); assertEquals(54f, max); assertEquals(1f, step); assertEquals(2, precision) }
        with(org.secu3.android.ui.parameters.pages.compose.KnockNumber.END) { assertEquals(-12f, min); assertEquals(54f, max); assertEquals(1f, step); assertEquals(2, precision) }
        with(org.secu3.android.ui.parameters.pages.compose.KnockNumber.RETARD) { assertEquals(0f, min); assertEquals(20f, max); assertEquals(.25f, step); assertEquals(2, precision) }
        with(org.secu3.android.ui.parameters.pages.compose.KnockNumber.ADVANCE) { assertEquals(0f, min); assertEquals(5f, max); assertEquals(.2f, step); assertEquals(2, precision) }
        with(org.secu3.android.ui.parameters.pages.compose.KnockNumber.MAX_RETARD) { assertEquals(0f, min); assertEquals(25f, max); assertEquals(.25f, step); assertEquals(2, precision) }
        with(org.secu3.android.ui.parameters.pages.compose.KnockNumber.THRESHOLD) { assertEquals(.1f, min); assertEquals(5f, max); assertEquals(.01f, step); assertEquals(2, precision) }
        with(org.secu3.android.ui.parameters.pages.compose.KnockNumber.DELAY) { assertEquals(1f, min); assertEquals(99f, max); assertEquals(1f, step); assertEquals(0, precision) }
        with(org.secu3.android.ui.parameters.pages.compose.KnockNumber.CONTROL) { assertEquals(-40f, min); assertEquals(120f, max); assertEquals(.25f, step); assertEquals(2, precision) }
    }

    @Test fun applyingEachNumberWritesOnlyItsPacketFieldAndPreservesUnhandledTail() {
        val raw = intArrayOf(
            '@'.code, 'w'.code, 1, 3, 0xFF, 0x88, 0x01, 0x2C, 4,
            0x00, 0xFA, 0x01, 0x2C, 0x02, 0x58, 0x01, 0xF4,
            7, 0xA5, 0x01, 0x90, 0xCA, 0xFE,
        )

        KnockNumber.entries.forEachIndexed { index, number ->
            val packet = KnockParamPacket().parse(raw.copyOf()) as KnockParamPacket
            val originalValues = KnockValues.from(packet, emptyList(), emptyList())
            val originalUseKnockChannel = packet.useKnockChannel
            val originalBpfFrequency = packet.bpfFrequency
            val originalIntegration = packet.intTimeCost
            val originalChannels = packet.selectedChanels
            val newValue = number.min + (index + 1) * number.step

            applyKnockNumber(packet, number, newValue)

            val changedValues = KnockValues.from(packet, emptyList(), emptyList())
            KnockNumber.entries.forEach { field ->
                val expected = if (field == number) {
                    if (number == KnockNumber.DELAY) newValue.toInt().toFloat() else newValue
                } else {
                    originalValues.value(field)
                }
                assertEquals("$number changed $field", expected, changedValues.value(field), 0.0001f)
            }
            assertEquals(originalUseKnockChannel, packet.useKnockChannel)
            assertEquals(originalBpfFrequency, packet.bpfFrequency)
            assertEquals(originalIntegration, packet.intTimeCost)
            assertEquals(originalChannels, packet.selectedChanels)
            assertArrayEquals(intArrayOf(0xCA, 0xFE), packet.pack().takeLast(2).toIntArray())
        }
    }

    @Test fun packetPacksAndParsesAllFieldsAndUnhandledTail() {
        val raw = intArrayOf('@'.code, 'w'.code, 1, 3, 0xFF, 0x88, 0x01, 0x2C, 4, 0x00, 0xFA, 0x01, 0x2C, 0x02, 0x58, 0x01, 0xF4, 7, 0xA5, 0x01, 0x90, 0xCA, 0xFE)
        val p = KnockParamPacket().parse(raw) as KnockParamPacket
        assertEquals(1,p.useKnockChannel); assertEquals(3,p.bpfFrequency)
        assertEquals(-3.75f,p.kWndBeginAngle,0.001f); assertEquals(9.375f,p.kWndEndAngle,0.001f)
        assertEquals(4,p.intTimeCost); assertEquals(7.8125f,p.retardStep,0.001f)
        assertEquals(9.375f,p.advanceStep,0.001f); assertEquals(18.75f,p.maxRetard,0.001f)
        assertEquals(1.25f,p.threshold,0.001f); assertEquals(7,p.recoveryDelay)
        assertEquals(0xA5,p.selectedChanels); assertEquals(100f,p.knkctlThrd,0.001f)
        val values = org.secu3.android.ui.parameters.pages.compose.KnockValues.from(
            p, listOf("a", "b", "c", "high"), listOf("short", "x", "y", "z", "long")
        )
        assertEquals("high", values.selector(org.secu3.android.ui.parameters.pages.compose.KnockSelector.BPF))
        assertEquals("long", values.selector(org.secu3.android.ui.parameters.pages.compose.KnockSelector.INTEGRATION))
        assertArrayEquals(raw.copyOfRange(1,raw.size),p.pack())
        assertEquals(raw.takeLast(2), p.pack().takeLast(2))
        p.selectKnockChanel(0, false)
        val changed = org.secu3.android.ui.parameters.pages.compose.KnockValues.from(
            p, listOf("a", "b", "c", "high"), listOf("short", "x", "y", "z", "long")
        )
        assertFalse(changed.channels()[0])
        assertEquals(0xA4, changed.selectedChannels)
        assertEquals(100f, changed.value(org.secu3.android.ui.parameters.pages.compose.KnockNumber.CONTROL), 0.001f)
        assertArrayEquals(intArrayOf(0xCA, 0xFE), p.pack().takeLast(2).toIntArray())
        p.bpfFrequency = 99; p.intTimeCost = -1
        val unknown = org.secu3.android.ui.parameters.pages.compose.KnockValues.from(
            p, listOf("a", "b", "c", "high"), listOf("short", "x", "y", "z", "long")
        )
        assertEquals("Code 99", unknown.selector(org.secu3.android.ui.parameters.pages.compose.KnockSelector.BPF))
        assertEquals("Code -1", unknown.selector(org.secu3.android.ui.parameters.pages.compose.KnockSelector.INTEGRATION))
    }
}
