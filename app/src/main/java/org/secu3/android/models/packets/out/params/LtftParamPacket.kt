/*
 *    SecuDroid  - An open source, free manager for SECU-3 engine control unit
 *    Copyright (C) 2025 Vitalii O. Kosharskyi. Ukraine, Kyiv
 *
 *    SECU-3  - An open source, free engine control unit
 *    Copyright (C) 2007-2025 Alexey A. Shabelnikov. Ukraine, Kyiv
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
import kotlin.math.roundToInt

data class LtftParamPacket(

    var mode: Int = 0,

    var learnClt: Float = 0.0f,  // Celsius

    var learnCltUp: Float = 0.0f,    // Celsius

    var learnIatUp: Float = 0.0f,    // Celsius

    var learnGrad: Float = 0.0f,

    var learnGpa: Float = 0.0f,  //kPa

    var learnGpd: Float = 0.0f,  //kPa

    var min: Float = 0.0f,   // %

    var max: Float = 0.0f,   // %

    var learnRpm0: Int = 0,

    var learnRpm1: Int = 0,

    var learnLoad0: Float = 0.0f,   //kPa

    var learnLoad1:Float = 0.0f,    //kPa

    var deadBand0: Float = 0.0f,    // %

    var deadBand1: Float = 0.0f,    // %
) : BaseOutputPacket() {

    override fun pack(): String {

        var data = "$OUTPUT_PACKET_SYMBOL$DESCRIPTOR"

        data += mode.toChar()
        data += learnClt.times(TEMPERATURE_MULTIPLIER).roundToInt().write2Bytes()
        data += learnCltUp.times(TEMPERATURE_MULTIPLIER).roundToInt().write2Bytes()
        data += learnIatUp.times(TEMPERATURE_MULTIPLIER).roundToInt().write2Bytes()
        data += learnGrad.times(256).roundToInt().toChar()
        data += learnGpa.times(MAP_MULTIPLIER).roundToInt().write2Bytes()
        data += learnGpd.times(MAP_MULTIPLIER).roundToInt().write2Bytes()
        data += min.div(100.0f / 512.0f).toInt().toUByte().toInt().toChar()
        data += max.div(100.0f / 512.0f).roundToInt().toChar()
        data += learnRpm0.write2Bytes()
        data += learnRpm1.write2Bytes()
        data += learnLoad0.times(MAP_MULTIPLIER).roundToInt().write2Bytes()
        data += learnLoad1.times(MAP_MULTIPLIER).roundToInt().write2Bytes()
        data += deadBand0.div(100.0f).times(512.0f).roundToInt().toChar()
        data += deadBand1.div(100.0f).times(512.0f).roundToInt().toChar()

        data += unhandledParams

        return data
    }

    companion object {

        internal const val DESCRIPTOR = 'O'

        fun parse(data: String) = LtftParamPacket().apply {

            mode = data[2].code

            learnClt = data.get2Bytes(3).toFloat().div(TEMPERATURE_MULTIPLIER)
            learnCltUp = data.get2Bytes(5).toFloat().div(TEMPERATURE_MULTIPLIER)
            learnIatUp = data.get2Bytes(7).toFloat().div(TEMPERATURE_MULTIPLIER)
            learnGrad = data[9].code.toFloat().div(256.0f)
            learnGpa = data.get2Bytes(10).toFloat().div(MAP_MULTIPLIER)
            learnGpd = data.get2Bytes(12).toFloat().div(MAP_MULTIPLIER)
            min = data[14].code.toByte().toFloat().div(512.0f / 100.0f)     // toByte because value is signed
            max = data[15].code.toFloat().div(512.0f / 100.0f)
            learnRpm0 = data.get2Bytes(16)
            learnRpm1 = data.get2Bytes(18)
            learnLoad0 = data.get2Bytes(20).toFloat().div(MAP_MULTIPLIER)
            learnLoad1 = data.get2Bytes(22).toFloat().div(MAP_MULTIPLIER)
            deadBand0 = data[24].code.toFloat().div(512.0f).times(100.0f)
            deadBand1 = data[25].code.toFloat().div(512.0f).times(100.0f)

            if (data.length == 26) {
                return@apply
            }

            unhandledParams = data.substring(26)
        }
    }

}
