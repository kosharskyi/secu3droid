/*
 *    SecuDroid  - An open source, free manager for SECU-3 engine control unit
 *    Copyright (C) 2024 Vitalii O. Kosharskyi. Ukraine, Kyiv
 *
 *    SECU-3  - An open source, free engine control unit
 *    Copyright (C) 2007-2024 Alexey A. Shabelnikov. Ukraine, Kyiv
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *    contacts:
 *                    http://secu-3.org
 *                    email: vetalkosharskiy@gmail.com
 */
package org.secu3.android.ui.parameters

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.secu3.android.R
import org.secu3.android.ui.messages.UiMessageDuration
import org.secu3.android.ui.messages.UiMessageQueue
import org.secu3.android.ui.PARAMETERS_ROUTE
import org.secu3.android.connection.Secu3ConnectionManager
import org.secu3.android.domain.parameters.EepromSaveResult
import org.secu3.android.domain.parameters.ParameterPage
import org.secu3.android.domain.parameters.ParameterCapabilities
import org.secu3.android.domain.parameters.ParameterReadResult
import org.secu3.android.domain.parameters.ParameterSession
import org.secu3.android.domain.parameters.ParameterSessionEvent
import org.secu3.android.domain.parameters.GasDoseControl
import org.secu3.android.domain.parameters.FuelInjectionControl
import org.secu3.android.domain.parameters.ChokeControl
import org.secu3.android.models.packets.base.OutputPacket
import org.secu3.android.models.packets.out.params.AccelerationParamPacket
import org.secu3.android.models.packets.out.params.AdcCorrectionsParamPacket
import org.secu3.android.models.packets.out.params.AnglesParamPacket
import org.secu3.android.models.packets.out.params.CarburParamPacket
import org.secu3.android.models.packets.out.params.ChokeControlParPacket
import org.secu3.android.models.packets.out.params.CkpsParamPacket
import org.secu3.android.models.packets.out.params.DbwParamPacket
import org.secu3.android.models.packets.out.params.FunSetParamPacket
import org.secu3.android.models.packets.out.params.GasDoseParamPacket
import org.secu3.android.models.packets.out.params.IdlingParamPacket
import org.secu3.android.models.packets.out.params.InjctrParPacket
import org.secu3.android.models.packets.out.params.KnockParamPacket
import org.secu3.android.models.packets.out.params.LambdaParamPacket
import org.secu3.android.models.packets.out.params.LtftParamPacket
import org.secu3.android.models.packets.out.params.MiscellaneousParamPacket
import org.secu3.android.models.packets.out.params.SecurityParamPacket
import org.secu3.android.models.packets.out.params.StarterParamPacket
import org.secu3.android.models.packets.out.params.TemperatureParamPacket
import org.secu3.android.models.packets.out.params.UniOutParamPacket
import org.secu3.android.ui.parameters.pages.compose.AnglesStateHolder
import org.secu3.android.ui.parameters.pages.compose.CarburStateHolder
import org.secu3.android.ui.parameters.pages.compose.CkpsStateHolder
import org.secu3.android.ui.parameters.pages.compose.AdcCorrectionsStateHolder
import org.secu3.android.ui.parameters.pages.compose.KnockStateHolder
import org.secu3.android.ui.parameters.pages.compose.LambdaControlStateHolder
import org.secu3.android.ui.parameters.pages.compose.LtftStateHolder
import org.secu3.android.ui.parameters.pages.compose.IdlingStateHolder
import org.secu3.android.ui.parameters.pages.compose.TemperatureStateHolder
import org.secu3.android.ui.parameters.pages.compose.StarterStateHolder
import org.secu3.android.ui.parameters.pages.AccelerationStateHolder
import org.secu3.android.ui.parameters.pages.MiscellaneousStateHolder
import org.secu3.android.ui.parameters.pages.SecurityStateHolder
import org.secu3.android.ui.parameters.pages.compose.FunctionsStateHolder
import org.secu3.android.ui.parameters.pages.compose.ChokeNumber
import org.secu3.android.ui.parameters.pages.compose.GasDoseNumber
import org.secu3.android.ui.parameters.pages.compose.DbwStateHolder
import org.secu3.android.ui.parameters.pages.compose.FuelInjectionNumber
import org.secu3.android.ui.parameters.pages.compose.FuelInjectionFlag
import org.secu3.android.ui.parameters.pages.UniversalOutputsStateHolder
import javax.inject.Inject

@HiltViewModel
class ParamsViewModel @Inject constructor(
    @ApplicationContext private val context: Context, // TODO: move this out
    private val secu3ConnectionManager: Secu3ConnectionManager,
    private val parameterSession: ParameterSession,
    internal val chokeControl: ChokeControl,
    internal val gasDoseControl: GasDoseControl,
    internal val fuelInjectionControl: FuelInjectionControl,
    private val uiMessageQueue: UiMessageQueue,
    private val parameterPageCatalog: ParameterPageCatalog,
) : ViewModel() {

    var isSendAllowed: Boolean = false

    internal var parameterEditSession: ParameterEditSession? by mutableStateOf(null)
        private set
    private var nextParameterEditSessionId = 0L

    internal var connectionSession by mutableIntStateOf(0)
        private set
    internal var failedPage: ParameterPage? by mutableStateOf(null)
        private set

    val connectionState = secu3ConnectionManager.connectionStateFlow
    val capabilities = secu3ConnectionManager.firmwareInfo
        .map(::ParameterCapabilities)
        .stateIn(viewModelScope, SharingStarted.Eagerly, ParameterCapabilities(secu3ConnectionManager.fwInfo))

    internal fun parameterPages(capabilities: ParameterCapabilities): List<ParameterPageSpec> =
        parameterPageCatalog.pages(capabilities)

    internal val starterStateHolder by lazy {
        StarterStateHolder(::sendPacket) { isSendAllowed = true }
    }

    internal val anglesStateHolder by lazy {
        AnglesStateHolder(::sendPacket, { isSendAllowed = false }, { isSendAllowed = true })
    }

    internal val idlingStateHolder by lazy {
        IdlingStateHolder(::sendPacket, { isSendAllowed = false }, { isSendAllowed = true })
    }

    internal val temperatureStateHolder by lazy {
        TemperatureStateHolder(::sendPacket, { isSendAllowed = false }, { isSendAllowed = true })
    }

    internal val carburStateHolder by lazy {
        CarburStateHolder(::sendPacket, { isSendAllowed = false }, { isSendAllowed = true })
    }

    internal val ckpsStateHolder by lazy {
        CkpsStateHolder(::sendPacket, { isSendAllowed = false }, { isSendAllowed = true })
    }

    internal val adcCorrectionsStateHolder by lazy {
        AdcCorrectionsStateHolder(::sendPacket, { isSendAllowed = false }, { isSendAllowed = true })
    }

    internal val knockStateHolder by lazy {
        KnockStateHolder(
            bpfOptions = context.resources.getStringArray(R.array.knock_filter_frequencies).toList(),
            integrationOptions = context.resources.getStringArray(R.array.knock_integration_timeouts).toList(),
            sendPacket = ::sendPacket,
            onPacketMapping = { isSendAllowed = false },
            onPacketMapped = { isSendAllowed = true },
        )
    }

    internal val lambdaControlStateHolder by lazy {
        LambdaControlStateHolder(::sendPacket, { isSendAllowed = false }, { isSendAllowed = true })
    }

    internal val accelerationStateHolder by lazy {
        AccelerationStateHolder(::sendPacket) { isSendAllowed = it }
    }

    internal val ltftStateHolder by lazy {
        LtftStateHolder(::sendPacket, { isSendAllowed = false }, { isSendAllowed = true })
    }

    internal val dbwStateHolder by lazy {
        DbwStateHolder(::sendPacket) { isSendAllowed = it }
    }

    internal val universalOutputsStateHolder by lazy {
        UniversalOutputsStateHolder(::sendPacket) { isSendAllowed = it }
    }

    internal val functionsStateHolder by lazy { FunctionsStateHolder(::sendPacket) }

    internal val miscellaneousStateHolder by lazy { MiscellaneousStateHolder(::sendPacket) }

    internal val securityStateHolder by lazy {
        SecurityStateHolder(::sendPacket, context.getString(R.string.sec_invalid_credentials))
    }

    init {
        parameterSession.start(viewModelScope, ::onParameterSessionEvent)
        chokeControl.start(viewModelScope)
        gasDoseControl.start(viewModelScope)
    }

    internal fun activatePage(page: ParameterPage) = parameterSession.activatePage(page)

    internal fun deactivatePage() = parameterSession.deactivatePage()

    internal fun pausePageSession() = parameterSession.pause()

    internal fun refreshPage(page: ParameterPage) = parameterSession.refreshPage(page)

    internal fun cancelManualCommands() {
        chokeControl.cancelManual()
        gasDoseControl.cancelManual()
    }

    internal fun editChoke(number: ChokeNumber, value: Float) {
        val sent = when (number) {
            ChokeNumber.STEPS -> chokeControl.editSteps(value)
            ChokeNumber.REGULATOR_FACTOR -> chokeControl.editRegulatorFactor(value)
            ChokeNumber.COLD_DURATION -> chokeControl.editColdDuration(value)
            ChokeNumber.HOT_DURATION -> chokeControl.editHotDuration(value)
            ChokeNumber.CRANK_TO_RUN -> chokeControl.editCrankToRunDuration(value)
            else -> false
        }
        showParameterPacketSentMessage(sent)
    }

    internal fun toggleChoke(number: ChokeNumber, enabled: Boolean) {
        val sent = when (number) {
            ChokeNumber.CLOSED_LOOP -> chokeControl.setClosedLoopRegulator(enabled)
            ChokeNumber.DISABLE_ON_GAS -> chokeControl.setRegulatorDisabledOnGas(enabled)
            ChokeNumber.THROTTLE_INIT -> chokeControl.setThrottlePositionAtInit(enabled)
            ChokeNumber.MAX_FREQUENCY_INIT -> chokeControl.setMaximumFrequencyAtInit(enabled)
            else -> false
        }
        showParameterPacketSentMessage(sent)
    }

    internal fun selectChokeFrequency(index: Int) = showParameterPacketSentMessage(chokeControl.selectFrequency(index))

    internal fun toggleChokeTest() = showParameterPacketSentMessage(chokeControl.toggleTest())

    internal fun stepChoke(direction: Int) = chokeControl.manualStep(direction)

    internal fun flushChokeManual() = showParameterPacketSentMessage(chokeControl.flushManual())

    internal fun finishChokeManual() = showParameterPacketSentMessage(chokeControl.finishManual())

    internal fun cancelChokeManual() = chokeControl.cancelManual()

    internal fun editGasDose(number: GasDoseNumber, value: Float) {
        val sent = when (number) {
            GasDoseNumber.STEPS -> gasDoseControl.editSteps(value)
            GasDoseNumber.STOICHIOMETRIC_RATIO -> gasDoseControl.editStoichiometricRatio(value)
            GasDoseNumber.FUEL_CUT_CLOSING -> gasDoseControl.editFuelCutClosing(value)
            GasDoseNumber.CORRECTION_POSITIVE -> gasDoseControl.editCorrectionPositive(value)
            GasDoseNumber.CORRECTION_NEGATIVE -> gasDoseControl.editCorrectionNegative(value)
        }
        showParameterPacketSentMessage(sent)
    }

    internal fun selectGasDoseFrequency(index: Int) = showParameterPacketSentMessage(gasDoseControl.selectFrequency(index))

    internal fun setGasDoseMaximumFrequency(enabled: Boolean) =
        showParameterPacketSentMessage(gasDoseControl.setMaximumFrequency(enabled))

    internal fun toggleGasDoseTest() = showParameterPacketSentMessage(gasDoseControl.toggleTest())

    internal fun stepGasDose(direction: Int) = gasDoseControl.manualStep(direction)

    internal fun flushGasDoseManual() = showParameterPacketSentMessage(gasDoseControl.flushManual())

    internal fun finishGasDoseManual() = showParameterPacketSentMessage(gasDoseControl.finishManual())

    internal fun cancelGasDoseManual() = gasDoseControl.cancelManual()

    internal fun editFuelInjection(number: FuelInjectionNumber, value: Float) {
        val sent = when (number) {
            FuelInjectionNumber.ENGINE_DISPLACEMENT -> fuelInjectionControl.editEngineDisplacement(value)
            FuelInjectionNumber.FLOW_0 -> fuelInjectionControl.editFlow(0, value)
            FuelInjectionNumber.TIMING_0 -> fuelInjectionControl.editTiming(0, value)
            FuelInjectionNumber.CRANKING_0 -> fuelInjectionControl.editCrankingTiming(0, value)
            FuelInjectionNumber.MIN_PW_0 -> fuelInjectionControl.editMinimumPulseWidth(0, value)
            FuelInjectionNumber.MAX_PW_0 -> fuelInjectionControl.editMaximumPulseWidth(0, value)
            FuelInjectionNumber.FLOW_1 -> fuelInjectionControl.editFlow(1, value)
            FuelInjectionNumber.TIMING_1 -> fuelInjectionControl.editTiming(1, value)
            FuelInjectionNumber.CRANKING_1 -> fuelInjectionControl.editCrankingTiming(1, value)
            FuelInjectionNumber.MIN_PW_1 -> fuelInjectionControl.editMinimumPulseWidth(1, value)
            FuelInjectionNumber.MAX_PW_1 -> fuelInjectionControl.editMaximumPulseWidth(1, value)
            FuelInjectionNumber.PULSES_PER_LITER -> fuelInjectionControl.editPulsesPerLiter(value)
            FuelInjectionNumber.DECREASE_RATE -> fuelInjectionControl.editDecreaseRate(value)
            FuelInjectionNumber.INCREASE_RATE -> fuelInjectionControl.editIncreaseRate(value)
        }
        showParameterPacketSentMessage(sent)
    }

    internal fun changeFuelInjectionConfig(bank: Int, value: Int) =
        showParameterPacketSentMessage(fuelInjectionControl.changeConfig(bank, value))

    internal fun changeFuelInjectionPulses(bank: Int, value: Int) =
        showParameterPacketSentMessage(fuelInjectionControl.changePulses(bank, value))

    internal fun changeFuelInjectionAngleSpec(bank: Int, value: Int) =
        showParameterPacketSentMessage(fuelInjectionControl.changeAngleSpec(bank, value))

    internal fun changeFuelInjectionFlag(flag: FuelInjectionFlag, value: Boolean) {
        val sent = when (flag) {
            FuelInjectionFlag.USE_MAP_0 -> fuelInjectionControl.setUseMap(0, value)
            FuelInjectionFlag.USE_MAP_1 -> fuelInjectionControl.setUseMap(1, value)
            FuelInjectionFlag.ADDITIONAL_CORRECTIONS -> fuelInjectionControl.setAdditionalCorrections(value)
            FuelInjectionFlag.AIR_DENSITY -> fuelInjectionControl.setAirDensity(value)
            FuelInjectionFlag.DIFFERENTIAL_PRESSURE -> fuelInjectionControl.setDifferentialPressure(value)
            FuelInjectionFlag.SWITCH_ROWS -> fuelInjectionControl.setSwitchRows(value)
            FuelInjectionFlag.FULL_SEQUENTIAL_AFTER_START -> fuelInjectionControl.setFullSequentialAfterStart(value)
        }
        showParameterPacketSentMessage(sent)
    }

    private fun showParameterPacketSentMessage(sent: Boolean) {
        if (sent) showUiMessage(R.string.packet_sent, UiMessageDuration.SHORT)
    }

    internal fun openParameterEditor(request: ParameterEditRequest) {
        parameterEditSession = ParameterEditSession(++nextParameterEditSessionId, request)
    }

    internal fun dismissParameterEditor(sessionId: Long) {
        if (parameterEditSession?.id == sessionId) parameterEditSession = null
    }

    internal fun acceptParameterEditor(sessionId: Long, value: Number) {
        val session = parameterEditSession?.takeIf { it.id == sessionId } ?: return
        val accepted = when (val request = session.request) {
            is ParameterEditRequest.IntValue -> (value as? Int)?.takeIf { it in request.min..request.max }
            is ParameterEditRequest.FloatValue -> (value as? Float)?.takeIf { it.isFinite() && it in request.min..request.max }
        } ?: return
        parameterEditSession = null
        when (val request = session.request) {
            is ParameterEditRequest.IntValue -> request.onValue(accepted as Int)
            is ParameterEditRequest.FloatValue -> request.onValue(accepted as Float)
        }
    }

    internal fun clearPageState() = parameterSession.closePageSession()

    private fun onParameterSessionEvent(event: ParameterSessionEvent) {
        when (event) {
            is ParameterSessionEvent.Invalidated -> {
                isSendAllowed = false
                connectionSession = event.sessionId
                parameterEditSession = null
                failedPage = null
                resetAllStateHolders()
            }
            is ParameterSessionEvent.PageLoading -> {
                if (failedPage == event.page) failedPage = null
                resetStateHolder(event.page)
            }
            is ParameterSessionEvent.PageLoaded -> applyPageResult(event.page, event.result)
            is ParameterSessionEvent.PageFailed -> failedPage = event.page
            ParameterSessionEvent.SaveStarted -> showUiMessage(R.string.saving, UiMessageDuration.SHORT)
            is ParameterSessionEvent.SaveFinished -> when (event.result) {
                EepromSaveResult.Confirmed -> showUiMessage(R.string.packet_saved, UiMessageDuration.SHORT)
                EepromSaveResult.Unconfirmed -> showUiMessage(R.string.parameter_save_unconfirmed, UiMessageDuration.LONG)
                EepromSaveResult.NotStarted -> Unit
            }
        }
    }

    private fun resetStateHolder(page: ParameterPage) {
        when (page) {
            ParameterPage.STARTER -> starterStateHolder.reset()
            ParameterPage.ANGLES -> anglesStateHolder.reset()
            ParameterPage.IDLING -> idlingStateHolder.reset()
            ParameterPage.TEMPERATURE -> temperatureStateHolder.reset()
            ParameterPage.FUEL_CUTOFF -> carburStateHolder.reset()
            ParameterPage.CKPS -> ckpsStateHolder.reset()
            ParameterPage.ADC_ERRORS -> adcCorrectionsStateHolder.reset()
            ParameterPage.KNOCK -> knockStateHolder.reset()
            ParameterPage.LAMBDA_CONTROL -> lambdaControlStateHolder.reset()
            ParameterPage.ACCELERATION -> accelerationStateHolder.reset()
            ParameterPage.LTFT -> ltftStateHolder.reset()
            ParameterPage.FUNCTIONS -> functionsStateHolder.reset()
            ParameterPage.MISCELLANEOUS -> miscellaneousStateHolder.reset()
            ParameterPage.SECURITY -> securityStateHolder.reset()
            ParameterPage.CHOKE_CONTROL -> chokeControl.reset()
            ParameterPage.GAS_DOSE -> gasDoseControl.reset()
            ParameterPage.EL_THROTTLE -> dbwStateHolder.reset()
            ParameterPage.FUEL_INJECTION -> fuelInjectionControl.reset()
            ParameterPage.UNIVERSAL_OUTPUTS -> universalOutputsStateHolder.reset()
        }
    }

    private fun resetAllStateHolders() {
        ParameterPage.entries.forEach(::resetStateHolder)
    }

    private fun applyPageResult(page: ParameterPage, result: ParameterReadResult) {
        when (page) {
            ParameterPage.STARTER -> starterStateHolder.onPacketReceived(packet<StarterParamPacket>(result))
            ParameterPage.ANGLES -> anglesStateHolder.onPacketReceived(packet<AnglesParamPacket>(result))
            ParameterPage.IDLING -> idlingStateHolder.onPacketReceived(packet<IdlingParamPacket>(result))
            ParameterPage.TEMPERATURE -> temperatureStateHolder.onPacketReceived(packet<TemperatureParamPacket>(result))
            ParameterPage.FUEL_CUTOFF -> carburStateHolder.onPacketReceived(packet<CarburParamPacket>(result))
            ParameterPage.CKPS -> ckpsStateHolder.onPacketReceived(packet<CkpsParamPacket>(result))
            ParameterPage.ADC_ERRORS -> adcCorrectionsStateHolder.onPacketReceived(packet<AdcCorrectionsParamPacket>(result))
            ParameterPage.KNOCK -> knockStateHolder.onPacketReceived(packet<KnockParamPacket>(result))
            ParameterPage.LAMBDA_CONTROL -> lambdaControlStateHolder.onPacketReceived(packet<LambdaParamPacket>(result))
            ParameterPage.ACCELERATION -> accelerationStateHolder.onPacketReceived(packet<AccelerationParamPacket>(result))
            ParameterPage.LTFT -> ltftStateHolder.receive(packet<LtftParamPacket>(result))
            ParameterPage.FUNCTIONS -> {
                val functionParameters = (result as ParameterReadResult.Functions).parameters
                isSendAllowed = false
                try {
                    functionsStateHolder.onNamesReceived(functionParameters.names)
                    functionsStateHolder.onPacketReceived(
                        received = functionParameters.settings,
                        loads = context.resources.getStringArray(R.array.load_measurement_items).toList(),
                        bars = context.resources.getStringArray(R.array.barocorr_items).toList(),
                        ve2 = context.resources.getStringArray(R.array.ve2_map_func_items).toList(),
                    )
                } finally {
                    isSendAllowed = true
                }
            }
            ParameterPage.MISCELLANEOUS -> {
                isSendAllowed = false
                try {
                    miscellaneousStateHolder.onPacketReceived(packet<MiscellaneousParamPacket>(result))
                } finally {
                    isSendAllowed = true
                }
            }
            ParameterPage.SECURITY -> {
                isSendAllowed = false
                try {
                    securityStateHolder.load(packet<SecurityParamPacket>(result))
                } finally {
                    isSendAllowed = true
                }
            }
            ParameterPage.CHOKE_CONTROL -> {
                chokeControl.receive(packet<ChokeControlParPacket>(result))
            }
            ParameterPage.GAS_DOSE -> {
                gasDoseControl.receive(packet<GasDoseParamPacket>(result))
            }
            ParameterPage.EL_THROTTLE -> dbwStateHolder.receive(packet<DbwParamPacket>(result))
            ParameterPage.FUEL_INJECTION -> fuelInjectionControl.receive(packet<InjctrParPacket>(result))
            ParameterPage.UNIVERSAL_OUTPUTS -> universalOutputsStateHolder.onPacketReceived(packet<UniOutParamPacket>(result))
        }
    }

    private inline fun <reified T : org.secu3.android.models.packets.base.InputPacket> packet(result: ParameterReadResult): T =
        (result as ParameterReadResult.Packet).packet as T

    fun sendPacket(packet: OutputPacket) {
        if (!isSendAllowed || !parameterSession.sendPacket(packet)) {
            return
        }
        showUiMessage(R.string.packet_sent, UiMessageDuration.SHORT)
    }

    fun savePacket() = parameterSession.save()

    internal fun showUiMessage(textRes: Int, duration: UiMessageDuration) {
        uiMessageQueue.publish(textRes, duration, scope = PARAMETERS_ROUTE)
    }
}
