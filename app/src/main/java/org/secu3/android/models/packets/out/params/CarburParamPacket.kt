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

import org.secu3.android.models.packets.base.Secu3Packet
import org.secu3.android.models.packets.base.InputPacket
import org.secu3.android.models.packets.base.OutputPacket
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

    ) : Secu3Packet(), InputPacket, OutputPacket {

    override fun pack(): IntArray {
        var data = intArrayOf(DESCRIPTOR.code)

        data += ieLot.write2Bytes()
        data += ieHit.write2Bytes()
        data += carbInvers
        data += feOnThresholds.times(MAP_MULTIPLIER).roundToInt().write2Bytes()
        data += ieLotG.write2Bytes()
        data += ieHitG.write2Bytes()
        data += shutoffDelay.times(100).roundToInt()
        data += tpsThreshold.times(TPS_MULTIPLIER).roundToInt().write2Bytes()
        data += fuelcutMapThrd.times(MAP_MULTIPLIER).roundToInt().write2Bytes()
        data += fuelcutCtsThrd.times(TEMPERATURE_MULTIPLIER).roundToInt().write2Bytes()
        data += revlimLot.write2Bytes()
        data += revlimHit.write2Bytes()

        data += fuelcut_uni
        data += igncut_uni

        data += unhandledParams

        return data
    }

    override fun parse(data: IntArray): InputPacket {
        ieLot = data.get2Bytes()
        ieHit = data.get2Bytes()
        carbInvers = data.get1Byte()
        feOnThresholds = data.get2Bytes().toFloat() / MAP_MULTIPLIER
        ieLotG = data.get2Bytes()
        ieHitG = data.get2Bytes()
        shutoffDelay = data.get1Byte().toFloat() / 100
        tpsThreshold = data.get2Bytes().toFloat() / TPS_MULTIPLIER
        fuelcutMapThrd = data.get2Bytes().toFloat() / MAP_MULTIPLIER
        fuelcutCtsThrd = data.get2Bytes().toFloat() / TEMPERATURE_MULTIPLIER
        revlimLot = data.get2Bytes()
        revlimHit = data.get2Bytes()

        fuelcut_uni = data.get1Byte()
        igncut_uni = data.get1Byte()

        data.setUnhandledParams()

        return this
    }

    companion object {
        internal const val DESCRIPTOR = 'k'
    }
}
