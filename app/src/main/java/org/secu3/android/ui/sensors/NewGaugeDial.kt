package org.secu3.android.ui.sensors

import android.graphics.Paint as NativePaint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.ui.Alignment
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.sp
import org.secu3.android.models.SensorGaugeStyle
import org.secu3.android.ui.sensors.models.GaugeScale
import org.secu3.android.ui.sensors.models.GaugeTick
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

internal data class NewGaugeDialLabel(val tick: GaugeTick, val text: String, val centerX: Float, val centerY: Float, val width: Float, val height: Float) {
    val index: Int get() = tick.index
    val value: Float get() = tick.value
}
internal data class NewGaugeDialMeasure(val height: Dp, val centerY: Dp, val labels: List<NewGaugeDialLabel>)

internal fun measureNewGaugeDial(
    scale: GaugeScale,
    width: Dp,
    density: Density,
    labelOverflow: Dp = 14.dp,
): NewGaugeDialMeasure {
    val labelPaint = NativePaint(NativePaint.ANTI_ALIAS_FLAG).apply {
        textSize = with(density) { 12.sp.toPx() }
    }
    val metrics = labelPaint.fontMetrics
    val widthPx = with(density) { width.toPx() }
    val overflowPx = with(density) { labelOverflow.toPx() }
    val radius = widthPx / 2f - with(density) { 5.dp.toPx() }
    val halfStroke = with(density) { 2.5.dp.toPx() }
    val labelGap = with(density) { 3.dp.toPx() }
    val edgePadding = with(density) { 2.dp.toPx() }
    val textHeight = metrics.descent - metrics.ascent
    val labels = placeNewGaugeDialLabels(
        scale = scale,
        labelPaint = labelPaint,
        metrics = metrics,
        radius = radius,
        canvasWidth = widthPx + overflowPx * 2f,
        halfStroke = halfStroke,
        labelGap = labelGap,
        edgePadding = edgePadding,
        laneStep = with(density) { 4.dp.toPx() },
    )
    val top = labels.minOfOrNull { it.centerY - it.height / 2f } ?: -radius - halfStroke - labelGap - textHeight
    val bottom = labels.maxOfOrNull { it.centerY + it.height / 2f } ?: radius / sqrt(2f) + halfStroke + labelGap + textHeight
    val centerY = edgePadding - top
    val height = centerY + bottom + edgePadding
    return with(density) { NewGaugeDialMeasure(height.toDp(), centerY.toDp(), labels) }
}
private fun placeNewGaugeDialLabels(
    scale: GaugeScale,
    labelPaint: NativePaint,
    metrics: NativePaint.FontMetrics,
    radius: Float,
    canvasWidth: Float,
    halfStroke: Float,
    labelGap: Float,
    edgePadding: Float,
    laneStep: Float,
): List<NewGaugeDialLabel> {
    val ticks = scale.ticks
    if (ticks.isEmpty()) return emptyList()
    val textHeight = metrics.descent - metrics.ascent
    val endpointRadius = radius / sqrt(2f)
    val middleIndex = (ticks.lastIndex / 2f).toInt()
    val required = listOf(0, middleIndex, ticks.lastIndex).distinct()

    fun place(indices: List<Int>): List<NewGaugeDialLabel>? {
        val placed = mutableListOf<NewGaugeDialLabel>()
        fun fits(label: NewGaugeDialLabel): Boolean {
            val halfWidth = label.width / 2f
            if (abs(label.centerX) + halfWidth > canvasWidth / 2f - edgePadding) return false
            return placed.none { other ->
                val separatedHorizontally = abs(label.centerX - other.centerX) >= (label.width + other.width) / 2f + labelGap
                val separatedVertically = abs(label.centerY - other.centerY) >= (label.height + other.height) / 2f + labelGap
                !separatedHorizontally && !separatedVertically
            }
        }

        // Keep min/max labels in the existing lower endpoint positions, which fit the phone card width.
        for (index in listOf(0, ticks.lastIndex).distinct().filter { it in indices }) {
            val tick = ticks[index]
            val angle = Math.toRadians(tick.angleDegrees.toDouble())
            val text = scale.format(tick)
            val label = NewGaugeDialLabel(
                tick = tick,
                text = text,
                centerX = cos(angle).toFloat() * endpointRadius,
                centerY = endpointRadius + halfStroke + labelGap + textHeight / 2f,
                width = labelPaint.measureText(text),
                height = textHeight,
            )
            if (!fits(label)) return null
            placed += label
        }

        // Put the midpoint first, then fit other values in lanes outside the band as needed.
        val interiorIndices = indices.filter { it !in listOf(0, ticks.lastIndex) }
            .sortedWith(compareBy<Int> { abs(it - middleIndex) }.thenBy { it })
        for (index in interiorIndices) {
            val tick = ticks[index]
            val angle = Math.toRadians(tick.angleDegrees.toDouble())
            val text = scale.format(tick)
            val textWidth = labelPaint.measureText(text)
            val radialHalfExtent = abs(cos(angle).toFloat()) * textWidth / 2f + abs(sin(angle).toFloat()) * textHeight / 2f
            val baseRadius = radius + halfStroke + labelGap + radialHalfExtent
            var candidate: NewGaugeDialLabel? = null
            for (lane in 0..2) {
                val labelRadius = baseRadius + lane * laneStep
                val label = NewGaugeDialLabel(
                    tick = tick,
                    text = text,
                    centerX = cos(angle).toFloat() * labelRadius,
                    centerY = sin(angle).toFloat() * labelRadius,
                    width = textWidth,
                    height = textHeight,
                )
                if (fits(label)) {
                    candidate = label
                    break
                }
            }
            if (candidate == null) return null
            placed += candidate
        }
        return placed
    }

    var selected = required
    var placedLabels = place(selected) ?: return emptyList()
    while (selected.size < ticks.size) {
        val candidate = (ticks.indices - selected.toSet()).sortedWith(
            compareByDescending<Int> { index -> selected.minOf { abs(index - it) } }.thenBy { it },
        ).firstNotNullOfOrNull { index ->
            val expanded = (selected + index).sorted()
            place(expanded)?.let { expanded to it }
        } ?: break
        selected = candidate.first
        placedLabels = candidate.second
    }
    return placedLabels.sortedBy { it.index }
}

@Composable
internal fun NewGaugeDial(
    scale: GaugeScale,
    value: Float?,
    formattedValue: String,
    unit: String,
    style: SensorGaugeStyle,
    dialMeasure: NewGaugeDialMeasure,
    modifier: Modifier = Modifier,
    labelOverflow: Dp = 14.dp,
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val sections = remember(scale, resources, context.theme) {
        scale.sections.map { section ->
            GaugeRange(section.start, section.end, Color(resources.getColor(section.color, context.theme)))
        }
    }
    val colors = MaterialTheme.colorScheme
    val density = LocalDensity.current
    val normalized = scale.normalized(value)
    BoxWithConstraints(modifier) {
        val faceWidth = maxWidth - labelOverflow * 2f
        Canvas(Modifier.fillMaxSize()) {
            drawGaugeCanvas(
                scale = scale,
                colors = colors,
                sections = sections,
                normalized = normalized,
                style = style,
                faceWidth = size.width - labelOverflow.toPx() * 2f,
                arcCenterY = with(density) { dialMeasure.centerY.toPx() },
                labels = dialMeasure.labels,
            )
        }
        if (style == SensorGaugeStyle.PreciseArc) {
            val outerStrokeWidth = 5.dp
            val blueStrokeWidth = 5.dp
            val interArcGap = 3.dp
            val textClearance = 8.dp
            val outerRadius = faceWidth / 2f - 5.dp
            val blueCenterRadius = outerRadius - outerStrokeWidth / 2f - interArcGap - blueStrokeWidth / 2f
            val blueInnerEdgeRadius = blueCenterRadius - blueStrokeWidth / 2f
            val readoutSize = (blueInnerEdgeRadius - textClearance) * sqrt(2f)
            val dialCenterY = dialMeasure.centerY
            GaugeReadout(
                formattedValue = formattedValue,
                unit = unit,
                color = colors.onSurface,
                unitColor = colors.onSurfaceVariant,
                modifier = Modifier.size(readoutSize).align(Alignment.TopCenter).offset(y = dialCenterY - readoutSize / 2f),
                maxFontSize = 28.sp,
            )
        }
    }
}

@Composable
private fun GaugeReadout(
    formattedValue: String,
    unit: String,
    color: Color,
    unitColor: Color,
    modifier: Modifier,
    maxFontSize: androidx.compose.ui.unit.TextUnit,
) {
    Column(
        modifier = modifier.clearAndSetSemantics {},
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = formattedValue,
            modifier = Modifier.weight(1f, fill = false).fillMaxWidth(),
            color = color,
            textAlign = TextAlign.Center,
            fontSize = maxFontSize,
            maxLines = 1,
            autoSize = TextAutoSize.StepBased(minFontSize = 12.sp, maxFontSize = maxFontSize, stepSize = 1.sp),
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.SemiBold,
                fontFeatureSettings = "tnum",
                lineHeight = 1.1.em,
            ),
        )
        Text(
            text = unit,
            color = unitColor,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            maxLines = 1,
            autoSize = TextAutoSize.StepBased(minFontSize = 11.sp, maxFontSize = 12.sp, stepSize = 1.sp),
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawGaugeCanvas(
    scale: GaugeScale,
    colors: androidx.compose.material3.ColorScheme,
    sections: List<GaugeRange>,
    normalized: Float?,
    style: SensorGaugeStyle,
    faceWidth: Float,
    arcCenterY: Float,
    labels: List<NewGaugeDialLabel>,
) {
    val outerStrokeWidth = 5.dp.toPx()
    val blueStrokeWidth = 5.dp.toPx()
    val centerX = size.width / 2f
    val radius = faceWidth / 2f - 5.dp.toPx()
    val arcRect = Rect(centerX - radius, arcCenterY - radius, centerX + radius, arcCenterY + radius)

    drawArc(
        color = colors.surfaceVariant,
        startAngle = GaugeScale.START_ANGLE_DEGREES,
        sweepAngle = GaugeScale.SWEEP_ANGLE_DEGREES,
        useCenter = false,
        topLeft = arcRect.topLeft,
        size = arcRect.size,
        style = Stroke(width = outerStrokeWidth, cap = androidx.compose.ui.graphics.StrokeCap.Butt),
    )
    sections.forEach { section ->
        drawArc(
            color = section.color.copy(alpha = .78f),
            startAngle = scale.angleAt(section.start),
            sweepAngle = scale.sweepFor(section.end - section.start),
            useCenter = false,
            topLeft = arcRect.topLeft,
            size = arcRect.size,
            style = Stroke(width = outerStrokeWidth, cap = androidx.compose.ui.graphics.StrokeCap.Butt),
        )
    }

    if (style == SensorGaugeStyle.PreciseArc) {
        normalized?.let { progress ->
            // Keep the arc bands visually grouped while leaving 8dp between the blue edge and readout bounds.
            val innerRadius = radius - (outerStrokeWidth + blueStrokeWidth) / 2f - 3.dp.toPx()
            val innerRect = Rect(centerX - innerRadius, arcCenterY - innerRadius, centerX + innerRadius, arcCenterY + innerRadius)
            if (progress > 0f) drawArc(
                color = colors.primary,
                startAngle = GaugeScale.START_ANGLE_DEGREES,
                sweepAngle = scale.sweepFor(progress),
                useCenter = false,
                topLeft = innerRect.topLeft,
                size = innerRect.size,
                style = Stroke(width = blueStrokeWidth, cap = androidx.compose.ui.graphics.StrokeCap.Round),
            )
        }
        drawGaugeTicks(scale, colors.onSurfaceVariant, centerX, arcCenterY, radius, preciseArc = true)
        drawDialLabels(labels, colors.onSurfaceVariant, centerX, arcCenterY)
    } else {
        drawGaugeTicks(scale, colors.onSurfaceVariant, centerX, arcCenterY, radius, preciseArc = false)
        normalized?.let { progress ->
            val angle = Math.toRadians(scale.angleAt(progress).toDouble())
            val needleLength = minOf(radius * .8f, radius - 14.dp.toPx()).coerceAtLeast(0f)
            drawLine(
                color = colors.primary,
                start = Offset(centerX, arcCenterY),
                end = Offset(centerX + cos(angle).toFloat() * needleLength, arcCenterY + sin(angle).toFloat() * needleLength),
                strokeWidth = 3.dp.toPx(),
                cap = androidx.compose.ui.graphics.StrokeCap.Round,
            )
            drawCircle(colors.primary, radius = 5.dp.toPx(), center = Offset(centerX, arcCenterY))
        }
        drawDialLabels(labels, colors.onSurfaceVariant, centerX, arcCenterY)
    }
}

private data class GaugeRange(val start: Float, val end: Float, val color: Color)

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawGaugeTicks(
    scale: GaugeScale,
    tickColor: Color,
    centerX: Float,
    centerY: Float,
    radius: Float,
    preciseArc: Boolean,
) {
    if (preciseArc) {
        // Configured ticks share their values and positions; this style keeps each mark inside the range band.
        scale.ticks.forEach { tick ->
            drawGaugeTick(tick.angleDegrees, tickColor, centerX, centerY, radius + 1.75.dp.toPx(), radius - 1.75.dp.toPx(), 1.5.dp.toPx())
        }
    } else {
        // Keep the configured marks' inner edge at radius - 10dp for needle-tip clearance.
        scale.ticks.forEach { tick ->
            drawGaugeTick(tick.angleDegrees, tickColor, centerX, centerY, radius - 4.dp.toPx(), radius - 10.dp.toPx(), 1.5.dp.toPx())
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawGaugeTick(
    angleDegrees: Float,
    color: Color,
    centerX: Float,
    centerY: Float,
    outerRadius: Float,
    innerRadius: Float,
    strokeWidth: Float,
) {
    val angle = Math.toRadians(angleDegrees.toDouble())
    drawLine(
        color,
        Offset(centerX + cos(angle).toFloat() * innerRadius, centerY + sin(angle).toFloat() * innerRadius),
        Offset(centerX + cos(angle).toFloat() * outerRadius, centerY + sin(angle).toFloat() * outerRadius),
        strokeWidth = strokeWidth,
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawDialLabels(
    labels: List<NewGaugeDialLabel>,
    textColor: Color,
    centerX: Float,
    centerY: Float,
) {
    val labelPaint = NativePaint(NativePaint.ANTI_ALIAS_FLAG).apply { color = textColor.toArgb(); textSize = 12.sp.toPx() }
    drawIntoCanvas { canvas ->
        labelPaint.textAlign = NativePaint.Align.CENTER
        val baselineOffset = (labelPaint.fontMetrics.ascent + labelPaint.fontMetrics.descent) / 2f
        labels.forEach { label ->
            canvas.nativeCanvas.drawText(label.text, centerX + label.centerX, centerY + label.centerY - baselineOffset, labelPaint)
        }
    }
}
