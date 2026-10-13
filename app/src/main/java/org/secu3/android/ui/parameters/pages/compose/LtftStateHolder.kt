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
import org.secu3.android.models.packets.out.params.LtftParamPacket

/** Owns the editable packet and presentation state for one LTFT view lifecycle. */
class LtftStateHolder(private val sendPacket: (LtftParamPacket) -> Unit) {
    private var packet: LtftParamPacket? = null
    var uiState: LtftUiState by mutableStateOf(LtftUiState.Loading)
        private set

    val isSendAllowed: Boolean get() = packet != null

    fun reset() {
        packet = null
        uiState = LtftUiState.Loading
    }

    /** A received packet initializes state only; receiving it never sends it back. */
    fun receive(received: LtftParamPacket) {
        packet = received
        uiState = LtftUiState.Data(LtftValues.from(received))
    }

    fun edit(number: LtftNumber, value: Float) {
        val currentPacket = packet ?: return
        applyLtftValue(currentPacket, number, value)
        val currentState = uiState as? LtftUiState.Data ?: return
        uiState = LtftUiState.Data(currentState.values.withValue(number, value))
        sendPacket(currentPacket)
    }

    fun selectMode(index: Int) {
        if (index !in 0..3) return
        val currentPacket = packet ?: return
        currentPacket.mode = index
        val currentState = uiState as? LtftUiState.Data ?: return
        uiState = LtftUiState.Data(currentState.values.copy(mode = index))
        sendPacket(currentPacket)
    }
}
