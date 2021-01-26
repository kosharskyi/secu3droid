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

data class TemperatureParamPacket(
    var tmpFlags: Int = 0,
    var ventOn: Int = 0,
    var ventOff: Int = 0,
    var ventPwmFrq: Int = 0,
    var condPvtOn: Float = 0f,
    var condPvtOff: Float = 0f,
    var condMinRpm: Int = 0,
    var ventTmr: Int = 0,

    ) : BaseOutputPacket() {

    override fun pack(): String {
        TODO("Not yet implemented")
    }

    val coolantUse: Boolean   //Flag of using coolant temperature sensor
        get() = tmpFlags.getBitValue(0) > 0

    val coolantMap: Boolean   //Flag which indicates using of lookup table for coolant temperature sensor
        get() = tmpFlags.getBitValue(1) > 0

    val ventPwm: Boolean   //Flag - control cooling fan by using PWM
        get() = tmpFlags.getBitValue(2) > 0


    companion object {

        internal const val DESCRIPTOR = 'j'

        fun parse(data: String) = TemperatureParamPacket().apply {

            tmpFlags = data[2].toInt()
            ventOn = data.get2Bytes(3) / TEMPERATURE_MULTIPLIER
            ventOff = data.get2Bytes(5) / TEMPERATURE_MULTIPLIER
            data.get2Bytes(7).let {
                ventPwmFrq = ((1f / it.toFloat()) * 524288).toInt()
            }
            condPvtOn = data.get2Bytes(9).toFloat() / VOLTAGE_MULTIPLIER
            condPvtOff = data.get2Bytes(11).toFloat() / VOLTAGE_MULTIPLIER
            condMinRpm = data.get2Bytes(13)
            ventTmr = data.get2Bytes(15) / 100

        }
    }

}
