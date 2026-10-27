package org.secu3.android.ui.sensors.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.secu3.android.R
import org.secu3.android.ui.messages.UiMessageDuration
import org.secu3.android.ui.messages.UiMessageQueue
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
    private val uiMessageQueue: UiMessageQueue,
) : ViewModel() {
    private val _state = MutableStateFlow<SensorsSettingsUiState?>(null)
    val state: StateFlow<SensorsSettingsUiState?> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            userPrefs.observeSettings { uiMessageQueue.publish(R.string.preferences_storage_error, UiMessageDuration.LONG, scope = "sensor-settings") }
                .collect { settings -> _state.value = readState(settings) }
        }
    }

    fun setOldView(enabled: Boolean) {
        persist { userPrefs.setOldSensorViewEnabled(enabled) }
    }

    fun setGaugeStyle(style: SensorGaugeStyle) {
        persist { userPrefs.setSensorGaugeStyle(style) }
    }

    fun setInjectionTimingMode(mode: InjectionTimingMode) {
        persist { userPrefs.setInjectionTimingMode(mode) }
    }

    private fun persist(write: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                write()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: IOException) {
                uiMessageQueue.publish(R.string.preferences_storage_error, UiMessageDuration.LONG, scope = "sensor-settings")
            }
        }
    }

    private fun readState(settings: UserPrefs.Settings) = SensorsSettingsUiState(
        oldView = settings.oldSensorViewEnabled,
        gaugeStyle = settings.sensorGaugeStyle,
        injectionTimingMode = settings.injectionTimingMode,
    )

}
