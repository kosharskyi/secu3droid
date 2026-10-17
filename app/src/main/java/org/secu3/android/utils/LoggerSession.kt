package org.secu3.android.utils

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.IOException

internal interface LoggerOutput {
    @Throws(IOException::class)
    fun writeBinary(bytes: ByteArray)

    @Throws(IOException::class)
    fun appendCsv(text: String)

    @Throws(IOException::class)
    fun flushCsv()

    @Throws(IOException::class)
    fun close()
}

internal data class LoggerSessionState(
    val started: Boolean = false,
    val nextErrorId: Long = 0,
    val pendingErrorId: Long? = null,
)

internal class LoggerSession {
    private val mutableState = MutableStateFlow(LoggerSessionState())
    val state: StateFlow<LoggerSessionState> = mutableState.asStateFlow()

    private var output: LoggerOutput? = null
    private var binary = false

    @Synchronized
    fun start(openOutput: () -> LoggerOutput, binary: Boolean, csvHeader: String? = null) {
        if (output != null) return

        try {
            output = openOutput()
            this.binary = binary
            if (!binary && csvHeader != null) {
                output?.appendCsv(csvHeader)
                output?.flushCsv()
            }
            mutableState.value = mutableState.value.copy(started = true)
        } catch (_: IOException) {
            failCurrentOperation()
        }
    }

    @Synchronized
    fun writeBinary(bytes: ByteArray) {
        if (!mutableState.value.started || !binary) return
        try {
            output?.writeBinary(bytes)
        } catch (_: IOException) {
            failCurrentOperation()
        }
    }

    @Synchronized
    fun writeCsv(text: String) {
        if (!mutableState.value.started || binary) return
        try {
            output?.appendCsv(text)
            output?.flushCsv()
        } catch (_: IOException) {
            failCurrentOperation()
        }
    }

    @Synchronized
    fun stop() {
        if (output == null) return
        val ownedOutput = output
        output = null
        mutableState.value = mutableState.value.copy(started = false)
        try {
            ownedOutput?.close()
        } catch (_: IOException) {
            publishError()
        }
    }

    @Synchronized
    fun acknowledgeError(errorId: Long) {
        if (mutableState.value.pendingErrorId == errorId) {
            mutableState.value = mutableState.value.copy(pendingErrorId = null)
        }
    }

    private fun failCurrentOperation() {
        val ownedOutput = output
        output = null
        try {
            ownedOutput?.close()
        } catch (_: IOException) {
            // Preserve one error event for the failed operation.
        }
        mutableState.value = mutableState.value.copy(started = false)
        publishError()
    }

    private fun publishError() {
        val nextErrorId = mutableState.value.nextErrorId + 1
        mutableState.value = mutableState.value.copy(
            started = false,
            nextErrorId = nextErrorId,
            pendingErrorId = nextErrorId,
        )
    }
}
