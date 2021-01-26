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

data class InjctrParPacket(

    var flags: Int = 0,

    var config: IntArray = IntArray(2),

    var flowRate0: Int = 0,
    var flowRate1: Int = 0,

    var cylDisp: Float = 0f,

    var sdIglConst0: Int = 0,
    var sdIglConst1: Int = 0,

    var ckpsEngineCyl: Int = 0,

    var timing0: Float = 0f,
    var timing1: Float = 0f,

    var timingCrk0: Float = 0f,
    var timingCrk1: Float = 0f,

    var angleSpec: Int = 0,

    var fffConst: Int = 0,

    var minPw: Int = 0

) : BaseOutputPacket() {

    override fun pack(): String {
        TODO("Not yet implemented")
    }

    var isAtMega644: Boolean = false

    val config0: Int
        get() = config[0].shr(4)

    val config0Pulses: Int
        get() = config[0].and(15)

    val config1: Int
        get() = config[1].shr(4)

    val config1Pulses: Int
        get() = config[1].and(15)


    val angleSpec0: Int
        get() = angleSpec.and(15)

    val angleSpec1: Int
        get() = angleSpec.shr(4)


    private val discrete: Float
        get() {
            if (isAtMega644) {
                return 3.2f
            }
            return 4.0f
        }

    val minPw0: Float
        get() = minPw.and(255).toFloat().times(discrete).div(1000.0f).times(8.0f)

    val minPw1: Float
        get() = minPw.shr(8).toFloat().times(discrete).div(1000.0f).times(8.0f)


    val useTimingMap: Boolean
        get() = flags.getBitValue(0) > 0

    val useTimingMapG: Boolean
        get() = flags.getBitValue(1) > 0

    val useAdditionalCorrections: Boolean
        get() = flags.getBitValue(2) > 0

    val useAirDensity: Boolean
        get() = flags.getBitValue(3) > 0

    val useDifferentialPressure: Boolean
        get() = flags.getBitValue(4) > 0

    val switchSecondInjRow: Boolean
        get() = flags.getBitValue(5) > 0

    companion object {

        internal const val DESCRIPTOR = ';'

        fun parse(data: String) = InjctrParPacket().apply {

            flags = data[2].toInt()

            config[0] = data[3].toInt()
            config[1] = data[4].toInt()
            
            flowRate0 = data.get2Bytes(5) / 64
            flowRate1 = data.get2Bytes(7) / 64
            
            cylDisp = data.get2Bytes(9).toFloat() / 16384 * 4

            sdIglConst0 = data.get4Bytes(11)
            sdIglConst1 = data.get4Bytes(15)

            ckpsEngineCyl = data[19].toInt()

            timing0 = data.get2Bytes(20).toFloat() / ANGLE_DIVIDER
            timing1 = data.get2Bytes(22).toFloat() / ANGLE_DIVIDER

            timingCrk0 = data.get2Bytes(24).toFloat() / ANGLE_DIVIDER
            timingCrk1 = data.get2Bytes(26).toFloat() / ANGLE_DIVIDER

            angleSpec = data[28].toInt()

            fffConst = data.get2Bytes(29)
            minPw = data.get2Bytes(31)

        }
    }

}
