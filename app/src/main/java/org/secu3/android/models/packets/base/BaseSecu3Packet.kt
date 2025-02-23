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
package org.secu3.android.models.packets.base

abstract class BaseSecu3Packet {

    protected var currentIndex = 2  // ignores @ and ! and descriptor

    protected var unhandledParams: IntArray = intArrayOf()
        private set

    protected fun IntArray.get1Byte(): Int {
        if (currentIndex >= size) {
            throw IllegalArgumentException("Packet too short; request $currentIndex but length is $size")
        }

        return this[currentIndex++] and 0xFF
    }

    protected fun IntArray.get2Bytes(): Int {
        if (currentIndex + 1 >= size) {
            throw IllegalArgumentException("Packet too short; request ${currentIndex + 2} but length is $size")
        }

        return this.sliceArray(currentIndex until currentIndex + 2).binToInt().also {
            currentIndex += 2
        }
    }

    protected fun IntArray.get3Bytes(): Int {
        if (currentIndex + 2 >= size) {
            throw IllegalArgumentException("Packet too short; request ${currentIndex + 3} but length is $size")
        }

        return this.sliceArray(currentIndex until currentIndex + 3).binToInt().also {
            currentIndex += 3
        }
    }

    protected fun IntArray.get4Bytes(): Int {
        if (currentIndex + 3 >= size) {
            throw IllegalArgumentException("Packet too short; request ${currentIndex + 4} but length is $size")
        }

        return this.sliceArray(currentIndex until currentIndex + 4).binToInt().also {
            currentIndex += 4
        }
    }

    protected fun IntArray.getString(length: Int): String {
        if (currentIndex + length > size) {
            throw IllegalArgumentException("Packet too short; request ${currentIndex + length} but length is $size")
        }

        val array = this.sliceArray(currentIndex until currentIndex + length).map { it.toByte() }.toByteArray()

        return String(array, Charsets.ISO_8859_1).also {
            currentIndex += length
        }
    }

    protected fun IntArray.setUnhandledParams() {

        if (currentIndex >= size) {
            return
        }

        unhandledParams = this.sliceArray(currentIndex until size)
    }

    private fun IntArray.binToInt(): Int {
        var v = 0
        for (element in this) {
            v = v shl 8
            v = v or element
        }
        return v
    }

    companion object {
        internal const val VOLTAGE_MULTIPLIER: Int = 400
        internal const val MAP_MULTIPLIER: Int = 64
        internal const val TEMPERATURE_MULTIPLIER: Int = 4
        internal const val TPS_MULTIPLIER: Int = 64
        internal const val GAS_DOSE_MULTIPLIER: Int = 2
        internal const val ANGLE_DIVIDER: Int = 32
        internal const val PARINJTIM_DIVIDER: Int = 16

        internal const val ADC_DISCRETE = 0.0025f
        internal const val CHOKE_MULTIPLIER = 2

        internal const val AFR_MULTIPLIER: Int = 128
        internal const val LOAD_PHYSICAL_MAGNITUDE_MULTIPLIER = 64
        internal const val PARINJTIM_MULT = 16.0f;
        internal const val FTLS_MULT = 64.0f;
        internal const val EGTS_MULT = 4.0f;
        internal const val OPS_MULT = 256.0f;
        internal const val INJPWCOEF_MULT = 4096.0f;
        internal const val MAFS_MULT = 64.0f;
        internal const val FTS_MULT = 4.0f;
        internal const val APPS_MULT = 64
        internal const val ETCPID_MULT = 4096.0f
    }
}