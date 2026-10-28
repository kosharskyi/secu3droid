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
import org.secu3.android.models.packets.out.params.StarterParamPacket
import javax.inject.Inject

data class StarterValues(
    val starterOff: Int,
    val smapAbandon: Int,
    val crankToRunCold: Float,
    val crankToRunHot: Float,
    val afterstartPetrol: Int,
    val afterstartGas: Int,
    val primeCold: Float,
    val primeHot: Float,
    val primeDelay: Float,
    val floodClearThreshold: Float,
    val stblCountdown: Int,
    val allowFloodClearStart: Boolean,
    val limitCrankingPw: Boolean,
)

/** Owns Starter packet edits, firmware admission, and immutable command snapshots. */
@ViewModelScoped
class StarterControl internal constructor(
    private val firmwareInfo: StateFlow<FirmwareInfoPacket?>,
    private val sendPacket: (OutputPacket) -> Boolean,
) {
    @Inject
    constructor(connectionManager: Secu3ConnectionManager, parameterSession: ParameterSession) : this(
        connectionManager.firmwareInfo,
        parameterSession::sendPacket,
    )

    private val mutableValues = MutableStateFlow<StarterValues?>(null)
    val values: StateFlow<StarterValues?> = mutableValues.asStateFlow()
    private var packet: StarterParamPacket? = null

    fun reset() {
        packet = null
        mutableValues.value = null
    }

    fun receive(received: StarterParamPacket) {
        packet = received
        publishValues()
    }

    fun editStarterOff(value: Int) = edit(value in STARTER_OFF_MIN..STARTER_OFF_MAX) { it.starterOff = value }
    fun editSmapAbandon(value: Int) = edit(value in SMAP_ABANDON_MIN..SMAP_ABANDON_MAX) { it.smapAbandon = value }
    fun editCrankToRunCold(value: Float) = editFloat(value, CRANK_TIME_MIN, CRANK_TIME_MAX, ParameterCapabilities(firmwareInfo.value).fuelInjection) { it.crankToRunTime = value }
    fun editCrankToRunHot(value: Float) = editFloat(value, CRANK_TIME_MIN, CRANK_TIME_MAX, ParameterCapabilities(firmwareInfo.value).fuelInjection) { it.injCrankToRun_time1 = value }
    fun editAfterstartPetrol(value: Int) = edit(value in AFTERSTART_MIN..AFTERSTART_MAX && ParameterCapabilities(firmwareInfo.value).fuelOrGasDose) { it.injAftstrStroke = value }
    fun editAfterstartGas(value: Int) = edit(value in AFTERSTART_MIN..AFTERSTART_MAX && ParameterCapabilities(firmwareInfo.value).fuelOrGasDose) { it.injAftStrokes1 = value }
    fun editPrimeCold(value: Float) = editFloat(value, PRIME_MIN, PRIME_MAX, ParameterCapabilities(firmwareInfo.value).fuelInjection) { it.injPrimeCold = value }
    fun editPrimeHot(value: Float) = editFloat(value, PRIME_MIN, PRIME_MAX, ParameterCapabilities(firmwareInfo.value).fuelInjection) { it.injPrimeHot = value }
    fun editPrimeDelay(value: Float) = editFloat(value, PRIME_DELAY_MIN, PRIME_DELAY_MAX, ParameterCapabilities(firmwareInfo.value).fuelInjection) { it.injPrimeDelay = value }
    fun editFloodClearThreshold(value: Float) = editFloat(value, FLOOD_CLEAR_MIN, FLOOD_CLEAR_MAX, ParameterCapabilities(firmwareInfo.value).floodClearThreshold) { it.injFloodclearTps = value }
    fun editStblCountdown(value: Int) = edit(value in STBL_MIN..STBL_MAX) { it.stblStrCnt = value }
    fun setAllowFloodClearStart(value: Boolean) = edit(ParameterCapabilities(firmwareInfo.value).floodClearThreshold) { it.allowStartOnClearFlood = value }
    fun setLimitCrankingPw(value: Boolean) = edit(ParameterCapabilities(firmwareInfo.value).fuelInjection) { it.limitMaxInjPwOnCranking = value }

    private inline fun editFloat(
        value: Float,
        min: Float,
        max: Float,
        capability: Boolean,
        change: (StarterParamPacket) -> Unit,
    ): Boolean = edit(value.isFinite() && value in min..max && capability, change)

    private inline fun edit(valid: Boolean, change: (StarterParamPacket) -> Unit): Boolean {
        if (!valid) return false
        val current = packet ?: return false
        val before = StarterFields(current)
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

    private data class StarterFields(
        val starterOff: Int,
        val smapAbandon: Int,
        val crankToRunTime: Float,
        val injAftstrStroke: Int,
        val injPrimeCold: Float,
        val injPrimeHot: Float,
        val injPrimeDelay: Float,
        val injFloodclearTps: Float,
        val injAftStrokes1: Int,
        val stblStrCnt: Int,
        val strtFlags: Int,
        val injCrankToRunTimeHot: Float,
    ) {
        constructor(packet: StarterParamPacket) : this(
            packet.starterOff, packet.smapAbandon, packet.crankToRunTime,
            packet.injAftstrStroke, packet.injPrimeCold, packet.injPrimeHot,
            packet.injPrimeDelay, packet.injFloodclearTps, packet.injAftStrokes1,
            packet.stblStrCnt, packet.strtFlags, packet.injCrankToRun_time1,
        )

        fun restore(packet: StarterParamPacket) {
            packet.starterOff = starterOff
            packet.smapAbandon = smapAbandon
            packet.crankToRunTime = crankToRunTime
            packet.injAftstrStroke = injAftstrStroke
            packet.injPrimeCold = injPrimeCold
            packet.injPrimeHot = injPrimeHot
            packet.injPrimeDelay = injPrimeDelay
            packet.injFloodclearTps = injFloodclearTps
            packet.injAftStrokes1 = injAftStrokes1
            packet.stblStrCnt = stblStrCnt
            packet.strtFlags = strtFlags
            packet.injCrankToRun_time1 = injCrankToRunTimeHot
        }
    }

    private fun publishValues() {
        val current = packet ?: return
        mutableValues.value = StarterValues(
            starterOff = current.starterOff,
            smapAbandon = current.smapAbandon,
            crankToRunCold = current.crankToRunTime,
            crankToRunHot = current.injCrankToRun_time1,
            afterstartPetrol = current.injAftstrStroke,
            afterstartGas = current.injAftStrokes1,
            primeCold = current.injPrimeCold,
            primeHot = current.injPrimeHot,
            primeDelay = current.injPrimeDelay,
            floodClearThreshold = current.injFloodclearTps,
            stblCountdown = current.stblStrCnt,
            allowFloodClearStart = current.allowStartOnClearFlood,
            limitCrankingPw = current.limitMaxInjPwOnCranking,
        )
    }

    companion object {
        const val STARTER_OFF_MIN = 0
        const val STARTER_OFF_MAX = 6000
        const val SMAP_ABANDON_MIN = 0
        const val SMAP_ABANDON_MAX = 6000
        const val CRANK_TIME_MIN = .1f
        const val CRANK_TIME_MAX = 99f
        const val AFTERSTART_MIN = 0
        const val AFTERSTART_MAX = 1020
        const val PRIME_MIN = 0f
        const val PRIME_MAX = 100f
        const val PRIME_DELAY_MIN = 0f
        const val PRIME_DELAY_MAX = 25f
        const val FLOOD_CLEAR_MIN = 0f
        const val FLOOD_CLEAR_MAX = 100f
        const val STBL_MIN = 0
        const val STBL_MAX = 255
    }
}
