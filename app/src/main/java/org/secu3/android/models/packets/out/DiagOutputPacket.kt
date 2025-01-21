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
package org.secu3.android.models.packets.out

import org.secu3.android.models.packets.input.FirmwareInfoPacket
import org.secu3.android.models.packets.base.BaseOutputPacket
import org.secu3.android.utils.getBitValue
import org.secu3.android.utils.setBitValue

class DiagOutputPacket(private val fwInfo: FirmwareInfoPacket) : BaseOutputPacket() {

    private var out: Int = 0
    private var frq: Int = 0
    private var duty: Int = 0
    private var chan: Int = 0




    var ignOut1: Boolean
        get() = out.getBitValue(0) > 0
        set(value) {
            out = out.setBitValue(value, 0)
        }

    var ignOut2: Boolean
        get() = out.getBitValue(1) > 0
        set(value) {
            out = out.setBitValue(value, 1)
        }

    var ignOut3: Boolean
        get() = out.getBitValue(2) > 0
        set(value) {
            out = out.setBitValue(value, 2)
        }

    var ignOut4: Boolean
        get() = out.getBitValue(3) > 0
        set(value) {
            out = out.setBitValue(value, 3)
        }

    var ignOut5: Boolean
        get() = out.getBitValue(4) > 0
        set(value) {
            out = out.setBitValue(value, 4)
        }

    var ie: Boolean
        get() = out.getBitValue(4) > 0
        set(value) {
            out = out.setBitValue(value, 4)
        }

    var fe: Boolean
        get() = out.getBitValue(5) > 0
        set(value) {
            out = out.setBitValue(value, 5)
        }



    private val ecfBitPosition: Int
        get() = if (fwInfo.isSecu3T) 6 else 5

    var ecf: Boolean
        get() = out.getBitValue(ecfBitPosition) > 0
        set(value) {
            out = out.setBitValue(value, ecfBitPosition)
        }

    var ce: Boolean
        get() = out.getBitValue(7) > 0
        set(value) {
            out = out.setBitValue(value, 7)
        }

    var stBlock: Boolean
        get() = out.getBitValue(8) > 0
        set(value) {
            out = out.setBitValue(value, 8)
        }

    var addIo1: Boolean
        get() = out.getBitValue(9) > 0
        set(value) {
            out = out.setBitValue(value, 9)
        }

    var addIo2: Boolean
        get() = out.getBitValue(10) > 0
        set(value) {
            out = out.setBitValue(value, 10)
        }





    var injO1: Boolean
        get() = out.getBitValue(6) > 0
        set(value) {
            out = out.setBitValue(value, 6)
        }

    var injO2: Boolean
        get() = out.getBitValue(7) > 0
        set(value) {
            out = out.setBitValue(value, 7)
        }

    var injO3: Boolean
        get() = out.getBitValue(8) > 0
        set(value) {
            out = out.setBitValue(value, 8)
        }

    var injO4: Boolean
        get() = out.getBitValue(9) > 0
        set(value) {
            out = out.setBitValue(value, 9)
        }

    var injO5: Boolean
        get() = out.getBitValue(10) > 0
        set(value) {
            out = out.setBitValue(value, 10)
        }




    var enableBlDeTesting: Boolean
        get() = out.getBitValue(12) > 0
        set(value) {
            if (value) {
                out = (1 shl 12).or(out)
                out = (1 shl 14).or(out)
            } else {
                out = (1 shl 12).inv().and(out)
                out = (1 shl 14).inv().and(out)
            }
        }

    var bl: Boolean
        get() = out.getBitValue(11) > 0
        set(value) {
            out = out.setBitValue(value, 11)
        }

    var de: Boolean
        get() = out.getBitValue(13) > 0
        set(value) {
            out = out.setBitValue(value, 13)
        }





    var stblO: Boolean
        get() = out.getBitValue(15) > 0
        set(value) {
            out = out.setBitValue(value, 15)
        }

    var celO: Boolean
        get() = out.getBitValue(16) > 0
        set(value) {
            out = out.setBitValue(value, 16)
        }

    var fpmpO: Boolean
        get() = out.getBitValue(17) > 0
        set(value) {
            out = out.setBitValue(value, 17)
        }

    var pwrrO: Boolean
        get() = out.getBitValue(18) > 0
        set(value) {
            out = out.setBitValue(value, 18)
        }

    var evapO: Boolean
        get() = out.getBitValue(19) > 0
        set(value) {
            out = out.setBitValue(value, 19)
        }

    var o2shO: Boolean
        get() = out.getBitValue(20) > 0
        set(value) {
            out = out.setBitValue(value, 20)
        }

    var condO: Boolean
        get() = out.getBitValue(21) > 0
        set(value) {
            out = out.setBitValue(value, 21)
        }

    var addO2: Boolean
        get() = out.getBitValue(22) > 0
        set(value) {
            out = out.setBitValue(value, 22)
        }


    var tachO: Boolean
        get() = out.getBitValue(23) > 0
        set(value) {
            out = out.setBitValue(value, 23)
        }


    var enableTachOtesting: Boolean
        get() = out.getBitValue(24) > 0
        set(value) {
            out = out.setBitValue(value, 24)
        }


    override fun pack(): IntArray {
        var data = intArrayOf(
            DESCRIPTOR.code,
        )

        data += out.write4Bytes()
        data += frq.write2Bytes()

        data += duty.write1Byte()
        data += chan.write1Byte()

        return data
    }

    companion object {

        internal const val DESCRIPTOR = '^'

    }
}
