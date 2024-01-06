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
    var igntimFlags: Int = 0,   // Ignition timing flags
    var shift_ingtim: Float = 0f, // Shift ignition timing (degrees)

) : BaseOutputPacket(){

    var alwaysUseIgnitionMap: Boolean           // Allways use working mode's ignition timing map
        get() = igntimFlags.getBitValue(0) > 0
        set(value) {
            igntimFlags = if (value) {
                1 or igntimFlags
            } else {
                1.inv().and(igntimFlags)
            }
        }

    var applyManualTimingCorrOnIdl: Boolean           // Apply manual ignition timing correction on idling
        get() = igntimFlags.getBitValue(1) > 0
        set(value) {
            igntimFlags = if (value) {
                (1 shl 1).or(igntimFlags)
            } else {
                (1 shl 1).inv().and(igntimFlags)
            }
        }

    var zeroAdvAngleWithCorr: Boolean           // Zero advance angle with octane correction
        get() = igntimFlags.getBitValue(2) > 0
        set(value) {
            igntimFlags = if (value) {
                (1 shl 2).or(igntimFlags)
            } else {
                (1 shl 2).inv().and(igntimFlags)
            }
        }


    companion object {

        internal const val DESCRIPTOR = 'm'

        fun parse(data: String) = AnglesParamPacket().apply {
            maxAngle = data.get2Bytes(2).toFloat() / ANGLE_DIVIDER
            minAngle = data.get2Bytes(4).toFloat() / ANGLE_DIVIDER
            angleCorrection = data.get2Bytes(6).toFloat() / ANGLE_DIVIDER
            angleDecSpeed = data.get2Bytes(8).toFloat() / ANGLE_DIVIDER
            angleIncSpeed = data.get2Bytes(10).toFloat() / ANGLE_DIVIDER
            zeroAdvAngle = data[12].code
            igntimFlags = data[13].code
            shift_ingtim = data.get2Bytes(14).toFloat() / ANGLE_DIVIDER

            if (data.length == 16) {
                return@apply
            }

            unhandledParams = data.substring(16)
        }
    }

    override fun pack(): String {
        var data = "$OUTPUT_PACKET_SYMBOL$DESCRIPTOR"

        data += maxAngle.times(ANGLE_DIVIDER).toInt().write2Bytes()
        data += minAngle.times(ANGLE_DIVIDER).toInt().write2Bytes()
        data += angleCorrection.times(ANGLE_DIVIDER).toInt().write2Bytes()
        data += angleDecSpeed.times(ANGLE_DIVIDER).toInt().write2Bytes()
        data += angleIncSpeed.times(ANGLE_DIVIDER).toInt().write2Bytes()
        data += zeroAdvAngle.toChar()
        data += igntimFlags.toChar()
        data += shift_ingtim.times(ANGLE_DIVIDER).toInt().write2Bytes()

        data += unhandledParams

        return data
    }
}
