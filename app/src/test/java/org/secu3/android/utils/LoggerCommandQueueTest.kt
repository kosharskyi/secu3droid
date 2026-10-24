package org.secu3.android.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class LoggerCommandQueueTest {
    @Test
    fun postedControlsDoNotWaitForBlockedWriteAndCommandsStayFifo() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val failures = CopyOnWriteArrayList<Exception>()
        val queue = LoggerCommandQueue(scope, onFailure = failures::add)
        val writeEntered = CountDownLatch(1)
        val releaseWrite = CountDownLatch(1)
        val order = CopyOnWriteArrayList<String>()
        val firstPacket = async(start = CoroutineStart.UNDISPATCHED) {
            queue.await {
                writeEntered.countDown()
                check(releaseWrite.await(5, TimeUnit.SECONDS))
                order += "packet-1"
            }
        }

        try {
            assertTrue(writeEntered.await(2, TimeUnit.SECONDS))
            val controlsPosted = async {
                queue.post { order += "start" }
                queue.post { order += "stop" }
                queue.post { order += "mark-1" }
                queue.post { order += "mark-2" }
                queue.post { order += "mark-3" }
            }
            withTimeout(1_000) { controlsPosted.await() }

            val secondPacket = async(start = CoroutineStart.UNDISPATCHED) {
                queue.await { order += "packet-2" }
            }
            assertFalse(secondPacket.isCompleted)
            releaseWrite.countDown()
            withTimeout(2_000) { firstPacket.await() }
            withTimeout(2_000) { secondPacket.await() }

            assertEquals(
                listOf("packet-1", "start", "stop", "mark-1", "mark-2", "mark-3", "packet-2"),
                order.toList(),
            )
            assertTrue(failures.isEmpty())
        } finally {
            releaseWrite.countDown()
            queue.closeAndJoin()
            scope.cancel()
        }
    }

    @Test
    fun failedAwaitedCommandCompletesCallerAndWorkerContinues() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val failures = CopyOnWriteArrayList<Exception>()
        val queue = LoggerCommandQueue(scope, onFailure = failures::add)

        try {
            val failure = runCatching { queue.await { throw IOException("write") } }.exceptionOrNull()
            assertTrue(failure is IOException)
            assertEquals("still running", queue.await { "still running" })
            assertEquals(1, failures.size)
        } finally {
            queue.closeAndJoin()
            scope.cancel()
        }
    }

    @Test
    fun cancellingWorkerCompletesQueuedAwaiters() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val callerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val queue = LoggerCommandQueue(scope, onFailure = {})
        val commandEntered = CountDownLatch(1)
        val releaseCommand = CountDownLatch(1)
        val pendingCommandRan = java.util.concurrent.atomic.AtomicBoolean(false)
        val runningCommand = callerScope.async(start = CoroutineStart.UNDISPATCHED) {
            queue.await {
                commandEntered.countDown()
                check(releaseCommand.await(5, TimeUnit.SECONDS))
            }
        }

        try {
            assertTrue(commandEntered.await(2, TimeUnit.SECONDS))
            val pendingCommand = callerScope.async(start = CoroutineStart.UNDISPATCHED) {
                queue.await { pendingCommandRan.set(true) }
            }
            assertFalse(pendingCommand.isCompleted)
            scope.cancel()
            releaseCommand.countDown()

            withTimeout(2_000) { runningCommand.await() }
            val failure = runCatching { withTimeout(2_000) { pendingCommand.await() } }.exceptionOrNull()
            assertTrue("unexpected pending command result: $failure", failure is kotlinx.coroutines.CancellationException)
            assertFalse(failure is TimeoutCancellationException)
            assertFalse(pendingCommandRan.get())
        } finally {
            releaseCommand.countDown()
            queue.closeAndJoin()
            scope.cancel()
            callerScope.cancel()
        }
    }

    @Test
    fun alreadyCancelledScopeRejectsAwaitedCommand() = runBlocking {
        val job = SupervisorJob().apply { cancel() }
        val scope = CoroutineScope(job + Dispatchers.IO)
        val queue = LoggerCommandQueue(scope, onFailure = {})

        try {
            val failure = runCatching {
                withTimeout(1_000) { queue.await { "should not run" } }
            }.exceptionOrNull()
            assertTrue(failure is kotlinx.coroutines.CancellationException)
            assertFalse(failure is TimeoutCancellationException)
        } finally {
            queue.closeAndJoin()
            scope.cancel()
        }
    }
}
