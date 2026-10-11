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
 *    contacts: http://secu-3.org, vetalkosharskiy@gmail.com
 */
package org.secu3.android.models.packets.out.params

import org.junit.Assert.*
import org.junit.Test
import java.nio.charset.Charset

class SecurityParamPacketTest {
    private fun packet() = SecurityParamPacket(iButton0 = listOf(1, 2, 3, 4, 5, 6), iButton1 = listOf(10, 11, 12, 13, 14, 15))

    @Test fun credentialEncodingAndUnknownDataSurviveSnapshot() {
        val p = packet().apply { btName = "ТЕСТ"; btPass = "123456"; btFlags = 0xA0; btType = 99 }
        val wire = p.pack()
        assertEquals(listOf(4, 6), wire.slice(1..2))
        assertEquals("ТЕСТ".toByteArray(Charset.forName("IBM866")).map { it.toInt() and 255 }, wire.slice(3..6))
        val snapshot = p.packedSnapshot()
        p.btType = 1
        assertEquals(99, snapshot.pack()[wire.size - 1])
        assertEquals(0xA0, wire[13])
    }

    @Test fun credentialsRequireProtocolSafeValuesAndNeverTruncate() {
        listOf("12a4", "123", "123456789").forEach { invalid ->
            val p = packet().apply { btName = "name"; btPass = invalid }
            assertThrows(IllegalArgumentException::class.java) { p.pack() }
        }
        assertThrows(IllegalArgumentException::class.java) { packet().apply { btName = "😀"; btPass = "1234" }.pack() }
        assertThrows(IllegalArgumentException::class.java) { packet().apply { iButton0 = listOf(1) }.pack() }
    }

    @Test fun parsedFlagsKeysAndTailArePreserved() {
        val raw = intArrayOf('@'.code, '#'.code, 0, 0, 0xA5, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 0xFE, 0x77)
        val p = SecurityParamPacket().parse(raw) as SecurityParamPacket
        assertEquals(0xA5, p.btFlags)
        assertEquals(listOf(1, 2, 3, 4, 5, 6), p.iButton0)
        assertEquals(13, p.btType)
        assertArrayEquals(raw.copyOfRange(1, raw.size), p.pack())
    }

    @Test fun credentialSnapshotIsOneShotAndRetainsUnknownPacketTail() {
        val raw = intArrayOf('@'.code, '#'.code, 0, 0, 0xA5, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 0xFE, 0x77)
        val p = SecurityParamPacket().parse(raw) as SecurityParamPacket
        val credentialPacket = p.credentialsSnapshot("ECU", "1234").pack()
        assertEquals(3, credentialPacket[1])
        assertEquals(4, credentialPacket[2])
        val laterPacket = p.packedSnapshot().pack()
        assertEquals(0, laterPacket[1])
        assertEquals(0, laterPacket[2])
        assertArrayEquals(raw.copyOfRange(1, raw.size), laterPacket)
    }
}
