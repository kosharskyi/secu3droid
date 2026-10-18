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
import org.secu3.android.ui.sensors.models.GaugeType
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

internal data class NewGaugeDialMeasure(val height: Dp, val centerY: Dp)

internal fun measureNewGaugeDial(width: Dp, density: Density): NewGaugeDialMeasure {
    val labelPaint = NativePaint(NativePaint.ANTI_ALIAS_FLAG).apply {
        textSize = with(density) { 12.sp.toPx() }
    }
    val metrics = labelPaint.fontMetrics
    val radius = with(density) { width.toPx() / 2f - 5.dp.toPx() }
    val halfStroke = with(density) { 2.5.dp.toPx() }
    val labelGap = with(density) { 3.dp.toPx() }
    val edgePadding = with(density) { 2.dp.toPx() }
    val centerY = radius + halfStroke + labelGap + (metrics.descent - metrics.ascent) + edgePadding
    val lastLabelBaseline = centerY + radius / sqrt(2f) + halfStroke + labelGap - metrics.ascent
    val height = lastLabelBaseline + metrics.descent + edgePadding
    return with(density) { NewGaugeDialMeasure(height.toDp(), centerY.toDp()) }
}

@Composable
internal fun NewGaugeDial(
    type: GaugeType,
    value: Float?,
    formattedValue: String,
    unit: String,
    style: SensorGaugeStyle,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val sections = remember(type, resources, context.theme) {
        type.sections.map { section ->
            GaugeRange(section.start, section.end, Color(resources.getColor(section.color, context.theme)))
        }
    }
    val colors = MaterialTheme.colorScheme
    val normalized = value?.let { ((it - type.minValue) / (type.maxValue - type.minValue)).coerceIn(0f, 1f) }
    BoxWithConstraints(modifier) {
        Canvas(Modifier.fillMaxSize()) { drawGaugeCanvas(type, colors, sections, normalized, style) }
        if (style == SensorGaugeStyle.PreciseArc) {
            val outerStrokeWidth = 5.dp
            val blueStrokeWidth = 5.dp
            val interArcGap = 3.dp
            val textClearance = 8.dp
            val outerRadius = maxWidth / 2f - 5.dp
            val blueCenterRadius = outerRadius - outerStrokeWidth / 2f - interArcGap - blueStrokeWidth / 2f
            val blueInnerEdgeRadius = blueCenterRadius - blueStrokeWidth / 2f
            val readoutSize = (blueInnerEdgeRadius - textClearance) * sqrt(2f)
            val dialCenterY = measureNewGaugeDial(maxWidth, LocalDensity.current).centerY
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
    type: GaugeType,
    colors: androidx.compose.material3.ColorScheme,
    sections: List<GaugeRange>,
    normalized: Float?,
    style: SensorGaugeStyle,
) {
    val width = size.width
    val outerStrokeWidth = 5.dp.toPx()
    val blueStrokeWidth = 5.dp.toPx()
    val centerX = width / 2f
    val radius = width / 2f - 5.dp.toPx()
    val textPaint = NativePaint(NativePaint.ANTI_ALIAS_FLAG).apply { textSize = 12.sp.toPx() }
    val metrics = textPaint.fontMetrics
    val halfStroke = outerStrokeWidth / 2f
    val labelGap = 3.dp.toPx()
    val edgePadding = 2.dp.toPx()
    val arcCenterY = radius + halfStroke + labelGap + (metrics.descent - metrics.ascent) + edgePadding
    val arcRect = Rect(centerX - radius, arcCenterY - radius, centerX + radius, arcCenterY + radius)

    drawArc(
        color = colors.surfaceVariant,
        startAngle = 135f,
        sweepAngle = 270f,
        useCenter = false,
        topLeft = arcRect.topLeft,
        size = arcRect.size,
        style = Stroke(width = outerStrokeWidth, cap = androidx.compose.ui.graphics.StrokeCap.Butt),
    )
    sections.forEach { section ->
        drawArc(
            color = section.color.copy(alpha = .78f),
            startAngle = 135f + 270f * section.start,
            sweepAngle = 270f * (section.end - section.start),
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
                startAngle = 135f,
                sweepAngle = 270f * progress,
                useCenter = false,
                topLeft = innerRect.topLeft,
                size = innerRect.size,
                style = Stroke(width = blueStrokeWidth, cap = androidx.compose.ui.graphics.StrokeCap.Round),
            )
        }
        drawDialLabels(type, colors.onSurfaceVariant, centerX, arcCenterY, radius)
    } else {
        drawThinTicks(colors.onSurfaceVariant, centerX, arcCenterY, radius)
        normalized?.let { progress ->
            val angle = Math.toRadians(135.0 + 270.0 * progress)
            val needleLength = radius * .56f
            drawLine(
                color = colors.primary,
                start = Offset(centerX, arcCenterY),
                end = Offset(centerX + cos(angle).toFloat() * needleLength, arcCenterY + sin(angle).toFloat() * needleLength),
                strokeWidth = 2.dp.toPx(),
                cap = androidx.compose.ui.graphics.StrokeCap.Round,
            )
            drawCircle(colors.primary, radius = 4.dp.toPx(), center = Offset(centerX, arcCenterY))
        }
        drawDialLabels(type, colors.onSurfaceVariant, centerX, arcCenterY, radius)
    }
}

private data class GaugeRange(val start: Float, val end: Float, val color: Color)

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawThinTicks(
    tickColor: Color,
    centerX: Float,
    centerY: Float,
    radius: Float,
) {
    listOf(0f, .25f, .5f, .75f, 1f).forEach { fraction ->
        val angle = Math.toRadians(135.0 + 270.0 * fraction)
        val outer = radius - 4.dp.toPx()
        val inner = outer - if (fraction == 0f || fraction == .5f || fraction == 1f) 6.dp.toPx() else 3.dp.toPx()
        drawLine(
            tickColor,
            Offset(centerX + cos(angle).toFloat() * inner, centerY + sin(angle).toFloat() * inner),
            Offset(centerX + cos(angle).toFloat() * outer, centerY + sin(angle).toFloat() * outer),
            strokeWidth = 1.dp.toPx(),
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawDialLabels(
    type: GaugeType,
    textColor: Color,
    centerX: Float,
    centerY: Float,
    radius: Float,
) {
    val values = listOf(type.minValue, (type.minValue + type.maxValue) / 2f, type.maxValue)
    val labelPaint = NativePaint(NativePaint.ANTI_ALIAS_FLAG).apply { color = textColor.toArgb(); textSize = 12.sp.toPx() }
    drawIntoCanvas { canvas ->
        values.forEachIndexed { index, tickValue ->
            val fraction = index / 2f
            val angle = Math.toRadians(135.0 + 270.0 * fraction)
            val label = formatNewGaugeTickLabel(tickValue)
            labelPaint.textAlign = NativePaint.Align.CENTER
            val labelWidth = labelPaint.measureText(label)
            val endpointRadius = radius / kotlin.math.sqrt(2f)
            val rawX = if (index == 1) centerX else centerX + cos(angle).toFloat() * endpointRadius
            val x = rawX.coerceIn(labelWidth / 2f + 2.dp.toPx(), size.width - labelWidth / 2f - 2.dp.toPx())
            val baseline = if (index == 1) {
                centerY - radius - 2.5.dp.toPx() - 3.dp.toPx() - labelPaint.fontMetrics.descent
            } else {
                centerY + endpointRadius + 2.5.dp.toPx() + 3.dp.toPx() - labelPaint.fontMetrics.ascent
            }
            canvas.nativeCanvas.drawText(label, x, baseline, labelPaint)
        }
    }
}

internal fun formatNewGaugeTickLabel(value: Float): String {
    val rounded = kotlin.math.round(value)
    return if (kotlin.math.abs(value - rounded) < .001f) {
        String.format(Locale.getDefault(), "%.0f", value)
    } else {
        String.format(Locale.getDefault(), "%.1f", value)
    }
}
