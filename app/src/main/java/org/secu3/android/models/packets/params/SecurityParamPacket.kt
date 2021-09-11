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
package org.secu3.android.models.packets.params

import org.secu3.android.models.packets.BaseOutputPacket

data class SecurityParamPacket(

    var btFlags: Int = 0,

    var iButton0: String = "000000",
    var iButton1: String = "000000"

) : BaseOutputPacket() {

    val useBt: Boolean                                  // specifies to use or not to use bluetooth
        get() = btFlags.getBitValue(0) > 0

    val setBtBaudRate: Boolean                          // indicates that bluetooth baud rate has to be set during start up
        get() = btFlags.getBitValue(1) > 0

    val useImmobilizer: Boolean                         // specifies to use or not to use immobilizer
        get() = btFlags.getBitValue(2) > 0

    val useReserveParams: Boolean                       // Use reserve parameters instead of parameters stored in the EEPROM
        get() = btFlags.getBitValue(3) > 0

    val checkFwCrc: Boolean                             //Check firmware CRC (time consuming operation)
        get() = btFlags.getBitValue(4) > 0

    val btType: Int                                     // Bluetooth chip type: 0 - BC417, 1 - BK3231
        get() = btFlags.getBitValue(5)

    override fun pack(): String {
        var data = "$OUTPUT_PACKET_SYMBOL$DESCRIPTOR"

        data += btFlags.toChar()

        data += iButton0
        data += iButton1

        return data
    }

    companion object {

        internal const val DESCRIPTOR = '#'

        internal const val IBTN_KEY_SIZE = 6

        fun parse(data: String) = SecurityParamPacket().apply {
            btFlags = data[4].toInt()
            iButton0 = data.substring(5, 11)
            iButton1 = data.substring(11, 17)
        }

    }

}
