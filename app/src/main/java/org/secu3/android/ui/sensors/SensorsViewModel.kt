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
 */
package org.secu3.android.ui.sensors

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.secu3.android.connection.Secu3ConnectionManager
import org.secu3.android.ui.common.SensorPacketObserver
import org.secu3.android.db.SensorConfigurationRepository
import org.secu3.android.db.models.GaugeState
import org.secu3.android.db.models.IndicatorState
import org.secu3.android.models.SensorGaugeStyle
import org.secu3.android.models.InjectionTimingMode
import org.secu3.android.ui.sensors.models.GaugeItem
import org.secu3.android.models.sensors.GaugeType
import org.secu3.android.ui.sensors.models.IndicatorItem
import org.secu3.android.models.sensors.IndicatorType
import org.secu3.android.ui.sensors.models.SensorPresentationCatalog
import org.secu3.android.utils.SecuLogger
import org.secu3.android.utils.Task
import org.secu3.android.utils.UserPrefs
import org.secu3.android.ui.messages.UiMessageDuration
import org.secu3.android.ui.messages.UiMessageQueue
import java.io.IOException
import kotlinx.coroutines.CancellationException
import javax.inject.Inject

@HiltViewModel
class SensorsViewModel @Inject constructor(
    private val secu3ConnectionManager: Secu3ConnectionManager,
    private val sensorPacketObserver: SensorPacketObserver,
    private val userPrefs: UserPrefs,
    private val secuLogger: SecuLogger,
    private val repository: SensorConfigurationRepository,
    val presentationCatalog: SensorPresentationCatalog,
    val gaugeDialRenderer: GaugeDialRenderer,
    private val savedStateHandle: SavedStateHandle,
    private val uiMessageQueue: UiMessageQueue,
) : ViewModel() {

    data class ChoiceRequest<T>(val requestId: Long, val choices: List<T>)

    private val _state = MutableStateFlow(readUiState())
    val state: StateFlow<SensorsUiState> = _state.asStateFlow()
    private val taskGate = SensorTaskGate(_state.value.selectedPage, secu3ConnectionManager::sendNewTask)
    private val sensorUpdateIntervalMs = 100L

    val connectionState = secu3ConnectionManager.connectionStateFlow
    val sensors = sensorPacketObserver.observeSensors(sensorUpdateIntervalMs)
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val rawSensors = sensorPacketObserver.observeRawSensors(sensorUpdateIntervalMs)
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val firmwareInfo = secu3ConnectionManager.firmwareInfo
    val gauges = combine(
        repository.observeGaugeStates(),
        sensors,
        state.map { it.injectionTimingMode }.distinctUntilChanged(),
    ) { configurations, packet, timingMode ->
        configurations.map { configuration ->
            val value = packet?.let { currentPacket ->
                val raw = configuration.gaugeType.getGaugeItem(currentPacket)
                val displayed = if (
                    configuration.gaugeType == GaugeType.BEGIN_INJ_PHASE ||
                    configuration.gaugeType == GaugeType.END_INJ_PHASE
                ) timingMode.transform(raw) else raw
                presentationCatalog.formatValue(configuration.gaugeType, displayed)
            }.orEmpty()
            GaugeItem(configuration, value)
        }
    }
    val indicators = combine(repository.observeIndicatorStates(), sensors) { configurations, packet ->
        configurations.map { configuration ->
            IndicatorItem(configuration, packet?.let(configuration.indicatorType::isActive) ?: false)
        }
    }

    private var nextChoiceRequestId = 0L
    private val showAddGaugeFlow = MutableStateFlow<ChoiceRequest<GaugeType>?>(null)
    val showAddGauge = showAddGaugeFlow.asStateFlow()
    private val showAddIndicatorFlow = MutableStateFlow<ChoiceRequest<IndicatorType>?>(null)
    val showAddIndicator = showAddIndicatorFlow.asStateFlow()

    init {
        viewModelScope.launch {
            userPrefs.observeSettings { uiMessageQueue.publish(org.secu3.android.R.string.preferences_storage_error, UiMessageDuration.LONG, scope = "sensors") }
                .collect(::refreshPreferences)
        }
        viewModelScope.launch {
            secuLogger.state.collect { loggerState ->
                _state.update {
                    it.copy(
                        loggerStarted = loggerState.started,
                        loggerErrorId = loggerState.pendingErrorId ?: 0,
                    )
                }
            }
        }
    }

    private fun readUiState() = SensorsUiState(
        selectedPage = (savedStateHandle.get<Int>(KEY_SELECTED_PAGE) ?: 0).coerceIn(0, 1),
        loggerStarted = secuLogger.isLoggerStarted,
        loggerErrorId = secuLogger.state.value.pendingErrorId ?: 0,
    )

    fun onRouteResumed() {
        _state.update {
            it.copy(
                loggerStarted = secuLogger.isLoggerStarted,
                loggerErrorId = secuLogger.state.value.pendingErrorId ?: 0,
            )
        }
        taskGate.setResumed(true)
    }

    fun onRoutePaused() {
        taskGate.setResumed(false)
    }

    fun selectPage(page: Int) {
        val selectedPage = page.coerceIn(0, 1)
        if (_state.value.selectedPage == selectedPage) return
        savedStateHandle[KEY_SELECTED_PAGE] = selectedPage
        _state.update { it.copy(selectedPage = selectedPage) }
        taskGate.selectPage(selectedPage)
    }

    fun setOldViewEnabled(enabled: Boolean) {
        viewModelScope.launch {
            try {
                userPrefs.setOldSensorViewEnabled(enabled)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: IOException) {
                uiMessageQueue.publish(org.secu3.android.R.string.preferences_storage_error, UiMessageDuration.LONG, scope = "sensors")
            }
        }
    }

    private fun refreshPreferences(settings: UserPrefs.Settings) {
        _state.update {
            it.copy(
                oldViewEnabled = settings.oldSensorViewEnabled,
                gaugeStyle = settings.sensorGaugeStyle,
                injectionTimingMode = settings.injectionTimingMode,
                loggerEnabled = settings.isSensorLoggerEnabled,
            )
        }
    }

    fun consumeGaugeChoices(requestId: Long) {
        if (showAddGaugeFlow.value?.requestId == requestId) showAddGaugeFlow.value = null
    }

    fun consumeIndicatorChoices(requestId: Long) {
        if (showAddIndicatorFlow.value?.requestId == requestId) showAddIndicatorFlow.value = null
    }

    fun startWriteLog() {
        secuLogger.startLogging()
    }

    fun logMark1() = secuLogger.setMark1()
    fun logMark2() = secuLogger.setMark2()
    fun logMark3() = secuLogger.setMark3()

    fun stopWriteLog() {
        secuLogger.stopLogging()
    }

    fun consumeLoggerError(errorId: Long) = secuLogger.acknowledgeLoggerError(errorId)

    fun addGaugeClick() {
        viewModelScope.launch {
            showAddGaugeFlow.value = ChoiceRequest(++nextChoiceRequestId, repository.getAvailableGaugeTypes())
        }
    }

    fun addGauge(gauge: GaugeType) { viewModelScope.launch { repository.addGauge(gauge) } }
    fun deleteGauge(gauge: GaugeType) { viewModelScope.launch { repository.deleteGauge(gauge) } }
    fun updateGaugeState(gaugeState: GaugeState) { viewModelScope.launch { repository.updateGauges(listOf(gaugeState)) } }

    fun addIndicatorClick() {
        viewModelScope.launch {
            showAddIndicatorFlow.value = ChoiceRequest(++nextChoiceRequestId, repository.getAvailableIndicatorTypes())
        }
    }

    fun addIndicator(indicator: IndicatorType) { viewModelScope.launch { repository.addIndicator(indicator) } }
    fun deleteIndicator(indicator: IndicatorType) { viewModelScope.launch { repository.deleteIndicator(indicator) } }
    fun gaugesSwiped(swipedList: List<GaugeState>) { viewModelScope.launch { repository.updateGauges(swipedList) } }
    fun indicatorsSwiped(swipedList: List<IndicatorState>) { viewModelScope.launch { repository.updateIndicators(swipedList) } }

    companion object {
        internal const val KEY_SELECTED_PAGE = "selected_sensors_page"
    }
}

data class SensorsUiState(
    val selectedPage: Int = 0,
    val oldViewEnabled: Boolean = false,
    val gaugeStyle: SensorGaugeStyle = SensorGaugeStyle.Default,
    val injectionTimingMode: InjectionTimingMode = InjectionTimingMode.Default,
    val loggerEnabled: Boolean = false,
    val loggerStarted: Boolean = false,
    val loggerErrorId: Long = 0,
)

internal class SensorTaskGate(
    initialPage: Int = 0,
    private val sendTask: (Task) -> Unit,
) {
    private var selectedPage = initialPage.coerceIn(0, 1)
    private var resumed = false

    fun selectPage(page: Int) {
        val normalized = page.coerceIn(0, 1)
        if (selectedPage == normalized) return
        selectedPage = normalized
        if (resumed) sendTask(taskForPage(normalized))
    }

    fun setResumed(value: Boolean) {
        if (resumed == value) return
        resumed = value
        if (resumed) sendTask(taskForPage(selectedPage))
    }

    private fun taskForPage(page: Int): Task = if (page == 0) Task.Secu3ReadSensors else Task.Secu3ReadRawSensors
}

private const val OLD_VIEW_KEY = "old_sensors_view"
