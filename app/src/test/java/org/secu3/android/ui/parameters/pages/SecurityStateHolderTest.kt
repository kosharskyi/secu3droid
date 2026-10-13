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
import org.junit.Assert.assertTrue
import org.junit.Test
import org.secu3.android.models.packets.base.OutputPacket
import org.secu3.android.models.packets.out.params.SecurityParamPacket

class SecurityStateHolderTest {
    private fun packet() = SecurityParamPacket(
        btFlags = 0xA0,
        iButton0 = listOf(1, 2, 3, 4, 5, 6),
        iButton1 = listOf(10, 11, 12, 13, 14, 15),
        btType = 4,
    )

    @Test fun stateLoadsFlagsAndReversedKeysAndDoesNotSendBeforeLoad() {
        val sent = mutableListOf<OutputPacket>()
        val state = SecurityStateHolder(sent::add, "invalid")
        state.changeType(2)
        state.changeBt(true)
        assertTrue(sent.isEmpty())

        state.load(packet())
        assertTrue(state.loaded)
        assertFalse(state.useBt)
        assertEquals(4, state.type)
        assertEquals("060504030201", state.key0)
        assertEquals("0F0E0D0C0B0A", state.key1)

        state.changeBt(true)
        val flags = sent.single().pack()[3]
        assertEquals(0xA3, flags) // preserves existing bits and enables BT and baud-rate bits
    }

    @Test fun validKeySendsReversedBytesAndSnapshotsDoNotChange() {
        val sent = mutableListOf<OutputPacket>()
        val state = SecurityStateHolder(sent::add, "invalid")
        val source = packet()
        state.load(source)
        state.changeKey0("112233445566")
        val snapshot = sent.single().pack()
        assertEquals(listOf(0x66, 0x55, 0x44, 0x33, 0x22, 0x11), snapshot.slice(4..9))

        state.changeType(1)
        assertEquals(4, snapshot[16])
        assertEquals(1, sent.last().pack()[16])
        assertEquals(listOf(0x66, 0x55, 0x44, 0x33, 0x22, 0x11), source.iButton0)
    }

    @Test fun loadedPacketRetainsUnknownTailWhenHolderChangesAValue() {
        val raw = intArrayOf('@'.code, '#'.code, 0, 0, 0xA5, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 0xFE, 0x77)
        val source = SecurityParamPacket().parse(raw) as SecurityParamPacket
        val sent = mutableListOf<OutputPacket>()
        val state = SecurityStateHolder(sent::add, "invalid")

        state.load(source)
        state.changeType(2)

        val updated = sent.single().pack()
        assertEquals(2, updated[16])
        assertEquals(listOf(0xFE, 0x77), updated.takeLast(2))
    }

    @Test fun credentialsAreValidatedRedactedAndClearedOnlyAfterSend() {
        val sent = mutableListOf<OutputPacket>()
        val source = packet()
        val state = SecurityStateHolder(sent::add, "invalid credentials")
        state.load(source)
        state.changeName("😀")
        state.changePass("1234")
        state.applyCredentials()
        assertEquals("invalid credentials", state.error)
        assertTrue(sent.isEmpty())
        assertEquals("", source.btName)
        assertEquals("", source.btPass)

        state.changeName("ecu")
        state.changePass("1234")
        state.applyCredentials()
        assertEquals(1, sent.size)
        assertEquals(3, sent.single().pack()[1])
        assertEquals(4, sent.single().pack()[2])
        assertEquals("", source.btName)
        assertEquals("", source.btPass)
        assertEquals("", state.name)
        assertEquals("", state.password)
        assertNull(parseIButtonKey("11223344556G"))
        assertEquals(listOf(0x66, 0x55, 0x44, 0x33, 0x22, 0x11), parseIButtonKey("112233445566"))
    }
}
