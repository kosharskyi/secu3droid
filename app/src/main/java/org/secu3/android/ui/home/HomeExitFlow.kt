package org.secu3.android.ui.home

internal enum class HomeExitAction { NONE, STOP_CONNECTION, NAVIGATE_START }

internal class HomeExitFlow {
    private var waitingForDisconnect = false
    private var navigationPending = false

    fun confirm(connectionRunning: Boolean, usbSessionLocked: Boolean = false, onAccepted: (HomeExitAction) -> Unit = {}): HomeExitAction {
        if (usbSessionLocked) return HomeExitAction.NONE
        if (waitingForDisconnect || navigationPending) return HomeExitAction.NONE
        val action = if (connectionRunning) {
            waitingForDisconnect = true
            HomeExitAction.STOP_CONNECTION
        } else {
            navigationPending = true
            HomeExitAction.NAVIGATE_START
        }
        onAccepted(action)
        return action
    }

    fun onDisconnected(): Boolean {
        if (!waitingForDisconnect) return false
        waitingForDisconnect = false
        navigationPending = true
        return true
    }

    fun consumeNavigation(): Boolean {
        if (!navigationPending) return false
        navigationPending = false
        return true
    }
}
