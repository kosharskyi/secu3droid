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
import org.secu3.android.models.packets.out.params.LambdaParamPacket
import javax.inject.Inject

data class LambdaValues(
    val strokesPerStep: Int,
    val msPerStep: Int,
    val positiveStep: Float,
    val negativeStep: Float,
    val positiveLimit: Float,
    val negativeLimit: Float,
    val switchPoint: Float,
    val coolantThreshold: Float,
    val rpmThreshold: Int,
    val activationDelay: Int,
    val deadband: Float,
    val sensorTypeCode: Int,
    val stoichiometricRatio: Float,
    val coldHeatingTime: Int,
    val hotHeatingTime: Int,
    val heatingTemperature: Int,
    val heatingPwmTime: Float,
    val airFlowThreshold: Float,
    val heatingVoltage: Boolean,
    val idlingCorrection: Boolean,
    val beforeCranking: Boolean,
    val mixSensors: Boolean,
    val selectedChannels: List<Boolean>,
)

/** Owns Lambda packet edits, firmware admission, rollback, and immutable parameter writes. */
@ViewModelScoped
class LambdaControl internal constructor(
    private val firmwareInfo: kotlinx.coroutines.flow.StateFlow<FirmwareInfoPacket?>,
    private val sendPacket: (OutputPacket) -> Boolean,
) {
    @Inject constructor(connectionManager: Secu3ConnectionManager, parameterSession: ParameterSession) : this(
        connectionManager.firmwareInfo,
        parameterSession::sendPacket,
    )

    private var packet: LambdaParamPacket? = null
    private val mutableValues = MutableStateFlow<LambdaValues?>(null)
    val values: StateFlow<LambdaValues?> = mutableValues.asStateFlow()

    fun reset() { packet = null; mutableValues.value = null }
    fun receive(received: LambdaParamPacket) { packet = received; publish() }

    fun editStrokesPerStep(value: Int) = edit(value in 0..255) { it.strPerStp = value }
    fun editMsPerStep(value: Int) = edit(value in 0..2500 && ParameterCapabilities(firmwareInfo.value).lambdaMsEditing(packet?.strPerStp ?: -1)) {
        it.msPerStp = (value / 10) * 10
    }
    fun editPositiveStep(value: Float) = edit(value in 0f..45f) { it.stepSizeP = value }
    fun editNegativeStep(value: Float) = edit(value in 0f..45f) { it.stepSizeM = value }
    fun editPositiveLimit(value: Float) = edit(value in .5f..100f) { it.corrLimitP = value }
    fun editNegativeLimit(value: Float) = edit(value in .5f..100f) { it.corrLimitM = value }
    fun editSwitchPoint(value: Float) = edit(value in .1f..5f) { it.swtPoint = value }
    fun editCoolantThreshold(value: Float) = edit(value in -40f..300f) { it.tempThrd = value }
    fun editRpmThreshold(value: Int) = edit(value in 0..30000) { it.rpmThrd = value }
    fun editActivationDelay(value: Int) = edit(value in 0..255) { it.activDelay = value }
    fun editDeadband(value: Float) = edit(value in 0f..5f) { it.deadBand = value }
    fun editStoichiometricRatio(value: Float) = edit(value in 8f..22f && ParameterCapabilities(firmwareInfo.value).lambdaStoich) { it.gdStoichval = value }
    fun editColdHeatingTime(value: Int) = edit(value in 0..255) { it.heatingTime0 = value }
    fun editHotHeatingTime(value: Int) = edit(value in 0..255) { it.heatingTime1 = value }
    fun editHeatingTemperature(value: Int) = edit(value in 0..255) { it.temperThrd = value }
    fun editHeatingPwmTime(value: Float) = edit(value in 0f..2.55f) { it.heatingAct = value }
    fun editAirFlowThreshold(value: Int) = edit(value in 100..2_000_000) { it.aflowThrd = value.toFloat() }

    fun setHeatingVoltage(enabled: Boolean) = edit { it.determineLambdaHeatingByVoltage = enabled }
    fun setIdlingCorrection(enabled: Boolean) = edit { it.lambdaCorrectionOnIdling = enabled }
    fun setBeforeCranking(enabled: Boolean) = edit { it.heatingBeforeCranking = enabled }
    fun setMixSensors(enabled: Boolean) = edit { it.mixSenorsValue = enabled }
    fun selectSensorType(code: Int) = edit(code == 0 || code == 1) { it.senstype = code }
    fun setChannelSelected(index: Int, selected: Boolean) = edit(index in 0..7 && ParameterCapabilities(firmwareInfo.value).lambdaChannelSelection) {
        val bit = 1 shl index
        it.lambdaSelectedChanel = if (selected) it.lambdaSelectedChanel or bit else it.lambdaSelectedChanel and bit.inv()
    }

    private inline fun edit(valid: Boolean = true, change: (LambdaParamPacket) -> Unit): Boolean {
        val current = packet ?: return false
        val capabilities = ParameterCapabilities(firmwareInfo.value)
        if (!valid || !capabilities.lambdaBaseEditing) return false
        val before = LambdaFields(current)
        val admitted = try {
            change(current)
            val bytes = current.pack().copyOf()
            sendPacket(object : OutputPacket { override fun pack(): IntArray = bytes.copyOf() })
        } catch (_: IllegalArgumentException) { false }
        if (!admitted) { before.restore(current); return false }
        publish()
        return true
    }

    private fun publish() {
        val p = packet ?: return
        mutableValues.value = LambdaValues(
            p.strPerStp, p.msPerStp, p.stepSizeP, p.stepSizeM, p.corrLimitP, p.corrLimitM,
            p.swtPoint, p.tempThrd, p.rpmThrd, p.activDelay, p.deadBand, p.senstype, p.gdStoichval,
            p.heatingTime0, p.heatingTime1, p.temperThrd, p.heatingAct, p.aflowThrd,
            p.determineLambdaHeatingByVoltage, p.lambdaCorrectionOnIdling, p.heatingBeforeCranking, p.mixSenorsValue,
            (0..7).map { p.lambdaSelectedChanel and (1 shl it) != 0 },
        )
    }

    private data class LambdaFields(
        val strokes: Int, val stepP: Float, val stepM: Float, val limitP: Float, val limitM: Float,
        val switch: Float, val coolant: Float, val rpm: Int, val delay: Int, val deadband: Float,
        val sensor: Int, val ms: Int, val flags: Int, val stoich: Float, val cold: Int, val hot: Int,
        val temperature: Int, val pwm: Float, val airFlow: Float, val channels: Int,
    ) {
        constructor(p: LambdaParamPacket) : this(p.strPerStp, p.stepSizeP, p.stepSizeM, p.corrLimitP, p.corrLimitM,
            p.swtPoint, p.tempThrd, p.rpmThrd, p.activDelay, p.deadBand, p.senstype, p.msPerStp, p.flags,
            p.gdStoichval, p.heatingTime0, p.heatingTime1, p.temperThrd, p.heatingAct, p.aflowThrd, p.lambdaSelectedChanel)
        fun restore(p: LambdaParamPacket) {
            p.strPerStp = strokes; p.stepSizeP = stepP; p.stepSizeM = stepM; p.corrLimitP = limitP; p.corrLimitM = limitM
            p.swtPoint = switch; p.tempThrd = coolant; p.rpmThrd = rpm; p.activDelay = delay; p.deadBand = deadband
            p.senstype = sensor; p.msPerStp = ms; p.flags = flags; p.gdStoichval = stoich; p.heatingTime0 = cold
            p.heatingTime1 = hot; p.temperThrd = temperature; p.heatingAct = pwm; p.aflowThrd = airFlow
            p.lambdaSelectedChanel = channels
        }
    }
}
