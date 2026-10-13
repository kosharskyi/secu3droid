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
import org.secu3.android.models.packets.out.params.CkpsParamPacket

/** Owns CKPS packet state and coordinates accepted edits with packet sends. */
class CkpsStateHolder(
    private val sendPacket: (CkpsParamPacket) -> Unit,
    private val onPacketMapping: () -> Unit = {},
    private val onPacketMapped: () -> Unit = {},
) {
    private var packet: CkpsParamPacket? = null
    var state: CkpsUiState by mutableStateOf(CkpsUiState.Loading)
        private set

    fun onPacketReceived(received: CkpsParamPacket) {
        onPacketMapping()
        packet = received
        state = CkpsUiState.Data(CkpsValues.from(received))
        onPacketMapped()
    }

    fun updateNumber(number: CkpsNumber, value: Float) {
        val current = packet ?: return
        if (state !is CkpsUiState.Data) return
        applyCkpsNumber(current, number, value)
        updateAndSend(current)
    }

    fun updateFlag(flag: CkpsFlag, value: Boolean) {
        val current = packet ?: return
        if (state !is CkpsUiState.Data) return
        applyCkpsFlag(current, flag, value)
        updateAndSend(current)
    }

    fun updateChoice(teeth: Boolean, value: Int) {
        val current = packet ?: return
        if (state !is CkpsUiState.Data || !applyCkpsChoice(current, teeth, value)) return
        updateAndSend(current)
    }

    private fun updateAndSend(current: CkpsParamPacket) {
        state = CkpsUiState.Data(CkpsValues.from(current))
        sendPacket(current)
    }
}
