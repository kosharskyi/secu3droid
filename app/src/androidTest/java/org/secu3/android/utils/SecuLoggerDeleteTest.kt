package org.secu3.android.utils

import android.content.Context
import android.content.ContextWrapper
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.secu3.android.models.packets.input.SensorsPacket
import java.io.File
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

class SecuLoggerDeleteTest {
    private lateinit var testRoot: File
    private lateinit var context: Context
    private lateinit var logger: SecuLogger
    private lateinit var prefs: UserPrefs
    private lateinit var files: FileHelper
    private lateinit var preferenceStoreName: String

    @Before
    fun setUp() {
        val base = InstrumentationRegistry.getInstrumentation().targetContext
        testRoot = File(base.cacheDir, "secu-logger-delete-${UUID.randomUUID()}").also { it.mkdirs() }
        preferenceStoreName = "secu-logger-delete-${testRoot.name}"
        context = object : ContextWrapper(base) {
            override fun getFilesDir() = File(testRoot, "files").also { it.mkdirs() }
            override fun getCacheDir() = File(testRoot, "cache").also { it.mkdirs() }
            override fun getSharedPreferences(name: String, mode: Int) =
                base.getSharedPreferences(preferenceStoreName, mode)
        }
        prefs = UserPrefs(context)
        files = FileHelper(context)
        logger = SecuLogger(prefs, files)
        prefs.isSensorLoggerEnabled = true
    }

    @After
    fun tearDown() {
        logger.stopLogging()
        testRoot.deleteRecursively()
        context.deleteSharedPreferences(preferenceStoreName)
    }

    @Test
    fun activeCsvLogCannotBeDeletedAndRecordingAndPendingMarkContinue() {
        prefs.isBinaryLogFormatEnabled = false
        logger.prepareToLog()
        logger.startLogging()
        val activeFile = files.listOfLogs.single()
        logger.setMark1()

        assertEquals(LogDeletionResult.Active, logger.deleteLogFile(activeFile))
        assertTrue(logger.isLoggerStarted)
        logger.logPacket(SensorsPacket())
        logger.logPacket(SensorsPacket())
        logger.stopLogging()

        assertTrue(activeFile.length() > 0)
        val markColumns = activeFile.readText().trimEnd().split("\r\n").map { row ->
            val columns = row.split(';')
            columns[columns.lastIndex - 2].trim()
        }
        assertEquals(listOf("1", "0"), markColumns)
        assertEquals(LogDeletionResult.Deleted, logger.deleteLogFile(activeFile))
        assertFalse(activeFile.exists())
    }

    @Test
    fun activeS3lLogCannotBeDeletedAndRecordingContinues() {
        prefs.isBinaryLogFormatEnabled = true
        logger.prepareToLog()
        logger.startLogging()
        val activeFile = files.listOfLogs.single()

        assertEquals(LogDeletionResult.Active, logger.deleteLogFile(activeFile))
        logger.logPacket(SensorsPacket())
        logger.stopLogging()

        assertEquals(252L, activeFile.length())
        assertEquals(LogDeletionResult.Deleted, logger.deleteLogFile(activeFile))
    }

    @Test
    fun unrelatedLogCanBeDeletedWhileRecordingAndDeleteFailureIsReported() {
        prefs.isBinaryLogFormatEnabled = false
        logger.prepareToLog()
        logger.startLogging()
        val activeFile = files.listOfLogs.single()
        val unrelatedFile = File(files.logsDir, "unrelated.csv").apply { writeText("old") }

        assertEquals(LogDeletionResult.Deleted, logger.deleteLogFile(unrelatedFile))
        assertTrue(logger.isLoggerStarted)
        logger.logPacket(SensorsPacket())
        assertTrue(activeFile.length() > 0)

        val nonEmptyDirectory = File(files.logsDir, "directory.csv").apply {
            mkdir()
            File(this, "child").writeText("keep")
        }
        assertEquals(LogDeletionResult.Failed, logger.deleteLogFile(nonEmptyDirectory))
        assertTrue(logger.isLoggerStarted)
    }

    @Test
    fun startingWaitsUntilDeletionLeavesLoggerCriticalSection() {
        prefs.isBinaryLogFormatEnabled = false
        logger.prepareToLog()
        val fileEnteredDelete = CountDownLatch(1)
        val allowDelete = CountDownLatch(1)
        val fileToDelete = File(files.logsDir, "blocking.csv").apply { writeText("old") }
        val blockingFile = object : File(fileToDelete.absolutePath) {
            override fun delete(): Boolean {
                fileEnteredDelete.countDown()
                check(allowDelete.await(5, TimeUnit.SECONDS))
                return super.delete()
            }
        }
        var result: LogDeletionResult? = null
        val deleteThread = thread { result = logger.deleteLogFile(blockingFile) }
        var startThread: Thread? = null
        val startAttempted = CountDownLatch(1)
        val startFinished = CountDownLatch(1)
        try {
            assertTrue(fileEnteredDelete.await(5, TimeUnit.SECONDS))
            startThread = thread {
                startAttempted.countDown()
                logger.startLogging()
                startFinished.countDown()
            }
            assertTrue(startAttempted.await(5, TimeUnit.SECONDS))
            assertFalse(startFinished.await(100, TimeUnit.MILLISECONDS))
        } finally {
            allowDelete.countDown()
            deleteThread.join(5_000)
            startThread?.join(5_000)
        }

        assertEquals(LogDeletionResult.Deleted, result)
        assertTrue(logger.isLoggerStarted)
        logger.logPacket(SensorsPacket())
        logger.stopLogging()
        assertTrue(files.logsDir.listFiles()?.any { it.extension == "csv" && it.length() > 0 } == true)
    }
}
