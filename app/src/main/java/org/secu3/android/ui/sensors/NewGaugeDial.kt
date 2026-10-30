package org.secu3.android.ui.sensors

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.graphics.Color
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
import androidx.compose.ui.unit.sp
import org.secu3.android.models.SensorGaugeStyle
import org.secu3.android.ui.sensors.models.GaugeScale
import org.secu3.android.ui.sensors.models.GaugeTick
import kotlin.math.sqrt

internal data class NewGaugeDialLabel(val tick: GaugeTick, val text: String, val centerX: Float, val centerY: Float, val width: Float, val height: Float) {
    val index: Int get() = tick.index
    val value: Float get() = tick.value
}
internal data class NewGaugeDialMeasure(
    val height: Dp,
    val centerY: Dp,
    val faceRadius: Dp,
    val labelTextSizePx: Float,
    val labels: List<NewGaugeDialLabel>,
)

@Composable
internal fun NewGaugeDial(
    scale: GaugeScale,
    renderer: GaugeDialRenderer,
    progress: () -> Float?,
    formattedValue: String,
    unit: String,
    style: SensorGaugeStyle,
    dialMeasure: NewGaugeDialMeasure,
    modifier: Modifier = Modifier,
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
    Box(modifier) {
        Canvas(Modifier.fillMaxSize()) {
            with(renderer) { drawGaugeCanvas(
                scale = scale,
                colors = colors,
                sections = sections,
                normalized = progress(),
                style = style,
                faceRadius = with(density) { dialMeasure.faceRadius.toPx() },
                arcCenterY = with(density) { dialMeasure.centerY.toPx() },
                labelTextSize = dialMeasure.labelTextSizePx,
                labels = dialMeasure.labels,
            ) }
        }
        if (style == SensorGaugeStyle.PreciseArc) {
            val outerStrokeWidth = 5.dp
            val blueStrokeWidth = 5.dp
            val interArcGap = 3.dp
            val textClearance = 8.dp
            val outerRadius = dialMeasure.faceRadius
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
