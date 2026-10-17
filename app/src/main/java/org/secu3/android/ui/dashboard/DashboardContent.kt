/*
 *    SecuDroid  - An open source, free manager for SECU-3 engine control unit
 *    Copyright (C) 2026 Vitalii O. Kosharskyi. Ukraine, Kyiv
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.secu3.android.ui.dashboard

import androidx.annotation.StringRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.secu3.android.ui.compose.SecuComposeTheme
import org.secu3.android.ui.compose.LocalSecuPalette
import org.secu3.android.R
import org.secu3.android.connection.Connected
import org.secu3.android.connection.ConnectionState
import org.secu3.android.connection.Disconnected
import org.secu3.android.connection.InProgress
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

@Composable
fun DashboardContent(
    data: DashboardViewData?,
    connectionState: ConnectionState?,
) {
    SecuComposeTheme {
        val colors = MaterialTheme.colorScheme
        val liveData = data.takeIf { connectionState == Connected }
        Surface(color = colors.background) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .background(colors.background)
                    .padding(horizontal = 24.dp, vertical = 14.dp)
            ) {
                val availableWidth = maxWidth
                val compact = maxHeight < 380.dp || availableWidth < 640.dp
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DashboardStatusBar(
                        data = liveData,
                        connectionState = connectionState,
                        compact = compact,
                    )

                    if (availableWidth < 600.dp) {
                        Column(
                            Modifier.fillMaxWidth().weight(1f).testTag("dashboard-primary-region"),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            ArcGauge(liveData?.rpm, 0f, 8000f, stringResource(R.string.units_rpm), Modifier.weight(1f).fillMaxWidth().testTag("dashboard-rpm-gauge"), compact, .72f, .88f)
                            ArcGauge(liveData?.speed, 0f, 220f, stringResource(R.string.units_km_h), Modifier.weight(1f).fillMaxWidth().testTag("dashboard-speed-gauge"), compact, .65f, .85f)
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth().weight(1f).testTag("dashboard-primary-region"),
                            horizontalArrangement = Arrangement.spacedBy(28.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            ArcGauge(liveData?.rpm, 0f, 8000f, stringResource(R.string.units_rpm), Modifier.weight(1f).fillMaxHeight().testTag("dashboard-rpm-gauge"), compact, .72f, .88f)
                            ArcGauge(liveData?.speed, 0f, 220f, stringResource(R.string.units_km_h), Modifier.weight(1f).fillMaxHeight().testTag("dashboard-speed-gauge"), compact, .65f, .85f)
                        }
                    }

                    SecondaryReadouts(
                        data = liveData,
                        compact = compact,
                        itemsPerRow = when {
                            availableWidth < 600.dp -> 2
                            availableWidth < 760.dp -> 3
                            else -> 5
                        },
                    )
                }
            }
        }
    }
}

@Preview(
    name = "Dashboard Landscape",
    widthDp = 891,
    heightDp = 411,
    showBackground = true,
        backgroundColor = 0xFFF3F6FA,
)
@Composable
private fun DashboardLandscapePreview() {
    DashboardContent(
        data = previewDashboardData(),
        connectionState = Connected,
    )
}

@Preview(
    name = "Dashboard Compact",
    widthDp = 740,
    heightDp = 360,
    showBackground = true,
        backgroundColor = 0xFFF3F6FA,
)
@Composable
private fun DashboardCompactPreview() {
    DashboardContent(
        data = previewDashboardData(
            rpm = 7050f,
            speed = 148f,
            ledCheckEngine = true,
            ledFan = true,
        ),
        connectionState = Connected,
    )
}

@Preview(
    name = "Dashboard Without Oil",
    widthDp = 891,
    heightDp = 411,
    showBackground = true,
        backgroundColor = 0xFFF3F6FA,
)
@Composable
private fun DashboardWithoutAfrPreview() {
    DashboardContent(
        data = previewDashboardData(
            oilTemperature = 0f,
            afr = 14.7f,
            fuelLevel = 36f,
        ),
        connectionState = Connected,
    )
}

private fun previewDashboardData(
    rpm: Float = 2250f,
    speed: Float = 84f,
    afr: Float = 14.7f,
    oilTemperature: Float = 92f,
    fuelLevel: Float = 36f,
    ledCheckEngine: Boolean = false,
    ledFan: Boolean = false,
) = DashboardViewData(
    rpm = rpm,
    speed = speed,
    load = 42f,
    map = 62f,
    tps = 18f,
    temperature = 87f,
    voltage = 13.8f,
    currentAngle = 12f,
    afr = afr,
    oilTemperature = oilTemperature,
    fuelLevel = fuelLevel,
    intakeAirTemperature = 28f,
    knockRetard = 0f,
    ledCheckEngine = ledCheckEngine,
    ledGasoline = true,
    ledEco = false,
    ledPower = false,
    ledChoke = false,
    ledFan = ledFan,
    ledRevLimit = rpm > 6900f,
)

@Composable
private fun DashboardStatusBar(
    data: DashboardViewData?,
    connectionState: ConnectionState?,
    compact: Boolean,
) {
    val statusText = when (connectionState) {
        Connected -> stringResource(if (data == null) R.string.dashboard_status_no_data else R.string.dashboard_status_online)
        InProgress -> stringResource(R.string.dashboard_status_connecting)
        Disconnected -> stringResource(R.string.dashboard_status_offline)
        else -> stringResource(R.string.dashboard_status_no_data)
    }
    val palette = LocalSecuPalette.current
    val statusColor = when (connectionState) {
        Connected -> if (data == null) MaterialTheme.colorScheme.onSurfaceVariant else palette.success
        InProgress -> palette.warning
        Disconnected -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()).testTag("dashboard-status-bar"),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StatusChip(statusText, statusColor, compact)

        if (data?.ledCheckEngine == true) {
            StatusChip(stringResource(R.string.dashboard_warning_check_engine), MaterialTheme.colorScheme.error, compact)
        }
        if ((data?.temperature ?: 0f) >= 105f) {
            StatusChip(stringResource(R.string.dashboard_warning_overheat), MaterialTheme.colorScheme.error, compact)
        } else if ((data?.temperature ?: 0f) >= 100f) {
            StatusChip(stringResource(R.string.dashboard_warning_hot), palette.warning, compact)
        }
        if ((data?.voltage ?: 14f) < 11.8f) {
            StatusChip(stringResource(R.string.dashboard_warning_low_voltage), MaterialTheme.colorScheme.error, compact)
        } else if ((data?.voltage ?: 14f) < 12.3f) {
            StatusChip(stringResource(R.string.dashboard_warning_voltage), palette.warning, compact)
        }
        if ((data?.oilTemperature ?: 0f) >= 130f) {
            StatusChip(stringResource(R.string.dashboard_warning_oil_temp), MaterialTheme.colorScheme.error, compact)
        } else if ((data?.oilTemperature ?: 0f) >= 115f) {
            StatusChip(stringResource(R.string.dashboard_warning_oil_hot), palette.warning, compact)
        }
        if (data?.ledRevLimit == true) {
            StatusChip(stringResource(R.string.dashboard_warning_rev_limit), MaterialTheme.colorScheme.error, compact)
        }
        if ((data?.knockRetard ?: 0f) > 0f) {
            StatusChip(stringResource(R.string.dashboard_warning_knock), MaterialTheme.colorScheme.error, compact)
        }
        if (data?.ledFan == true) {
            StatusChip(stringResource(R.string.dashboard_indicator_fan), palette.warning, compact)
        }
        if (data?.ledGasoline == true) {
            StatusChip(stringResource(R.string.dashboard_indicator_gas), palette.success, compact)
        }
        if (data?.ledEco == true) {
            StatusChip(stringResource(R.string.dashboard_indicator_eco), palette.success, compact)
        }
        if (data?.ledPower == true) {
            StatusChip(stringResource(R.string.dashboard_indicator_power), palette.warning, compact)
        }
        if (data?.ledChoke == true) {
            StatusChip(stringResource(R.string.dashboard_indicator_choke), palette.warning, compact)
        }
    }
}

@Composable
private fun StatusChip(text: String, color: Color, compact: Boolean) {
    Text(
        text = text,
        color = MaterialTheme.colorScheme.onSurface,
        fontSize = if (compact) 12.sp else 14.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.18f))
            .padding(horizontal = 10.dp, vertical = if (compact) 4.dp else 6.dp)
    )
}

@Composable
private fun ArcGauge(
    value: Float?,
    minValue: Float,
    maxValue: Float,
    unit: String,
    modifier: Modifier = Modifier,
    compact: Boolean,
    cautionAt: Float,
    criticalAt: Float,
) {
    val normalized = value?.let { ((it - minValue) / (maxValue - minValue)).coerceIn(0f, 1f) } ?: 0f
    val palette = LocalSecuPalette.current
    val colors = MaterialTheme.colorScheme
    val trackColor = if (colors.background.luminance() > 0.5f) {
        lerp(colors.outline, colors.background, 0.18f)
    } else {
        colors.surfaceVariant
    }
    val valueColor = when {
        value == null -> colors.onSurfaceVariant
        normalized >= criticalAt -> colors.error
        normalized >= cautionAt -> palette.warning
        else -> palette.success
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxHeight()
                .aspectRatio(1.22f)
        ) {
            val diameter = min(size.width, size.height) * 0.88f
            val strokeWidth = diameter * 0.055f
            val topLeft = Offset(
                x = (size.width - diameter) / 2f,
                y = (size.height - diameter) / 2f
            )
            val arcSize = Size(diameter, diameter)
            val startAngle = 135f
            val sweepAngle = 270f

            drawArc(
                color = trackColor,
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            drawArc(
                color = valueColor,
                startAngle = startAngle,
                sweepAngle = sweepAngle * normalized,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            val center = Offset(size.width / 2f, size.height / 2f)
            val outerRadius = diameter / 2f
            val innerRadius = outerRadius - strokeWidth * 1.9f
            val tickCount = 11

            repeat(tickCount) { index ->
                val fraction = index.toFloat() / (tickCount - 1)
                val angleRad = (startAngle + sweepAngle * fraction) * PI.toFloat() / 180f
                val tickColor = if (value != null && fraction <= normalized) valueColor else colors.onSurfaceVariant.copy(alpha = 0.38f)
                val tickStart = Offset(
                    x = center.x + cos(angleRad) * innerRadius,
                    y = center.y + sin(angleRad) * innerRadius
                )
                val tickEnd = Offset(
                    x = center.x + cos(angleRad) * outerRadius,
                    y = center.y + sin(angleRad) * outerRadius
                )
                drawLine(
                    color = tickColor,
                    start = tickStart,
                    end = tickEnd,
                    strokeWidth = strokeWidth * 0.16f,
                    cap = StrokeCap.Round
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = value?.toInt()?.toString() ?: "—",
                color = colors.onSurface,
                fontSize = if (compact) 48.sp else 68.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = unit,
                color = colors.onSurfaceVariant,
                fontSize = if (compact) 15.sp else 18.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SecondaryReadouts(
    data: DashboardViewData?,
    compact: Boolean,
    itemsPerRow: Int,
) {
    androidx.compose.foundation.layout.FlowRow(
        modifier = Modifier.fillMaxWidth().testTag("dashboard-secondary-readouts"),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        maxItemsInEachRow = itemsPerRow,
    ) {
        ReadoutTile("dashboard-readout-clt", stringResource(R.string.dashboard_readout_clt), data?.temperature, stringResource(R.string.units_degrees_celcius), false, compact, Modifier.weight(1f))
        ReadoutTile("dashboard-readout-voltage", stringResource(R.string.dashboard_readout_voltage), data?.voltage, stringResource(R.string.units_volts), true, compact, Modifier.weight(1f))
        ReadoutTile("dashboard-readout-load", stringResource(R.string.dashboard_readout_load), data?.load, "", false, compact, Modifier.weight(1f))
        ReadoutTile("dashboard-readout-tps", stringResource(R.string.dashboard_readout_tps), data?.tps, stringResource(R.string.units_percents), false, compact, Modifier.weight(1f))
        val optionalReadout = optionalDailyReadout(data)
        ReadoutTile(
            "dashboard-readout-optional",
            stringResource(optionalReadout.label),
            optionalReadout.value,
            optionalReadout.unit?.let { stringResource(it) }.orEmpty(),
            optionalReadout.decimal,
            compact,
            Modifier.weight(1f)
        )
    }
}

private fun optionalDailyReadout(data: DashboardViewData?): DashboardReadout {
    return when {
        data?.oilTemperature != null && data.oilTemperature > 0f -> {
            DashboardReadout(R.string.dashboard_readout_oil, data.oilTemperature, R.string.units_degrees_celcius)
        }
        data?.afr != null && data.afr > 0f -> DashboardReadout(R.string.dashboard_readout_afr, data.afr, null, true)
        data?.fuelLevel != null && data.fuelLevel > 0f -> DashboardReadout(R.string.dashboard_readout_fuel, data.fuelLevel, R.string.units_liter)
        data?.map != null && data.map > 0f -> DashboardReadout(R.string.dashboard_readout_map, data.map, R.string.units_pressure_kpa)
        data?.intakeAirTemperature != null && data.intakeAirTemperature < 999f -> {
            DashboardReadout(R.string.dashboard_readout_iat, data.intakeAirTemperature, R.string.units_degrees_celcius)
        }
        else -> DashboardReadout(R.string.dashboard_readout_aux, null, null)
    }
}

private data class DashboardReadout(
    @StringRes val label: Int,
    val value: Float?,
    @StringRes val unit: Int?,
    val decimal: Boolean = false,
)

@Composable
private fun ReadoutTile(
    testTag: String,
    label: String,
    value: Float?,
    unit: String,
    decimal: Boolean,
    compact: Boolean,
    modifier: Modifier = Modifier,
) {
    val formattedValue = value?.let {
        if (decimal) "%.1f".format(it) else it.toInt().toString()
    } ?: "--"

    Row(
        modifier = modifier.testTag(testTag)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 12.dp, vertical = if (compact) 7.dp else 10.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = label,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = if (compact) 12.sp else 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.testTag("$testTag-label"),
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = unit,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                modifier = Modifier.testTag("$testTag-unit"),
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = formattedValue,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = if (compact) 24.sp else 31.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            modifier = Modifier.testTag("$testTag-value"),
        )
    }
}
