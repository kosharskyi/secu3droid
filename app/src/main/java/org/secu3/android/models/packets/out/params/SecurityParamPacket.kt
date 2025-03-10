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

import org.secu3.android.models.packets.base.Secu3Packet
import org.secu3.android.models.packets.base.InputPacket
import org.secu3.android.models.packets.base.OutputPacket
import org.secu3.android.utils.getBitValue
import org.secu3.android.utils.setBitValue

data class SecurityParamPacket(

    var btName: String = "",    // max length 8 character
    var btPass: String = "",    // max length 8 character

    var btFlags: Int = 0,

    var iButton0: List<Int> = emptyList(),
    var iButton1: List<Int> = emptyList(),

) : Secu3Packet(), InputPacket, OutputPacket {

    var useBt: Boolean                                  // specifies to use or not to use bluetooth
        get() = btFlags.getBitValue(0) > 0
        set(value) {
            btFlags = btFlags.setBitValue(value, 0)
        }

    val setBtBaudRate: Boolean                          // indicates that bluetooth baud rate has to be set during start up
        get() = btFlags.getBitValue(1) > 0

    var useImmobilizer: Boolean                         // specifies to use or not to use immobilizer
        get() = btFlags.getBitValue(2) > 0
        set(value) {
            btFlags = btFlags.setBitValue(value, 2)
        }

    val useReserveParams: Boolean                       // Use reserve parameters instead of parameters stored in the EEPROM
        get() = btFlags.getBitValue(3) > 0

    val checkFwCrc: Boolean                             //Check firmware CRC (time consuming operation)
        get() = btFlags.getBitValue(4) > 0

    override fun pack(): IntArray {
        var data = intArrayOf(DESCRIPTOR.code)

        val nameLength = btName.length.coerceIn(0, 8)
        val passLength = btPass.length.coerceIn(0, 8)

        if (nameLength > 0 && passLength >= 4) {
            data += nameLength
            data += passLength
            data += btName.substring(0, nameLength).map { it.code }.toIntArray()
            data += btPass.substring(0, passLength).map { it.code }.toIntArray()
        } else {
            data += 0
            data += 0
        }

        data += btFlags

        data += iButton0
        data += iButton1

        data += unhandledParams

        return data
    }

    override fun parse(data: IntArray): InputPacket {
        //Number of characters in name (must be zero)
        val numNam = data.get1Byte()
        if (numNam > 0) {
            throw IllegalArgumentException("Bt name is not empty")
        }

        //Number of characters in password (must be zero)
        val numPass = data.get1Byte()
        if (numPass > 0) {
            throw IllegalArgumentException("Bt password is not empty")
        }

        btFlags = data.get1Byte()

        for (i in 0 until IBTN_KEY_SIZE) {
            iButton0 += data.get1Byte()
        }

        for (i in 0 until IBTN_KEY_SIZE) {
            iButton1 += data.get1Byte()
        }

        data.setUnhandledParams()

        return this
    }

    companion object {

        internal const val DESCRIPTOR = '#'

        private const val IBTN_KEY_SIZE = 6
    }
}
