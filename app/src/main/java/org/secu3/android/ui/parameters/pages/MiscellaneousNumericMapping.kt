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

import org.secu3.android.models.packets.out.params.MiscellaneousParamPacket
import org.secu3.android.models.packets.base.protocolRoundToInt
import kotlin.math.abs
import kotlin.math.roundToInt

/** Applies a validated numeric edit to the existing packet, without sending or replacing it. */
internal object MiscellaneousNumericMapping {
    fun applyInt(packet: MiscellaneousParamPacket, field: MiscField, value: Int) {
        when (field) {
            MiscField.PERIOD -> {
                require(value in 0..500)
                packet.uartPeriodTms = value
            }
            MiscField.CUTOFF -> {
                require(value in 1000..30000)
                packet.ignCutoffThrd = value
            }
            MiscField.START -> {
                require(value in -36..72)
                packet.hopStartAng = value.toFloat()
            }
            MiscField.DURATION -> {
                require(value in 6..180)
                packet.hopDuratAng = value.toFloat()
            }
            MiscField.BEGIN -> applyEvapPair(packet, value, packet.evapAfEnd)
            MiscField.END -> applyEvapPair(packet, packet.evapAfbegin, value)
            MiscField.PWM0 -> {
                require(value in 10..5000)
                packet.pwmFrq0 = value
            }
            MiscField.PWM1 -> {
                require(value in 10..5000)
                packet.pwmFrq1 = value
            }
            MiscField.VSS -> {
                require(value in 100..60000)
                packet.vssPeriodDist = value.toFloat()
            }
            MiscField.TIMEOUT -> error("Timeout is a float parameter")
        }
    }

    fun applyFloat(packet: MiscellaneousParamPacket, field: MiscField, value: Float) {
        require(field == MiscField.TIMEOUT) { "Only timeout is a float parameter" }
        require(value.isFinite() && value in 0.1f..25.5f)
        packet.fpTimeoutStrt = value
    }

    private fun applyEvapPair(packet: MiscellaneousParamPacket, begin: Int, end: Int) {
        require(begin in EVAP_MIN..EVAP_MAX && end in EVAP_MIN..EVAP_MAX)
        require(end - begin >= MiscellaneousParamPacket.MIN_EVAP_AIRFLOW_DELTA)
        val wireBegin = begin.div(32f).protocolRoundToInt() * 32
        val maxDelta = EVAP_MAX - maxOf(begin, wireBegin)
        require(maxDelta >= MiscellaneousParamPacket.MIN_EVAP_AIRFLOW_DELTA)
        val delta = (end - maxOf(begin, wireBegin)).coerceIn(MiscellaneousParamPacket.MIN_EVAP_AIRFLOW_DELTA, maxDelta)
        val targetSlope = 32f / delta
        require(targetSlope.isFinite())
        val requestedRaw = (targetSlope * 32f * 1048576f).roundToInt()
        require(requestedRaw in 1..0xFFFF)
        val representable = (-8..8).mapNotNull { offset ->
            val raw = requestedRaw + offset
            if (raw !in 1..0xFFFF) return@mapNotNull null
            val decodedSlope = raw.toFloat().div(1048576f).div(32f)
            val decodedModelEnd = (32f / decodedSlope + begin).roundToInt()
            val decodedWireEnd = (32f / decodedSlope + wireBegin).roundToInt()
            val decodedModelDelta = decodedModelEnd - begin
            val decodedWireDelta = decodedWireEnd - wireBegin
            if (decodedModelEnd in EVAP_MIN..EVAP_MAX &&
                decodedWireEnd in EVAP_MIN..EVAP_MAX &&
                decodedModelDelta >= MiscellaneousParamPacket.MIN_EVAP_AIRFLOW_DELTA &&
                decodedWireDelta >= MiscellaneousParamPacket.MIN_EVAP_AIRFLOW_DELTA
            ) {
                raw to decodedSlope
            } else {
                null
            }
        }
        val (raw, decodedSlope) = representable.minByOrNull { (candidateRaw, _) -> abs(candidateRaw - requestedRaw) }
            ?: throw IllegalArgumentException("EVAP slope has no safe wire representation")
        require((decodedSlope * 32f * 1048576f).roundToInt() == raw)
        packet.evapAfbegin = begin
        packet.evapAfslope = decodedSlope
    }

    private const val EVAP_MIN = 100
    private const val EVAP_MAX = 2_000_000
}
