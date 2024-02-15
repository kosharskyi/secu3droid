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
package org.secu3.android.ui.sensors

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.launch
import org.secu3.android.Secu3Repository
import org.secu3.android.db.models.GaugeState
import org.secu3.android.models.packets.input.AdcRawDatPacket
import org.secu3.android.models.packets.input.SensorsPacket
import org.secu3.android.ui.sensors.models.GaugeItem
import org.secu3.android.ui.sensors.models.GaugeType
import org.secu3.android.ui.sensors.models.IndicatorItem
import org.secu3.android.ui.sensors.models.IndicatorType
import org.secu3.android.utils.AppPrefs
import org.secu3.android.utils.UserPrefs
import org.secu3.android.utils.SecuLogger
import org.secu3.android.utils.Task
import javax.inject.Inject

@HiltViewModel
class SensorsViewModel @Inject constructor(private val secu3Repository: Secu3Repository,
                                           private val mUserPrefs: UserPrefs,
                                           private val mAppPrefs: AppPrefs,
                                           private val secuLogger: SecuLogger,
                                           private val repository: SensorsRepository
                                           ) : ViewModel() {

    val isLoggerEnabled: Boolean
        get() = mUserPrefs.isSensorLoggerEnabled

    val isLoggerStarted: Boolean
        get() = secuLogger.isLoggerStarted


    val gaugesLiveData: LiveData<List<GaugeItem>>
        get() = secu3Repository.receivedPacketFlow.sample(500).filter { it is SensorsPacket }
            .map { it as SensorsPacket }.map { repository.convertToGaugeItemList(it) }.asLiveData()


    val indicatorLiveData: LiveData<List<IndicatorItem>>
        get() = secu3Repository.receivedPacketFlow.sample(500).filter { it is SensorsPacket }
            .map { it as SensorsPacket }.map { repository.convertToIndicatorItemList(it) }.asLiveData()


    val rawSensorsLiveData: LiveData<AdcRawDatPacket>
        get() = secu3Repository.receivedPacketFlow.sample(500).filter { it is AdcRawDatPacket }
            .map { it as AdcRawDatPacket }.asLiveData()

    private val showAddGaugeFlow = MutableSharedFlow<List<GaugeType>>()
    val showAddGaugeLiveData: LiveData<List<GaugeType>>
        get() = showAddGaugeFlow.asLiveData()


    fun sendNewTask(task: Task) {
        secu3Repository.sendNewTask(task)
    }


    fun startWriteLog() {
        if (secuLogger.isLoggerStarted) {
            return
        }

        secuLogger.prepareToLog()
        secuLogger.startLogging()
    }

    fun logMark1() {
        secuLogger.setMark1()
    }

    fun logMark2() {
        secuLogger.setMark2()
    }

    fun logMark3() {
        secuLogger.setMark3()
    }

    fun stopWriteLog() {
        secuLogger.stopLogging()
    }

    fun addGaugeClick() {
        viewModelScope.launch {
            val storedGauges = repository.getStoredGaugeStates().map { it.gaugeType }
            val gauges = GaugeType.entries.filter { it !in storedGauges }
            showAddGaugeFlow.emit(gauges)
        }
    }

    fun addGauge(gauge: GaugeType) {
        viewModelScope.launch {
            repository.addGauge(gauge)
        }
    }

    fun deleteGauge(gauge: GaugeType) {
        viewModelScope.launch {
            repository.deleteGauge(gauge)
        }
    }

    fun getIndicatorsAvailableToAdd(): List<IndicatorType> {
        return IndicatorType.entries.filter { it !in mAppPrefs.indicatorsEnabled }
    }

    fun addIndicator(indicator: IndicatorType) {
        mAppPrefs.apply {
            val indicators = indicatorsEnabled.toMutableList()
            indicators.add(indicator)
            indicatorsEnabled = indicators
        }
    }

    fun deleteIndicator(indicator: IndicatorType) {
        mAppPrefs.apply {
            val indicators = indicatorsEnabled.toMutableList()
            indicators.remove(indicator)
            indicatorsEnabled = indicators
        }
    }

    fun itemsSwiped(swipedList: List<GaugeState>) {
        viewModelScope.launch {
            repository.updateGauges(swipedList)
        }
    }
}
