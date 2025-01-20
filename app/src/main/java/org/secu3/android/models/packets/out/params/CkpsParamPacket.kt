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
import org.secu3.android.utils.setBitValue
import kotlin.math.roundToInt

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

    override fun pack(): IntArray {
        var data = intArrayOf(DESCRIPTOR.code)

        data += ckpsCogsBtdc
        data += ckpsIgnitCogs
        data += ckpsEngineCyl
        data += ckpsCogsNum
        data += ckpsMissNum
        data += hallFlags

        data += hallWndWidth.times(ANGLE_DIVIDER).roundToInt().write2Bytes()
        data += hallDegreesBtdc.times(ANGLE_DIVIDER).roundToInt().write2Bytes()

        data += unhandledParams

        return data
    }

    companion object {

        internal const val DESCRIPTOR = 't'

        fun parse(data: IntArray) = CkpsParamPacket().apply {
            ckpsCogsBtdc = data.get1Byte()
            ckpsIgnitCogs = data.get1Byte()
            ckpsEngineCyl = data.get1Byte()
            ckpsCogsNum = data.get1Byte()
            ckpsMissNum = data.get1Byte()
            hallFlags = data.get1Byte()
            hallWndWidth = data.get2Bytes().toFloat() / ANGLE_DIVIDER
            hallDegreesBtdc = data.get2Bytes().toFloat() / ANGLE_DIVIDER

            data.setUnhandledParams()
        }
    }
}

