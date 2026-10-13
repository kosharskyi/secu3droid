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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.secu3.android.models.packets.out.params.SecurityParamPacket

/** Owns Security tab packet state and sends immutable snapshots of user changes. */
internal class SecurityStateHolder(
    private val sendPacket: (org.secu3.android.models.packets.base.OutputPacket) -> Unit,
    private val invalidCredentialsMessage: String,
) {
    private var packet: SecurityParamPacket? = null

    var loaded by mutableStateOf(false)
        private set
    var name by mutableStateOf("")
        private set
    var password by mutableStateOf("")
        private set
    var key0 by mutableStateOf("")
        private set
    var key1 by mutableStateOf("")
        private set
    var error by mutableStateOf("")
        private set
    var useBt by mutableStateOf(false)
        private set
    var useImm by mutableStateOf(false)
        private set
    var type by mutableIntStateOf(0)
        private set
    var useReserveParams by mutableStateOf(false)
        private set
    var checkFirmwareCrc by mutableStateOf(false)
        private set

    fun reset() {
        packet = null
        loaded = false
        name = ""
        password = ""
        key0 = ""
        key1 = ""
        error = ""
        useBt = false
        useImm = false
        type = 0
        useReserveParams = false
        checkFirmwareCrc = false
    }

    fun load(value: SecurityParamPacket) {
        packet = value
        useBt = value.useBt
        useImm = value.useImmobilizer
        type = value.btType
        useReserveParams = value.useReserveParams
        checkFirmwareCrc = value.checkFwCrc
        key0 = formatKey(value.iButton0)
        key1 = formatKey(value.iButton1)
        // Credentials are intentionally write-only in the ECU protocol.
        name = ""
        password = ""
        error = ""
        loaded = true
    }

    private fun commit(change: SecurityParamPacket.() -> Unit) {
        val current = packet ?: return
        try {
            change(current)
            error = ""
            sendPacket(current.packedSnapshot())
        } catch (e: IllegalArgumentException) {
            error = e.message.orEmpty()
        }
    }

    fun changeType(index: Int) {
        type = index
        commit { btType = index }
    }

    fun changeBt(value: Boolean) {
        useBt = value
        commit { useBt = value; btFlags = btFlags or (1 shl 1) }
    }

    fun changeImm(value: Boolean) {
        useImm = value
        commit { useImmobilizer = value }
    }

    fun changeName(value: String) {
        if (value.length <= 16) name = value
    }

    fun changePass(value: String) {
        if (value.length <= 8 && value.all { it in '0'..'9' }) password = value
    }

    fun changeKey0(value: String) {
        if (value.length <= 12) {
            key0 = value.uppercase()
            updateKey(key0, first = true)
        }
    }

    fun changeKey1(value: String) {
        if (value.length <= 12) {
            key1 = value.uppercase()
            updateKey(key1, first = false)
        }
    }

    private fun updateKey(value: String, first: Boolean) {
        val bytes = parseIButtonKey(value) ?: return
        commit { if (first) iButton0 = bytes else iButton1 = bytes }
    }

    fun applyCredentials() {
        val current = packet ?: return
        try {
            val snapshot = current.credentialsSnapshot(name, password)
            error = ""
            sendPacket(snapshot)
            name = ""
            password = ""
        } catch (_: IllegalArgumentException) {
            error = invalidCredentialsMessage
        }
    }

    private fun formatKey(bytes: List<Int>) =
        if (bytes.size == 6) bytes.asReversed().joinToString("") { "%02X".format(it) } else ""
}

internal fun parseIButtonKey(value: String): List<Int>? {
    if (value.length != 12 || !value.all { it.digitToIntOrNull(16) != null }) return null
    return value.chunked(2).reversed().map { it.toInt(16) }
}
