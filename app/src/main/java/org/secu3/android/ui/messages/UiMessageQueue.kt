package org.secu3.android.ui.messages

import android.os.SystemClock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton

/** A presentation-only queue consumed by the Activity while it is resumed. */
@Singleton
class UiMessageQueue @Inject constructor() {
    private val mutablePending = MutableStateFlow<List<UiMessage>>(emptyList())
    val pending: StateFlow<List<UiMessage>> = mutablePending.asStateFlow()
    private val mutableActiveScope = MutableStateFlow<String?>(null)
    val activeScope: StateFlow<String?> = mutableActiveScope.asStateFlow()
    val availableMessages: Flow<List<UiMessage>> = combine(pending, activeScope) { messages, scope ->
        messages.filter { it.scope == null || it.scope == scope }
    }
    private var nextId = 1L
    private var clock: () -> Long = SystemClock::elapsedRealtime

    internal constructor(clock: () -> Long) : this() {
        this.clock = clock
    }

    @Synchronized
    fun publish(
        textRes: Int,
        duration: UiMessageDuration,
        scope: String? = null,
    ) {
        val nowMillis = clock()
        if (scope != null && mutableActiveScope.value != null && scope != mutableActiveScope.value) return
        val live = mutablePending.value.filter { it.expiresAtMillis > nowMillis }
        if (live.any { it.textRes == textRes && it.scope == scope && it.duration == duration }) {
            mutablePending.value = live
            return
        }
        val message = UiMessage(nextId++, textRes, duration, scope, nowMillis + TTL_MILLIS)
        mutablePending.value = (live + message).takeLast(CAPACITY)
    }

    @Synchronized
    fun acknowledge(id: Long) {
        mutablePending.value = mutablePending.value.filterNot { it.id == id }
    }

    @Synchronized
    fun discardExpired() {
        val nowMillis = clock()
        mutablePending.value = mutablePending.value.filter { it.expiresAtMillis > nowMillis }
    }

    @Synchronized
    fun onActiveScopeChanged(activeScope: String?) {
        if (activeScope == mutableActiveScope.value) return
        mutableActiveScope.value = activeScope
        if (activeScope != null) {
            mutablePending.value = mutablePending.value.filter { it.scope == null || it.scope == activeScope }
        }
    }

    @Synchronized
    fun onActivityAttached() {
        mutableActiveScope.value = null
    }

    private companion object {
        const val CAPACITY = 16
        const val TTL_MILLIS = 15_000L
    }
}

data class UiMessage(
    val id: Long,
    val textRes: Int,
    val duration: UiMessageDuration,
    /** Navigation destination route, or null for a message that survives navigation. */
    val scope: String?,
    val expiresAtMillis: Long,
)

enum class UiMessageDuration { SHORT, LONG }
