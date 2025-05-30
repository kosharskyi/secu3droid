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
import org.secu3.android.models.packets.out.params.UniOutParamPacket.Companion.UNI_OUTPUT_NUM
import org.secu3.android.utils.getBitValue
import org.secu3.android.utils.setBitValue
import kotlin.math.roundToInt

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
    var mapselUni: Int = 0,
    var barocorrType: Int = 0,
    var funcFlags: Int = 0,

    var ve2MapFunc: Int = 0,
    var gasVUni: Int = 0,

    var ckpsEngineCyl: Int = 0, //used for calculations on SECU-3 Manager side
    var injCylDisp: Float = 0f,    //used for calculations on SECU-3 Manager side
    var mafload_const: Float = 0f, //calculated in manager before send
    var tps_raw: Float = 0f,        //for TPS learning

    var gpsCurveOffset: Float = 0f,     // Gas Pressure Sensor
    var gpsCurveGradient: Float = 0f,   // Gas Pressure Sensor

    var fpsCurveOffset: Float = 0f,     // Fuel Pressure Sensor
    var fpsCurveGradient: Float = 0f,   // Fuel Pressure Sensor

    var apps1CurveOffset: Float = 0f,     // Accelerator pedal position sensor
    var apps1CurveGradient: Float = 0f,   // APPS
    var apps1Raw: Int = 0,              // Redundant: for APPS1 learning


): Secu3Packet(), InputPacket, OutputPacket {

    override fun pack(): IntArray {
        var data = intArrayOf(DESCRIPTOR.code)

        data += fnGasoline
        data += fnGas
        data += loadLower.times(MAP_MULTIPLIER).roundToInt().write2Bytes()
        data += loadUpper.times(MAP_MULTIPLIER).roundToInt().write2Bytes()
        data += mapCurveOffset.div(ADC_DISCRETE).roundToInt().write2Bytes()
        data += mapCurveGradient.times(128.0f).times(MAP_MULTIPLIER).times(ADC_DISCRETE).roundToInt().write2Bytes()
        data += map2CurveOffset.div(ADC_DISCRETE).roundToInt().write2Bytes()
        data += map2CurveGradient.times(128.0f).times(MAP_MULTIPLIER).times(ADC_DISCRETE).roundToInt().write2Bytes()
        data += tpsCurveOffset.div(ADC_DISCRETE).roundToInt().write2Bytes()
        data += tpsCurveGradient.times(128.0f).times(TPS_MULTIPLIER * 64f).times(ADC_DISCRETE).roundToInt().write2Bytes()

        data += loadSrcCfg
        data += mapselUni
        data += barocorrType
        data += funcFlags

        data += ve2MapFunc

        if (gasVUni == UNI_OUTPUT_NUM) {
            data += 0xF
        } else {
            data += gasVUni
        }

        data += 0 // stub for cyl_num

        data += injCylDisp.times(16384.0f).roundToInt().write2Bytes()
        data += mafload_const.toInt().write4Bytes()

        data += gpsCurveOffset.div(ADC_DISCRETE).roundToInt().write2Bytes()
        data += gpsCurveGradient.times(128.0f).times(MAP_MULTIPLIER).times(ADC_DISCRETE).roundToInt().write2Bytes()

        data += fpsCurveOffset.div(ADC_DISCRETE).roundToInt().write2Bytes()
        data += fpsCurveGradient.times(128.0f).times(MAP_MULTIPLIER).times(ADC_DISCRETE).roundToInt().write2Bytes()

        data += apps1CurveOffset.div(ADC_DISCRETE).roundToInt().write2Bytes()
        data += apps1CurveGradient.times(128.0f).times(APPS_MULT * 2).times(ADC_DISCRETE).roundToInt().write2Bytes()

        data += unhandledParams

        return data
    }

    var mapselUniPetrol: Int
        get() = mapselUni.and(0xF)
        set(value) {
            mapselUni = mapselUni.and(0xF0).or(value)
        }

    var mapselUniGas: Int
        get() = mapselUni.shr(4)
        set(value) {
            mapselUni = mapselUni.and(0xF).or(value.shl(4))
        }

    var useLoadGrid: Boolean
        get() = funcFlags.getBitValue(0) > 0
        set(value) {
            funcFlags = funcFlags.setBitValue(value, 0)
        }

    override fun parse(data: IntArray): InputPacket {
        fnGasoline = data.get1Byte()
        fnGas = data.get1Byte()
        loadLower = data.get2Bytes().toFloat() / MAP_MULTIPLIER
        loadUpper = data.get2Bytes().toFloat() / MAP_MULTIPLIER
        mapCurveOffset = data.get2Bytes().toFloat() * ADC_DISCRETE
        mapCurveGradient = data.get2Bytes().toFloat() / (MAP_MULTIPLIER * ADC_DISCRETE * 128.0f)
        map2CurveOffset = data.get2Bytes().toFloat() * ADC_DISCRETE
        map2CurveGradient = data.get2Bytes().toFloat() / (MAP_MULTIPLIER * ADC_DISCRETE * 128.0f)
        tpsCurveOffset = data.get2Bytes().toFloat() * ADC_DISCRETE
        tpsCurveGradient = data.get2Bytes().toFloat() / (TPS_MULTIPLIER.times(64) * ADC_DISCRETE * 128.0f)
        loadSrcCfg = data.get1Byte()
        mapselUni = data.get1Byte()
        barocorrType = data.get1Byte()
        funcFlags = data.get1Byte()

        ve2MapFunc = data.get1Byte()
        gasVUni = data.get1Byte()

        ckpsEngineCyl = data.get1Byte()
        injCylDisp = data.get2Bytes().toFloat().div(16384.0f)
        mafload_const = data.get4Bytes().toFloat()
        tps_raw = data.get2Bytes().times(ADC_DISCRETE)

        gpsCurveOffset = data.get2Bytes().toFloat() * ADC_DISCRETE
        gpsCurveGradient = data.get2Bytes().toFloat() / (MAP_MULTIPLIER * ADC_DISCRETE * 128.0f)

        fpsCurveOffset = data.get2Bytes().toFloat() * ADC_DISCRETE
        fpsCurveGradient = data.get2Bytes().toFloat() / (MAP_MULTIPLIER * ADC_DISCRETE * 128.0f)

        apps1CurveOffset = data.get2Bytes().toFloat() * ADC_DISCRETE
        apps1CurveGradient = data.get2Bytes().toFloat() / ((APPS_MULT * 2) * ADC_DISCRETE * 128.0f)

        data.setUnhandledParams()

        return this
    }

    companion object {
        internal const val DESCRIPTOR = 'n'
    }
}