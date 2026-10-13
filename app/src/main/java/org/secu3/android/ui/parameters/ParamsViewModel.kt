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
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.secu3.android.R
import org.secu3.android.connection.ConnectionState
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
import org.secu3.android.ui.parameters.pages.compose.StarterStateHolder
import org.secu3.android.utils.Task
import javax.inject.Inject

@HiltViewModel
class ParamsViewModel @Inject constructor(
    @ApplicationContext private val context: Context, // TODO: move this out
    private val secu3ConnectionManager: Secu3ConnectionManager) : ViewModel() {

    var isSendAllowed: Boolean = false

    private val pageReadGate = ParameterPageReadGate()
    private val loadedPages = mutableSetOf<Int>()
    private val pageReadJobs = mutableMapOf<Int, Job>()

    internal val starterStateHolder by lazy {
        StarterStateHolder(::sendPacket) { isSendAllowed = true }
    }

    internal val anglesStateHolder by lazy {
        AnglesStateHolder(::sendPacket, { isSendAllowed = false }, { isSendAllowed = true })
    }

    internal fun activatePage(titleRes: Int) {
        if (!isConnected) return
        pageReadGate.activate(titleRes)
        if (titleRes == R.string.params_tab_start_title || titleRes == R.string.params_tab_angles_title) {
            loadViewModelPage(titleRes)
        }
    }

    internal fun deactivatePage() = pageReadGate.deactivate()

    internal val isConnected: Boolean
        get() = secu3ConnectionManager.isConnected

    internal fun clearPageState() {
        deactivatePage()
        pageReadJobs.values.forEach { it.cancel() }
        pageReadJobs.clear()
        loadedPages.clear()
        starterStateHolder.reset()
        anglesStateHolder.reset()
    }

    private suspend inline fun <reified T : InputPacket> readPacket(task: Task): T = readParameterPacket(
        sendRequest = { secu3ConnectionManager.sendNewTask(task) },
        restoreSensors = { secu3ConnectionManager.sendNewTask(Task.Secu3ReadSensors) },
        receive = { onSubscribed -> secu3ConnectionManager.receivedPacketFlow(onSubscribed).first { it is T } as T },
    )

    private fun loadViewModelPage(titleRes: Int) {
        if (titleRes in loadedPages || pageReadJobs[titleRes]?.isActive == true) return
        pageReadJobs[titleRes] = viewModelScope.launch {
            try {
                when (titleRes) {
                    R.string.params_tab_start_title -> {
                        starterStateHolder.reset()
                        val packet = pageReadGate.read(titleRes) {
                            readPacket<StarterParamPacket>(Task.Secu3ReadStarterParam)
                        }.first()
                        starterStateHolder.onPacketReceived(packet)
                    }
                    R.string.params_tab_angles_title -> {
                        anglesStateHolder.reset()
                        val packet = pageReadGate.read(titleRes) {
                            readPacket<AnglesParamPacket>(Task.Secu3ReadAnglesParam)
                        }.first()
                        anglesStateHolder.onPacketReceived(packet)
                    }
                }
                loadedPages += titleRes
            } finally {
                // A cancelled job may finish after a new view has started its replacement.
                if (pageReadJobs[titleRes] == coroutineContext[Job]) pageReadJobs.remove(titleRes)
            }
        }
    }

    val connectionStatusLiveData: LiveData<ConnectionState> = secu3ConnectionManager.connectionStateFlow.asLiveData()

    val fwInfoPacket: FirmwareInfoPacket?
        get() = secu3ConnectionManager.fwInfo
    val fwInfoLiveData: LiveData<FirmwareInfoPacket>
        get() = secu3ConnectionManager.firmwareLiveData

    val idlingLiveData: LiveData<IdlingParamPacket>
        get() = pageReadGate.read(R.string.params_tab_idling_title) {
            readPacket<IdlingParamPacket>(Task.Secu3ReadIdlingParam)
        }.filterNotNull().asLiveData(timeoutInMs = 0)

    val fnNameLiveData: LiveData<FnNameDatPacket>
        get() = pageReadGate.read(R.string.params_tab_functions_title) {
            readParameterPacket(
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
        }.filterNotNull().asLiveData(timeoutInMs = 0)

    val funsetLiveData: LiveData<FunSetParamPacket>
        get() = pageReadGate.read(R.string.params_tab_functions_title) {
            readPacket<FunSetParamPacket>(Task.Secu3ReadFunsetParam)
        }.filterNotNull().asLiveData(timeoutInMs = 0)

    val temperatureLiveData: LiveData<TemperatureParamPacket>
        get() = pageReadGate.read(R.string.params_tab_temperature_title) {
            readPacket<TemperatureParamPacket>(Task.Secu3ReadTemperatureParam)
        }.filterNotNull().asLiveData(timeoutInMs = 0)

    val carburLiveData: LiveData<CarburParamPacket>
        get() = pageReadGate.read(R.string.params_tab_fuel_cutoff_title) {
            readPacket<CarburParamPacket>(Task.Secu3ReadCarburParam)
        }.filterNotNull().asLiveData(timeoutInMs = 0)

    val adcCorrectionsLiveData: LiveData<AdcCorrectionsParamPacket>
        get() = pageReadGate.read(R.string.params_tab_adc_errors_corr_title) {
            readPacket<AdcCorrectionsParamPacket>(Task.Secu3ReadAdcErrorsCorrectionsParam)
        }.filterNotNull().asLiveData(timeoutInMs = 0)

    val ckpsLiveData: LiveData<CkpsParamPacket>
        get() = pageReadGate.read(R.string.params_tab_ckps_title) {
            readPacket<CkpsParamPacket>(Task.Secu3ReadCkpsParam)
        }.filterNotNull().asLiveData(timeoutInMs = 0)

    val knockLiveData: LiveData<KnockParamPacket>
        get() = pageReadGate.read(R.string.params_tab_knock_title) {
            readPacket<KnockParamPacket>(Task.Secu3ReadKnockParam)
        }.filterNotNull().asLiveData(timeoutInMs = 0)

    val miscellaneousLiveData: LiveData<MiscellaneousParamPacket>
        get() = pageReadGate.read(R.string.params_tab_miscellaneous_title) {
            readPacket<MiscellaneousParamPacket>(Task.Secu3ReadMiscellaneousParam)
        }.filterNotNull().asLiveData(timeoutInMs = 0)

    val chokeLiveData: LiveData<ChokeControlParPacket>
        get() = pageReadGate.read(R.string.params_tab_choke_control_title) {
            readPacket<ChokeControlParPacket>(Task.Secu3ReadChokeControlParam)
        }.filterNotNull().asLiveData(timeoutInMs = 0)

    val securityLiveData: LiveData<SecurityParamPacket>
        get() = pageReadGate.read(R.string.params_tab_security_title) {
            readPacket<SecurityParamPacket>(Task.Secu3ReadSecurityParam)
        }.filterNotNull().asLiveData(timeoutInMs = 0)

    val uniOutLiveData: LiveData<UniOutParamPacket>
        get() = pageReadGate.read(R.string.params_tab_universal_outputs_title) {
            readPacket<UniOutParamPacket>(Task.Secu3ReadUniversalOutputsParam)
        }.filterNotNull().asLiveData(timeoutInMs = 0)

    val fuelInjectionLiveData: LiveData<InjctrParPacket>
        get() = pageReadGate.read(R.string.params_tab_fuel_injection_title) {
            if (secu3ConnectionManager.fwInfo?.isFuelInjectEnabled != true) {
                return@read null
            }
            val packet = readPacket<InjctrParPacket>(Task.Secu3ReadFuelInjectionParam)

            packet.isAtMega644 = secu3ConnectionManager.fwInfo?.isATMEGA644 ?: true
            packet
        }.filterNotNull().asLiveData(timeoutInMs = 0)

    val lambdaLiveData: LiveData<LambdaParamPacket>
        get() = pageReadGate.read(R.string.params_tab_lambda_control_title) {
            val fw = secu3ConnectionManager.fwInfo ?: return@read null
            if (fw.isFuelInjectEnabled.not() && fw.isCarbAfrEnabled.not() && fw.isGdControlEnabled.not()) {
                return@read null
            }
            readPacket<LambdaParamPacket>(Task.Secu3ReadLambdaParam)
        }.filterNotNull().asLiveData(timeoutInMs = 0)

    val accelerationLiveData: LiveData<AccelerationParamPacket>
        get() = pageReadGate.read(R.string.params_tab_acceleration_title) {
            val fw = secu3ConnectionManager.fwInfo ?: return@read null
            if (fw.isFuelInjectEnabled.not() && fw.isGdControlEnabled.not()) {
                return@read null
            }
            readPacket<AccelerationParamPacket>(Task.Secu3ReadAccelerationParam)
        }.filterNotNull().asLiveData(timeoutInMs = 0)

    val gasDoseLiveData: LiveData<GasDoseParamPacket>
        get() = pageReadGate.read(R.string.params_tab_gas_dose_title) {
            val fw = secu3ConnectionManager.fwInfo ?: return@read null
            if (fw.isGdControlEnabled.not()) {
                return@read null
            }

            readPacket<GasDoseParamPacket>(Task.Secu3ReadGasDoseParam)
        }.filterNotNull().asLiveData(timeoutInMs = 0)

    val ltftLiveData: LiveData<LtftParamPacket>
        get() = pageReadGate.read(R.string.params_tab_ltft_title) {
            readPacket<LtftParamPacket>(Task.Secu3ReadLtftParam)
        }.filterNotNull().asLiveData(timeoutInMs = 0)

    val dbwLiveData: LiveData<DbwParamPacket>
        get() = pageReadGate.read(R.string.params_tab_el_throttle_title) {
            readPacket<DbwParamPacket>(Task.Secu3ReadDbwParam)
        }.filterNotNull().asLiveData(timeoutInMs = 0)



    fun sendPacket(packet: OutputPacket) {
        if (isSendAllowed.not()) {
            return
        }
        secu3ConnectionManager.sendOutPacket(packet)

        Toast.makeText(context, context.getString(R.string.packet_sent), Toast.LENGTH_SHORT).show()  // TODO: move this out
    }

    fun savePacket() {
        viewModelScope.launch(Dispatchers.IO) {
            val opCompNc = secu3ConnectionManager.receivedPacketFlow.filter { it is OpCompNc }
                .map { it as OpCompNc }
                .filter { it.isEepromParamSave }
                .first()

            if (opCompNc.isEepromParamSave) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, context.getString(R.string.packet_saved), Toast.LENGTH_SHORT).show()  // TODO: move this out
                }
            }
        }

        Toast.makeText(context, context.getString(R.string.saving), Toast.LENGTH_SHORT).show()  // TODO: move this out
        
        secu3ConnectionManager.sendNewTask(Task.Secu3OpComSaveEeprom)
    }
}
