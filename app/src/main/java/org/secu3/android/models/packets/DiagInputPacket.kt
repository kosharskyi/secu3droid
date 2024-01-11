/*
 *    SecuDroid  - An open source, free manager for SECU-3 engine control unit
 *    Copyright (C) 2024 Vitaliy O. Kosharskyi. Ukraine, Kyiv
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
package org.secu3.android.models.packets

import org.secu3.android.utils.getBitValue

data class DiagInputPacket(

    var flags: Int = 0,

    var voltage: Float = 0f,

    var map: Float = 0f,

    var temperature: Float = 0f,

    var addI1: Float = 0f,                // ADD_I1 voltage
    var addI2: Float = 0f,                // ADD_I2 voltage
    var addI3: Float = 0f,                // ADD_I3 voltage
    var addI4: Float = 0f,                // ADD_I4 voltage

    var addI5: Float = 0f,                // ADD_I5 voltage
    var addI6: Float = 0f,                // ADD_I6 voltage
    var addI7: Float = 0f,                // ADD_I7 voltage
    var addI8: Float = 0f,                // ADD_I8 voltage

    var carb: Float = 0f,

    var ks1: Float = 0f,
    var ks2: Float = 0f,

    var bits: Int = 0

) : BaseSecu3Packet() {

    val gasV: Boolean
        get() = bits.getBitValue(0) > 0

    val ckps: Boolean
        get() = bits.getBitValue(1) > 0

    val refS: Boolean
        get() = bits.getBitValue(2) > 0

    val ps: Boolean
        get() = bits.getBitValue(3) > 0

    val bl: Boolean
        get() = bits.getBitValue(4) > 0

    val de: Boolean
        get() = bits.getBitValue(5) > 0

    companion object {

        internal const val DESCRIPTOR = '='

        fun parse(data: String) = DiagInputPacket().apply {
            flags = data[2].code
            voltage = data.get2Bytes(3).toFloat() / VOLTAGE_MULTIPLIER
            map = data.get2Bytes(5).toFloat() / VOLTAGE_MULTIPLIER
            temperature = data.get2Bytes(7).toShort().toFloat() / VOLTAGE_MULTIPLIER

            addI1 = data.get2Bytes(9).toFloat() / VOLTAGE_MULTIPLIER
            addI2 = data.get2Bytes(11).toFloat() / VOLTAGE_MULTIPLIER
            addI3 = data.get2Bytes(13).toFloat() / VOLTAGE_MULTIPLIER
            addI4 = data.get2Bytes(15).toFloat() / VOLTAGE_MULTIPLIER

            addI5 = data.get2Bytes(17).toFloat() / VOLTAGE_MULTIPLIER
            addI6 = data.get2Bytes(19).toFloat() / VOLTAGE_MULTIPLIER
            addI7 = data.get2Bytes(21).toFloat() / VOLTAGE_MULTIPLIER
            addI8 = data.get2Bytes(23).toFloat() / VOLTAGE_MULTIPLIER

            carb = data.get2Bytes(25).toFloat() / VOLTAGE_MULTIPLIER

            ks1 = data.get2Bytes(27).toFloat() / VOLTAGE_MULTIPLIER
            ks2 = data.get2Bytes(29).toFloat() / VOLTAGE_MULTIPLIER

            bits = data.get2Bytes(31)

        }

    }

}