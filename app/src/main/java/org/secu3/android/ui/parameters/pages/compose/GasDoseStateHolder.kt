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

import org.secu3.android.models.packets.base.OutputPacket
import org.secu3.android.models.packets.out.params.GasDoseParamPacket

/** Owns Gas Dose packet state and snapshots all asynchronous sends. */
class GasDoseStateHolder(
    private val onValuesChanged: (GasDoseValues?) -> Unit,
    private val onSend: (OutputPacket) -> Unit,
) {
    private var packet: GasDoseParamPacket? = null
    private var firmwareAvailable = true
    private var manualDelta = 0

    val values: GasDoseValues? get() = packet?.let(GasDoseValues::from)
    val isFirmwareAvailable: Boolean get() = firmwareAvailable

    fun setFirmwareAvailable(available: Boolean) {
        firmwareAvailable = available
        if (!available) manualDelta = 0
    }

    /** Drops view-scoped packet data and unfinished movement before a fresh receive. */
    fun reset() {
        packet = null
        manualDelta = 0
        onValuesChanged(null)
    }

    fun receive(received: GasDoseParamPacket) {
        manualDelta = 0
        received.manualPositionD = 0
        packet = received
        onValuesChanged(GasDoseValues.from(received))
    }

    fun edit(number: GasDoseNumber, value: Float) {
        if (!firmwareAvailable) return
        val current = packet ?: return
        applyGasDoseNumber(current, number, value)
        sendOrdinary(current)
    }

    fun selectFrequency(index: Int) {
        if (!firmwareAvailable || index !in 0..3) return
        val current = packet ?: return
        current.freq = index
        sendOrdinary(current)
    }

    fun setMaximumFrequency(enabled: Boolean) {
        if (!firmwareAvailable) return
        val current = packet ?: return
        current.maxFreqInit = if (enabled) 1 else 0
        sendOrdinary(current)
    }

    fun toggleTest() {
        if (!firmwareAvailable) return
        val current = packet ?: return
        current.testing = !current.testing
        sendOrdinary(current)
    }

    fun manualStep(direction: Int) {
        if (!firmwareAvailable || packet == null) return
        manualDelta = (manualDelta + direction.coerceIn(-1, 1)).coerceIn(-127, 127)
    }

    fun flushManual() {
        if (!firmwareAvailable) { manualDelta = 0; return }
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

    private fun sendOrdinary(current: GasDoseParamPacket) {
        onValuesChanged(GasDoseValues.from(current))
        onSend(current.snapshotWithManualPosition(0))
    }
}
