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
import org.secu3.android.models.packets.out.params.AdcCorrectionsParamPacket
import javax.inject.Inject

data class AdcCorrectionsValues(
    val mapFactor: Float, val mapCorrection: Float,
    val voltageFactor: Float, val voltageCorrection: Float,
    val ctsFactor: Float, val ctsCorrection: Float,
    val tpsFactor: Float, val tpsCorrection: Float,
    val addI1Factor: Float, val addI1Correction: Float,
    val addI2Factor: Float, val addI2Correction: Float,
    val addI3Factor: Float, val addI3Correction: Float,
    val addI4Factor: Float, val addI4Correction: Float,
    val addI5Factor: Float, val addI5Correction: Float,
    val addI6Factor: Float, val addI6Correction: Float,
    val addI7Factor: Float, val addI7Correction: Float,
    val addI8Factor: Float, val addI8Correction: Float,
    val adcCompMode: Boolean,
) {
    companion object {
        fun from(packet: AdcCorrectionsParamPacket) = AdcCorrectionsValues(
            packet.mapAdcFactor, packet.mapAdcCorrection,
            packet.ubatAdcFactor, packet.ubatAdcCorrection,
            packet.tempAdcFactor, packet.tempAdcCorrection,
            packet.tpsAdcFactor, packet.tpsAdcCorrection,
            packet.ai1AdcFactor, packet.ai1AdcCorrection,
            packet.ai2AdcFactor, packet.ai2AdcCorrection,
            packet.ai3AdcFactor, packet.ai3AdcCorrection,
            packet.ai4AdcFactor, packet.ai4AdcCorrection,
            packet.ai5AdcFactor, packet.ai5AdcCorrection,
            packet.ai6AdcFactor, packet.ai6AdcCorrection,
            packet.ai7AdcFactor, packet.ai7AdcCorrection,
            packet.ai8AdcFactor, packet.ai8AdcCorrection,
            packet.adcCompMode,
        )
    }
}

/** Owns ADC correction edits, firmware gates, and transactional raw-word-safe sends. */
@ViewModelScoped
class AdcCorrectionsControl internal constructor(
    private val firmwareInfo: StateFlow<FirmwareInfoPacket?>,
    private val sendPacket: (OutputPacket) -> Boolean,
) {
    @Inject
    constructor(connectionManager: Secu3ConnectionManager, parameterSession: ParameterSession) : this(
        connectionManager.firmwareInfo, parameterSession::sendPacket,
    )

    private val mutableValues = MutableStateFlow<AdcCorrectionsValues?>(null)
    val values: StateFlow<AdcCorrectionsValues?> = mutableValues.asStateFlow()
    private var packet: AdcCorrectionsParamPacket? = null

    fun reset() {
        packet = null
        mutableValues.value = null
    }

    fun receive(received: AdcCorrectionsParamPacket) {
        packet = received
        mutableValues.value = AdcCorrectionsValues.from(received)
    }

    fun editMapFactor(value: Float) = edit(0, true, value) { it.mapAdcFactor = value }
    fun editMapCorrection(value: Float) = edit(0, false, value) { it.mapAdcCorrection = value }
    fun editVoltageFactor(value: Float) = edit(1, true, value) { it.ubatAdcFactor = value }
    fun editVoltageCorrection(value: Float) = edit(1, false, value) { it.ubatAdcCorrection = value }
    fun editCtsFactor(value: Float) = edit(2, true, value) { it.tempAdcFactor = value }
    fun editCtsCorrection(value: Float) = edit(2, false, value) { it.tempAdcCorrection = value }
    fun editTpsFactor(value: Float) = edit(3, true, value) { it.tpsAdcFactor = value }
    fun editTpsCorrection(value: Float) = edit(3, false, value) { it.tpsAdcCorrection = value }
    fun editAddI1Factor(value: Float) = edit(4, true, value) { it.ai1AdcFactor = value }
    fun editAddI1Correction(value: Float) = edit(4, false, value) { it.ai1AdcCorrection = value }
    fun editAddI2Factor(value: Float) = edit(5, true, value) { it.ai2AdcFactor = value }
    fun editAddI2Correction(value: Float) = edit(5, false, value) { it.ai2AdcCorrection = value }
    fun editAddI3Factor(value: Float) = edit(6, true, value) { it.ai3AdcFactor = value }
    fun editAddI3Correction(value: Float) = edit(6, false, value) { it.ai3AdcCorrection = value }
    fun editAddI4Factor(value: Float) = edit(7, true, value) { it.ai4AdcFactor = value }
    fun editAddI4Correction(value: Float) = edit(7, false, value) { it.ai4AdcCorrection = value }
    fun editAddI5Factor(value: Float) = edit(8, true, value) { it.ai5AdcFactor = value }
    fun editAddI5Correction(value: Float) = edit(8, false, value) { it.ai5AdcCorrection = value }
    fun editAddI6Factor(value: Float) = edit(9, true, value) { it.ai6AdcFactor = value }
    fun editAddI6Correction(value: Float) = edit(9, false, value) { it.ai6AdcCorrection = value }
    fun editAddI7Factor(value: Float) = edit(10, true, value) { it.ai7AdcFactor = value }
    fun editAddI7Correction(value: Float) = edit(10, false, value) { it.ai7AdcCorrection = value }
    fun editAddI8Factor(value: Float) = edit(11, true, value) { it.ai8AdcFactor = value }
    fun editAddI8Correction(value: Float) = edit(11, false, value) { it.ai8AdcCorrection = value }

    fun setCompensationMode(enabled: Boolean): Boolean {
        val current = packet ?: return false
        return editPacket(current) { it.adcCompMode = enabled }
    }

    private inline fun edit(
        channel: Int,
        factor: Boolean,
        value: Float,
        crossinline change: (AdcCorrectionsParamPacket) -> Unit,
    ): Boolean {
        if (!value.isFinite() || (if (factor) value !in 0f..3.999f else value !in -2f..1.999f)) return false
        if (!ParameterCapabilities(firmwareInfo.value).adcChannelEnabled(channel)) return false
        val current = packet ?: return false
        return editPacket(current) {
            it.markChannelEdited(channel)
            change(it)
        }
    }

    private inline fun editPacket(
        current: AdcCorrectionsParamPacket,
        change: (AdcCorrectionsParamPacket) -> Unit,
    ): Boolean {
        val checkpoint = current.checkpoint()
        val admitted = try {
            change(current)
            val bytes = current.pack().copyOf()
            sendPacket(object : OutputPacket {
                override fun pack(): IntArray = bytes.copyOf()
            })
        } catch (_: RuntimeException) {
            false
        }
        if (!admitted) {
            current.restore(checkpoint)
            return false
        }
        mutableValues.value = AdcCorrectionsValues.from(current)
        return true
    }
}
