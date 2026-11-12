/*
 *    SecuDroid  - An open source, free manager for SECU-3 engine control unit
 *    Copyright (C) 2024 Vitaliy O. Kosharskyi. Ukraine, Kyiv
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

package org.secu3.android.ui.errors

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.secu3.android.R
import org.secu3.android.connection.Connected
import org.secu3.android.connection.Secu3ConnectionManager
import org.secu3.android.domain.errors.ErrorsSession
import org.secu3.android.domain.errors.ErrorsSnapshot
import org.secu3.android.ui.ERRORS_ROUTE
import org.secu3.android.ui.messages.UiMessageDuration
import org.secu3.android.ui.messages.UiMessageQueue
import org.secu3.android.utils.prefs.UserPrefs
import java.io.IOException
import javax.inject.Inject

internal enum class ErrorsConfirmation { Clear, Write }

@HiltViewModel
class ErrorsViewModel @Inject constructor(
    private val secu3ConnectionManager: Secu3ConnectionManager,
    private val session: ErrorsSession,
    private val userPrefs: UserPrefs,
    private val uiMessageQueue: UiMessageQueue,
) : ViewModel() {

    val connectionState = secu3ConnectionManager.connectionStateFlow

    internal val errorsSnapshot: StateFlow<ErrorsSnapshot> = session.snapshot

    private val _confirmation = MutableStateFlow<ErrorsConfirmation?>(null)
    internal val confirmation = _confirmation.asStateFlow()

    init {
        session.start(viewModelScope)
        viewModelScope.launch {
            secu3ConnectionManager.connectionStateFlow.collect { state ->
                if (state != Connected) dismissConfirmation()
            }
        }
    }

    fun onRouteResumed() = session.onRouteResumed()

    fun onRoutePaused() {
        dismissConfirmation()
        session.onRoutePaused()
    }

    fun setHoldActiveErrors(enabled: Boolean) = session.setHoldActiveErrors(enabled)
    fun readSavedErrors() = session.readSavedErrors()
    fun retryActiveRead() = session.retryActiveRead()
    fun beginEditing() = session.beginEditing()
    fun setDraftError(bit: Int, selected: Boolean) = session.setDraftError(bit, selected)
    fun discardDraft() = session.discardDraft()

    fun setAutoReadSavedErrors(enabled: Boolean) {
        viewModelScope.launch {
            try {
                userPrefs.setAutoReadSavedErrors(enabled)
            } catch (_: IOException) {
                uiMessageQueue.publish(R.string.preferences_storage_error, UiMessageDuration.LONG, scope = ERRORS_ROUTE)
            }
        }
    }

    internal fun requestConfirmation(action: ErrorsConfirmation) {
        val snapshot = errorsSnapshot.value
        val allowed = when (action) {
            ErrorsConfirmation.Clear -> snapshot.canClearErrors
            ErrorsConfirmation.Write -> snapshot.canWriteDraft
        }
        if (allowed && secu3ConnectionManager.currentConnectionState == Connected) {
            _confirmation.value = action
        }
    }

    fun dismissConfirmation() {
        _confirmation.value = null
    }

    fun confirmWrite() {
        val action = _confirmation.value
        dismissConfirmation()
        if (secu3ConnectionManager.currentConnectionState != Connected) return
        when (action) {
            ErrorsConfirmation.Clear -> session.clearErrors()
            ErrorsConfirmation.Write -> session.writeDraft()
            null -> Unit
        }
    }

    override fun onCleared() {
        session.close()
    }

}
