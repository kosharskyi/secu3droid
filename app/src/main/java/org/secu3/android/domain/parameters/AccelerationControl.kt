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
import org.secu3.android.models.packets.out.params.AccelerationParamPacket
import javax.inject.Inject

data class AccelerationValues(
    val tpsThreshold: Int,
    val coldMultiplier: Float,
    val decayTime: Int,
    val aeType: Int,
    val aeTime: Int,
    val balance: Float,
    val mapdotThreshold: Int,
    val xtauStart: Float,
    val xtauFinish: Float,
    val wallwetModel: Int,
) {
    companion object {
        fun from(packet: AccelerationParamPacket) = AccelerationValues(
            packet.injAeTpsdotThrd, packet.injAeColdaccMult, packet.injAeDecayTime,
            packet.injAeType, packet.injAeTime, packet.injAeBallance,
            packet.injAeMapdotThrd, packet.injXtauSThrd, packet.injXtauFThrd,
            packet.wallwetModel,
        )
    }
}

/** Owns Acceleration packet edits while preserving all received fields until explicitly changed. */
@ViewModelScoped
class AccelerationControl internal constructor(
    private val firmwareInfo: StateFlow<FirmwareInfoPacket?>,
    private val sendPacket: (OutputPacket) -> Boolean,
) {
    @Inject
    constructor(connectionManager: Secu3ConnectionManager, parameterSession: ParameterSession) : this(
        connectionManager.firmwareInfo,
        parameterSession::sendPacket,
    )

    private val mutableValues = MutableStateFlow<AccelerationValues?>(null)
    val values: StateFlow<AccelerationValues?> = mutableValues.asStateFlow()
    private var packet: AccelerationParamPacket? = null

    fun reset() {
        packet = null
        mutableValues.value = null
    }

    fun receive(received: AccelerationParamPacket) {
        packet = received
        publishValues()
    }

    fun editTpsThreshold(value: Int) = edit(value in BYTE_MIN..BYTE_MAX) { it.injAeTpsdotThrd = value }
    fun editColdMultiplier(value: Int) = edit(value in COLD_MIN..COLD_MAX) { it.injAeColdaccMult = value.toFloat() }
    fun editDecayTime(value: Int) = edit(value in BYTE_MIN..BYTE_MAX) { it.injAeDecayTime = value }
    fun editAeTime(value: Int) = edit(value in BYTE_MIN..BYTE_MAX && ParameterCapabilities(firmwareInfo.value).accelerationTimeEnabled(packet?.injAeType ?: -1)) { it.injAeTime = value }
    fun editMapdotThreshold(value: Int) = edit(value in BYTE_MIN..BYTE_MAX) { it.injAeMapdotThrd = value }
    fun editXtauStart(value: Int) = edit(value in XTAU_MIN..XTAU_MAX && ParameterCapabilities(firmwareInfo.value).fuelInjection && (packet?.wallwetModel ?: 0) > 0) { it.injXtauSThrd = value.toFloat() }
    fun editXtauFinish(value: Int) = edit(value in XTAU_MIN..XTAU_MAX && ParameterCapabilities(firmwareInfo.value).fuelInjection && (packet?.wallwetModel ?: 0) > 0) { it.injXtauFThrd = value.toFloat() }
    fun setBalance(value: Float) = edit(value.isFinite() && value in 0f..100f) { it.injAeBallance = value }

    fun selectAeType(code: Int) = edit(code in 0..1) { it.injAeType = code }

    fun selectWallwetModel(code: Int): Boolean = edit(
        valid = code in 0..3 && ParameterCapabilities(firmwareInfo.value).fuelInjection,
    ) { it.wallwetModel = code }

    private inline fun edit(
        valid: Boolean,
        change: (AccelerationParamPacket) -> Unit,
    ): Boolean {
        val capabilities = ParameterCapabilities(firmwareInfo.value)
        if (!valid || !capabilities.fuelOrGasDose) return false
        val current = packet ?: return false
        val before = AccelerationFields(current)
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
        packet?.let { mutableValues.value = AccelerationValues.from(it) }
    }

    private data class AccelerationFields(
        val tps: Int, val cold: Float, val decay: Int, val aeType: Int,
        val aeTime: Int, val balance: Float, val mapdot: Int,
        val xtauStart: Float, val xtauFinish: Float, val wallwet: Int,
    ) {
        constructor(p: AccelerationParamPacket) : this(
            p.injAeTpsdotThrd, p.injAeColdaccMult, p.injAeDecayTime, p.injAeType,
            p.injAeTime, p.injAeBallance, p.injAeMapdotThrd,
            p.injXtauSThrd, p.injXtauFThrd, p.wallwetModel,
        )

        fun restore(p: AccelerationParamPacket) {
            p.injAeTpsdotThrd = tps
            p.injAeColdaccMult = cold
            p.injAeDecayTime = decay
            p.injAeType = aeType
            p.injAeTime = aeTime
            p.injAeBallance = balance
            p.injAeMapdotThrd = mapdot
            p.injXtauSThrd = xtauStart
            p.injXtauFThrd = xtauFinish
            p.wallwetModel = wallwet
        }
    }

    private companion object {
        const val BYTE_MIN = 0
        const val BYTE_MAX = 255
        const val COLD_MIN = 100
        const val COLD_MAX = 600
        const val XTAU_MIN = -255
        const val XTAU_MAX = 0
    }
}
