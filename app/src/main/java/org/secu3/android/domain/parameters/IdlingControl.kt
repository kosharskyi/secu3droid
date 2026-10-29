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
import org.secu3.android.models.packets.out.params.IdlingParamPacket
import javax.inject.Inject

data class IdlingValues(
    val iFac1: Float,
    val iFac2: Float,
    val maxAngle: Float,
    val minAngle: Float,
    val targetRpm: Int,
    val deadBandRpm: Int,
    val turnOnTemperature: Float,
    val iacAddAfterExit: Float,
    val rpmAddOnRun: Int,
    val proportionalNegative: Float,
    val integralNegative: Float,
    val proportionalPositive: Float,
    val integralPositive: Float,
    val transientThreshold1: Float,
    val transientThreshold2: Float,
    val integratorRpmLimit: Int,
    val mapThreshold: Float,
    val iacMinimumPosition: Float,
    val iacMaximumPosition: Float,
    val iacDeadBandRpm: Int,
    val differential: Float,
    val rigidityLoad: Float,
    val rigidityRpm: Float,
    val loadThreshold: Float,
    val regulatorEnabled: Boolean,
    val regulatorOnGas: Boolean,
    val proportionalMode: Boolean,
    val closedLoop: Boolean,
    val throttleAssistMap: Boolean,
    val closedLoopOnGas: Boolean,
    val worksWithIacRegulator: Boolean,
) {
    companion object {
        fun from(packet: IdlingParamPacket) = IdlingValues(
            iFac1 = packet.iFac1,
            iFac2 = packet.iFac2,
            maxAngle = packet.idlregMaxAngle,
            minAngle = packet.idlregMinAngle,
            targetRpm = packet.idlingRpm,
            deadBandRpm = packet.minefr,
            turnOnTemperature = packet.idlregTurnOnTemp,
            iacAddAfterExit = packet.idlToRunAdd,
            rpmAddOnRun = packet.rpmOnRunAdd,
            proportionalNegative = packet.idlRegP0,
            integralNegative = packet.idlRegI0,
            proportionalPositive = packet.idlRegP1,
            integralPositive = packet.idlRegI1,
            transientThreshold1 = packet.coefThrd1,
            transientThreshold2 = packet.coefThrd2,
            integratorRpmLimit = packet.integratorRpmLim,
            mapThreshold = packet.mapValue,
            iacMinimumPosition = packet.iacMinPos,
            iacMaximumPosition = packet.iacMaxPos,
            iacDeadBandRpm = packet.iacRegDb,
            differential = packet.idlRegD,
            rigidityLoad = packet.irrKLoad,
            rigidityRpm = packet.irrKRpm,
            loadThreshold = packet.loadIdlThrd,
            regulatorEnabled = packet.useRegulator,
            regulatorOnGas = packet.useRegulatorOnGas,
            proportionalMode = packet.pRegMode,
            closedLoop = packet.useClosedLoop,
            throttleAssistMap = packet.useThrassmap,
            closedLoopOnGas = packet.useClosedLoopOnGas,
            worksWithIacRegulator = packet.idlRegWorksWithIAC,
        )
    }
}

/** Owns IDLREG packet edits, live firmware gates, and immutable command snapshots. */
@ViewModelScoped
class IdlingControl internal constructor(
    private val firmware: () -> FirmwareInfoPacket?,
    private val sendPacket: (OutputPacket) -> Boolean,
) {
    @Inject
    constructor(connectionManager: Secu3ConnectionManager, parameterSession: ParameterSession) : this(
        firmware = { connectionManager.fwInfo },
        sendPacket = parameterSession::sendPacket,
    )

    private val mutableValues = MutableStateFlow<IdlingValues?>(null)
    val values: StateFlow<IdlingValues?> = mutableValues.asStateFlow()
    private var packet: IdlingParamPacket? = null

    fun reset() {
        packet = null
        mutableValues.value = null
    }

    fun receive(received: IdlingParamPacket) {
        packet = received
        publishValues()
    }

    fun editIFac1(value: Float) = editFloat(value, 0f, 10f) { it.iFac1 = value }
    fun editIFac2(value: Float) = editFloat(value, 0f, 10f) { it.iFac2 = value }
    fun editMaxAngle(value: Float) = editFloat(value, -15f, 30f) { it.idlregMaxAngle = value }
    fun editMinAngle(value: Float) = editFloat(value, -15f, 30f) { it.idlregMinAngle = value }
    fun editTargetRpm(value: Int) = editInt(value, 0, 1800) { it.idlingRpm = value }
    fun editDeadBand(value: Int) = editInt(value, 0, 500) { it.minefr = value }
    fun editTurnOnTemperature(value: Float) = editFloat(value, -40f, 300f) { it.idlregTurnOnTemp = value }
    fun editIacAddAfterExit(value: Float) = editFloat(value, 0f, 100f, capabilities().iacAdditionEnabled(throttleAssistMap())) { it.idlToRunAdd = value }
    fun editRpmAddOnRun(value: Int) = editInt(value, 0, 2000, capabilities().fuelInjection) { it.rpmOnRunAdd = value }
    fun editProportionalNegative(value: Float) = editFloat(value, 0f, 5f, capabilities().fuelInjection) { it.idlRegP0 = value }
    fun editIntegralNegative(value: Float) = editFloat(value, 0f, 5f, capabilities().fuelInjection) { it.idlRegI0 = value }
    fun editProportionalPositive(value: Float) = editFloat(value, 0f, 5f, capabilities().fuelInjection) { it.idlRegP1 = value }
    fun editIntegralPositive(value: Float) = editFloat(value, 0f, 5f, capabilities().fuelInjection) { it.idlRegI1 = value }
    fun editTransientThreshold1(value: Float) = editFloat(value, 1f, 2.99f, capabilities().fuelInjection) { it.coefThrd1 = value }
    fun editTransientThreshold2(value: Float) = editFloat(value, 1f, 2.99f, capabilities().fuelInjection) { it.coefThrd2 = value }
    fun editIntegratorRpmLimit(value: Int) = editInt(value, 0, 1200, capabilities().fuelInjection) { it.integratorRpmLim = value }
    fun editMapThreshold(value: Float) = editFloat(value, 0f, 100f, capabilities().fuelInjection) { it.mapValue = value }
    fun editIacMinimumPosition(value: Float) = editFloat(value, 0f, 100f, capabilities().fuelInjection) { it.iacMinPos = value }
    fun editIacMaximumPosition(value: Float) = editFloat(value, 0f, 100f, capabilities().fuelInjection) { it.iacMaxPos = value }
    fun editIacDeadBand(value: Int) = editInt(value, 0, 500, capabilities().fuelInjection) { it.iacRegDb = value }
    fun editDifferential(value: Float) = editFloat(value, 0f, 5f, capabilities().fuelInjection) { it.idlRegD = value }
    fun editRigidityLoad(value: Float) = editFloat(value, 0f, 48f, capabilities().fuelInjection) { it.irrKLoad = value }
    fun editRigidityRpm(value: Float) = editFloat(value, 0f, 48f, capabilities().fuelInjection) { it.irrKRpm = value }
    fun editLoadThreshold(value: Float) = editFloat(value, 0f, 500f, capabilities().fuelInjection) { it.loadIdlThrd = value }

    fun setRegulatorEnabled(enabled: Boolean) = edit { it.useRegulator = enabled }
    fun setRegulatorOnGas(enabled: Boolean) = edit { it.useRegulatorOnGas = enabled }
    fun setProportionalMode(enabled: Boolean) = edit { it.pRegMode = enabled }
    fun setClosedLoop(enabled: Boolean) = edit(capabilities().fuelInjection) { it.useClosedLoop = enabled }
    fun setThrottleAssistMap(enabled: Boolean) = edit(capabilities().fuelInjection) { it.useThrassmap = enabled }
    fun setClosedLoopOnGas(enabled: Boolean) = edit(capabilities().fuelInjection) { it.useClosedLoopOnGas = enabled }
    fun setWorksWithIacRegulator(enabled: Boolean) = edit(capabilities().fuelInjection) { it.idlRegWorksWithIAC = enabled }

    private fun capabilities() = ParameterCapabilities(firmware())
    private fun throttleAssistMap() = packet?.useThrassmap == true

    private inline fun editFloat(value: Float, min: Float, max: Float, capability: Boolean = true, change: (IdlingParamPacket) -> Unit) =
        edit(value.isFinite() && value in min..max && capability, change)

    private inline fun editInt(value: Int, min: Int, max: Int, capability: Boolean = true, change: (IdlingParamPacket) -> Unit) =
        edit(value in min..max && capability, change)

    private inline fun edit(valid: Boolean = true, change: (IdlingParamPacket) -> Unit): Boolean {
        if (!valid) return false
        val current = packet ?: return false
        val before = IdlingFields(current)
        val admitted = try {
            change(current)
            val bytes = current.pack().copyOf()
            sendPacket(object : OutputPacket {
                override fun pack(): IntArray = bytes.copyOf()
            })
        } catch (_: IllegalArgumentException) {
            false
        }
        if (!admitted) {
            before.restore(current)
            return false
        }
        publishValues()
        return true
    }

    private fun publishValues() {
        packet?.let { mutableValues.value = IdlingValues.from(it) }
    }

    private data class IdlingFields(
        val iFac1: Float, val iFac2: Float, val maxAngle: Float, val minAngle: Float,
        val targetRpm: Int, val deadBandRpm: Int, val turnOnTemperature: Float,
        val iacAddAfterExit: Float, val rpmAddOnRun: Int, val p0: Float, val i0: Float,
        val p1: Float, val i1: Float, val threshold1: Float, val threshold2: Float,
        val integratorRpmLimit: Int, val mapThreshold: Float, val iacMinimum: Float,
        val iacMaximum: Float, val iacDeadBand: Int, val differential: Float,
        val rigidityLoad: Float, val rigidityRpm: Float, val loadThreshold: Float,
        val flags: Int,
    ) {
        constructor(p: IdlingParamPacket) : this(
            p.iFac1, p.iFac2, p.idlregMaxAngle, p.idlregMinAngle, p.idlingRpm, p.minefr,
            p.idlregTurnOnTemp, p.idlToRunAdd, p.rpmOnRunAdd, p.idlRegP0, p.idlRegI0,
            p.idlRegP1, p.idlRegI1, p.coefThrd1, p.coefThrd2, p.integratorRpmLim,
            p.mapValue, p.iacMinPos, p.iacMaxPos, p.iacRegDb, p.idlRegD, p.irrKLoad,
            p.irrKRpm, p.loadIdlThrd, p.idlFlags,
        )

        fun restore(p: IdlingParamPacket) {
            p.iFac1 = iFac1; p.iFac2 = iFac2; p.idlregMaxAngle = maxAngle; p.idlregMinAngle = minAngle
            p.idlingRpm = targetRpm; p.minefr = deadBandRpm; p.idlregTurnOnTemp = turnOnTemperature
            p.idlToRunAdd = iacAddAfterExit; p.rpmOnRunAdd = rpmAddOnRun; p.idlRegP0 = p0; p.idlRegI0 = i0
            p.idlRegP1 = p1; p.idlRegI1 = i1; p.coefThrd1 = threshold1; p.coefThrd2 = threshold2
            p.integratorRpmLim = integratorRpmLimit; p.mapValue = mapThreshold; p.iacMinPos = iacMinimum
            p.iacMaxPos = iacMaximum; p.iacRegDb = iacDeadBand; p.idlRegD = differential
            p.irrKLoad = rigidityLoad; p.irrKRpm = rigidityRpm; p.loadIdlThrd = loadThreshold; p.idlFlags = flags
        }
    }
}
