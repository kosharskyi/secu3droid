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
import org.secu3.android.utils.getBitValue
import org.secu3.android.utils.setBitValue
import kotlin.math.roundToInt

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
    var idlRegP0: Float = 0f,
    var idlRegP1: Float = 0f,
    var idlRegI0: Float = 0f,
    var idlRegI1: Float = 0f,
    var coefThrd1: Float = 0f,
    var coefThrd2: Float = 0f,
    var integratorRpmLim: Int = 0,
    var mapValue: Float = 0f,
    var iacMinPos: Float = 0f,
    var iacMaxPos: Float = 0f,
    var iacRegDb: Int = 0,
    var idlRegD: Float = 0f,

    var irrKLoad: Float = 0f,
    var irrKRpm: Float = 0f,


    ) : BaseOutputPacket() {

    var useRegulator: Boolean
        get() = idlFlags.getBitValue(0) > 0
        set(value) {
            idlFlags = idlFlags.setBitValue(value, 0)
        }

    var useRegulatorOnGas: Boolean
        get() = idlFlags.getBitValue(1) > 0
        set(value) {
            idlFlags = idlFlags.setBitValue(value, 1)
        }

    var useClosedLoop: Boolean
        get() = idlFlags.getBitValue(2) > 0
        set(value) {
            idlFlags = idlFlags.setBitValue(value, 2)
        }

    var pRegMode: Boolean
        get() = idlFlags.getBitValue(3) > 0
        set(value) {
            idlFlags = idlFlags.setBitValue(value, 3)
        }

    var useClosedLoopOnGas: Boolean
        get() = idlFlags.getBitValue(4) > 0
        set(value) {
            idlFlags = idlFlags.setBitValue(value, 4)
        }

    var useThrassmap: Boolean
        get() = idlFlags.getBitValue(5) > 0
        set(value) {
            idlFlags = idlFlags.setBitValue(value, 5)
        }

    var idlRegWorksWithIAC: Boolean // Ign. time idling regulator works together with IAC regulator
        get() = idlFlags.getBitValue(6) > 0
        set(value) {
            idlFlags = idlFlags.setBitValue(value, 6)
        }


    companion object {

        internal const val DESCRIPTOR = 'l'

        fun parse(data: IntArray) = IdlingParamPacket().apply {
            idlFlags = data.get1Byte()
            iFac1 = data.get2Bytes().toShort().toFloat() / 256
            iFac2 = data.get2Bytes().toShort().toFloat() / 256
            minefr = data.get2Bytes()
            idlingRpm = data.get2Bytes()
            idlregMinAngle = data.get2Bytes().toShort().toFloat() / ANGLE_DIVIDER
            idlregMaxAngle = data.get2Bytes().toShort().toFloat() / ANGLE_DIVIDER
            idlregTurnOnTemp = data.get2Bytes().toShort().toFloat() / TEMPERATURE_MULTIPLIER
            idlToRunAdd = data.get1Byte().toFloat() / 2.0f
            rpmOnRunAdd = data.get1Byte() * 10
            idlRegP0 = data.get2Bytes().toFloat() / 256
            idlRegP1 = data.get2Bytes().toFloat() / 256
            idlRegI0 = data.get2Bytes().toFloat() / 256
            idlRegI1 = data.get2Bytes().toFloat() / 256
            coefThrd1 = data.get1Byte().toFloat().div(128).plus(1.0f)
            coefThrd2 = data.get1Byte().toFloat().div(128).plus(1.0f)
            integratorRpmLim = data.get1Byte().times(10)
            mapValue = data.get2Bytes().toFloat() / MAP_MULTIPLIER
            iacMinPos = data.get1Byte().toFloat() / 2
            iacMaxPos = data.get1Byte().toFloat() / 2
            iacRegDb = data.get2Bytes()
            idlRegD = data.get2Bytes().toFloat() / 256

            irrKLoad = data.get2Bytes().toFloat() / 32.0f
            irrKRpm = data.get2Bytes().toFloat() / 32.0f

            data.setUnhandledParams()
        }
    }

    override fun pack(): IntArray {
        var data = intArrayOf(DESCRIPTOR.code)

        data += idlFlags
        data += iFac1.times(256).roundToInt().write2Bytes()
        data += iFac2.times(256).roundToInt().write2Bytes()
        data += minefr.write2Bytes()
        data += idlingRpm.write2Bytes()
        data += idlregMinAngle.times(ANGLE_DIVIDER).roundToInt().write2Bytes()
        data += idlregMaxAngle.times(ANGLE_DIVIDER).roundToInt().write2Bytes()
        data += idlregTurnOnTemp.times(TEMPERATURE_MULTIPLIER).roundToInt().write2Bytes()
        data += idlToRunAdd.times(2.0f).roundToInt()
        data += rpmOnRunAdd.div(10)
        data += idlRegP0.times(256).roundToInt().write2Bytes()
        data += idlRegP1.times(256).roundToInt().write2Bytes()
        data += idlRegI0.times(256).roundToInt().write2Bytes()
        data += idlRegI1.times(256).roundToInt().write2Bytes()
        data += coefThrd1.minus(1.0f).times(128).roundToInt()
        data += coefThrd2.minus(1.0f).times(128).roundToInt()
        data += integratorRpmLim.div(10)
        data += mapValue.times(MAP_MULTIPLIER).roundToInt().write2Bytes()
        data += iacMinPos.times(2).roundToInt()
        data += iacMaxPos.times(2).roundToInt()
        data += iacRegDb.write2Bytes()
        data += idlRegD.times(256).roundToInt().write2Bytes()

        data += irrKLoad.times(32.0f).roundToInt().write2Bytes()
        data += irrKRpm.times(32.0f).roundToInt().write2Bytes()

        data += unhandledParams

        return data
    }
}
