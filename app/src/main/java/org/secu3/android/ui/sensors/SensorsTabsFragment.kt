/*
 *    SecuDroid  - An open source, free manager for SECU-3 engine control unit
 *    Copyright (C) 2026 Vitalii O. Kosharskyi. Ukraine, Kyiv
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

import android.os.Bundle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import org.secu3.android.models.packets.input.AdcRawDatPacket
import org.secu3.android.models.packets.input.SensorsPacket
import org.secu3.android.ui.compose.SecuComposeTheme
import org.secu3.android.utils.Task
import org.secu3.android.utils.UserPrefs
import javax.inject.Inject

@AndroidEntryPoint
class SensorsTabsFragment : Fragment() {
    private val viewModel: SensorsViewModel by viewModels()
    @Inject lateinit var prefs: UserPrefs
    private var selectedPage: Int = 0
    private val oldViewEnabled = mutableStateOf(false)
    private val loggerStarted = mutableStateOf(false)
    private val loggerEnabled = mutableStateOf(false)
    private val columns = mutableStateOf(2)
    private val gauges by lazy(LazyThreadSafetyMode.NONE) { viewModel.gaugesLiveData }
    private val indicators by lazy(LazyThreadSafetyMode.NONE) { viewModel.indicatorLiveData }
    private val gaugeChoices by lazy(LazyThreadSafetyMode.NONE) { viewModel.showAddGaugeLiveData }
    private val indicatorChoices by lazy(LazyThreadSafetyMode.NONE) { viewModel.showAddIndicatorLiveData }
    private val sensors by lazy(LazyThreadSafetyMode.NONE) { viewModel.sensorsLiveData }
    private val rawSensors by lazy(LazyThreadSafetyMode.NONE) { viewModel.rawSensorsLiveData }
    private val connection by lazy(LazyThreadSafetyMode.NONE) { viewModel.connectionStatusLiveData }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        selectedPage = savedInstanceState?.getInt(STATE_SELECTED_PAGE, 0) ?: 0
        oldViewEnabled.value = prefs.oldSensorViewEnabled
        loggerStarted.value = viewModel.isLoggerStarted
        loggerEnabled.value = viewModel.isLoggerEnabled
        columns.value = viewModel.columnsCount
    }

    override fun onCreateView(inflater: android.view.LayoutInflater, container: android.view.ViewGroup?, savedInstanceState: Bundle?) =
        ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                SecuComposeTheme {
                    val gaugeItems by gauges.observeAsState(emptyList())
                    val indicatorItems by indicators.observeAsState(emptyList())
                    val gaugeChoiceRequest by gaugeChoices.observeAsState()
                    val indicatorChoiceRequest by indicatorChoices.observeAsState()
                    val sensorPacket by sensors.observeAsState<SensorsPacket?, SensorsPacket>(null)
                    val rawPacket by rawSensors.observeAsState<AdcRawDatPacket?, AdcRawDatPacket>(null)
                    val status by connection.observeAsState()
                    SensorsTabsScreen(
                        initialPage = selectedPage,
                        oldViewEnabled = oldViewEnabled.value,
                        loggerEnabled = loggerEnabled.value,
                        loggerStarted = loggerStarted.value,
                        connectionState = status,
                        columns = columns.value,
                        gauges = gaugeItems, indicators = indicatorItems, gaugeChoices = gaugeChoiceRequest,
                        indicatorChoices = indicatorChoiceRequest, sensors = sensorPacket, rawSensors = rawPacket,
                        onPageChanged = { page -> if (page != selectedPage) { selectedPage = page; viewModel.sendNewTask(taskForPage(page)) } },
                        onNavigateUp = { findNavController().navigateUp() },
                        onSettings = { findNavController().navigate(SensorsTabsFragmentDirections.actionOpenSettings()) },
                        onAddGaugeRequest = viewModel::addGaugeClick,
                        onAddIndicatorRequest = viewModel::addIndicatorClick,
                        onAddGauge = viewModel::addGauge, onAddIndicator = viewModel::addIndicator,
                        onGaugeChoicesConsumed = viewModel::consumeGaugeChoices,
                        onIndicatorChoicesConsumed = viewModel::consumeIndicatorChoices,
                        onDeleteGauge = viewModel::deleteGauge, onDeleteIndicator = viewModel::deleteIndicator,
                        onUpdateGauge = viewModel::updateGaugeState,
                        onReorderGauges = viewModel::gaugesSwiped, onReorderIndicators = viewModel::indicatorsSwiped,
                        onStartLog = { viewModel.startWriteLog(); loggerStarted.value = viewModel.isLoggerStarted },
                        onStopLog = { viewModel.stopWriteLog(); loggerStarted.value = viewModel.isLoggerStarted },
                        onMark = { mark -> when (mark) { 1 -> viewModel.logMark1(); 2 -> viewModel.logMark2(); else -> viewModel.logMark3() } },
                    )
                }
            }
        }

    override fun onResume() {
        super.onResume()
        oldViewEnabled.value = prefs.oldSensorViewEnabled
        loggerStarted.value = viewModel.isLoggerStarted
        loggerEnabled.value = viewModel.isLoggerEnabled
        columns.value = viewModel.columnsCount
        viewModel.sendNewTask(taskForPage(selectedPage))
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(STATE_SELECTED_PAGE, selectedPage)
        super.onSaveInstanceState(outState)
    }

    companion object { private const val STATE_SELECTED_PAGE = "selected_sensors_page" }
}

internal fun taskForPage(page: Int): Task = if (page == 0) Task.Secu3ReadSensors else Task.Secu3ReadRawSensors
