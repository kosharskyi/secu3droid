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
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import org.secu3.android.models.packets.out.params.TemperatureParamPacket

/** Owns Temperature page packet state and coordinates accepted edits with packet sends. */
class TemperatureStateHolder(
    private val sendPacket: (TemperatureParamPacket) -> Unit,
    private val onPacketMapping: () -> Unit = {},
    private val onPacketMapped: () -> Unit = {},
) {
    private var packet: TemperatureParamPacket? = null
    var state: TemperatureUiState by mutableStateOf(TemperatureUiState.Loading)
        private set

    fun observePackets(owner: LifecycleOwner, packets: LiveData<TemperatureParamPacket>) {
        packets.observe(owner, ::onPacketReceived)
    }

    fun onPacketReceived(received: TemperatureParamPacket) {
        onPacketMapping()
        packet = received
        state = TemperatureUiState.Data(TemperatureValues.from(received))
        onPacketMapped()
    }

    fun updateValue(number: TemperatureNumber, value: Float) {
        val currentPacket = packet ?: return
        val currentState = state as? TemperatureUiState.Data ?: return
        applyTemperatureValue(currentPacket, number, value)
        state = TemperatureUiState.Data(currentState.values.withValue(number, value))
        sendPacket(currentPacket)
    }

    fun updateFlag(flag: TemperatureFlag, checked: Boolean) {
        val currentPacket = packet ?: return
        val currentState = state as? TemperatureUiState.Data ?: return
        applyTemperatureFlag(currentPacket, flag, checked)
        state = TemperatureUiState.Data(currentState.values.withFlag(flag, checked))
        sendPacket(currentPacket)
    }
}
