/* SecuDroid  - An open source, free manager for SECU-3 engine control unit
   Copyright (C) 2020 Vitaliy O. Kosharskiy. Ukraine, Kharkiv

   SECU-3  - An open source, free engine control unit
   Copyright (C) 2007 Alexey A. Shabelnikov. Ukraine, Kyiv

   This program is free software: you can redistribute it and/or modify
   it under the terms of the GNU General Public License as published by
   the Free Software Foundation, either version 3 of the License, or
   (at your option) any later version.

   This program is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.

   You should have received a copy of the GNU General Public License
   along with this program.  If not, see <http://www.gnu.org/licenses/>.

   contacts:
              http://secu-3.org
              email: vetalkosharskiy@gmail.com
*/
package org.secu3.android.ui.parameters

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.secu3.android.Secu3Repository
import org.secu3.android.models.packets.BaseOutputPacket
import org.secu3.android.models.packets.FirmwareInfoPacket
import org.secu3.android.models.packets.FnName
import org.secu3.android.models.packets.FnNameDatPacket
import org.secu3.android.models.packets.params.*
import org.secu3.android.utils.LifeTimePrefs
import org.secu3.android.utils.Task
import javax.inject.Inject

@HiltViewModel
class ParamsViewModel @Inject constructor(private val secu3Repository: Secu3Repository, private val prefs: LifeTimePrefs) : ViewModel() {

    val connectionStatusLiveData: LiveData<Boolean>
        get() = secu3Repository.connectionStatusLiveData

    var mFnNameDatPacket: FnNameDatPacket? = null


    private val mFnNameLiveData = MutableLiveData<FnNameDatPacket>()
    val fnNameLiveData: LiveData<FnNameDatPacket>
        get() = mFnNameLiveData

    val fwInfoLiveData: LiveData<FirmwareInfoPacket>
        get() = secu3Repository.firmwareLiveData

    private val mStarterLiveData = MutableLiveData<StarterParamPacket>()
    val starterLiveData: LiveData<StarterParamPacket>
        get() = mStarterLiveData

    private val mAnglesLiveData = MutableLiveData<AnglesParamPacket>()
    val anglesLiveData: LiveData<AnglesParamPacket>
        get() = mAnglesLiveData

    private val mIdlingLiveData = MutableLiveData<IdlingParamPacket>()
    val idlingLiveData: LiveData<IdlingParamPacket>
        get() = mIdlingLiveData

    private val mFunsetLiveData = MutableLiveData<FunSetParamPacket>()
    val funsetLiveData: LiveData<FunSetParamPacket>
        get() = mFunsetLiveData

    private val mTemperatureLiveData = MutableLiveData<TemperatureParamPacket>()
    val temperatureLiveData: LiveData<TemperatureParamPacket>
        get() = mTemperatureLiveData

    private val mCarburLiveData = MutableLiveData<CarburParamPacket>()
    val carburLiveData: LiveData<CarburParamPacket>
        get() = mCarburLiveData

    private val mAdcCorrectionsLiveData = MutableLiveData<AdcCorrectionsParamPacket>()
    val adcCorrectionsLiveData: LiveData<AdcCorrectionsParamPacket>
        get() = mAdcCorrectionsLiveData

    private val mCkpsLiveData = MutableLiveData<CkpsParamPacket>()
    val ckpsLiveData: LiveData<CkpsParamPacket>
        get() = mCkpsLiveData

    private val mKnockLiveData = MutableLiveData<KnockParamPacket>()
    val knockLiveData: LiveData<KnockParamPacket>
        get() = mKnockLiveData

    private val mMiscellaneousLiveData = MutableLiveData<MiscellaneousParamPacket>()
    val miscellaneousLiveData: LiveData<MiscellaneousParamPacket>
        get() = mMiscellaneousLiveData

    private val mChokeLiveData = MutableLiveData<ChokeControlParPacket>()
    val chokeLiveData: LiveData<ChokeControlParPacket>
        get() = mChokeLiveData

    private val mSecurityLiveData = MutableLiveData<SecurityParamPacket>()
    val securityLiveData: LiveData<SecurityParamPacket>
        get() = mSecurityLiveData

    private val mUniOutLiveData = MutableLiveData<UniOutParamPacket>()
    val uniOutLiveData: LiveData<UniOutParamPacket>
        get() = mUniOutLiveData

    private val mFuelInjectionLiveData = MutableLiveData<InjctrParPacket>()
    val fuelInjectionLiveData: LiveData<InjctrParPacket>
        get() = mFuelInjectionLiveData

    private val mLambdaLiveData = MutableLiveData<LambdaParamPacket>()
    val lambdaLiveData: LiveData<LambdaParamPacket>
        get() = mLambdaLiveData

    private val mAccelerationLiveData = MutableLiveData<AccelerationParamPacket>()
    val accelerationLiveData: LiveData<AccelerationParamPacket>
        get() = mAccelerationLiveData

    private val mGasDoseLiveData = MutableLiveData<GasDoseParamPacket>()
    val gasDoseLiveData: LiveData<GasDoseParamPacket>
        get() = mGasDoseLiveData


    fun sendPacket(packet: BaseOutputPacket) {
        secu3Repository.sendOutPacket(packet)
    }







    private suspend fun fnNameDatReceive() = withContext(Dispatchers.IO) {
        while (mFnNameDatPacket?.isAllFnNamesReceived != true) {

            val packet = secu3Repository.receivedPacketLiveData.first { it is FnNameDatPacket } as FnNameDatPacket

            mFnNameDatPacket = mFnNameDatPacket?: packet.also {
                it.fnNameList = MutableList(packet.tablesNumber) { FnName(-1, "placeholder name") }
            }

            mFnNameDatPacket?.fnNameList?.set(packet.fnName.index, packet.fnName)
        }
    }

    init {

        secu3Repository.sendNewTask(Task.Secu3ReadFnNameDat)

        viewModelScope.launch {

            fnNameDatReceive()

            mFnNameLiveData.value = mFnNameDatPacket
            secu3Repository.sendNewTask(Task.Secu3ReadStarterParam)



            mStarterLiveData.value = secu3Repository.receivedPacketLiveData.first { it is StarterParamPacket } as StarterParamPacket
            secu3Repository.sendNewTask(Task.Secu3ReadAnglesParam)

            mAnglesLiveData.value = secu3Repository.receivedPacketLiveData.first { it is AnglesParamPacket } as AnglesParamPacket
            secu3Repository.sendNewTask(Task.Secu3ReadIdlingParam)

            mIdlingLiveData.value = secu3Repository.receivedPacketLiveData.first { it is IdlingParamPacket } as IdlingParamPacket
            secu3Repository.sendNewTask(Task.Secu3ReadFunsetParam)

            mFunsetLiveData.value = secu3Repository.receivedPacketLiveData.first { it is FunSetParamPacket } as FunSetParamPacket
            secu3Repository.sendNewTask(Task.Secu3ReadTemperatureParam)

            mTemperatureLiveData.value = secu3Repository.receivedPacketLiveData.first { it is TemperatureParamPacket } as TemperatureParamPacket
            secu3Repository.sendNewTask(Task.Secu3ReadCarburParam)

            mCarburLiveData.value = secu3Repository.receivedPacketLiveData.first { it is CarburParamPacket } as CarburParamPacket
            secu3Repository.sendNewTask(Task.Secu3ReadAdcErrorsCorrectionsParam)

            mAdcCorrectionsLiveData.value = secu3Repository.receivedPacketLiveData.first { it is AdcCorrectionsParamPacket } as AdcCorrectionsParamPacket
            secu3Repository.sendNewTask(Task.Secu3ReadCkpsParam)

            mCkpsLiveData.value = secu3Repository.receivedPacketLiveData.first { it is CkpsParamPacket } as CkpsParamPacket
            secu3Repository.sendNewTask(Task.Secu3ReadKnockParam)

            mKnockLiveData.value = secu3Repository.receivedPacketLiveData.first { it is KnockParamPacket } as KnockParamPacket
            secu3Repository.sendNewTask(Task.Secu3ReadMiscellaneousParam)

            mMiscellaneousLiveData.value = secu3Repository.receivedPacketLiveData.first { it is MiscellaneousParamPacket } as MiscellaneousParamPacket
            secu3Repository.sendNewTask(Task.Secu3ReadChokeControlParam)

            mChokeLiveData.value = secu3Repository.receivedPacketLiveData.first { it is ChokeControlParPacket } as ChokeControlParPacket
            secu3Repository.sendNewTask(Task.Secu3ReadSecurityParam)

            mSecurityLiveData.value = secu3Repository.receivedPacketLiveData.first { it is SecurityParamPacket } as SecurityParamPacket
            secu3Repository.sendNewTask(Task.Secu3ReadUniversalOutputsParam)



            mUniOutLiveData.value = (secu3Repository.receivedPacketLiveData.first { it is UniOutParamPacket } as UniOutParamPacket).also {
                it.speedSensorPulses = prefs.speedPulses
            }

            secu3Repository.fwInfo?.let {
                if (it.isFuelInjectEnabled) {
                    secu3Repository.sendNewTask(Task.Secu3ReadFuelInjectionParam)
                } else if (it.isFuelInjectEnabled || it.isCarbAfrEnabled || it.isGdControlEnabled) {
                    secu3Repository.sendNewTask(Task.Secu3ReadLambdaParam)
                } else if (it.isFuelInjectEnabled || it.isGdControlEnabled) {
                    secu3Repository.sendNewTask(Task.Secu3ReadAccelerationParam)
                } else {
                    secu3Repository.sendNewTask(Task.Secu3ReadSensors)
                }
            }



            mFuelInjectionLiveData.value = (secu3Repository.receivedPacketLiveData.first { it is InjctrParPacket } as InjctrParPacket).also {
                it.isAtMega644 = secu3Repository.fwInfo?.isATMEGA644 ?: true
            }

            secu3Repository.fwInfo?.let {
                if (it.isFuelInjectEnabled || it.isCarbAfrEnabled || it.isGdControlEnabled) {
                    secu3Repository.sendNewTask(Task.Secu3ReadLambdaParam)
                } else if (it.isFuelInjectEnabled || it.isGdControlEnabled) {
                    secu3Repository.sendNewTask(Task.Secu3ReadAccelerationParam)
                } else {
                    secu3Repository.sendNewTask(Task.Secu3ReadSensors)
                }
            }


            mLambdaLiveData.value = secu3Repository.receivedPacketLiveData.first { it is LambdaParamPacket } as LambdaParamPacket
            secu3Repository.sendNewTask(Task.Secu3ReadAccelerationParam)


            mAccelerationLiveData.value = secu3Repository.receivedPacketLiveData.first { it is AccelerationParamPacket } as AccelerationParamPacket
            secu3Repository.fwInfo?.let {
                if (it.isGdControlEnabled) {
                    secu3Repository.sendNewTask(Task.Secu3ReadGasDoseParam)
                } else {
                    secu3Repository.sendNewTask(Task.Secu3ReadSensors)
                }
            }

            mGasDoseLiveData.value = secu3Repository.receivedPacketLiveData.first { it is GasDoseParamPacket } as GasDoseParamPacket
            secu3Repository.sendNewTask(Task.Secu3ReadSensors)
        }
    }
}