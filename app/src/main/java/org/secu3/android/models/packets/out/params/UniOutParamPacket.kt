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

data class UniOutParamPacket(

    private var rawOutput1Flags: Int = 0,
    var output1Condition1: Int = 0,
    var output1Condition2: Int = 0,
    private var rawOutput1OnThrd1: Int = 0,
    private var rawOutput1OffThrd1: Int = 0,
    private var rawOutput1OnThrd2: Int = 0,
    private var rawOutput1OffThrd2: Int = 0,

    private var rawOutput2Flags: Int = 0,
    var output2Condition1: Int = 0,
    var output2Condition2: Int = 0,
    private var rawOutput2OnThrd1: Int = 0,
    private var rawOutput2OffThrd1: Int = 0,
    private var rawOutput2OnThrd2: Int = 0,
    private var rawOutput2OffThrd2: Int = 0,

    private var rawOutput3Flags: Int = 0,
    var output3Condition1: Int = 0,
    var output3Condition2: Int = 0,
    private var rawOutput3OnThrd1: Int = 0,
    private var rawOutput3OffThrd1: Int = 0,
    private var rawOutput3OnThrd2: Int = 0,
    private var rawOutput3OffThrd2: Int = 0,

    private var rawOutput4Flags: Int = 0,
    var output4Condition1: Int = 0,
    var output4Condition2: Int = 0,
    private var rawOutput4OnThrd1: Int = 0,
    private var rawOutput4OffThrd1: Int = 0,
    private var rawOutput4OnThrd2: Int = 0,
    private var rawOutput4OffThrd2: Int = 0,

    private var rawOutput5Flags: Int = 0,
    var output5Condition1: Int = 0,
    var output5Condition2: Int = 0,
    private var rawOutput5OnThrd1: Int = 0,
    private var rawOutput5OffThrd1: Int = 0,
    private var rawOutput5OnThrd2: Int = 0,
    private var rawOutput5OffThrd2: Int = 0,

    private var rawOutput6Flags: Int = 0,
    var output6Condition1: Int = 0,
    var output6Condition2: Int = 0,
    private var rawOutput6OnThrd1: Int = 0,
    private var rawOutput6OffThrd1: Int = 0,
    private var rawOutput6OnThrd2: Int = 0,
    private var rawOutput6OffThrd2: Int = 0,

    var logicFunction_1_2: Int = 0

) : BaseOutputPacket() {

    override fun pack(): String {
        var data = "$OUTPUT_PACKET_SYMBOL$DESCRIPTOR"

        data += rawOutput1Flags.toChar()
        data += output1Condition1.toChar()
        data += output1Condition2.toChar()
        data += rawOutput1OnThrd1.write2Bytes()
        data += rawOutput1OffThrd1.write2Bytes()
        data += rawOutput1OnThrd2.write2Bytes()
        data += rawOutput1OffThrd2.write2Bytes()

        data += rawOutput2Flags.toChar()
        data += output2Condition1.toChar()
        data += output2Condition2.toChar()
        data += rawOutput2OnThrd1.write2Bytes()
        data += rawOutput2OffThrd1.write2Bytes()
        data += rawOutput2OnThrd2.write2Bytes()
        data += rawOutput2OffThrd2.write2Bytes()

        data += rawOutput3Flags.toChar()
        data += output3Condition1.toChar()
        data += output3Condition2.toChar()
        data += rawOutput3OnThrd1.write2Bytes()
        data += rawOutput3OffThrd1.write2Bytes()
        data += rawOutput3OnThrd2.write2Bytes()
        data += rawOutput3OffThrd2.write2Bytes()

        data += rawOutput4Flags.toChar()
        data += output4Condition1.toChar()
        data += output4Condition2.toChar()
        data += rawOutput4OnThrd1.write2Bytes()
        data += rawOutput4OffThrd1.write2Bytes()
        data += rawOutput4OnThrd2.write2Bytes()
        data += rawOutput4OffThrd2.write2Bytes()

        data += rawOutput5Flags.toChar()
        data += output5Condition1.toChar()
        data += output5Condition2.toChar()
        data += rawOutput5OnThrd1.write2Bytes()
        data += rawOutput5OffThrd1.write2Bytes()
        data += rawOutput5OnThrd2.write2Bytes()
        data += rawOutput5OffThrd2.write2Bytes()

        data += rawOutput6Flags.toChar()
        data += output6Condition1.toChar()
        data += output6Condition2.toChar()
        data += rawOutput6OnThrd1.write2Bytes()
        data += rawOutput6OffThrd1.write2Bytes()
        data += rawOutput6OnThrd2.write2Bytes()
        data += rawOutput6OffThrd2.write2Bytes()


        data += logicFunction_1_2.toChar()

        data += unhandledParams

        return data
    }

    var output1Cond1Inversion: Boolean
        get() = rawOutput1Flags.getBitValue(0) > 0
        set(value) {
            rawOutput1Flags = rawOutput1Flags.setBitValue(value, 0)
        }

    var output1Cond2Inversion: Boolean
        get() = rawOutput1Flags.getBitValue(1) > 0
        set(value) {
            rawOutput1Flags = rawOutput1Flags.setBitValue(value, 1)
        }
    var output1Use: Boolean
        get() = rawOutput1Flags.getBitValue(2) > 0
        set(value) {
            rawOutput1Flags = rawOutput1Flags.setBitValue(value, 2)
        }

    var output1Inversion: Boolean
        get() = rawOutput1Flags.getBitValue(3) > 0
        set(value) {
            rawOutput1Flags = rawOutput1Flags.setBitValue(value, 3)
        }

    var output1LogicFunc: Int
        get() = rawOutput1Flags shr 4
        set(value) {
            rawOutput1Flags = rawOutput1Flags.and(0xF).or(value shl 4)
        }

    var output1OnThrd1: Float
        get() = decodeCondVal(rawOutput1OnThrd1, output1Condition1)
        set(value) {
            rawOutput1OnThrd1 = encodeCondVal(value, output1Condition1)
        }

    var output1OffThrd1: Float
        get() = decodeCondVal(rawOutput1OffThrd1, output1Condition1)
        set(value) {
            rawOutput1OffThrd1 = encodeCondVal(value, output1Condition1)
        }

    var output1OnThrd2: Float
        get() = decodeCondVal(rawOutput1OnThrd2, output1Condition2)
        set(value) {
            rawOutput1OnThrd2 = encodeCondVal(value, output1Condition2)
        }

    var output1OffThrd2: Float
        get() = decodeCondVal(rawOutput1OffThrd2, output1Condition2)
        set(value) {
            rawOutput1OffThrd2 = encodeCondVal(value, output1Condition2)
        }




    var output2Cond1Inversion: Boolean
        get() = rawOutput2Flags.getBitValue(0) > 0
        set(value) {
            rawOutput2Flags = rawOutput2Flags.setBitValue(value, 0)
        }

    var output2Cond2Inversion: Boolean
        get() = rawOutput2Flags.getBitValue(1) > 0
        set(value) {
            rawOutput2Flags = rawOutput2Flags.setBitValue(value, 1)
        }

    var output2Use: Boolean
        get() = rawOutput2Flags.getBitValue(2) > 0
        set(value) {
            rawOutput2Flags = rawOutput2Flags.setBitValue(value, 2)
        }

    var output2Inversion: Boolean
        get() = rawOutput2Flags.getBitValue(3) > 0
        set(value) {
            rawOutput2Flags = rawOutput2Flags.setBitValue(value, 3)
        }

    var output2LogicFunc: Int
        get() = rawOutput2Flags shr 4
        set(value) {
            rawOutput2Flags = rawOutput2Flags.and(0xF).or(value shl 4)
        }

    var output2OnThrd1: Float
        get() = decodeCondVal(rawOutput2OnThrd1, output2Condition1)
        set(value) {
            rawOutput2OnThrd1 = encodeCondVal(value, output2Condition1)
        }

    var output2OffThrd1: Float
        get() = decodeCondVal(rawOutput2OffThrd1, output2Condition1)
        set(value) {
            rawOutput2OffThrd1 = encodeCondVal(value, output2Condition1)
        }

    var output2OnThrd2: Float
        get() = decodeCondVal(rawOutput2OnThrd2, output2Condition2)
        set(value) {
            rawOutput2OnThrd2 = encodeCondVal(value, output2Condition2)
        }

    var output2OffThrd2: Float
        get() = decodeCondVal(rawOutput2OffThrd2, output2Condition2)
        set(value) {
            rawOutput2OffThrd2 = encodeCondVal(value, output2Condition2)
        }




    var output3Cond1Inversion: Boolean
        get() = rawOutput3Flags.getBitValue(0) > 0
        set(value) {
            rawOutput3Flags = rawOutput3Flags.setBitValue(value, 0)
        }

    var output3Cond2Inversion: Boolean
        get() = rawOutput3Flags.getBitValue(1) > 0
        set(value) {
            rawOutput3Flags = rawOutput3Flags.setBitValue(value, 1)
        }

    var output3Use: Boolean
        get() = rawOutput3Flags.getBitValue(2) > 0
        set(value) {
            rawOutput3Flags = rawOutput3Flags.setBitValue(value, 2)
        }

    var output3Inversion: Boolean
        get() = rawOutput3Flags.getBitValue(3) > 0
        set(value) {
            rawOutput3Flags = rawOutput3Flags.setBitValue(value, 3)
        }

    var output3LogicFunc: Int
        get() = rawOutput3Flags shr 4
        set(value) {
            rawOutput3Flags = rawOutput3Flags.and(0xF).or(value shl 4)
        }

    var output3OnThrd1: Float
        get() = decodeCondVal(rawOutput3OnThrd1, output3Condition1)
        set(value) {
            rawOutput3OnThrd1 = encodeCondVal(value, output3Condition1)
        }

    var output3OffThrd1: Float
        get() = decodeCondVal(rawOutput3OffThrd1, output3Condition1)
        set(value) {
            rawOutput3OffThrd1 = encodeCondVal(value, output3Condition1)
        }

    var output3OnThrd2: Float
        get() = decodeCondVal(rawOutput3OnThrd2, output3Condition2)
        set(value) {
            rawOutput3OnThrd2 = encodeCondVal(value, output3Condition2)
        }

    var output3OffThrd2: Float
        get() = decodeCondVal(rawOutput3OffThrd2, output3Condition2)
        set(value) {
            rawOutput3OffThrd2 = encodeCondVal(value, output3Condition2)
        }





    var output4Cond1Inversion: Boolean
        get() = rawOutput4Flags.getBitValue(0) > 0
        set(value) {
            rawOutput4Flags = rawOutput4Flags.setBitValue(value, 0)
        }

    var output4Cond2Inversion: Boolean
        get() = rawOutput4Flags.getBitValue(1) > 0
        set(value) {
            rawOutput4Flags = rawOutput4Flags.setBitValue(value, 1)
        }

    var output4Use: Boolean
    get() = rawOutput4Flags.getBitValue(2) > 0
    set(value) {
        rawOutput4Flags = rawOutput4Flags.setBitValue(value, 2)
    }

    var output4Inversion: Boolean
    get() = rawOutput4Flags.getBitValue(3) > 0
    set(value) {
        rawOutput4Flags = rawOutput3Flags.setBitValue(value, 3)
    }

    var output4LogicFunc: Int
        get() = rawOutput4Flags shr 4
        set(value) {
            rawOutput4Flags = rawOutput4Flags.and(0xF).or(value shl 4)
        }

    var output4OnThrd1: Float
        get() = decodeCondVal(rawOutput4OnThrd1, output4Condition1)
        set(value) {
            rawOutput4OnThrd1 = encodeCondVal(value, output4Condition1)
        }

    var output4OffThrd1: Float
        get() = decodeCondVal(rawOutput4OffThrd1, output4Condition1)
        set(value) {
            rawOutput4OffThrd1 = encodeCondVal(value, output4Condition1)
        }

    var output4OnThrd2: Float
        get() = decodeCondVal(rawOutput4OnThrd2, output4Condition2)
        set(value) {
            rawOutput4OnThrd2 = encodeCondVal(value, output4Condition2)
        }

    var output4OffThrd2: Float
        get() = decodeCondVal(rawOutput4OffThrd2, output4Condition2)
        set(value) {
            rawOutput4OffThrd2 = encodeCondVal(value, output4Condition2)
        }





    var output5Cond1Inversion: Boolean
        get() = rawOutput5Flags.getBitValue(0) > 0
        set(value) {
            rawOutput5Flags = rawOutput5Flags.setBitValue(value, 0)
        }

    var output5Cond2Inversion: Boolean
        get() = rawOutput5Flags.getBitValue(1) > 0
        set(value) {
            rawOutput5Flags = rawOutput5Flags.setBitValue(value, 1)
        }

    var output5Use: Boolean
    get() = rawOutput5Flags.getBitValue(2) > 0
    set(value) {
        rawOutput5Flags = rawOutput5Flags.setBitValue(value, 2)
    }

    var output5Inversion: Boolean
    get() = rawOutput5Flags.getBitValue(3) > 0
    set(value) {
        rawOutput5Flags = rawOutput5Flags.setBitValue(value, 3)
    }

    var output5LogicFunc: Int
        get() = rawOutput5Flags shr 4
        set(value) {
            rawOutput5Flags = rawOutput5Flags.and(0xF).or(value shl 4)
        }

    var output5OnThrd1: Float
        get() = decodeCondVal(rawOutput5OnThrd1, output5Condition1)
        set(value) {
            rawOutput5OnThrd1 = encodeCondVal(value, output5Condition1)
        }

    var output5OffThrd1: Float
        get() = decodeCondVal(rawOutput5OffThrd1, output5Condition1)
        set(value) {
            rawOutput5OffThrd1 = encodeCondVal(value, output5Condition1)
        }

    var output5OnThrd2: Float
        get() = decodeCondVal(rawOutput5OnThrd2, output5Condition2)
        set(value) {
            rawOutput5OnThrd2 = encodeCondVal(value, output5Condition2)
        }

    var output5OffThrd2: Float
        get() = decodeCondVal(rawOutput5OffThrd2, output5Condition2)
        set(value) {
            rawOutput5OffThrd2 = encodeCondVal(value, output5Condition2)
        }






    var output6Cond1Inversion: Boolean
        get() = rawOutput6Flags.getBitValue(0) > 0
        set(value) {
            rawOutput6Flags = rawOutput6Flags.setBitValue(value, 0)
        }

    var output6Cond2Inversion: Boolean
        get() = rawOutput6Flags.getBitValue(1) > 0
        set(value) {
            rawOutput6Flags = rawOutput6Flags.setBitValue(value, 1)
        }

    var output6Use: Boolean
    get() = rawOutput6Flags.getBitValue(2) > 0
    set(value) {
        rawOutput6Flags = rawOutput6Flags.setBitValue(value, 2)
    }

    var output6Inversion: Boolean
    get() = rawOutput6Flags.getBitValue(3) > 0
    set(value) {
        rawOutput6Flags = rawOutput6Flags.setBitValue(value, 3)
    }

    var output6LogicFunc: Int
        get() = rawOutput5Flags shr 4
        set(value) {
            rawOutput6Flags = rawOutput6Flags.and(0xF).or(value shl 4)
        }

    var output6OnThrd1: Float
        get() = decodeCondVal(rawOutput6OnThrd1, output6Condition1)
        set(value) {
            rawOutput6OnThrd1 = encodeCondVal(value, output6Condition1)
        }

    var output6OffThrd1: Float
        get() = decodeCondVal(rawOutput6OffThrd1, output6Condition1)
        set(value) {
            rawOutput6OffThrd1 = encodeCondVal(value, output6Condition1)
        }

    var output6OnThrd2: Float
        get() = decodeCondVal(rawOutput6OnThrd2, output6Condition2)
        set(value) {
            rawOutput6OnThrd2 = encodeCondVal(value, output6Condition2)
        }

    var output6OffThrd2: Float
        get() = decodeCondVal(rawOutput6OffThrd2, output6Condition2)
        set(value) {
            rawOutput6OffThrd2 = encodeCondVal(value, output6Condition2)
        }


    private fun decodeCondVal(value: Int, condition: Int): Float {
        return when (condition) {
            CONDITION.COOLANT_TEMPER.id -> value.toFloat() / TEMPERATURE_MULTIPLIER
            CONDITION.RPM.id -> value.toFloat()
            CONDITION.MAP.id -> value.toFloat() / MAP_MULTIPLIER
            CONDITION.UBAT.id -> value.toFloat() / VOLTAGE_MULTIPLIER
            CONDITION.CARB.id -> value.toFloat()
            CONDITION.VSPD.id -> {
                value.div(32.0f).coerceAtMost(999.9f)
            }
            CONDITION.AIRFL.id -> value.toFloat()
            CONDITION.TMR.id, CONDITION.ITTMR.id, CONDITION.ESTMR.id -> value.toFloat() / 100.0f
            CONDITION.CPOS.id -> value.toFloat() / CHOKE_MULTIPLIER
            CONDITION.AANG.id -> value.toFloat() / ANGLE_DIVIDER
            CONDITION.KLEV.id -> value.toFloat() * ADC_DISCRETE
            CONDITION.TPS.id -> value.toFloat() / TPS_MULTIPLIER
            CONDITION.ATS.id -> value.toFloat() / TEMPERATURE_MULTIPLIER

            CONDITION.AI1.id, CONDITION.AI2.id, CONDITION.AI3.id, CONDITION.AI4.id, CONDITION.AI5.id, CONDITION.AI6.id, CONDITION.AI7.id, CONDITION.AI8.id -> value.toFloat() * ADC_DISCRETE

            CONDITION.GASV.id -> value.toFloat()
            CONDITION.IPW.id -> (value.toFloat() * 3.2f) / 1000.0f
            CONDITION.CE.id -> value.toFloat()
            CONDITION.OFTMR.id, CONDITION.LOOPTMR.id -> value.toFloat() / 100.0f

            CONDITION.GRTS.id -> value.toFloat().div(TEMPERATURE_MULTIPLIER)

            CONDITION.MAP2.id -> value.toFloat().div(MAP_MULTIPLIER)

            CONDITION.TMP2.id -> value.toFloat().div(TEMPERATURE_MULTIPLIER)

            CONDITION.INPUT1.id -> value.toFloat()

            CONDITION.INPUT2.id -> value.toFloat()

            CONDITION.MAF.id -> value.toFloat().div(MAFS_MULT)

            CONDITION.TPSDOT.id -> value.toFloat()

            else -> 0f
        }
    }

    private fun encodeCondVal(value: Float, condition: Int): Int {
        return when (condition) {
            CONDITION.COOLANT_TEMPER.id -> (value * TEMPERATURE_MULTIPLIER).roundToInt()
            CONDITION.RPM.id -> value.roundToInt()
            CONDITION.MAP.id -> (value * MAP_MULTIPLIER).roundToInt()
            CONDITION.UBAT.id -> (value * VOLTAGE_MULTIPLIER).roundToInt()
            CONDITION.CARB.id -> value.roundToInt()
            CONDITION.VSPD.id -> value.times(32.0f).roundToInt()
            CONDITION.AIRFL.id -> value.roundToInt()
            CONDITION.TMR.id, CONDITION.ITTMR.id, CONDITION.ESTMR.id -> (value * 100.0f).roundToInt()
            CONDITION.CPOS.id -> (value * CHOKE_MULTIPLIER).roundToInt()
            CONDITION.AANG.id -> (value * ANGLE_DIVIDER).roundToInt()
            CONDITION.KLEV.id -> (value * 1.0f.div(ADC_DISCRETE)).roundToInt()
            CONDITION.TPS.id -> (value * TPS_MULTIPLIER).roundToInt()
            CONDITION.ATS.id -> (value * TEMPERATURE_MULTIPLIER).roundToInt()

            CONDITION.AI1.id, CONDITION.AI2.id, CONDITION.AI3.id, CONDITION.AI4.id, CONDITION.AI5.id, CONDITION.AI6.id, CONDITION.AI7.id, CONDITION.AI8.id -> (value * 1.0f.div(ADC_DISCRETE)).roundToInt()

            CONDITION.GASV.id -> value.roundToInt()
            CONDITION.IPW.id -> ((value * 1000.0f) / 3.2f).roundToInt()
            CONDITION.CE.id -> value.roundToInt()
            CONDITION.OFTMR.id, CONDITION.LOOPTMR.id -> (value * 100.0f).roundToInt()

            CONDITION.GRTS.id -> value.times(TEMPERATURE_MULTIPLIER).roundToInt()

            CONDITION.MAP2.id -> value.times(MAP_MULTIPLIER).roundToInt()

            CONDITION.TMP2.id -> value.times(TEMPERATURE_MULTIPLIER).roundToInt()

            CONDITION.INPUT1.id -> value.roundToInt()

            CONDITION.INPUT2.id -> value.roundToInt()

            CONDITION.MAF.id -> value.times(MAFS_MULT).roundToInt()

            CONDITION.TPSDOT.id -> value.roundToInt() // %/s

            else -> 0
        }
    }

    enum class CONDITION (val id: Int, val condition1: Boolean, val condition2: Boolean, @StringRes val strId: Int) {
        COOLANT_TEMPER(0, true, true, R.string.uniout_condition_cts), // Coolant temperature
        RPM(1, true, true, R.string.uniout_condition_rpm), // RPM
        MAP(2, true, true, R.string.uniout_condition_map), // MAP
        UBAT(3, true, true, R.string.uniout_condition_ubat), // Board voltage
        CARB(4, true, true, R.string.uniout_condition_carb), // Throttle position limit switch
        VSPD(5, true, true, R.string.uniout_condition_vspd), // Vehicle speed
        AIRFL(6, true, true, R.string.uniout_condition_airfl), // Air flow
        TMR(7, false, true, R.string.uniout_condition_tmr), // Timer, allowed only for 2nd condition
        ITTMR(8, true, true, R.string.uniout_condition_ittmr), // Timer, triggered after turning on of ignition
        ESTMR(9, true, true, R.string.uniout_condition_estmr), // Timer, triggered after starting of engine
        CPOS(10, true, true, R.string.uniout_condition_cpos), // Choke position
        AANG(11, true, true, R.string.uniout_condition_aang), // Advance angle
        KLEV(12, true, true, R.string.uniout_condition_klev), // Knock signal level
        TPS(13, true, true, R.string.uniout_condition_tps), // Throttle position sensor
        ATS(14, true, true, R.string.uniout_condition_ats), // Intake air temperature sensor
        AI1(15, true, true, R.string.uniout_condition_ai1), // Analog input 1
        AI2(16, true, true, R.string.uniout_condition_ai2), // Analog input 2
        GASV(17, true, true, R.string.uniout_condition_gasv), // Gas valve input
        IPW(18, true, true, R.string.uniout_condition_ipw), // Injector pulse width
        CE(19, true, true, R.string.uniout_condition_ce), // CE state
        OFTMR(20, true, true, R.string.uniout_condition_oftmr), // On/Off delay timer
        AI3(21, true, true, R.string.uniout_condition_ai3), // Analog input 3
        AI4(22, true, true, R.string.uniout_condition_ai3), // Analog input 4
        LOOPTMR(23, false, true, R.string.uniout_condition_looptmr), // Looper timer 1 condition
        AI5(24, true, true, R.string.uniout_condition_ai5), // Analog input 5
        AI6(25, true, true, R.string.uniout_condition_ai6), // Analog input 6
        AI7(26, true, true, R.string.uniout_condition_ai7), // Analog input 7
        AI8(27, true, true, R.string.uniout_condition_ai8), // Analog input 8
        GRTS(28, true, true, R.string.uniout_condition_grts),     //GRTS
        MAP2(29, true, true, R.string.uniout_condition_map2),     //MAP2
        TMP2(30, true, true, R.string.uniout_condition_temp2),     //TMP2
        INPUT1(31, true, true, R.string.uniout_condition_input1),   //INPUT1
        INPUT2(32, true, true, R.string.uniout_condition_input2),   //INPUT2
        MAF(33, true, true, R.string.uniout_condition_maf),      //MAF
        TPSDOT(34, true, true, R.string.uniout_condition_tps_dot),   //TPS dot
        GPS(35, true, true, R.string.uniout_condition_gps),   //TPS dot
    }


    companion object {

        const val LF_OR = 0
        const val LF_AND = 1
        const val LF_XOR = 2
        const val LF_2ND = 3
        const val LF_NONE = 15
        const val LF_COUNT = 5

        const val UNI_OUTPUT_NUM = 6

        internal const val DESCRIPTOR = '&'

        fun parse(data: String) = UniOutParamPacket().apply {

            rawOutput1Flags = data.get1Byte()
            output1Condition1 = data.get1Byte()
            output1Condition2 = data.get1Byte()
            rawOutput1OnThrd1 = data.get2Bytes()
            rawOutput1OffThrd1 = data.get2Bytes()
            rawOutput1OnThrd2 = data.get2Bytes()
            rawOutput1OffThrd2 = data.get2Bytes()

            rawOutput2Flags = data.get1Byte()
            output2Condition1 = data.get1Byte()
            output2Condition2 = data.get1Byte()
            rawOutput2OnThrd1 = data.get2Bytes()
            rawOutput2OffThrd1 = data.get2Bytes()
            rawOutput2OnThrd2 = data.get2Bytes()
            rawOutput2OffThrd2 = data.get2Bytes()

            rawOutput3Flags = data.get1Byte()
            output3Condition1 = data.get1Byte()
            output3Condition2 = data.get1Byte()
            rawOutput3OnThrd1 = data.get2Bytes()
            rawOutput3OffThrd1 = data.get2Bytes()
            rawOutput3OnThrd2 = data.get2Bytes()
            rawOutput3OffThrd2 = data.get2Bytes()

            rawOutput4Flags = data.get1Byte()
            output4Condition1 = data.get1Byte()
            output4Condition2 = data.get1Byte()
            rawOutput4OnThrd1 = data.get2Bytes()
            rawOutput4OffThrd1 = data.get2Bytes()
            rawOutput4OnThrd2 = data.get2Bytes()
            rawOutput4OffThrd2 = data.get2Bytes()

            rawOutput5Flags = data.get1Byte()
            output5Condition1 = data.get1Byte()
            output5Condition2 = data.get1Byte()
            rawOutput5OnThrd1 = data.get2Bytes()
            rawOutput5OffThrd1 = data.get2Bytes()
            rawOutput5OnThrd2 = data.get2Bytes()
            rawOutput5OffThrd2 = data.get2Bytes()

            rawOutput6Flags = data.get1Byte()
            output6Condition1 = data.get1Byte()
            output6Condition2 = data.get1Byte()
            rawOutput6OnThrd1 = data.get2Bytes()
            rawOutput6OffThrd1 = data.get2Bytes()
            rawOutput6OnThrd2 = data.get2Bytes()
            rawOutput6OffThrd2 = data.get2Bytes()


            logicFunction_1_2 = data.get1Byte()

            data.setUnhandledParams()
        }
    }
}
