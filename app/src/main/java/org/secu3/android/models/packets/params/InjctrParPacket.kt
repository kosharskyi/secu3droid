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
import kotlin.math.roundToInt

data class InjctrParPacket(

    var flags: Int = 0,

    var config: IntArray = IntArray(2),

    var flowRate0: Float = 0f,
    var flowRate1: Float = 0f,

    var cylDisp: Float = 0f,

    var sdIglConst0: Int = 0,
    var sdIglConst1: Int = 0,

    var ckpsEngineCyl: Int = 0,

    var timing0: Int = 0,
    var timing1: Int = 0,

    var timingCrk0: Int = 0,
    var timingCrk1: Int = 0,

    var angleSpec: Int = 0,

    var fffConst: Int = 0,

    var minPw: Int = 0

) : BaseOutputPacket() {

    override fun pack(): String {
        var data = "$OUTPUT_PACKET_SYMBOL$DESCRIPTOR"

        data += flags.toChar()

        data += config[0].toChar()
        data += config[1].toChar()

        data += flowRate0.times(64).toInt().write2Bytes()
        data += flowRate1.times(64).toInt().write2Bytes()

        data += cylDisp.div(4).times(16384).toInt().write2Bytes()

        data += sdIglConst0.write4Bytes()
        data += sdIglConst1.write4Bytes()

        data += ckpsEngineCyl.toChar()

        data += timing0.times(PARINJTIM_DIVIDER).write2Bytes()
        data += timing1.times(PARINJTIM_DIVIDER).write2Bytes()

        data += timingCrk0.times(PARINJTIM_DIVIDER).write2Bytes()
        data += timingCrk1.times(PARINJTIM_DIVIDER).write2Bytes()

        data += angleSpec.toChar()

        data += fffConst.toFloat().div(1000f*60f).times(65536f).toInt().write2Bytes()
        data += minPw.write2Bytes()

        data += END_PACKET_SYMBOL
        return data
    }

    var isAtMega644: Boolean = false

    var config0: Int
        get() = config[0].shr(4)
        set(value) {
            config[0] = config[0].and(0x0F).or(value.shl(4))
        }

    var config0Pulses: Int
        get() = config[0].and(0xF)
        set(value) {
            config[0] = config[0].and(0xF0).or(value)
        }

    var config1: Int
        get() = config[1].shr(4)
        set(value) {
            config[1] = config[1].and(0x0F).or(value.shl(4))
        }

    var config1Pulses: Int
        get() = config[1].and(0xF)
        set(value) {
            config[1] = config[1].and(0xF0).or(value)
        }


    var angleSpec0: Int
        get() = angleSpec.and(0xF)
        set(value) {
            angleSpec = angleSpec.and(0xF0).or(value)
        }

    var angleSpec1: Int
        get() = angleSpec.shr(4)
        set(value) {
            angleSpec = angleSpec.and(0x0F).or(value.shl(4))
        }


    private val discrete: Float
        get() {
            if (isAtMega644) {
                return 3.2f
            }
            return 4.0f
        }

    var minPw0: Float
        get() = minPw.and(0xFF).toFloat().times(discrete).div(1000.0f).times(8.0f)
        set(value) {
            minPw = value.div(8.0f).times(1000.0f).div(discrete).toInt().or(minPw.and(0xFF00))
        }

    var minPw1: Float
        get() = minPw.shr(8).toFloat().times(discrete).div(1000.0f).times(8.0f)
        set(value) {
            minPw = value.div(8.0f).times(1000.0f).div(discrete).toInt().shl(8).or(minPw.and(0x00FF))
        }


    var useTimingMap: Boolean
        get() = flags.getBitValue(0) > 0
        set(value) {
            flags = if (value) {
                1.or(flags)
            } else {
                1.inv().and(flags)
            }
        }

    var useTimingMapG: Boolean
        get() = flags.getBitValue(1) > 0
        set(value) {
            flags = if (value) {
                1.shl(1).or(flags)
            } else {
                1.shl(1).inv().and(flags)
            }
        }

    var useAdditionalCorrections: Boolean
        get() = flags.getBitValue(2) > 0
        set(value) {
            flags = if (value) {
                1.shl(2).or(flags)
            } else {
                1.shl(2).inv().and(flags)
            }
        }

    var useAirDensity: Boolean
        get() = flags.getBitValue(3) > 0
        set(value) {
            flags = if (value) {
                1.shl(3).or(flags)
            } else {
                1.shl(3).inv().and(flags)
            }
        }

    var useDifferentialPressure: Boolean
        get() = flags.getBitValue(4) > 0
        set(value) {
            flags = if (value) {
                1.shl(4).or(flags)
            } else {
                1.shl(4).inv().and(flags)
            }
        }

    var switchSecondInjRow: Boolean
        get() = flags.getBitValue(5) > 0
        set(value) {
            flags = if (value) {
                1.shl(5).or(flags)
            } else {
                1.shl(5).inv().and(flags)
            }
        }

    companion object {

        internal const val DESCRIPTOR = ';'

        fun parse(data: String) = InjctrParPacket().apply {

            flags = data[2].toInt()

            config[0] = data[3].toInt()
            config[1] = data[4].toInt()
            
            flowRate0 = data.get2Bytes(5).toFloat() / 64
            flowRate1 = data.get2Bytes(7).toFloat() / 64
            
            cylDisp = data.get2Bytes(9).toFloat() / 16384 * 4

            sdIglConst0 = data.get4Bytes(11)
            sdIglConst1 = data.get4Bytes(15)

            ckpsEngineCyl = data[19].toInt()

            timing0 = data.get2Bytes(20) / PARINJTIM_DIVIDER  //FIXME change constant
            timing1 = data.get2Bytes(22) / PARINJTIM_DIVIDER  //FIXME change constant

            timingCrk0 = data.get2Bytes(24) / PARINJTIM_DIVIDER   //FIXME change constant
            timingCrk1 = data.get2Bytes(26) / PARINJTIM_DIVIDER   //FIXME change constant

            angleSpec = data[28].toInt()

            fffConst = data.get2Bytes(29).toFloat().div(65536f).times(1000*60).roundToInt()
            minPw = data.get2Bytes(31)

        }
    }

}
