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
import org.secu3.android.models.packets.base.OutputPacket
import org.secu3.android.models.packets.out.params.AnglesParamPacket
import javax.inject.Inject

data class AnglesValues(
    val maxAngle: Float,
    val minAngle: Float,
    val angleCorrection: Float,
    val angleDecSpeed: Float,
    val angleIncSpeed: Float,
    val zeroAdvAngle: Int,
    val alwaysUseIgnitionMap: Boolean,
    val applyManualTimingCorrOnIdl: Boolean,
    val zeroAdvAngleWithCorr: Boolean,
    val shiftIgnitionTiming: Float,
)

/** Owns Angles packet edits, desktop-compatible value rules, and immutable command snapshots. */
@ViewModelScoped
class AnglesControl internal constructor(
    private val sendPacket: (OutputPacket) -> Boolean,
) {
    @Inject
    constructor(parameterSession: ParameterSession) : this(
        parameterSession::sendPacket,
    )

    private val mutableValues = MutableStateFlow<AnglesValues?>(null)
    val values: StateFlow<AnglesValues?> = mutableValues.asStateFlow()
    private var packet: AnglesParamPacket? = null

    fun reset() {
        packet = null
        mutableValues.value = null
    }

    fun receive(received: AnglesParamPacket) {
        packet = received
        publishValues()
    }

    fun editMaxAngle(value: Float) = editFloat(value, ANGLE_MIN, ANGLE_MAX) { it.maxAngle = value }
    fun editMinAngle(value: Float) = editFloat(value, ANGLE_MIN, ANGLE_MAX) { it.minAngle = value }
    fun editAngleCorrection(value: Float) = editFloat(value, CORRECTION_MIN, CORRECTION_MAX, packet?.zeroAdvAngle == 0) { it.angleCorrection = value }
    fun editAngleDecSpeed(value: Float) = editFloat(value, SPEED_MIN, SPEED_MAX) { it.angleDecSpeed = value }
    fun editAngleIncSpeed(value: Float) = editFloat(value, SPEED_MIN, SPEED_MAX) { it.angleIncSpeed = value }
    fun editShiftIgnitionTiming(value: Float) = editFloat(value, SHIFT_MIN, SHIFT_MAX) { it.shift_ingtim = value }

    fun setZeroAdvAngle(enabled: Boolean) = edit {
        it.zeroAdvAngle = if (enabled) 1 else 0
        if (enabled) it.zeroAdvAngleWithCorr = false
    }
    fun setAlwaysUseIgnitionMap(enabled: Boolean) = edit { it.alwaysUseIgnitionMap = enabled }
    fun setApplyManualTimingCorrOnIdl(enabled: Boolean) = edit { it.applyManualTimingCorrOnIdl = enabled }
    fun setZeroAdvAngleWithCorr(enabled: Boolean) = edit {
        it.zeroAdvAngleWithCorr = enabled
        if (enabled) it.zeroAdvAngle = 0
    }

    private inline fun editFloat(value: Float, min: Float, max: Float, extraCondition: Boolean = true, change: (AnglesParamPacket) -> Unit): Boolean =
        edit(value.isFinite() && value in min..max && extraCondition, change)

    private inline fun edit(valid: Boolean = true, change: (AnglesParamPacket) -> Unit): Boolean {
        if (!valid) return false
        val current = packet ?: return false
        val before = AnglesFields(current)
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
        mutableValues.value = AnglesValues(
            maxAngle = current.maxAngle,
            minAngle = current.minAngle,
            angleCorrection = current.angleCorrection,
            angleDecSpeed = current.angleDecSpeed,
            angleIncSpeed = current.angleIncSpeed,
            zeroAdvAngle = current.zeroAdvAngle,
            alwaysUseIgnitionMap = current.alwaysUseIgnitionMap,
            applyManualTimingCorrOnIdl = current.applyManualTimingCorrOnIdl,
            zeroAdvAngleWithCorr = current.zeroAdvAngleWithCorr,
            shiftIgnitionTiming = current.shift_ingtim,
        )
    }

    private data class AnglesFields(
        val maxAngle: Float,
        val minAngle: Float,
        val angleCorrection: Float,
        val angleDecSpeed: Float,
        val angleIncSpeed: Float,
        val zeroAdvAngle: Int,
        val igntimFlags: Int,
        val shiftIgnitionTiming: Float,
    ) {
        constructor(packet: AnglesParamPacket) : this(
            packet.maxAngle, packet.minAngle, packet.angleCorrection, packet.angleDecSpeed,
            packet.angleIncSpeed, packet.zeroAdvAngle, packet.igntimFlags, packet.shift_ingtim,
        )

        fun restore(packet: AnglesParamPacket) {
            packet.maxAngle = maxAngle
            packet.minAngle = minAngle
            packet.angleCorrection = angleCorrection
            packet.angleDecSpeed = angleDecSpeed
            packet.angleIncSpeed = angleIncSpeed
            packet.zeroAdvAngle = zeroAdvAngle
            packet.igntimFlags = igntimFlags
            packet.shift_ingtim = shiftIgnitionTiming
        }
    }

    companion object {
        const val ANGLE_MIN = -15f
        const val ANGLE_MAX = 60f
        const val CORRECTION_MIN = -30f
        const val CORRECTION_MAX = 30f
        const val SPEED_MIN = 0f
        const val SPEED_MAX = 10f
        const val SHIFT_MIN = -15f
        const val SHIFT_MAX = 55f
    }
}
