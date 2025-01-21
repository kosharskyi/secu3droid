/*
 *    SecuDroid  - An open source, free manager for SECU-3 engine control unit
 *    Copyright (C) 2025 Vitalii O. Kosharskyi. Ukraine, Kyiv
 *
 *    SECU-3  - An open source, free engine control unit
 *    Copyright (C) 2007-2025 Alexey A. Shabelnikov. Ukraine, Kyiv
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
package org.secu3.android.models.packets.out

import org.secu3.android.models.packets.base.BaseOutputPacket

data class CheckEngineSavedErrorsPacket(

    var errors: Int = 0

) : BaseOutputPacket() {

    fun isError(errorBit: Int): Boolean {
        val flags = errors shr errorBit
        return flags and 0x01 != 0
    }

    override fun pack(): IntArray {
        return intArrayOf(DESCRIPTOR.code) + errors.write4Bytes()
    }

    companion object {

        internal const val DESCRIPTOR = 'x'

        fun parse(data: IntArray) = CheckEngineSavedErrorsPacket().apply {
            errors = data.get4Bytes()
        }

    }
}