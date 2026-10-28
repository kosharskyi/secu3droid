package org.secu3.android.ui.sensors

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector2D
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.pointerInput

/** UI-only state for one grid; persistence remains with the screen's callbacks. */
internal class SensorGridReorderState<T>(
    initialItems: List<T>,
    private val keyOf: (T) -> String,
    private val onReorderingChanged: (Boolean) -> Unit,
) {
    var items by mutableStateOf(initialItems)
    var isDragging by mutableStateOf(false)
        private set
    var activeKey by mutableStateOf<String?>(null)
        private set
    var activeOffset by mutableStateOf(Offset.Zero)
        private set
    var pendingDrop by mutableStateOf<Pair<String, String>?>(null)
        private set
    var pendingOrder by mutableStateOf<List<String>?>(null)
        private set
    val positions = mutableStateMapOf<String, Rect>()
    val animations = mutableMapOf<String, Animatable<Offset, AnimationVector2D>>()
    private var target by mutableStateOf<String?>(null)
    private var baseOrder by mutableStateOf<List<String>>(emptyList())
    private var previewOrder by mutableStateOf<List<String>>(emptyList())
    private var basePositions by mutableStateOf<Map<String, Rect>>(emptyMap())

    fun reconcile(incoming: List<T>) {
        if (isDragging) return
        val pending = pendingOrder
        if (pending == null) {
            items = incoming
            return
        }
        val current = incoming.map(keyOf)
        val reconciled = pending.filter { it in current } + current.filter { it !in pending }
        pendingOrder = if (current == reconciled) null else reconciled
        items = if (pendingOrder == null) incoming else reconciled.mapNotNull { key -> incoming.find { keyOf(it) == key } }
    }

    fun previewOffset(key: String): Offset = slotOffset(key, baseOrder, previewOrder, basePositions)

    fun animation(key: String): Animatable<Offset, AnimationVector2D> =
        animations.getOrPut(key) { Animatable(Offset.Zero, Offset.VectorConverter) }

    suspend fun animatePreview(key: String, offset: Offset) {
        animation(key).animateTo(offset, tween(PREVIEW_ANIMATION_MILLIS))
    }

    fun itemModifier(key: String): Modifier = Modifier.pointerInput(positions, key) {
        var dragPosition = Offset.Zero
        var dragBounds: Map<String, Rect> = emptyMap()
        detectDragGesturesAfterLongPress(
            onDragStart = {
                dragBounds = positions.toMap()
                val bounds = dragBounds[key]
                if (bounds != null) {
                    dragPosition = bounds.center
                    updateDragging(true)
                    activeKey = key
                    activeOffset = Offset.Zero
                    target = key
                    baseOrder = items.map(keyOf)
                    previewOrder = baseOrder
                    basePositions = positions.toMap()
                }
            },
            onDragEnd = { finish(key, false) },
            onDragCancel = { finish(key, true) },
            onDrag = { change, delta ->
                change.consume()
                dragPosition += delta
                activeOffset += delta
                val nearest = dragBounds.entries.minByOrNull { (_, bounds) ->
                    val dx = bounds.center.x - dragPosition.x
                    val dy = bounds.center.y - dragPosition.y
                    dx * dx + dy * dy
                }?.key
                if (nearest != null) {
                    target = nearest
                    previewOrder = moveItem(baseOrder, key, nearest) { it }
                }
            },
        )
    }

    suspend fun completeDrop(latestItems: () -> List<T>, onCommit: (List<T>) -> Unit) {
        val drop = pendingDrop ?: return
        var elapsed = 0L
        var previousFrame = withFrameNanos { it }
        while (elapsed < PREVIEW_ANIMATION_NANOS && animations.values.any { it.isRunning }) {
            val frame = withFrameNanos { it }
            elapsed += frame - previousFrame
            previousFrame = frame
        }
        animations.values.forEach { it.snapTo(Offset.Zero) }
        val moved = moveItem(items, drop.first, drop.second, keyOf)
        val ordered = moved.map(keyOf)
        val latest = latestItems()
        items = (ordered + latest.map(keyOf).filter { it !in ordered })
            .mapNotNull { key -> latest.find { keyOf(it) == key } }
        pendingOrder = items.map(keyOf)
        onCommit(items)
        pendingDrop = null
        reset()
    }

    private fun finish(key: String, cancelled: Boolean) {
        if (cancelled) {
            if (pendingDrop == null) reset()
            return
        }
        activeOffset = previewOffset(key)
        pendingDrop = key to (target ?: key)
    }

    private fun reset() {
        updateDragging(false)
        activeKey = null
        activeOffset = Offset.Zero
        target = null
        baseOrder = emptyList()
        previewOrder = emptyList()
        basePositions = emptyMap()
    }

    private fun updateDragging(value: Boolean) {
        if (isDragging != value) {
            isDragging = value
            onReorderingChanged(value)
        }
    }

    private fun <V> moveItem(values: List<V>, source: String, destination: String, key: (V) -> String): List<V> {
        val from = values.indexOfFirst { key(it) == source }
        val to = values.indexOfFirst { key(it) == destination }
        if (from < 0 || to < 0 || from == to) return values
        return values.toMutableList().also { it.add(to, it.removeAt(from)) }
    }

    private fun slotOffset(key: String, base: List<String>, preview: List<String>, bounds: Map<String, Rect>): Offset {
        val index = preview.indexOf(key)
        if (index < 0) return Offset.Zero
        val destinationKey = base.getOrNull(index) ?: return Offset.Zero
        val current = bounds[key] ?: return Offset.Zero
        val destination = bounds[destinationKey] ?: return Offset.Zero
        return destination.center - current.center
    }

    private companion object {
        const val PREVIEW_ANIMATION_MILLIS = 220
        const val PREVIEW_ANIMATION_NANOS = PREVIEW_ANIMATION_MILLIS * 1_000_000L
    }
}
