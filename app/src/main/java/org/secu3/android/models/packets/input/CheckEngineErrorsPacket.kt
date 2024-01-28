/*
 *    SecuDroid  - An open source, free manager for SECU-3 engine control unit
 *    Copyright (C) 2024 Vitalii O. Kosharskyi. Ukraine, Kyiv
 *
 *    SECU-3  - An open source, free engine control unit
 *    Copyright (C) 2007-2024 Alexey A. Shabelnikov. Ukraine, Kyiv
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

import org.secu3.android.models.packets.base.BaseSecu3Packet

data class CheckEngineErrorsPacket(

    var errors: Int = 0

) : BaseSecu3Packet() {

    fun isError(errorBit: Int): Boolean {
        val flags = errors shr errorBit
        return flags and 0x01 != 0
    }

    companion object {

        internal const val DESCRIPTOR = 'v'

        fun parse(data: String) = CheckEngineErrorsPacket().apply {
            errors = data.get4Bytes(2)
        }

    }
}