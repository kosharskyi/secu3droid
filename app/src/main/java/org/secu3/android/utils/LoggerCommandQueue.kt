package org.secu3.android.utils

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch

/** Serializes logger controls and writes without tying the worker to a screen or service. */
internal class LoggerCommandQueue(
    scope: CoroutineScope,
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val onFailure: (Exception) -> Unit,
) {
    private data class Command(
        val run: () -> Unit,
        val fail: (Throwable) -> Unit = {},
    )

    private val commands = Channel<Command>(Channel.UNLIMITED)
    private val worker: Job = scope.launch(dispatcher) {
        for (command in commands) {
            try {
                currentCoroutineContext().ensureActive()
                command.run()
            } catch (cancelled: CancellationException) {
                command.fail(cancelled)
                throw cancelled
            } catch (failure: Exception) {
                command.fail(failure)
                runCatching { onFailure(failure) }
            }
        }
    }.also { worker ->
        worker.invokeOnCompletion { cause ->
            val failure = cause as? CancellationException
                ?: CancellationException("Logger command queue closed")
            commands.close(failure)
            while (true) {
                val command = commands.tryReceive().getOrNull() ?: break
                command.fail(failure)
            }
        }
    }

    fun post(command: () -> Unit) {
        val result = commands.trySend(Command(run = command))
        if (result.isFailure) {
            runCatching { onFailure(IllegalStateException("Logger command queue is unavailable", result.exceptionOrNull())) }
        }
    }

    suspend fun <T> await(command: () -> T): T {
        val result = CompletableDeferred<T>()
        commands.send(
            Command(
                run = { result.complete(command()) },
                fail = { result.completeExceptionally(it) },
            ),
        )
        return result.await()
    }

    /** Test and orderly-shutdown hook: drain accepted work before joining the worker. */
    suspend fun closeAndJoin() {
        commands.close()
        worker.join()
    }
}
