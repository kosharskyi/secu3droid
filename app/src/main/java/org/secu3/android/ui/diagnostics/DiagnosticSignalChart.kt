/*
 *    SecuDroid  - An open source, free manager for SECU-3 engine control unit
 *    Copyright (C) 2026 Vitalii O. Kosharskyi. Ukraine, Kyiv
 *
 *    SECU-3  - An open source, free engine control unit
 *    Copyright (C) 2007-2026 Alexey A. Shabelnikov. Ukraine, Kyiv
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 */
package org.secu3.android.ui.diagnostics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import org.secu3.android.R
import org.secu3.android.domain.diagnostics.DiagnosticSample
import org.secu3.android.domain.diagnostics.DiagnosticsSession
import java.util.Locale

@Composable
internal fun DiagnosticSignalChart(
    label: String,
    samples: List<DiagnosticSample>,
    value: (DiagnosticSample) -> Float,
    helpText: String,
    modifier: Modifier = Modifier,
) {
    val latest = samples.lastOrNull()?.let(value)
    val description = if (latest == null) {
        stringResource(R.string.diag_chart_empty_description, label)
    } else {
        stringResource(R.string.diag_chart_description, label, stringResource(R.string.diag_chart_latest_value, String.format(Locale.US, "%.3f", latest)))
    }
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = description }
            .testTag("diagnostic_chart_$label"),
        shape = RoundedCornerShape(16.dp),
        color = colors.surfaceContainerLow,
        contentColor = colors.onSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.outlineVariant),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Row(Modifier.weight(1f), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Text(label, Modifier.weight(1f, fill = false), style = MaterialTheme.typography.titleSmall)
                    DiagnosticHelpButton(label, helpText, "diagnostic_help_chart_$label")
                }
                latest?.let {
                    Text(
                        stringResource(R.string.diag_chart_latest_value, String.format(Locale.US, "%.3f", it)),
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.onSurfaceVariant,
                    )
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(Modifier.height(112.dp), verticalArrangement = Arrangement.SpaceBetween) {
                    Text(stringResource(R.string.diag_chart_voltage_top), style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                    Text(stringResource(R.string.diag_chart_voltage_bottom), style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                }
                Column(Modifier.weight(1f)) {
                    Canvas(
                        Modifier.fillMaxWidth().height(112.dp).clip(RoundedCornerShape(4.dp))
                            .testTag("diagnostic_chart_plot_$label"),
                    ) {
                        val plotColor = colors.primary
                        val gridColor = colors.outlineVariant
                        val yFor = { voltage: Float -> size.height * (1f - voltage.coerceIn(0f, 5f) / 5f) }
                        for (voltage in 0..5) {
                            val y = yFor(voltage.toFloat())
                            drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
                        }
                        val newestTime = samples.lastOrNull()?.timestampNanos ?: return@Canvas
                        val windowStart = newestTime - DiagnosticsSession.DIAGNOSTIC_SAMPLE_WINDOW_NANOS
                        val path = Path()
                        var hasPoint = false
                        clipRect {
                            samples.forEach { sample ->
                                val rawValue = value(sample)
                                if (!rawValue.isFinite()) return@forEach
                                val x = ((sample.timestampNanos - windowStart).toDouble() / DiagnosticsSession.DIAGNOSTIC_SAMPLE_WINDOW_NANOS)
                                    .toFloat().coerceIn(0f, 1f) * size.width
                                val point = Offset(x, yFor(rawValue))
                                if (hasPoint) path.lineTo(point.x, point.y) else {
                                    path.moveTo(point.x, point.y)
                                    hasPoint = true
                                }
                            }
                            if (hasPoint) drawPath(path, plotColor, style = Stroke(width = 2.5.dp.toPx()))
                            val lastValid = samples.lastOrNull { value(it).isFinite() }
                            if (lastValid != null) {
                                val x = ((lastValid.timestampNanos - windowStart).toDouble() / DiagnosticsSession.DIAGNOSTIC_SAMPLE_WINDOW_NANOS)
                                    .toFloat().coerceIn(0f, 1f) * size.width
                                drawCircle(plotColor, radius = 3.dp.toPx(), center = Offset(x, yFor(value(lastValid))))
                            }
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(stringResource(R.string.diag_chart_time_start), style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                        Text(stringResource(R.string.diag_chart_time_end), style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
