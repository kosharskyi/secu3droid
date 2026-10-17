package org.secu3.android.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

class LoggerSessionTest {
    @Test
    fun binaryOpenFailureIsReportedAndNextStartCanRecover() {
        val session = LoggerSession()
        val recovered = RecordingOutput()

        session.start(openOutput = { throw IOException("open") }, binary = true)
        assertFalse(session.state.value.started)
        assertEquals(1L, session.state.value.pendingErrorId)

        session.start(openOutput = { recovered }, binary = true)
        assertTrue(session.state.value.started)
        session.writeBinary(byteArrayOf(1, 2))
        assertEquals(1, recovered.binaryWrites)
    }

    @Test
    fun csvOpenFailureIsReportedAndNextStartCanRecover() {
        val session = LoggerSession()
        val recovered = RecordingOutput()

        session.start(openOutput = { throw IOException("open") }, binary = false, csvHeader = "header\n")
        assertFalse(session.state.value.started)
        assertEquals(1L, session.state.value.pendingErrorId)

        session.start(openOutput = { recovered }, binary = false)
        session.writeCsv("row\n")
        assertTrue(session.state.value.started)
        assertEquals("row\n", recovered.csvText)
    }

    @Test
    fun binaryWriteFailureClosesOutputStopsAndAllowsRestart() {
        val session = LoggerSession()
        val broken = RecordingOutput(writeFailure = true, closeFailure = true)
        val recovered = RecordingOutput()
        session.start({ broken }, binary = true)

        session.writeBinary(byteArrayOf(1))

        assertFalse(session.state.value.started)
        assertEquals(1, broken.closeCalls)
        assertEquals(1L, session.state.value.pendingErrorId)
        assertEquals(1L, session.state.value.nextErrorId)
        session.start({ recovered }, binary = true)
        assertTrue(session.state.value.started)
    }

    @Test
    fun csvHeaderFlushFailureClosesAndLeavesSessionStopped() {
        val session = LoggerSession()
        val output = RecordingOutput(flushFailure = true)

        session.start({ output }, binary = false, csvHeader = "header\n")

        assertFalse(session.state.value.started)
        assertEquals("header\n", output.csvText)
        assertEquals(1, output.flushCalls)
        assertEquals(1, output.closeCalls)
        assertEquals(1L, session.state.value.pendingErrorId)
    }

    @Test
    fun csvAppendFailureClosesAndLeavesSessionStopped() {
        val session = LoggerSession()
        val output = RecordingOutput(appendFailure = true)
        session.start({ output }, binary = false)

        session.writeCsv("row\n")

        assertFalse(session.state.value.started)
        assertEquals(1, output.closeCalls)
        assertEquals(0, output.flushCalls)
        assertEquals(1L, session.state.value.pendingErrorId)
    }

    @Test
    fun csvWriteAndNormalCloseFailuresAreReportedOnceAndCanRecover() {
        val session = LoggerSession()
        val output = RecordingOutput(flushFailure = true)
        session.start({ output }, binary = false)
        session.writeCsv("row\n")

        assertFalse(session.state.value.started)
        assertEquals(1, output.closeCalls)
        assertEquals(1L, session.state.value.nextErrorId)
        session.acknowledgeError(0)
        assertEquals(1L, session.state.value.pendingErrorId)
        session.acknowledgeError(1)
        assertNull(session.state.value.pendingErrorId)

        val closeFailure = RecordingOutput(closeFailure = true)
        session.start({ closeFailure }, binary = false)
        session.stop()
        assertFalse(session.state.value.started)
        assertEquals(1, closeFailure.closeCalls)
        assertEquals(2L, session.state.value.pendingErrorId)
    }

    @Test
    fun stopWaitsForInFlightWriteBeforeClosingOutput() {
        val session = LoggerSession()
        val output = BlockingOutput()
        session.start({ output }, binary = true)
        val writeThread = thread { session.writeBinary(byteArrayOf(1)) }
        assertTrue(output.writeEntered.await(1, TimeUnit.SECONDS))
        val stopAttempted = CountDownLatch(1)
        val stopFinished = CountDownLatch(1)
        val stopThread = thread {
            stopAttempted.countDown()
            session.stop()
            stopFinished.countDown()
        }
        try {
            assertTrue(stopAttempted.await(1, TimeUnit.SECONDS))
            assertFalse(stopFinished.await(100, TimeUnit.MILLISECONDS))
            assertEquals(0, output.closeCalls)
        } finally {
            output.allowWriteToFinish.countDown()
            writeThread.join(1_000)
            stopThread.join(1_000)
        }
        assertFalse(session.state.value.started)
        assertTrue(output.closedAfterWrite)
        assertEquals(1, output.closeCalls)
    }

    private open class RecordingOutput(
        private val writeFailure: Boolean = false,
        private val appendFailure: Boolean = false,
        private val flushFailure: Boolean = false,
        private val closeFailure: Boolean = false,
    ) : LoggerOutput {
        var binaryWrites = 0
        var csvText = ""
        var flushCalls = 0
        var closeCalls = 0

        override fun writeBinary(bytes: ByteArray) {
            binaryWrites++
            if (writeFailure) throw IOException("write")
        }

        override fun appendCsv(text: String) {
            if (appendFailure) throw IOException("append")
            csvText += text
        }

        override fun flushCsv() {
            flushCalls++
            if (flushFailure) throw IOException("flush")
        }

        override fun close() {
            closeCalls++
            if (closeFailure) throw IOException("close")
        }
    }

    private class BlockingOutput : RecordingOutput() {
        val writeEntered = CountDownLatch(1)
        val allowWriteToFinish = CountDownLatch(1)
        @Volatile var closedAfterWrite = false
        @Volatile private var writeFinished = false

        override fun writeBinary(bytes: ByteArray) {
            writeEntered.countDown()
            allowWriteToFinish.await(1, TimeUnit.SECONDS)
            writeFinished = true
        }

        override fun close() {
            closedAfterWrite = writeFinished
            super.close()
        }
    }
}
