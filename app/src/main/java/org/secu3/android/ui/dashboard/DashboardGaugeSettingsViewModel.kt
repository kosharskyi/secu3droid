/*
 *SecuDroid  - An open source, free manager for SECU-3 engine control unit
 *Copyright (C) 2026 Vitalii O. Kosharskyi. Ukraine, Kyiv
 *
 *SECU-3  - An open source, free engine control unit
 *Copyright (C) 2007-2026 Alexey A. Shabelnikov. Ukraine, Kyiv
 *
 *This program is free software: you can redistribute it and/or modify
 *it under the terms of the GNU General Public License as published by
 *the Free Software Foundation, either version 3 of the License, or
 *(at your option) any later version.
 *
 *This program is distributed in the hope that it will be useful,
 *but WITHOUT ANY WARRANTY; without even the implied warranty of
 *MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *GNU General Public License for more details.
 *
 *You should have received a copy of the GNU General Public License
 *along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *contacts:
 *                http://secu-3.org
 *                email: vetalkosharskiy@gmail.com
 */

package org.secu3.android.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.secu3.android.models.sensors.GaugeType
import org.secu3.android.ui.sensors.models.SensorPresentationCatalog
import org.secu3.android.utils.prefs.UserPrefs

data class DashboardGaugeSettingsUiState(
    val leftGauge: GaugeType = UserPrefs.DEFAULT_DASHBOARD_LEFT_GAUGE,
    val rightGauge: GaugeType = UserPrefs.DEFAULT_DASHBOARD_RIGHT_GAUGE,
    val settingsLoaded: Boolean = false,
    val settingsReadFailed: Boolean = false,
    val saveStatus: DashboardGaugeSaveStatus = DashboardGaugeSaveStatus.Idle,
)

enum class DashboardGaugeSaveStatus {
    Idle,
    Saving,
    Failed,
    Saved,
}

@HiltViewModel
class DashboardGaugeSettingsViewModel @Inject constructor(
    private val userPrefs: UserPrefs,
    val presentationCatalog: SensorPresentationCatalog,
) : ViewModel() {
    private val _uiState = MutableStateFlow(DashboardGaugeSettingsUiState())
    val uiState: StateFlow<DashboardGaugeSettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            userPrefs.observeSettings { _uiState.update { it.copy(settingsReadFailed = true) } }
                .collect { settings ->
                    _uiState.update {
                        it.copy(
                            leftGauge = settings.dashboardLeftGauge,
                            rightGauge = settings.dashboardRightGauge,
                            settingsLoaded = true,
                            settingsReadFailed = false,
                        )
                    }
                }
        }
    }

    fun saveGaugeSettings(left: GaugeType, right: GaugeType) {
        if (_uiState.value.saveStatus == DashboardGaugeSaveStatus.Saving) return
        _uiState.update { it.copy(saveStatus = DashboardGaugeSaveStatus.Saving) }
        viewModelScope.launch {
            try {
                userPrefs.setDashboardGauges(left, right)
                _uiState.update {
                    it.copy(
                        leftGauge = left,
                        rightGauge = right,
                        settingsLoaded = true,
                        settingsReadFailed = false,
                        saveStatus = DashboardGaugeSaveStatus.Saved,
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: IOException) {
                _uiState.update { it.copy(saveStatus = DashboardGaugeSaveStatus.Failed) }
            }
        }
    }
}