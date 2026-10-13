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
package org.secu3.android.ui.parameters.pages

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.secu3.android.models.packets.out.params.AccelerationParamPacket
import org.secu3.android.ui.parameters.pages.compose.AccelerationNumber
import org.secu3.android.ui.parameters.pages.compose.AccelerationValues
import org.secu3.android.ui.parameters.pages.compose.applyAccelerationValue

/** Owns the received Acceleration packet and coordinates edits and sends. */
internal class AccelerationStateHolder(
    private val sendPacket: (AccelerationParamPacket) -> Unit,
    private val setSendAllowed: (Boolean) -> Unit = {},
) {
    private var packet: AccelerationParamPacket? = null
    var values: AccelerationValues? by mutableStateOf(null)
        private set

    fun reset() {
        packet = null
        values = null
    }

    fun onPacketReceived(received: AccelerationParamPacket) {
        setSendAllowed(false)
        packet = received
        values = AccelerationValues.from(received)
        setSendAllowed(true)
    }

    fun updateNumber(number: AccelerationNumber, value: Float) {
        val currentPacket = packet ?: return
        applyAccelerationValue(currentPacket, number, value)
        values = values?.withValue(number, value)
        sendPacket(currentPacket)
    }

    fun selectAeType(index: Int) {
        val currentPacket = packet ?: return
        currentPacket.injAeType = index
        values = values?.copy(aeType = index)
        sendPacket(currentPacket)
    }

    fun selectWallwetModel(index: Int) {
        val currentPacket = packet ?: return
        currentPacket.wallwetModel = index
        values = values?.copy(wallwetModel = index)
        sendPacket(currentPacket)
    }

    fun setBalance(value: Float) {
        val currentPacket = packet ?: return
        currentPacket.injAeBallance = value
        values = values?.copy(balance = value)
        sendPacket(currentPacket)
    }
}
