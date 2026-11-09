/*
 *    SecuDroid  - An open source, free manager for SECU-3 engine control unit
 *    Copyright (C) 2026 Vitalii O. Kosharskyi. Ukraine, Kyiv
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
package org.secu3.android.models.packets.input

import org.secu3.android.models.packets.base.InputPacket
import org.secu3.android.models.packets.base.Secu3Packet

data class LtftMapChunkPacket(
    var tableIndex: Int = 0,
    var address: Int = 0,
    var values: List<Float> = emptyList(),
) : Secu3Packet(), InputPacket {

    override fun parse(data: IntArray): InputPacket {
        require(data.size in MIN_PACKET_SIZE..MAX_PACKET_SIZE) {
            "LTFT map packet must contain a selector, address, and at most 16 values"
        }

        val parsedTableIndex = if ((data[2] and 0xFF) == 0) 0 else 1
        val parsedAddress = data[3] and 0xFF
        val valueCount = data.size - MIN_PACKET_SIZE
        require(parsedAddress + valueCount <= ADDRESS_SPACE_SIZE) {
            "LTFT map fragment exceeds the 256-entry table"
        }

        tableIndex = parsedTableIndex
        address = parsedAddress
        values = (4 until data.size).map { index ->
            data[index].toByte().toFloat() / LTFT_VALUE_MULTIPLIER * PERCENT_MULTIPLIER
        }
        return this
    }

    companion object {
        internal const val DESCRIPTOR = 'K'
        private const val MIN_PACKET_SIZE = 4 // two-byte frame header plus selector and address
        private const val MAX_VALUES = 16
        private const val MAX_PACKET_SIZE = MIN_PACKET_SIZE + MAX_VALUES
        private const val ADDRESS_SPACE_SIZE = 256
        private const val LTFT_VALUE_MULTIPLIER = 512f
        private const val PERCENT_MULTIPLIER = 100f
    }
}
