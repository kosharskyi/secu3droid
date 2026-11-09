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

data class LoadGridPacket(
    var values: List<Float> = emptyList(),
) : Secu3Packet(), InputPacket {

    override fun parse(data: IntArray): InputPacket {
        require(data.size == PACKET_SIZE) { "Load grid packet must contain exactly 16 values" }
        require((data[2] and 0xFF) == 0) { "Load grid address must be zero" }

        values = (0 until VALUE_COUNT).map { index ->
            val offset = DATA_OFFSET + index * BYTES_PER_VALUE
            val rawValue = (data[offset] and 0xFF) or ((data[offset + 1] and 0xFF) shl 8)
            rawValue.toFloat() / Secu3Packet.LOAD_PHYSICAL_MAGNITUDE_MULTIPLIER
        }
        return this
    }

    companion object {
        internal const val DESCRIPTOR = ')'
        private const val VALUE_COUNT = 16
        private const val BYTES_PER_VALUE = 2
        private const val DATA_OFFSET = 3
        private const val PACKET_SIZE = DATA_OFFSET + VALUE_COUNT * BYTES_PER_VALUE
    }
}
