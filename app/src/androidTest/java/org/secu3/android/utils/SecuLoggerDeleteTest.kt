package org.secu3.android.utils

import android.content.Context
import android.content.ContextWrapper
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.secu3.android.models.packets.input.SensorsPacket
import org.secu3.android.ui.messages.UiMessageQueue
import org.secu3.android.utils.prefs.IsolatedUserPrefs
import org.secu3.android.utils.prefs.UserPrefs
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
    private lateinit var prefsFixture: IsolatedUserPrefs
    private lateinit var messages: UiMessageQueue

    @Before
    fun setUp() {
        val base = InstrumentationRegistry.getInstrumentation().targetContext
        testRoot = File(base.cacheDir, "secu-logger-delete-${UUID.randomUUID()}").also { it.mkdirs() }
        context = object : ContextWrapper(base) {
            override fun getFilesDir() = File(testRoot, "files").also { it.mkdirs() }
            override fun getCacheDir() = File(testRoot, "cache").also { it.mkdirs() }
        }
        prefsFixture = IsolatedUserPrefs(context, testRoot.name)
        prefs = prefsFixture.prefs
        files = FileHelper(context)
        messages = UiMessageQueue()
        logger = SecuLogger(prefs, files, messages, SensorLogEncoder())
    }

    @After
    fun tearDown() {
        runBlocking {
            logger.stopLogging()
            logger.awaitCommandsForTests()
            logger.shutdownForTests()
            prefsFixture.close()
        }
        testRoot.deleteRecursively()
    }

    @Test
    fun activeCsvLogCannotBeDeletedAndRecordingAndPendingMarkContinue() = runBlocking {
        prefs.setBinaryLogFormatEnabled(false)
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
        prefs.setBinaryLogFormatEnabled(true)
        logger.startLogging()
        logger.awaitCommandsForTests()
        val activeFile = files.listOfLogs.single()

        assertEquals(LogDeletionResult.Active, logger.deleteLogFile(activeFile))
        logger.logPacket(SensorsPacket())
        logger.stopLogging()
        logger.awaitCommandsForTests()

        assertEquals(252L, activeFile.length())
        assertEquals(LogDeletionResult.Deleted, logger.deleteLogFile(activeFile))
    }

    @Test
    fun loggerHasNoAutomaticPolicyAndDoesNotUseControlVisibilityAsPermission() = runBlocking {
        assertFalse(prefs.settings.first().showLogControls)
        assertFalse(prefs.settings.first().isAlwaysWriteLogEnabled)
        logger.logPacket(SensorsPacket(rpm = 500))
        logger.awaitCommandsForTests()
        assertFalse(logger.isLoggerStarted)
        assertTrue(files.listOfLogs.isEmpty())

        prefs.setShowLogControls(true)
        prefs.setAlwaysWriteLogEnabled(true)
        logger.logPacket(SensorsPacket(rpm = 1_000))
        logger.awaitCommandsForTests()
        assertFalse("logger has no automatic-start policy", logger.isLoggerStarted)

        logger.startLogging()
        withTimeout(5_000) { logger.state.first { it } }
        val activeFile = files.listOfLogs.single()
        prefs.setShowLogControls(false)
        prefs.setAlwaysWriteLogEnabled(false)
        logger.logPacket(SensorsPacket(rpm = 2_000))
        assertTrue("settings policy is owned by manager, not logger", logger.isLoggerStarted)
        assertTrue(activeFile.readText().contains("2000"))
        logger.stopLogging()
        logger.awaitCommandsForTests()
        assertFalse(logger.isLoggerStarted)
    }

    @Test
    fun openFailurePublishesErrorAndLoggerCanRestart() = runBlocking {
        prefs.setBinaryLogFormatEnabled(false)
        val logsPath = File(context.filesDir, "logs")
        logsPath.deleteRecursively()
        logsPath.writeText("not a directory")

        logger.startLogging()
        logger.awaitCommandsForTests()
        assertFalse(logger.isLoggerStarted)
        val error = messages.pending.value.single { it.textRes == org.secu3.android.R.string.sensor_logger_io_error }

        messages.acknowledge(error.id)
        assertTrue(messages.pending.value.none { it.textRes == org.secu3.android.R.string.sensor_logger_io_error })

        assertTrue(logsPath.delete())
        logger.startLogging()
        logger.awaitCommandsForTests()
        assertTrue(logger.isLoggerStarted)
        logger.stopLogging()
        logger.awaitCommandsForTests()
    }

    @Test
    fun writeFailureClosesActiveFilePublishesOneErrorAndAllowsRestart() = runBlocking {
        prefs.setBinaryLogFormatEnabled(false)
        logger.startLogging()
        logger.awaitCommandsForTests()
        val activeLogField = SecuLogger::class.java.getDeclaredField("activeLog").apply { isAccessible = true }
        val activeLog = activeLogField.get(logger)
        val writerField = activeLog!!::class.java.getDeclaredField("csvWriter").apply { isAccessible = true }
        (writerField.get(activeLog) as java.io.FileWriter).close()

        logger.logPacket(SensorsPacket(rpm = 1_500))
        logger.awaitCommandsForTests()

        assertFalse(logger.isLoggerStarted)
        assertEquals(
            1,
            messages.pending.value.count { it.textRes == org.secu3.android.R.string.sensor_logger_io_error },
        )

        logger.startLogging()
        logger.awaitCommandsForTests()
        assertTrue(logger.isLoggerStarted)
        assertEquals(
            1,
            messages.pending.value.count { it.textRes == org.secu3.android.R.string.sensor_logger_io_error },
        )
        logger.stopLogging()
        logger.awaitCommandsForTests()
    }

    @Test
    fun unrelatedLogCanBeDeletedWhileRecordingAndDeleteFailureIsReported() = runBlocking {
        prefs.setBinaryLogFormatEnabled(false)
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
        prefs.setBinaryLogFormatEnabled(false)
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
