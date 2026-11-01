package org.secu3.android.connection

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test
import org.secu3.android.models.RawPacket

class ConnectionLifecycleTest {
    @Test fun pendingStateQueueRetainsFastTransitionsWithinOneRun() = runBlocking {
        val connection = TestConnection()
        try {
            connection.publishStates(Connected, Reconnecting, Connected)
            val events = mutableListOf<ConnectionEvent>()
            connection.consumePendingEvents(events::add)

            assertEquals(listOf(Connecting, Connected, Reconnecting, Connected), events.map { it.state })
            assertTrue(events.all { it.end == null })
        } finally { connection.stopConnection(); connection.shutdown() }
    }

    @Test fun replacementDiscardsUndeliveredStatusesFromThePreviousRun() = runBlocking {
        val connection = TestConnection()
        try {
            connection.publishStates(Connected, Reconnecting)
            connection.start()
            val events = mutableListOf<ConnectionEvent>()
            connection.consumePendingEvents(events::add)

            assertEquals(listOf(Connecting), events.map { it.state })
            assertTrue(events.all { it.end == null })
        } finally { connection.stopConnection(); connection.shutdown() }
    }

    @Test fun replacedAttemptCannotConfirmItsSuccessor() = runBlocking {
        val connection = TestConnection()
        try {
            val first = connection.start()
            val second = connection.start()
            assertTrue(first != second)
            assertTrue(connection.isConnected)
            assertFalse(connection.isReady)
            connection.receivePacket(first, firmware())
            assertFalse(connection.isReady)
            connection.receivePacket(second, firmware())
            assertTrue(connection.isReady)
            assertEquals(second, connection.readyAttemptId)
            connection.stopConnection()
            assertFalse(connection.isReady)
            assertEquals(Disconnected, connection.currentConnectionState)
        } finally { connection.shutdown() }
    }

    @Test fun subscribedRequestCannotAcceptReplyFromReplacementAttempt() = runBlocking {
        val connection = TestConnection()
        val subscribed = CompletableDeferred<Unit>()
        try {
            connection.start()
            val reply = async(start = CoroutineStart.UNDISPATCHED) {
                withTimeoutOrNull(200) { connection.receivedPacketFlow { subscribed.complete(Unit) }.first() }
            }
            subscribed.await()
            val replacement = connection.start()
            connection.receivePacket(replacement, firmware())
            assertNull(reply.await())
        } finally { connection.stopConnection(); connection.shutdown() }
    }

    private class TestConnection : Connection<Unit>() {
        suspend fun publishStates(vararg states: ConnectionState) {
            val published = CompletableDeferred<Unit>()
            launchConnection { job ->
                states.forEach { publishState(job, it) }
                published.complete(Unit)
                awaitCancellation()
            }
            withTimeout(2_000) { published.await() }
        }
        suspend fun start(): Long {
            val started = CompletableDeferred<Long>()
            launchConnection { job ->
                val attempt = beginAttempt(job)!!
                check(installEndpoint(Unit))
                started.complete(attempt)
                awaitCancellation()
            }
            return withTimeout(2_000) { started.await() }
        }
        fun shutdown() { scope.cancel(); backgroundScope.cancel() }
        override fun onFirmwarePacket(job: Job, attempt: Long) = markConnected(job, attempt)
        override fun isEndpointOpen(endpoint: Unit) = true
        override fun writeEndpoint(endpoint: Unit, bytes: ByteArray) = Unit
        override fun closeEndpoint(endpoint: Unit) = Unit
    }
    private fun firmware() = RawPacket(
        intArrayOf('@'.code, 'y'.code) + "SECU-3 test firmware".padEnd(52).map { it.code }.toIntArray() +
            intArrayOf(0, 0, 0, 0, 0x10, 0, 0),
    )
}
