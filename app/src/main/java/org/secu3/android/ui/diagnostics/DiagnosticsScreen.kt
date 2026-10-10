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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LiveData
import androidx.compose.runtime.livedata.observeAsState
import kotlinx.coroutines.launch
import org.secu3.android.R
import org.secu3.android.connection.ConnectionState
import org.secu3.android.models.packets.input.DiagInputPacket
import org.secu3.android.models.packets.out.DiagOutputPacket
import org.secu3.android.ui.compose.ConnectionStatusIndicator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DiagnosticsScreen(
    isSecu3T: Boolean?,
    connectionStatus: LiveData<ConnectionState>,
    inputLiveData: LiveData<DiagInputPacket>,
    outputPacket: DiagOutputPacket,
    enableBlDeLiveData: LiveData<Boolean>,
    enableTachOLiveData: LiveData<Boolean>,
    onNavigateUp: () -> Unit,
    onAdditionalSettings: () -> Unit,
    onOutputChanged: (DiagnosticOutput, Boolean) -> Unit,
    onChannelSelected: (Int) -> Unit,
    onFrequencyEdit: (Float, (Float) -> Unit) -> Unit,
    onDutyEdit: (Float, (Float) -> Unit) -> Unit,
    initialPage: Int = 0,
) {
    val status by connectionStatus.observeAsState()
    val input by inputLiveData.observeAsState()
    val enableBlDe by enableBlDeLiveData.observeAsState(false)
    val enableTachO by enableTachOLiveData.observeAsState(false)
    var selectedChannel by remember(outputPacket) { mutableIntStateOf(outputPacket.chan) }
    var frequency by remember(outputPacket) { mutableFloatStateOf(outputPacket.frq) }
    var duty by remember(outputPacket) {
        mutableFloatStateOf(if (outputPacket.chan == 0 && outputPacket.duty == 0f) 50f else outputPacket.duty)
    }
    val pagerState = rememberPagerState(initialPage = initialPage.coerceIn(0, 1), pageCount = { 2 })
    val scope = rememberCoroutineScope()
    val titles = stringArrayResource(R.array.diagnostics_fragments_title)

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_activity_diagnostics)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back), modifier = Modifier.testTag("diagnostics-back-icon"))
                    }
                },
                actions = {
                    ConnectionStatusIndicator(status, Modifier.padding(end = 8.dp))
                    IconButton(onClick = onAdditionalSettings) {
                        Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.title_activity_settings), modifier = Modifier.testTag("diagnostics-settings-icon"))
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            PrimaryTabRow(selectedTabIndex = pagerState.currentPage, modifier = Modifier.testTag("diagnostics-tabs")) {
                titles.take(2).forEachIndexed { index, title ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                        text = { Text(title) },
                    )
                }
            }
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize().testTag("diagnostics-pager"),
                key = { it },
            ) { page ->
                when (page) {
                    0 -> DiagnosticsInputsScreen(
                        input = input,
                        isSecu3T = isSecu3T,
                        selectedChannel = selectedChannel,
                        frequency = frequency,
                        duty = duty,
                        testSignalTitle = stringResource(R.string.test_signal_title),
                        outputChannelTitle = stringResource(R.string.output_channel_title),
                        frequencyLabel = stringResource(R.string.test_signal_frequency_label),
                        dutyLabel = stringResource(R.string.test_signal_duty_label),
                        onChannelSelected = { selectedChannel = it; onChannelSelected(it) },
                        onFrequencyEdit = { onFrequencyEdit(frequency) { frequency = it } },
                        onDutyEdit = { onDutyEdit(duty) { duty = it } },
                    )
                    else -> DiagnosticsOutputsScreen(
                        isSecu3T = isSecu3T,
                        outputBits = outputPacket.out,
                        enableBlDe = enableBlDe,
                        enableTachO = enableTachO,
                        onOutputChanged = onOutputChanged,
                    )
                }
            }
        }
    }
}
