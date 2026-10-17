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
import org.secu3.android.models.packets.base.Secu3Packet
import org.secu3.android.models.packets.base.InputPacket
import org.secu3.android.models.packets.base.OutputPacket
import org.secu3.android.models.packets.base.protocolRoundToInt
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

    var timing: FloatArray = FloatArray(2),

    var timingCrk: FloatArray = FloatArray(2),

    var angleSpec: Int = 0,

    var fffConst: Float = 0f,

    var minPw: Int = 0,

    var injMafConst: IntArray = IntArray(2),

    var mafloadConst: Int = 0,

    var injMaxPw: FloatArray = FloatArray(2),

    var injPwDecSpeed: Float = 0f,
    var injPwIncSpeed: Float = 0f,


    ) : Secu3Packet(), InputPacket, OutputPacket {

    override fun pack(): IntArray {
        var data = intArrayOf(DESCRIPTOR.code)

        data += flags

        data += config[0]
        data += config[1]

        data += flowRate[0].times(64).roundToInt().write2Bytes()
        data += flowRate[1].times(64).roundToInt().write2Bytes()

        data += cylDisp.times(16384).roundToInt().write2Bytes()

        data += sdIglConst[0].write4Bytes()
        data += sdIglConst[1].write4Bytes()

        data += ckpsEngineCyl

        data += timing[0].times(PARINJTIM_DIVIDER).protocolRoundToInt().write2Bytes()
        data += timing[1].times(PARINJTIM_DIVIDER).protocolRoundToInt().write2Bytes()

        data += timingCrk[0].times(PARINJTIM_DIVIDER).protocolRoundToInt().write2Bytes()
        data += timingCrk[1].times(PARINJTIM_DIVIDER).protocolRoundToInt().write2Bytes()

        data += angleSpec

        data += fffConst.div(60000f).times(65536f).roundToInt().write2Bytes()

        data += minPw.write2Bytes()


        data += injMafConst[0].write4Bytes()
        data += injMafConst[1].write4Bytes()
        data += mafloadConst.write4Bytes()

        data += injMaxPw[0].times(1000.0f / 3.2f).roundToInt().write2Bytes()
        data += injMaxPw[1].times(1000.0f / 3.2f).roundToInt().write2Bytes()

        data += injPwDecSpeed.times(1000.0f / 3.2f).roundToInt().write2Bytes()
        data += injPwIncSpeed.times(1000.0f / 3.2f).roundToInt().write2Bytes()

        data += unhandledParams

        return data
    }

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
            if (ckpsEngineCyl <= 0 || !value.isFinite() || value <= 0f) return
            cylDisp = value / ckpsEngineCyl
            recalculateDerivedConstants()
        }

    /** Recompute injector and synthetic-load constants when their inputs change. */
    fun recalculateDerivedConstants() {
        for (bank in 0..1) configChanged(bank, if (bank == 0) config0Pulses else config1Pulses)
        if (ckpsEngineCyl > 0 && cylDisp.isFinite() && cylDisp > 0f) {
            mafloadConst = (((101.35 * 64.0 * 128.0 * 120.0) / (64.0 * 0.0012041)) /
                (cylDisp.toDouble() * 1000.0 * ckpsEngineCyl.toDouble())).toInt()
        }
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


    private val discrete: Float = 3.2f

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

    var fullSequentialAfterStart: Boolean
        get() = flags.getBitValue(6) > 0
        set(value) {
            flags = flags.setBitValue(value, 6)
        }


    private fun configChanged(configNum: Int, pulses: Int) {

        // A partially received or unsupported configuration must not corrupt the
        // derived constant. In particular, never divide by zero while the UI is
        // replacing a configuration and its dependent pulse count.
        if (ckpsEngineCyl <= 0 || pulses <= 0 || flowRate[configNum] <= 0f ||
            !flowRate[configNum].isFinite() || !cylDisp.isFinite() || cylDisp <= 0f) return

        val cfg = if (configNum == 0) config0 else config1
        if (InjConfig.entries.none { it.id == cfg } || pulses !in validPulses(cfg, ckpsEngineCyl)) return

        val bnkNum = bnkNum(cfg).toFloat()
        val injNum = injNum(cfg).toFloat()
        val mifr = flowRate[configNum] * fuelDensity[configNum]

        if (mifr <= 0f || !mifr.isFinite() || injNum <= 0 || bnkNum <= 0) return
        sdIglConst[configNum] = (((cylDisp * 3.482f * 18750000f) / mifr) * (bnkNum * ckpsEngineCyl.toFloat()) / (injNum * pulses.toFloat())).toInt().coerceAtMost(524288)
        injMafConst[configNum] = (((120.0 * 18750000.0) / mifr.toDouble() *
            (bnkNum.toDouble() / (injNum.toDouble() * pulses.toDouble())) / 64.0).toInt()).coerceAtMost(103200 * 4)
    }

    private fun validPulses(config: Int, cylinders: Int): List<Int> = when (config) {
        InjConfig.THROTTLEBODY.id, InjConfig.SIMULTANEOUS.id -> when (cylinders) {
            1 -> listOf(1); 2 -> listOf(1, 2); 3 -> listOf(1, 3); 4 -> listOf(1, 2, 4)
            5 -> listOf(1, 5); 6 -> listOf(1, 2, 3, 6); 8 -> listOf(1, 2, 4, 8); else -> emptyList()
        }
        InjConfig.TWO_BANK_ALTERN.id -> when (cylinders) { 4 -> listOf(2, 4); 6 -> listOf(2, 6); 8 -> listOf(2, 4, 8); else -> emptyList() }
        InjConfig.SEMISEQUENTIAL.id, InjConfig.SEMISEQSEPAR.id -> when (cylinders) {
            2 -> listOf(1, 2); 4 -> listOf(2, 4); 6 -> listOf(3, 6); 8 -> listOf(4, 8); else -> emptyList()
        }
        InjConfig.FULLSEQUENTIAL.id -> listOf(cylinders).filter { it > 0 }
        else -> emptyList()
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

    override fun parse(data: IntArray): InputPacket {
        flags = data.get1Byte()

        config[0] = data.get1Byte()
        config[1] = data.get1Byte()

        flowRate[0] = data.get2Bytes().toFloat() / 64
        flowRate[1] = data.get2Bytes().toFloat() / 64

        cylDisp = data.get2Bytes().toFloat() / 16384

        sdIglConst[0] = data.get4Bytes()
        sdIglConst[1] = data.get4Bytes()

        ckpsEngineCyl = data.get1Byte()

        timing[0] = data.get2BytesSigned().toFloat() / PARINJTIM_DIVIDER
        timing[1] = data.get2BytesSigned().toFloat() / PARINJTIM_DIVIDER

        timingCrk[0] = data.get2BytesSigned().toFloat() / PARINJTIM_DIVIDER
        timingCrk[1] = data.get2BytesSigned().toFloat() / PARINJTIM_DIVIDER

        angleSpec = data.get1Byte()

        fffConst = data.get2Bytes().toFloat().div(65536f).times(60000f)
        minPw = data.get2Bytes()

        injMafConst[0] = data.get4Bytes()
        injMafConst[1] = data.get4Bytes()

        mafloadConst = data.get4Bytes()

        injMaxPw[0] = data.get2Bytes().toFloat().times(3.2f / 1000.0f)
        injMaxPw[1] = data.get2Bytes().toFloat().times(3.2f / 1000.0f)

        injPwDecSpeed = data.get2Bytes().toFloat().times(3.2f / 1000.0f)
        injPwIncSpeed = data.get2Bytes().toFloat().times(3.2f / 1000.0f)

        data.setUnhandledParams()

        return this
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
        if (fffConst.compareTo(other.fffConst) != 0) return false
        if (minPw != other.minPw) return false

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
        result = 31 * result + fffConst.hashCode()
        result = 31 * result + minPw
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
    }
}
