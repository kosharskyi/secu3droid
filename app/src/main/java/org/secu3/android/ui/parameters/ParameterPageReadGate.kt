/*
 *    SecuDroid  - An open source, free manager for SECU-3 engine control unit
 *    Copyright (C) 2026 Vitalii O. Kosharskyi. Ukraine, Kyiv
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 */
package org.secu3.android.ui.parameters

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** A preloaded page may observe its data, but may read the ECU only after it is displayed. */
internal class ParameterPageReadGate {
    private val activePage = MutableStateFlow<Int?>(null)
    private val readMutex = Mutex()

    fun activate(titleRes: Int) {
        activePage.value = titleRes
    }

    fun deactivate() {
        activePage.value = null
    }

    /** Cancels an unfinished read when the page hides, and retries when it becomes active again. */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun <T> read(titleRes: Int, receive: suspend () -> T): Flow<T> = activePage.transformLatest { page ->
        if (page == titleRes) {
            // ECU requests change its streaming mode. Finish cancellation cleanup before
            // allowing another page (or the next Functions request) to change that mode.
            readMutex.withLock { emit(receive()) }
        }
    }.buffer(0).take(1)
}

/** Subscribe before changing ECU mode so even the first response cannot be missed. */
internal suspend fun <T> readParameterPacket(
    sendRequest: () -> Unit,
    restoreSensors: () -> Unit,
    receive: suspend (onSubscribed: () -> Unit) -> T,
): T {
    try {
        return receive(sendRequest)
    } finally {
        restoreSensors()
    }
}
