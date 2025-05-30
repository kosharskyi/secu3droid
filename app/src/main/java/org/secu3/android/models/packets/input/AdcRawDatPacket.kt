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
package org.secu3.android.models.packets.input

import org.secu3.android.models.packets.base.Secu3Packet
import org.secu3.android.models.packets.base.InputPacket

data class AdcRawDatPacket(
    var map: Float = 0f,
    var voltage: Float = 0f,
    var temperature: Float = 0f,
    var knockValue: Float = 0f,
    var tps: Float = 0f,                  // TPS throttle position sensor (0...100%, x2)
    var addI1: Float = 0f,                // ADD_I1 voltage
    var addI2: Float = 0f,                // ADD_I2 voltage
    var addI3: Float = 0f,                // ADD_I3 voltage
    var addI4: Float = 0f,                // ADD_I4 voltage
    var addI5: Float = 0f,                // ADD_I5 voltage
    var addI6: Float = 0f,                // ADD_I6 voltage
    var addI7: Float = 0f,                // ADD_I7 voltage
    var addI8: Float = 0f,                // ADD_I8 voltage

) : Secu3Packet(), InputPacket {

    override fun parse(data: IntArray): InputPacket {
        map = data.get2Bytes().toShort().toFloat() / VOLTAGE_MULTIPLIER
        voltage = data.get2Bytes().toShort().toFloat() / VOLTAGE_MULTIPLIER
        temperature = data.get2Bytes().toShort().toFloat() / VOLTAGE_MULTIPLIER
        knockValue = data.get2Bytes().toShort().toFloat() / VOLTAGE_MULTIPLIER
        tps = data.get2Bytes().toShort().toFloat() / VOLTAGE_MULTIPLIER
        addI1 = data.get2Bytes().toShort().toFloat() / VOLTAGE_MULTIPLIER
        addI2 = data.get2Bytes().toShort().toFloat() / VOLTAGE_MULTIPLIER
        addI3 = data.get2Bytes().toShort().toFloat() / VOLTAGE_MULTIPLIER
        addI4 = data.get2Bytes().toShort().toFloat() / VOLTAGE_MULTIPLIER
        addI5 = data.get2Bytes().toShort().toFloat() / VOLTAGE_MULTIPLIER
        addI6 = data.get2Bytes().toShort().toFloat() / VOLTAGE_MULTIPLIER
        addI7 = data.get2Bytes().toShort().toFloat() / VOLTAGE_MULTIPLIER
        addI8 = data.get2Bytes().toShort().toFloat() / VOLTAGE_MULTIPLIER

        return this
    }

    companion object {
        internal const val DESCRIPTOR = 's'
    }
}