package org.secu3.android.ui.errors

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.onSubscription
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test
import org.secu3.android.models.packets.base.InputPacket
import org.secu3.android.models.packets.out.CheckEngineSavedErrorsPacket

class SavedErrorsRequestTest {
    @Test
    fun responseIsSubscribedAndCachedBeforeRequestReturns() = runBlocking {
        val packets = MutableSharedFlow<InputPacket>()
        val expected = CheckEngineSavedErrorsPacket(errors = 0x1234)
        val events = mutableListOf<String>()
        var cached: CheckEngineSavedErrorsPacket? = null

        val result = withTimeout(1_000) { readSavedErrors(
            packets = { onSubscribed -> packets.onSubscription { onSubscribed() } },
            sendRequest = {
                events += "request"
                assertEquals(1, packets.subscriptionCount.value)
                launch(start = CoroutineStart.UNDISPATCHED) {
                    packets.emit(org.secu3.android.models.packets.input.CheckEngineErrorsPacket())
                    packets.emit(expected)
                }
            },
            onReceived = {
                cached = it
                events += "cached"
            },
        ) }

        assertSame(expected, result)
        assertSame(expected, cached)
        assertEquals(listOf("request", "cached"), events)
    }
}
