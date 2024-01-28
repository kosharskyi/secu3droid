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
import kotlin.math.roundToInt

data class AccelerationParamPacket(

    var injAeTpsdotThrd: Int = 0,
    var injAeColdaccMult: Int = 0,
    var injAeDecayTime: Int = 0,
    var injAeType: Int = 0,
    var injAeTime: Int = 0,
    var injAeBallance: Float = 0f,
    var injAeMapdotThrd: Int = 0,
    var injXtauSThrd: Float = 0f,
    var injXtauFThrd: Float = 0f,
    var wallwetModel: Int = 0,


) : BaseOutputPacket() {

    override fun pack(): String {
        var data = "$OUTPUT_PACKET_SYMBOL$DESCRIPTOR"

        data += injAeTpsdotThrd.toChar()

        data += injAeColdaccMult.toFloat().div(100).minus(1.0f).times(128f).roundToInt().write2Bytes()

        data += injAeDecayTime.toChar()

        data += injAeType.toChar()
        data += injAeTime.toChar()

        data += injAeBallance.times(2.56f).roundToInt().coerceAtMost(255).toChar()
        data += injAeMapdotThrd.toChar()
        data += injXtauSThrd.roundToInt().unaryMinus().toChar()
        data += injXtauFThrd.roundToInt().unaryMinus().toChar()
        data += wallwetModel.toChar()

        data += unhandledParams

        return data
    }

    companion object {

        internal const val DESCRIPTOR = '|'

        fun parse(data: String) = AccelerationParamPacket().apply {

            injAeTpsdotThrd = data[2].code
            data.get2Bytes(3).let {
                injAeColdaccMult = it.toShort().toFloat().plus(128.0f).div(128.0f).times(100.0f).toInt()
            }
            injAeDecayTime = data[5].code

            injAeType = data[6].code
            injAeTime = data[7].code

            injAeBallance = data[8].code.toFloat().div(2.56f)   //multiply by 100% and divide by 256
            injAeMapdotThrd = data[9].code
            injXtauSThrd = -data[10].code.toFloat()
            injXtauFThrd = -data[11].code.toFloat()
            wallwetModel = data[12].code

            if (data.length == 13) {
                return@apply
            }

            unhandledParams = data.substring(13)
        }
    }

}
