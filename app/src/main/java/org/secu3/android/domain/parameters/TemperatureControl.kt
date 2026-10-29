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
import org.secu3.android.models.packets.out.params.TemperatureParamPacket
import javax.inject.Inject

data class TemperatureValues(
    val fanOn: Float,
    val fanOff: Float,
    val pwmFrequency: Int,
    val conditionerOn: Float,
    val conditionerOff: Float,
    val conditionerMinRpm: Int,
    val fanTimer: Float,
    val coolantSensor: Boolean,
    val pwmControl: Boolean,
    val sensorCurveTable: Boolean,
)

/** Owns Temperature packet edits, live firmware gates, and immutable command snapshots. */
@ViewModelScoped
class TemperatureControl internal constructor(
    private val firmware: () -> FirmwareInfoPacket?,
    private val sendPacket: (OutputPacket) -> Boolean,
) {
    @Inject
    constructor(connectionManager: Secu3ConnectionManager, parameterSession: ParameterSession) : this(
        firmware = { connectionManager.fwInfo },
        sendPacket = parameterSession::sendPacket,
    )

    private val mutableValues = MutableStateFlow<TemperatureValues?>(null)
    val values: StateFlow<TemperatureValues?> = mutableValues.asStateFlow()
    private var packet: TemperatureParamPacket? = null

    fun reset() {
        packet = null
        mutableValues.value = null
    }

    fun receive(received: TemperatureParamPacket) {
        packet = received
        publishValues()
    }

    fun editFanOn(value: Float): Boolean = editFloat(value, FAN_TEMPERATURE_MIN, FAN_TEMPERATURE_MAX) { current ->
        current.ventOn = value
        if (!current.ventPwm && value < current.ventOff + FAN_HYSTERESIS) {
            val adjustedOff = value - FAN_HYSTERESIS
            if (adjustedOff !in FAN_TEMPERATURE_MIN..FAN_TEMPERATURE_MAX) return@editFloat false
            current.ventOff = adjustedOff
        }
        true
    }

    fun editFanOff(value: Float): Boolean = editFloat(value, FAN_TEMPERATURE_MIN, FAN_TEMPERATURE_MAX) { current ->
        current.ventOff = value
        if (value > current.ventOn - FAN_HYSTERESIS) {
            val adjustedOn = value + FAN_HYSTERESIS
            if (adjustedOn !in FAN_TEMPERATURE_MIN..FAN_TEMPERATURE_MAX) return@editFloat false
            current.ventOn = adjustedOn
        }
        true
    }

    fun editPwmFrequency(value: Int): Boolean = edit(value in PWM_FREQUENCY_MIN..PWM_FREQUENCY_MAX && capabilities().fanPwm) {
        it.ventPwmFrq = value
        true
    }

    fun editConditionerOn(value: Float): Boolean = editFloat(
        value, CONDITIONER_VOLTAGE_MIN, CONDITIONER_VOLTAGE_MAX, capabilities().nonSecu3TFuelInjection,
    ) { it.condPvtOn = value; true }

    fun editConditionerOff(value: Float): Boolean = editFloat(
        value, CONDITIONER_VOLTAGE_MIN, CONDITIONER_VOLTAGE_MAX, capabilities().nonSecu3TFuelInjection,
    ) { it.condPvtOff = value; true }

    fun editConditionerMinRpm(value: Int): Boolean = edit(value in CONDITIONER_RPM_MIN..CONDITIONER_RPM_MAX && capabilities().fuelInjection) {
        it.condMinRpm = value
        true
    }

    fun editFanTimer(value: Int): Boolean = edit(value in FAN_TIMER_MIN..FAN_TIMER_MAX) {
        it.ventTmr = value.toFloat()
        true
    }

    fun setCoolantSensor(enabled: Boolean): Boolean = edit { it.coolantUse = enabled; true }

    fun setPwmControl(enabled: Boolean): Boolean = edit(capabilities().fanPwm) { current ->
        current.ventPwm = enabled
        if (current.ventOn < current.ventOff + FAN_HYSTERESIS) {
            val adjustedOff = current.ventOn - FAN_HYSTERESIS
            if (adjustedOff !in FAN_TEMPERATURE_MIN..FAN_TEMPERATURE_MAX) return@edit false
            current.ventOff = adjustedOff
        }
        true
    }

    fun setSensorCurveTable(enabled: Boolean): Boolean = edit(capabilities().tempCurve) { it.coolantMap = enabled; true }

    private fun capabilities() = ParameterCapabilities(firmware())

    private inline fun editFloat(
        value: Float,
        min: Float,
        max: Float,
        capability: Boolean = true,
        change: (TemperatureParamPacket) -> Boolean,
    ): Boolean = edit(value.isFinite() && value in min..max && capability) { current -> change(current) }

    private inline fun edit(valid: Boolean = true, change: (TemperatureParamPacket) -> Boolean): Boolean {
        if (!valid) return false
        val current = packet ?: return false
        val before = TemperatureFields(current)
        val admitted = try {
            if (!change(current)) {
                before.restore(current)
                return false
            }
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
        mutableValues.value = TemperatureValues(
            fanOn = current.ventOn,
            fanOff = current.ventOff,
            pwmFrequency = current.ventPwmFrq,
            conditionerOn = current.condPvtOn,
            conditionerOff = current.condPvtOff,
            conditionerMinRpm = current.condMinRpm,
            fanTimer = current.ventTmr,
            coolantSensor = current.coolantUse,
            pwmControl = current.ventPwm,
            sensorCurveTable = current.coolantMap,
        )
    }

    private data class TemperatureFields(
        val tmpFlags: Int,
        val ventOn: Float,
        val ventOff: Float,
        val ventPwmFrq: Int,
        val condPvtOn: Float,
        val condPvtOff: Float,
        val condMinRpm: Int,
        val ventTmr: Float,
    ) {
        constructor(packet: TemperatureParamPacket) : this(
            packet.tmpFlags, packet.ventOn, packet.ventOff, packet.ventPwmFrq,
            packet.condPvtOn, packet.condPvtOff, packet.condMinRpm, packet.ventTmr,
        )

        fun restore(packet: TemperatureParamPacket) {
            packet.tmpFlags = tmpFlags
            packet.ventOn = ventOn
            packet.ventOff = ventOff
            packet.ventPwmFrq = ventPwmFrq
            packet.condPvtOn = condPvtOn
            packet.condPvtOff = condPvtOff
            packet.condMinRpm = condMinRpm
            packet.ventTmr = ventTmr
        }
    }

    companion object {
        const val FAN_TEMPERATURE_MIN = -10f
        const val FAN_TEMPERATURE_MAX = 125f
        const val FAN_HYSTERESIS = .25f
        const val PWM_FREQUENCY_MIN = 10
        const val PWM_FREQUENCY_MAX = 5000
        const val CONDITIONER_VOLTAGE_MIN = 0f
        const val CONDITIONER_VOLTAGE_MAX = 5f
        const val CONDITIONER_RPM_MIN = 500
        const val CONDITIONER_RPM_MAX = 3000
        const val FAN_TIMER_MIN = 0
        const val FAN_TIMER_MAX = 600
    }
}
