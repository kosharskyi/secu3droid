package org.secu3.android.ui.sensors.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.secu3.android.models.SensorGaugeStyle
import org.secu3.android.models.InjectionTimingMode
import org.secu3.android.utils.UserPrefs

data class SensorsSettingsUiState(
    val oldView: Boolean = false,
    val gaugeStyle: SensorGaugeStyle = SensorGaugeStyle.Default,
    val injectionTimingMode: InjectionTimingMode = InjectionTimingMode.Default,
)

@HiltViewModel
class SensorsSettingsViewModel @Inject constructor(
    private val userPrefs: UserPrefs,
) : ViewModel() {
    private val _state = MutableStateFlow(readState())
    val state: StateFlow<SensorsSettingsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            userPrefs.preferenceChanges.collect { refreshFromPreferences() }
        }
    }

    fun setOldView(enabled: Boolean) {
        userPrefs.oldSensorViewEnabled = enabled
    }

    fun setGaugeStyle(style: SensorGaugeStyle) {
        userPrefs.sensorGaugeStyle = style
    }

    fun setInjectionTimingMode(mode: InjectionTimingMode) {
        userPrefs.injectionTimingMode = mode
    }

    private fun refreshFromPreferences() {
        _state.value = readState()
    }

    private fun readState() = SensorsSettingsUiState(
        oldView = userPrefs.oldSensorViewEnabled,
        gaugeStyle = userPrefs.sensorGaugeStyle,
        injectionTimingMode = userPrefs.injectionTimingMode,
    )

}
