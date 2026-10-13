/*
 *    SecuDroid  - An open source, free manager for SECU-3 engine control unit
 *    Copyright (C) 2026 Vitalii O. Kosharskyi. Ukraine, Kyiv
 *
 *    SECU-3  - An open source, free engine control unit
 *    Copyright (C) 2007-2026 Alexey A. Shabelnikov. Ukraine, Kyiv
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
package org.secu3.android.ui.parameters.pages.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.secu3.android.models.packets.out.params.LambdaParamPacket

/** Owns Lambda Control packet state and sends only explicit, valid user edits. */
class LambdaControlStateHolder(
    private val sendPacket: (LambdaParamPacket) -> Unit,
    private val onPacketMapping: () -> Unit = {},
    private val onPacketMapped: () -> Unit = {},
) {
    private var packet: LambdaParamPacket? = null
    var values: LambdaValues? by mutableStateOf(null)
        private set

    fun onPacketReceived(received: LambdaParamPacket) {
        onPacketMapping()
        packet = received
        values = LambdaValues.from(received)
        onPacketMapped()
    }

    fun updateValue(number: LambdaNumber, value: Float) {
        val current = packet ?: return
        applyLambdaNumber(current, number, value)
        val applied = if (number == LambdaNumber.MS) current.msPerStp.toFloat() else value.coerceIn(number.min, number.max)
        values = values?.withValue(number, applied)
        sendPacket(current)
    }

    fun updateFlag(flag: LambdaFlag, checked: Boolean) {
        val current = packet ?: return
        applyLambdaFlag(current, flag, checked)
        values = values?.withFlag(flag, checked)
        sendPacket(current)
    }

    fun updateChannel(index: Int, checked: Boolean) {
        if (index !in 0..7) return
        val current = packet ?: return
        applyLambdaChannel(current, index, checked)
        val currentValues = values ?: return
        values = currentValues.copy(channels = currentValues.channels.toMutableList().also { it[index] = checked })
        sendPacket(current)
    }

    fun updateSensorType(index: Int, sensorTypeCount: Int) {
        if (index !in 0 until sensorTypeCount) return
        val current = packet ?: return
        current.senstype = index
        values = values?.copy(sensorType = index)
        sendPacket(current)
    }
}
