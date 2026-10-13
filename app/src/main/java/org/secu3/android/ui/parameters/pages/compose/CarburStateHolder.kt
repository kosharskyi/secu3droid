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
import org.secu3.android.models.packets.out.params.CarburParamPacket

/** Owns Carbur page packet state and coordinates accepted edits with packet sends. */
class CarburStateHolder(
    private val sendPacket: (CarburParamPacket) -> Unit,
    private val onPacketMapping: () -> Unit = {},
    private val onPacketMapped: () -> Unit = {},
) {
    private var packet: CarburParamPacket? = null
    var state: CarburUiState by mutableStateOf(CarburUiState.Loading)
        private set

    fun reset() {
        packet = null
        state = CarburUiState.Loading
    }

    fun onPacketReceived(received: CarburParamPacket) {
        onPacketMapping()
        packet = received
        state = CarburUiState.Data(CarburValues.from(received))
        onPacketMapped()
    }

    fun updateValue(number: CarburNumber, value: Float) {
        val current = packet ?: return
        val currentState = state as? CarburUiState.Data ?: return
        applyCarburValue(current, number, value)
        state = CarburUiState.Data(currentState.values.withValue(number, value))
        sendPacket(current)
    }

    fun updateInvers(checked: Boolean) {
        val current = packet ?: return
        val currentState = state as? CarburUiState.Data ?: return
        current.carbInvers = if (checked) 1 else 0
        state = CarburUiState.Data(currentState.values.copy(invers = checked))
        sendPacket(current)
    }

    fun updateSelector(ignition: Boolean, index: Int) {
        val current = packet ?: return
        val currentState = state as? CarburUiState.Data ?: return
        if (!applyCarburSelector(current, ignition, index)) return
        state = CarburUiState.Data(currentState.values.copy(fuelCutUni = current.fuelcut_uni, ignitionCutUni = current.igncut_uni))
        sendPacket(current)
    }
}
