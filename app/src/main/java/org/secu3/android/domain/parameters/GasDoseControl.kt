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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.secu3.android.connection.Secu3ConnectionManager
import org.secu3.android.models.packets.base.OutputPacket
import org.secu3.android.models.packets.input.FirmwareInfoPacket
import org.secu3.android.models.packets.out.params.GasDoseParamPacket
import javax.inject.Inject

data class GasDoseValues(
    val steps: Float,
    val stoichiometricRatio: Float,
    val fuelCutClosing: Float,
    val correctionPositive: Float,
    val correctionNegative: Float,
    val testing: Boolean,
    val frequency: Int,
    val maxFrequencyAtInit: Boolean,
)

/** Owns Gas Dose parameter editing and admission of immutable command snapshots. */
@ViewModelScoped
class GasDoseControl internal constructor(
    private val firmwareInfo: StateFlow<FirmwareInfoPacket?>,
    private val sendPacket: (OutputPacket) -> Boolean,
    private val manualPosition: ManualPositionDelta = ManualPositionDelta(),
) {
    @Inject
    constructor(
        connectionManager: Secu3ConnectionManager,
        parameterSession: ParameterSession,
        manualPosition: ManualPositionDelta,
    ) : this(
        connectionManager.firmwareInfo,
        parameterSession::sendPacket,
        manualPosition,
    )

    private val mutableValues = MutableStateFlow<GasDoseValues?>(null)
    val values: StateFlow<GasDoseValues?> = mutableValues.asStateFlow()

    private var packet: GasDoseParamPacket? = null
    private var firmwareJob: Job? = null

    fun start(scope: CoroutineScope) {
        if (firmwareJob != null) return
        firmwareJob = scope.launch {
            firmwareInfo.collect { firmware ->
                if (!ParameterCapabilities(firmware).gasDose) manualPosition.clear()
            }
        }
    }

    fun reset() {
        packet = null
        manualPosition.clear()
        mutableValues.value = null
    }

    fun receive(received: GasDoseParamPacket) {
        manualPosition.clear()
        received.manualPositionD = 0
        packet = received
        publishValues()
    }

    fun editSteps(value: Float): Boolean {
        if (!canControl() || !value.isFinite()) return false
        val current = packet ?: return false
        current.steps = value.toInt().coerceIn(STEPS_MIN.toInt(), STEPS_MAX.toInt())
        return sendOrdinary(current)
    }

    fun editStoichiometricRatio(value: Float): Boolean {
        if (!canControl() || !value.isFinite()) return false
        val current = packet ?: return false
        current.lambdaStoichval = value.coerceIn(STOICHIOMETRIC_RATIO_MIN, STOICHIOMETRIC_RATIO_MAX)
        return sendOrdinary(current)
    }

    fun editFuelCutClosing(value: Float): Boolean {
        if (!canControl() || !value.isFinite()) return false
        val current = packet ?: return false
        current.fcClosing = value.coerceIn(FUEL_CUT_CLOSING_MIN, FUEL_CUT_CLOSING_MAX)
        return sendOrdinary(current)
    }

    fun editCorrectionPositive(value: Float): Boolean {
        if (!canControl() || !value.isFinite()) return false
        val current = packet ?: return false
        current.lambdaCorrLimitP = value.coerceIn(CORRECTION_MIN, CORRECTION_MAX)
        return sendOrdinary(current)
    }

    fun editCorrectionNegative(value: Float): Boolean {
        if (!canControl() || !value.isFinite()) return false
        val current = packet ?: return false
        current.lambdaCorrLimitM = value.coerceIn(CORRECTION_MIN, CORRECTION_MAX)
        return sendOrdinary(current)
    }

    fun selectFrequency(index: Int): Boolean {
        if (!canControl() || index !in FREQUENCY_MIN..FREQUENCY_MAX) return false
        val current = packet ?: return false
        current.freq = index
        return sendOrdinary(current)
    }

    fun setMaximumFrequency(enabled: Boolean): Boolean {
        if (!canControl()) return false
        val current = packet ?: return false
        current.maxFreqInit = if (enabled) 1 else 0
        return sendOrdinary(current)
    }

    fun toggleTest(): Boolean {
        if (!canControl()) return false
        val current = packet ?: return false
        current.testing = !current.testing
        return sendOrdinary(current)
    }

    fun manualStep(direction: Int) {
        if (!canControl() || packet == null) return
        manualPosition.add(direction)
    }

    fun flushManual(): Boolean {
        if (!canControl()) {
            manualPosition.clear()
            return false
        }
        val current = packet ?: return false
        val delta = manualPosition.take()
        if (delta == 0) return false
        return sendPacket(current.snapshotWithManualPosition(delta))
    }

    fun finishManual(): Boolean = flushManual()

    fun cancelManual() {
        manualPosition.clear()
        packet?.manualPositionD = 0
    }

    private fun canControl() = ParameterCapabilities(firmwareInfo.value).gasDose

    private fun sendOrdinary(current: GasDoseParamPacket): Boolean {
        publishValues()
        val snapshot: OutputPacket = current.snapshotWithManualPosition(0)
        return sendPacket(snapshot)
    }

    private fun publishValues() {
        val current = packet ?: return
        mutableValues.value = GasDoseValues(
            steps = current.steps.toFloat(),
            stoichiometricRatio = current.lambdaStoichval,
            fuelCutClosing = current.fcClosing,
            correctionPositive = current.lambdaCorrLimitP,
            correctionNegative = current.lambdaCorrLimitM,
            testing = current.testing,
            frequency = current.freq,
            maxFrequencyAtInit = current.maxFreqInit > 0,
        )
    }

    companion object {
        const val STEPS_MIN = 10f
        const val STEPS_MAX = 2000f
        const val STOICHIOMETRIC_RATIO_MIN = 8f
        const val STOICHIOMETRIC_RATIO_MAX = 22f
        const val FUEL_CUT_CLOSING_MIN = 0f
        const val FUEL_CUT_CLOSING_MAX = 100f
        const val CORRECTION_MIN = .5f
        const val CORRECTION_MAX = 100f
        const val FREQUENCY_MIN = 0
        const val FREQUENCY_MAX = 3
    }
}
