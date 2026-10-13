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
import org.secu3.android.models.packets.out.params.UniOutParamPacket

/** Owns the received packet and coordinates Universal Outputs edits and sends. */
internal class UniversalOutputsStateHolder(
    private val sendPacket: (UniOutParamPacket) -> Unit,
    private val setSendAllowed: (Boolean) -> Unit = {},
) {
    private var packet: UniOutParamPacket? = null
    var values: UniversalOutputsValues? by mutableStateOf(null)
        private set

    fun reset() {
        packet = null
        values = null
    }

    fun onPacketReceived(received: UniOutParamPacket) {
        setSendAllowed(false)
        packet = received
        values = UniversalOutputsValues.from(received)
        setSendAllowed(true)
    }

    fun updateOutput(index: Int, updated: UniversalOutputValues) {
        val currentPacket = packet ?: return
        if (index !in 1..6) return
        val before = values?.outputs?.getOrNull(index - 1) ?: return
        applyUniversalOutput(currentPacket, index, before, updated)
        values = UniversalOutputsValues.from(currentPacket)
        sendPacket(currentPacket)
    }

    fun updatePairFunction(value: Int) {
        val currentPacket = packet ?: return
        currentPacket.logicFunction_1_2 = value
        values = UniversalOutputsValues.from(currentPacket)
        sendPacket(currentPacket)
    }
}
