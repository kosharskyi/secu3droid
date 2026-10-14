package org.secu3.android.ui.sensors.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.secu3.android.utils.UserPrefs
import org.secu3.android.utils.getDefaultSharedPreferences

data class SensorsSettingsUiState(
    val oldView: Boolean = false,
    val columns: Int = 2,
)

@HiltViewModel
class SensorsSettingsViewModel @Inject constructor(
    private val userPrefs: UserPrefs,
    @ApplicationContext context: Context,
) : ViewModel() {
    private val preferences = context.getDefaultSharedPreferences()
    private val _state = MutableStateFlow(readState())
    val state: StateFlow<SensorsSettingsUiState> = _state.asStateFlow()
    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == null || key == OLD_VIEW_KEY || key == COLUMNS_KEY) refreshFromPreferences()
    }

    init {
        preferences.registerOnSharedPreferenceChangeListener(listener)
    }

    fun setOldView(enabled: Boolean) {
        userPrefs.oldSensorViewEnabled = enabled
        _state.update { it.copy(oldView = enabled) }
    }

    fun setColumns(columns: Int) {
        val normalized = columns.coerceIn(2, 8)
        userPrefs.columnsCount = normalized
        _state.update { it.copy(columns = normalized) }
    }

    private fun refreshFromPreferences() {
        _state.value = readState()
    }

    private fun readState() = SensorsSettingsUiState(
        oldView = userPrefs.oldSensorViewEnabled,
        columns = userPrefs.columnsCount.coerceIn(2, 8),
    )

    override fun onCleared() {
        preferences.unregisterOnSharedPreferenceChangeListener(listener)
    }

    private companion object {
        const val OLD_VIEW_KEY = "old_sensors_view"
        const val COLUMNS_KEY = "columns_count"
    }
}
