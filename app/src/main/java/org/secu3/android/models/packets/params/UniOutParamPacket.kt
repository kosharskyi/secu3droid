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
        when (condition) {
            CONDITION_COOLANT_TEMPER -> return value.toFloat() / TEMPERATURE_MULTIPLIER
            CONDITION_RPM -> return value.toFloat()
            CONDITION_MAP -> return value.toFloat() / MAP_MULTIPLIER
            CONDITION_UBAT -> return value.toFloat() / VOLTAGE_MULTIPLIER
            CONDITION_CARB -> return value.toFloat()
            CONDITION_VSPD -> {
                val periodS = value / 312500.0f // Period in seconds

                var speed: Float = periodDistance / periodS * 3600.0f / 1000.0f // Kph

                if (speed > 999.9f) speed = 999.9f
                return speed
            }
            CONDITION_AIRFL -> return value.toFloat()
            CONDITION_TMR, CONDITION_ITTMR, CONDITION_ESTMR -> return value.toFloat() / 100.0f
            CONDITION_CPOS -> return value.toFloat() / CHOKE_MULTIPLIER
            CONDITION_AANG -> return value.toFloat() / ANGLE_DIVIDER
            CONDITION_KLEV -> return value.toFloat() / ADC_MULTIPLIER
            CONDITION_TPS -> return value.toFloat() / TPS_MULTIPLIER
            CONDITION_ATS -> return value.toFloat() / TEMPERATURE_MULTIPLIER

            CONDITION_AI1, CONDITION_AI2, CONDITION_AI3, CONDITION_AI4, CONDITION_AI5, CONDITION_AI6, CONDITION_AI7, CONDITION_AI8 -> return value.toFloat() / ADC_MULTIPLIER

            CONDITION_GASV -> return value.toFloat()
            CONDITION_IPW -> return (value.toFloat() * 3.2f) / 1000.0f
            CONDITION_CE -> return value.toFloat()
            CONDITION_OFTMR, CONDITION_LOOPTMR -> return value.toFloat() / 100.0f
        }

        return 0f
    }

    private fun encodeCondVal(value: Float, condition: Int): Int {
        when (condition) {
            CONDITION_COOLANT_TEMPER -> return (value * TEMPERATURE_MULTIPLIER).roundToInt()
            CONDITION_RPM -> return value.roundToInt()
            CONDITION_MAP -> return (value * MAP_MULTIPLIER).roundToInt()
            CONDITION_UBAT -> return (value * VOLTAGE_MULTIPLIER).roundToInt()
            CONDITION_CARB -> return value.roundToInt()
            CONDITION_VSPD -> return value.roundToInt() // FIXME: 05.01.21 need proper implementation
            CONDITION_AIRFL -> return value.roundToInt()
            CONDITION_TMR, CONDITION_ITTMR, CONDITION_ESTMR -> return (value * 100.0f).roundToInt()
            CONDITION_CPOS -> return (value * CHOKE_MULTIPLIER).roundToInt()
            CONDITION_AANG -> return (value * ANGLE_DIVIDER).roundToInt()
            CONDITION_KLEV -> return (value * ADC_MULTIPLIER).roundToInt()
            CONDITION_TPS -> return (value * TPS_MULTIPLIER).roundToInt()
            CONDITION_ATS -> return (value * TEMPERATURE_MULTIPLIER).roundToInt()

            CONDITION_AI1, CONDITION_AI2, CONDITION_AI3, CONDITION_AI4, CONDITION_AI5, CONDITION_AI6, CONDITION_AI7, CONDITION_AI8 -> return (value * ADC_MULTIPLIER).roundToInt()

            CONDITION_GASV -> return value.roundToInt()
            CONDITION_IPW -> return ((value * 1000.0f) / 3.2f).roundToInt()
            CONDITION_CE -> return value.roundToInt()
            CONDITION_OFTMR, CONDITION_LOOPTMR -> return (value * 100.0f).roundToInt()
        }

        return 0
    }


    companion object {

        const val LF_OR = 0
        const val LF_AND = 1
        const val LF_XOR = 2
        const val LF_2ND = 3
        const val LF_NONE = 15
        const val LF_COUNT = 5


        private const val CONDITIONS_COUNTER = 27
        const val CONDITION_COOLANT_TEMPER = 0 // Coolant temperature
        const val CONDITION_RPM = 1 // RPM
        const val CONDITION_MAP = 2 // MAP
        const val CONDITION_UBAT = 3 // Board voltage
        const val CONDITION_CARB = 4 // Throttle position limit switch
        const val CONDITION_VSPD = 5 // Vehicle speed
        const val CONDITION_AIRFL = 6 // Air flow
        const val CONDITION_TMR = 7 // Timer, allowed only for 2nd condition
        const val CONDITION_ITTMR = 8 // Timer, triggered after turning on of ignition
        const val CONDITION_ESTMR = 9 // Timer, triggered after starting of engine
        const val CONDITION_CPOS = 10 // Choke position
        const val CONDITION_AANG = 11 // Advance angle
        const val CONDITION_KLEV = 12 // Knock signal level
        const val CONDITION_TPS = 13 // Throttle position sensor
        const val CONDITION_ATS = 14 // Intake air temperature sensor
        const val CONDITION_AI1 = 15 // Analog input 1
        const val CONDITION_AI2 = 16 // Analog input 2
        const val CONDITION_GASV = 17 // Gas valve input
        const val CONDITION_IPW = 18 // Injector pulse width
        const val CONDITION_CE = 19 // CE state
        const val CONDITION_OFTMR = 20 // On/Off delay timer
        const val CONDITION_AI3 = 21 // Analog input 3
        const val CONDITION_AI4 = 22 // Analog input 4
        const val CONDITION_LOOPTMR = 23 // Looper timer 1 condition
        const val CONDITION_AI5 = 24 // Analog input 5
        const val CONDITION_AI6 = 25 // Analog input 6
        const val CONDITION_AI7 = 26 // Analog input 7
        const val CONDITION_AI8 = 27 // Analog input 8


        const val UNI_OUTPUT_NUM = 6

        internal const val DESCRIPTOR = '&'

        fun parse(data: String) = UniOutParamPacket().apply {

            rawOutput1Flags = data[2].code
            output1Condition1 = data[3].code
            output1Condition2 = data[4].code
            rawOutput1OnThrd1 = data.get2Bytes(5)
            rawOutput1OffThrd1 = data.get2Bytes(7)
            rawOutput1OnThrd2 = data.get2Bytes(9)
            rawOutput1OffThrd2 = data.get2Bytes(11)

            rawOutput2Flags = data[13].code
            output2Condition1 = data[14].code
            output2Condition2 = data[15].code
            rawOutput2OnThrd1 = data.get2Bytes(16)
            rawOutput2OffThrd1 = data.get2Bytes(18)
            rawOutput2OnThrd2 = data.get2Bytes(20)
            rawOutput2OffThrd2 = data.get2Bytes(22)

            rawOutput3Flags = data[24].code
            output3Condition1 = data[25].code
            output3Condition2 = data[26].code
            rawOutput3OnThrd1 = data.get2Bytes(27)
            rawOutput3OffThrd1 = data.get2Bytes(29)
            rawOutput3OnThrd2 = data.get2Bytes(31)
            rawOutput3OffThrd2 = data.get2Bytes(33)

            rawOutput4Flags = data[35].code
            output4Condition1 = data[36].code
            output4Condition2 = data[37].code
            rawOutput4OnThrd1 = data.get2Bytes(38)
            rawOutput4OffThrd1 = data.get2Bytes(40)
            rawOutput4OnThrd2 = data.get2Bytes(42)
            rawOutput4OffThrd2 = data.get2Bytes(44)

            rawOutput5Flags = data[46].code
            output5Condition1 = data[47].code
            output5Condition2 = data[48].code
            rawOutput5OnThrd1 = data.get2Bytes(49)
            rawOutput5OffThrd1 = data.get2Bytes(51)
            rawOutput5OnThrd2 = data.get2Bytes(53)
            rawOutput5OffThrd2 = data.get2Bytes(55)

            rawOutput6Flags = data[57].code
            output6Condition1 = data[58].code
            output6Condition2 = data[59].code
            rawOutput6OnThrd1 = data.get2Bytes(60)
            rawOutput6OffThrd1 = data.get2Bytes(62)
            rawOutput6OnThrd2 = data.get2Bytes(64)
            rawOutput6OffThrd2 = data.get2Bytes(66)


            logicFunction_1_2 = data[68].code

            if (data.length == 69) {
                return@apply
            }

            unhandledParams = data.substring(69)
        }
    }
}
