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
import org.secu3.android.models.packets.base.OutputPacket
import org.secu3.android.models.packets.out.params.GasDoseParamPacket

/** Owns Gas Dose packet state and snapshots all asynchronous sends. */
class GasDoseStateHolder(
    private val onValuesChanged: (GasDoseValues?) -> Unit,
    private val onSend: (OutputPacket) -> Unit,
) {
    private var packet: GasDoseParamPacket? = null
    private var manualDelta = 0

    var values: GasDoseValues? by mutableStateOf(null)
        private set
    @set:JvmName("setFirmwareAvailableState")
    var isFirmwareAvailable: Boolean by mutableStateOf(true)
        private set

    fun setFirmwareAvailable(available: Boolean) {
        isFirmwareAvailable = available
        if (!available) manualDelta = 0
    }

    /** Drops view-scoped packet data and unfinished movement before a fresh receive. */
    fun reset() {
        packet = null
        manualDelta = 0
        values = null
        onValuesChanged(null)
    }

    fun receive(received: GasDoseParamPacket) {
        manualDelta = 0
        received.manualPositionD = 0
        packet = received
        publishValues()
    }

    fun edit(number: GasDoseNumber, value: Float) {
        if (!isFirmwareAvailable) return
        val current = packet ?: return
        applyGasDoseNumber(current, number, value)
        sendOrdinary(current)
    }

    fun selectFrequency(index: Int) {
        if (!isFirmwareAvailable || index !in 0..3) return
        val current = packet ?: return
        current.freq = index
        sendOrdinary(current)
    }

    fun setMaximumFrequency(enabled: Boolean) {
        if (!isFirmwareAvailable) return
        val current = packet ?: return
        current.maxFreqInit = if (enabled) 1 else 0
        sendOrdinary(current)
    }

    fun toggleTest() {
        if (!isFirmwareAvailable) return
        val current = packet ?: return
        current.testing = !current.testing
        sendOrdinary(current)
    }

    fun manualStep(direction: Int) {
        if (!isFirmwareAvailable || packet == null) return
        manualDelta = (manualDelta + direction.coerceIn(-1, 1)).coerceIn(-127, 127)
    }

    fun flushManual() {
        if (!isFirmwareAvailable) { manualDelta = 0; return }
        val current = packet ?: return
        val delta = manualDelta
        manualDelta = 0
        if (delta != 0) onSend(current.snapshotWithManualPosition(delta))
    }

    fun finishManual() = flushManual()

    fun cancelManual() {
        manualDelta = 0
        packet?.manualPositionD = 0
    }

    private fun publishValues() {
        val current = packet?.let(GasDoseValues::from)
        values = current
        onValuesChanged(current)
    }

    private fun sendOrdinary(current: GasDoseParamPacket) {
        publishValues()
        onSend(current.snapshotWithManualPosition(0))
    }
}
