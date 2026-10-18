package org.secu3.android.ui.sensors.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import kotlinx.coroutines.flow.collect
import org.secu3.android.ui.MainActivity
import org.secu3.android.ui.compose.SecuComposeTheme

@Composable
internal fun SensorsSettingsRoute(
    entry: NavBackStackEntry,
    activity: MainActivity,
    onNavigateUp: () -> Unit,
) {
    val viewModel: SensorsSettingsViewModel = viewModel(
        viewModelStoreOwner = entry,
        factory = activity.defaultViewModelProviderFactory,
        extras = entry.defaultViewModelCreationExtras,
    )
    var state by remember(viewModel) { mutableStateOf(viewModel.state.value) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(lifecycle, viewModel) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.state.collect { state = it }
        }
    }
    SecuComposeTheme {
        SensorsSettingsScreen(
            oldView = state.oldView,
            gaugeStyle = state.gaugeStyle,
            injectionTimingMode = state.injectionTimingMode,
            onOldViewChange = viewModel::setOldView,
            onGaugeStyleChange = viewModel::setGaugeStyle,
            onInjectionTimingModeChange = viewModel::setInjectionTimingMode,
            onNavigateUp = onNavigateUp,
        )
    }
}
