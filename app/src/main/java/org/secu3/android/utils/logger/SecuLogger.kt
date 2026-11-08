/*
 *    SecuDroid  - An open source, free manager for SECU-3 engine control unit
 *    Copyright (C) 2024 Vitaliy O. Kosharskyi. Ukraine, Kyiv
 *
 *    SECU-3  - An open source, free engine control unit
 *    Copyright (C) 2007-2024 Alexey A. Shabelnikov. Ukraine, Kyiv
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.secu3.android.utils.logger

import org.secu3.android.utils.FileHelper

import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import org.secu3.android.R
import org.secu3.android.models.packets.input.SensorsPacket
import org.secu3.android.ui.messages.UiMessageDuration
import org.secu3.android.ui.messages.UiMessageQueue
import org.secu3.android.utils.prefs.UserPrefs
import org.secu3.android.utils.analytics.UsageAnalytics
import org.threeten.bp.LocalTime
import java.io.File
import java.io.FileOutputStream
import java.io.FileWriter
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecuLogger @Inject constructor(
    private val prefs: UserPrefs,
    private val fileHelper: FileHelper,
    private val uiMessageQueue: UiMessageQueue,
    private val encoder: SensorLogEncoder,
    private val usageAnalytics: UsageAnalytics,
) {
    private data class ActiveLog(
        val file: File,
        val binaryStream: FileOutputStream? = null,
        val csvWriter: FileWriter? = null,
    ) {
        val isBinary: Boolean get() = binaryStream != null
    }

    private val loggerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val commands = LoggerCommandQueue(loggerScope) { failure ->
        Log.e("SecuLogger", "Logger command failed", failure)
    }
    private val mutableState = MutableStateFlow(false)
    internal val state: StateFlow<Boolean> = mutableState.asStateFlow()
    val isLoggerStarted: Boolean get() = mutableState.value

    private var activeLog: ActiveLog? = null
    private var mark: Int = 0
    private var csvDelimiter: String = ";"

    /** Delete shares the command FIFO with writes so a file cannot become active between checking and deleting it. */
    suspend fun deleteLogFile(file: File): LogDeletionResult = commands.await {
        val current = activeLog
        if (current != null && sameFile(current.file, file)) {
            return@await LogDeletionResult.Active
        }
        try {
            if (file.delete()) LogDeletionResult.Deleted else LogDeletionResult.Failed
        } catch (_: SecurityException) {
            LogDeletionResult.Failed
        }
    }

    private fun sameFile(first: File, second: File): Boolean =
        runCatching { first.canonicalFile == second.canonicalFile }
            .getOrElse { first.absoluteFile == second.absoluteFile }

    fun startLogging() {
        commands.post {
            if (activeLog != null) return@post
            mark = 0
            val settings = try {
                prefs.settings.first()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: IOException) {
                reportFailure("Could not read sensor log settings", failure)
                return@post
            }
            try {
                val file = createLogFile(settings.isBinaryLogFormatEnabled)
                val openedLog = if (settings.isBinaryLogFormatEnabled) {
                    ActiveLog(file = file, binaryStream = FileOutputStream(file))
                } else {
                    val writer = FileWriter(file, true)
                    try {
                        if (settings.isCsvTitleEnabled) {
                            writer.append(encoder.csvTitle(settings.csvDelimiter))
                            writer.flush()
                        }
                    } catch (failure: Exception) {
                        closeAfterFailure(writer, failure)
                        throw failure
                    }
                    csvDelimiter = settings.csvDelimiter
                    ActiveLog(file = file, csvWriter = writer)
                }
                activeLog = openedLog
                mutableState.value = true
                usageAnalytics.logRecordingStarted(if (openedLog.isBinary) "s3l" else "csv")
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                activeLog = null
                mutableState.value = false
                reportFailure("Could not start sensor log", failure)
            }
        }
    }

    fun stopLogging() {
        commands.post {
            mark = 0
            closeActiveLog()
        }
    }

    fun setMark1() {
        commands.post { mark = 1 }
    }

    fun setMark2() {
        commands.post { mark = 2 }
    }

    fun setMark3() {
        commands.post { mark = 4 }
    }

    suspend fun logPacket(sensorsPacket: SensorsPacket) = commands.await {
        val current = activeLog ?: return@await
        try {
            if (current.isBinary) {
                current.binaryStream!!.write(encoder.encodeBinaryRecord(sensorsPacket, mark, LocalTime.now()))
            } else {
                current.csvWriter!!.append(encoder.encodeCsvRow(sensorsPacket, mark, csvDelimiter, LocalTime.now()))
                current.csvWriter.flush()
            }
            mark = 0
        } catch (failure: IOException) {
            mark = 0
            closeActiveLog(failure)
        }
    }

    private fun closeActiveLog(operationFailure: IOException? = null) {
        val current = activeLog ?: return
        activeLog = null
        mutableState.value = false
        var failure = operationFailure
        try {
            if (current.isBinary) current.binaryStream!!.close() else current.csvWriter!!.close()
        } catch (closeFailure: IOException) {
            if (failure == null) failure = closeFailure else failure.addSuppressed(closeFailure)
        }
        failure?.let {
            val message = if (operationFailure == null) "Could not close sensor log" else "Could not write sensor log"
            reportFailure(message, it)
        }
    }

    private fun closeAfterFailure(writer: FileWriter, failure: Throwable) {
        try {
            writer.close()
        } catch (closeFailure: IOException) {
            failure.addSuppressed(closeFailure)
        }
    }

    private fun reportFailure(message: String, failure: Throwable) {
        Log.e("SecuLogger", message, failure)
        uiMessageQueue.publish(R.string.sensor_logger_io_error, UiMessageDuration.LONG)
    }

    private fun createLogFile(binary: Boolean): File {
        val base = if (binary) fileHelper.generateS3lFile else fileHelper.generateCsvFile
        if (!base.exists()) return base
        var suffix = 1
        while (true) {
            val candidate = File(base.parentFile, "${base.nameWithoutExtension}_$suffix.${base.extension}")
            if (!candidate.exists()) return candidate
            suffix++
        }
    }

    /** Flush accepted commands and stop the worker in tests before removing its files. */
    internal suspend fun shutdownForTests() {
        commands.closeAndJoin()
        loggerScope.coroutineContext[Job]?.cancelAndJoin()
    }

    internal suspend fun awaitCommandsForTests() {
        commands.await { Unit }
    }
}

enum class LogDeletionResult {
    Deleted,
    Active,
    Failed,
}
