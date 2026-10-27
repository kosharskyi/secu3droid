/*
 *    SecuDroid  - An open source, free manager for SECU-3 engine control unit
 *    Copyright (C) 2026 Vitalii O. Kosharskyi. Ukraine, Kyiv
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 */
package org.secu3.android.domain.parameters

import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.secu3.android.connection.Secu3ConnectionManager
import org.secu3.android.models.packets.base.OutputPacket
import org.secu3.android.models.packets.base.protocolRoundToInt
import org.secu3.android.models.packets.input.FirmwareInfoPacket
import org.secu3.android.models.packets.out.params.MiscellaneousParamPacket
import kotlin.math.abs
import kotlin.math.roundToInt
import javax.inject.Inject

data class MiscellaneousValues(
    val baudDivisor: Int,
    val period: Int,
    val ignitionCutoff: Boolean,
    val cutoff: Int,
    val start: Float,
    val duration: Float,
    val pumpOffOnGas: Boolean,
    val injectorsOffOnGas: Boolean,
    val injectorsOffOnPetrol: Boolean,
    val begin: Int,
    val end: Int,
    val beginBounds: Pair<Int, Int>?,
    val endBounds: Pair<Int, Int>?,
    val timeout: Float,
    val pwm0: Int,
    val pwm1: Int,
    val vss: Float,
    val hasValidationError: Boolean,
)

/** Owns Miscellaneous packet edits, firmware admission, validation, and immutable commands. */
@ViewModelScoped
class MiscellaneousControl internal constructor(
    private val firmwareInfo: StateFlow<FirmwareInfoPacket?>,
    private val sendPacket: (OutputPacket) -> Boolean,
) {
    @Inject
    constructor(connectionManager: Secu3ConnectionManager, parameterSession: ParameterSession) : this(
        connectionManager.firmwareInfo,
        parameterSession::sendPacket,
    )

    private val mutableValues = MutableStateFlow<MiscellaneousValues?>(null)
    val values: StateFlow<MiscellaneousValues?> = mutableValues.asStateFlow()
    private var packet: MiscellaneousParamPacket? = null

    fun reset() {
        packet = null
        mutableValues.value = null
    }

    fun receive(received: MiscellaneousParamPacket) {
        packet = received
        publishValues()
    }

    fun selectBaudDivisor(code: Int): Boolean {
        if (code !in BAUD_RATES) return false
        return edit { it.uartDivisor = code }
    }

    fun editPeriod(value: Int) = edit { packet ->
        require(value in PERIOD_MIN..PERIOD_MAX)
        packet.uartPeriodTms = value
    }

    fun editCutoff(value: Int): Boolean {
        if (packet?.ignCutoff?.let { it <= 0 } != false) return false
        return edit { packet ->
            require(value in CUTOFF_MIN..CUTOFF_MAX)
            packet.ignCutoffThrd = value
        }
    }

    fun editStart(value: Int) = edit(allowInvalidEvap = false, capability = { it.hallOutput }) { packet ->
        require(value in START_MIN..START_MAX)
        packet.hopStartAng = value.toFloat()
    }

    fun editDuration(value: Int) = edit(capability = { it.hallOutput }) { packet ->
        require(value in DURATION_MIN..DURATION_MAX)
        packet.hopDuratAng = value.toFloat()
    }

    fun editEvapBegin(value: Int) = edit(
        allowInvalidEvap = true,
        capability = { it.nonSecu3TFuelInjection },
    ) { packet -> applyEvapPair(packet, value, packet.evapAfEnd) }

    fun editEvapEnd(value: Int) = edit(
        allowInvalidEvap = true,
        capability = { it.nonSecu3TFuelInjection },
    ) { packet -> applyEvapPair(packet, packet.evapAfbegin, value) }

    fun editTimeout(value: Float) = edit { packet ->
        require(value.isFinite() && value in TIMEOUT_MIN..TIMEOUT_MAX)
        packet.fpTimeoutStrt = value
    }

    fun editPwm0(value: Int) = edit { packet ->
        require(value in PWM_MIN..PWM_MAX)
        packet.pwmFrq0 = value
    }

    fun editPwm1(value: Int) = edit { packet ->
        require(value in PWM_MIN..PWM_MAX)
        packet.pwmFrq1 = value
    }

    fun editVss(value: Int) = edit { packet ->
        require(value in VSS_MIN..VSS_MAX)
        packet.vssPeriodDist = value.toFloat()
    }

    fun setIgnitionCutoff(value: Boolean) = edit { it.ignCutoff = if (value) 1 else 0 }

    fun setPumpOffOnGas(value: Boolean) = edit { it.offPumpOnGas = value }

    fun setInjectorsOffOnGas(value: Boolean) = edit(capability = { it.fuelInjection }) { it.offInjOnGas = value }

    fun setInjectorsOffOnPetrol(value: Boolean) = edit(capability = { it.fuelInjection }) { it.offInjOnPetrol = value }

    private inline fun edit(
        allowInvalidEvap: Boolean = false,
        capability: (ParameterCapabilities) -> Boolean = { true },
        change: (MiscellaneousParamPacket) -> Unit,
    ): Boolean {
        val current = packet ?: return false
        if (!capability(ParameterCapabilities(firmwareInfo.value))) return false
        if (!allowInvalidEvap && !isPackable(current)) {
            publishValues(validationError = true)
            return false
        }

        val before = current.copy()
        val snapshot = try {
            change(current)
            current.snapshot()
        } catch (_: IllegalArgumentException) {
            restore(current, before)
            publishValues(validationError = true)
            return false
        }

        publishValues()
        return sendPacket(snapshot)
    }

    private fun isPackable(packet: MiscellaneousParamPacket): Boolean = try {
        packet.pack()
        true
    } catch (_: IllegalArgumentException) {
        false
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
            ) raw to decodedSlope else null
        }
        val (raw, decodedSlope) = representable.minByOrNull { (candidateRaw, _) -> abs(candidateRaw - requestedRaw) }
            ?: throw IllegalArgumentException("EVAP slope has no safe wire representation")
        require((decodedSlope * 32f * 1048576f).roundToInt() == raw)
        packet.evapAfbegin = begin
        packet.evapAfslope = decodedSlope
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

    private fun publishValues(validationError: Boolean = false) {
        val current = packet ?: return
        val begin = current.evapAfbegin
        val end = current.evapAfEnd
        mutableValues.value = MiscellaneousValues(
            baudDivisor = current.uartDivisor,
            period = current.uartPeriodTms,
            ignitionCutoff = current.ignCutoff > 0,
            cutoff = current.ignCutoffThrd,
            start = current.hopStartAng,
            duration = current.hopDuratAng,
            pumpOffOnGas = current.offPumpOnGas,
            injectorsOffOnGas = current.offInjOnGas,
            injectorsOffOnPetrol = current.offInjOnPetrol,
            begin = begin,
            end = end,
            beginBounds = evapEditBounds(begin = begin, end = end, editBegin = true),
            endBounds = evapEditBounds(begin = begin, end = end, editBegin = false),
            timeout = current.fpTimeoutStrt,
            pwm0 = current.pwmFrq0,
            pwm1 = current.pwmFrq1,
            vss = current.vssPeriodDist,
            hasValidationError = validationError,
        )
    }

    private fun evapEditBounds(begin: Int, end: Int, editBegin: Boolean): Pair<Int, Int>? {
        val bounds = if (editBegin) EVAP_MIN to minOf(EVAP_MAX, end - MiscellaneousParamPacket.MIN_EVAP_AIRFLOW_DELTA)
        else maxOf(EVAP_MIN, begin + MiscellaneousParamPacket.MIN_EVAP_AIRFLOW_DELTA) to EVAP_MAX
        return bounds.takeIf { it.first <= it.second }
    }

    companion object {
        val BAUD_RATES: Map<Int, Int> = linkedMapOf(
            0x0411 to 2400, 0x0208 to 4800, 0x0103 to 9600, 0x00AD to 14400, 0x0081 to 19200,
            0x0056 to 28800, 0x0040 to 38400, 0x002A to 57600, 0x0015 to 115200, 0x0009 to 250000,
            0x0004 to 500000,
        )

        const val PERIOD_MIN = 0
        const val PERIOD_MAX = 500
        const val CUTOFF_MIN = 1000
        const val CUTOFF_MAX = 30000
        const val START_MIN = -36
        const val START_MAX = 72
        const val DURATION_MIN = 6
        const val DURATION_MAX = 180
        const val PWM_MIN = 10
        const val PWM_MAX = 5000
        const val VSS_MIN = 100
        const val VSS_MAX = 60000
        const val TIMEOUT_MIN = 0.1f
        const val TIMEOUT_MAX = 25.5f
        private const val EVAP_MIN = 100
        private const val EVAP_MAX = 2_000_000
    }
}
