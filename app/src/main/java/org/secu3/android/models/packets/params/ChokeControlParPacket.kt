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

data class ChokeControlParPacket(

    var smSteps: Int = 0,

    var rpmIf: Float = 0f,

    var corrTime0: Float = 0f,
    var corrTime1: Float = 0f,

    var flags: Int = 0,

    var smFreq: Int = 0,

    var injCrankToRunTime: Float = 0f

) : BaseOutputPacket() {

    val useClosedLoopRmpRegulator: Boolean
        get() = flags.getBitValue(0) > 0

    val dontUseRpmRegOnGas: Boolean
        get() = flags.getBitValue(1) > 0

    val useThrottlePosInChokeInit: Boolean
        get() = flags.getBitValue(2) > 0

    val maxSTEPfreqAtInit: Boolean
        get() = flags.getBitValue(3) > 0

    override fun pack(): String {
        TODO("Not yet implemented")
    }

    companion object {

        internal const val DESCRIPTOR = '%'

        fun parse(data: String) = ChokeControlParPacket().apply {

            smSteps = data.get2Bytes(2)
            rpmIf = data.get2Bytes(6).toFloat() / 1000
            corrTime0 = data.get2Bytes(8).toFloat() / 100
            corrTime1 = data.get2Bytes(10).toFloat() / 100
            flags = data[12].toInt()
            smFreq = data[13].toInt()
            injCrankToRunTime = data.get2Bytes(14).toFloat() / 100

        }
    }
}
