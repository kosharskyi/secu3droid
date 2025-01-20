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

    override fun pack(): IntArray {
        var data = intArrayOf(
            DESCRIPTOR.code,
        )

        data += injAeTpsdotThrd

        data += injAeColdaccMult.toFloat().div(100).minus(1.0f).times(128f).roundToInt().write2Bytes()

        data += injAeDecayTime

        data += injAeType
        data += injAeTime

        data += injAeBallance.times(2.56f).roundToInt().coerceAtMost(255)
        data += injAeMapdotThrd
        data += injXtauSThrd.roundToInt().unaryMinus()
        data += injXtauFThrd.roundToInt().unaryMinus()
        data += wallwetModel

        data += unhandledParams

        return data
    }

    companion object {

        internal const val DESCRIPTOR = '|'

        fun parse(data: IntArray) = AccelerationParamPacket().apply {

            injAeTpsdotThrd = data.get1Byte()
            data.get2Bytes().let {
                injAeColdaccMult = it.toShort().toFloat().plus(128.0f).div(128.0f).times(100.0f).toInt()
            }
            injAeDecayTime = data.get1Byte()

            injAeType = data.get1Byte()
            injAeTime = data.get1Byte()

            injAeBallance = data.get1Byte().toFloat().div(2.56f)   //multiply by 100% and divide by 256
            injAeMapdotThrd = data.get1Byte()
            injXtauSThrd = -data.get1Byte().toFloat()
            injXtauFThrd = -data.get1Byte().toFloat()
            wallwetModel = data.get1Byte()

            data.setUnhandledParams()
        }
    }

}
