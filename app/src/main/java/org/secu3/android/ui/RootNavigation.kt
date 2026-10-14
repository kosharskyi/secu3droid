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
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import org.secu3.android.ui.compose.SecuComposeTheme
import org.secu3.android.ui.parameters.ParametersRoute

internal const val LEGACY_ROUTE = "legacy"
internal const val PARAMETERS_ROUTE = "parameters"

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
        }
    }
}
