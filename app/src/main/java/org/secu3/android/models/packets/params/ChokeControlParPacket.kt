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
            flags = if (value) {
                1.or(flags)
            } else {
                1.inv().and(flags)
            }
        }

    var dontUseRpmRegOnGas: Boolean
        get() = flags.getBitValue(1) > 0
        set(value) {
            flags = if (value) {
                1.shl(1).or(flags)
            } else {
                1.shl(1).inv().and(flags)
            }
        }

    var useThrottlePosInChokeInit: Boolean
        get() = flags.getBitValue(2) > 0
        set(value) {
            flags = if (value) {
                1.shl(2).or(flags)
            } else {
                1.shl(2).inv().and(flags)
            }
        }

    var maxSTEPfreqAtInit: Boolean
        get() = flags.getBitValue(3) > 0
        set(value) {
            flags = if (value) {
                1.shl(3).or(flags)
            } else {
                1.shl(3).inv().and(flags)
            }
        }

    override fun pack(): String {
        var data = "$OUTPUT_PACKET_SYMBOL$DESCRIPTOR"

        data += data.write2Bytes(smSteps)

        data += testing.toChar()
        data += manualPositionD.toChar()

        data += rpmIf.times(1000).toInt().write2Bytes()
        data += corrTime0.times(100).toInt().write2Bytes()
        data += corrTime1.times(100).toInt().write2Bytes()
        data += flags.toChar()
        data += smFreq.toChar()
        data += injCrankToRunTime.times(100).toInt().write2Bytes()

        return data
    }

    companion object {

        internal const val DESCRIPTOR = '%'

        fun parse(data: String) = ChokeControlParPacket().apply {

            smSteps = data.get2Bytes(2)
            rpmIf = data.get2Bytes(6).toFloat() / 1000
            corrTime0 = data.get2Bytes(8).toFloat() / 100
            corrTime1 = data.get2Bytes(10).toFloat() / 100
            flags = data[12].toInt()
            smFreq = data[13].toInt()
            injCrankToRunTime = data.get2Bytes(14).toFloat() / 100

        }
    }
}
