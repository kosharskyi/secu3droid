/*
 *    SecuDroid  - An open source, free manager for SECU-3 engine control unit
 *    Copyright (C) 2025 Vitalii O. Kosharskyi. Ukraine, Kyiv
 *
 *    SECU-3  - An open source, free engine control unit
 *    Copyright (C) 2007-2025 Alexey A. Shabelnikov. Ukraine, Kyiv
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
import kotlin.math.roundToInt

data class DbwParamPacket (

    var etc_p: Float = 0f,
    var etc_i: Float = 0f,
    var etc_d: Float = 0f,
    var etc_nmax_duty: Float = 0f,
    var etc_pmax_duty: Float = 0f,
    var pid_period: Float = 0f,
    var frictorq_open: Float = 0f,
    var frictorq_close: Float = 0f,
    var frictorq_thrd: Float = 0f,
    var frictorq_idleadd_max: Float = 0f,
    var homePosition_edited: Float = 0f


) : Secu3Packet(), InputPacket, OutputPacket {

    override fun pack(): IntArray {
        var data = intArrayOf(DESCRIPTOR.code)

        data += etc_p.times(ETCPID_MULT).roundToInt().write2Bytes()
        data += etc_i.times(ETCPID_MULT).roundToInt().write2Bytes()
        data += etc_d.times(ETCPID_MULT).roundToInt().write2Bytes()
        data += etc_nmax_duty.times(2.0f).roundToInt().write1Byte()
        data += etc_pmax_duty.times(2.0f).roundToInt().write1Byte()
        data += pid_period.times(100.0f).roundToInt().write1Byte()
        data += frictorq_open.times(16.0f).roundToInt().write1Byte()
        data += frictorq_close.times(16.0f).roundToInt().write1Byte()
        data += frictorq_thrd.roundToInt().write1Byte()
        data += frictorq_idleadd_max.times(TPS_MULTIPLIER).roundToInt().write2Bytes()

        data += unhandledParams

        return data
    }

    override fun parse(data: IntArray): InputPacket {
        etc_p = data.get2Bytes().toFloat().div(ETCPID_MULT)
        etc_i = data.get2Bytes().toFloat().div(ETCPID_MULT)
        etc_d = data.get2Bytes().toFloat().div(ETCPID_MULT)
        etc_nmax_duty = data.get1Byte().toFloat().div(2.0f)
        etc_pmax_duty = data.get1Byte().toFloat().div(2.0f)
        pid_period = data.get1Byte().toFloat().div(100.0f)
        frictorq_open = data.get1Byte().toFloat().div(16.0f)
        frictorq_close = data.get1Byte().toFloat().div(16.0f)
        frictorq_thrd = data.get1Byte().toFloat()
        frictorq_idleadd_max = data.get2Bytes().toFloat().div(TPS_MULTIPLIER)
        homePosition_edited = data.get2Bytes().toFloat().div(TPS_MULTIPLIER)

//        data.setUnhandledParams()

        return this
    }


    companion object {

        internal const val DESCRIPTOR = 'P'

    }
}