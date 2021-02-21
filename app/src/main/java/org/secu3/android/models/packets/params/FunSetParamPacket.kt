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

data class FunSetParamPacket(

    var fnGasoline: Int = 0,
    var fnGas: Int = 0,
    var loadLower: Float = 0f,
    var loadUpper: Float = 0f,

    var mapCurveOffset: Float = 0f,
    var mapCurveGradient: Float = 0f,

    var map2CurveOffset: Float = 0f,
    var map2CurveGradient: Float = 0f,

    var tpsCurveOffset: Float = 0f,
    var tpsCurveGradient: Float = 0f,

    var loadSrcCfg: Int = 0,
    var mapserUni: Int = 0,
    var barocorrType: Int = 0,
    var funcFlags: Int = 0,

    ): BaseOutputPacket() {

    override fun pack(): String {
        var data = "$OUTPUT_PACKET_SYMBOL$DESCRIPTOR"

        data += fnGasoline.toChar()
        data += fnGas.toChar()
        data += loadLower.times(MAP_MULTIPLIER).toInt().write2Bytes()
        data += loadUpper.times(MAP_MULTIPLIER).toInt().write2Bytes()
        data += mapCurveOffset.times(VOLTAGE_MULTIPLIER).toInt().write2Bytes()
        data += mapCurveGradient.div(100).times(2048).toInt().write2Bytes()
        data += map2CurveOffset.times(VOLTAGE_MULTIPLIER).toInt().write2Bytes()
        data += map2CurveGradient.div(100).times(2048).toInt().write2Bytes()
        data += tpsCurveOffset.times(VOLTAGE_MULTIPLIER).toInt().write2Bytes()
        data += tpsCurveGradient.div(100).times(4096).toInt().write2Bytes()

        data += loadSrcCfg.toChar()
        data += mapserUni.toChar()
        data += barocorrType.toChar()
//        data += funcFlags.toChar()

        data += END_PACKET_SYMBOL
        return data
    }

    val mapserUniPetrol: Int
        get() = mapserUni.and(15)

    val mapserUniGas: Int
        get() = mapserUni.and(240).shr(4)

    companion object {

        internal const val DESCRIPTOR = 'n'

        fun parse(data: String) = FunSetParamPacket().apply {

            fnGasoline = data[2].toInt()
            fnGas = data[3].toInt()
            loadLower = data.get2Bytes(4).toFloat() / MAP_MULTIPLIER
            loadUpper = data.get2Bytes(6).toFloat() / MAP_MULTIPLIER
            mapCurveOffset = data.get2Bytes(8).toFloat() / VOLTAGE_MULTIPLIER
            mapCurveGradient = data.get2Bytes(10).toFloat() / 2048 * 100
            map2CurveOffset = data.get2Bytes(12).toFloat() / VOLTAGE_MULTIPLIER
            map2CurveGradient = data.get2Bytes(14).toFloat() / 2048 * 100
            tpsCurveOffset = data.get2Bytes(16).toFloat() / VOLTAGE_MULTIPLIER
            tpsCurveGradient = data.get2Bytes(18).toFloat() / 4096 * 100
            loadSrcCfg = data[20].toInt()
            mapserUni = data[21].toInt()
            barocorrType = data[22].toInt()
//            funcFlags = data[23].toInt()
        }

    }
}