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

data class LambdaParamPacket(

    var strPerStp: Int = 0,

    var stepSizeP: Float = 0f,
    var stepSizeM: Float = 0f,

    var corrLimitP: Float = 0f,
    var corrLimitM: Float = 0f,

    var swtPoint: Float = 0f,

    var tempThrd: Float = 0f,

    var rpmThrd: Int = 0,

    var activDelay: Int = 0,

    var deadBand: Float = 0f,

    var senstype: Int = 0,

    var msPerStp: Int = 0,

    var flags: Int = 0,

    var gdStoichval: Float = 0f,

    var heatingTime0: Int = 0,  // cold engine
    var heatingTime1: Int = 0,  // hot engine

    var temperThrd: Int = 0,

    var heatingAct: Float = 0f,

    var aflowThrd: Float = 0f,

    var lambdaSelectedChanel: Int = 0,

) : BaseOutputPacket() {

    var determineLambdaHeatingByVoltage: Boolean
        get() = flags.getBitValue(0) > 0
        set(value) {
            flags = flags.setBitValue(value, 0)
        }

    var lambdaCorrectionOnIdling: Boolean
        get() = flags.getBitValue(1) > 0
        set(value) {
            flags = flags.setBitValue(value, 1)
        }

    var heatingBeforeCranking: Boolean
        get() = flags.getBitValue(2) > 0
        set(value) {
            flags = flags.setBitValue(value, 2)
        }

    var mixSenorsValue: Boolean
        get() = flags.getBitValue(3) > 0
        set(value) {
            flags = flags.setBitValue(value, 3)
        }




    var lambdaChanel1: Boolean
        get() = lambdaSelectedChanel.getBitValue(0) > 0
        set(value) {
            lambdaSelectedChanel = lambdaSelectedChanel.setBitValue(value,0)
        }

    var lambdaChanel2: Boolean
        get() = lambdaSelectedChanel.getBitValue(1) > 0
        set(value) {
            lambdaSelectedChanel = lambdaSelectedChanel.setBitValue(value,1)
        }

    var lambdaChanel3: Boolean
        get() = lambdaSelectedChanel.getBitValue(2) > 0
        set(value) {
            lambdaSelectedChanel = lambdaSelectedChanel.setBitValue(value,2)
        }

    var lambdaChanel4: Boolean
        get() = lambdaSelectedChanel.getBitValue(3) > 0
        set(value) {
            lambdaSelectedChanel = lambdaSelectedChanel.setBitValue(value,3)
        }

    var lambdaChanel5: Boolean
        get() = lambdaSelectedChanel.getBitValue(4) > 0
        set(value) {
            lambdaSelectedChanel = lambdaSelectedChanel.setBitValue(value,4)
        }

    var lambdaChanel6: Boolean
        get() = lambdaSelectedChanel.getBitValue(5) > 0
        set(value) {
            lambdaSelectedChanel = lambdaSelectedChanel.setBitValue(value,5)
        }

    var lambdaChanel7: Boolean
        get() = lambdaSelectedChanel.getBitValue(6) > 0
        set(value) {
            lambdaSelectedChanel = lambdaSelectedChanel.setBitValue(value,6)
        }

    var lambdaChanel8: Boolean
        get() = lambdaSelectedChanel.getBitValue(7) > 0
        set(value) {
            lambdaSelectedChanel = lambdaSelectedChanel.setBitValue(value,7)
        }

    override fun pack(): String {
        var data = "$DESCRIPTOR"

        data += strPerStp.toChar()

        data += stepSizeP.div(100).times(512).roundToInt().toChar()
        data += stepSizeM.div(100).times(512).roundToInt().toChar()

        data += corrLimitP.div(100).times(512).roundToInt().write2Bytes()
        data += corrLimitM.div(100).times(512).roundToInt().write2Bytes()

        data += swtPoint.div(ADC_DISCRETE).roundToInt().write2Bytes()
        data += tempThrd.times(TEMPERATURE_MULTIPLIER).roundToInt().write2Bytes()
        data += rpmThrd.write2Bytes()

        data += activDelay.toChar()

        data += deadBand.div(ADC_DISCRETE).roundToInt().write2Bytes()
        data += senstype.toChar()
        data += msPerStp.div(10).toChar()
        data += flags.toChar()
        data += gdStoichval.times(128.0f).roundToInt().write2Bytes()

        data += heatingTime0.toChar()
        data += heatingTime1.toChar()
        data += temperThrd.toChar()
        data += heatingAct.times(100).roundToInt().toChar()

        data += aflowThrd.div(32).roundToInt().write2Bytes()

        data += lambdaSelectedChanel.toChar()

        data += unhandledParams

        return data
    }

    companion object {

        internal const val DESCRIPTOR = '-'

        fun parse(data: IntArray) = LambdaParamPacket().apply {

            strPerStp = data.get1Byte()

            stepSizeP = data.get1Byte().toFloat() / 512 * 100
            stepSizeM = data.get1Byte().toFloat() / 512 * 100

            corrLimitP = data.get2Bytes().toFloat() / 512 * 100
            corrLimitM = data.get2Bytes().toFloat() / 512 * 100

            swtPoint = data.get2Bytes().toFloat() * ADC_DISCRETE
            tempThrd = data.get2Bytes().toFloat() / TEMPERATURE_MULTIPLIER
            rpmThrd = data.get2Bytes()

            activDelay = data.get1Byte()

            deadBand = data.get2Bytes().toFloat() * ADC_DISCRETE
            senstype = data.get1Byte()
            msPerStp = data.get1Byte() * 10
            flags = data.get1Byte()
            gdStoichval = data.get2Bytes().toFloat() / 128.0f

            heatingTime0 = data.get1Byte()
            heatingTime1 = data.get1Byte()
            temperThrd = data.get1Byte()
            heatingAct = data.get1Byte().toFloat() / 100
            aflowThrd = data.get2Bytes().toFloat() * 32.0f

            lambdaSelectedChanel = data.get1Byte()

            data.setUnhandledParams()
        }
    }
}
