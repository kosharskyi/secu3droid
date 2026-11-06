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

internal class LoggerSession(private val onError: () -> Unit) {
    private val mutableStarted = MutableStateFlow(false)
    val started: StateFlow<Boolean> = mutableStarted.asStateFlow()

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
            mutableStarted.value = true
        } catch (_: IOException) {
            failCurrentOperation()
        }
    }

    @Synchronized
    fun writeBinary(bytes: ByteArray) {
        if (!mutableStarted.value || !binary) return
        try {
            output?.writeBinary(bytes)
        } catch (_: IOException) {
            failCurrentOperation()
        }
    }

    @Synchronized
    fun writeCsv(text: String) {
        if (!mutableStarted.value || binary) return
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
        mutableStarted.value = false
        try {
            ownedOutput?.close()
        } catch (_: IOException) {
            onError()
        }
    }

    @Synchronized
    fun fail() = failCurrentOperation()

    private fun failCurrentOperation() {
        val ownedOutput = output
        output = null
        try {
            ownedOutput?.close()
        } catch (_: IOException) {
            // Preserve one error event for the failed operation.
        }
        mutableStarted.value = false
        onError()
    }
}
