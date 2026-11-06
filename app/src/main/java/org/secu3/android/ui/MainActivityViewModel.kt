/*
 *    SecuDroid  - An open source, free manager for SECU-3 engine control unit
 *    Copyright (C) 2024 Vitalii O. Kosharskyi. Ukraine, Kyiv
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

package org.secu3.android.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.secu3.android.connection.Connected
import org.secu3.android.connection.ConnectionState
import org.secu3.android.connection.ConnectionTimeout
import org.secu3.android.connection.Disconnected
import org.secu3.android.connection.Secu3ConnectionManager
import org.secu3.android.connection.UsbReconnecting
import org.secu3.android.connection.UsbReconnectingAwaitingEcu
import org.secu3.android.utils.prefs.UserPrefs
import javax.inject.Inject

@HiltViewModel
class MainActivityViewModel @Inject constructor(
    val prefs: UserPrefs,
    val connectionManager: Secu3ConnectionManager,
) : ViewModel() {
    data class PendingNavigation(val route: String, val userInitiated: Boolean) {
        val automaticHomeOnlyFromStart: Boolean get() = !userInitiated && route == HOME_ROUTE
    }

    private val _pendingNavigation = MutableStateFlow<PendingNavigation?>(null)
    val pendingNavigation = _pendingNavigation.asStateFlow()
    private var navigationActive = false
    private var latestStateVersion = 0L
    private var handledStateVersion = 0L

    init {
        viewModelScope.launch {
            connectionManager.connectionStateFlow.collect { state ->
                latestStateVersion++
                if (navigationActive) applyConnectionNavigation(state)
            }
        }
    }

    fun setConnectionNavigationActive(active: Boolean) {
        navigationActive = active
        if (active) applyConnectionNavigation(connectionManager.currentConnectionState)
    }

    fun onConnectionNavigationPaused() {
        navigationActive = false
        if (_pendingNavigation.value?.userInitiated == false) {
            _pendingNavigation.value = null
            if (latestStateVersion > 0) handledStateVersion = latestStateVersion - 1
        }
    }

    fun requestNavigation(route: String) {
        _pendingNavigation.value = PendingNavigation(route, userInitiated = true)
    }

    fun completeNavigation(request: PendingNavigation) {
        if (_pendingNavigation.value == request) _pendingNavigation.value = null
    }

    private fun requestAutomaticNavigation(route: String) {
        val request = PendingNavigation(route, userInitiated = false)
        _pendingNavigation.value = request
    }

    private fun discardPendingAutomaticHome() {
        if (_pendingNavigation.value?.automaticHomeOnlyFromStart == true) _pendingNavigation.value = null
    }

    private fun applyConnectionNavigation(state: ConnectionState) {
        if (handledStateVersion == latestStateVersion) return
        handledStateVersion = latestStateVersion
        when {
            state == Connected -> requestAutomaticNavigation(HOME_ROUTE)
            state == Disconnected || state == ConnectionTimeout -> requestAutomaticNavigation(START_ROUTE)
            state == UsbReconnecting || state == UsbReconnectingAwaitingEcu -> requestAutomaticNavigation(START_ROUTE)
            else -> discardPendingAutomaticHome()
        }
    }
}
