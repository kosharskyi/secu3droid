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
import org.secu3.android.models.packets.out.params.MiscellaneousParamPacket
import kotlin.math.roundToInt

/** Owns Miscellaneous packet state, validation, and accepted packet sends. */
internal class MiscellaneousStateHolder(
    private val sendPacket: (MiscellaneousParamPacket) -> Unit,
) {
    private var packet: MiscellaneousParamPacket? = null
    val currentPacket: MiscellaneousParamPacket? get() = packet
    var state: MiscState? by mutableStateOf(null)
        private set
    var hasValidationError: Boolean by mutableStateOf(false)
        private set

    fun reset() {
        packet = null
        state = null
        hasValidationError = false
    }

    fun onPacketReceived(received: MiscellaneousParamPacket) {
        packet = received
        state = received.toState()
        hasValidationError = false
    }

    fun selectBaud(code: Int) = update { it.uartDivisor = code }

    fun editInt(key: MiscField, value: Int) {
        val isEvapEdit = key == MiscField.BEGIN || key == MiscField.END
        update(skipExistingPacketValidation = isEvapEdit) {
            MiscellaneousNumericMapping.applyInt(it, key, value)
        }
    }

    fun editFloat(value: Float) = update {
        MiscellaneousNumericMapping.applyFloat(it, MiscField.TIMEOUT, value)
    }

    fun toggle(key: MiscToggle, value: Boolean) = update { packet ->
        when (key) {
            MiscToggle.IGN -> packet.ignCutoff = if (value) 1 else 0
            MiscToggle.PUMP -> packet.offPumpOnGas = value
            MiscToggle.GAS -> packet.offInjOnGas = value
            MiscToggle.PETROL -> packet.offInjOnPetrol = value
        }
    }

    private fun update(skipExistingPacketValidation: Boolean = false, change: (MiscellaneousParamPacket) -> Unit) {
        val current = packet ?: return
        if (!skipExistingPacketValidation && !isPackable(current)) {
            hasValidationError = true
            return
        }

        val before = current.copy()
        try {
            change(current)
            current.pack()
        } catch (_: IllegalArgumentException) {
            restore(current, before)
            hasValidationError = true
            return
        }

        hasValidationError = false
        state = current.toState()
        sendPacket(current)
    }

    private fun isPackable(packet: MiscellaneousParamPacket): Boolean = try {
        packet.pack()
        true
    } catch (_: IllegalArgumentException) {
        false
    }

    private fun restore(target: MiscellaneousParamPacket, snapshot: MiscellaneousParamPacket) {
        target.uartDivisor = snapshot.uartDivisor
        target.uartPeriodTms = snapshot.uartPeriodTms
        target.ignCutoff = snapshot.ignCutoff
        target.ignCutoffThrd = snapshot.ignCutoffThrd
        target.hopStartAng = snapshot.hopStartAng
        target.hopDuratAng = snapshot.hopDuratAng
        target.flpmpFlags = snapshot.flpmpFlags
        target.evapAfbegin = snapshot.evapAfbegin
        target.evapAfslope = snapshot.evapAfslope
        target.fpTimeoutStrt = snapshot.fpTimeoutStrt
        target.pwmFrq0 = snapshot.pwmFrq0
        target.pwmFrq1 = snapshot.pwmFrq1
        target.vssPeriodDist = snapshot.vssPeriodDist
    }
}

private fun MiscellaneousParamPacket.toState() = MiscState(
    uartDivisor, uartPeriodTms, ignCutoff > 0, ignCutoffThrd, hopStartAng, hopDuratAng,
    offPumpOnGas, offInjOnGas, offInjOnPetrol, evapAfbegin, evapAfEnd, fpTimeoutStrt,
    pwmFrq0, pwmFrq1, vssPeriodDist.roundToInt(),
)
