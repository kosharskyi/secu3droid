/*
 *    SecuDroid  - An open source, free manager for SECU-3 engine control unit
 *    Copyright (C) 2026 Vitalii O. Kosharskyi. Ukraine, Kyiv
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

package org.secu3.android.ui.sensors

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LooksOne
import androidx.compose.material.icons.filled.LooksTwo
import androidx.compose.material.icons.filled.Looks3
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.drop
import org.secu3.android.R
import org.secu3.android.connection.ConnectionState
import org.secu3.android.connection.Connected
import org.secu3.android.db.models.GaugeState
import org.secu3.android.db.models.IndicatorState
import org.secu3.android.models.packets.input.AdcRawDatPacket
import org.secu3.android.models.packets.input.SensorsPacket
import org.secu3.android.models.packets.input.FirmwareInfoPacket
import org.secu3.android.models.SensorGaugeStyle
import org.secu3.android.models.InjectionTimingMode
import org.secu3.android.ui.compose.SecuTopAppBar
import org.secu3.android.ui.compose.SecuTabRow
import org.secu3.android.ui.sensors.SensorsViewModel.ChoiceRequest
import org.secu3.android.ui.sensors.models.SensorPresentationCatalog
import org.secu3.android.ui.sensors.models.GaugeItem
import org.secu3.android.models.sensors.GaugeType
import org.secu3.android.ui.sensors.models.IndicatorItem
import org.secu3.android.models.sensors.IndicatorType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SensorsTabsScreen(
    initialPage: Int,
    oldViewEnabled: Boolean,
    loggerEnabled: Boolean,
    loggerStarted: Boolean,
    loggerErrorId: Long,
    snackbarHostState: SnackbarHostState,
    connectionState: ConnectionState?,
    gauges: List<GaugeItem>, indicators: List<IndicatorItem>,
    gaugeChoices: ChoiceRequest<GaugeType>?, indicatorChoices: ChoiceRequest<IndicatorType>?,
    sensors: SensorsPacket?, rawSensors: AdcRawDatPacket?,
    firmwareInfo: FirmwareInfoPacket?,
    injectionTimingMode: InjectionTimingMode,
    presentationCatalog: SensorPresentationCatalog,
    renderer: GaugeDialRenderer,
    onPageChanged: (Int) -> Unit,
    onNavigateUp: () -> Unit,
    onSettings: () -> Unit,
    onAddGaugeRequest: () -> Unit, onAddIndicatorRequest: () -> Unit,
    onAddGauge: (GaugeType) -> Unit, onAddIndicator: (IndicatorType) -> Unit,
    onGaugeChoicesConsumed: (Long) -> Unit, onIndicatorChoicesConsumed: (Long) -> Unit,
    onDeleteGauge: (GaugeType) -> Unit, onDeleteIndicator: (IndicatorType) -> Unit,
    onUpdateGauge: (GaugeState) -> Unit,
    onReorderGauges: (List<GaugeState>) -> Unit, onReorderIndicators: (List<IndicatorState>) -> Unit,
    onStartLog: () -> Unit, onStopLog: () -> Unit, onMark: (Int) -> Unit,
    onLoggerErrorConsumed: (Long) -> Unit,
    onReorderingChanged: (Boolean) -> Unit = {},
    gaugeStyle: SensorGaugeStyle = SensorGaugeStyle.Default,
    safeDrawingInsets: WindowInsets = WindowInsets.safeDrawing,
    systemBarsInsets: WindowInsets = WindowInsets.systemBars,
) {
    val scaffoldWindowInsets = safeDrawingInsets.only(WindowInsetsSides.Vertical)
        .union(systemBarsInsets.only(WindowInsetsSides.Horizontal))
    val pagerHorizontalInsets = safeDrawingInsets.only(WindowInsetsSides.Horizontal)
    val additionalFabInsets = safeDrawingInsets.exclude(systemBarsInsets).only(WindowInsetsSides.Horizontal)
    val pagerState = rememberPagerState(initialPage = initialPage.coerceIn(0, 1), pageCount = { 2 })
    var reordering by remember { mutableStateOf(false) }
    val latestReordering by rememberUpdatedState(reordering)
    val latestReorderingChanged by rememberUpdatedState(onReorderingChanged)
    DisposableEffect(Unit) {
        onDispose { if (latestReordering) latestReorderingChanged(false) }
    }
    val scope = rememberCoroutineScope()
    val mark1Description = stringResource(R.string.mark_1)
    val mark2Description = stringResource(R.string.mark_2)
    val mark3Description = stringResource(R.string.mark_3)
    val dataAvailable = connectionState == Connected && sensors != null
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val loggerErrorMessage = stringResource(R.string.sensor_logger_io_error)
    LaunchedEffect(pagerState) { snapshotFlow { pagerState.settledPage }.drop(1).collect(onPageChanged) }
    LaunchedEffect(loggerErrorId, lifecycle, snackbarHostState) {
        if (loggerErrorId > 0) {
            lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                snackbarHostState.showSnackbar(loggerErrorMessage)
                onLoggerErrorConsumed(loggerErrorId)
            }
        }
    }
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = scaffoldWindowInsets,
        topBar = {
            SecuTopAppBar(
                title = stringResource(R.string.app_name),
                connectionState = connectionState,
                onNavigateUp = onNavigateUp,
                showStatus = true,
                actions = {
                    IconButton(onClick = onSettings) { Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.title_activity_settings)) }
                },
            )
        },
        floatingActionButtonPosition = FabPosition.Start,
        floatingActionButton = {
            if (loggerEnabled) Box(Modifier.windowInsetsPadding(additionalFabInsets)) {
                if (loggerStarted) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        FilledTonalIconButton(onClick = { onMark(1) }, modifier = Modifier.semantics { contentDescription = mark1Description }.testTag("log-mark-1")) { Icon(Icons.Default.LooksOne, null) }
                        FilledTonalIconButton(onClick = { onMark(2) }, modifier = Modifier.semantics { contentDescription = mark2Description }.testTag("log-mark-2")) { Icon(Icons.Default.LooksTwo, null) }
                        FilledTonalIconButton(onClick = { onMark(3) }, modifier = Modifier.semantics { contentDescription = mark3Description }.testTag("log-mark-3")) { Icon(Icons.Default.Looks3, null) }
                        FloatingActionButton(onClick = onStopLog, modifier = Modifier.testTag("log-stop"), containerColor = MaterialTheme.colorScheme.error, contentColor = MaterialTheme.colorScheme.onError) { Icon(Icons.Default.Stop, stringResource(R.string.stop)) }
                    }
                } else {
                    ExtendedFloatingActionButton(onClick = onStartLog, modifier = Modifier.testTag("log-start")) {
                        Icon(Icons.Default.Add, null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.log_start_writing))
                    }
                }
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding)) {
            SecuTabRow(
                titles = listOf(R.string.sensors_tab_title, R.string.menu_raw_sensors).map { stringResource(it) },
                selectedIndex = pagerState.currentPage,
                selectionPosition = { pagerState.currentPage + pagerState.currentPageOffsetFraction },
                onSelected = { index -> scope.launch { pagerState.animateScrollToPage(index) } },
                scrollable = false,
                testTag = "sensors-tabs",
            )
            HorizontalPager(
                state = pagerState,
                userScrollEnabled = !reordering,
                modifier = Modifier.fillMaxSize().windowInsetsPadding(pagerHorizontalInsets).testTag("sensors-pager"),
                key = { it },
            ) { page ->
                when (page) {
                    0 -> if (oldViewEnabled) OldSensorsScreen(sensors.takeIf { dataAvailable }, injectionTimingMode = injectionTimingMode, presentationCatalog = presentationCatalog) else SensorsScreen(
                        gauges, indicators, gaugeChoices, indicatorChoices,
                        onAddGaugeRequest, onAddIndicatorRequest, onAddGauge, onAddIndicator,
                        onGaugeChoicesConsumed, onIndicatorChoicesConsumed, onDeleteGauge, onDeleteIndicator,
                        onUpdateGauge, onReorderGauges, onReorderIndicators,
                        onReorderingChanged = { active -> reordering = active; onReorderingChanged(active) },
                        dataAvailable = dataAvailable,
                        gaugeStyle = gaugeStyle,
                        injectionTimingMode = injectionTimingMode,
                        presentationCatalog = presentationCatalog,
                        renderer = renderer,
                    )
                    else -> RawSensorsScreen(rawSensors.takeIf { connectionState == Connected }, capabilities = RawSensorCapabilities.from(firmwareInfo))
                }
            }
        }
    }
}
