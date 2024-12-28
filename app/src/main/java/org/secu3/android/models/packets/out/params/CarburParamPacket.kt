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
    var revlimHit: Int = 0,
    var fuelcut_uni: Int = 0,
    var igncut_uni: Int = 0,

) : BaseOutputPacket() {

    override fun pack(): String {
        var data = "$OUTPUT_PACKET_SYMBOL$DESCRIPTOR"

        data += ieLot.write2Bytes()
        data += ieHit.write2Bytes()
        data += carbInvers.toChar()
        data += feOnThresholds.times(MAP_MULTIPLIER).roundToInt().write2Bytes()
        data += ieLotG.write2Bytes()
        data += ieHitG.write2Bytes()
        data += shutoffDelay.times(100).roundToInt().toChar()
        data += tpsThreshold.times(TPS_MULTIPLIER).roundToInt().write2Bytes()
        data += fuelcutMapThrd.times(MAP_MULTIPLIER).roundToInt().write2Bytes()
        data += fuelcutCtsThrd.times(TEMPERATURE_MULTIPLIER).roundToInt().write2Bytes()
        data += revlimLot.write2Bytes()
        data += revlimHit.write2Bytes()

        data += fuelcut_uni.toChar()
        data += igncut_uni.toChar()

        data += unhandledParams

        return data
    }

    companion object {

        internal const val DESCRIPTOR = 'k'

        fun parse(data: String) = CarburParamPacket().apply {
            ieLot = data.get2Bytes(2)
            ieHit = data.get2Bytes(4)
            carbInvers = data[6].code
            feOnThresholds = data.get2Bytes(7).toFloat() / MAP_MULTIPLIER
            ieLotG = data.get2Bytes(9)
            ieHitG = data.get2Bytes(11)
            shutoffDelay = data[13].code.toFloat() / 100
            tpsThreshold = data.get2Bytes(14).toFloat() / TPS_MULTIPLIER
            fuelcutMapThrd = data.get2Bytes(16).toFloat() / MAP_MULTIPLIER
            fuelcutCtsThrd = data.get2Bytes(18).toFloat() / TEMPERATURE_MULTIPLIER
            revlimLot = data.get2Bytes(20)
            revlimHit = data.get2Bytes(22)

            fuelcut_uni = data[24].code
            igncut_uni = data[25].code

            if (data.length == 26) {
                return@apply
            }

            unhandledParams = data.substring(26)
        }
    }
}
