/*
 *    SecuDroid  - An open source, free manager for SECU-3 engine control unit
 *    Copyright (C) 2026 Vitalii O. Kosharskyi. Ukraine, Kyiv
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 */
package org.secu3.android.ui.ltft

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import dagger.hilt.android.lifecycle.HiltViewModel
import org.secu3.android.connection.Secu3ConnectionManager
import org.secu3.android.domain.ltft.LtftMapsSession
import org.secu3.android.ui.MainActivity
import javax.inject.Inject

@Composable
internal fun LtftMapsRoute(entry: NavBackStackEntry, activity: MainActivity, onNavigateUp: () -> Unit) {
    val viewModel: LtftMapsViewModel = viewModel(
        viewModelStoreOwner = entry,
        factory = activity.defaultViewModelProviderFactory,
        extras = entry.defaultViewModelCreationExtras,
    )
    val connection by viewModel.connectionState.collectAsStateWithLifecycle(initialValue = null)
    val state by viewModel.state.collectAsStateWithLifecycle()
    val writeState by viewModel.writeState.collectAsStateWithLifecycle()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> viewModel.onRouteResumed()
                Lifecycle.Event.ON_PAUSE -> viewModel.onRoutePaused()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) viewModel.onRouteResumed()
        onDispose {
            viewModel.onRoutePaused()
            lifecycle.removeObserver(observer)
        }
    }

    LtftMapsScreen(
        state = state,
        connectionState = connection,
        writeState = writeState,
        onNavigateUp = onNavigateUp,
        onRefresh = viewModel::refresh,
        onReset = viewModel::resetMaps,
        onSave = viewModel::saveMaps,
    )
}

@HiltViewModel
internal class LtftMapsViewModel @Inject constructor(
    connectionManager: Secu3ConnectionManager,
    private val session: LtftMapsSession,
) : ViewModel() {
    val connectionState = connectionManager.connectionStateFlow
    val state = session.state
    val writeState = session.writeState

    init {
        session.start(viewModelScope)
    }

    fun onRouteResumed() = session.onRouteResumed()
    fun onRoutePaused() = session.onRoutePaused()
    fun refresh() = session.refresh()
    fun resetMaps() = session.resetMaps()
    fun saveMaps() = session.saveMaps()

    override fun onCleared() = session.close()
}
