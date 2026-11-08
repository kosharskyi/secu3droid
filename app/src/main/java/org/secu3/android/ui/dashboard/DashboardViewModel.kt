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

package org.secu3.android.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.secu3.android.connection.Secu3ConnectionManager
import org.secu3.android.models.InjectionTimingMode
import org.secu3.android.models.DashboardGaugeStyle
import org.secu3.android.models.sensors.GaugeType
import org.secu3.android.ui.common.SensorPacketObserver
import org.secu3.android.ui.sensors.models.SensorPresentationCatalog
import org.secu3.android.ui.sensors.GaugeDialRenderer
import org.secu3.android.utils.Task
import org.secu3.android.utils.prefs.UserPrefs
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val secu3ConnectionManager: Secu3ConnectionManager,
    sensorPacketObserver: SensorPacketObserver,
    private val userPrefs: UserPrefs,
    val presentationCatalog: SensorPresentationCatalog,
    val gaugeDialRenderer: GaugeDialRenderer,
) : ViewModel() {
    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    val packetFlow = combine(
        sensorPacketObserver.observeSensors(updateIntervalMs = 100L),
        uiState,
    ) { packet, settings ->
        packet?.takeIf { settings.settingsLoaded }?.let {
            DashboardViewData.inflate(
                packet = it,
                leftGauge = settings.leftGauge,
                rightGauge = settings.rightGauge,
                injectionTimingMode = settings.injectionTimingMode,
            )
        }
    }

    val connectionState = secu3ConnectionManager.connectionStateFlow

    init {
        viewModelScope.launch {
            userPrefs.observeSettings { }
                .collect { settings ->
                    _uiState.update {
                        it.copy(
                            leftGauge = settings.dashboardLeftGauge,
                            rightGauge = settings.dashboardRightGauge,
                            gaugeStyle = settings.dashboardGaugeStyle,
                            injectionTimingMode = settings.injectionTimingMode,
                            settingsLoaded = true,
                        )
                    }
                }
        }
    }

    fun setTask(task: Task) {
        secu3ConnectionManager.sendNewTask(task)
    }
}

data class DashboardUiState(
    val leftGauge: GaugeType = UserPrefs.DEFAULT_DASHBOARD_LEFT_GAUGE,
    val rightGauge: GaugeType = UserPrefs.DEFAULT_DASHBOARD_RIGHT_GAUGE,
    val gaugeStyle: DashboardGaugeStyle = DashboardGaugeStyle.Default,
    val injectionTimingMode: InjectionTimingMode = InjectionTimingMode.Default,
    val settingsLoaded: Boolean = false,
)
