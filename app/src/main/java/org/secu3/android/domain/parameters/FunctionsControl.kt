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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.secu3.android.connection.Secu3ConnectionManager
import org.secu3.android.models.FnName
import org.secu3.android.models.packets.base.OutputPacket
import org.secu3.android.models.packets.input.FirmwareInfoPacket
import org.secu3.android.models.packets.out.params.FunSetParamPacket
import javax.inject.Inject
import kotlin.math.abs

enum class PressureSensor(val maxPressureKpa: Float) { MAP(500f), MAP2(500f), GPS(500f), FPS(1000f) }
enum class PressureUnit { PA, KPA, BAR, MMHG, TECHNICAL_ATM, PSI }
enum class VoltageUnit { VOLTS, MILLIVOLTS }

data class PressureCurveInput(
    val knownGradient: Boolean,
    val pressure1: String,
    val pressureUnit1: PressureUnit,
    val voltage1: String,
    val voltageUnit1: VoltageUnit,
    val gradient: String,
    val gradientPressureUnit: PressureUnit,
    val gradientVoltageUnit: VoltageUnit,
    val pressure2: String,
    val pressureUnit2: PressureUnit,
    val voltage2: String,
    val voltageUnit2: VoltageUnit,
    val gradientReciprocal: Boolean = false,
)

data class PressureCurvePreview(val offsetVolts: Float, val gradientKpaPerVolt: Float)

internal sealed interface TpsLearningState {
    enum class Capture { Ready, Measuring, Retry }
    enum class Error { SpanTooSmall, OutsidePacketRange }

    data class Measure(
        val openVoltage: Float? = null,
        val capture: Capture = Capture.Ready,
    ) : TpsLearningState

    data class Review(
        val openVoltage: Float,
        val closedVoltage: Float,
        val offsetVolts: Float,
        val gradientPercentPerVolt: Float,
    ) : TpsLearningState

    data class Invalid(val reason: Error) : TpsLearningState
}

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
    private val isFunctionsPageActive: () -> Boolean = { false },
    private val readTpsVoltage: suspend () -> Float? = { null },
    private val sendPacket: (OutputPacket) -> Boolean,
) {
    @Inject
    constructor(connectionManager: Secu3ConnectionManager, parameterSession: ParameterSession) : this(
        firmware = { connectionManager.fwInfo },
        isFunctionsPageActive = parameterSession::isFunctionsPageActive,
        readTpsVoltage = parameterSession::readTpsVoltage,
        sendPacket = parameterSession::sendPacket,
    )

    private val mutableValues = MutableStateFlow<FunctionsValues?>(null)
    val values: StateFlow<FunctionsValues?> = mutableValues.asStateFlow()
    private val mutableTpsLearning = MutableStateFlow<TpsLearningState?>(null)
    internal val tpsLearning = mutableTpsLearning.asStateFlow()
    private var tpsCaptureJob: Job? = null
    private var packet: FunSetParamPacket? = null
    private var names: List<String> = emptyList()

    fun reset() {
        cancelTpsLearning()
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

    internal fun startTpsLearning() {
        if (packet == null || !isFunctionsPageActive()) return
        val current = mutableTpsLearning.value
        if (current == null || current is TpsLearningState.Invalid) {
            mutableTpsLearning.value = TpsLearningState.Measure()
        }
    }

    internal fun captureTpsPosition(scope: CoroutineScope) {
        val step = mutableTpsLearning.value as? TpsLearningState.Measure ?: return
        if (step.capture == TpsLearningState.Capture.Measuring || !isFunctionsPageActive()) return
        mutableTpsLearning.value = step.copy(capture = TpsLearningState.Capture.Measuring)
        tpsCaptureJob = scope.launch {
            delay(TPS_SETTLE_MILLIS)
            val voltage = try {
                readTpsVoltage()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                null
            }
            currentCoroutineContext().ensureActive()
            if (!isFunctionsPageActive()) {
                mutableTpsLearning.value = null
                return@launch
            }
            mutableTpsLearning.value = when {
                voltage == null -> step.copy(capture = TpsLearningState.Capture.Retry)
                step.openVoltage == null -> TpsLearningState.Measure(openVoltage = voltage)
                else -> calculateTpsLearning(step.openVoltage, voltage)
            }
        }
    }

    internal fun cancelTpsLearning() {
        tpsCaptureJob?.cancel()
        tpsCaptureJob = null
        mutableTpsLearning.value = null
    }

    internal fun applyTpsLearning(): Boolean {
        val result = mutableTpsLearning.value as? TpsLearningState.Review ?: return false
        val admitted = edit({ isFunctionsPageActive() && it.canEncodeTpsCurve(result.offsetVolts, result.gradientPercentPerVolt) }) {
            it.tpsCurveOffset = result.offsetVolts
            it.tpsCurveGradient = result.gradientPercentPerVolt
        }
        if (admitted) cancelTpsLearning()
        return admitted
    }

    private fun calculateTpsLearning(openVolts: Float, closedVolts: Float): TpsLearningState {
        val difference = openVolts - closedVolts
        if (abs(difference) <= 0.1f) return TpsLearningState.Invalid(TpsLearningState.Error.SpanTooSmall)
        val offset = -closedVolts
        val gradient = 100f / difference
        if (packet?.canEncodeTpsCurve(offset, gradient) != true) {
            return TpsLearningState.Invalid(TpsLearningState.Error.OutsidePacketRange)
        }
        return TpsLearningState.Review(openVolts, closedVolts, offset, gradient)
    }

    fun pressureCurveInput(sensor: PressureSensor): PressureCurveInput? {
        val current = packet ?: return null
        if (!pressureSensorSupported(sensor)) return null
        val (offset, gradient) = when (sensor) {
            PressureSensor.MAP -> current.mapCurveOffset to current.mapCurveGradient
            PressureSensor.MAP2 -> current.map2CurveOffset to current.map2CurveGradient
            PressureSensor.GPS -> current.gpsCurveOffset to current.gpsCurveGradient
            PressureSensor.FPS -> current.fpsCurveOffset to current.fpsCurveGradient
        }
        val voltage = 1f
        val pressure = ((voltage + offset) * gradient).toString()
        return PressureCurveInput(
            knownGradient = true,
            pressure1 = pressure, pressureUnit1 = PressureUnit.KPA,
            voltage1 = voltage.toString(), voltageUnit1 = VoltageUnit.VOLTS,
            gradient = gradient.toString(), gradientPressureUnit = PressureUnit.KPA,
            gradientVoltageUnit = VoltageUnit.VOLTS,
            pressure2 = pressure, pressureUnit2 = PressureUnit.KPA,
            voltage2 = voltage.toString(), voltageUnit2 = VoltageUnit.VOLTS,
        )
    }

    fun previewPressureCurve(sensor: PressureSensor, input: PressureCurveInput): PressureCurvePreview? {
        val current = packet ?: return null
        if (!pressureSensorSupported(sensor)) return null
        val rawP1 = input.pressure1.toFloatOrNull()?.takeIf(Float::isFinite) ?: return null
        val rawV1 = input.voltage1.toFloatOrNull()?.takeIf(Float::isFinite) ?: return null
        val p1 = pressureToPa(rawP1, input.pressureUnit1)
        val v1 = voltageToVolts(rawV1, input.voltageUnit1)
        if (!inPressureBounds(p1, sensor) || !inVoltageBounds(v1)) return null
        val gradientPaPerVolt = if (input.knownGradient) {
            val userGradient = input.gradient.toFloatOrNull()?.takeIf(Float::isFinite) ?: return null
            val gradient = if (input.gradientReciprocal) {
                pressureToPa(1f, input.gradientPressureUnit) / voltageToVolts(userGradient, input.gradientVoltageUnit)
            } else {
                pressureToPa(userGradient, input.gradientPressureUnit) / voltageToVolts(1f, input.gradientVoltageUnit)
            }
            // A conversion such as 1 Pa/mV can land just below 1000 Pa/V in Float.
            if (kotlin.math.abs(gradient) !in Math.nextDown(1000f)..Math.nextUp(500000f)) return null
            gradient
        } else {
            val rawP2 = input.pressure2.toFloatOrNull()?.takeIf(Float::isFinite) ?: return null
            val rawV2 = input.voltage2.toFloatOrNull()?.takeIf(Float::isFinite) ?: return null
            val p2 = pressureToPa(rawP2, input.pressureUnit2)
            val v2 = voltageToVolts(rawV2, input.voltageUnit2)
            if (!inPressureBounds(p2, sensor) || !inVoltageBounds(v2)) return null
            if (v1 >= v2) return null
            (p2 - p1) / (v2 - v1)
        }
        if (!gradientPaPerVolt.isFinite() || gradientPaPerVolt == 0f) return null
        val result = PressureCurvePreview(p1 / gradientPaPerVolt - v1, gradientPaPerVolt / 1000f)
        return result.takeIf { current.canEncodePressureCurve(it.offsetVolts, it.gradientKpaPerVolt) }
    }

    fun applyPressureCurve(sensor: PressureSensor, input: PressureCurveInput): Boolean {
        val result = previewPressureCurve(sensor, input) ?: return false
        return edit({ pressureSensorSupported(sensor) }) {
            when (sensor) {
                PressureSensor.MAP -> { it.mapCurveOffset = result.offsetVolts; it.mapCurveGradient = result.gradientKpaPerVolt }
                PressureSensor.MAP2 -> { it.map2CurveOffset = result.offsetVolts; it.map2CurveGradient = result.gradientKpaPerVolt }
                PressureSensor.GPS -> { it.gpsCurveOffset = result.offsetVolts; it.gpsCurveGradient = result.gradientKpaPerVolt }
                PressureSensor.FPS -> { it.fpsCurveOffset = result.offsetVolts; it.fpsCurveGradient = result.gradientKpaPerVolt }
            }
        }
    }

    private fun pressureSensorSupported(sensor: PressureSensor): Boolean = when (sensor) {
        PressureSensor.GPS, PressureSensor.FPS -> ParameterCapabilities(firmware()).dbw
        else -> true
    }

    private fun inPressureBounds(pa: Float, sensor: PressureSensor): Boolean {
        val kpa = pa / 1000f
        return kpa.isFinite() && kpa in 0f..sensor.maxPressureKpa
    }

    private fun inVoltageBounds(volts: Float) = volts.isFinite() && volts in 0f..5.5f

    private fun pressureToPa(value: Float, unit: PressureUnit): Float = when (unit) {
        PressureUnit.PA -> value
        PressureUnit.KPA -> (value * 1000.0).toFloat()
        PressureUnit.BAR -> (value * 100000.0).toFloat()
        PressureUnit.MMHG -> (value / (7.5006 * .001)).toFloat()
        PressureUnit.TECHNICAL_ATM -> (value / (10.197 * .000001)).toFloat()
        PressureUnit.PSI -> (value / (145.04 * .000001)).toFloat()
    }

    private fun voltageToVolts(value: Float, unit: VoltageUnit): Float = when (unit) {
        VoltageUnit.VOLTS -> value
        VoltageUnit.MILLIVOLTS -> (value / 1000.0).toFloat()
    }

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
        private const val TPS_SETTLE_MILLIS = 1_000L
        private const val MAF_LOAD_SOURCE = 4
        private const val DISABLED_OUTPUT = 15
    }
}
