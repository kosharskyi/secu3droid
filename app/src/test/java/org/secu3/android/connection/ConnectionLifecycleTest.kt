package org.secu3.android.connection

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test
import org.secu3.android.models.RawPacket

class ConnectionLifecycleTest {
    @Test fun replacedAttemptCannotConfirmItsSuccessor() = runBlocking {
        val connection = TestConnection()
        try {
            val first = connection.start(1)
            val second = connection.start(2)
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
            assertNull(connection.latestEvent.firmware)
        } finally { connection.shutdown() }
    }

    @Test fun subscribedRequestCannotAcceptReplyFromReplacementAttempt() = runBlocking {
        val connection = TestConnection()
        val subscribed = CompletableDeferred<Unit>()
        try {
            connection.start(1)
            val reply = async(start = CoroutineStart.UNDISPATCHED) {
                withTimeoutOrNull(200) { connection.receivedPacketFlow { subscribed.complete(Unit) }.first() }
            }
            subscribed.await()
            val replacement = connection.start(2)
            connection.receivePacket(replacement, firmware())
            assertNull(reply.await())
        } finally { connection.stopConnection(); connection.shutdown() }
    }

    private class TestConnection : Connection<Unit>() {
        suspend fun start(generation: Long): Long {
            val started = CompletableDeferred<Long>()
            launchConnection(generation) { job ->
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
