/*
 *    SecuDroid  - An open source, free manager for SECU-3 engine control unit
 *    Copyright (C) 2026 Vitalii O. Kosharskyi. Ukraine, Kyiv
 *
 *    SECU-3  - An open source, free engine control unit
 *    Copyright (C) 2007-2026 Alexey A. Shabelnikov. Ukraine, Kyiv
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
package org.secu3.android.ui.diagnostics

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.secu3.android.R
import org.secu3.android.connection.ConnectionState
import org.secu3.android.connection.Connected
import org.secu3.android.models.packets.input.DiagInputPacket
import org.secu3.android.ui.compose.SecuTopAppBar
import org.secu3.android.ui.compose.SecuTabRow
import org.secu3.android.ui.parameters.ParameterEditRequest
import org.secu3.android.ui.parameters.dialogs.NumericEditDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DiagnosticsScreen(
    isSecu3T: Boolean?,
    connectionStatus: ConnectionState?,
    diagnosticInput: DiagInputPacket?,
    state: DiagnosticsViewModel.UiState,
    onNavigateUp: () -> Unit,
    onAdditionalSettings: () -> Unit,
    onOutputChanged: (DiagnosticOutput, Boolean) -> Unit,
    onChannelSelected: (Int) -> Unit,
    onFrequencyEdit: (Float) -> Unit,
    onDutyEdit: (Float) -> Unit,
    initialPage: Int = 0,
    hasTpic8101: Boolean? = null,
) {
    val status = connectionStatus
    val input = diagnosticInput.takeIf { state.controlsEnabled && status == Connected }
    val controlsEnabled = state.controlsEnabled && status == Connected && input != null
    val enableBlDe = state.enableBlDe
    val enableTachO = state.enableTachO
    var editorTarget by rememberSaveable { mutableStateOf<String?>(null) }
    var editorInitialValue by rememberSaveable { mutableFloatStateOf(0f) }
    var editorSessionId by rememberSaveable { mutableLongStateOf(0L) }
    LaunchedEffect(controlsEnabled) {
        if (!controlsEnabled) {
            editorTarget = null
            editorSessionId++
        }
    }
    val pagerState = rememberPagerState(initialPage = initialPage.coerceIn(0, 1), pageCount = { 2 })
    val scope = rememberCoroutineScope()
    val titles = stringArrayResource(R.array.diagnostics_fragments_title)

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            SecuTopAppBar(
                title = stringResource(R.string.title_activity_diagnostics),
                connectionState = status,
                onNavigateUp = onNavigateUp,
                showStatus = true,
                navigationIconModifier = Modifier.testTag("diagnostics-back-icon"),
                actions = {
                    IconButton(onClick = onAdditionalSettings, enabled = controlsEnabled, modifier = Modifier.testTag("diagnostics-settings-icon")) {
                        Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.title_activity_settings))
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            SecuTabRow(
                titles = titles.take(2),
                selectedIndex = pagerState.currentPage,
                selectionPosition = { pagerState.currentPage + pagerState.currentPageOffsetFraction },
                onSelected = { index -> scope.launch { pagerState.animateScrollToPage(index) } },
                scrollable = false,
                testTag = "diagnostics-tabs",
            )
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize().testTag("diagnostics-pager"),
                key = { it },
            ) { page ->
                when (page) {
                    0 -> DiagnosticsInputsScreen(
                        input = input,
                        isSecu3T = isSecu3T,
                        selectedChannel = state.channel,
                        frequency = state.frequency,
                        duty = state.duty,
                        testSignalTitle = stringResource(R.string.test_signal_title),
                        outputChannelTitle = stringResource(R.string.output_channel_title),
                        frequencyLabel = stringResource(R.string.test_signal_frequency_label),
                        dutyLabel = stringResource(R.string.test_signal_duty_label),
                        hasTpic8101 = hasTpic8101,
                        controlsEnabled = controlsEnabled,
                        onChannelSelected = onChannelSelected,
                        onFrequencyEdit = {
                            editorInitialValue = state.frequency
                            editorTarget = "frequency"
                            editorSessionId++
                        },
                        onDutyEdit = {
                            editorInitialValue = state.duty
                            editorTarget = "duty"
                            editorSessionId++
                        },
                    )
                    else -> DiagnosticsOutputsScreen(
                        isSecu3T = isSecu3T,
                        outputBits = state.outputBits,
                        enableBlDe = enableBlDe,
                        enableTachO = enableTachO,
                        selectedChannel = state.channel,
                        controlsEnabled = controlsEnabled,
                        onOutputChanged = onOutputChanged,
                    )
                }
            }
        }
    }
    val editRequest = when (editorTarget) {
        "frequency" -> ParameterEditRequest.FloatValue(
            title = stringResource(R.string.test_signal_frequency_hz),
            value = editorInitialValue,
            min = 10f,
            max = 5000f,
            step = .1f,
            precision = 1,
            onValue = {},
        )
        "duty" -> ParameterEditRequest.FloatValue(
            title = stringResource(R.string.test_signal_duty),
            value = editorInitialValue,
            min = 0f,
            max = 100f,
            step = .5f,
            precision = 1,
            onValue = {},
        )
        else -> null
    }
    editRequest?.let { request ->
        val target = editorTarget
        val currentSessionId = editorSessionId
        NumericEditDialog(
            request = request,
            sessionId = currentSessionId,
            onConfirm = { value ->
                if (controlsEnabled && editorTarget == target && editorSessionId == currentSessionId) {
                    editorTarget = null
                    when (target) {
                        "frequency" -> onFrequencyEdit(value.toFloat())
                        "duty" -> onDutyEdit(value.toFloat())
                    }
                }
            },
            onDismiss = {
                if (editorTarget == target && editorSessionId == currentSessionId) editorTarget = null
            },
        )
    }
}
