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

abstract class BaseSecu3Packet {

    var packetCrc: ByteArray = ByteArray(2)

    protected fun String.get2Bytes(startIndex: Int): Int {
        if (startIndex + 2 > length) {
            throw IllegalArgumentException("Packet too short; request ${startIndex + 2} but length is $length")
        }

        return this.substring(startIndex, startIndex + 2).binToInt()
    }

    protected fun String.get3Bytes(startIndex: Int): Int {
        if (startIndex + 3 > length) {
            throw IllegalArgumentException("Packet too short; request ${startIndex + 3} but length is $length")
        }
        return this.substring(startIndex, startIndex + 3).binToInt()
    }

    protected fun String.get4Bytes(startIndex: Int): Int {
        if (startIndex + 4 > length) {
            throw IllegalArgumentException("Packet too short; request ${startIndex + 4} but length is $length")
        }
        return this.substring(startIndex, startIndex + 4).binToInt()
    }

    private fun String.binToInt(): Int {
        var v = 0
        for (element in this) {
            v = v shl 8
            v = v or element.code
        }
        return v
    }

    protected fun Int.getBitValue(bitNumber: Int): Int {
        return this shr bitNumber and 1
    }

    protected fun Int.setBitValue(value: Boolean, bitNumber: Int): Int {
        return if (value) {
            1.shl(bitNumber).or(this)
        } else {
            1.shl(bitNumber).inv().and(this)
        }
    }

    var speedSensorPulses: Int = 0

    val periodDistance: Float
        get() = (1000.0f / speedSensorPulses)


    companion object {

        const val INPUT_PACKET_SYMBOL = '@'
        const val OUTPUT_PACKET_SYMBOL = '!'
        const val END_PACKET_SYMBOL = '\r'

        internal const val VOLTAGE_MULTIPLIER: Int = 400
        internal const val MAP_MULTIPLIER: Int = 64
        internal const val TEMPERATURE_MULTIPLIER: Int = 4
        internal const val TPS_MULTIPLIER: Int = 2
        internal const val GAS_DOSE_MULTIPLIER: Int = 2
        internal const val ANGLE_DIVIDER: Int = 32
        internal const val PARINJTIM_DIVIDER: Int = 16

        internal const val ADC_MULTIPLIER = 400
        internal const val CHOKE_MULTIPLIER = 2

        internal const val AFR_MULTIPLIER: Int = 128

        const val MAX_PACKET_SIZE = 250
    }
}