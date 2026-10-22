package org.secu3.android.ui.compose

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable

/** Shares the dial spring while leaving lifecycle and unavailable-value policy to each screen. */
@Composable
internal fun rememberAnimatedGaugeProgress(target: Float): () -> Float {
    val animatedProgress = animateFloatAsState(
        targetValue = target,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium,
            visibilityThreshold = 0.001f,
        ),
        label = "gaugeProgress",
    )
    return { animatedProgress.value.coerceIn(0f, 1f) }
}
