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

    var aflowThrd: Int = 0

) : BaseOutputPacket() {

    var determineLambdaHeatingByVoltage: Boolean
        get() = flags.getBitValue(0) > 0
        set(value) {
            flags = if (value) {
                1.or(flags)
            } else {
                1.inv().and(flags)
            }
        }

    var lambdaCorrectionOnIdling: Boolean
        get() = flags.getBitValue(1) > 0
        set(value) {
            flags = if (value) {
                1.shl(1).or(flags)
            } else {
                1.shl(1).inv().and(flags)
            }
        }

    var heatingBeforeCranking: Boolean
        get() = flags.getBitValue(2) > 0
        set(value) {
            flags = if (value) {
                1.shl(2).or(flags)
            } else {
                1.shl(2).inv().and(flags)
            }
        }


    override fun pack(): String {
        var data = "$OUTPUT_PACKET_SYMBOL$DESCRIPTOR"

        data += strPerStp.toChar()

        data += stepSizeP.div(100).times(512).toInt().toChar()
        data += stepSizeM.div(100).times(512).toInt().toChar()

        data += corrLimitP.div(100).times(512).toInt().write2Bytes()
        data += corrLimitM.div(100).times(512).toInt().write2Bytes()

        data += swtPoint.times(VOLTAGE_MULTIPLIER).toInt().write2Bytes()
        data += tempThrd.times(TEMPERATURE_MULTIPLIER).toInt().write2Bytes()
        data += rpmThrd.write2Bytes()

        data += activDelay.toChar()

        data += deadBand.times(VOLTAGE_MULTIPLIER).toInt().write2Bytes()
        data += senstype.toChar()
        data += msPerStp.toChar()
        data += flags.toChar()
        data += gdStoichval.times(AFR_MULTIPLIER).toInt().write2Bytes()

        data += heatingTime0.toChar()
        data += heatingTime1.toChar()
        data += temperThrd.toChar()
        data += heatingAct.times(100).toInt().toChar()

        data += aflowThrd.div(32).write2Bytes()

        data += unhandledParams

        return data
    }

    companion object {

        internal const val DESCRIPTOR = '-'

        fun parse(data: String) = LambdaParamPacket().apply {

            strPerStp = data[2].code

            stepSizeP = data[3].code.toFloat() / 512 * 100
            stepSizeM = data[4].code.toFloat() / 512 * 100

            corrLimitP = data.get2Bytes(5).toFloat() / 512 * 100
            corrLimitM = data.get2Bytes(7).toFloat() / 512 * 100

            swtPoint = data.get2Bytes(9).toFloat() / VOLTAGE_MULTIPLIER
            tempThrd = data.get2Bytes(11).toFloat() / TEMPERATURE_MULTIPLIER
            rpmThrd = data.get2Bytes(13)

            activDelay = data[15].code

            deadBand = data.get2Bytes(16).toFloat() / VOLTAGE_MULTIPLIER
            senstype = data[18].code
            msPerStp = data[19].code
            flags = data[20].code
            gdStoichval = data.get2Bytes(21).toFloat() / AFR_MULTIPLIER

            heatingTime0 = data[23].code
            heatingTime1 = data[24].code
            temperThrd = data[25].code
            heatingAct = data[26].code.toFloat() / 100
            aflowThrd = data.get2Bytes(27) * 32

            if (data.length == 29) {
                return@apply
            }

            unhandledParams = data.substring(29)

        }
    }
}
