package org.secu3.android.ui.parameters.compose

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Shared press-and-hold behavior for parameter controls that accumulate manual ECU movement. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ManualPositionButton(
    testTag: String,
    direction: Int,
    label: String,
    icon: String,
    leading: Boolean,
    enabled: Boolean,
    onStep: (Int) -> Unit,
    onFlush: () -> Unit,
    onRelease: () -> Unit,
    onCancel: () -> Unit,
) {
    var pressed by remember(testTag) { mutableStateOf(false) }
    val currentOnStep by rememberUpdatedState(onStep)
    val currentOnFlush by rememberUpdatedState(onFlush)
    val currentOnRelease by rememberUpdatedState(onRelease)
    val currentOnCancel by rememberUpdatedState(onCancel)
    val scope = rememberCoroutineScope()
    val shape = RoundedCornerShape(
        topStart = if (leading) 24.dp else 0.dp,
        bottomStart = if (leading) 24.dp else 0.dp,
        topEnd = if (leading) 0.dp else 24.dp,
        bottomEnd = if (leading) 0.dp else 24.dp,
    )
    val color = if (pressed) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.primary
    val contentColor = if (pressed) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onPrimary

    Surface(
        color = color,
        contentColor = contentColor,
        shape = shape,
        modifier = Modifier
            .testTag(testTag)
            .size(width = 64.dp, height = 48.dp)
            .semantics {
                role = Role.Button
                onClick(label) {
                    if (enabled) {
                        currentOnStep(direction)
                        currentOnRelease()
                    }
                    true
                }
            }
            .pointerInput(direction, enabled) {
                if (!enabled) return@pointerInput
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    pressed = true
                    var repeater: Job? = null
                    var flusher: Job? = null
                    var completed = false
                    try {
                        currentOnStep(direction)
                        repeater = scope.launch {
                            delay(400L)
                            while (true) {
                                currentOnStep(direction)
                                delay(80L)
                            }
                        }
                        flusher = scope.launch {
                            while (true) {
                                delay(250L)
                                currentOnFlush()
                            }
                        }
                        if (waitForUpOrCancellation() == null) {
                            completed = true
                            currentOnCancel()
                        } else {
                            completed = true
                            currentOnRelease()
                        }
                    } catch (cancellation: CancellationException) {
                        if (!completed) {
                            completed = true
                            currentOnCancel()
                        }
                        throw cancellation
                    } finally {
                        repeater?.cancel()
                        flusher?.cancel()
                        pressed = false
                    }
                }
            },
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(icon, style = MaterialTheme.typography.headlineSmall)
        }
    }
}
