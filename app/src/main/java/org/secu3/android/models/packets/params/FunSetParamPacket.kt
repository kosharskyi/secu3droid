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
import org.secu3.android.models.packets.params.UniOutParamPacket.Companion.UNI_OUTPUT_NUM

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

    var ve2MapFunc: Int = 0,
    var gasVUni: Int = 0,

    var ckpsEngineCyl: Int = 0, //used for calculations on SECU-3 Manager side
    var injCylDisp: Float = 0f,    //used for calculations on SECU-3 Manager side
    var mafload_const: Int = 0, //calculated in manager before send
    var tps_raw: Int = 0,        //for TPS learning

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
        data += funcFlags.toChar()

        data += ve2MapFunc.toChar()

        if (gasVUni == UNI_OUTPUT_NUM) {
            data += 0xF
        } else {
            data += gasVUni.toChar()
        }

        data += 0.toChar() // stub for cyl_num

        data += injCylDisp.times(16384.0f).toInt().write2Bytes()
        data += mafload_const.write4Bytes()
        data += tps_raw.write2Bytes()

        data += unhandledParams

        return data
    }

    var mapserUniPetrol: Int
        get() = mapserUni.and(0xF)
        set(value) {
            mapserUni = mapserUni.and(0xF0).or(value)
        }

    var mapserUniGas: Int
        get() = mapserUni.and(0xF0).shr(4)
        set(value) {
            mapserUni = mapserUni.and(0xF).or(value.shl(4))
        }

    companion object {

        internal const val DESCRIPTOR = 'n'

        fun parse(data: String) = FunSetParamPacket().apply {

            fnGasoline = data[2].code
            fnGas = data[3].code
            loadLower = data.get2Bytes(4).toFloat() / MAP_MULTIPLIER
            loadUpper = data.get2Bytes(6).toFloat() / MAP_MULTIPLIER
            mapCurveOffset = data.get2Bytes(8).toFloat() / VOLTAGE_MULTIPLIER
            mapCurveGradient = data.get2Bytes(10).toFloat() / 2048 * 100
            map2CurveOffset = data.get2Bytes(12).toFloat() / VOLTAGE_MULTIPLIER
            map2CurveGradient = data.get2Bytes(14).toFloat() / 2048 * 100
            tpsCurveOffset = data.get2Bytes(16).toFloat() / VOLTAGE_MULTIPLIER
            tpsCurveGradient = data.get2Bytes(18).toFloat() / 4096 * 100
            loadSrcCfg = data[20].code
            mapserUni = data[21].code
            barocorrType = data[22].code
            funcFlags = data[23].code

            ve2MapFunc = data[24].code
            gasVUni = data[25].code

            ckpsEngineCyl = data[26].code
            injCylDisp = data.get2Bytes(27).toFloat().div(16384.0f)
            mafload_const = data.get4Bytes(29)
            tps_raw = data.get2Bytes(33)

            if (data.length == 35) {
                return@apply
            }

            unhandledParams = data.substring(35)
        }

    }
}