package org.secu3.android.ui.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeExitFlowTest {
    @Test fun lockedUsbSessionCannotRequestAnExit() {
        val flow = HomeExitFlow()
        assertEquals(HomeExitAction.NONE, flow.confirm(connectionRunning = true, usbSessionLocked = true))
        assertFalse(flow.onDisconnected())
        assertFalse(flow.consumeNavigation())
    }

    @Test fun connectionExitStopsOnceThenNavigatesOnceAfterDisconnect() {
        val flow = HomeExitFlow()
        assertEquals(HomeExitAction.STOP_CONNECTION, flow.confirm(connectionRunning = true))
        assertEquals(HomeExitAction.NONE, flow.confirm(connectionRunning = true))
        assertTrue(flow.onDisconnected())
        assertFalse(flow.onDisconnected())
        assertTrue(flow.consumeNavigation())
        assertFalse(flow.consumeNavigation())
    }

    @Test fun disconnectedExitNavigatesOnceWithoutStoppingConnection() {
        val flow = HomeExitFlow()
        assertEquals(HomeExitAction.NAVIGATE_START, flow.confirm(connectionRunning = false))
        assertEquals(HomeExitAction.NONE, flow.confirm(connectionRunning = false))
        assertFalse(flow.onDisconnected())
        assertTrue(flow.consumeNavigation())
    }

    @Test fun cancellingRejectedExitClearsPendingStateAndAllowsAnotherConfirmation() {
        val waitingForDisconnect = HomeExitFlow()
        assertEquals(HomeExitAction.STOP_CONNECTION, waitingForDisconnect.confirm(connectionRunning = true))
        waitingForDisconnect.cancelPendingExit()
        assertFalse(waitingForDisconnect.onDisconnected())
        assertEquals(HomeExitAction.STOP_CONNECTION, waitingForDisconnect.confirm(connectionRunning = true))

        val navigationPending = HomeExitFlow()
        assertEquals(HomeExitAction.NAVIGATE_START, navigationPending.confirm(connectionRunning = false))
        navigationPending.cancelPendingExit()
        assertFalse(navigationPending.consumeNavigation())
        assertEquals(HomeExitAction.NAVIGATE_START, navigationPending.confirm(connectionRunning = false))
    }
}
