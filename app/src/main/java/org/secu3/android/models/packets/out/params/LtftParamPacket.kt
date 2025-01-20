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

        var data = "$DESCRIPTOR"

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

        fun parse(data: IntArray) = LtftParamPacket().apply {

            mode = data.get1Byte()

            learnClt = data.get2Bytes().toFloat().div(TEMPERATURE_MULTIPLIER)
            learnCltUp = data.get2Bytes().toFloat().div(TEMPERATURE_MULTIPLIER)
            learnIatUp = data.get2Bytes().toFloat().div(TEMPERATURE_MULTIPLIER)
            learnGrad = data.get1Byte().toFloat().div(256.0f)
            learnGpa = data.get2Bytes().toFloat().div(MAP_MULTIPLIER)
            learnGpd = data.get2Bytes().toFloat().div(MAP_MULTIPLIER)
            min = data.get1Byte().toByte().toFloat().div(512.0f / 100.0f)     // toByte because value is signed
            max = data.get1Byte().toFloat().div(512.0f / 100.0f)
            learnRpm0 = data.get2Bytes()
            learnRpm1 = data.get2Bytes()
            learnLoad0 = data.get2Bytes().toFloat().div(MAP_MULTIPLIER)
            learnLoad1 = data.get2Bytes().toFloat().div(MAP_MULTIPLIER)
            deadBand0 = data.get1Byte().toFloat().div(512.0f).times(100.0f)
            deadBand1 = data.get1Byte().toFloat().div(512.0f).times(100.0f)

            data.setUnhandledParams()
        }
    }

}
