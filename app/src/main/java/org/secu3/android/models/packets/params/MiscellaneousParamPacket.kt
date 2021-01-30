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

data class MiscellaneousParamPacket(

    var uartDivisor: Int = 0,
    var uartPeriodTms: Int = 0,
    var ignCutoff: Int = 0,
    var ignCutoffThrd: Int = 0,
    var hopStartCogs: Int = 0,
    var hopDuratCogs: Int = 0,
    var flpmpFlags: Int = 0,
    var evapAfbegin: Int = 0,
    var evapAfslope: Float = 0f,
    var fpTimeoutStrt: Float = 0f,
    var pwmFrq0: Int = 0,
    var pwmFrq1: Int = 0,

    ) : BaseOutputPacket() {

    var evapAfEnd: Int
        get() {
            val slope = if (evapAfslope > .0f) evapAfslope else 0.000001f
            return (32.0f / slope + evapAfbegin).toInt() //convert afslope to afend
        }
        set(value) {
            evapAfslope = 32.0f / (value - evapAfbegin); //convert afend to afslope
        }


    val offPumpOnGas: Boolean
        get() = flpmpFlags.getBitValue(0) > 0

    val offInjOnGas: Boolean
        get() = flpmpFlags.getBitValue(1) > 0

    val offInjOnPetrol: Boolean
        get() = flpmpFlags.getBitValue(2) > 0

    override fun pack(): String {
        var data = "$OUTPUT_PACKET_SYMBOL$DESCRIPTOR"

        data += uartDivisor.write2Bytes(data)
        data += uartPeriodTms.div(10).toChar()
        data += ignCutoff.toChar()
        data += ignCutoffThrd.write2Bytes(data)

        data += hopStartCogs.toChar()
        data += hopDuratCogs.toChar()
        data += flpmpFlags.toChar()

        data += evapAfbegin.div(32).write2Bytes(data)
        data += evapAfEnd.times(32).times(1048576.0f).toChar()

        data += fpTimeoutStrt.times(10).toChar()

//        data += pwmFrq0.write2Bytes(data)
//        data += pwmFrq0.write2Bytes(data)

        data += END_PACKET_SYMBOL
        return data
    }

    companion object {

        internal const val DESCRIPTOR = 'z'

        fun parse(data: String) = MiscellaneousParamPacket().apply {
            uartDivisor = data.get2Bytes(2)
            uartPeriodTms = data[4].toInt() * 10
            ignCutoff = data[5].toInt()
            ignCutoffThrd = data.get2Bytes(6)
            hopStartCogs = data[8].toInt()
            hopDuratCogs = data[9].toInt()
            flpmpFlags = data[10].toInt()
            evapAfbegin = data.get2Bytes(11) * 32
            evapAfslope = data.get2Bytes(13).toFloat().div(1048576.0f).div(32)
            fpTimeoutStrt = data[15].toFloat() / 10
//            pwmFrq0 = data.get2Bytes(16)
//            pwmFrq1 = data.get2Bytes(18)
        }
    }

}
