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
import org.secu3.android.models.FnName
import org.secu3.android.models.packets.input.FnNameDatPacket
import org.secu3.android.models.packets.out.params.FunSetParamPacket

/** Holds Functions page data and coordinates edits with packet sends. */
class FunctionsStateHolder(
    private val sendPacket: (FunSetParamPacket) -> Unit,
) {
    private var packet: FunSetParamPacket? = null
    private var namesLoaded = false
    var names: List<String> = emptyList()
        private set
    var state: FunctionsUiState by mutableStateOf(FunctionsUiState.Loading)
        private set

    fun reset() {
        packet = null
        namesLoaded = false
        names = emptyList()
        state = FunctionsUiState.Loading
    }

    fun onNamesReceived(received: FnNameDatPacket) {
        names = received.fnNameList.map(FnName::name)
        namesLoaded = true
    }

    fun onPacketReceived(
        received: FunSetParamPacket,
        loads: List<String>,
        bars: List<String>,
        ve2: List<String>,
    ) {
        if (!namesLoaded) return
        packet = received
        state = FunctionsUiState.Data(
            values = FunctionsValues.from(received),
            choices = FunctionsChoices.from(received, names, loads, bars, ve2),
        )
    }

    fun updateValue(number: FunctionsNumber, value: Float) {
        val currentPacket = packet ?: return
        val currentState = state as? FunctionsUiState.Data ?: return
        applyFunctionsValue(currentPacket, number, value)
        state = currentState.copy(values = currentState.values.withValue(number, value))
        sendPacket(currentPacket)
    }

    fun updateFlag(checked: Boolean) {
        val currentPacket = packet ?: return
        val currentState = state as? FunctionsUiState.Data ?: return
        applyFunctionsFlag(currentPacket, checked)
        state = currentState.copy(values = currentState.values.copy(useLoadGrid = checked))
        sendPacket(currentPacket)
    }

    fun updateChoice(choice: FunctionsChoices.Selector, index: Int) {
        val currentPacket = packet ?: return
        val currentState = state as? FunctionsUiState.Data ?: return
        if (applyFunctionsChoice(currentPacket, choice, index, names)) {
            state = currentState.copy(choices = currentState.choices.withSelection(choice, index))
            sendPacket(currentPacket)
        }
    }
}
