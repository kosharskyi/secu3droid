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

data class ChokeControlParPacket(

    var smSteps: Int = 0,
    var testing: Int = 0,           //fake parameter (actually it is status)
    var manualPositionD: Int = 0,   //fake parameter

    var rpmIf: Float = 0f,

    var corrTime0: Float = 0f,
    var corrTime1: Float = 0f,

    var flags: Int = 0,

    var smFreq: Int = 0,

    var injCrankToRunTime: Float = 0f

) : BaseOutputPacket() {

    var useClosedLoopRmpRegulator: Boolean
        get() = flags.getBitValue(0) > 0
        set(value) {
            flags = flags.setBitValue(value, 0)
        }

    var dontUseRpmRegOnGas: Boolean
        get() = flags.getBitValue(1) > 0
        set(value) {
            flags = flags.setBitValue(value, 1)
        }

    var useThrottlePosInChokeInit: Boolean
        get() = flags.getBitValue(2) > 0
        set(value) {
            flags = flags.setBitValue(value, 2)
        }

    var maxSTEPfreqAtInit: Boolean
        get() = flags.getBitValue(3) > 0
        set(value) {
            flags = flags.setBitValue(value, 3)
        }

    override fun pack(): IntArray {
        var data = intArrayOf(
            DESCRIPTOR.code
        )

        data += smSteps.write2Bytes()

        data += testing
        data += manualPositionD

        data += rpmIf.times(1024.0f).roundToInt().write2Bytes()
        data += corrTime0.times(100).roundToInt().write2Bytes()
        data += corrTime1.times(100).roundToInt().write2Bytes()
        data += flags
        data += smFreq
        data += injCrankToRunTime.times(100).roundToInt().write2Bytes()

        data += unhandledParams

        return data
    }

    companion object {

        internal const val DESCRIPTOR = '%'

        fun parse(data: IntArray) = ChokeControlParPacket().apply {

            smSteps = data.get2Bytes()
            data.get1Byte()     // testing fake param
            data.get1Byte()     // manual position fake param
            rpmIf = data.get2Bytes().toFloat() / 1024.0f
            corrTime0 = data.get2Bytes().toFloat() / 100
            corrTime1 = data.get2Bytes().toFloat() / 100
            flags = data.get1Byte()
            smFreq = data.get1Byte()
            injCrankToRunTime = data.get2Bytes().toFloat() / 100

            data.setUnhandledParams()
        }
    }
}
