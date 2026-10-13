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
import org.secu3.android.models.packets.out.params.StarterParamPacket

/** Holds Starter page state and coordinates edits with packet sends. */
class StarterStateHolder(
    private val sendPacket: (StarterParamPacket) -> Unit,
    private val onPacketLoaded: () -> Unit = {},
) {
    private var packet: StarterParamPacket? = null
    var state: StarterUiState by mutableStateOf(StarterUiState.Loading)
        private set

    fun reset() {
        packet = null
        state = StarterUiState.Loading
    }

    fun onPacketReceived(received: StarterParamPacket) {
        packet = received
        state = StarterUiState.Data(StarterValues.from(received))
        onPacketLoaded()
    }

    /** Applies the accepted value returned by an edit dialog, then sends the updated packet. */
    fun updateValue(number: StarterNumber, value: Float) {
        val currentPacket = packet ?: return
        applyStarterValue(currentPacket, number, value)
        val currentState = state as? StarterUiState.Data ?: return
        state = StarterUiState.Data(currentState.values.withValue(number, value))
        sendPacket(currentPacket)
    }

    fun updateFlag(flag: StarterFlag, checked: Boolean) {
        val currentPacket = packet ?: return
        when (flag) {
            StarterFlag.ALLOW_FLOOD_CLEAR_START -> currentPacket.allowStartOnClearFlood = checked
            StarterFlag.LIMIT_CRANKING_PW -> currentPacket.limitMaxInjPwOnCranking = checked
        }
        val currentState = state as? StarterUiState.Data ?: return
        state = StarterUiState.Data(currentState.values.withFlag(flag, checked))
        sendPacket(currentPacket)
    }
}
