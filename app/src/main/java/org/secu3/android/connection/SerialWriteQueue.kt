package org.secu3.android.connection

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

/** A transport-local FIFO whose writes stay bound to the endpoint captured by the sender. */
internal class SerialWriteQueue<E>(
    scope: CoroutineScope,
    private val isCurrentEndpoint: (E) -> Boolean,
    private val write: (E, ByteArray) -> Unit,
    private val afterWrite: suspend () -> Unit = {},
    private val onFailure: (Exception) -> Unit,
) {
    private data class Entry<E>(val endpoint: E, val bytes: ByteArray)

    private val entries = Channel<Entry<E>>(Channel.UNLIMITED)

    init {
        scope.launch {
            for (entry in entries) {
                try {
                    if (!isCurrentEndpoint(entry.endpoint)) continue
                    write(entry.endpoint, entry.bytes)
                    afterWrite()
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (failure: Exception) {
                    onFailure(failure)
                }
            }
        }
    }

    fun enqueue(endpoint: E?, bytes: ByteArray) {
        if (endpoint == null || !isCurrentEndpoint(endpoint)) return
        entries.trySend(Entry(endpoint, bytes.copyOf()))
    }
}
