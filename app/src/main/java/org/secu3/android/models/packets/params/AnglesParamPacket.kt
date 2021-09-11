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

data class AnglesParamPacket(
    var maxAngle: Float = 0f,
    var minAngle: Float = 0f,
    var angleCorrection: Float = 0f,
    var angleDecSpeed: Float = 0f,
    var angleIncSpeed: Float = 0f,
    var zeroAdvAngle: Int = 0,

) : BaseOutputPacket(){


    companion object {

        internal const val DESCRIPTOR = 'm'

        fun parse(data: String) = AnglesParamPacket().apply {
            maxAngle = data.get2Bytes(2).toFloat() / ANGLE_DIVIDER
            minAngle = data.get2Bytes(4).toFloat() / ANGLE_DIVIDER
            angleCorrection = data.get2Bytes(6).toFloat() / ANGLE_DIVIDER
            angleDecSpeed = data.get2Bytes(8).toFloat() / ANGLE_DIVIDER
            angleIncSpeed = data.get2Bytes(10).toFloat() / ANGLE_DIVIDER
            zeroAdvAngle = data[12].toInt()
        }
    }

    override fun pack(): String {
        var data = "$OUTPUT_PACKET_SYMBOL$DESCRIPTOR"

        data += data.write2Bytes(maxAngle.times(ANGLE_DIVIDER).toInt())
        data += data.write2Bytes(minAngle.times(ANGLE_DIVIDER).toInt())
        data += data.write2Bytes(angleCorrection.times(ANGLE_DIVIDER).toInt())
        data += data.write2Bytes(angleDecSpeed.times(ANGLE_DIVIDER).toInt())
        data += data.write2Bytes(angleIncSpeed.times(ANGLE_DIVIDER).toInt())
        data += zeroAdvAngle.toChar()

        return data
    }
}
