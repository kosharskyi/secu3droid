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

data class GasDoseParamPacket(

    var steps: Int = 0,
    var testing: Int = 0,           //fake parameter (actually it is status)
    var manualPositionD: Int = 0,   //fake parameter
    var fcClosing: Float = 0f,
    var lambdaCorrLimitP: Float = 0f,
    var lambdaCorrLimitM: Float = 0f,
    var lambdaStoichval: Float = 0f,
    var freq: Int = 0,
    var maxFreqInit: Int = 0,

): BaseOutputPacket() {

    override fun pack(): String {
        var data = "$OUTPUT_PACKET_SYMBOL$DESCRIPTOR"

        data += steps.write2Bytes()
        data += testing.toChar()
        data += manualPositionD.toChar()
        data += fcClosing.times(GAS_DOSE_MULTIPLIER).toChar()
        data += lambdaCorrLimitP.times(512.0f).div(100).toInt().write2Bytes()
        data += lambdaCorrLimitM.times(512.0f).div(100).toInt().write2Bytes()
        data += lambdaStoichval.times(AFR_MULTIPLIER).toInt().write2Bytes()
        data += freq.toChar()
        data += maxFreqInit.toChar()

        return data
    }

    companion object {

        internal const val DESCRIPTOR = '*'

        fun parse(data: String) = GasDoseParamPacket().apply {
            steps = data.get2Bytes(2)
            fcClosing = data[6].toFloat() / GAS_DOSE_MULTIPLIER
            lambdaCorrLimitP = data.get2Bytes(7).toFloat().times(100.0f).div(512.0f)
            lambdaCorrLimitM = data.get2Bytes(9).toFloat().times(100.0f).div(512.0f)
            lambdaStoichval = data.get2Bytes(11).toFloat() / AFR_MULTIPLIER
            freq = data[13].toInt()
            maxFreqInit = data[14].toInt()
        }
    }
}
