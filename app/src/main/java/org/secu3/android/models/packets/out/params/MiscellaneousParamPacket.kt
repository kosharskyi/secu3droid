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
import org.secu3.android.models.packets.base.protocolRoundToInt
import org.secu3.android.utils.getBitValue
import org.secu3.android.utils.setBitValue
import kotlin.math.roundToInt

data class MiscellaneousParamPacket(

    var uartDivisor: Int = 0,
    var uartPeriodTms: Int = 0,
    var ignCutoff: Int = 0,
    var ignCutoffThrd: Int = 0,
    var hopStartAng: Float = 0f,
    var hopDuratAng: Float = 0f,
    var flpmpFlags: Int = 0,
    var evapAfbegin: Int = 0,
    var evapAfslope: Float = 0f,
    var fpTimeoutStrt: Float = 0f,
    var pwmFrq0: Int = 0,
    var pwmFrq1: Int = 0,
    var vssPeriodDist: Float = 0f, //Number of VSS pulses per 1km

) : Secu3Packet(), InputPacket, OutputPacket {

    var evapAfEnd: Int
        get() {
            if (!evapAfslope.isFinite() || evapAfslope <= 0f) return evapAfbegin
            return (32.0f / evapAfslope + evapAfbegin).roundToInt()
        }
        set(value) {
            require(value - evapAfbegin >= MIN_EVAP_AIRFLOW_DELTA) { "EVAP airflow end must exceed start by at least $MIN_EVAP_AIRFLOW_DELTA RxL" }
            evapAfslope = 32.0f / (value - evapAfbegin)
        }


    var offPumpOnGas: Boolean
        get() = flpmpFlags.getBitValue(0) > 0
        set(value) {
            flpmpFlags = flpmpFlags.setBitValue(value, 0)
        }

    var offInjOnGas: Boolean
        get() = flpmpFlags.getBitValue(1) > 0
        set(value) {
            flpmpFlags = flpmpFlags.setBitValue(value, 1)
        }

    var offInjOnPetrol: Boolean
        get() = flpmpFlags.getBitValue(2) > 0
        set(value) {
            flpmpFlags = flpmpFlags.setBitValue(value, 2)
        }

    override fun pack(): IntArray {
        var data = intArrayOf(DESCRIPTOR.code)

        data += uartDivisor.write2Bytes()
        data += uartPeriodTms.div(10)
        data += ignCutoff
        data += ignCutoffThrd.write2Bytes()

        data += hopStartAng.times(ANGLE_DIVIDER).protocolRoundToInt().write2Bytes()
        data += hopDuratAng.times(ANGLE_DIVIDER).roundToInt().write2Bytes()
        data += flpmpFlags

        data += evapAfbegin.div(32).write2Bytes()
        require(evapAfEnd - evapAfbegin >= MIN_EVAP_AIRFLOW_DELTA) { "EVAP airflow pair is below the minimum safe range" }
        val evapSlopeRaw = (evapAfslope * 32f * 1048576f).roundToInt()
        require(evapAfslope.isFinite() && evapAfslope > 0f && evapSlopeRaw in 1..0xFFFF) { "EVAP slope cannot be encoded" }
        data += evapSlopeRaw.write2Bytes()

        data += fpTimeoutStrt.times(10).roundToInt()

        data += 1.0.div(pwmFrq0.toFloat()).times(524288.0f).roundToInt().write2Bytes()
        data += 1.0.div(pwmFrq1.toFloat()).times(524288.0f).roundToInt().write2Bytes()

        data +=  (1000.0f * 32768.0f).div(vssPeriodDist).roundToInt().write2Bytes()

        data += unhandledParams

        return data
    }

    override fun parse(data: IntArray): InputPacket {
        uartDivisor = data.get2Bytes()
        uartPeriodTms = data.get1Byte() * 10
        ignCutoff = data.get1Byte()
        ignCutoffThrd = data.get2Bytes()
        hopStartAng = data.get2BytesSigned().toFloat() / ANGLE_DIVIDER
        hopDuratAng = data.get2Bytes().toFloat() / ANGLE_DIVIDER
        flpmpFlags = data.get1Byte()
        evapAfbegin = data.get2Bytes() * 32
        evapAfslope = data.get2Bytes().toFloat().div(1048576.0f).div(32)
        fpTimeoutStrt = data.get1Byte().toFloat() / 10

        pwmFrq0 = data.get2Bytes().let {
            if (it == 0) {
                5000
            } else {
                1.0.div(it.toFloat().div(524288.0f)).roundToInt()
            }
        }
        pwmFrq1 = data.get2Bytes().let {
            if (it == 0) {
                5000
            } else {
                1.0.div(it.toFloat().div(524288.0f)).roundToInt()
            }
        }

        //Number of VSS pulses per 1km
        vssPeriodDist = data.get2Bytes().let {
            (1000.0f * 32768.0f) / it
        }

        data.setUnhandledParams()

        return this
    }

    companion object {
        internal const val DESCRIPTOR = 'z'
        const val MIN_EVAP_AIRFLOW_DELTA = 16500
    }
}
