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

    var flowRate: FloatArray = FloatArray(2),

    private var cylDisp: Float = 0f,

    var sdIglConst: IntArray = IntArray(2),

    var ckpsEngineCyl: Int = 0,

    var timing: IntArray = IntArray(2),

    var timingCrk: IntArray = IntArray(2),

    var angleSpec: Int = 0,

    var fffConst: Int = 0,

    var minPw: Int = 0

) : BaseOutputPacket() {

    override fun pack(): String {
        var data = "$OUTPUT_PACKET_SYMBOL$DESCRIPTOR"

        data += flags.toChar()

        data += config[0].toChar()
        data += config[1].toChar()

        data += flowRate[0].times(64).toInt().write2Bytes()
        data += flowRate[1].times(64).toInt().write2Bytes()

        data += cylDisp.times(16384).toInt().write2Bytes()

        data += sdIglConst[0].write4Bytes()
        data += sdIglConst[1].write4Bytes()

        data += ckpsEngineCyl.toChar()

        data += timing[0].times(PARINJTIM_DIVIDER).write2Bytes()
        data += timing[1].times(PARINJTIM_DIVIDER).write2Bytes()

        data += timingCrk[0].times(PARINJTIM_DIVIDER).write2Bytes()
        data += timingCrk[1].times(PARINJTIM_DIVIDER).write2Bytes()

        data += angleSpec.toChar()

        data += fffConst.toFloat().div(1000f*60f).times(65536f).toInt().write2Bytes()
        data += minPw.write2Bytes()

        return data
    }

    var isAtMega644: Boolean = false

    var config0: Int
        get() = config[0].shr(4)
        set(value) {
            config[0] = config[0].and(0x0F).or(value.shl(4))
            configChanged(0, config0Pulses)
        }

    var config0Pulses: Int
        get() = config[0].and(0xF)
        set(value) {
            config[0] = config[0].and(0xF0).or(value)
            configChanged(0, value)
        }

    var config1: Int
        get() = config[1].shr(4)
        set(value) {
            config[1] = config[1].and(0x0F).or(value.shl(4))
            configChanged(1, config1Pulses)
        }

    var config1Pulses: Int
        get() = config[1].and(0xF)
        set(value) {
            config[1] = config[1].and(0xF0).or(value)
            configChanged(1, value)
        }

    var engineDisp: Float
        get() = cylDisp * ckpsEngineCyl
        set(value) {
            cylDisp = value / ckpsEngineCyl
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


    private fun configChanged(configNum: Int, pulses: Int) {

        val cfg = if (configNum == 0) config0 else config1

        val bnkNum = bnkNum(cfg).toFloat()
        val injNum = injNum(cfg).toFloat()
        val mifr = flowRate[configNum] * fuelDensity[configNum]

        sdIglConst[configNum] = (((cylDisp * 3.482f * 18750000f) / mifr) * (bnkNum * ckpsEngineCyl.toFloat()) / (injNum * pulses.toFloat())).toInt()
    }

    private fun injNum(config: Int): Int {
        return when (config) {
            INJCFG_THROTTLEBODY -> 1
            INJCFG_SIMULTANEOUS -> ckpsEngineCyl
            INJCFG_2BANK_ALTERN -> ckpsEngineCyl
            INJCFG_SEMISEQUENTIAL -> ckpsEngineCyl
            INJCFG_SEMISEQSEPAR -> ckpsEngineCyl
            INJCFG_FULLSEQUENTIAL -> ckpsEngineCyl
            else -> ckpsEngineCyl
        }
    }

    private fun bnkNum(config: Int): Int {
        return when (config) {
            INJCFG_THROTTLEBODY -> 1
            INJCFG_SIMULTANEOUS -> 1
            INJCFG_2BANK_ALTERN -> 2
            INJCFG_SEMISEQUENTIAL -> ckpsEngineCyl / 2
            INJCFG_SEMISEQSEPAR -> ckpsEngineCyl / 2
            INJCFG_FULLSEQUENTIAL -> ckpsEngineCyl
            else -> ckpsEngineCyl
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as InjctrParPacket

        if (flags != other.flags) return false
        if (!config.contentEquals(other.config)) return false
        if (!flowRate.contentEquals(other.flowRate)) return false
        if (cylDisp != other.cylDisp) return false
        if (!sdIglConst.contentEquals(other.sdIglConst)) return false
        if (ckpsEngineCyl != other.ckpsEngineCyl) return false
        if (!timing.contentEquals(other.timing)) return false
        if (!timingCrk.contentEquals(other.timingCrk)) return false
        if (angleSpec != other.angleSpec) return false
        if (fffConst != other.fffConst) return false
        if (minPw != other.minPw) return false
        if (isAtMega644 != other.isAtMega644) return false

        return true
    }

    override fun hashCode(): Int {
        var result = flags
        result = 31 * result + config.contentHashCode()
        result = 31 * result + flowRate.contentHashCode()
        result = 31 * result + cylDisp.hashCode()
        result = 31 * result + sdIglConst.contentHashCode()
        result = 31 * result + ckpsEngineCyl
        result = 31 * result + timing.contentHashCode()
        result = 31 * result + timingCrk.contentHashCode()
        result = 31 * result + angleSpec
        result = 31 * result + fffConst
        result = 31 * result + minPw
        result = 31 * result + isAtMega644.hashCode()
        return result
    }

    companion object {

        internal const val DESCRIPTOR = ';'

        private val fuelDensity = arrayOf(
            0.710f, //petrol density (0.710 g/cc)
            0.536f  //LPG density (0.536 g/cc)
        )

        const val INJCFG_THROTTLEBODY  = 0;  //single injector for N cylinders
        const val INJCFG_SIMULTANEOUS = 1;   //N injectors, all injectors work simultaneously
        const val INJCFG_2BANK_ALTERN = 2;   //N injectors split into 2 banks, banks work alternately
        const val INJCFG_SEMISEQUENTIAL = 3; //N injectors, injectors work in pairs
        const val INJCFG_FULLSEQUENTIAL = 4; //N injectors, each injector works 1 time per cycle
        const val INJCFG_SEMISEQSEPAR  = 5;  //N injectors, injectors work in pairs, each injector has its own separate output

        fun parse(data: String) = InjctrParPacket().apply {

            flags = data[2].toInt()

            config[0] = data[3].toInt()
            config[1] = data[4].toInt()
            
            flowRate[0] = data.get2Bytes(5).toFloat() / 64
            flowRate[1] = data.get2Bytes(7).toFloat() / 64
            
            cylDisp = data.get2Bytes(9).toFloat() / 16384

            sdIglConst[0] = data.get4Bytes(11)
            sdIglConst[1] = data.get4Bytes(15)

            ckpsEngineCyl = data[19].toInt()

            timing[0] = data.get2Bytes(20) / PARINJTIM_DIVIDER
            timing[1] = data.get2Bytes(22) / PARINJTIM_DIVIDER

            timingCrk[0] = data.get2Bytes(24) / PARINJTIM_DIVIDER
            timingCrk[1] = data.get2Bytes(26) / PARINJTIM_DIVIDER

            angleSpec = data[28].toInt()

            fffConst = data.get2Bytes(29).toFloat().div(65536f).times(1000*60).roundToInt()
            minPw = data.get2Bytes(31)

        }
    }

}
