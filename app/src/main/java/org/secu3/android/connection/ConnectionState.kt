/*
 *    SecuDroid  - An open source, free manager for SECU-3 engine control unit
 *    Copyright (C) 2025 Vitalii O. Kosharskyi. Ukraine, Kyiv
 *
 *    SECU-3  - An open source, free engine control unit
 *    Copyright (C) 2007-2025 Alexey A. Shabelnikov. Ukraine, Kyiv
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

package org.secu3.android.connection

sealed class ConnectionState()

data object Connected : ConnectionState()
data object Connecting : ConnectionState()
/** Bluetooth has lost an established socket and is retrying it. */
data object Reconnecting : ConnectionState()
/** The initial USB handshake is waiting for a valid ECU response. */
data object UsbAwaitingEcu : ConnectionState()
/** An established USB connection is recovering after a transport or ECU loss. */
data object UsbReconnecting : ConnectionState()
/** An established USB connection is waiting for a valid ECU response during recovery. */
data object UsbReconnectingAwaitingEcu : ConnectionState()
/** The ECU session ended; the transport may continue retrying afterwards. */
data object Disconnected : ConnectionState()
data class ConnectionAttempt(val currentAttempt: Int) : ConnectionState()

data object ConnectionTimeout : ConnectionState()

data class ConnectionError(val e: Exception) : ConnectionState()
data class ConnectionFailed(val e: Exception) : ConnectionState()
data class ConnectionLost(val e: Exception) : ConnectionState()

/** A transport status plus the lifecycle outcome that accompanies it. */
data class ConnectionEvent(
    val state: ConnectionState,
    val firmware: org.secu3.android.models.packets.input.FirmwareInfoPacket? = null,
    val end: End? = null,
) {
    enum class End { RETRYING, STOPPED }
}
