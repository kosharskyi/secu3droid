/*
 *    SecuDroid  - An open source, free manager for SECU-3 engine control unit
 *    Copyright (C) 2024 Vitaliy O. Kosharskyi. Ukraine, Kyiv
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
package org.secu3.android.models.packets.params

import org.secu3.android.models.packets.BaseOutputPacket

data class AccelerationParamPacket(

    var injAeTpsdotThrd: Int = 0,
    var injAeColdaccMult: Int = 0,
    var injAeDecayTime: Int = 0,
    var injAeType: Int = 0,
    var injAeTime: Int = 0,
    var injAeBallance: Int = 0,
    var injAeMapdotThrd: Int = 0,
    var injXtauSThrd: Int = 0,
    var injXtauFThrd: Int = 0,
    var wallwetModel: Int = 0,


) : BaseOutputPacket() {

    override fun pack(): String {
        var data = "$OUTPUT_PACKET_SYMBOL$DESCRIPTOR"

        data += injAeTpsdotThrd.toChar()

        data += injAeColdaccMult.toFloat().div(100).minus(1.0f).times(128f).toInt().write2Bytes()

        data += injAeDecayTime.toChar()

        data += injAeType.toChar()
        data += injAeTime.toChar()

        data += injAeBallance.toChar()
        data += injAeMapdotThrd.toChar()
        data += injXtauSThrd.toChar()
        data += injXtauFThrd.toChar()
        data += wallwetModel.toChar()

        data += unhandledParams

        return data
    }

    companion object {

        internal const val DESCRIPTOR = '|'

        fun parse(data: String) = AccelerationParamPacket().apply {

            injAeTpsdotThrd = data[2].code
            data.get2Bytes(3).let {
                injAeColdaccMult = ((it.toFloat() / 128f + 1.0f) * 100).toInt()
            }
            injAeDecayTime = data[5].code

            injAeType = data[6].code
            injAeTime = data[7].code

            injAeBallance = data[8].code
            injAeMapdotThrd = data[9].code
            injXtauSThrd = data[10].code
            injXtauFThrd = data[11].code
            wallwetModel = data[12].code

            if (data.length == 13) {
                return@apply
            }

            unhandledParams = data.substring(13)
        }
    }

}
