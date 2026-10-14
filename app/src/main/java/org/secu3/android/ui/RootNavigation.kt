/*
 *    SecuDroid  - An open source, free manager for SECU-3 engine control unit
 *    Copyright (C) 2026 Vitalii O. Kosharskyi. Ukraine, Kyiv
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 */
package org.secu3.android.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import androidx.navigation.navigation
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import org.secu3.android.ui.compose.SecuComposeTheme
import org.secu3.android.ui.errors.ErrorsRoute
import org.secu3.android.ui.firmware.FirmwareRoute
import org.secu3.android.ui.logger.SecuLogsRoute
import org.secu3.android.ui.parameters.ParametersRoute
import org.secu3.android.ui.settings.SettingsRoute
import org.secu3.android.ui.dashboard.DashboardRoute
import org.secu3.android.ui.sensors.SensorsRoute
import org.secu3.android.ui.sensors.settings.SensorsSettingsRoute
import org.secu3.android.ui.diagnostics.DiagnosticsRoute
import org.secu3.android.ui.diagnostics.DiagnosticsViewModel
import org.secu3.android.ui.diagnostics.DiagnosticAdditionalDialogContent

internal const val LEGACY_ROUTE = "legacy"
internal const val PARAMETERS_ROUTE = "parameters"
internal const val SETTINGS_ROUTE = "settings"
internal const val ERRORS_ROUTE = "errors"
internal const val LOGS_ROUTE = "logs"
internal const val FIRMWARE_ROUTE = "firmware"
internal const val DASHBOARD_ROUTE = "dashboard"
internal const val SENSORS_ROUTE = "sensors"
internal const val SENSOR_SETTINGS_ROUTE = "sensor-settings"
internal const val DIAGNOSTICS_GRAPH_ROUTE = "diagnostics-graph"
internal const val DIAGNOSTICS_ROUTE = "diagnostics"
internal const val DIAGNOSTICS_ADDITIONAL_ROUTE = "diagnostics-additional"

@Composable
internal fun RootNavigation(activity: MainActivity) {
    val navController = rememberNavController()
    DisposableEffect(navController, activity) {
        activity.attachRootNavController(navController)
        val listener = NavController.OnDestinationChangedListener { _, destination, _ ->
            activity.onRootDestinationChanged(destination.route)
        }
        navController.addOnDestinationChangedListener(listener)
        activity.onRootDestinationChanged(navController.currentDestination?.route)
        onDispose {
            navController.removeOnDestinationChangedListener(listener)
            activity.detachRootNavController(navController)
        }
    }
    SecuComposeTheme {
        NavHost(
            navController = navController,
            startDestination = LEGACY_ROUTE,
            modifier = Modifier.fillMaxSize(),
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None },
        ) {
            composable(LEGACY_ROUTE) { Box(Modifier.fillMaxSize()) }
            composable(PARAMETERS_ROUTE) { entry ->
                ParametersRoute(entry, activity, navController)
            }
            composable(SETTINGS_ROUTE) { entry ->
                SettingsRoute(entry, activity, onNavigateUp = { navController.popBackStack() })
            }
            composable(ERRORS_ROUTE) { entry ->
                ErrorsRoute(entry, activity, onNavigateUp = { navController.popBackStack() })
            }
            composable(LOGS_ROUTE) { entry ->
                SecuLogsRoute(entry, activity, onNavigateUp = { navController.popBackStack() })
            }
            composable(FIRMWARE_ROUTE) { entry ->
                FirmwareRoute(entry, activity, onDismiss = { navController.popBackStack() })
            }
            composable(DASHBOARD_ROUTE) { entry ->
                DashboardRoute(entry, activity)
            }
            composable(SENSORS_ROUTE) { entry ->
                SensorsRoute(
                    entry = entry,
                    activity = activity,
                    onNavigateUp = { navController.popBackStack() },
                    onOpenSettings = { navController.navigate(SENSOR_SETTINGS_ROUTE) },
                )
            }
            composable(SENSOR_SETTINGS_ROUTE) { entry ->
                SensorsSettingsRoute(entry, activity, onNavigateUp = { navController.popBackStack() })
            }
            navigation(route = DIAGNOSTICS_GRAPH_ROUTE, startDestination = DIAGNOSTICS_ROUTE) {
                composable(DIAGNOSTICS_ROUTE) { entry ->
                    val graphEntry = androidx.compose.runtime.remember(entry) { navController.getBackStackEntry(DIAGNOSTICS_GRAPH_ROUTE) }
                    val viewModel: DiagnosticsViewModel = viewModel(
                        viewModelStoreOwner = graphEntry,
                        factory = activity.defaultViewModelProviderFactory,
                        extras = graphEntry.defaultViewModelCreationExtras,
                    )
                    DiagnosticsRoute(
                        viewModel = viewModel,
                        onNavigateUp = { navController.popBackStack() },
                        onAdditionalSettings = { navController.navigate(DIAGNOSTICS_ADDITIONAL_ROUTE) },
                    )
                }
                dialog(DIAGNOSTICS_ADDITIONAL_ROUTE, dialogProperties = DialogProperties(usePlatformDefaultWidth = false)) { entry ->
                    val graphEntry = androidx.compose.runtime.remember(entry) { navController.getBackStackEntry(DIAGNOSTICS_GRAPH_ROUTE) }
                    val viewModel: DiagnosticsViewModel = viewModel(
                        viewModelStoreOwner = graphEntry,
                        factory = activity.defaultViewModelProviderFactory,
                        extras = graphEntry.defaultViewModelCreationExtras,
                    )
                    val state by viewModel.uiState.collectAsStateWithLifecycle()
                    SecuComposeTheme {
                        DiagnosticAdditionalDialogContent(
                            initialEnableBlDe = state.enableBlDe,
                            initialEnableTachO = state.enableTachO,
                            showTachO = viewModel.firmwareInfo?.isSecu3T != true,
                            onOk = { blDe, tachO ->
                                viewModel.applyAdditionalOptions(blDe, tachO)
                                navController.popBackStack()
                            },
                            onCancel = { navController.popBackStack() },
                        )
                    }
                }
            }
        }
    }
}
