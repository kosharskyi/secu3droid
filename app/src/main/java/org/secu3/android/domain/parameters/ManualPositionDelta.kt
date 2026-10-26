package org.secu3.android.domain.parameters

import javax.inject.Inject

/** Accumulates one controller's unsent manual movement until it is flushed or discarded. */
class ManualPositionDelta @Inject constructor() {
    private var pending = 0

    fun add(direction: Int) {
        pending = (pending + direction.coerceIn(-1, 1)).coerceIn(MIN, MAX)
    }

    fun take(): Int = pending.also { pending = 0 }

    fun clear() {
        pending = 0
    }

    companion object {
        const val MIN = -127
        const val MAX = 127
    }
}
