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

data class AnglesParamPacket(
    var maxAngle: Float = 0f,
    var minAngle: Float = 0f,
    var angleCorrection: Float = 0f,
    var angleDecSpeed: Float = 0f,
    var angleIncSpeed: Float = 0f,
    var zeroAdvAngle: Int = 0,
    var igntimFlags: Int = 0,   // Ignition timing flags
    var shift_ingtim: Float = 0f, // Shift ignition timing (degrees)

) : Secu3Packet(), InputPacket, OutputPacket{

    var alwaysUseIgnitionMap: Boolean           // Allways use working mode's ignition timing map
        get() = igntimFlags.getBitValue(0) > 0
        set(value) { igntimFlags = igntimFlags.setBitValue(value, 0) }

    var applyManualTimingCorrOnIdl: Boolean           // Apply manual ignition timing correction on idling
        get() = igntimFlags.getBitValue(1) > 0
        set(value) { igntimFlags = igntimFlags.setBitValue(value, 1) }

    var zeroAdvAngleWithCorr: Boolean           // Zero advance angle with octane correction
        get() = igntimFlags.getBitValue(2) > 0
        set(value) { igntimFlags = igntimFlags.setBitValue(value, 2) }


    override fun parse(data: IntArray): InputPacket {
        maxAngle = data.get2Bytes().toShort().toFloat() / ANGLE_DIVIDER
        minAngle = data.get2Bytes().toShort().toFloat() / ANGLE_DIVIDER
        angleCorrection = data.get2Bytes().toShort().toFloat() / ANGLE_DIVIDER
        angleDecSpeed = data.get2Bytes().toShort().toFloat() / ANGLE_DIVIDER
        angleIncSpeed = data.get2Bytes().toShort().toFloat() / ANGLE_DIVIDER
        zeroAdvAngle = data.get1Byte()
        igntimFlags = data.get1Byte()
        shift_ingtim = data.get2Bytes().toFloat() / ANGLE_DIVIDER

        data.setUnhandledParams()

        return this
    }

    override fun pack(): IntArray {
        var data = intArrayOf(
            DESCRIPTOR.code
        )

        data += maxAngle.times(ANGLE_DIVIDER).roundToInt().write2Bytes()
        data += minAngle.times(ANGLE_DIVIDER).roundToInt().write2Bytes()
        data += angleCorrection.times(ANGLE_DIVIDER).roundToInt().write2Bytes()
        data += angleDecSpeed.times(ANGLE_DIVIDER).roundToInt().write2Bytes()
        data += angleIncSpeed.times(ANGLE_DIVIDER).roundToInt().write2Bytes()
        data += zeroAdvAngle
        data += igntimFlags
        data += shift_ingtim.times(ANGLE_DIVIDER).roundToInt().write2Bytes()

        data += unhandledParams

        return data
    }

    companion object {
        internal const val DESCRIPTOR = 'm'
    }
}
