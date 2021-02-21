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

abstract class BaseOutputPacket : BaseSecu3Packet() {

    abstract fun pack(): String

    protected fun Int.write2Bytes(): String {
        var data = ""

        data += this.shr(8).and(0xFF).toChar()
        data += this.and(0xFF).toChar()

        return data
    }

    protected fun Int.write4Bytes(): String {
        var data = ""

        data += this.shr(24).and(0xFF).toChar()
        data += this.shr(16).and(0xFF).toChar()
        data += this.shr(8).and(0xFF).toChar()
        data += this.and(0xFF).toChar()

        return data
    }

    protected fun String.write2Bytes(valueToWrite: Int): String {
        var data = ""

        data += valueToWrite.shr(8).and(0xFF).toChar()
        data += valueToWrite.and(0xFF).toChar()

        return data
    }

    protected fun String.write4Bytes(valueToWrite: Int): String {
        var data = ""

        data += valueToWrite.shr(24).and(0xFF).toChar()
        data += valueToWrite.shr(16).and(0xFF).toChar()
        data += valueToWrite.shr(8).and(0xFF).toChar()
        data += valueToWrite.and(0xFF).toChar()

        return data
    }

}