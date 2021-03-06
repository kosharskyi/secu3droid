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

data class StarterParamPacket(
    var starterOff: Int = 0,
    var smapAbandon: Int = 0,
    var crankToRunTime: Float = 0f,
    var injAftstrStroke: Int = 0,
    var injPrimeCold: Float = 0f,
    var injPrimeHot: Float = 0f,
    var injPrimeDelay: Float = 0f,
    var injFloodclearTps: Float = 0f,
    var injAftStrokes1: Int = 0,

) : BaseOutputPacket(){


    companion object {

        internal const val DESCRIPTOR = 'o'

        fun parse(data: String) = StarterParamPacket().apply {
            starterOff = data.get2Bytes(2)
            smapAbandon = data.get2Bytes(4)
            crankToRunTime = data.get2Bytes(6).toFloat() / 100
            injAftstrStroke = data[8].toInt() * 4
            injPrimeCold = data.get2Bytes(9).toFloat() * 32 / 10000
            injPrimeHot = data.get2Bytes(11).toFloat() * 32 / 10000
            injPrimeDelay = data[13].toFloat() / 10
            injFloodclearTps = data[14].toFloat() / 2f
            injAftStrokes1 = data[15].toInt() * 4
        }
    }

    override fun pack(): String {
        var data = "$OUTPUT_PACKET_SYMBOL$DESCRIPTOR"

        data += starterOff.write2Bytes()
        data += smapAbandon.write2Bytes()
        data += crankToRunTime.times(100).toInt().write2Bytes()
        data += injAftstrStroke.div(4).toChar()
        data += injPrimeCold.times(10000).div(32).toInt().write2Bytes()
        data += injPrimeHot.times(10000).div(32).toInt().write2Bytes()
        data += injPrimeDelay.times(10).toChar()
        data += injFloodclearTps.times(2).toChar()
        data += injAftStrokes1.div(4).toChar()

        data += END_PACKET_SYMBOL
        return data
    }
}
