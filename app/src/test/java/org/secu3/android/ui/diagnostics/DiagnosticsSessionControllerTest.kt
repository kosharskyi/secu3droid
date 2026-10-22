package org.secu3.android.ui.diagnostics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.secu3.android.utils.Task

class DiagnosticsSessionControllerTest {
    @Test
    fun offlineStartWaitsForPacketSubscriptionAvailability() {
        val commands = mutableListOf<Task>()
        val session = DiagnosticsSessionController(commands::add)

        session.start()
        session.onOpCode(6)
        session.onInputReceived()
        assertEquals(emptyList<Task>(), commands)
        assertFalse(session.isActive)

        session.onConnectionChanged(true)
        assertEquals(listOf(Task.Secu3OpComEnterDiagnostics), commands)
        session.onConnectionChanged(true)
        session.start()
        assertEquals(1, commands.count { it == Task.Secu3OpComEnterDiagnostics })
    }

    @Test
    fun connectedBeforeStartAndFastAckAreHandledOnce() {
        val commands = mutableListOf<Task>()
        val session = DiagnosticsSessionController(commands::add)

        session.onConnectionChanged(true)
        session.start()
        assertEquals(listOf(Task.Secu3OpComEnterDiagnostics), commands)
        session.onOpCode(6)
        session.onOpCode(6)
        assertEquals(1, commands.count { it == Task.Secu3DiagInput })
        assertFalse(session.isActive)

        session.onInputReceived()
        session.onInputReceived()
        assertTrue(session.isActive)
    }

    @Test
    fun reconnectRequestsOneNewEntryAndIgnoresLateEventsUntilCurrentInput() {
        val commands = mutableListOf<Task>()
        val session = DiagnosticsSessionController(commands::add)
        session.start()
        session.onConnectionChanged(true)
        session.onOpCode(6)
        session.onInputReceived()
        assertTrue(session.isActive)

        session.onConnectionChanged(false)
        session.onOpCode(6)
        session.onInputReceived()
        assertFalse(session.isActive)
        assertEquals(1, commands.count { it == Task.Secu3OpComEnterDiagnostics })
        assertEquals(1, commands.count { it == Task.Secu3DiagInput })

        session.onConnectionChanged(true)
        session.onConnectionChanged(true)
        assertEquals(2, commands.count { it == Task.Secu3OpComEnterDiagnostics })
        session.onOpCode(6)
        session.onOpCode(6)
        assertEquals(2, commands.count { it == Task.Secu3DiagInput })
        assertFalse(session.isActive)
        session.onInputReceived()
        assertTrue(session.isActive)
    }

    @Test
    fun closeLeavesOnlyAnAvailableEnteredSessionAndIgnoresLateEvents() {
        val offlineCommands = mutableListOf<Task>()
        val offline = DiagnosticsSessionController(offlineCommands::add)
        offline.start()
        offline.close()
        offline.onConnectionChanged(true)
        offline.onOpCode(6)
        assertEquals(emptyList<Task>(), offlineCommands)

        val commands = mutableListOf<Task>()
        val session = DiagnosticsSessionController(commands::add)
        session.start()
        session.onConnectionChanged(true)
        session.close()
        session.close()
        session.onConnectionChanged(false)
        session.onConnectionChanged(true)
        session.onOpCode(6)
        session.onInputReceived()
        assertEquals(
            listOf(
                Task.Secu3OpComEnterDiagnostics,
                Task.Secu3OpComLeaveDiagnostics,
                Task.Secu3ReadSensors,
            ),
            commands,
        )
        assertFalse(session.isActive)
    }

    @Test
    fun lossBeforeCloseSuppressesCleanupForUnavailableConnection() {
        val commands = mutableListOf<Task>()
        val session = DiagnosticsSessionController(commands::add)
        session.start()
        session.onConnectionChanged(true)
        session.onConnectionChanged(false)
        session.close()

        assertEquals(listOf(Task.Secu3OpComEnterDiagnostics), commands)
    }
}
