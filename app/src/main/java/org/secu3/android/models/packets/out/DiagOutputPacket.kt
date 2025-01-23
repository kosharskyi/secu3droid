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

import org.secu3.android.models.packets.base.BaseOutputPacket
import org.secu3.android.utils.getBitValue
import org.secu3.android.utils.setBitValue
import kotlin.math.roundToInt

class DiagOutputPacket : BaseOutputPacket() {

    var out: Int = 0
    var frq: Float = 10.0f
    var duty: Float = 0.0f
    var chan: Int = 0

    //SECU-3T (13 values):
    // IGN_OUT1, IGN_OUT2, IGN_OUT3, IGN_OUT4, IE, FE, ECF, CE, ST_BLOCK, ADD_O1, ADD_O2, BL, DE

    //SECU-3i (23 values):
    // IGN_O1, IGN_O2, IGN_O3, IGN_O4, IGN_O5, ECF, INJ_O1, INJ_O2, INJ_O3, INJ_O4, INJ_O5, BL, DE, STBL_O, CEL_O, FPMP_O, PWRR_O, EVAP_O, O2SH_O, COND_O, ADD_O2, TACH_O, GPA6_O


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





    var ecf: Boolean
        get() = out.getBitValue(6) > 0
        set(value) {
            out = out.setBitValue(value, 6)
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






    var ecf0: Boolean
        get() = out.getBitValue(5) > 0
        set(value) {
            out = out.setBitValue(value, 5)
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


    var bl: Boolean
        get() = out.getBitValue(11) > 0
        set(value) {
            out = out.setBitValue(value, 11)
            out = out.setBitValue(value, 12)
        }

    var de: Boolean
        get() = out.getBitValue(13) > 0
        set(value) {
            out = out.setBitValue(value, 13)
            out = out.setBitValue(value, 14)
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

    var tachO: Boolean          // special
        get() = out.getBitValue(23) > 0
        set(value) {
            out = out.setBitValue(value, 23)
            out = out.setBitValue(value, 24)
        }

    var gpa6_O: Boolean          // special
        get() = out.getBitValue(25) > 0
        set(value) {
            out = out.setBitValue(value, 25)
        }


    override fun pack(): IntArray {
        var data = intArrayOf(
            DESCRIPTOR.code,
        )

        data += out.write4Bytes()
        data += 1.0f.div(frq).times(524288.0f).roundToInt().write2Bytes()

        data += duty.div(100.0f).times(255.0f).roundToInt().write1Byte()
        data += chan.write1Byte()

        return data
    }

    companion object {

        internal const val DESCRIPTOR = '^'

    }
}
