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
import org.secu3.android.models.packets.out.params.IdlingParamPacket

/** Owns Idling page packet state and coordinates accepted edits with packet sends. */
class IdlingStateHolder(
    private val sendPacket: (IdlingParamPacket) -> Unit,
    private val onPacketMapping: () -> Unit = {},
    private val onPacketMapped: () -> Unit = {},
) {
    private var packet: IdlingParamPacket? = null
    var state: IdlingUiState by mutableStateOf(IdlingUiState.Loading)
        private set

    fun reset() {
        packet = null
        state = IdlingUiState.Loading
    }

    fun onPacketReceived(received: IdlingParamPacket) {
        onPacketMapping()
        packet = received
        state = IdlingUiState.Data(IdlingValues.from(received))
        onPacketMapped()
    }

    /** Applies an accepted dialog value, updates Compose state, and sends the packet. */
    fun updateValue(number: IdlingNumber, value: Float) {
        val currentPacket = packet ?: return
        val currentState = state as? IdlingUiState.Data ?: return
        applyIdlingValue(currentPacket, number, value)
        state = IdlingUiState.Data(currentState.values.withValue(number, value))
        sendPacket(currentPacket)
    }

    fun updateFlag(flag: IdlingFlag, checked: Boolean) {
        val currentPacket = packet ?: return
        val currentState = state as? IdlingUiState.Data ?: return
        applyIdlingFlag(currentPacket, flag, checked)
        state = IdlingUiState.Data(currentState.values.withFlag(flag, checked))
        sendPacket(currentPacket)
    }
}
