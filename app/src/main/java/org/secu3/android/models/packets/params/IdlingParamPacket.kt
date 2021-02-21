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

data class IdlingParamPacket(
    var idlFlags: Int = 0,
    var iFac1: Float = 0f,
    var iFac2: Float = 0f,
    var minefr: Int = 0,
    var idlingRpm: Int = 0,
    var idlregMinAngle: Float = 0f,
    var idlregMaxAngle: Float = 0f,
    var idlregTurnOnTemp: Float = 0f,

    var idlToRunAdd: Float = 0f,
    var rpmOnRunAdd: Int = 0,
    var idlRegP: Float = 0f,
    var idlRegI: Float = 0f,
    var coefThrd1: Float = 0f,
    var coefThrd2: Float = 0f,
    var integratorRpmLim: Int = 0,
    var mapValue: Float = 0f,
    var iacMinPos: Float = 0f,
    var iacMaxPos: Float = 0f,


    ) : BaseOutputPacket() {

    var useRegulator: Boolean
        get() = idlFlags.getBitValue(0) > 0
        set(value) {
            idlFlags = if (value) {
                1 or idlFlags
            } else {
                1.inv().and(idlFlags)
            }
        }

    var useRegulatorOnGas: Boolean
        get() = idlFlags.getBitValue(1) > 0
        set(value) {
            idlFlags = if (value) {
                (1 shl 1).or(idlFlags)
            } else {
                (1 shl 1).inv().or(idlFlags)
            }
        }

    var useClosedLoop: Boolean
        get() = idlFlags.getBitValue(2) > 0
        set(value) {
            idlFlags = if (value) {
                (1 shl 2).or(idlFlags)
            } else {
                (1 shl 2).inv().or(idlFlags)
            }
        }

    var pRegMode: Boolean
        get() = idlFlags.getBitValue(3) > 0
        set(value) {
            idlFlags = if (value) {
                (1 shl 3).or(idlFlags)
            } else {
                (1 shl 3).inv().or(idlFlags)
            }
        }

    var useClosedLoopOnGas: Boolean
        get() = idlFlags.getBitValue(4) > 0
        set(value) {
            idlFlags = if (value) {
                (1 shl 4).or(idlFlags)
            } else {
                (1 shl 4).inv().or(idlFlags)
            }
        }


    companion object {

        internal const val DESCRIPTOR = 'l'

        fun parse(data: String) = IdlingParamPacket().apply {
            idlFlags = data[2].toInt()
            iFac1 = data.get2Bytes(3).toFloat() / 256
            iFac2 = data.get2Bytes(5).toFloat() / 256
            minefr = data.get2Bytes(7)
            idlingRpm = data.get2Bytes(9)
            idlregMinAngle = data.get2Bytes(11).toShort().toFloat() / ANGLE_DIVIDER
            idlregMaxAngle = data.get2Bytes(13).toShort().toFloat() / ANGLE_DIVIDER
            idlregTurnOnTemp = data.get2Bytes(15).toFloat() / TEMPERATURE_MULTIPLIER
            idlToRunAdd = data[17].toFloat() / 2.0f
            rpmOnRunAdd = data[18].toInt() * 10
            idlRegP = data.get2Bytes(19).toFloat() / 256
            idlRegI = data.get2Bytes(21).toFloat() / 256
            coefThrd1 = data[23].toFloat().div(128).plus(1.0f)
            coefThrd2 = data[24].toFloat().div(128).plus(1.0f)
            integratorRpmLim = data[25].toInt().times(10)
            mapValue = data.get2Bytes(26).toFloat() / MAP_MULTIPLIER
            iacMinPos = data[28].toFloat() / 2
            iacMaxPos = data[29].toFloat() / 2
        }
    }

    override fun pack(): String {
        var data = "$OUTPUT_PACKET_SYMBOL$DESCRIPTOR"

        data += idlFlags.toChar()
        data += iFac1.times(256).toInt().write2Bytes()
        data += iFac2.times(256).toInt().write2Bytes()
        data += minefr.write2Bytes()
        data += idlingRpm.write2Bytes()
        data += idlregMinAngle.times(ANGLE_DIVIDER).toInt().write2Bytes()
        data += idlregMaxAngle.times(ANGLE_DIVIDER).toInt().write2Bytes()
        data += idlregTurnOnTemp.times(TEMPERATURE_MULTIPLIER).toInt().write2Bytes()
        data += idlToRunAdd.times(2.0f).toInt().toChar()
        data += rpmOnRunAdd.div(10).toChar()
        data += idlRegP.times(256).toInt().toChar()
        data += idlRegI.times(256).toInt().toChar()
        data += coefThrd1.minus(1.0f).times(128).toInt().toChar()
        data += coefThrd2.minus(1.0f).times(128).toInt().toChar()
        data += integratorRpmLim.div(10).toChar()
        data += mapValue.times(MAP_MULTIPLIER).toInt().write2Bytes()
        data += iacMinPos.times(2).toInt().toChar()
        data += iacMaxPos.times(2).toInt().toChar()

        data += END_PACKET_SYMBOL
        return data
    }
}
