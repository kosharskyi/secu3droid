package org.secu3.android.ui.diagnostics

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.secu3.android.ui.compose.SecuComposeTheme

@Composable
internal fun DiagnosticsRoute(
    viewModel: DiagnosticsViewModel,
    onNavigateUp: () -> Unit,
    onAdditionalSettings: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    SecuComposeTheme {
        DiagnosticsScreen(
            isSecu3T = viewModel.firmwareInfo?.isSecu3T,
            connectionStatus = viewModel.connectionStatusLiveData,
            inputLiveData = viewModel.diagInputLiveData,
            state = state,
            onNavigateUp = onNavigateUp,
            onAdditionalSettings = onAdditionalSettings,
            onOutputChanged = viewModel::setOutput,
            onChannelSelected = viewModel::selectChannel,
            onFrequencyEdit = viewModel::editFrequency,
            onDutyEdit = viewModel::editDuty,
        )
    }
}
