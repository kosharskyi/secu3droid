/*
 *    SecuDroid  - An open source, free manager for SECU-3 engine control unit
 *    Copyright (C) 2026 Vitalii O. Kosharskyi. Ukraine, Kyiv
 */
package org.secu3.android.domain.parameters

import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.secu3.android.connection.Secu3ConnectionManager
import org.secu3.android.models.packets.base.OutputPacket
import org.secu3.android.models.packets.input.FirmwareInfoPacket
import org.secu3.android.models.packets.out.params.LtftParamPacket
import javax.inject.Inject

data class LtftValues(
    val mode: Int,
    val lowerClt: Float,
    val upperClt: Float,
    val upperIat: Float,
    val learningGradient: Float,
    val absoluteGasPressure: Float,
    val differentialGasPressure: Float,
    val bottomLimit: Float,
    val topLimit: Float,
    val lowerRpm: Int,
    val upperRpm: Int,
    val lowerLoad: Float,
    val upperLoad: Float,
    val negativeInsensitivity: Float,
    val positiveInsensitivity: Float,
) {
    companion object {
        fun from(packet: LtftParamPacket) = LtftValues(
            packet.mode, packet.learnClt, packet.learnCltUp, packet.learnIatUp,
            packet.learnGrad, packet.learnGpa, packet.learnGpd, packet.min,
            packet.max, packet.learnRpm0, packet.learnRpm1, packet.learnLoad0,
            packet.learnLoad1, packet.deadBand0, packet.deadBand1,
        )
    }
}

/** Owns LTFT edits while retaining raw ECU values and packet tail bytes. */
@ViewModelScoped
class LtftControl internal constructor(
    private val firmware: () -> FirmwareInfoPacket?,
    private val sendPacket: (OutputPacket) -> Boolean,
) {
    @Inject
    constructor(connectionManager: Secu3ConnectionManager, parameterSession: ParameterSession) : this(
        firmware = { connectionManager.fwInfo },
        sendPacket = parameterSession::sendPacket,
    )

    private val mutableValues = MutableStateFlow<LtftValues?>(null)
    val values: StateFlow<LtftValues?> = mutableValues.asStateFlow()
    private var packet: LtftParamPacket? = null

    fun reset() {
        packet = null
        mutableValues.value = null
    }

    fun receive(received: LtftParamPacket) {
        packet = received
        publishValues()
    }

    fun editLowerClt(value: Float) = editFloat(value, 0f, 270f) { it.learnClt = value }
    fun editUpperClt(value: Float) = editFloat(value, 0f, 270f) { it.learnCltUp = value }
    fun editUpperIat(value: Float) = editFloat(value, 0f, 270f) { it.learnIatUp = value }
    fun editLearningGradient(value: Float) = editFloat(value, 0f, .99f) { it.learnGrad = value }
    fun editAbsoluteGasPressure(value: Float) = editFloat(value, 0f, 500f) { it.learnGpa = value }
    fun editDifferentialGasPressure(value: Float) = editFloat(value, 0f, 500f) { it.learnGpd = value }
    fun editBottomLimit(value: Float) = editFloat(value, -24.6f, 0f) { it.min = value }
    fun editTopLimit(value: Float) = editFloat(value, 0f, 24.6f) { it.max = value }
    fun editLowerRpm(value: Int) = editInt(value, 0, 25000) { it.learnRpm0 = value }
    fun editUpperRpm(value: Int) = editInt(value, 0, 25000) { it.learnRpm1 = value }
    fun editLowerLoad(value: Float) = editFloat(value, 0f, 500f) { it.learnLoad0 = value }
    fun editUpperLoad(value: Float) = editFloat(value, 0f, 500f) { it.learnLoad1 = value }
    fun editNegativeInsensitivity(value: Float) = editFloat(value, 0f, 10f) { it.deadBand0 = value }
    fun editPositiveInsensitivity(value: Float) = editFloat(value, 0f, 10f) { it.deadBand1 = value }
    fun selectMode(code: Int) = edit(code in 0..3) { it.mode = code }

    private inline fun editFloat(value: Float, min: Float, max: Float, change: (LtftParamPacket) -> Unit) =
        edit(value.isFinite() && value in min..max, change)

    private inline fun editInt(value: Int, min: Int, max: Int, change: (LtftParamPacket) -> Unit) =
        edit(value in min..max, change)

    private inline fun edit(valid: Boolean, change: (LtftParamPacket) -> Unit): Boolean {
        if (!valid || !ParameterCapabilities(firmware()).fuelInjection) return false
        val current = packet ?: return false
        val before = LtftFields(current)
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
        packet?.let { mutableValues.value = LtftValues.from(it) }
    }

    private data class LtftFields(
        val mode: Int, val lowerClt: Float, val upperClt: Float, val upperIat: Float,
        val gradient: Float, val absolutePressure: Float, val differentialPressure: Float,
        val bottom: Float, val top: Float, val lowerRpm: Int, val upperRpm: Int,
        val lowerLoad: Float, val upperLoad: Float, val negative: Float, val positive: Float,
    ) {
        constructor(p: LtftParamPacket) : this(
            p.mode, p.learnClt, p.learnCltUp, p.learnIatUp, p.learnGrad, p.learnGpa,
            p.learnGpd, p.min, p.max, p.learnRpm0, p.learnRpm1, p.learnLoad0,
            p.learnLoad1, p.deadBand0, p.deadBand1,
        )

        fun restore(p: LtftParamPacket) {
            p.mode = mode; p.learnClt = lowerClt; p.learnCltUp = upperClt; p.learnIatUp = upperIat
            p.learnGrad = gradient; p.learnGpa = absolutePressure; p.learnGpd = differentialPressure
            p.min = bottom; p.max = top; p.learnRpm0 = lowerRpm; p.learnRpm1 = upperRpm
            p.learnLoad0 = lowerLoad; p.learnLoad1 = upperLoad; p.deadBand0 = negative; p.deadBand1 = positive
        }
    }
}
