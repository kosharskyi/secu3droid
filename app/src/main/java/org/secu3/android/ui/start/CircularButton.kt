package org.secu3.android.ui.start

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ripple
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.TextUnit
import kotlin.math.roundToInt

@Composable
fun CircularButton(
    modifier: Modifier = Modifier,
    textNormal: String,
    textInProgress: String,
    isInProgress: Boolean,
    onClick: () -> Unit,
    size: Dp = 150.dp,
    borderColor: Color? = null,
    progressSegmentColor: Color? = null,
    textColor: Color? = null,
    borderWidth: Dp = 2.dp,
    progressSegmentWidth: Dp = 6.dp
) {
    val resolvedBorderColor = borderColor ?: MaterialTheme.colorScheme.outlineVariant
    val resolvedProgressColor = progressSegmentColor ?: MaterialTheme.colorScheme.primary
    val resolvedTextColor = textColor ?: MaterialTheme.colorScheme.onSurface
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val baseFontSize = 20.sp
    val baseTextStyle = MaterialTheme.typography.titleLarge.copy(fontSize = baseFontSize, lineHeight = 24.sp)
    val baseLabelWidthPx = remember(textMeasurer, textNormal, textInProgress, baseTextStyle) {
        maxOf(
            textMeasurer.measure(textNormal, style = baseTextStyle, softWrap = false, maxLines = 1).size.width,
            textMeasurer.measure(textInProgress, style = baseTextStyle, softWrap = false, maxLines = 1).size.width,
        )
    }
    val preferredHorizontalPadding = 24.dp
    val minimumHorizontalPadding = 8.dp
    val ringClearance = maxOf(borderWidth, progressSegmentWidth) + 4.dp
    val desiredDiameter = with(density) {
        maxOf(
            size,
            baseLabelWidthPx.toDp() + preferredHorizontalPadding * 2 + ringClearance * 2,
        )
    }

    BoxWithConstraints(modifier = modifier, contentAlignment = Alignment.Center) {
        val diameter = desiredDiameter.coerceAtMost(maxWidth).coerceAtMost(maxHeight)
        val diameterPx = with(density) { diameter.toPx() }
        val ringClearancePx = with(density) { ringClearance.toPx() }
        val preferredPaddingPx = with(density) { preferredHorizontalPadding.toPx() }
        val minimumPaddingPx = with(density) { minimumHorizontalPadding.toPx() }
        val textAvailableAtMinimumPadding = (diameterPx - ringClearancePx * 2 - minimumPaddingPx * 2).roundToInt().coerceAtLeast(0)

        val fittedFontSize = remember(
            textMeasurer,
            textNormal,
            textInProgress,
            baseTextStyle,
            textAvailableAtMinimumPadding,
        ) {
            fun maxLabelWidth(fontSize: TextUnit): Int {
                val style = baseTextStyle.copy(fontSize = fontSize, lineHeight = fontSize * 1.2f)
                return maxOf(
                    textMeasurer.measure(textNormal, style = style, softWrap = false, maxLines = 1).size.width,
                    textMeasurer.measure(textInProgress, style = style, softWrap = false, maxLines = 1).size.width,
                )
            }

            if (maxLabelWidth(baseFontSize) <= textAvailableAtMinimumPadding) {
                baseFontSize
            } else {
                var lower = 12f
                var upper = baseFontSize.value
                repeat(12) {
                    val middle = (lower + upper) / 2f
                    if (maxLabelWidth(middle.sp) <= textAvailableAtMinimumPadding) lower = middle else upper = middle
                }
                lower.sp
            }
        }
        val fittedStyle = baseTextStyle.copy(fontSize = fittedFontSize, lineHeight = fittedFontSize * 1.2f)
        val fittedLabelWidthPx = remember(textMeasurer, textNormal, textInProgress, fittedStyle) {
            maxOf(
                textMeasurer.measure(textNormal, style = fittedStyle, softWrap = false, maxLines = 1).size.width,
                textMeasurer.measure(textInProgress, style = fittedStyle, softWrap = false, maxLines = 1).size.width,
            )
        }
        val horizontalPadding = with(density) {
            (((diameterPx - ringClearancePx * 2 - fittedLabelWidthPx).toDp()) / 2)
                .coerceIn(minimumHorizontalPadding, preferredHorizontalPadding)
        }

        Box(
            modifier = Modifier
                .size(diameter)
                .clip(CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = true),
                    onClick = onClick,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.matchParentSize()) {
                val inset = maxOf(borderWidth, progressSegmentWidth).toPx() / 2
                val actualDiameter = minOf(this.size.width, this.size.height)
                drawCircle(
                    color = resolvedBorderColor,
                    style = Stroke(width = borderWidth.toPx()),
                    radius = actualDiameter / 2 - inset,
                )
            }

            if (isInProgress) {
                val infiniteTransition = rememberInfiniteTransition()
                val rotationAngle by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 360f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1500, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Restart,
                    ),
                )

                Canvas(modifier = Modifier.matchParentSize()) {
                    val inset = maxOf(borderWidth, progressSegmentWidth).toPx() / 2
                    val arcSize = Size(this.size.width - inset * 2, this.size.height - inset * 2)
                    rotate(rotationAngle) {
                        drawArc(
                            color = resolvedProgressColor,
                            startAngle = 270f,
                            sweepAngle = 60f,
                            useCenter = false,
                            style = Stroke(width = progressSegmentWidth.toPx()),
                            topLeft = Offset(inset, inset),
                            size = arcSize,
                        )
                    }
                }
            }

            Text(
                text = if (isInProgress) textInProgress else textNormal,
                color = resolvedTextColor,
                style = fittedStyle,
                textAlign = TextAlign.Center,
                softWrap = false,
                maxLines = 1,
                overflow = TextOverflow.Clip,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = horizontalPadding, vertical = 12.dp)
                    .testTag("start-connect-label"),
            )
        }
    }
}

@Preview
@Composable
private fun CircularButtonExample() {
    var isButtonInProgress by remember { mutableStateOf(false) }

    org.secu3.android.ui.compose.SecuComposeTheme {
        CircularButton(
            textNormal = "Connect",
            textInProgress = "Connecting...",
            isInProgress = isButtonInProgress,
            onClick = { isButtonInProgress = isButtonInProgress.not() },
            size = 120.dp,
        )
    }
}
