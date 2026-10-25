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
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.secu3.android.R
import org.secu3.android.connection.ConnectionState
import org.secu3.android.connection.Secu3ConnectionManager
import org.secu3.android.domain.parameters.EepromSaveResult
import org.secu3.android.domain.parameters.ParameterPage
import org.secu3.android.domain.parameters.ParameterReadResult
import org.secu3.android.domain.parameters.ParameterSession
import org.secu3.android.domain.parameters.ParameterSessionEvent
import org.secu3.android.models.packets.base.OutputPacket
import org.secu3.android.models.packets.input.FirmwareInfoPacket
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
import org.secu3.android.ui.parameters.pages.compose.ChokeControlStateHolder
import org.secu3.android.ui.parameters.pages.compose.GasDoseStateHolder
import org.secu3.android.ui.parameters.pages.compose.DbwStateHolder
import org.secu3.android.ui.parameters.pages.compose.FuelInjectionStateHolder
import org.secu3.android.ui.parameters.pages.UniversalOutputsStateHolder
import javax.inject.Inject

@HiltViewModel
class ParamsViewModel @Inject constructor(
    @ApplicationContext private val context: Context, // TODO: move this out
    private val secu3ConnectionManager: Secu3ConnectionManager,
    private val parameterSession: ParameterSession,
) : ViewModel() {

    var isSendAllowed: Boolean = false

    internal var parameterEditSession: ParameterEditSession? by mutableStateOf(null)
        private set
    private var nextParameterEditSessionId = 0L

    internal var connectionSession by mutableIntStateOf(0)
        private set
    internal var failedPage: ParameterPage? by mutableStateOf(null)
        private set

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

    internal val chokeControlStateHolder by lazy {
        ChokeControlStateHolder({}, ::sendPacket)
    }

    internal val gasDoseStateHolder by lazy {
        GasDoseStateHolder({}, ::sendPacket).also {
            it.setFirmwareAvailable(fwInfoPacket?.isGdControlEnabled ?: true)
        }
    }

    internal val dbwStateHolder by lazy {
        DbwStateHolder(::sendPacket) { isSendAllowed = it }
    }

    internal val fuelInjectionStateHolder by lazy {
        FuelInjectionStateHolder(::sendPacket) { isSendAllowed = it }
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
        viewModelScope.launch {
            secu3ConnectionManager.firmwareLiveData.asFlow().collect { firmware ->
                gasDoseStateHolder.setFirmwareAvailable(firmware.isGdControlEnabled)
                fuelInjectionStateHolder.updateFirmware(firmware.isFuelInjectEnabled, firmware.isSecu3T)
            }
        }
    }

    internal fun activatePage(page: ParameterPage) = parameterSession.activatePage(page)

    internal fun deactivatePage() = parameterSession.deactivatePage()

    internal fun pausePageSession() = parameterSession.pause()

    internal fun refreshPage(page: ParameterPage) = parameterSession.refreshPage(page)

    internal fun cancelManualCommands() {
        chokeControlStateHolder.cancelManual()
        gasDoseStateHolder.cancelManual()
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

    internal val isConnected: Boolean
        get() = secu3ConnectionManager.isConnected

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
            is ParameterSessionEvent.PageUnavailable -> Unit
            is ParameterSessionEvent.PageFailed -> failedPage = event.page
            ParameterSessionEvent.SaveStarted -> Toast.makeText(context, R.string.saving, Toast.LENGTH_SHORT).show()
            is ParameterSessionEvent.SaveFinished -> when (event.result) {
                EepromSaveResult.Confirmed -> Toast.makeText(context, R.string.packet_saved, Toast.LENGTH_SHORT).show()
                EepromSaveResult.Unconfirmed -> Toast.makeText(context, R.string.parameter_save_unconfirmed, Toast.LENGTH_LONG).show()
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
            ParameterPage.CHOKE_CONTROL -> chokeControlStateHolder.reset()
            ParameterPage.GAS_DOSE -> {
                gasDoseStateHolder.reset()
                gasDoseStateHolder.setFirmwareAvailable(fwInfoPacket?.isGdControlEnabled ?: true)
            }
            ParameterPage.EL_THROTTLE -> dbwStateHolder.reset()
            ParameterPage.FUEL_INJECTION -> {
                fuelInjectionStateHolder.reset()
                fwInfoPacket?.let { fuelInjectionStateHolder.updateFirmware(it.isFuelInjectEnabled, it.isSecu3T) }
                    ?: fuelInjectionStateHolder.updateFirmware(injectorEnabled = true, secu3t = false)
            }
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
                isSendAllowed = false
                try {
                    chokeControlStateHolder.receive(packet<ChokeControlParPacket>(result))
                } finally {
                    isSendAllowed = true
                }
            }
            ParameterPage.GAS_DOSE -> {
                isSendAllowed = false
                try {
                    gasDoseStateHolder.receive(packet<GasDoseParamPacket>(result))
                } finally {
                    isSendAllowed = true
                }
            }
            ParameterPage.EL_THROTTLE -> dbwStateHolder.receive(packet<DbwParamPacket>(result))
            ParameterPage.FUEL_INJECTION -> fuelInjectionStateHolder.receive(packet<InjctrParPacket>(result))
            ParameterPage.UNIVERSAL_OUTPUTS -> universalOutputsStateHolder.onPacketReceived(packet<UniOutParamPacket>(result))
        }
    }

    private inline fun <reified T : org.secu3.android.models.packets.base.InputPacket> packet(result: ParameterReadResult): T =
        (result as ParameterReadResult.Packet).packet as T

    val connectionStatusLiveData: LiveData<ConnectionState> = secu3ConnectionManager.connectionStateFlow.asLiveData()

    val fwInfoPacket: FirmwareInfoPacket?
        get() = secu3ConnectionManager.fwInfo



    fun sendPacket(packet: OutputPacket) {
        if (!isSendAllowed || !parameterSession.sendPacket(packet)) {
            return
        }
        Toast.makeText(context, context.getString(R.string.packet_sent), Toast.LENGTH_SHORT).show()  // TODO: move this out
    }

    fun savePacket() = parameterSession.save()
}
