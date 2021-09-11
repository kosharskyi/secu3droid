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

data class CarburParamPacket(

    var ieLot: Int = 0,
    var ieHit: Int = 0,
    var carbInvers: Int = 0,
    var feOnThresholds: Float = 0f,
    var ieLotG: Int = 0,
    var ieHitG: Int = 0,
    var shutoffDelay: Float = 0f,
    var tpsThreshold: Float = 0f,
    var fuelcutMapThrd: Float = 0f,
    var fuelcutCtsThrd: Float = 0f,
    var revlimLot: Int = 0,
    var revlimHit: Int = 0

) : BaseOutputPacket() {

    override fun pack(): String {
        var data = "$OUTPUT_PACKET_SYMBOL$DESCRIPTOR"

        data += data.write2Bytes(ieLot)
        data += data.write2Bytes(ieHit)
        data += carbInvers.toChar()
        data += feOnThresholds.times(MAP_MULTIPLIER).toInt().write2Bytes()
        data += ieLotG.write2Bytes()
        data += ieHitG.write2Bytes()
        data += shutoffDelay.times(100).toInt().toChar()
        data += tpsThreshold.times(TPS_MULTIPLIER).toInt().toChar()
        data += fuelcutMapThrd.times(MAP_MULTIPLIER).toInt().write2Bytes()
        data += fuelcutCtsThrd.times(TEMPERATURE_MULTIPLIER).toInt().write2Bytes()
        data += revlimLot.write2Bytes()
        data += revlimHit.write2Bytes()

        return data
    }

    companion object {

        internal const val DESCRIPTOR = 'k'

        fun parse(data: String) = CarburParamPacket().apply {
            ieLot = data.get2Bytes(2)
            ieHit = data.get2Bytes(4)
            carbInvers = data[6].toInt()
            feOnThresholds = data.get2Bytes(7).toFloat() / MAP_MULTIPLIER
            ieLotG = data.get2Bytes(9)
            ieHitG = data.get2Bytes(11)
            shutoffDelay = data[13].toFloat() / 100
            tpsThreshold = data[14].toFloat() / TPS_MULTIPLIER
            fuelcutMapThrd = data.get2Bytes(15).toFloat() / MAP_MULTIPLIER
            fuelcutCtsThrd = data.get2Bytes(17).toFloat() / TEMPERATURE_MULTIPLIER
            revlimLot = data.get2Bytes(19)
            revlimHit = data.get2Bytes(21)
        }
    }
}
