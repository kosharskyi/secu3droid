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
    val firmware by viewModel.firmwareInfo.collectAsStateWithLifecycle()
    val connection by viewModel.connectionState.collectAsStateWithLifecycle(initialValue = null)
    val input by viewModel.diagInput.collectAsStateWithLifecycle()
    SecuComposeTheme {
        DiagnosticsScreen(
            isSecu3T = firmware?.isSecu3T,
            hasTpic8101 = firmware?.isTPIC8101,
            connectionStatus = connection,
            diagnosticInput = input,
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
