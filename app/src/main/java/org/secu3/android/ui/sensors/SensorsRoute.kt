package org.secu3.android.ui.sensors

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import org.secu3.android.models.packets.input.AdcRawDatPacket
import org.secu3.android.models.packets.input.FirmwareInfoPacket
import org.secu3.android.models.packets.input.SensorsPacket
import org.secu3.android.ui.MainActivity
import org.secu3.android.ui.compose.SecuComposeTheme

@Composable
internal fun SensorsRoute(
    entry: NavBackStackEntry,
    activity: MainActivity,
    onNavigateUp: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val viewModel: SensorsViewModel = viewModel(
        viewModelStoreOwner = entry,
        factory = activity.defaultViewModelProviderFactory,
        extras = entry.defaultViewModelCreationExtras,
    )
    val connection by viewModel.connectionState.collectAsStateWithLifecycle(initialValue = null)
    val gauges by viewModel.gauges.collectAsStateWithLifecycle(emptyList())
    val indicators by viewModel.indicators.collectAsStateWithLifecycle(emptyList())
    val gaugeChoices by viewModel.showAddGauge.collectAsStateWithLifecycle()
    val indicatorChoices by viewModel.showAddIndicator.collectAsStateWithLifecycle()
    val sensors: SensorsPacket? by viewModel.sensors.collectAsStateWithLifecycle(initialValue = null)
    val rawSensors: AdcRawDatPacket? by viewModel.rawSensors.collectAsStateWithLifecycle(initialValue = null)
    val firmware: FirmwareInfoPacket? by viewModel.firmwareInfo.collectAsStateWithLifecycle()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val snackbarHostState = remember(viewModel) { SnackbarHostState() }

    androidx.compose.runtime.DisposableEffect(lifecycle, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> viewModel.onRouteResumed()
                Lifecycle.Event.ON_PAUSE -> viewModel.onRoutePaused()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            viewModel.onRoutePaused()
            lifecycle.removeObserver(observer)
        }
    }
    SecuComposeTheme {
        SensorsTabsScreen(
            initialPage = state.selectedPage,
            oldViewEnabled = state.oldViewEnabled,
            loggerEnabled = state.loggerEnabled,
            loggerStarted = state.loggerStarted,
            loggerErrorId = state.loggerErrorId,
            snackbarHostState = snackbarHostState,
            gaugeStyle = state.gaugeStyle,
            injectionTimingMode = state.injectionTimingMode,
            presentationCatalog = viewModel.presentationCatalog,
            firmwareInfo = firmware,
            connectionState = connection,
            gauges = gauges,
            indicators = indicators,
            gaugeChoices = gaugeChoices,
            indicatorChoices = indicatorChoices,
            sensors = sensors,
            rawSensors = rawSensors,
            onPageChanged = { page ->
                if (!lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) viewModel.onRoutePaused()
                viewModel.selectPage(page)
            },
            onNavigateUp = onNavigateUp,
            onSettings = onOpenSettings,
            onAddGaugeRequest = viewModel::addGaugeClick,
            onAddIndicatorRequest = viewModel::addIndicatorClick,
            onAddGauge = viewModel::addGauge,
            onAddIndicator = viewModel::addIndicator,
            onGaugeChoicesConsumed = viewModel::consumeGaugeChoices,
            onIndicatorChoicesConsumed = viewModel::consumeIndicatorChoices,
            onDeleteGauge = viewModel::deleteGauge,
            onDeleteIndicator = viewModel::deleteIndicator,
            onUpdateGauge = viewModel::updateGaugeState,
            onReorderGauges = viewModel::gaugesSwiped,
            onReorderIndicators = viewModel::indicatorsSwiped,
            onStartLog = viewModel::startWriteLog,
            onStopLog = viewModel::stopWriteLog,
            onLoggerErrorConsumed = viewModel::consumeLoggerError,
            onMark = { mark -> when (mark) { 1 -> viewModel.logMark1(); 2 -> viewModel.logMark2(); else -> viewModel.logMark3() } },
        )
    }
}
