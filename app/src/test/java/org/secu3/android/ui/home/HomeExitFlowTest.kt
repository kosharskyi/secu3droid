package org.secu3.android.ui.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeExitFlowTest {
    @Test fun lockedUsbSessionCannotRequestAnExit() {
        val flow = HomeExitFlow()
        var stopServiceCalls = 0
        assertEquals(HomeExitAction.NONE, flow.confirm(connectionRunning = true, usbSessionLocked = true) { stopServiceCalls++ })
        assertFalse(flow.onDisconnected())
        assertFalse(flow.consumeNavigation())
        assertEquals(0, stopServiceCalls)
    }

    @Test fun connectionExitStopsOnceThenNavigatesOnceAfterDisconnect() {
        val flow = HomeExitFlow()
        var stopServiceCalls = 0
        assertEquals(HomeExitAction.STOP_CONNECTION, flow.confirm(connectionRunning = true) { stopServiceCalls++ })
        assertEquals(HomeExitAction.NONE, flow.confirm(connectionRunning = true) { stopServiceCalls++ })
        assertEquals(1, stopServiceCalls)
        assertTrue(flow.onDisconnected())
        assertFalse(flow.onDisconnected())
        assertTrue(flow.consumeNavigation())
        assertFalse(flow.consumeNavigation())
    }

    @Test fun disconnectedExitNavigatesOnceWithoutStoppingConnection() {
        val flow = HomeExitFlow()
        var stopServiceCalls = 0
        assertEquals(HomeExitAction.NAVIGATE_START, flow.confirm(connectionRunning = false) { stopServiceCalls++ })
        assertEquals(HomeExitAction.NONE, flow.confirm(connectionRunning = false) { stopServiceCalls++ })
        assertEquals(1, stopServiceCalls)
        assertFalse(flow.onDisconnected())
        assertTrue(flow.consumeNavigation())
    }
}
