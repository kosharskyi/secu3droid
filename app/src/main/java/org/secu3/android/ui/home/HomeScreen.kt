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

package org.secu3.android.ui.home

import android.content.res.Configuration
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.foundation.Image
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.secu3.android.R
import org.secu3.android.ui.compose.SecuComposeTheme
import org.secu3.android.ui.compose.ConfirmationDialog
import org.secu3.android.ui.compose.ConnectionStatusIndicator
import org.secu3.android.connection.ConnectionState
import org.secu3.android.connection.Connected
import org.secu3.android.connection.InProgress
import org.secu3.android.connection.Disconnected

enum class HomeLayout { GRID, LANDSCAPE_COLUMNS }

data class HomeScreenState(
    val connectionStatus: ConnectionState = Disconnected,
    val firmwareAvailable: Boolean = false,
    val connectionRunning: Boolean = false,
    val usbSessionLocked: Boolean = false,
    val dialog: HomeDialog? = null,
    val releaseMessage: String? = null,
)

data class HomeScreenActions(
    val onDashboard: () -> Unit = {},
    val onSensors: () -> Unit = {},
    val onParameters: () -> Unit = {},
    val onErrors: () -> Unit = {},
    val onLogs: () -> Unit = {},
    val onSettings: () -> Unit = {},
    val onFirmware: () -> Unit = {},
    val onExitImmediately: () -> Unit = {},
    val onExitRequested: () -> Unit = {},
    val onExitCancelled: () -> Unit = {},
    val onExitConfirmed: () -> Unit = {},
    val onDiagnosticsRequested: () -> Unit = {},
    val onDialogDismissed: () -> Unit = {},
    val onPermissionRationaleConfirmed: () -> Unit = {},
    val onPermissionRationaleCancelled: () -> Unit = {},
    val onDiagnosticsConfirmed: () -> Unit = {},
    val onDownloadRelease: () -> Unit = {},
    val onReleaseNotes: () -> Unit = {},
)

enum class HomeDialog { EXIT, DIAGNOSTICS, RELEASE, RELEASE_PERMISSION_RATIONALE }

private enum class HomeDestination { DASHBOARD, SENSORS, PARAMETERS, ERRORS, DIAGNOSTICS, LOGS, SETTINGS }
private data class HomeMenuItem(@StringRes val label: Int, @DrawableRes val icon: Int, val destination: HomeDestination)

private val menuItems = listOf(
    HomeMenuItem(R.string.menu_dashboard, R.drawable.ic_dashboard, HomeDestination.DASHBOARD),
    HomeMenuItem(R.string.sensors, R.drawable.ic_sensors, HomeDestination.SENSORS),
    HomeMenuItem(R.string.menu_param, R.drawable.ic_wrench, HomeDestination.PARAMETERS),
    HomeMenuItem(R.string.title_activity_errors, R.drawable.indicators_check_engine_landscape, HomeDestination.ERRORS),
    HomeMenuItem(R.string.title_activity_diagnostics, R.drawable.ic_diagnostic, HomeDestination.DIAGNOSTICS),
    HomeMenuItem(R.string.menu_secu_logs, R.drawable.ic_log_file, HomeDestination.LOGS),
    HomeMenuItem(R.string.title_activity_settings, R.drawable.ic_settings_24, HomeDestination.SETTINGS),
)

@Composable
fun HomeScreen(
    state: HomeScreenState,
    actions: HomeScreenActions,
    modifier: Modifier = Modifier,
    layoutOverride: HomeLayout? = null,
    contentInsets: WindowInsets = WindowInsets.safeDrawing,
) {
    val dialog = state.dialog
    val layout = layoutOverride ?: homeLayout(LocalConfiguration.current)
    SecuComposeTheme {
        Box(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).testTag("home-full-root")) {
          Column(Modifier.fillMaxSize().windowInsetsPadding(contentInsets).testTag("home-safe-content")) {
            Row(Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween) {
                if (!state.usbSessionLocked) TextButton(
                    modifier = Modifier.testTag("home-exit-button"),
                    onClick = { if (state.connectionRunning) actions.onExitRequested() else actions.onExitImmediately() },
                ) {
                    Icon(painterResource(R.drawable.ic_logout_24), contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
                    Text(stringResource(R.string.exit), color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(start = 8.dp))
                } else {
                    Box(Modifier.weight(1f))
                }
                val statusDescription = when (state.connectionStatus) {
                    Connected -> stringResource(
                        if (state.firmwareAvailable) R.string.home_connection_connected_firmware else R.string.home_connection_connected,
                    )
                    InProgress -> stringResource(R.string.home_connection_connecting)
                    else -> stringResource(R.string.home_connection_disconnected)
                }
                ConnectionStatusIndicator(
                    connectionState = state.connectionStatus,
                    contentDescriptionOverride = statusDescription,
                    onClick = if (state.firmwareAvailable) actions.onFirmware else null,
                    testTag = "home-ecu-status",
                )
            }
            if (layout == HomeLayout.LANDSCAPE_COLUMNS) {
                Row(Modifier.weight(1f).fillMaxWidth().testTag("home-menu-viewport").horizontalScroll(rememberScrollState()).padding(start = 32.dp, end = 24.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    MenuCard(menuItems.first(), Modifier.fillMaxHeight().width(220.dp), adaptIconSize = true) { handleDestination(menuItems.first().destination, actions) { actions.onDiagnosticsRequested() } }
                    menuItems.drop(1).chunked(2).forEachIndexed { columnIndex, pair ->
                        Column(Modifier.fillMaxHeight().width(160.dp).testTag("home-landscape-pair-$columnIndex"), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            pair.forEach { item ->
                                MenuCard(item, Modifier.weight(1f).fillMaxWidth(), adaptIconSize = true) { handleDestination(item.destination, actions) { actions.onDiagnosticsRequested() } }
                            }
                        }
                    }
                }
            } else {
                Column(Modifier.weight(1f).fillMaxWidth().testTag("home-menu-viewport").verticalScroll(rememberScrollState()).padding(horizontal = 32.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    MenuCard(menuItems.first(), Modifier.fillMaxWidth().height(150.dp)) { handleDestination(menuItems.first().destination, actions) { actions.onDiagnosticsRequested() } }
                    val rest = menuItems.drop(1)
                    rest.chunked(2).forEach { pair ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            pair.forEach { item -> MenuCard(item, Modifier.weight(1f).height(150.dp)) { handleDestination(item.destination, actions) { actions.onDiagnosticsRequested() } } }
                            if (pair.size == 1) Box(Modifier.weight(1f))
                        }
                    }
                }
            }
          }
          when (dialog) {
        HomeDialog.EXIT -> ConfirmationDialog(
            title = R.string.disconnect_device,
            message = R.string.disconnect_device_msg,
            onConfirm = actions.onExitConfirmed,
            onCancel = actions.onExitCancelled,
        )
        HomeDialog.DIAGNOSTICS -> ConfirmationDialog(
            title = android.R.string.dialog_alert_title,
            message = R.string.menu_diagnostics_warning_title,
            onConfirm = { actions.onDialogDismissed(); actions.onDiagnosticsConfirmed() },
            onCancel = actions.onDialogDismissed,
            onDismiss = actions.onDialogDismissed,
        )
        HomeDialog.RELEASE -> AlertDialog(onDismissRequest = actions.onDialogDismissed,
            modifier = Modifier
                .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), RoundedCornerShape(24.dp))
                .semantics { contentDescription = "release_dialog" },
            shape = RoundedCornerShape(24.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp,
            title = { Text(stringResource(R.string.new_version_available), modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
            text = { Column(Modifier.heightIn(max = 300.dp).verticalScroll(rememberScrollState())) {
                Text(state.releaseMessage.orEmpty())
                TextButton(onClick = actions.onDialogDismissed) { Text(stringResource(R.string.remind_me_later)) }
            } },
            confirmButton = { TextButton(onClick = { actions.onDialogDismissed(); actions.onDownloadRelease() }) { Text(stringResource(R.string.download)) } },
            dismissButton = { TextButton(onClick = { actions.onDialogDismissed(); actions.onReleaseNotes() }) { Text(stringResource(R.string.what_s_new)) } })
        HomeDialog.RELEASE_PERMISSION_RATIONALE -> ConfirmationDialog(
            title = R.string.download_manager_permission_rationale_title,
            message = R.string.download_manager_permission_rationale_message,
            onConfirm = actions.onPermissionRationaleConfirmed,
            onCancel = actions.onPermissionRationaleCancelled,
        )
        null -> Unit
          }
        }
    }
}

@Composable
private fun MenuCard(item: HomeMenuItem, modifier: Modifier, adaptIconSize: Boolean = false, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val foregroundColor = if (colors.background.luminance() > 0.5f) {
        colors.onSurface
    } else {
        colors.onSurfaceVariant
    }
    Card(onClick = onClick, modifier = modifier.testTag("home-card-${item.destination.name.lowercase()}"),
        colors = CardDefaults.cardColors(containerColor = colors.surface, contentColor = foregroundColor),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)) {
        BoxWithConstraints(Modifier.fillMaxSize().padding(8.dp)) {
          val iconSize = if (!adaptIconSize || maxHeight >= 180.dp) 96.dp else if (maxHeight >= 130.dp) 72.dp else (maxHeight * 0.45f).coerceAtLeast(24.dp)
          Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceBetween) {
            Box(
                Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(item.icon),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    colorFilter = ColorFilter.tint(foregroundColor),
                    modifier = Modifier.size(iconSize).testTag("home-icon-${item.destination.name.lowercase()}"),
                )
            }
            Text(stringResource(item.label), textAlign = TextAlign.Center, style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp).testTag("home-label-${item.destination.name.lowercase()}"))
          }
        }
    }
}

fun homeLayout(configuration: Configuration): HomeLayout =
    if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) HomeLayout.LANDSCAPE_COLUMNS else HomeLayout.GRID

private fun handleDestination(destination: HomeDestination, actions: HomeScreenActions, showDiagnostics: () -> Unit) {
    when (destination) {
        HomeDestination.DASHBOARD -> actions.onDashboard()
        HomeDestination.SENSORS -> actions.onSensors()
        HomeDestination.PARAMETERS -> actions.onParameters()
        HomeDestination.ERRORS -> actions.onErrors()
        HomeDestination.DIAGNOSTICS -> showDiagnostics()
        HomeDestination.LOGS -> actions.onLogs()
        HomeDestination.SETTINGS -> actions.onSettings()
    }
}
