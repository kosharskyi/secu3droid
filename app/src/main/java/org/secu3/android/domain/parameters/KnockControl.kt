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
import org.secu3.android.models.packets.out.params.KnockParamPacket
import javax.inject.Inject

data class KnockValues(
    val windowBegin: Float,
    val windowEnd: Float,
    val retardStep: Float,
    val advanceStep: Float,
    val maxRetard: Float,
    val voltageThreshold: Float,
    val recoveryDelay: Int,
    val coolantThreshold: Float,
    val useKnockChannel: Int,
    val bpfFrequencyCode: Int,
    val integrationTimeoutCode: Int,
    val selectedChannels: Int,
) {
    fun isChannelSelected(channel: Int): Boolean = channel in 0..7 && selectedChannels and (1 shl channel) != 0

    companion object {
        fun from(packet: KnockParamPacket) = KnockValues(
            windowBegin = packet.kWndBeginAngle,
            windowEnd = packet.kWndEndAngle,
            retardStep = packet.retardStep,
            advanceStep = packet.advanceStep,
            maxRetard = packet.maxRetard,
            voltageThreshold = packet.threshold,
            recoveryDelay = packet.recoveryDelay,
            coolantThreshold = packet.knkctlThrd,
            useKnockChannel = packet.useKnockChannel,
            bpfFrequencyCode = packet.bpfFrequency,
            integrationTimeoutCode = packet.intTimeCost,
            selectedChannels = packet.selectedChanels,
        )
    }
}

/** Owns Knock packet edits and preserves raw ECU fields until that field is explicitly edited. */
@ViewModelScoped
class KnockControl internal constructor(
    private val sendPacket: (OutputPacket) -> Boolean,
) {
    @Inject
    constructor(parameterSession: ParameterSession) : this(
        sendPacket = parameterSession::sendPacket,
    )

    private val mutableValues = MutableStateFlow<KnockValues?>(null)
    val values: StateFlow<KnockValues?> = mutableValues.asStateFlow()
    private var packet: KnockParamPacket? = null

    fun reset() {
        packet = null
        mutableValues.value = null
    }

    fun receive(received: KnockParamPacket) {
        packet = received
        publishValues()
    }

    fun editWindowBegin(value: Float) = editFloat(value, -12f, 54f) { it.kWndBeginAngle = value }
    fun editWindowEnd(value: Float) = editFloat(value, -12f, 54f) { it.kWndEndAngle = value }
    fun editRetardStep(value: Float) = editFloat(value, 0f, 20f) { it.retardStep = value }
    fun editAdvanceStep(value: Float) = editFloat(value, 0f, 5f) { it.advanceStep = value }
    fun editMaxRetard(value: Float) = editFloat(value, 0f, 25f) { it.maxRetard = value }
    fun editVoltageThreshold(value: Float) = editFloat(value, .1f, 5f) { it.threshold = value }
    fun editRecoveryDelay(value: Int) = editInt(value, 1, 99) { it.recoveryDelay = value }
    fun editCoolantThreshold(value: Float) = editFloat(value, -40f, 120f) { it.knkctlThrd = value }

    fun setKnockEnabled(enabled: Boolean) = edit { it.useKnockChannel = if (enabled) 1 else 0 }

    fun selectChannel(channel: Int, checked: Boolean): Boolean {
        if (channel !in 0..7) return false
        return edit { it.selectKnockChanel(channel, checked) }
    }

    fun selectBpfFrequency(code: Int) = edit(code in 0..63) { it.bpfFrequency = code }
    fun selectIntegrationTimeout(code: Int) = edit(code in 0..31) { it.intTimeCost = code }

    private inline fun editFloat(value: Float, min: Float, max: Float, change: (KnockParamPacket) -> Unit) =
        edit(value.isFinite() && value in min..max, change)

    private inline fun editInt(value: Int, min: Int, max: Int, change: (KnockParamPacket) -> Unit) =
        edit(value in min..max, change)

    private inline fun edit(valid: Boolean = true, change: (KnockParamPacket) -> Unit): Boolean {
        if (!valid) return false
        val current = packet ?: return false
        val before = KnockFields(current)
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
        packet?.let { mutableValues.value = KnockValues.from(it) }
    }

    private data class KnockFields(
        val useKnockChannel: Int,
        val bpfFrequency: Int,
        val windowBegin: Float,
        val windowEnd: Float,
        val integrationTimeout: Int,
        val retardStep: Float,
        val advanceStep: Float,
        val maxRetard: Float,
        val threshold: Float,
        val recoveryDelay: Int,
        val selectedChannels: Int,
        val coolantThreshold: Float,
    ) {
        constructor(p: KnockParamPacket) : this(
            p.useKnockChannel, p.bpfFrequency, p.kWndBeginAngle, p.kWndEndAngle, p.intTimeCost,
            p.retardStep, p.advanceStep, p.maxRetard, p.threshold, p.recoveryDelay,
            p.selectedChanels, p.knkctlThrd,
        )

        fun restore(p: KnockParamPacket) {
            p.useKnockChannel = useKnockChannel
            p.bpfFrequency = bpfFrequency
            p.kWndBeginAngle = windowBegin
            p.kWndEndAngle = windowEnd
            p.intTimeCost = integrationTimeout
            p.retardStep = retardStep
            p.advanceStep = advanceStep
            p.maxRetard = maxRetard
            p.threshold = threshold
            p.recoveryDelay = recoveryDelay
            p.selectedChanels = selectedChannels
            p.knkctlThrd = coolantThreshold
        }
    }
}
