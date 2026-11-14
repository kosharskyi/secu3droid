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
import kotlinx.coroutines.Job
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOn
import org.secu3.android.domain.sensors.SensorChartSession
import org.secu3.android.domain.sensors.SensorChartSample
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
import org.secu3.android.utils.logger.SecuLogger
import org.secu3.android.utils.Task
import org.secu3.android.utils.prefs.UserPrefs
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
    private val sensorChartSession: SensorChartSession,
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
    private val _chartSamples = MutableStateFlow<Map<GaugeType, List<SensorChartSample>>>(emptyMap())
    val chartSamples: StateFlow<Map<GaugeType, List<SensorChartSample>>> = _chartSamples.asStateFlow()
    private val chartTypes = MutableStateFlow<Set<GaugeType>>(emptySet())
    private var chartCollectionJob: Job? = null
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
    private val showAddChartFlow = MutableStateFlow<ChoiceRequest<GaugeType>?>(null)
    val showAddChart = showAddChartFlow.asStateFlow()
    private val showAddIndicatorFlow = MutableStateFlow<ChoiceRequest<IndicatorType>?>(null)
    val showAddIndicator = showAddIndicatorFlow.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeGaugeStates().map { states -> states.filter { it.isChart }.map { it.gaugeType }.toSet() }
                .distinctUntilChanged()
                .collect { types ->
                    chartTypes.value = types
                    restartChartCollection()
                }
        }
        viewModelScope.launch {
            userPrefs.observeSettings { uiMessageQueue.publish(org.secu3.android.R.string.preferences_storage_error, UiMessageDuration.LONG, scope = "sensors") }
                .collect(::refreshPreferences)
        }
        viewModelScope.launch {
            secuLogger.state.collect { loggerState ->
                _state.update {
                    it.copy(
                        loggerStarted = loggerState,
                    )
                }
            }
        }
        viewModelScope.launch {
            secu3ConnectionManager.connectionStateFlow.collect { connection ->
                if (connection == org.secu3.android.connection.Connected) taskGate.reissueCurrentTask()
            }
        }
    }

    private fun readUiState() = SensorsUiState(
        selectedPage = (savedStateHandle.get<Int>(KEY_SELECTED_PAGE) ?: 0).coerceIn(0, 1),
        loggerStarted = secuLogger.isLoggerStarted,
    )

    fun onRouteResumed() {
        _routeResumed = true
        _state.update {
            it.copy(
                loggerStarted = secuLogger.isLoggerStarted,
            )
        }
        taskGate.setResumed(true)
        restartChartCollection()
    }

    fun onRoutePaused() {
        _routeResumed = false
        taskGate.setResumed(false)
        stopChartCollection()
    }

    fun selectPage(page: Int) {
        val selectedPage = page.coerceIn(0, 1)
        if (_state.value.selectedPage == selectedPage) return
        savedStateHandle[KEY_SELECTED_PAGE] = selectedPage
        _state.update { it.copy(selectedPage = selectedPage) }
        taskGate.selectPage(selectedPage)
        restartChartCollection()
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
                showLoggerControls = settings.showLogControls,
            )
        }
        restartChartCollection()
    }

    private fun restartChartCollection() {
        val shouldCollect = _routeResumed && _state.value.selectedPage == 0 &&
            !_state.value.oldViewEnabled && chartTypes.value.isNotEmpty()
        if (!shouldCollect) {
            stopChartCollection()
            return
        }
        if (chartCollectionJob?.isActive == true) return
        chartCollectionJob = viewModelScope.launch {
            sensorChartSession.observe()
                .conflate()
                .combine(chartTypes) { snapshot, types ->
                    sensorChartSession.readings(snapshot, types)
                }
                .flowOn(Dispatchers.Default)
                .collect { _chartSamples.value = it }
        }
    }

    private var _routeResumed = false

    private fun stopChartCollection() {
        chartCollectionJob?.cancel()
        chartCollectionJob = null
        _chartSamples.value = emptyMap()
    }

    fun consumeGaugeChoices(requestId: Long) {
        if (showAddGaugeFlow.value?.requestId == requestId) showAddGaugeFlow.value = null
    }

    fun consumeChartChoices(requestId: Long) {
        if (showAddChartFlow.value?.requestId == requestId) showAddChartFlow.value = null
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

    fun addGaugeClick() {
        viewModelScope.launch {
            showAddGaugeFlow.value = ChoiceRequest(++nextChoiceRequestId, repository.getAvailableGaugeTypes())
        }
    }

    fun addChartClick() {
        viewModelScope.launch {
            showAddChartFlow.value = ChoiceRequest(++nextChoiceRequestId, repository.getAvailableChartTypes())
        }
    }

    fun addGauge(gauge: GaugeType) { viewModelScope.launch { repository.addGauge(gauge) } }
    fun addChart(gauge: GaugeType) { viewModelScope.launch { repository.addChart(gauge) } }
    fun deleteGauge(gauge: GaugeType) { viewModelScope.launch { repository.deleteGauge(gauge) } }
    fun deleteChart(gauge: GaugeType) { viewModelScope.launch { repository.deleteChart(gauge) } }
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
    val showLoggerControls: Boolean = false,
    val loggerStarted: Boolean = false,
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

    fun reissueCurrentTask() {
        if (resumed) sendTask(taskForPage(selectedPage))
    }

    private fun taskForPage(page: Int): Task = if (page == 0) Task.Secu3ReadSensors else Task.Secu3ReadRawSensors
}

private const val OLD_VIEW_KEY = "old_sensors_view"
