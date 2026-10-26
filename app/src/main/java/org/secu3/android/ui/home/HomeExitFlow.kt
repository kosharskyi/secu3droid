package org.secu3.android.ui.home

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal enum class HomeExitAction { NONE, STOP_CONNECTION, NAVIGATE_START }

internal class HomeExitFlow {
    private var waitingForDisconnect = false
    private val mutableNavigationPending = MutableStateFlow(false)
    val navigationPending: StateFlow<Boolean> = mutableNavigationPending.asStateFlow()

    fun confirm(connectionRunning: Boolean, usbSessionLocked: Boolean = false): HomeExitAction {
        if (usbSessionLocked) return HomeExitAction.NONE
        if (waitingForDisconnect || mutableNavigationPending.value) return HomeExitAction.NONE
        waitingForDisconnect = true
        return if (connectionRunning) HomeExitAction.STOP_CONNECTION else HomeExitAction.NAVIGATE_START
    }

    fun onDisconnected() {
        if (!waitingForDisconnect) return
        waitingForDisconnect = false
        mutableNavigationPending.value = true
    }

    fun consumeNavigation() {
        mutableNavigationPending.value = false
    }

    fun cancelPendingExit() {
        waitingForDisconnect = false
        mutableNavigationPending.value = false
    }
}
