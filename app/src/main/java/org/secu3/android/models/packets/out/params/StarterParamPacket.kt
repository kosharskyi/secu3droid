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
import org.secu3.android.utils.getBitValue
import org.secu3.android.utils.setBitValue
import kotlin.math.roundToInt

data class StarterParamPacket(
    var starterOff: Int = 0,
    var smapAbandon: Int = 0,
    var crankToRunTime: Float = 0f,
    var injAftstrStroke: Int = 0,
    var injPrimeCold: Float = 0f,
    var injPrimeHot: Float = 0f,
    var injPrimeDelay: Float = 0f,
    var injFloodclearTps: Float = 0f,
    var injAftStrokes1: Int = 0,
    var stblStrCnt: Int = 0,
    var strtFlags: Int = 0,

) : Secu3Packet(), InputPacket, OutputPacket{

    var allowStartOnClearFlood: Boolean
        get() = strtFlags.getBitValue(0) > 0
        set(value) {
            strtFlags = strtFlags.setBitValue(value, 0)
        }
    var limitMaxInjPwOnCranking: Boolean
        get() = strtFlags.getBitValue(1) > 0
        set(value) {
            strtFlags = strtFlags.setBitValue(value, 1)
        }


    override fun parse(data: IntArray): InputPacket {
        starterOff = data.get2Bytes()
        smapAbandon = data.get2Bytes()
        crankToRunTime = data.get2Bytes().toFloat() / 100
        injAftstrStroke = data.get1Byte() * 4
        injPrimeCold = data.get2Bytes().toFloat() * 32 / 10000
        injPrimeHot = data.get2Bytes().toFloat() * 32 / 10000
        injPrimeDelay = data.get1Byte().toFloat() / 10
        injFloodclearTps = data.get2Bytes().toFloat() / TPS_MULTIPLIER
        injAftStrokes1 = data.get1Byte() * 4
        stblStrCnt = data.get1Byte()
        strtFlags = data.get1Byte()

        data.setUnhandledParams()

        return this
    }

    override fun pack(): IntArray {
        var data = intArrayOf(DESCRIPTOR.code)

        data += starterOff.write2Bytes()
        data += smapAbandon.write2Bytes()
        data += crankToRunTime.times(100).roundToInt().write2Bytes()
        data += injAftstrStroke.div(4)
        data += injPrimeCold.times(10000).div(32).roundToInt().write2Bytes()
        data += injPrimeHot.times(10000).div(32).roundToInt().write2Bytes()
        data += injPrimeDelay.times(10).roundToInt()
        data += injFloodclearTps.times(TPS_MULTIPLIER).roundToInt().write2Bytes()
        data += injAftStrokes1.div(4)
        data += stblStrCnt
        data += strtFlags

        data += unhandledParams

        return data
    }

    companion object {
        internal const val DESCRIPTOR = 'o'
    }
}
