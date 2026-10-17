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
import android.util.Log
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.secu3.android.R
import org.secu3.android.connection.ConnectionState
import org.secu3.android.connection.Connected
import org.secu3.android.connection.Secu3ConnectionManager
import org.secu3.android.models.FnName
import org.secu3.android.models.packets.base.OutputPacket
import org.secu3.android.models.packets.base.InputPacket
import org.secu3.android.models.packets.input.FirmwareInfoPacket
import org.secu3.android.models.packets.input.FnNameDatPacket
import org.secu3.android.models.packets.out.OpCompNc
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
import org.secu3.android.utils.Task
import javax.inject.Inject
import java.util.concurrent.TimeoutException

internal fun cancelAndClearPageReadJobs(jobs: MutableMap<Int, Job>) {
    val jobsToCancel = jobs.values.toList()
    jobs.clear()
    jobsToCancel.forEach { it.cancel() }
}

@HiltViewModel
class ParamsViewModel @Inject constructor(
    @ApplicationContext private val context: Context, // TODO: move this out
    private val secu3ConnectionManager: Secu3ConnectionManager) : ViewModel() {

    var isSendAllowed: Boolean = false

    internal var parameterEditSession: ParameterEditSession? by mutableStateOf(null)
        private set
    private var nextParameterEditSessionId = 0L

    private val pageReadGate = ParameterPageReadGate()
    private val pageReadJobs = mutableMapOf<Int, Job>()
    private val loadedPages = mutableSetOf<Int>()
    private var activePageTitle: Int? = null
    private var parameterWriteRevision = 0L
    private var saveJob: Job? = null
    internal var connectionSession by mutableIntStateOf(0)
        private set
    internal var failedPage: Int? by mutableStateOf(null)
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
        viewModelScope.launch {
            secu3ConnectionManager.connectionStateFlow.collect { state ->
                if (state != Connected) {
                    connectionSession++
                    clearPageState()
                }
            }
        }
        viewModelScope.launch {
            secu3ConnectionManager.firmwareLiveData.asFlow().collect { firmware ->
                gasDoseStateHolder.setFirmwareAvailable(firmware.isGdControlEnabled)
                fuelInjectionStateHolder.updateFirmware(firmware.isFuelInjectEnabled, firmware.isSecu3T)
            }
        }
    }

    internal fun activatePage(titleRes: Int) {
        if (!isConnected) return
        activePageTitle = titleRes
        pageReadGate.activate(titleRes)
        if (titleRes in setOf(
                R.string.params_tab_start_title,
                R.string.params_tab_angles_title,
                R.string.params_tab_idling_title,
                R.string.params_tab_temperature_title,
                R.string.params_tab_fuel_cutoff_title,
                R.string.params_tab_ckps_title,
                R.string.params_tab_adc_errors_corr_title,
                R.string.params_tab_knock_title,
                R.string.params_tab_lambda_control_title,
                R.string.params_tab_acceleration_title,
                R.string.params_tab_ltft_title,
                R.string.params_tab_functions_title,
                R.string.params_tab_miscellaneous_title,
                R.string.params_tab_security_title,
                R.string.params_tab_choke_control_title,
                R.string.params_tab_gas_dose_title,
                R.string.params_tab_el_throttle_title,
                R.string.params_tab_fuel_injection_title,
                R.string.params_tab_universal_outputs_title,
            )) {
            loadViewModelPage(titleRes)
        }
    }

    internal fun deactivatePage() = pageReadGate.deactivate()

    internal fun invalidatePageCache() {
        loadedPages.clear()
        parameterWriteRevision++
    }

    internal fun refreshPage(titleRes: Int) {
        if (isConnected) {
            loadedPages.remove(titleRes)
            loadViewModelPage(titleRes)
        }
    }

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

    internal fun clearPageState() {
        isSendAllowed = false
        activePageTitle = null
        loadedPages.clear()
        parameterWriteRevision++
        deactivatePage()
        parameterEditSession = null
        failedPage = null
        val saving = saveJob
        saveJob = null
        saving?.cancel()
        cancelAndClearPageReadJobs(pageReadJobs)
        starterStateHolder.reset()
        anglesStateHolder.reset()
        idlingStateHolder.reset()
        temperatureStateHolder.reset()
        carburStateHolder.reset()
        ckpsStateHolder.reset()
        adcCorrectionsStateHolder.reset()
        knockStateHolder.reset()
        lambdaControlStateHolder.reset()
        accelerationStateHolder.reset()
        ltftStateHolder.reset()
        functionsStateHolder.reset()
        miscellaneousStateHolder.reset()
        securityStateHolder.reset()
        chokeControlStateHolder.reset()
        gasDoseStateHolder.reset()
        gasDoseStateHolder.setFirmwareAvailable(fwInfoPacket?.isGdControlEnabled ?: true)
        dbwStateHolder.reset()
        fuelInjectionStateHolder.reset()
        fwInfoPacket?.let { fuelInjectionStateHolder.updateFirmware(it.isFuelInjectEnabled, it.isSecu3T) }
            ?: fuelInjectionStateHolder.updateFirmware(injectorEnabled = true, secu3t = false)
        universalOutputsStateHolder.reset()
    }

    private suspend inline fun <reified T : InputPacket> readPacket(task: Task): T = readParameterPacket(
        sendRequest = { secu3ConnectionManager.sendNewTask(task) },
        restoreSensors = { secu3ConnectionManager.sendNewTask(Task.Secu3ReadSensors) },
        receive = { onSubscribed -> secu3ConnectionManager.receivedPacketFlow(onSubscribed).first { it is T } as T },
    )

    private fun loadViewModelPage(titleRes: Int) {
        if (titleRes in loadedPages) return
        if (pageReadJobs[titleRes]?.isActive == true) return
        if (failedPage == titleRes) failedPage = null
        var readRevision = parameterWriteRevision
        pageReadJobs[titleRes] = viewModelScope.launch {
            var readCompleted = true
            try {
                when (titleRes) {
                    R.string.params_tab_start_title -> {
                        starterStateHolder.reset()
                        val packet = pageReadGate.read(titleRes, onStarted = { readRevision = parameterWriteRevision }) {
                            readPacket<StarterParamPacket>(Task.Secu3ReadStarterParam)
                        }.first()
                        starterStateHolder.onPacketReceived(packet)
                    }
                    R.string.params_tab_angles_title -> {
                        anglesStateHolder.reset()
                        val packet = pageReadGate.read(titleRes, onStarted = { readRevision = parameterWriteRevision }) {
                            readPacket<AnglesParamPacket>(Task.Secu3ReadAnglesParam)
                        }.first()
                        anglesStateHolder.onPacketReceived(packet)
                    }
                    R.string.params_tab_idling_title -> {
                        idlingStateHolder.reset()
                        val packet = pageReadGate.read(titleRes, onStarted = { readRevision = parameterWriteRevision }) {
                            readPacket<IdlingParamPacket>(Task.Secu3ReadIdlingParam)
                        }.first()
                        idlingStateHolder.onPacketReceived(packet)
                    }
                    R.string.params_tab_temperature_title -> {
                        temperatureStateHolder.reset()
                        val packet = pageReadGate.read(titleRes, onStarted = { readRevision = parameterWriteRevision }) {
                            readPacket<TemperatureParamPacket>(Task.Secu3ReadTemperatureParam)
                        }.first()
                        temperatureStateHolder.onPacketReceived(packet)
                    }
                    R.string.params_tab_fuel_cutoff_title -> {
                        carburStateHolder.reset()
                        val packet = pageReadGate.read(titleRes, onStarted = { readRevision = parameterWriteRevision }) {
                            readPacket<CarburParamPacket>(Task.Secu3ReadCarburParam)
                        }.first()
                        carburStateHolder.onPacketReceived(packet)
                    }
                    R.string.params_tab_ckps_title -> {
                        ckpsStateHolder.reset()
                        val packet = pageReadGate.read(titleRes, onStarted = { readRevision = parameterWriteRevision }) {
                            readPacket<CkpsParamPacket>(Task.Secu3ReadCkpsParam)
                        }.first()
                        ckpsStateHolder.onPacketReceived(packet)
                    }
                    R.string.params_tab_adc_errors_corr_title -> {
                        adcCorrectionsStateHolder.reset()
                        val packet = pageReadGate.read(titleRes, onStarted = { readRevision = parameterWriteRevision }) {
                            readPacket<AdcCorrectionsParamPacket>(Task.Secu3ReadAdcErrorsCorrectionsParam)
                        }.first()
                        adcCorrectionsStateHolder.onPacketReceived(packet)
                    }
                    R.string.params_tab_knock_title -> {
                        knockStateHolder.reset()
                        val packet = pageReadGate.read(titleRes, onStarted = { readRevision = parameterWriteRevision }) {
                            readPacket<KnockParamPacket>(Task.Secu3ReadKnockParam)
                        }.first()
                        knockStateHolder.onPacketReceived(packet)
                    }
                    R.string.params_tab_lambda_control_title -> {
                        lambdaControlStateHolder.reset()
                        val packet = pageReadGate.read(titleRes, onStarted = { readRevision = parameterWriteRevision }) {
                            val fw = secu3ConnectionManager.fwInfo ?: return@read null
                            if (!fw.isFuelInjectEnabled && !fw.isCarbAfrEnabled && !fw.isGdControlEnabled) {
                                return@read null
                            }
                            readPacket<LambdaParamPacket>(Task.Secu3ReadLambdaParam)
                        }.first()
                        if (packet != null) lambdaControlStateHolder.onPacketReceived(packet) else readCompleted = false
                    }
                    R.string.params_tab_acceleration_title -> {
                        accelerationStateHolder.reset()
                        val packet = pageReadGate.read(titleRes, onStarted = { readRevision = parameterWriteRevision }) {
                            val fw = secu3ConnectionManager.fwInfo ?: return@read null
                            if (!fw.isFuelInjectEnabled && !fw.isGdControlEnabled) return@read null
                            readPacket<AccelerationParamPacket>(Task.Secu3ReadAccelerationParam)
                        }.first()
                        if (packet != null) accelerationStateHolder.onPacketReceived(packet) else readCompleted = false
                    }
                    R.string.params_tab_ltft_title -> {
                        ltftStateHolder.reset()
                        val packet = pageReadGate.read(titleRes, onStarted = { readRevision = parameterWriteRevision }) {
                            readPacket<LtftParamPacket>(Task.Secu3ReadLtftParam)
                        }.first()
                        ltftStateHolder.receive(packet)
                    }
                    R.string.params_tab_fuel_injection_title -> {
                        fuelInjectionStateHolder.reset()
                        fwInfoPacket?.let { fuelInjectionStateHolder.updateFirmware(it.isFuelInjectEnabled, it.isSecu3T) }
                            ?: fuelInjectionStateHolder.updateFirmware(injectorEnabled = true, secu3t = false)
                        val packet = pageReadGate.read(titleRes, onStarted = { readRevision = parameterWriteRevision }) {
                            if (secu3ConnectionManager.fwInfo?.isFuelInjectEnabled != true) return@read null
                            readPacket<InjctrParPacket>(Task.Secu3ReadFuelInjectionParam)
                        }.first()
                        if (packet != null) {
                            fuelInjectionStateHolder.receive(packet)
                        } else readCompleted = false
                    }
                    R.string.params_tab_universal_outputs_title -> {
                        universalOutputsStateHolder.reset()
                        val packet = pageReadGate.read(titleRes, onStarted = { readRevision = parameterWriteRevision }) {
                            readPacket<UniOutParamPacket>(Task.Secu3ReadUniversalOutputsParam)
                        }.first()
                        universalOutputsStateHolder.onPacketReceived(packet)
                    }
                    R.string.params_tab_choke_control_title -> {
                        chokeControlStateHolder.reset()
                        val packet = pageReadGate.read(titleRes, onStarted = { readRevision = parameterWriteRevision }) {
                            readPacket<ChokeControlParPacket>(Task.Secu3ReadChokeControlParam)
                        }.first()
                        isSendAllowed = false
                        try {
                            chokeControlStateHolder.receive(packet)
                        } finally {
                            isSendAllowed = true
                        }
                    }
                    R.string.params_tab_gas_dose_title -> {
                        gasDoseStateHolder.reset()
                        gasDoseStateHolder.setFirmwareAvailable(fwInfoPacket?.isGdControlEnabled ?: true)
                        val packet = pageReadGate.read(titleRes, onStarted = { readRevision = parameterWriteRevision }) {
                            val firmware = secu3ConnectionManager.fwInfo ?: return@read null
                            if (!firmware.isGdControlEnabled) return@read null
                            readPacket<GasDoseParamPacket>(Task.Secu3ReadGasDoseParam)
                        }.first()
                        if (packet != null) {
                            isSendAllowed = false
                            try {
                                gasDoseStateHolder.receive(packet)
                            } finally {
                                isSendAllowed = true
                            }
                        } else readCompleted = false
                    }
                    R.string.params_tab_el_throttle_title -> {
                        dbwStateHolder.reset()
                        val packet = pageReadGate.read(titleRes, onStarted = { readRevision = parameterWriteRevision }) {
                            readPacket<DbwParamPacket>(Task.Secu3ReadDbwParam)
                        }.first()
                        dbwStateHolder.receive(packet)
                    }
                    R.string.params_tab_functions_title -> {
                        functionsStateHolder.reset()
                        val (namesPacket, funSetPacket) = pageReadGate.read(titleRes, onStarted = { readRevision = parameterWriteRevision }) {
                            val namesPacket = readParameterPacket(
                                sendRequest = { secu3ConnectionManager.sendNewTask(Task.Secu3ReadFnNameDat) },
                                restoreSensors = { secu3ConnectionManager.sendNewTask(Task.Secu3ReadSensors) },
                            ) { onSubscribed ->
                                var names: FnNameDatPacket? = null
                                secu3ConnectionManager.receivedPacketFlow(onSubscribed)
                                    .filter { it is FnNameDatPacket }
                                    .map { it as FnNameDatPacket }
                                    .first { packet ->
                                        val accumulated = names ?: packet.also {
                                            it.fnNameList = MutableList(packet.tablesNumber) { FnName(-1, "placeholder name") }
                                            names = it
                                        }
                                        accumulated.fnNameList[packet.fnName.index] = packet.fnName
                                        accumulated.isAllFnNamesReceived
                                    }
                                checkNotNull(names)
                            }
                            val funSetPacket = readPacket<FunSetParamPacket>(Task.Secu3ReadFunsetParam)
                            namesPacket to funSetPacket
                        }.first()
                        isSendAllowed = false
                        try {
                            functionsStateHolder.onNamesReceived(namesPacket)
                            functionsStateHolder.onPacketReceived(
                                received = funSetPacket,
                                loads = context.resources.getStringArray(R.array.load_measurement_items).toList(),
                                bars = context.resources.getStringArray(R.array.barocorr_items).toList(),
                                ve2 = context.resources.getStringArray(R.array.ve2_map_func_items).toList(),
                            )
                        } finally {
                            isSendAllowed = true
                        }
                    }
                    R.string.params_tab_miscellaneous_title -> {
                        miscellaneousStateHolder.reset()
                        val packet = pageReadGate.read(titleRes, onStarted = { readRevision = parameterWriteRevision }) {
                            readPacket<MiscellaneousParamPacket>(Task.Secu3ReadMiscellaneousParam)
                        }.first()
                        isSendAllowed = false
                        try {
                            miscellaneousStateHolder.onPacketReceived(packet)
                        } finally {
                            isSendAllowed = true
                        }
                    }
                    R.string.params_tab_security_title -> {
                        securityStateHolder.reset()
                        val packet = pageReadGate.read(titleRes, onStarted = { readRevision = parameterWriteRevision }) {
                            readPacket<SecurityParamPacket>(Task.Secu3ReadSecurityParam)
                        }.first()
                        isSendAllowed = false
                        try {
                            securityStateHolder.load(packet)
                        } finally {
                            isSendAllowed = true
                        }
                    }
                }
                if (readCompleted && parameterWriteRevision == readRevision) {
                    loadedPages += titleRes
                }
            } catch (_: TimeoutException) {
                failedPage = titleRes
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Log.w("ParamsViewModel", "Could not read parameter page $titleRes", error)
                failedPage = titleRes
            } finally {
                // A cancelled job may finish after a new view has started its replacement.
                if (pageReadJobs[titleRes] == coroutineContext[Job]) pageReadJobs.remove(titleRes)
            }
        }
    }

    val connectionStatusLiveData: LiveData<ConnectionState> = secu3ConnectionManager.connectionStateFlow.asLiveData()

    val fwInfoPacket: FirmwareInfoPacket?
        get() = secu3ConnectionManager.fwInfo



    fun sendPacket(packet: OutputPacket) {
        if (!isSendAllowed || !isConnected) {
            return
        }
        secu3ConnectionManager.sendOutPacket(packet)
        parameterWriteRevision++
        loadedPages.removeAll { it != activePageTitle }

        Toast.makeText(context, context.getString(R.string.packet_sent), Toast.LENGTH_SHORT).show()  // TODO: move this out
    }

    fun savePacket() {
        if (!isConnected || saveJob?.isActive == true) return
        saveJob = viewModelScope.launch {
            try {
                readParameterPacket(
                    sendRequest = {
                        secu3ConnectionManager.sendNewTask(Task.Secu3OpComSaveEeprom)
                        Toast.makeText(context, R.string.saving, Toast.LENGTH_SHORT).show()
                    },
                    restoreSensors = {},
                ) { onSubscribed ->
                    secu3ConnectionManager.receivedPacketFlow(onSubscribed)
                        .first { it is OpCompNc && it.isEepromParamSave }
                }
                Toast.makeText(context, R.string.packet_saved, Toast.LENGTH_SHORT).show()
            } catch (_: TimeoutException) {
                Toast.makeText(context, R.string.parameter_save_unconfirmed, Toast.LENGTH_LONG).show()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Log.w("ParamsViewModel", "Could not confirm parameter save", error)
                Toast.makeText(context, R.string.parameter_save_unconfirmed, Toast.LENGTH_LONG).show()
            } finally {
                if (saveJob == coroutineContext[Job]) saveJob = null
            }
        }
    }
}
