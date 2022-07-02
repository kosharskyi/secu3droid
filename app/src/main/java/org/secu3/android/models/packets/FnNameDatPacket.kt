/* SecuDroid  - An open source, free manager for SECU-3 engine control unit
   Copyright (C) 2020 Vitaliy O. Kosharskiy. Ukraine, Kharkiv

   SECU-3  - An open source, free engine control unit
   Copyright (C) 2007 Alexey A. Shabelnikov. Ukraine, Kyiv

   This program is free software: you can redistribute it and/or modify
   it under the terms of the GNU General Public License as published by
   the Free Software Foundation, either version 3 of the License, or
   (at your option) any later version.

   This program is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.

   You should have received a copy of the GNU General Public License
   along with this program.  If not, see <http://www.gnu.org/licenses/>.

   contacts:
              http://secu-3.org
              email: vetalkosharskiy@gmail.com
*/
package org.secu3.android.models.packets

import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

data class FnNameDatPacket(

    var tablesNumber: Int = 0,

    var fnName: FnName = FnName(-1, "")

): BaseSecu3Packet() {

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

    companion object {

        private const val F_NAME_SIZE = 16          //!< number of symbols in names of tables' sets
        internal const val DESCRIPTOR = 'p'

        fun parse(data: String) = FnNameDatPacket().apply {
            tablesNumber = data[2].toInt()

            val name = data.substring(4, 4 +  F_NAME_SIZE).toByteArray(StandardCharsets.ISO_8859_1).toString(Charset.forName("IBM866"))

            fnName = FnName(data[3].toInt(), name)
        }

    }

}
