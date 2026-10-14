package org.secu3.android.ui.diagnostics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.secu3.android.utils.Task

class DiagnosticsSessionControllerTest {
    @Test
    fun entersOnceHandlesAcknowledgementsAndLeavesOnceInOrder() {
        val commands = mutableListOf<Task>()
        val session = DiagnosticsSessionController(commands::add)
        assertEquals(emptyList<Task>(), commands)
        session.start()
        session.start()
        assertEquals(listOf(Task.Secu3OpComEnterDiagnostics), commands)
        assertFalse(session.isActive)

        session.onOpCode(6)
        session.onOpCode(6)
        assertTrue(session.isActive)
        assertEquals(2, commands.count { it == Task.Secu3DiagInput })
        session.onOpCode(7)
        assertFalse(session.isActive)
        session.onInputReceived()
        assertTrue(session.isActive)

        session.close()
        session.close()
        assertEquals(
            listOf(
                Task.Secu3OpComEnterDiagnostics,
                Task.Secu3DiagInput,
                Task.Secu3DiagInput,
                Task.Secu3OpComLeaveDiagnostics,
                Task.Secu3ReadSensors,
            ),
            commands,
        )
    }

    @Test
    fun ignoresLateAcknowledgementsAndInputsAfterClose() {
        val commands = mutableListOf<Task>()
        val session = DiagnosticsSessionController(commands::add)
        session.start()
        session.close()
        session.onOpCode(6)
        session.onOpCode(7)
        session.onInputReceived()
        assertEquals(
            listOf(Task.Secu3OpComEnterDiagnostics, Task.Secu3OpComLeaveDiagnostics, Task.Secu3ReadSensors),
            commands,
        )
        assertFalse(session.isActive)
    }
}
