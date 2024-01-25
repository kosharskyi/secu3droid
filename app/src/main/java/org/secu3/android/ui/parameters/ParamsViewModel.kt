/*
 *    SecuDroid  - An open source, free manager for SECU-3 engine control unit
 *    Copyright (C) 2024 Vitaliy O. Kosharskyi. Ukraine, Kyiv
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

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.sample
import org.secu3.android.Secu3Repository
import org.secu3.android.models.packets.BaseOutputPacket
import org.secu3.android.models.packets.FirmwareInfoPacket
import org.secu3.android.models.packets.FnName
import org.secu3.android.models.packets.FnNameDatPacket
import org.secu3.android.models.packets.params.AccelerationParamPacket
import org.secu3.android.models.packets.params.AdcCorrectionsParamPacket
import org.secu3.android.models.packets.params.AnglesParamPacket
import org.secu3.android.models.packets.params.CarburParamPacket
import org.secu3.android.models.packets.params.ChokeControlParPacket
import org.secu3.android.models.packets.params.CkpsParamPacket
import org.secu3.android.models.packets.params.FunSetParamPacket
import org.secu3.android.models.packets.params.GasDoseParamPacket
import org.secu3.android.models.packets.params.IdlingParamPacket
import org.secu3.android.models.packets.params.InjctrParPacket
import org.secu3.android.models.packets.params.KnockParamPacket
import org.secu3.android.models.packets.params.LambdaParamPacket
import org.secu3.android.models.packets.params.MiscellaneousParamPacket
import org.secu3.android.models.packets.params.SecurityParamPacket
import org.secu3.android.models.packets.params.StarterParamPacket
import org.secu3.android.models.packets.params.TemperatureParamPacket
import org.secu3.android.models.packets.params.UniOutParamPacket
import org.secu3.android.utils.LifeTimePrefs
import org.secu3.android.utils.Task
import javax.inject.Inject

@HiltViewModel
class ParamsViewModel @Inject constructor(private val secu3Repository: Secu3Repository, private val prefs: LifeTimePrefs) : ViewModel() {

    val connectionStatusLiveData: LiveData<Boolean>
        get() = secu3Repository.connectionStatusLiveData

    val fwInfoLiveData: LiveData<FirmwareInfoPacket>
        get() = secu3Repository.firmwareLiveData

    val starterLiveData: LiveData<StarterParamPacket>
        get() = flow {
            secu3Repository.sendNewTask(Task.Secu3ReadStarterParam)
            secu3Repository.receivedPacketFlow
                .filter { it is StarterParamPacket }
                .map { it as StarterParamPacket }
                .sample(1000)
                .collect {
                    emit(it)
                    secu3Repository.sendNewTask(Task.Secu3ReadSensors)
                }
        }.asLiveData()

    val anglesLiveData: LiveData<AnglesParamPacket>
        get() = flow {
            secu3Repository.sendNewTask(Task.Secu3ReadAnglesParam)
            secu3Repository.receivedPacketFlow
                .filter { it is AnglesParamPacket }
                .map { it as AnglesParamPacket }
                .sample(1000)
                .collect {
                    emit(it)
                    secu3Repository.sendNewTask(Task.Secu3ReadSensors)
                }
        }.asLiveData()

    val idlingLiveData: LiveData<IdlingParamPacket>
        get() = flow {
            secu3Repository.sendNewTask(Task.Secu3ReadIdlingParam)
            secu3Repository.receivedPacketFlow
                .filter { it is IdlingParamPacket }
                .map { it as IdlingParamPacket }
                .sample(1000)
                .collect {
                    emit(it)
                    secu3Repository.sendNewTask(Task.Secu3ReadSensors)
                }
        }.asLiveData()

    val fnNameLiveData: LiveData<FnNameDatPacket>
        get() = flow {
            var fnNameDatPacket: FnNameDatPacket? = null

            secu3Repository.sendNewTask(Task.Secu3ReadFnNameDat)
            while (fnNameDatPacket?.isAllFnNamesReceived != true) {

                val packet = secu3Repository.receivedPacketFlow.first { it is FnNameDatPacket } as FnNameDatPacket

                fnNameDatPacket = fnNameDatPacket?: packet.also {
                    it.fnNameList = MutableList(packet.tablesNumber) { FnName(-1, "placeholder name") }
                }

                fnNameDatPacket.fnNameList.set(packet.fnName.index, packet.fnName)
            }
            emit(fnNameDatPacket)
            secu3Repository.sendNewTask(Task.Secu3ReadFunsetParam)
        }.asLiveData()

    val funsetLiveData: LiveData<FunSetParamPacket>
        get() = flow {
            secu3Repository.receivedPacketFlow
                .filter { it is FunSetParamPacket }
                .map { it as FunSetParamPacket }
                .sample(1000)
                .collect {
                    emit(it)
                    secu3Repository.sendNewTask(Task.Secu3ReadSensors)
                }
        }.asLiveData()

    val temperatureLiveData: LiveData<TemperatureParamPacket>
        get() = flow {
            secu3Repository.sendNewTask(Task.Secu3ReadTemperatureParam)
            secu3Repository.receivedPacketFlow
                .filter { it is TemperatureParamPacket }
                .map { it as TemperatureParamPacket }
                .sample(1000)
                .collect {
                    emit(it)
                    secu3Repository.sendNewTask(Task.Secu3ReadSensors)
                }
        }.asLiveData()

    val carburLiveData: LiveData<CarburParamPacket>
        get() = flow {
            secu3Repository.sendNewTask(Task.Secu3ReadCarburParam)
            secu3Repository.receivedPacketFlow
                .filter { it is CarburParamPacket }
                .map { it as CarburParamPacket }
                .sample(1000)
                .collect {
                    emit(it)
                    secu3Repository.sendNewTask(Task.Secu3ReadSensors)
                }
        }.asLiveData()

    val adcCorrectionsLiveData: LiveData<AdcCorrectionsParamPacket>
        get() = flow {
            secu3Repository.sendNewTask(Task.Secu3ReadAdcErrorsCorrectionsParam)
            secu3Repository.receivedPacketFlow
                .filter { it is AdcCorrectionsParamPacket }
                .map { it as AdcCorrectionsParamPacket }
                .sample(1000)
                .collect {
                    emit(it)
                    secu3Repository.sendNewTask(Task.Secu3ReadSensors)
                }
        }.asLiveData()

    val ckpsLiveData: LiveData<CkpsParamPacket>
        get() = flow {
            secu3Repository.sendNewTask(Task.Secu3ReadCkpsParam)
            secu3Repository.receivedPacketFlow
                .filter { it is CkpsParamPacket }
                .map { it as CkpsParamPacket }
                .sample(1000)
                .collect {
                    emit(it)
                    secu3Repository.sendNewTask(Task.Secu3ReadSensors)
                }
        }.asLiveData()

    val knockLiveData: LiveData<KnockParamPacket>
        get() = flow {
            secu3Repository.sendNewTask(Task.Secu3ReadKnockParam)
            secu3Repository.receivedPacketFlow
                .filter { it is KnockParamPacket }
                .map { it as KnockParamPacket }
                .sample(1000)
                .collect {
                    emit(it)
                    secu3Repository.sendNewTask(Task.Secu3ReadSensors)
                }
        }.asLiveData()

    val miscellaneousLiveData: LiveData<MiscellaneousParamPacket>
        get() = flow {
            secu3Repository.sendNewTask(Task.Secu3ReadMiscellaneousParam)
            secu3Repository.receivedPacketFlow
                .filter { it is MiscellaneousParamPacket }
                .map { it as MiscellaneousParamPacket }
                .sample(1000)
                .collect {
                    emit(it)
                    secu3Repository.sendNewTask(Task.Secu3ReadSensors)
                }
        }.asLiveData()

    val chokeLiveData: LiveData<ChokeControlParPacket>
        get() = flow {
            secu3Repository.sendNewTask(Task.Secu3ReadChokeControlParam)
            secu3Repository.receivedPacketFlow
                .filter { it is ChokeControlParPacket }
                .map { it as ChokeControlParPacket }
                .sample(1000)
                .collect {
                    emit(it)
                }
        }.asLiveData()

    val securityLiveData: LiveData<SecurityParamPacket>
        get() = flow {
            secu3Repository.sendNewTask(Task.Secu3ReadSecurityParam)
            secu3Repository.receivedPacketFlow
                .filter { it is SecurityParamPacket }
                .map { it as SecurityParamPacket }
                .sample(1000)
                .collect {
                    emit(it)
                    secu3Repository.sendNewTask(Task.Secu3ReadSensors)
                }
        }.asLiveData()

    val uniOutLiveData: LiveData<UniOutParamPacket>
        get() = flow {
            secu3Repository.sendNewTask(Task.Secu3ReadUniversalOutputsParam)
            secu3Repository.receivedPacketFlow
                .filter { it is UniOutParamPacket }
                .map { it as UniOutParamPacket }
                .sample(1000)
                .collect {
                    it.speedSensorPulses = prefs.speedPulses
                    emit(it)
                    secu3Repository.sendNewTask(Task.Secu3ReadSensors)
                }
        }.asLiveData()

    val fuelInjectionLiveData: LiveData<InjctrParPacket>
        get() = flow {
            if (secu3Repository.fwInfo?.isFuelInjectEnabled != true) {
                return@flow
            }
            secu3Repository.sendNewTask(Task.Secu3ReadFuelInjectionParam)
            secu3Repository.receivedPacketFlow
                .filter { it is InjctrParPacket }
                .map { it as InjctrParPacket }
                .sample(1000)
                .collect {
                    it.isAtMega644 = secu3Repository.fwInfo?.isATMEGA644 ?: true
                    emit(it)
                    secu3Repository.sendNewTask(Task.Secu3ReadSensors)
                }
        }.asLiveData()
    val lambdaLiveData: LiveData<LambdaParamPacket>
        get() = flow {
            val fw = secu3Repository.fwInfo ?: return@flow
            if (fw.isFuelInjectEnabled.not() || fw.isCarbAfrEnabled.not() || fw.isGdControlEnabled.not()) {
                return@flow
            }
            secu3Repository.sendNewTask(Task.Secu3ReadLambdaParam)
            secu3Repository.receivedPacketFlow
                .filter { it is LambdaParamPacket }
                .map { it as LambdaParamPacket }
                .sample(1000)
                .collect {
                    emit(it)
                    secu3Repository.sendNewTask(Task.Secu3ReadSensors)
                }
        }.asLiveData()
    val accelerationLiveData: LiveData<AccelerationParamPacket>
        get() = flow {
            val fw = secu3Repository.fwInfo ?: return@flow
            if (fw.isFuelInjectEnabled.not() || fw.isGdControlEnabled.not()) {
                return@flow
            }
            secu3Repository.sendNewTask(Task.Secu3ReadAccelerationParam)
            secu3Repository.receivedPacketFlow
                .filter { it is AccelerationParamPacket }
                .map { it as AccelerationParamPacket }
                .sample(1000)
                .collect {
                    emit(it)
                    secu3Repository.sendNewTask(Task.Secu3ReadSensors)
                }
        }.asLiveData()

    val gasDoseLiveData: LiveData<GasDoseParamPacket>
        get() = flow {
            secu3Repository.sendNewTask(Task.Secu3ReadGasDoseParam)
            secu3Repository.receivedPacketFlow
                .filter { it is GasDoseParamPacket }
                .map { it as GasDoseParamPacket }
                .sample(1000)
                .collect {
                    emit(it)
                    secu3Repository.sendNewTask(Task.Secu3ReadSensors)
                }
        }.asLiveData()


    fun sendPacket(packet: BaseOutputPacket) {
        secu3Repository.sendOutPacket(packet)
    }

}