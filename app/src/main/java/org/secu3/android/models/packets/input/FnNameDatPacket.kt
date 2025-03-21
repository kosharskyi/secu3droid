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

import org.secu3.android.models.FnName
import org.secu3.android.models.packets.base.Secu3Packet
import org.secu3.android.models.packets.base.InputPacket

data class FnNameDatPacket(

    var tablesNumber: Int = 0,

    var fnName: FnName = FnName(-1, "")

): Secu3Packet(), InputPacket {

    var fnNameList: MutableList<FnName> = mutableListOf()

    val isAllFnNamesReceived: Boolean
        get() {
            if (fnNameList.isEmpty()) {
                return false
            }

            for (fnName in fnNameList) {
                if (fnName.index == -1) {
                    return false
                }
            }
            return true
        }

    override fun parse(data: IntArray): InputPacket {
        tablesNumber = data.get1Byte()
        val index = data.get1Byte()

        val name = data.getString(F_NAME_SIZE)

        fnName = FnName(index, name)

        return this
    }

    companion object {

        private const val F_NAME_SIZE = 16          //!< number of symbols in names of tables' sets
        internal const val DESCRIPTOR = 'p'

    }

}
