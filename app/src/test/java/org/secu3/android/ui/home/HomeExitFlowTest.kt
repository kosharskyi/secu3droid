package org.secu3.android.ui.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeExitFlowTest {
    @Test fun lockedUsbSessionCannotRequestAnExit() {
        val flow = HomeExitFlow()
        assertEquals(HomeExitAction.NONE, flow.confirm(connectionRunning = true, usbSessionLocked = true))
        flow.onDisconnected()
        assertFalse(flow.navigationPending.value)
        flow.consumeNavigation()
        assertFalse(flow.navigationPending.value)
    }

    @Test fun connectionExitStopsOnceThenNavigatesOnceAfterDisconnect() {
        val flow = HomeExitFlow()
        assertEquals(HomeExitAction.STOP_CONNECTION, flow.confirm(connectionRunning = true))
        assertFalse(flow.navigationPending.value)
        assertEquals(HomeExitAction.NONE, flow.confirm(connectionRunning = true))
        flow.onDisconnected()
        assertTrue(flow.navigationPending.value)
        flow.onDisconnected()
        assertTrue(flow.navigationPending.value)
        flow.consumeNavigation()
        assertFalse(flow.navigationPending.value)
        flow.consumeNavigation()
        assertFalse(flow.navigationPending.value)
    }

    @Test fun disconnectedExitWaitsForStopAcceptanceThenNavigatesOnce() {
        val flow = HomeExitFlow()
        assertEquals(HomeExitAction.NAVIGATE_START, flow.confirm(connectionRunning = false))
        assertEquals(HomeExitAction.NONE, flow.confirm(connectionRunning = false))
        assertFalse(flow.navigationPending.value)
        flow.consumeNavigation()
        assertFalse(flow.navigationPending.value)
        flow.onDisconnected()
        assertTrue(flow.navigationPending.value)
        flow.consumeNavigation()
        assertFalse(flow.navigationPending.value)
    }

    @Test fun cancellingRejectedExitClearsPendingStateAndAllowsAnotherConfirmation() {
        val waitingForDisconnect = HomeExitFlow()
        assertEquals(HomeExitAction.STOP_CONNECTION, waitingForDisconnect.confirm(connectionRunning = true))
        waitingForDisconnect.cancelPendingExit()
        waitingForDisconnect.onDisconnected()
        assertFalse(waitingForDisconnect.navigationPending.value)
        assertEquals(HomeExitAction.STOP_CONNECTION, waitingForDisconnect.confirm(connectionRunning = true))

        val navigationPending = HomeExitFlow()
        assertEquals(HomeExitAction.NAVIGATE_START, navigationPending.confirm(connectionRunning = false))
        assertFalse(navigationPending.navigationPending.value)
        navigationPending.cancelPendingExit()
        navigationPending.onDisconnected()
        assertFalse(navigationPending.navigationPending.value)
        navigationPending.consumeNavigation()
        assertFalse(navigationPending.navigationPending.value)
        assertEquals(HomeExitAction.NAVIGATE_START, navigationPending.confirm(connectionRunning = false))
    }
}
