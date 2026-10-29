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
import org.secu3.android.models.FnName
import org.secu3.android.models.packets.base.OutputPacket
import org.secu3.android.models.packets.input.FirmwareInfoPacket
import org.secu3.android.models.packets.out.params.FunSetParamPacket
import javax.inject.Inject

data class FunctionsValues(
    val fnGasoline: Int,
    val fnGas: Int,
    val loadLower: Float,
    val loadUpper: Float,
    val mapCurveOffset: Float,
    val mapCurveGradient: Float,
    val tpsCurveOffset: Float,
    val tpsCurveGradient: Float,
    val map2CurveOffset: Float,
    val map2CurveGradient: Float,
    val gpsCurveOffset: Float,
    val gpsCurveGradient: Float,
    val fpsCurveOffset: Float,
    val fpsCurveGradient: Float,
    val apps1CurveOffset: Float,
    val apps1CurveGradient: Float,
    val loadSource: Int,
    val mapselPetrol: Int,
    val mapselGas: Int,
    val barocorrType: Int,
    val useLoadGrid: Boolean,
    val ve2MapFunc: Int,
    val gasVUni: Int,
    val functionNames: List<String>,
    val engineCylinderCount: Int,
    val engineDisplacementPerCylinder: Float,
    val mafLoadConstant: Float,
) {
    val engineDisplacement get() = engineDisplacementPerCylinder * engineCylinderCount

    companion object {
        fun from(packet: FunSetParamPacket, names: List<String>) = FunctionsValues(
            fnGasoline = packet.fnGasoline,
            fnGas = packet.fnGas,
            loadLower = packet.loadLower,
            loadUpper = packet.loadUpper,
            mapCurveOffset = packet.mapCurveOffset,
            mapCurveGradient = packet.mapCurveGradient,
            tpsCurveOffset = packet.tpsCurveOffset,
            tpsCurveGradient = packet.tpsCurveGradient,
            map2CurveOffset = packet.map2CurveOffset,
            map2CurveGradient = packet.map2CurveGradient,
            gpsCurveOffset = packet.gpsCurveOffset,
            gpsCurveGradient = packet.gpsCurveGradient,
            fpsCurveOffset = packet.fpsCurveOffset,
            fpsCurveGradient = packet.fpsCurveGradient,
            apps1CurveOffset = packet.apps1CurveOffset,
            apps1CurveGradient = packet.apps1CurveGradient,
            loadSource = packet.loadSrcCfg,
            mapselPetrol = packet.mapselUniPetrol,
            mapselGas = packet.mapselUniGas,
            barocorrType = packet.barocorrType,
            useLoadGrid = packet.useLoadGrid,
            ve2MapFunc = packet.ve2MapFunc,
            gasVUni = packet.gasVUni,
            functionNames = names.toList(),
            engineCylinderCount = packet.ckpsEngineCyl,
            engineDisplacementPerCylinder = packet.injCylDisp,
            mafLoadConstant = packet.mafload_const,
        )
    }
}

/** Owns live Functions commands while retaining read-only packet fields and unknown bytes. */
@ViewModelScoped
class FunctionsControl internal constructor(
    private val firmware: () -> FirmwareInfoPacket?,
    private val sendPacket: (OutputPacket) -> Boolean,
) {
    @Inject
    constructor(connectionManager: Secu3ConnectionManager, parameterSession: ParameterSession) : this(
        firmware = { connectionManager.fwInfo },
        sendPacket = parameterSession::sendPacket,
    )

    private val mutableValues = MutableStateFlow<FunctionsValues?>(null)
    val values: StateFlow<FunctionsValues?> = mutableValues.asStateFlow()
    private var packet: FunSetParamPacket? = null
    private var names: List<String> = emptyList()

    fun reset() {
        packet = null
        names = emptyList()
        mutableValues.value = null
    }

    fun receive(parameters: FunctionParameters) {
        this.names = parameters.names.fnNameList.sortedBy(FnName::index).map(FnName::name)
        packet = parameters.settings
        publishValues()
    }

    fun editLoadLower(value: Float) = editFloat(value, .25f, 105f,
        { ParameterCapabilities(firmware()).functionsLowerLoadEnabled(it.useLoadGrid) }) { it.loadLower = value }

    fun editLoadUpper(value: Float) = editFloat(value, .25f, 500f,
        { ParameterCapabilities(firmware()).functionsUpperLoadEnabled(it.useLoadGrid, it.loadSrcCfg) }) { it.loadUpper = value }

    fun editMapCurveOffset(value: Float) = editFloat(value, -5f, 5f) { it.mapCurveOffset = value }
    fun editMapCurveGradient(value: Float) = editFloat(value, -150f, 150f) { it.mapCurveGradient = value }
    fun editTpsCurveOffset(value: Float) = editFloat(value, -5f, 5f) { it.tpsCurveOffset = value }
    fun editTpsCurveGradient(value: Float) = editFloat(value, -100f, 100f) { it.tpsCurveGradient = value }
    fun editMap2CurveOffset(value: Float) = editFloat(value, -5f, 5f) { it.map2CurveOffset = value }
    fun editMap2CurveGradient(value: Float) = editFloat(value, -150f, 150f) { it.map2CurveGradient = value }
    fun editGpsCurveOffset(value: Float) = editFloat(value, -5f, 5f, { ParameterCapabilities(firmware()).dbw }) { it.gpsCurveOffset = value }
    fun editGpsCurveGradient(value: Float) = editFloat(value, -150f, 150f, { ParameterCapabilities(firmware()).dbw }) { it.gpsCurveGradient = value }
    fun editFpsCurveOffset(value: Float) = editFloat(value, -5f, 5f, { ParameterCapabilities(firmware()).dbw }) { it.fpsCurveOffset = value }
    fun editFpsCurveGradient(value: Float) = editFloat(value, -500f, 500f, { ParameterCapabilities(firmware()).dbw }) { it.fpsCurveGradient = value }
    fun editApps1CurveOffset(value: Float) = editFloat(value, -5f, 5f, { ParameterCapabilities(firmware()).dbw }) { it.apps1CurveOffset = value }
    fun editApps1CurveGradient(value: Float) = editFloat(value, -500f, 500f, { ParameterCapabilities(firmware()).dbw }) { it.apps1CurveGradient = value }

    fun setUseLoadGrid(checked: Boolean) = edit(change = { it.useLoadGrid = checked })

    fun selectGasolineMap(index: Int) = edit({ index in names.indices }) { it.fnGasoline = index }
    fun selectGasMap(index: Int) = edit({ index in names.indices }) { it.fnGas = index }
    fun selectLoadSource(code: Int) = edit({ code in 0..4 }) { it.loadSrcCfg = code }
    fun selectMapsForPetrol(code: Int) = edit({ isOutputCode(code) }) { it.mapselUniPetrol = code }
    fun selectMapsForGas(code: Int) = edit({ isOutputCode(code) }) { it.mapselUniGas = code }
    fun selectBarometricCorrection(code: Int) = edit({ code in 0..3 }) { it.barocorrType = code }
    fun selectVe2Function(code: Int) = edit({ code in 0..2 && ParameterCapabilities(firmware()).fuelInjection }) { it.ve2MapFunc = code }
    fun selectGasVoltageInput(code: Int) = edit({ isOutputCode(code) }) { it.gasVUni = code }

    fun engineDisplacementForMafEditor(): Float? {
        val current = packet ?: return null
        if (current.loadSrcCfg != MAF_LOAD_SOURCE || ParameterCapabilities(firmware()).fuelInjection || current.ckpsEngineCyl <= 0) return null
        return current.injCylDisp * current.ckpsEngineCyl
    }

    fun updateMafEngineDisplacement(value: Float): Boolean {
        val current = packet ?: return false
        val firmwareSupportsFuelInjection = ParameterCapabilities(firmware()).fuelInjection
        if (current.loadSrcCfg != MAF_LOAD_SOURCE || firmwareSupportsFuelInjection || current.ckpsEngineCyl <= 0 ||
            !value.isFinite() || value !in .01f..8f
        ) return false
        return edit(change = {
            it.injCylDisp = value / it.ckpsEngineCyl
            it.mafload_const = calculateMafLoadConstant(it.injCylDisp, it.ckpsEngineCyl)
        })
    }

    private fun editFloat(
        value: Float,
        min: Float,
        max: Float,
        supported: (FunSetParamPacket) -> Boolean = { true },
        change: (FunSetParamPacket) -> Unit,
    ) = edit({ value.isFinite() && value in min..max && supported(it) }, change)

    private inline fun edit(
        crossinline valid: (FunSetParamPacket) -> Boolean = { true },
        crossinline change: (FunSetParamPacket) -> Unit,
    ): Boolean {
        val current = packet ?: return false
        if (!valid(current)) return false
        val before = Fields(current)
        val admitted = try {
            change(current)
            val bytes = current.pack().copyOf()
            sendPacket(object : OutputPacket {
                override fun pack() = bytes.copyOf()
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
        packet?.let { mutableValues.value = FunctionsValues.from(it, names) }
    }

    private fun isOutputCode(code: Int) = code in 0..5 || code == DISABLED_OUTPUT

    private fun calculateMafLoadConstant(cylinderDisplacement: Float, cylinderCount: Int): Float =
        (((101.35 * 64.0 * 128.0 * 120.0) / (64.0 * 0.0012041)) /
            (cylinderDisplacement.toDouble() * 1000.0 * cylinderCount)).toFloat()

    private data class Fields(
        val values: FunctionsValues,
        val mapsel: Int,
        val flags: Int,
    ) {
        constructor(packet: FunSetParamPacket) : this(
            FunctionsValues.from(packet, emptyList()), packet.mapselUni, packet.funcFlags,
        )

        fun restore(packet: FunSetParamPacket) {
            packet.fnGasoline = values.fnGasoline
            packet.fnGas = values.fnGas
            packet.loadLower = values.loadLower
            packet.loadUpper = values.loadUpper
            packet.mapCurveOffset = values.mapCurveOffset
            packet.mapCurveGradient = values.mapCurveGradient
            packet.tpsCurveOffset = values.tpsCurveOffset
            packet.tpsCurveGradient = values.tpsCurveGradient
            packet.map2CurveOffset = values.map2CurveOffset
            packet.map2CurveGradient = values.map2CurveGradient
            packet.gpsCurveOffset = values.gpsCurveOffset
            packet.gpsCurveGradient = values.gpsCurveGradient
            packet.fpsCurveOffset = values.fpsCurveOffset
            packet.fpsCurveGradient = values.fpsCurveGradient
            packet.apps1CurveOffset = values.apps1CurveOffset
            packet.apps1CurveGradient = values.apps1CurveGradient
            packet.loadSrcCfg = values.loadSource
            packet.mapselUni = mapsel
            packet.barocorrType = values.barocorrType
            packet.funcFlags = flags
            packet.ve2MapFunc = values.ve2MapFunc
            packet.gasVUni = values.gasVUni
            packet.ckpsEngineCyl = values.engineCylinderCount
            packet.injCylDisp = values.engineDisplacementPerCylinder
            packet.mafload_const = values.mafLoadConstant
        }
    }

    companion object {
        private const val MAF_LOAD_SOURCE = 4
        private const val DISABLED_OUTPUT = 15
    }
}
