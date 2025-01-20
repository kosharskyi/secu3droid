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
package org.secu3.android.models.packets.out.params

import org.secu3.android.models.packets.base.BaseOutputPacket
import org.secu3.android.utils.getBitValue

data class SecurityParamPacket(

    var btFlags: Int = 0,

    var iButton0: String = "000000",
    var iButton1: String = "000000",

    var btType: Int = 0 // Bluetooth chip type: 0 - BC417, 1 - BK3231, 2 - BK3231S(JDY-31), 3 - BC352(HC-05), 4 - BK3432, 5 - BK3431S

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

    override fun pack(): String {
        var data = "$OUTPUT_PACKET_SYMBOL$DESCRIPTOR"

        data += btFlags.toChar()

        data += iButton0
        data += iButton1

        data += btType.toChar()

        data += unhandledParams

        return data
    }

    companion object {

        internal const val DESCRIPTOR = '#'

        private const val IBTN_KEY_SIZE = 6

        fun parse(data: String) = SecurityParamPacket().apply {
            data.get1Byte()
            data.get1Byte()
            btFlags = data.get1Byte()
            iButton0 = data.getString(IBTN_KEY_SIZE)
            iButton1 = data.getString(IBTN_KEY_SIZE)
            btType = data.get1Byte()

            data.setUnhandledParams()
        }
    }
}
