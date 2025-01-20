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

data class TemperatureParamPacket(
    var tmpFlags: Int = 0,
    var ventOn: Float = 0f,
    var ventOff: Float = 0f,
    var ventPwmFrq: Int = 0,
    var condPvtOn: Float = 0f,
    var condPvtOff: Float = 0f,
    var condMinRpm: Int = 0,
    var ventTmr: Int = 0,

    ) : BaseOutputPacket() {

    override fun pack(): String {
        var data = "$OUTPUT_PACKET_SYMBOL$DESCRIPTOR"

        data += tmpFlags.toChar()

        data += ventOn.times(TEMPERATURE_MULTIPLIER).roundToInt().write2Bytes()
        data += ventOff.times(TEMPERATURE_MULTIPLIER).roundToInt().write2Bytes()

        data += 1f.div(ventPwmFrq.toFloat()).times(524288.0).roundToInt().write2Bytes()

        data += condPvtOn.div(ADC_DISCRETE).roundToInt().write2Bytes()
        data += condPvtOff.div(ADC_DISCRETE).roundToInt().write2Bytes()

        data += condMinRpm.write2Bytes()
        data += ventTmr.times(100).write2Bytes()

        data += unhandledParams

        return data
    }

    var coolantUse: Boolean   //Flag of using coolant temperature sensor
        get() = tmpFlags.getBitValue(0) > 0
        set(value) {
            tmpFlags = tmpFlags.setBitValue(value, 0)
        }

    var coolantMap: Boolean   //Flag which indicates using of lookup table for coolant temperature sensor
        get() = tmpFlags.getBitValue(1) > 0
        set(value) {
            tmpFlags = tmpFlags.setBitValue(value, 1)
        }

    var ventPwm: Boolean   //Flag - control cooling fan by using PWM
        get() = tmpFlags.getBitValue(2) > 0
        set(value) {
            tmpFlags = tmpFlags.setBitValue(value, 2)
        }


    companion object {

        internal const val DESCRIPTOR = 'j'

        fun parse(data: String) = TemperatureParamPacket().apply {

            tmpFlags = data.get1Byte()
            ventOn = data.get2Bytes().toShort().toFloat().div(TEMPERATURE_MULTIPLIER)
            ventOff = data.get2Bytes().toShort().toFloat().div(TEMPERATURE_MULTIPLIER)
            data.get2Bytes().let {
                ventPwmFrq = (1f / (( it.toDouble() / 524288))).roundToInt()
            }
            condPvtOn = data.get2Bytes().toFloat() * ADC_DISCRETE
            condPvtOff = data.get2Bytes().toFloat() * ADC_DISCRETE
            condMinRpm = data.get2Bytes()
            ventTmr = data.get2Bytes() / 100

            data.setUnhandledParams()
        }
    }

}
