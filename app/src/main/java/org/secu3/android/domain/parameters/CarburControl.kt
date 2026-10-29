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
import org.secu3.android.models.packets.out.params.CarburParamPacket
import javax.inject.Inject

data class CarburValues(
    val idleLowRpm: Int,
    val idleHighRpm: Int,
    val idleLowGasRpm: Int,
    val idleHighGasRpm: Int,
    val cutoffDelay: Float,
    val valvePressure: Float,
    val tpsThreshold: Float,
    val mapThreshold: Float,
    val coolantThreshold: Float,
    val revLimitLowRpm: Int,
    val revLimitHighRpm: Int,
    val inverted: Boolean,
    val fuelCutOutput: Int,
    val ignitionCutOutput: Int,
)

/** Owns Carbur packet edits, live firmware gates, and immutable command snapshots. */
@ViewModelScoped
class CarburControl internal constructor(
    private val firmware: () -> FirmwareInfoPacket?,
    private val sendPacket: (OutputPacket) -> Boolean,
) {
    @Inject
    constructor(connectionManager: Secu3ConnectionManager, parameterSession: ParameterSession) : this(
        firmware = { connectionManager.fwInfo },
        sendPacket = parameterSession::sendPacket,
    )

    private val mutableValues = MutableStateFlow<CarburValues?>(null)
    val values: StateFlow<CarburValues?> = mutableValues.asStateFlow()
    private var packet: CarburParamPacket? = null

    /** Firmware selector codes in displayed option order; 15 is the disabled sentinel. */
    val selectorCodes = listOf(0, 1, 2, 3, 4, 5, 15)

    fun reset() {
        packet = null
        mutableValues.value = null
    }

    fun receive(received: CarburParamPacket) {
        packet = received
        publishValues()
    }

    fun editIdleLowRpm(value: Int) = edit(value in IDLE_RPM_MIN..IDLE_RPM_MAX) { it.ieLot = value }
    fun editIdleHighRpm(value: Int) = edit(value in IDLE_RPM_MIN..IDLE_RPM_MAX) { it.ieHit = value }
    fun editIdleLowGasRpm(value: Int) = edit(value in IDLE_RPM_MIN..IDLE_RPM_MAX) { it.ieLotG = value }
    fun editIdleHighGasRpm(value: Int) = edit(value in IDLE_RPM_MIN..IDLE_RPM_MAX) { it.ieHitG = value }
    fun editCutoffDelay(value: Float) = editFloat(value, CUTOFF_DELAY_MIN, CUTOFF_DELAY_MAX) { it.shutoffDelay = value }
    fun editValvePressure(value: Float) = editFloat(value, VALVE_PRESSURE_MIN, VALVE_PRESSURE_MAX) { it.feOnThresholds = value }
    fun editTpsThreshold(value: Float) = editFloat(value, TPS_MIN, TPS_MAX) { it.tpsThreshold = value }
    fun editMapThreshold(value: Float) = editFloat(value, MAP_MIN, MAP_MAX, ParameterCapabilities(firmware()).fuelInjection) { it.fuelcutMapThrd = value }
    fun editCoolantThreshold(value: Float) = editFloat(value, CTS_MIN, CTS_MAX, ParameterCapabilities(firmware()).fuelInjection) { it.fuelcutCtsThrd = value }
    fun editRevLimitLowRpm(value: Int) = edit(value in REV_RPM_MIN..REV_RPM_MAX && ParameterCapabilities(firmware()).fuelOrGasDose) { it.revlimLot = value }
    fun editRevLimitHighRpm(value: Int) = edit(value in REV_RPM_MIN..REV_RPM_MAX && ParameterCapabilities(firmware()).fuelOrGasDose) { it.revlimHit = value }

    fun setInverted(enabled: Boolean) = edit { it.carbInvers = if (enabled) 1 else 0 }

    fun selectFuelCutOutput(index: Int) = selectOutput(index, ignition = false)
    fun selectIgnitionCutOutput(index: Int) = selectOutput(index, ignition = true)

    private fun selectOutput(index: Int, ignition: Boolean): Boolean {
        val allowed = ignition || ParameterCapabilities(firmware()).fuelInjection
        val code = selectorCodes.getOrNull(index) ?: return false
        return edit(allowed) { packet ->
            if (ignition) packet.igncut_uni = code else packet.fuelcut_uni = code
        }
    }

    private inline fun editFloat(
        value: Float,
        min: Float,
        max: Float,
        capability: Boolean = true,
        change: (CarburParamPacket) -> Unit,
    ): Boolean = edit(value.isFinite() && value in min..max && capability, change)

    private inline fun edit(valid: Boolean = true, change: (CarburParamPacket) -> Unit): Boolean {
        if (!valid) return false
        val current = packet ?: return false
        val before = CarburFields(current)
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
        val current = packet ?: return
        mutableValues.value = CarburValues(
            idleLowRpm = current.ieLot,
            idleHighRpm = current.ieHit,
            idleLowGasRpm = current.ieLotG,
            idleHighGasRpm = current.ieHitG,
            cutoffDelay = current.shutoffDelay,
            valvePressure = current.feOnThresholds,
            tpsThreshold = current.tpsThreshold,
            mapThreshold = current.fuelcutMapThrd,
            coolantThreshold = current.fuelcutCtsThrd,
            revLimitLowRpm = current.revlimLot,
            revLimitHighRpm = current.revlimHit,
            inverted = current.carbInvers > 0,
            fuelCutOutput = current.fuelcut_uni,
            ignitionCutOutput = current.igncut_uni,
        )
    }

    private data class CarburFields(
        val idleLowRpm: Int,
        val idleHighRpm: Int,
        val inverse: Int,
        val valvePressure: Float,
        val idleLowGasRpm: Int,
        val idleHighGasRpm: Int,
        val cutoffDelay: Float,
        val tpsThreshold: Float,
        val mapThreshold: Float,
        val coolantThreshold: Float,
        val revLimitLowRpm: Int,
        val revLimitHighRpm: Int,
        val fuelCutOutput: Int,
        val ignitionCutOutput: Int,
    ) {
        constructor(packet: CarburParamPacket) : this(
            packet.ieLot, packet.ieHit, packet.carbInvers, packet.feOnThresholds,
            packet.ieLotG, packet.ieHitG, packet.shutoffDelay, packet.tpsThreshold,
            packet.fuelcutMapThrd, packet.fuelcutCtsThrd, packet.revlimLot, packet.revlimHit,
            packet.fuelcut_uni, packet.igncut_uni,
        )

        fun restore(packet: CarburParamPacket) {
            packet.ieLot = idleLowRpm
            packet.ieHit = idleHighRpm
            packet.carbInvers = inverse
            packet.feOnThresholds = valvePressure
            packet.ieLotG = idleLowGasRpm
            packet.ieHitG = idleHighGasRpm
            packet.shutoffDelay = cutoffDelay
            packet.tpsThreshold = tpsThreshold
            packet.fuelcutMapThrd = mapThreshold
            packet.fuelcutCtsThrd = coolantThreshold
            packet.revlimLot = revLimitLowRpm
            packet.revlimHit = revLimitHighRpm
            packet.fuelcut_uni = fuelCutOutput
            packet.igncut_uni = ignitionCutOutput
        }
    }

    companion object {
        const val IDLE_RPM_MIN = 250
        const val IDLE_RPM_MAX = 30000
        const val CUTOFF_DELAY_MIN = 0f
        const val CUTOFF_DELAY_MAX = 2.5f
        const val VALVE_PRESSURE_MIN = 0f
        const val VALVE_PRESSURE_MAX = 500f
        const val TPS_MIN = 0f
        const val TPS_MAX = 100f
        const val MAP_MIN = 0f
        const val MAP_MAX = 250f
        const val CTS_MIN = -40f
        const val CTS_MAX = 300f
        const val REV_RPM_MIN = 500
        const val REV_RPM_MAX = 30000
    }
}
