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
import org.secu3.android.models.packets.input.FirmwareInfoPacket
import org.secu3.android.models.packets.out.params.InjctrParPacket
import javax.inject.Inject

data class FuelBankValues(
    val flow: Float,
    val config: Int,
    val pulses: Int,
    val timing: Float,
    val cranking: Float,
    val useMap: Boolean,
    val angleSpec: Int,
    val minPw: Float,
    val maxPw: Float,
    val configOptions: List<Int>,
    val pulseOptions: List<Int>,
)

data class FuelInjectionValues(
    val engineDisplacement: Float,
    val banks: List<FuelBankValues>,
    val pulsesPerLiter: Float,
    val additionalCorrections: Boolean,
    val airDensity: Boolean,
    val differentialPressure: Boolean,
    val switchRows: Boolean,
    val fullSequentialAfterStart: Boolean,
    val decreaseRate: Float,
    val increaseRate: Float,
)

/** Owns Fuel Injection packet edits, firmware guards, and immutable command snapshots. */
@ViewModelScoped
class FuelInjectionControl internal constructor(
    private val firmwareInfo: StateFlow<FirmwareInfoPacket?>,
    private val sendPacket: (OutputPacket) -> Boolean,
) {
    @Inject
    constructor(connectionManager: Secu3ConnectionManager, parameterSession: ParameterSession) : this(
        connectionManager.firmwareInfo,
        parameterSession::sendPacket,
    )

    private val mutableValues = MutableStateFlow<FuelInjectionValues?>(null)
    val values: StateFlow<FuelInjectionValues?> = mutableValues.asStateFlow()

    private var packet: InjctrParPacket? = null

    fun reset() {
        packet = null
        mutableValues.value = null
    }

    fun receive(received: InjctrParPacket) {
        packet = received
        publishValues()
    }

    fun editEngineDisplacement(value: Float): Boolean {
        if (!canControl() || !value.isFinite()) return false
        val current = packet ?: return false
        if (current.ckpsEngineCyl <= 0 || value <= 0f) return false
        current.engineDisp = value
        return sendOrdinary(current)
    }

    fun editFlow(bank: Int, value: Float): Boolean {
        if (!canControl() || bank !in 0..1 || !value.isFinite()) return false
        val current = packet ?: return false
        current.flowRate[bank] = value
        current.recalculateDerivedConstants()
        return sendOrdinary(current)
    }

    fun editTiming(bank: Int, value: Float): Boolean {
        if (!canControl() || bank !in 0..1 || !value.isFinite()) return false
        val current = packet ?: return false
        if (if (bank == 0) current.useTimingMap else current.useTimingMapG) return false
        current.timing[bank] = value
        return sendOrdinary(current)
    }

    fun editCrankingTiming(bank: Int, value: Float): Boolean {
        if (!canControl() || bank !in 0..1 || !value.isFinite()) return false
        val current = packet ?: return false
        current.timingCrk[bank] = value
        return sendOrdinary(current)
    }

    fun editMinimumPulseWidth(bank: Int, value: Float): Boolean {
        if (!canControl() || bank !in 0..1 || !value.isFinite()) return false
        val current = packet ?: return false
        if (bank == 0) current.minPw0 = value else current.minPw1 = value
        return sendOrdinary(current)
    }

    fun editMaximumPulseWidth(bank: Int, value: Float): Boolean {
        if (!canControl() || bank !in 0..1 || !value.isFinite()) return false
        val current = packet ?: return false
        current.injMaxPw[bank] = value
        return sendOrdinary(current)
    }

    fun editPulsesPerLiter(value: Float): Boolean {
        if (!canControl() || !value.isFinite()) return false
        val current = packet ?: return false
        current.fffConst = value
        return sendOrdinary(current)
    }

    fun editDecreaseRate(value: Float): Boolean {
        if (!canControl() || !value.isFinite()) return false
        val current = packet ?: return false
        current.injPwDecSpeed = value
        return sendOrdinary(current)
    }

    fun editIncreaseRate(value: Float): Boolean {
        if (!canControl() || !value.isFinite()) return false
        val current = packet ?: return false
        current.injPwIncSpeed = value
        return sendOrdinary(current)
    }

    fun changeConfig(bank: Int, config: Int): Boolean {
        if (!canControl() || bank !in 0..1) return false
        val current = packet ?: return false
        if (config !in current.validConfigurations()) return false
        val pulseChoices = current.validPulses(config)
        if (pulseChoices.isEmpty()) return false
        val oldPulses = if (bank == 0) current.config0Pulses else current.config1Pulses
        val nextPulses = oldPulses.takeIf(pulseChoices::contains) ?: pulseChoices.first()
        if (bank == 0) {
            current.config0Pulses = nextPulses
            current.config0 = config
        } else {
            current.config1Pulses = nextPulses
            current.config1 = config
        }
        return sendOrdinary(current)
    }

    fun changePulses(bank: Int, pulses: Int): Boolean {
        if (!canControl() || bank !in 0..1) return false
        val current = packet ?: return false
        val config = if (bank == 0) current.config0 else current.config1
        if (pulses !in current.validPulses(config)) return false
        if (bank == 0) current.config0Pulses = pulses else current.config1Pulses = pulses
        return sendOrdinary(current)
    }

    fun changeAngleSpec(bank: Int, value: Int): Boolean {
        if (!canControl() || bank !in 0..1 || value !in 0..2) return false
        val current = packet ?: return false
        if (bank == 0) current.angleSpec0 = value else current.angleSpec1 = value
        return sendOrdinary(current)
    }

    fun setUseMap(bank: Int, value: Boolean): Boolean {
        if (!canControl() || bank !in 0..1) return false
        val current = packet ?: return false
        if (bank == 0) current.useTimingMap = value else current.useTimingMapG = value
        return sendOrdinary(current)
    }

    fun setAdditionalCorrections(value: Boolean): Boolean {
        if (!canControl() || !ParameterCapabilities(firmwareInfo.value).dbw) return false
        val current = packet ?: return false
        current.useAdditionalCorrections = value
        return sendOrdinary(current)
    }

    fun setAirDensity(value: Boolean): Boolean {
        if (!canControl()) return false
        val current = packet ?: return false
        current.useAirDensity = value
        return sendOrdinary(current)
    }

    fun setDifferentialPressure(value: Boolean): Boolean {
        if (!canControl() || !ParameterCapabilities(firmwareInfo.value).dbw) return false
        val current = packet ?: return false
        current.useDifferentialPressure = value
        return sendOrdinary(current)
    }

    fun setSwitchRows(value: Boolean): Boolean {
        if (!canControl() || !ParameterCapabilities(firmwareInfo.value).dbw) return false
        val current = packet ?: return false
        current.switchSecondInjRow = value
        return sendOrdinary(current)
    }

    fun setFullSequentialAfterStart(value: Boolean): Boolean {
        if (!canControl()) return false
        val current = packet ?: return false
        current.fullSequentialAfterStart = value
        return sendOrdinary(current)
    }

    private fun canControl() = ParameterCapabilities(firmwareInfo.value).fuelInjection

    private fun sendOrdinary(current: InjctrParPacket): Boolean {
        publishValues()
        return sendPacket(current.snapshot())
    }

    private fun publishValues() {
        val current = packet ?: return
        mutableValues.value = FuelInjectionValues(
            engineDisplacement = current.engineDisp,
            banks = (0..1).map { bank ->
                val config = if (bank == 0) current.config0 else current.config1
                FuelBankValues(
                    flow = current.flowRate[bank],
                    config = config,
                    pulses = if (bank == 0) current.config0Pulses else current.config1Pulses,
                    timing = current.timing[bank],
                    cranking = current.timingCrk[bank],
                    useMap = if (bank == 0) current.useTimingMap else current.useTimingMapG,
                    angleSpec = if (bank == 0) current.angleSpec0 else current.angleSpec1,
                    minPw = if (bank == 0) current.minPw0 else current.minPw1,
                    maxPw = current.injMaxPw[bank],
                    configOptions = current.validConfigurations(),
                    pulseOptions = current.validPulses(config),
                )
            },
            pulsesPerLiter = current.fffConst,
            additionalCorrections = current.useAdditionalCorrections,
            airDensity = current.useAirDensity,
            differentialPressure = current.useDifferentialPressure,
            switchRows = current.switchSecondInjRow,
            fullSequentialAfterStart = current.fullSequentialAfterStart,
            decreaseRate = current.injPwDecSpeed,
            increaseRate = current.injPwIncSpeed,
        )
    }

    companion object {
        // Current editor bounds; command methods separately preserve accepted finite protocol values.
        const val ENGINE_DISPLACEMENT_MIN = .01f
        const val ENGINE_DISPLACEMENT_MAX = 8f
        const val FLOW_MIN = 50f
        const val FLOW_MAX = 1023f
        const val TIMING_MIN = 0f
        const val TIMING_MAX = 720f
        const val MINIMUM_PULSE_WIDTH_MIN = .1f
        const val MINIMUM_PULSE_WIDTH_MAX = 6.5f
        const val MAXIMUM_PULSE_WIDTH_MIN = 0f
        const val MAXIMUM_PULSE_WIDTH_MAX = 100f
        const val PULSES_PER_LITER_MIN = 8000f
        const val PULSES_PER_LITER_MAX = 32000f
        const val RATE_MIN = .01f
        const val RATE_MAX = 10f
    }
}
