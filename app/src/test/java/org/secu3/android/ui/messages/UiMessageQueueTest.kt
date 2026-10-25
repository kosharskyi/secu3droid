package org.secu3.android.ui.messages

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout

class UiMessageQueueTest {
    private var now = 1_000L
    private val queue = UiMessageQueue { now }

    @Test
    fun duplicateMessagesCoalesceAndAcknowledgmentRemovesOnlyMatchingId() {
        queue.publish(1, UiMessageDuration.SHORT, scope = "parameters")
        val first = queue.pending.value.single()
        queue.publish(1, UiMessageDuration.SHORT, scope = "parameters")
        queue.publish(2, UiMessageDuration.SHORT, scope = "parameters")

        assertEquals(listOf(first.id, first.id + 1), queue.pending.value.map { it.id })
        queue.acknowledge(first.id)
        assertEquals(listOf(2), queue.pending.value.map { it.textRes })
    }

    @Test
    fun queueDropsOldestOnOverflowAndExpiresByMonotonicTime() {
        repeat(17) { queue.publish(it, UiMessageDuration.SHORT) }
        assertEquals(16, queue.pending.value.size)
        assertEquals((1..16).toList(), queue.pending.value.map { it.textRes })

        now += 15_001L
        queue.discardExpired()
        assertTrue(queue.pending.value.isEmpty())
    }

    @Test
    fun changingScreenDiscardsOldScreenMessagesAndRejectsLateProducers() {
        queue.publish(1, UiMessageDuration.SHORT, scope = "home")
        queue.onActiveScopeChanged("parameters")
        queue.publish(2, UiMessageDuration.SHORT, scope = "home")
        queue.publish(3, UiMessageDuration.SHORT, scope = "parameters")

        assertEquals(listOf(3), queue.pending.value.map { it.textRes })
        queue.publish(4, UiMessageDuration.SHORT)
        assertEquals(listOf(3, 4), queue.pending.value.map { it.textRes })
    }

    @Test
    fun restoredRouteWakesConsumerWaitingForScopedMessage() = runBlocking {
        queue.publish(1, UiMessageDuration.SHORT, scope = "parameters")
        val awaitingMessage = async(start = CoroutineStart.UNDISPATCHED) {
            withTimeout(1_000) { queue.availableMessages.first { it.isNotEmpty() }.single() }
        }

        queue.onActiveScopeChanged("parameters")

        assertEquals(1, awaitingMessage.await().textRes)
    }

    @Test
    fun activityReattachmentHidesScopedMessagesUntilSameRouteIsRestored() = runBlocking {
        queue.onActiveScopeChanged("parameters")
        queue.publish(1, UiMessageDuration.SHORT, scope = "parameters")
        queue.onActivityAttached()
        val awaitingMessage = async(start = CoroutineStart.UNDISPATCHED) {
            withTimeout(1_000) { queue.availableMessages.first { it.isNotEmpty() }.single() }
        }

        assertTrue(!awaitingMessage.isCompleted)
        queue.onActiveScopeChanged("parameters")

        assertEquals(1, awaitingMessage.await().textRes)
    }

    @Test
    fun activityReattachmentThenDifferentRouteDiscardsPreviousScope() {
        queue.onActiveScopeChanged("home")
        queue.publish(1, UiMessageDuration.SHORT, scope = "home")
        queue.onActivityAttached()

        assertEquals(listOf(1), queue.pending.value.map { it.textRes })
        queue.onActiveScopeChanged("parameters")
        assertTrue(queue.pending.value.isEmpty())
    }

    @Test
    fun formattedArgumentsAreSnapshottedAndPartOfDuplicateIdentity() {
        val args = mutableListOf<UiMessageArgument>(UiMessageArgument.Text("before"))
        queue.publish(1, UiMessageDuration.SHORT, arguments = args)
        args[0] = UiMessageArgument.Text("after")
        queue.publish(1, UiMessageDuration.SHORT, arguments = listOf(UiMessageArgument.Text("after")))

        assertEquals(2, queue.pending.value.size)
        assertEquals(UiMessageArgument.Text("before"), queue.pending.value.first().arguments.single())
    }
}
