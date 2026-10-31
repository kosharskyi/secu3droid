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
import org.secu3.android.connection.ConnectionTimeout
import org.secu3.android.connection.Disconnected
import org.secu3.android.connection.Secu3ConnectionManager
import org.secu3.android.utils.UserPrefs
import javax.inject.Inject

@HiltViewModel
class MainActivityViewModel @Inject constructor(
    val prefs: UserPrefs,
    val connectionManager: Secu3ConnectionManager,
) : ViewModel() {
    private val _pendingNavigation = MutableStateFlow<String?>(null)
    val pendingNavigation = _pendingNavigation.asStateFlow()

    init {
        viewModelScope.launch {
            connectionManager.connectionStateFlow.collect { state ->
                when (state) {
                    Connected -> requestNavigation(HOME_ROUTE)
                    Disconnected, ConnectionTimeout -> requestNavigation(START_ROUTE)
                    else -> _pendingNavigation.compareAndSet(HOME_ROUTE, null)
                }
            }
        }
    }

    fun requestNavigation(route: String) { _pendingNavigation.value = route }

    fun completeNavigation(route: String) { _pendingNavigation.compareAndSet(route, null) }
}
