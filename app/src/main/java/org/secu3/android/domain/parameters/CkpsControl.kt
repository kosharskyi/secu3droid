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
import org.secu3.android.models.packets.out.params.CkpsParamPacket
import javax.inject.Inject
import kotlin.math.ceil

data class CkpsValues(
    val wheel: Int,
    val missingTeeth: Int,
    val ignitionPulseCount: Int,
    val hallWindowWidth: Float,
    val hallDegreesBeforeTdc: Float,
    val mttf: Float,
    val ckpsEdge: Boolean,
    val referenceEdge: Boolean,
    val risingSpark: Boolean,
    val mergeOutputs: Boolean,
    val camReference: Boolean,
    val teethBeforeTdc: Int,
    val cylinders: Int,
    val teethBeforeTdcOptions: List<Int>,
)

/** Owns CKPS packet edits, firmware admission, geometry, and immutable commands. */
@ViewModelScoped
class CkpsControl internal constructor(
    private val firmwareInfo: StateFlow<FirmwareInfoPacket?>,
    private val sendPacket: (OutputPacket) -> Boolean,
) {
    @Inject
    constructor(connectionManager: Secu3ConnectionManager, parameterSession: ParameterSession) : this(
        connectionManager.firmwareInfo,
        parameterSession::sendPacket,
    )

    private val mutableValues = MutableStateFlow<CkpsValues?>(null)
    val values: StateFlow<CkpsValues?> = mutableValues.asStateFlow()
    private var packet: CkpsParamPacket? = null

    fun reset() {
        packet = null
        mutableValues.value = null
    }

    fun receive(received: CkpsParamPacket) {
        packet = received
        publishValues()
    }

    fun editWheel(value: Float): Boolean {
        if (!value.isFinite() || value % 1f != 0f || value !in WHEEL_MIN..WHEEL_MAX || !ParameterCapabilities(firmwareInfo.value).ckps) return false
        val current = packet ?: return false
        current.ckpsCogsNum = value.toInt()
        return sendOrdinary(current)
    }

    fun editMissingTeeth(value: Float): Boolean {
        if (!value.isFinite() || value % 1f != 0f || value !in MISSING_MIN..MISSING_MAX) return false
        val current = packet ?: return false
        if (!ParameterCapabilities(firmwareInfo.value).ckps) return false
        current.ckpsMissNum = value.toInt()
        if (current.ckpsMissNum > 0) current.mttf = MTTF_BY_MISSING_TEETH[current.ckpsMissNum]
        return sendOrdinary(current)
    }

    fun editIgnitionPulseCount(value: Float): Boolean {
        if (!value.isFinite() || value % 1f != 0f || value !in PULSE_MIN..PULSE_MAX || !ParameterCapabilities(firmwareInfo.value).ignitionCogs) return false
        val current = packet ?: return false
        current.ckpsIgnitCogs = value.toInt()
        return sendOrdinary(current)
    }

    fun editHallWindowWidth(value: Float): Boolean {
        if (!value.isFinite() || !ParameterCapabilities(firmwareInfo.value).hallWindow) return false
        val current = packet ?: return false
        current.hallWndWidth = value
        return sendOrdinary(current)
    }

    fun editHallDegreesBeforeTdc(value: Float): Boolean {
        if (!value.isFinite() || !ParameterCapabilities(firmwareInfo.value).hallWindow) return false
        val current = packet ?: return false
        current.hallDegreesBtdc = value
        return sendOrdinary(current)
    }

    fun editMttf(value: Float): Boolean {
        if (!value.isFinite()) return false
        val current = packet ?: return false
        if (!ParameterCapabilities(firmwareInfo.value).mttfEnabled(current.ckpsMissNum.toFloat())) return false
        current.mttf = value
        return sendOrdinary(current)
    }

    fun setCkpsEdge(value: Boolean) = edit { it.ckpsEdge = value }
    fun setReferenceEdge(value: Boolean) = edit { it.refsEdge = value }

    fun setRisingSpark(value: Boolean): Boolean {
        if (!ParameterCapabilities(firmwareInfo.value).risingSpark) return false
        return edit { it.risingSpark = value }
    }

    fun setMergeOutputs(value: Boolean): Boolean {
        if (!ParameterCapabilities(firmwareInfo.value).inputsMerging) return false
        return edit { it.mergeOuts = value }
    }

    fun setCamReference(value: Boolean): Boolean {
        val current = packet ?: return false
        if (!ParameterCapabilities(firmwareInfo.value).camReferenceEnabled(current.ckpsMissNum.toFloat())) return false
        return edit { it.useCamRef = value }
    }

    fun selectTeethBeforeTdc(value: Int): Boolean {
        val current = packet ?: return false
        if (!ParameterCapabilities(firmwareInfo.value).cogsBtdc || value !in teethOptions(current.ckpsCogsNum, current.ckpsEngineCyl)) return false
        current.ckpsCogsBtdc = value
        return sendOrdinary(current)
    }

    fun selectCylinders(value: Int): Boolean {
        val current = packet ?: return false
        val capabilities = ParameterCapabilities(firmwareInfo.value)
        if (value !in capabilities.ckpsCylinderOptions(current.mergeOuts)) return false
        current.ckpsEngineCyl = value
        return sendOrdinary(current)
    }

    private fun edit(apply: (CkpsParamPacket) -> Unit): Boolean {
        val current = packet ?: return false
        apply(current)
        return sendOrdinary(current)
    }

    private fun sendOrdinary(current: CkpsParamPacket): Boolean {
        publishValues()
        return sendPacket(current.snapshot())
    }

    private fun publishValues() {
        val current = packet ?: return
        mutableValues.value = CkpsValues(
            wheel = current.ckpsCogsNum,
            missingTeeth = current.ckpsMissNum,
            ignitionPulseCount = current.ckpsIgnitCogs,
            hallWindowWidth = current.hallWndWidth,
            hallDegreesBeforeTdc = current.hallDegreesBtdc,
            mttf = current.mttf,
            ckpsEdge = current.ckpsEdge,
            referenceEdge = current.refsEdge,
            risingSpark = current.risingSpark,
            mergeOutputs = current.mergeOuts,
            camReference = current.useCamRef,
            teethBeforeTdc = current.ckpsCogsBtdc,
            cylinders = current.ckpsEngineCyl,
            teethBeforeTdcOptions = teethOptions(current.ckpsCogsNum, current.ckpsEngineCyl),
        )
    }

    private fun teethOptions(wheel: Int, cylinders: Int): List<Int> {
        if (wheel <= 0 || cylinders !in 1..8) return emptyList()
        val degrees = when (cylinders) { 1, 2 -> 540.0; 3, 4, 5 -> 330.0; else -> 270.0 }
        val lower = ceil(66.0 * wheel / 360.0).toInt()
        val upper = ceil(degrees * wheel / 360.0).toInt().coerceAtMost(255)
        return (lower..upper).toList()
    }

    companion object {
        const val WHEEL_MIN = 10f
        const val WHEEL_MAX = 200f
        const val MISSING_MIN = 0f
        const val MISSING_MAX = 6f
        const val PULSE_MIN = 1f
        const val PULSE_MAX = 200f
        const val HALL_WIDTH_MIN = 0f
        const val HALL_WIDTH_MAX = 120f
        const val DEGREES_MIN = 30f
        const val DEGREES_MAX = 120f
        const val MTTF_MIN = 1f
        const val MTTF_MAX = 8f
        private val MTTF_BY_MISSING_TEETH = listOf(0f, 1.5f, 2.5f, 3f, 4f, 5f, 6f)
    }
}
