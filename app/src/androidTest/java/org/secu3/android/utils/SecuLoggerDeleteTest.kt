package org.secu3.android.utils

import android.content.Context
import android.content.ContextWrapper
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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
        runBlocking {
            logger.stopLogging()
            logger.shutdownForTests()
        }
        testRoot.deleteRecursively()
        context.deleteSharedPreferences(preferenceStoreName)
    }

    @Test
    fun activeCsvLogCannotBeDeletedAndRecordingAndPendingMarkContinue() = runBlocking {
        prefs.isBinaryLogFormatEnabled = false
        logger.startLogging()
        logger.awaitCommandsForTests()
        val activeFile = files.listOfLogs.single()
        logger.setMark1()
        logger.setMark2()
        logger.setMark3()
        logger.startLogging()

        assertEquals(LogDeletionResult.Active, logger.deleteLogFile(activeFile))
        assertTrue(logger.isLoggerStarted)
        logger.logPacket(SensorsPacket())
        logger.logPacket(SensorsPacket())
        logger.stopLogging()
        logger.awaitCommandsForTests()

        val firstSessionMarks = activeFile.readText().trimEnd().split("\r\n").map { row ->
            val columns = row.split(';')
            columns[columns.lastIndex - 2].trim()
        }
        assertEquals(listOf("4", "0"), firstSessionMarks)

        val filesBeforeRestart = files.listOfLogs.toSet()
        logger.setMark2()
        logger.startLogging()
        logger.awaitCommandsForTests()
        val restartedFile = files.listOfLogs.singleOrNull { it !in filesBeforeRestart } ?: activeFile
        logger.logPacket(SensorsPacket())
        logger.stopLogging()
        logger.awaitCommandsForTests()

        val restartedMarkColumns = restartedFile.readText().trimEnd().split("\r\n").map { row ->
            val columns = row.split(';')
            columns[columns.lastIndex - 2].trim()
        }
        assertEquals("0", restartedMarkColumns.last())
        assertEquals(LogDeletionResult.Deleted, logger.deleteLogFile(activeFile))
        assertFalse(activeFile.exists())
    }

    @Test
    fun activeS3lLogCannotBeDeletedAndRecordingContinues() = runBlocking {
        prefs.isBinaryLogFormatEnabled = true
        logger.startLogging()
        logger.awaitCommandsForTests()
        val activeFile = files.listOfLogs.single()

        assertEquals(LogDeletionResult.Active, logger.deleteLogFile(activeFile))
        logger.logPacket(SensorsPacket())
        logger.stopLogging()

        assertEquals(252L, activeFile.length())
        assertEquals(LogDeletionResult.Deleted, logger.deleteLogFile(activeFile))
    }

    @Test
    fun openFailureStaysPendingUntilAcknowledgedAndLoggerCanRestart() = runBlocking {
        prefs.isBinaryLogFormatEnabled = false
        val logsPath = File(context.filesDir, "logs")
        logsPath.deleteRecursively()
        logsPath.writeText("not a directory")

        logger.startLogging()
        logger.awaitCommandsForTests()
        assertFalse(logger.isLoggerStarted)
        val errorId = logger.state.value.pendingErrorId
        assertTrue(errorId != null)

        logger.acknowledgeLoggerError(errorId!!)
        logger.awaitCommandsForTests()
        assertNull(logger.state.value.pendingErrorId)

        assertTrue(logsPath.delete())
        logger.startLogging()
        logger.awaitCommandsForTests()
        assertTrue(logger.isLoggerStarted)
        logger.stopLogging()
        logger.awaitCommandsForTests()
    }

    @Test
    fun unrelatedLogCanBeDeletedWhileRecordingAndDeleteFailureIsReported() = runBlocking {
        prefs.isBinaryLogFormatEnabled = false
        logger.startLogging()
        logger.awaitCommandsForTests()
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
    fun startEnqueuesWithoutWaitingForDeletionAndRunsAfterIt() = runBlocking {
        prefs.isBinaryLogFormatEnabled = false
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
        val deleteThread = thread {
            result = runBlocking { logger.deleteLogFile(blockingFile) }
        }
        try {
            assertTrue(fileEnteredDelete.await(5, TimeUnit.SECONDS))
            logger.startLogging()
            assertFalse(logger.isLoggerStarted)
        } finally {
            allowDelete.countDown()
            deleteThread.join(5_000)
        }

        assertEquals(LogDeletionResult.Deleted, result)
        logger.awaitCommandsForTests()
        assertTrue(logger.isLoggerStarted)
        logger.logPacket(SensorsPacket())
        logger.stopLogging()
        assertTrue(files.logsDir.listFiles()?.any { it.extension == "csv" && it.length() > 0 } == true)
    }
}
