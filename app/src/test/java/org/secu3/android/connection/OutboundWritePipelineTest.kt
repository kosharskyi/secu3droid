package org.secu3.android.connection

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.cancel
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.secu3.android.models.packets.base.OutputPacket
import org.secu3.android.models.packets.out.ChangeModePacket
import java.util.Collections
import java.util.concurrent.CopyOnWriteArrayList

class OutboundWritePipelineTest {

    @Test
    fun queuePreservesFifoAcrossPacketKindsAndCopiesEncodedBytes() = runBlocking {
        val endpoint = Endpoint()
        val writes = Collections.synchronizedList(mutableListOf<ByteArray>())
        val completed = CompletableDeferred<Unit>()
        val workerJob = SupervisorJob()
        val workerScope = CoroutineScope(coroutineContext + workerJob)
        val queue = SerialWriteQueue<Endpoint>(
            scope = workerScope,
            isCurrentEndpoint = { it.open },
            write = { _, bytes ->
                writes += bytes
                if (writes.size == 3) completed.complete(Unit)
            },
            onFailure = { throw AssertionError(it) },
        )
        try {
            val first = wire('a')
            queue.enqueue(endpoint, first)
            first[1] = 'z'.code.toByte()
            first.fill(0)
            queue.enqueue(endpoint, wire('s'))
            queue.enqueue(endpoint, wire('e'))

            withTimeout(2_000) { completed.await() }
            assertArrayEquals(wire('a'), writes[0])
            assertArrayEquals(wire('s'), writes[1])
            assertArrayEquals(wire('e'), writes[2])
        } finally {
            workerJob.cancelAndJoin()
        }
    }

    @Test
    fun replacedStillOpenEndpointDropsPendingWritesWithoutRedirectingThem() = runBlocking {
        val oldEndpoint = Endpoint()
        val newEndpoint = Endpoint()
        var current = oldEndpoint
        val firstWriteStarted = CompletableDeferred<Unit>()
        val releaseFirstWrite = CompletableDeferred<Unit>()
        val oldWrites = Collections.synchronizedList(mutableListOf<ByteArray>())
        val newWrites = Collections.synchronizedList(mutableListOf<ByteArray>())
        val workerJob = SupervisorJob()
        val workerScope = CoroutineScope(coroutineContext + workerJob + Dispatchers.IO)
        val queue = SerialWriteQueue<Endpoint>(
            scope = workerScope,
            isCurrentEndpoint = { it === current && it.open },
            write = { endpoint, bytes ->
                if (bytes[1] == 'a'.code.toByte()) {
                    firstWriteStarted.complete(Unit)
                    runBlocking { releaseFirstWrite.await() }
                }
                (if (endpoint === oldEndpoint) oldWrites else newWrites) += bytes
            },
            onFailure = { throw AssertionError(it) },
        )
        try {
            queue.enqueue(oldEndpoint, wire('a'))
            withTimeout(2_000) { firstWriteStarted.await() }
            queue.enqueue(oldEndpoint, wire('b'))
            current = newEndpoint
            releaseFirstWrite.complete(Unit)
            queue.enqueue(newEndpoint, wire('c'))
            withTimeout(2_000) {
                while (newWrites.size != 1) kotlinx.coroutines.delay(1)
            }

            assertTrue(oldEndpoint.open)
            assertEquals(listOf('a'.code), oldWrites.map { it[1].toInt() })
            assertEquals(listOf('c'.code), newWrites.map { it[1].toInt() })
        } finally {
            releaseFirstWrite.complete(Unit)
            workerJob.cancelAndJoin()
        }
    }

    @Test
    fun disconnectedWriteIsDroppedAndFailureDoesNotRetryOrStopQueue() = runBlocking {
        val endpoint = Endpoint(open = false)
        val attempts = mutableListOf<Byte>()
        val failures = mutableListOf<Exception>()
        val completed = CompletableDeferred<Unit>()
        val workerJob = SupervisorJob()
        val workerScope = CoroutineScope(coroutineContext + workerJob)
        val queue = SerialWriteQueue<Endpoint>(
            scope = workerScope,
            isCurrentEndpoint = { it.open },
            write = { _, bytes ->
                attempts += bytes[1]
                if (bytes[1] == 'f'.code.toByte()) throw IllegalStateException("one attempt")
                completed.complete(Unit)
            },
            onFailure = failures::add,
        )
        try {
            queue.enqueue(endpoint, wire('d'))
            endpoint.open = true
            queue.enqueue(endpoint, wire('f'))
            queue.enqueue(endpoint, wire('s'))
            withTimeout(2_000) { completed.await() }

            assertEquals(listOf('f'.code.toByte(), 's'.code.toByte()), attempts)
            assertEquals(1, failures.size)
            assertFalse(attempts.contains('d'.code.toByte()))
        } finally {
            workerJob.cancelAndJoin()
        }
    }

    @Test
    fun inheritedSendDataSnapshotsOnCallAndPreservesSameClassFifo() = runBlocking {
        val connection = TestConnection()
        val endpoint = Endpoint()
        connection.start()
        assertTrue(connection.attach(endpoint))
        try {
            var payload = intArrayOf('x'.code, 1)
            val packet = object : OutputPacket {
                override fun pack(): IntArray = payload
            }
            connection.sendData(packet)
            payload = intArrayOf('x'.code, 2)
            connection.sendData(packet)
            connection.sendData(ChangeModePacket('s'))
            awaitWrites(connection, 3)

            assertArrayEquals(
                byteArrayOf('!'.code.toByte(), 'x'.code.toByte(), 1, 3, 2, 0x0D),
                connection.writes[0],
            )
            assertArrayEquals(
                byteArrayOf('!'.code.toByte(), 'x'.code.toByte(), 2, 5, 3, 0x0D),
                connection.writes[1],
            )
            assertArrayEquals(
                byteArrayOf('!'.code.toByte(), 'h'.code.toByte(), 's'.code.toByte(), 0, 0x5B, 0x75, 0x0D),
                connection.writes[2],
            )
        } finally {
            connection.stopConnection()
            connection.shutdown()
        }
    }

    @Test
    fun inheritedSendDataEscapesReservedPayloadAndChecksumGolden() = runBlocking {
        val connection = TestConnection()
        connection.start()
        assertTrue(connection.attach(Endpoint()))
        try {
            connection.sendData(object : OutputPacket {
                override fun pack(): IntArray = intArrayOf('x'.code, 0x0D, 0x0A, 0x21)
            })
            awaitWrites(connection, 1)
            assertArrayEquals(
                byteArrayOf('!'.code.toByte(), 'x'.code.toByte(), 0x0A, 0x83.toByte(), 0x0A, 0x84.toByte(), 0x0A, 0x81.toByte(), 0x97.toByte(), 0x3B, 0x0D),
                connection.writes[0],
            )
        } finally {
            connection.stopConnection()
            connection.shutdown()
        }
    }

    private suspend fun awaitWrites(connection: TestConnection, expected: Int) {
        withTimeout(2_000) {
            while (connection.writes.size < expected) kotlinx.coroutines.delay(1)
        }
    }

    private suspend fun awaitClosed(endpoint: Endpoint) {
        withTimeout(2_000) {
            while (!endpoint.closed) kotlinx.coroutines.delay(1)
        }
    }

    @Test
    fun staleInvalidationCannotCloseReplacementAndStopClosesConnectingEndpoint() = runBlocking {
        val connection = TestConnection()
        val oldEndpoint = Endpoint()
        val replacement = Endpoint()
        connection.start()
        assertTrue(connection.attach(oldEndpoint))
        assertTrue(connection.remove(oldEndpoint))
        assertTrue(connection.attach(replacement))

        assertFalse(connection.fail(oldEndpoint))
        assertFalse(oldEndpoint.closed)
        assertTrue(connection.isConnected)

        connection.stopConnection()
        awaitClosed(replacement)
        assertTrue(replacement.closed)
        assertFalse(connection.isConnected)
        connection.shutdown()

        val reconnecting = TestConnection()
        val connectingEndpoint = Endpoint(open = false)
        reconnecting.start()
        assertTrue(reconnecting.attach(connectingEndpoint))
        reconnecting.stopConnection()
        awaitClosed(connectingEndpoint)
        assertTrue(connectingEndpoint.closed)
        assertFalse(reconnecting.isConnected)
        reconnecting.shutdown()
    }

    @Test
    fun stopCancelsConnectionWorkAndQueueStillWritesAfterRestart() = runBlocking {
        val connection = TestConnection()
        val firstEndpoint = Endpoint()
        val workStarted = CompletableDeferred<Unit>()
        val workCancelled = CompletableDeferred<Unit>()

        try {
            connection.start()
            assertTrue(connection.attach(firstEndpoint))
            connection.startCancelableWork(workStarted, workCancelled)
            withTimeout(2_000) { workStarted.await() }

            connection.sendData(ChangeModePacket('s'))
            awaitWrites(connection, 1)

            connection.stopConnection()
            withTimeout(2_000) { workCancelled.await() }
            awaitClosed(firstEndpoint)
            assertTrue(firstEndpoint.closed)

            val secondEndpoint = Endpoint()
            connection.start()
            assertTrue(connection.attach(secondEndpoint))
            connection.sendData(ChangeModePacket('s'))
            awaitWrites(connection, 2)
            assertEquals(2, connection.writes.size)
        } finally {
            connection.stopConnection()
            connection.shutdown()
        }
    }

    @Test
    fun secondStopDoesNotCancelPendingDisconnectedNotification() = runBlocking {
        val connection = TestConnection()
        val collectorEntered = CompletableDeferred<Unit>()
        val releaseCollector = CompletableDeferred<Unit>()
        val disconnectedReceived = CompletableDeferred<Unit>()
        val collector = launch {
            connection.connectionStateFlow.collect { state ->
                when (state) {
                    InProgress -> {
                        collectorEntered.complete(Unit)
                        releaseCollector.await()
                    }
                    Disconnected -> disconnectedReceived.complete(Unit)
                    else -> Unit
                }
            }
        }
        try {
            withTimeout(2_000) { connection.awaitStateSubscriber() }
            val initialState = async { connection.publishState(InProgress) }
            withTimeout(2_000) { collectorEntered.await() }

            connection.start()
            connection.stopConnection()
            withTimeout(2_000) { connection.awaitDisconnectStarted() }
            connection.stopConnection()

            releaseCollector.complete(Unit)
            withTimeout(2_000) { disconnectedReceived.await() }
            initialState.cancelAndJoin()
        } finally {
            releaseCollector.complete(Unit)
            collector.cancelAndJoin()
            connection.stopConnection()
            connection.shutdown()
        }
    }

    private fun wire(value: Char) = byteArrayOf('!'.code.toByte(), value.code.toByte())

    private class Endpoint(var open: Boolean = true, var closed: Boolean = false)

    private class TestConnection : Connection<Endpoint>() {
        val writes = CopyOnWriteArrayList<ByteArray>()
        private val disconnectStarted = CompletableDeferred<Unit>()

        fun start() {
            isRunning = true
        }

        fun startCancelableWork(started: CompletableDeferred<Unit>, cancelled: CompletableDeferred<Unit>) {
            scope.launch {
                started.complete(Unit)
                try {
                    awaitCancellation()
                } finally {
                    cancelled.complete(Unit)
                }
            }
        }

        fun shutdown() {
            cancelScopes()
        }

        suspend fun publishState(state: ConnectionState) {
            mConnectionStateFlow.emit(state)
        }

        suspend fun awaitStateSubscriber() {
            mConnectionStateFlow.subscriptionCount.first { it > 0 }
        }

        suspend fun awaitDisconnectStarted() {
            disconnectStarted.await()
        }

        fun attach(endpoint: Endpoint): Boolean = installEndpoint(endpoint)

        fun remove(endpoint: Endpoint): Boolean = detachEndpoint(endpoint)

        fun fail(endpoint: Endpoint): Boolean = invalidateEndpoint(endpoint)

        override fun disconnect() {
            val endpoint = detachCurrentEndpoint()
            backgroundScope.launch {
                endpoint?.let(::closeEndpoint)
                disconnectStarted.complete(Unit)
                mConnectionStateFlow.emit(Disconnected)
            }
        }

        override fun isEndpointOpen(endpoint: Endpoint): Boolean = endpoint.open

        override fun writeEndpoint(endpoint: Endpoint, bytes: ByteArray) {
            writes += bytes.copyOf()
        }

        override fun closeEndpoint(endpoint: Endpoint) {
            endpoint.closed = true
            endpoint.open = false
        }


    }

}
