package org.secu3.android.domain.parameters

import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.secu3.android.connection.Secu3ConnectionManager
import org.secu3.android.models.packets.base.OutputPacket
import org.secu3.android.models.packets.input.FirmwareInfoPacket
import org.secu3.android.models.packets.out.params.ChokeControlParPacket
import javax.inject.Inject

data class ChokeValues(
    val steps: Int,
    val regulatorFactor: Float,
    val coldDuration: Float,
    val hotDuration: Float,
    val crankToRunDuration: Float,
    val closedLoopRegulator: Boolean,
    val disableRegulatorOnGas: Boolean,
    val throttlePositionAtInit: Boolean,
    val maximumFrequencyAtInit: Boolean,
    val testing: Boolean,
    val frequency: Int,
)

/** Owns Choke Control packet edits, firmware admission, and immutable command snapshots. */
@ViewModelScoped
class ChokeControl internal constructor(
    private val firmwareInfo: StateFlow<FirmwareInfoPacket?>,
    private val sendPacket: (OutputPacket) -> Boolean,
    private val manualPosition: ManualPositionDelta,
) {
    @Inject
    constructor(
        connectionManager: Secu3ConnectionManager,
        parameterSession: ParameterSession,
        manualPosition: ManualPositionDelta,
    ) : this(connectionManager.firmwareInfo, parameterSession::sendPacket, manualPosition)

    private val mutableValues = MutableStateFlow<ChokeValues?>(null)
    val values: StateFlow<ChokeValues?> = mutableValues.asStateFlow()

    private var packet: ChokeControlParPacket? = null
    private var firmwareJob: Job? = null

    fun start(scope: CoroutineScope) {
        if (firmwareJob != null) return
        firmwareJob = scope.launch {
            firmwareInfo.collect { firmware ->
                if (!ParameterCapabilities(firmware).choke) manualPosition.clear()
            }
        }
    }

    fun reset() {
        packet = null
        manualPosition.clear()
        mutableValues.value = null
    }

    fun receive(received: ChokeControlParPacket) {
        manualPosition.clear()
        received.manualPositionD = 0
        packet = received
        publishValues()
    }

    fun editSteps(value: Float): Boolean {
        if (!canControl() || !value.isFinite()) return false
        val current = packet ?: return false
        current.smSteps = value.toInt().coerceIn(STEPS_MIN, STEPS_MAX)
        return sendOrdinary(current)
    }

    fun editRegulatorFactor(value: Float): Boolean {
        if (!canEditCarburetorFields() || !value.isFinite()) return false
        val current = packet ?: return false
        current.rpmIf = value.coerceIn(REGULATOR_FACTOR_MIN, REGULATOR_FACTOR_MAX)
        return sendOrdinary(current)
    }

    fun editColdDuration(value: Float): Boolean {
        if (!canEditCarburetorFields() || !value.isFinite()) return false
        val current = packet ?: return false
        current.corrTime0 = value.coerceIn(CORRECTION_DURATION_MIN, CORRECTION_DURATION_MAX)
        return sendOrdinary(current)
    }

    fun editHotDuration(value: Float): Boolean {
        if (!canEditCarburetorFields() || !value.isFinite()) return false
        val current = packet ?: return false
        current.corrTime1 = value.coerceIn(CORRECTION_DURATION_MIN, CORRECTION_DURATION_MAX)
        return sendOrdinary(current)
    }

    fun editCrankToRunDuration(value: Float): Boolean {
        if (!canEditCarburetorFields() || !value.isFinite()) return false
        val current = packet ?: return false
        current.injCrankToRunTime = value.coerceIn(CRANK_TO_RUN_MIN, CRANK_TO_RUN_MAX)
        return sendOrdinary(current)
    }

    fun setClosedLoopRegulator(enabled: Boolean): Boolean {
        if (!canEditCarburetorFields()) return false
        val current = packet ?: return false
        current.useClosedLoopRmpRegulator = enabled
        return sendOrdinary(current)
    }

    fun setRegulatorDisabledOnGas(enabled: Boolean): Boolean {
        if (!canEditCarburetorFields()) return false
        val current = packet ?: return false
        current.dontUseRpmRegOnGas = enabled
        return sendOrdinary(current)
    }

    fun setThrottlePositionAtInit(enabled: Boolean): Boolean {
        if (!canControl()) return false
        val current = packet ?: return false
        current.useThrottlePosInChokeInit = enabled
        return sendOrdinary(current)
    }

    fun setMaximumFrequencyAtInit(enabled: Boolean): Boolean {
        if (!canControl()) return false
        val current = packet ?: return false
        current.maxSTEPfreqAtInit = enabled
        return sendOrdinary(current)
    }

    fun selectFrequency(index: Int): Boolean {
        if (!canControl() || index !in FREQUENCY_MIN..FREQUENCY_MAX) return false
        val current = packet ?: return false
        current.smFreq = index
        return sendOrdinary(current)
    }

    fun toggleTest(): Boolean {
        if (!ParameterCapabilities(firmwareInfo.value).choke) return false
        val current = packet ?: return false
        current.testing = !current.testing
        return sendOrdinary(current)
    }

    fun manualStep(direction: Int) {
        if (!ParameterCapabilities(firmwareInfo.value).choke || packet == null) return
        manualPosition.add(direction)
    }

    fun flushManual(): Boolean {
        if (!ParameterCapabilities(firmwareInfo.value).choke) {
            manualPosition.clear()
            return false
        }
        val current = packet ?: return false
        val delta = manualPosition.take()
        if (delta == 0) return false
        return sendPacket(current.snapshotWithManualPosition(delta))
    }

    fun finishManual(): Boolean = flushManual()

    fun cancelManual() {
        manualPosition.clear()
        packet?.manualPositionD = 0
    }

    private fun canControl() = ParameterCapabilities(firmwareInfo.value).choke

    private fun canEditCarburetorFields() = canControl() && ParameterCapabilities(firmwareInfo.value).carburetor

    private fun sendOrdinary(current: ChokeControlParPacket): Boolean {
        publishValues()
        return sendPacket(current.snapshotWithManualPosition(0))
    }

    private fun publishValues() {
        packet?.let { current ->
            mutableValues.value = ChokeValues(
                steps = current.smSteps,
                regulatorFactor = current.rpmIf,
                coldDuration = current.corrTime0,
                hotDuration = current.corrTime1,
                crankToRunDuration = current.injCrankToRunTime,
                closedLoopRegulator = current.useClosedLoopRmpRegulator,
                disableRegulatorOnGas = current.dontUseRpmRegOnGas,
                throttlePositionAtInit = current.useThrottlePosInChokeInit,
                maximumFrequencyAtInit = current.maxSTEPfreqAtInit,
                testing = current.testing,
                frequency = current.smFreq,
            )
        }
    }

    companion object {
        const val STEPS_MIN = 50
        const val STEPS_MAX = 2000
        const val REGULATOR_FACTOR_MIN = .01f
        const val REGULATOR_FACTOR_MAX = 1f
        const val CORRECTION_DURATION_MIN = .1f
        const val CORRECTION_DURATION_MAX = 10f
        const val CRANK_TO_RUN_MIN = .1f
        const val CRANK_TO_RUN_MAX = 99f
        const val FREQUENCY_MIN = 0
        const val FREQUENCY_MAX = 3
    }
}
