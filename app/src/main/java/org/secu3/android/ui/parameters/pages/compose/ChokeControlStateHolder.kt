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
import org.secu3.android.models.packets.out.params.ChokeControlParPacket

data class ChokeValues(
    val numbers: Map<ChokeNumber, Float>,
    val flags: List<Boolean>,
    val testing: Boolean,
    val frequency: Int,
) {
    fun value(number: ChokeNumber) = numbers.getValue(number)

    companion object {
        fun from(p: ChokeControlParPacket) = ChokeValues(
            mapOf(
                ChokeNumber.STEPS to p.smSteps.toFloat(),
                ChokeNumber.REGULATOR_FACTOR to p.rpmIf,
                ChokeNumber.COLD_DURATION to p.corrTime0,
                ChokeNumber.HOT_DURATION to p.corrTime1,
                ChokeNumber.CRANK_TO_RUN to p.injCrankToRunTime,
            ),
            listOf(
                p.useClosedLoopRmpRegulator,
                p.dontUseRpmRegOnGas,
                p.useThrottlePosInChokeInit,
                p.maxSTEPfreqAtInit,
            ),
            p.testing,
            p.smFreq,
        )
    }
}

fun applyChokeNumber(p: ChokeControlParPacket, n: ChokeNumber, value: Float) {
    when (n) {
        ChokeNumber.STEPS -> p.smSteps = value.toInt().coerceIn(50, 2000)
        ChokeNumber.REGULATOR_FACTOR -> p.rpmIf = value.coerceIn(.01f, 1f)
        ChokeNumber.COLD_DURATION -> p.corrTime0 = value.coerceIn(.1f, 10f)
        ChokeNumber.HOT_DURATION -> p.corrTime1 = value.coerceIn(.1f, 10f)
        ChokeNumber.CRANK_TO_RUN -> p.injCrankToRunTime = value.coerceIn(.1f, 99f)
        else -> Unit
    }
}

/** Owns Choke Control's editable packet and coordinates immutable outgoing snapshots. */
class ChokeControlStateHolder(
    private val onValuesChanged: (ChokeValues) -> Unit,
    private val onSend: (OutputPacket) -> Unit,
) {
    private var packet: ChokeControlParPacket? = null
    private var manualDelta = 0

    val values: ChokeValues? get() = packet?.let(ChokeValues::from)

    /** Accepting a device packet resets the outgoing-only command and any unfinished movement. */
    fun receive(received: ChokeControlParPacket) {
        manualDelta = 0
        received.manualPositionD = 0
        packet = received
        publishValues()
    }

    fun edit(number: ChokeNumber, value: Float) {
        val current = packet ?: return
        if (number !in EDITABLE_NUMBERS) return
        applyChokeNumber(current, number, value)
        sendOrdinarySnapshot(current)
    }

    fun toggle(number: ChokeNumber, checked: Boolean) {
        val current = packet ?: return
        when (number) {
            ChokeNumber.CLOSED_LOOP -> current.useClosedLoopRmpRegulator = checked
            ChokeNumber.DISABLE_ON_GAS -> current.dontUseRpmRegOnGas = checked
            ChokeNumber.THROTTLE_INIT -> current.useThrottlePosInChokeInit = checked
            ChokeNumber.MAX_FREQUENCY_INIT -> current.maxSTEPfreqAtInit = checked
            else -> return
        }
        sendOrdinarySnapshot(current)
    }

    fun selectFrequency(index: Int) {
        if (index !in 0..3) return
        val current = packet ?: return
        current.smFreq = index
        sendOrdinarySnapshot(current)
    }

    fun manualStep(direction: Int) {
        if (packet == null) return
        manualDelta = (manualDelta + direction.coerceIn(-1, 1)).coerceIn(-127, 127)
    }

    /** Sends and clears the currently accumulated manual movement, if any. */
    fun flushManual() {
        val current = packet ?: return
        val delta = manualDelta
        manualDelta = 0
        if (delta != 0) onSend(current.snapshotWithManualPosition(delta))
    }

    /** Final gesture flush; safe after periodic flushes and repeated calls. */
    fun finishManual() = flushManual()

    fun cancelManual() {
        manualDelta = 0
        packet?.manualPositionD = 0
    }

    fun toggleTest() {
        val current = packet ?: return
        current.testing = !current.testing
        sendOrdinarySnapshot(current)
    }

    private fun sendOrdinarySnapshot(current: ChokeControlParPacket) {
        publishValues()
        onSend(current.snapshotWithManualPosition(0))
    }

    private fun publishValues() {
        packet?.let { onValuesChanged(ChokeValues.from(it)) }
    }

    private companion object {
        val EDITABLE_NUMBERS = setOf(
            ChokeNumber.STEPS,
            ChokeNumber.REGULATOR_FACTOR,
            ChokeNumber.COLD_DURATION,
            ChokeNumber.HOT_DURATION,
            ChokeNumber.CRANK_TO_RUN,
        )
    }
}
