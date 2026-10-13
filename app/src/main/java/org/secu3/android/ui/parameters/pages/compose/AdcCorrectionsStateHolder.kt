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
import org.secu3.android.models.packets.out.params.AdcCorrectionsParamPacket

/** Owns ADC corrections packet state and coordinates accepted edits with packet sends. */
class AdcCorrectionsStateHolder(
    private val sendPacket: (AdcCorrectionsParamPacket) -> Unit,
    private val onPacketMapping: () -> Unit = {},
    private val onPacketMapped: () -> Unit = {},
) {
    private var packet: AdcCorrectionsParamPacket? = null
    var state: AdcCorrectionsUiState by mutableStateOf(AdcCorrectionsUiState.Loading)
        private set

    fun reset() {
        packet = null
        state = AdcCorrectionsUiState.Loading
    }

    fun onPacketReceived(received: AdcCorrectionsParamPacket) {
        onPacketMapping()
        packet = received
        state = AdcCorrectionsUiState.Data(AdcCorrectionsValues.from(received))
        onPacketMapped()
    }

    fun updateValue(number: AdcNumber, value: Float) {
        val current = packet ?: return
        val values = (state as? AdcCorrectionsUiState.Data)?.values ?: return
        current.setAdcValue(number, value)
        state = AdcCorrectionsUiState.Data(values.withValue(number, value))
        sendPacket(current)
    }

    fun updateCompMode(enabled: Boolean) {
        val current = packet ?: return
        val values = (state as? AdcCorrectionsUiState.Data)?.values ?: return
        current.adcCompMode = enabled
        state = AdcCorrectionsUiState.Data(values.withCompMode(enabled))
        sendPacket(current)
    }
}
