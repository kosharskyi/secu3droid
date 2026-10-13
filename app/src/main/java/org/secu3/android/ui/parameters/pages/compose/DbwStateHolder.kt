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
import org.secu3.android.models.packets.out.params.DbwParamPacket

/** Owns the received DBW packet and editable values for one view lifecycle. */
class DbwStateHolder(
    private val sendPacket: (DbwParamPacket) -> Unit,
    private val setSendAllowed: (Boolean) -> Unit = {},
) {
    private var packet: DbwParamPacket? = null
    var uiState: DbwUiState by mutableStateOf(DbwUiState.Loading)
        private set

    val isSendAllowed: Boolean get() = packet != null

    fun reset() {
        packet = null
        uiState = DbwUiState.Loading
        setSendAllowed(false)
    }

    /** Receiving initializes the screen and send gate without echoing the packet. */
    fun receive(received: DbwParamPacket) {
        setSendAllowed(false)
        packet = received
        uiState = DbwUiState.Data(DbwValues.from(received))
        setSendAllowed(true)
    }

    fun edit(number: DbwNumber, value: Float) {
        val currentPacket = packet ?: return
        val currentState = uiState as? DbwUiState.Data ?: return
        applyDbwValue(currentPacket, number, value)
        uiState = DbwUiState.Data(currentState.values.withValue(number, value))
        sendPacket(currentPacket)
    }
}
