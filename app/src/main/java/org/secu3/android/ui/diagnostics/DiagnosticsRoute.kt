package org.secu3.android.ui.diagnostics

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.secu3.android.ui.compose.SecuComposeTheme

@Composable
internal fun DiagnosticsRoute(
    viewModel: DiagnosticsViewModel,
    onNavigateUp: () -> Unit,
    onAdditionalSettings: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val firmware by viewModel.firmwareLiveData.observeAsState(viewModel.firmwareInfo)
    SecuComposeTheme {
        DiagnosticsScreen(
            isSecu3T = firmware?.isSecu3T,
            hasTpic8101 = firmware?.isTPIC8101,
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
