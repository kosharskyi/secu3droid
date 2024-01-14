/*
 *    SecuDroid  - An open source, free manager for SECU-3 engine control unit
 *    Copyright (C) 2024 Vitaliy O. Kosharskyi. Ukraine, Kyiv
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
package org.secu3.android.models.packets.params

import org.secu3.android.models.packets.BaseOutputPacket
import org.secu3.android.utils.getBitValue
import org.secu3.android.utils.setBitValue

data class CkpsParamPacket(

    var ckpsCogsBtdc: Int = 0,

    var ckpsIgnitCogs: Int = 0,

    var ckpsEngineCyl: Int = 0,

    var ckpsCogsNum: Int = 0,

    var ckpsMissNum: Int = 0,

    var hallFlags: Int = 0,

    var hallWndWidth: Float = 0f,

    var hallDegreesBtdc: Float = 0f

) : BaseOutputPacket() {

    var risingSpark: Boolean
        get() = hallFlags.getBitValue(0) > 0
        set(value) {
            hallFlags = hallFlags.setBitValue(value, 0)
        }

    var useCamRef: Boolean
        get() = hallFlags.getBitValue(1) > 0
        set(value) {
            hallFlags.setBitValue(value, 1)
        }

    var ckpsEdge: Boolean
        get() = hallFlags.getBitValue(2) > 0
        set(value) {
            hallFlags = hallFlags.setBitValue(value, 2)
        }

    var refsEdge: Boolean
        get() = hallFlags.getBitValue(3) > 0
        set(value) {
            hallFlags = hallFlags.setBitValue(value, 3)
        }

    var mergeOuts: Boolean
        get() = hallFlags.getBitValue(4) > 0
        set(value) {
            hallFlags = hallFlags.setBitValue(value, 4)
        }

    override fun pack(): String {
        var data = "$OUTPUT_PACKET_SYMBOL$DESCRIPTOR"

        data += ckpsCogsBtdc.toChar()
        data += ckpsIgnitCogs.toChar()
        data += ckpsEngineCyl.toChar()
        data += ckpsCogsNum.toChar()
        data += ckpsMissNum.toChar()
        data += hallFlags.toChar()

        data += hallWndWidth.times(ANGLE_DIVIDER).toInt().write2Bytes()
        data += hallDegreesBtdc.times(ANGLE_DIVIDER).toInt().write2Bytes()

        data += unhandledParams

        return data
    }

    companion object {

        internal const val DESCRIPTOR = 't'

        fun parse(data: String) = CkpsParamPacket().apply {
            ckpsCogsBtdc = data[2].code
            ckpsIgnitCogs = data[3].code
            ckpsEngineCyl = data[4].code
            ckpsCogsNum = data[5].code
            ckpsMissNum = data[6].code
            hallFlags = data[7].code
            hallWndWidth = data.get2Bytes(8).toFloat() / ANGLE_DIVIDER
            hallDegreesBtdc = data.get2Bytes(10).toFloat() / ANGLE_DIVIDER

            if (data.length == 12) {
                return@apply
            }

            unhandledParams = data.substring(12)
        }
    }
}

