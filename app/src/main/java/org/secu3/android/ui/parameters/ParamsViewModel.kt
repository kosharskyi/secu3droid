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
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.secu3.android.R
import org.secu3.android.connection.ConnectionState
import org.secu3.android.connection.Secu3ConnectionManager
import org.secu3.android.models.FnName
import org.secu3.android.models.packets.base.OutputPacket
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
import org.secu3.android.utils.Task
import javax.inject.Inject

@HiltViewModel
class ParamsViewModel @Inject constructor(
    @ApplicationContext private val context: Context, // TODO: move this out
    private val secu3ConnectionManager: Secu3ConnectionManager) : ViewModel() {

    var isSendAllowed: Boolean = false

    val connectionStatusLiveData: LiveData<ConnectionState>
        get() = secu3ConnectionManager.connectionStateFlow.asLiveData()

    val fwInfoPacket: FirmwareInfoPacket?
        get() = secu3ConnectionManager.fwInfo
    val fwInfoLiveData: LiveData<FirmwareInfoPacket>
        get() = secu3ConnectionManager.firmwareLiveData

    val starterLiveData: LiveData<StarterParamPacket>
        get() = flow {
            secu3ConnectionManager.sendNewTask(Task.Secu3ReadStarterParam)
            val packet = secu3ConnectionManager.receivedPacketFlow.first { it is StarterParamPacket } as StarterParamPacket

            emit(packet)
            secu3ConnectionManager.sendNewTask(Task.Secu3ReadSensors)
        }.asLiveData()

    val anglesLiveData: LiveData<AnglesParamPacket>
        get() = flow {
            secu3ConnectionManager.sendNewTask(Task.Secu3ReadAnglesParam)
            val packet = secu3ConnectionManager.receivedPacketFlow.first { it is AnglesParamPacket } as AnglesParamPacket

            emit(packet)
            secu3ConnectionManager.sendNewTask(Task.Secu3ReadSensors)
        }.asLiveData()

    val idlingLiveData: LiveData<IdlingParamPacket>
        get() = flow {
            secu3ConnectionManager.sendNewTask(Task.Secu3ReadIdlingParam)
            val packet = secu3ConnectionManager.receivedPacketFlow.first { it is IdlingParamPacket } as IdlingParamPacket

            emit(packet)
            secu3ConnectionManager.sendNewTask(Task.Secu3ReadSensors)
        }.asLiveData()

    val fnNameLiveData: LiveData<FnNameDatPacket>
        get() = flow {
            var fnNameDatPacket: FnNameDatPacket? = null

            secu3ConnectionManager.sendNewTask(Task.Secu3ReadFnNameDat)
            while (fnNameDatPacket?.isAllFnNamesReceived != true) {

                val packet = secu3ConnectionManager.receivedPacketFlow.first { it is FnNameDatPacket } as FnNameDatPacket

                fnNameDatPacket = fnNameDatPacket ?: packet.also {
                    it.fnNameList = MutableList(packet.tablesNumber) { FnName(-1, "placeholder name") }
                }

                fnNameDatPacket.fnNameList[packet.fnName.index] = packet.fnName
            }
            emit(fnNameDatPacket)
        }.asLiveData()

    val funsetLiveData: LiveData<FunSetParamPacket>
        get() = flow {
            secu3ConnectionManager.sendNewTask(Task.Secu3ReadFunsetParam)
            val packet = secu3ConnectionManager.receivedPacketFlow.first { it is FunSetParamPacket } as FunSetParamPacket

            emit(packet)
            secu3ConnectionManager.sendNewTask(Task.Secu3ReadSensors)
        }.asLiveData()

    val temperatureLiveData: LiveData<TemperatureParamPacket>
        get() = flow {
            secu3ConnectionManager.sendNewTask(Task.Secu3ReadTemperatureParam)
            val packet = secu3ConnectionManager.receivedPacketFlow.first { it is TemperatureParamPacket } as TemperatureParamPacket

            emit(packet)
            secu3ConnectionManager.sendNewTask(Task.Secu3ReadSensors)
        }.asLiveData()

    val carburLiveData: LiveData<CarburParamPacket>
        get() = flow {
            secu3ConnectionManager.sendNewTask(Task.Secu3ReadCarburParam)
            val packet = secu3ConnectionManager.receivedPacketFlow.first { it is CarburParamPacket } as CarburParamPacket

            emit(packet)
            secu3ConnectionManager.sendNewTask(Task.Secu3ReadSensors)
        }.asLiveData()

    val adcCorrectionsLiveData: LiveData<AdcCorrectionsParamPacket>
        get() = flow {
            secu3ConnectionManager.sendNewTask(Task.Secu3ReadAdcErrorsCorrectionsParam)
            val packet = secu3ConnectionManager.receivedPacketFlow.first { it is AdcCorrectionsParamPacket } as AdcCorrectionsParamPacket

            emit(packet)
            secu3ConnectionManager.sendNewTask(Task.Secu3ReadSensors)
        }.asLiveData()

    val ckpsLiveData: LiveData<CkpsParamPacket>
        get() = flow {
            secu3ConnectionManager.sendNewTask(Task.Secu3ReadCkpsParam)
            val packet = secu3ConnectionManager.receivedPacketFlow.first { it is CkpsParamPacket } as CkpsParamPacket

            emit(packet)
            secu3ConnectionManager.sendNewTask(Task.Secu3ReadSensors)
        }.asLiveData()

    val knockLiveData: LiveData<KnockParamPacket>
        get() = flow {
            secu3ConnectionManager.sendNewTask(Task.Secu3ReadKnockParam)
            val packet = secu3ConnectionManager.receivedPacketFlow.first { it is KnockParamPacket } as KnockParamPacket

            emit(packet)
            secu3ConnectionManager.sendNewTask(Task.Secu3ReadSensors)
        }.asLiveData()

    val miscellaneousLiveData: LiveData<MiscellaneousParamPacket>
        get() = flow {
            secu3ConnectionManager.sendNewTask(Task.Secu3ReadMiscellaneousParam)
            val packet = secu3ConnectionManager.receivedPacketFlow.first { it is MiscellaneousParamPacket } as MiscellaneousParamPacket

            emit(packet)
            secu3ConnectionManager.sendNewTask(Task.Secu3ReadSensors)
        }.asLiveData()

    val chokeLiveData: LiveData<ChokeControlParPacket>
        get() = flow {
            secu3ConnectionManager.sendNewTask(Task.Secu3ReadChokeControlParam)
            val packet = secu3ConnectionManager.receivedPacketFlow.first { it is ChokeControlParPacket } as ChokeControlParPacket

            emit(packet)
            secu3ConnectionManager.sendNewTask(Task.Secu3ReadSensors)
        }.asLiveData()

    val securityLiveData: LiveData<SecurityParamPacket>
        get() = flow {
            secu3ConnectionManager.sendNewTask(Task.Secu3ReadSecurityParam)
            val packet = secu3ConnectionManager.receivedPacketFlow.first { it is SecurityParamPacket } as SecurityParamPacket

            emit(packet)
            secu3ConnectionManager.sendNewTask(Task.Secu3ReadSensors)
        }.asLiveData()

    val uniOutLiveData: LiveData<UniOutParamPacket>
        get() = flow {
            secu3ConnectionManager.sendNewTask(Task.Secu3ReadUniversalOutputsParam)
            val packet = secu3ConnectionManager.receivedPacketFlow.first { it is UniOutParamPacket } as UniOutParamPacket

            emit(packet)
            secu3ConnectionManager.sendNewTask(Task.Secu3ReadSensors)
        }.asLiveData()

    val fuelInjectionLiveData: LiveData<InjctrParPacket>
        get() = flow {
            if (secu3ConnectionManager.fwInfo?.isFuelInjectEnabled != true) {
                return@flow
            }
            secu3ConnectionManager.sendNewTask(Task.Secu3ReadFuelInjectionParam)
            val packet = secu3ConnectionManager.receivedPacketFlow.first { it is InjctrParPacket } as InjctrParPacket

            packet.isAtMega644 = secu3ConnectionManager.fwInfo?.isATMEGA644 ?: true
            emit(packet)
            secu3ConnectionManager.sendNewTask(Task.Secu3ReadSensors)
        }.asLiveData()

    val lambdaLiveData: LiveData<LambdaParamPacket>
        get() = flow {
            val fw = secu3ConnectionManager.fwInfo ?: return@flow
            if (fw.isFuelInjectEnabled.not() && fw.isCarbAfrEnabled.not() && fw.isGdControlEnabled.not()) {
                return@flow
            }
            secu3ConnectionManager.sendNewTask(Task.Secu3ReadLambdaParam)
            val packet = secu3ConnectionManager.receivedPacketFlow
                .filter { it is LambdaParamPacket }
                .map { it as LambdaParamPacket }
                .first()

            emit(packet)
            secu3ConnectionManager.sendNewTask(Task.Secu3ReadSensors)
        }.asLiveData()

    val accelerationLiveData: LiveData<AccelerationParamPacket>
        get() = flow {
            val fw = secu3ConnectionManager.fwInfo ?: return@flow
            if (fw.isFuelInjectEnabled.not() && fw.isGdControlEnabled.not()) {
                return@flow
            }
            secu3ConnectionManager.sendNewTask(Task.Secu3ReadAccelerationParam)
            val packet = secu3ConnectionManager.receivedPacketFlow
                .filter { it is AccelerationParamPacket }
                .map { it as AccelerationParamPacket }
                .first()

            emit(packet)
            secu3ConnectionManager.sendNewTask(Task.Secu3ReadSensors)
        }.asLiveData()

    val gasDoseLiveData: LiveData<GasDoseParamPacket>
        get() = flow {
            val fw = secu3ConnectionManager.fwInfo ?: return@flow
            if (fw.isGdControlEnabled.not()) {
                return@flow
            }

            secu3ConnectionManager.sendNewTask(Task.Secu3ReadGasDoseParam)
            val packet = secu3ConnectionManager.receivedPacketFlow
                .filter { it is GasDoseParamPacket }
                .map { it as GasDoseParamPacket }
                .first()

            emit(packet)
            secu3ConnectionManager.sendNewTask(Task.Secu3ReadSensors)
        }.asLiveData()

    val ltftLiveData: LiveData<LtftParamPacket>
        get() = flow {
            secu3ConnectionManager.sendNewTask(Task.Secu3ReadLtftParam)
            val packet = secu3ConnectionManager.receivedPacketFlow.first { it is LtftParamPacket } as LtftParamPacket

            emit(packet)
            secu3ConnectionManager.sendNewTask(Task.Secu3ReadSensors)
        }.asLiveData()

    val dbwLiveData: LiveData<DbwParamPacket>
        get() = flow {
            secu3ConnectionManager.sendNewTask(Task.Secu3ReadDbwParam)
            val packet = secu3ConnectionManager.receivedPacketFlow.first { it is DbwParamPacket } as DbwParamPacket

            emit(packet)
            secu3ConnectionManager.sendNewTask(Task.Secu3ReadSensors)
        }.asLiveData()



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
