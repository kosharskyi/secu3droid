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

import androidx.annotation.StringRes
import org.secu3.android.R
import org.secu3.android.models.packets.base.BaseOutputPacket
import org.secu3.android.utils.getBitValue
import org.secu3.android.utils.setBitValue
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

    var minPw: Int = 0,

    var injMafConst: IntArray = IntArray(2),

    var mafloadConst: Int = 0,

    var injMaxPw: FloatArray = FloatArray(2),


) : BaseOutputPacket() {

    override fun pack(): String {
        var data = "$DESCRIPTOR"

        data += flags.toChar()

        data += config[0].toChar()
        data += config[1].toChar()

        data += flowRate[0].times(64).roundToInt().write2Bytes()
        data += flowRate[1].times(64).roundToInt().write2Bytes()

        data += cylDisp.times(16384).roundToInt().write2Bytes()

        data += sdIglConst[0].write4Bytes()
        data += sdIglConst[1].write4Bytes()

        data += ckpsEngineCyl.toChar()

        data += timing[0].times(PARINJTIM_DIVIDER).write2Bytes()
        data += timing[1].times(PARINJTIM_DIVIDER).write2Bytes()

        data += timingCrk[0].times(PARINJTIM_DIVIDER).write2Bytes()
        data += timingCrk[1].times(PARINJTIM_DIVIDER).write2Bytes()

        data += angleSpec.toChar()

        data += fffConst.toFloat().div(1000f*60f).times(65536f).roundToInt().write2Bytes()

        data += minPw.write2Bytes()


        data += injMafConst[0].write4Bytes()
        data += injMafConst[1].write4Bytes()
        data += mafloadConst.write4Bytes()

        data += injMaxPw[0].times(1000.0f / 3.2f).roundToInt().write2Bytes()
        data += injMaxPw[1].times(1000.0f / 3.2f).roundToInt().write2Bytes()

        data += unhandledParams

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
                return 4.0f
            }
            return 3.2f
        }

    var minPw0: Float
        get() = minPw.and(0xFF).toFloat().times(8.0f).times(discrete).div(1000.0f)
        set(value) {
            minPw = value.times(1000.0f).div(discrete).div(8.0f).roundToInt().or(minPw.and(0xFF00))
        }

    var minPw1: Float
        get() = minPw.shr(8).toFloat().times(8.0f).times(discrete).div(1000.0f)
        set(value) {
            minPw = value.times(1000.0f).div(discrete).div(8.0f).roundToInt().shl(8).or(minPw.and(0x00FF))
        }


    var useTimingMap: Boolean
        get() = flags.getBitValue(0) > 0
        set(value) {
            flags = flags.setBitValue(value, 0)
        }

    var useTimingMapG: Boolean
        get() = flags.getBitValue(1) > 0
        set(value) {
            flags = flags.setBitValue(value, 1)
        }

    var useAdditionalCorrections: Boolean
        get() = flags.getBitValue(2) > 0
        set(value) {
            flags = flags.setBitValue(value, 2)
        }

    var useAirDensity: Boolean
        get() = flags.getBitValue(3) > 0
        set(value) {
            flags = flags.setBitValue(value, 3)
        }

    var useDifferentialPressure: Boolean
        get() = flags.getBitValue(4) > 0
        set(value) {
            flags = flags.setBitValue(value, 4)
        }

    var switchSecondInjRow: Boolean
        get() = flags.getBitValue(5) > 0
        set(value) {
            flags = flags.setBitValue(value, 5)
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
            InjConfig.THROTTLEBODY.id -> 1
            InjConfig.SIMULTANEOUS.id -> ckpsEngineCyl
            InjConfig.TWO_BANK_ALTERN.id -> ckpsEngineCyl
            InjConfig.SEMISEQUENTIAL.id -> ckpsEngineCyl
            InjConfig.SEMISEQSEPAR.id -> ckpsEngineCyl
            InjConfig.FULLSEQUENTIAL.id -> ckpsEngineCyl
            else -> ckpsEngineCyl
        }
    }

    private fun bnkNum(config: Int): Int {
        return when (config) {
             InjConfig.THROTTLEBODY.id -> 1
            InjConfig.SIMULTANEOUS.id -> 1
            InjConfig.TWO_BANK_ALTERN.id -> 2
            InjConfig.SEMISEQUENTIAL.id -> ckpsEngineCyl / 2
            InjConfig.SEMISEQSEPAR.id -> ckpsEngineCyl / 2
            InjConfig.FULLSEQUENTIAL.id -> ckpsEngineCyl
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

    enum class InjConfig (val id: Int, @StringRes val strId: Int) {
        THROTTLEBODY(0, R.string.injctr_par_inj_config_throttlebody),  //single injector for N cylinders
        SIMULTANEOUS(1, R.string.injctr_par_inj_config_simultaneous),   //N injectors, all injectors work simultaneously
        TWO_BANK_ALTERN(2, R.string.injctr_par_inj_config_2banks_altern),   //N injectors split into 2 banks, banks work alternately
        SEMISEQUENTIAL(3, R.string.injctr_par_inj_config_semi_sequential), //N injectors, injectors work in pairs
        SEMISEQSEPAR(5, R.string.injctr_par_inj_config_ss_sep_channel),  //N injectors, injectors work in pairs, each injector has its own separate output
        FULLSEQUENTIAL(4, R.string.injctr_par_inj_config_full_sequential) //N injectors, each injector works 1 time per cycle
    }

    companion object {

        internal const val DESCRIPTOR = ';'

        private val fuelDensity = arrayOf(
            0.710f, //petrol density (0.710 g/cc)
            0.536f  //LPG density (0.536 g/cc)
        )

        fun parse(data: IntArray) = InjctrParPacket().apply {

            flags = data.get1Byte()

            config[0] = data.get1Byte()
            config[1] = data.get1Byte()
            
            flowRate[0] = data.get2Bytes().toFloat() / 64
            flowRate[1] = data.get2Bytes().toFloat() / 64
            
            cylDisp = data.get2Bytes().toFloat() / 16384

            sdIglConst[0] = data.get4Bytes()
            sdIglConst[1] = data.get4Bytes()

            ckpsEngineCyl = data.get1Byte()

            timing[0] = data.get2Bytes() / PARINJTIM_DIVIDER
            timing[1] = data.get2Bytes() / PARINJTIM_DIVIDER

            timingCrk[0] = data.get2Bytes() / PARINJTIM_DIVIDER
            timingCrk[1] = data.get2Bytes() / PARINJTIM_DIVIDER

            angleSpec = data.get1Byte()

            fffConst = data.get2Bytes().toFloat().div(65536f).times(1000*60).roundToInt()
            minPw = data.get2Bytes()

            injMafConst[0] = data.get4Bytes()
            injMafConst[1] = data.get4Bytes()

            mafloadConst = data.get4Bytes()

            injMaxPw[0] = data.get2Bytes().toFloat().times(3.2f / 1000.0f)
            injMaxPw[1] = data.get2Bytes().toFloat().times(3.2f / 1000.0f)

            data.setUnhandledParams()
        }
    }
}
