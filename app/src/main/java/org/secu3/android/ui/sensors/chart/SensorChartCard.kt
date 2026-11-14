package org.secu3.android.ui.sensors.chart

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.secu3.android.R
import org.secu3.android.connection.Connected
import org.secu3.android.connection.ConnectionState
import org.secu3.android.domain.sensors.SensorChartSample
import org.secu3.android.domain.sensors.SensorChartSession
import org.secu3.android.models.InjectionTimingMode
import org.secu3.android.models.sensors.GaugeType
import org.secu3.android.ui.sensors.models.GaugeItem
import org.secu3.android.ui.sensors.models.SensorPresentationCatalog
import kotlin.math.pow
import java.text.NumberFormat
import java.util.Locale

@Composable
internal fun SensorChartCard(
    item: GaugeItem,
    samples: List<SensorChartSample>,
    connectionState: ConnectionState?,
    timingMode: InjectionTimingMode,
    presentationCatalog: SensorPresentationCatalog,
    modifier: Modifier = Modifier,
    onMenu: () -> Unit,
) {
    val type = item.state.gaugeType
    val gauge = presentationCatalog.gauge(type)
    val title = stringResource(gauge.title)
    val unit = stringResource(gauge.units).trim()
    val displaySamples = remember(connectionState, samples, type, timingMode) {
        if (connectionState != Connected) emptyList()
        else if (type == GaugeType.BEGIN_INJ_PHASE || type == GaugeType.END_INJ_PHASE) {
            samples.filter { it.value.isFinite() }.map { it.copy(value = timingMode.transform(it.value)) }.filter { it.value.isFinite() }
        } else samples.filter { it.value.isFinite() }
    }
    val current = displaySamples.lastOrNull()?.let { presentationCatalog.formatValue(type, it.value) } ?: "—"
    val colors = MaterialTheme.colorScheme
    Card(
        modifier = modifier.fillMaxWidth().testTag("sensor-chart-card-${type.name}"),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, colors.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        onClick = onMenu,
    ) {
        Column(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(title, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, style = MaterialTheme.typography.titleMedium)
            Text(
                "$current $unit".trimEnd(),
                modifier = Modifier.fillMaxWidth().testTag("sensor-chart-latest-${type.name}"),
                color = colors.primary,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleLarge.copy(fontFeatureSettings = "tnum", fontWeight = FontWeight.SemiBold),
                maxLines = 1,
            )
            if (displaySamples.isEmpty()) {
                Text(
                    stringResource(if (connectionState == Connected) R.string.sensor_chart_waiting else R.string.dashboard_status_no_data),
                    modifier = Modifier.fillMaxWidth(),
                    color = colors.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center,
                )
            } else {
                val minimum = displaySamples.minOf { it.value }
                val maximum = displaySamples.maxOf { it.value }
                val span = maximum - minimum
                val headroom = if (span > 0f) span * 0.08f else maxOf(kotlin.math.abs(maximum) * 0.05f, 10f.pow(-presentationCatalog.decimals(type)))
                val low = minimum - headroom
                val high = maximum + headroom
                val locale = LocalConfiguration.current.locales[0] ?: Locale.getDefault()
                val formatter = remember(locale, type) {
                    NumberFormat.getNumberInstance(locale).apply {
                        isGroupingUsed = false
                        maximumFractionDigits = presentationCatalog.decimals(type).coerceAtMost(6)
                    }
                }
                val description = stringResource(R.string.sensor_chart_description, title)
                Row(Modifier.height(112.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Column(Modifier.fillMaxHeight(), verticalArrangement = Arrangement.SpaceBetween) {
                        listOf(high, (high + low) / 2f, low).forEach { value ->
                            Text(formatter.format(value), color = colors.onSurfaceVariant, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                        }
                    }
                    Canvas(
                        Modifier.weight(1f).fillMaxHeight().testTag("sensor-chart-plot-${type.name}")
                            .semantics { contentDescription = description },
                    ) {
                        val left = 2.dp.toPx()
                        val right = size.width - left
                        val top = 6.dp.toPx()
                        val bottom = size.height - 6.dp.toPx()
                        if (right <= left || bottom <= top) return@Canvas
                        for (step in 0..2) {
                            val y = top + (bottom - top) * step / 2f
                            drawLine(colors.outlineVariant, Offset(left, y), Offset(right, y), 1.dp.toPx())
                        }
                        val startTime = displaySamples.last().elapsedMillis - SensorChartSession.WINDOW_MILLIS
                        val path = Path()
                        var latest = Offset.Zero
                        displaySamples.forEachIndexed { index, sample ->
                            val time = ((sample.elapsedMillis - startTime).toDouble() / SensorChartSession.WINDOW_MILLIS)
                                .toFloat().coerceIn(0f, 1f)
                            val value = ((sample.value - low) / (high - low)).coerceIn(0f, 1f)
                            latest = Offset(left + time * (right - left), bottom - value * (bottom - top))
                            if (index == 0) path.moveTo(latest.x, latest.y) else path.lineTo(latest.x, latest.y)
                        }
                        drawPath(path, colors.primary, style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                        drawCircle(colors.primary, radius = 3.dp.toPx(), center = latest)
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(stringResource(R.string.sensor_chart_time_start), color = colors.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                    Text(stringResource(R.string.sensor_chart_time_end), color = colors.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}
