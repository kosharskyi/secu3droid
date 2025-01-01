package org.secu3.android.ui.start

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Text
import androidx.compose.material.ripple
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CircularButton(
    modifier: Modifier = Modifier,
    textNormal: String,
    textInProgress: String,
    isInProgress: Boolean,
    onClick: () -> Unit,
    size: Dp = 150.dp,
    borderColor: Color = Color.Blue,
    progressSegmentColor: Color = Color.Red,
    textColor: Color = Color.Black,
    borderWidth: Dp = 3.dp,
    progressSegmentWidth: Dp = 3.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true),
                onClick = onClick,
            )
        ,

        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val inset = borderWidth.toPx() / 2
            // Static circle (common for both states)
            drawCircle(
                color = borderColor,
                style = Stroke(width = borderWidth.toPx()),
                radius = size.toPx() / 2 - inset
            )
        }

        if (isInProgress) {
            // In Progress State: Rotating segment and text
            val infiniteTransition = rememberInfiniteTransition()
            val rotationAngle by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1500, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Restart
                )
            )

            // Rotating segment
            Canvas(modifier = Modifier.matchParentSize()) {
                rotate(rotationAngle) {
                    drawArc(
                        color = progressSegmentColor,
                        startAngle = 270f,
                        sweepAngle = 60f, // The size of the segment
                        useCenter = false,
                        style = Stroke(width = progressSegmentWidth.toPx())
                    )
                }
            }
        }

        val text = if (isInProgress) textInProgress else textNormal

        Text(
            text = text,
            color = textColor,
            fontSize = 24.sp
        )
    }
}

@Preview
@Composable
private fun CircularButtonExample() {
    var isButtonInProgress by remember { mutableStateOf(false) }

    CircularButton(
        textNormal = "Connect",
        textInProgress = "Connecting...",
        isInProgress = isButtonInProgress,
        onClick = {
            isButtonInProgress = isButtonInProgress.not()
        },
        size = 120.dp,
        borderColor = Color.Gray,
        progressSegmentColor = Color.Cyan,
        textColor = Color.Black
    )
}
