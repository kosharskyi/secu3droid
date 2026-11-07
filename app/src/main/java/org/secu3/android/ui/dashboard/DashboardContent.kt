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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.key
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.secu3.android.ui.compose.SecuComposeTheme
import org.secu3.android.ui.compose.LocalSecuPalette
import org.secu3.android.ui.compose.rememberAnimatedGaugeProgress
import org.secu3.android.R
import org.secu3.android.connection.Connected
import org.secu3.android.connection.ConnectionState
import org.secu3.android.connection.Disconnected
import org.secu3.android.connection.Connecting
import org.secu3.android.connection.Reconnecting
import org.secu3.android.connection.UsbAwaitingEcu
import org.secu3.android.connection.UsbReconnecting
import org.secu3.android.connection.UsbReconnectingAwaitingEcu
import org.secu3.android.models.InjectionTimingMode
import org.secu3.android.models.sensors.GaugeType
import org.secu3.android.ui.sensors.models.GaugePresentation
import org.secu3.android.ui.sensors.models.GaugeScale
import org.secu3.android.ui.sensors.models.SensorPresentationCatalog
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

internal const val DASHBOARD_DECORATIVE_TICK_COUNT = 11

@Composable
fun DashboardContent(
    data: DashboardViewData?,
    connectionState: ConnectionState?,
    dashboardSettings: DashboardUiState,
    presentationCatalog: SensorPresentationCatalog,
    onOpenGaugeSettings: () -> Unit,
) {
    var dashboardDarkTheme by rememberSaveable { mutableStateOf<Boolean?>(null) }
    DashboardTheme(darkTheme = dashboardDarkTheme) {
        val colors = MaterialTheme.colorScheme
        val isDarkTheme = colors.background.luminance() < 0.5f
        val liveData = data.takeIf { connectionState == Connected }
        val primaryGaugeData = liveData?.takeIf {
            it.primaryLeft.type == dashboardSettings.leftGauge &&
                it.primaryRight.type == dashboardSettings.rightGauge &&
                it.injectionTimingMode == dashboardSettings.injectionTimingMode
        }
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        DashboardStatusBar(
                            data = liveData,
                            connectionState = connectionState,
                            compact = compact,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(
                            onClick = { dashboardDarkTheme = !isDarkTheme },
                            modifier = Modifier.size(48.dp).testTag("dashboard-theme-toggle"),
                        ) {
                            Icon(
                                imageVector = if (isDarkTheme) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                                contentDescription = stringResource(
                                    if (isDarkTheme) R.string.dashboard_theme_switch_to_light
                                    else R.string.dashboard_theme_switch_to_dark
                                ),
                                tint = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                        IconButton(
                            onClick = onOpenGaugeSettings,
                            modifier = Modifier.size(48.dp).testTag("dashboard-gauge-settings"),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Settings,
                                contentDescription = stringResource(R.string.dashboard_gauges_configure),
                            )
                        }
                    }

                    if (availableWidth < 600.dp) {
                        Column(
                            Modifier.fillMaxWidth().weight(1f).testTag("dashboard-primary-region"),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            key(dashboardSettings.leftGauge) {
                                DashboardPrimaryGauge(
                                    value = primaryGaugeData?.primaryLeft?.value,
                                    type = dashboardSettings.leftGauge,
                                    presentationCatalog = presentationCatalog,
                                    injectionTimingMode = dashboardSettings.injectionTimingMode,
                                    modifier = Modifier.weight(1f).fillMaxWidth().testTag("dashboard-rpm-gauge"),
                                    compact = compact,
                                    progressTag = "dashboard-rpm-progress",
                                    gaugeSlot = "left",
                                )
                            }
                            key(dashboardSettings.rightGauge) {
                                DashboardPrimaryGauge(
                                    value = primaryGaugeData?.primaryRight?.value,
                                    type = dashboardSettings.rightGauge,
                                    presentationCatalog = presentationCatalog,
                                    injectionTimingMode = dashboardSettings.injectionTimingMode,
                                    modifier = Modifier.weight(1f).fillMaxWidth().testTag("dashboard-speed-gauge"),
                                    compact = compact,
                                    progressTag = "dashboard-speed-progress",
                                    gaugeSlot = "right",
                                )
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth().weight(1f).testTag("dashboard-primary-region"),
                            horizontalArrangement = Arrangement.spacedBy(28.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            key(dashboardSettings.leftGauge) {
                                DashboardPrimaryGauge(
                                    value = primaryGaugeData?.primaryLeft?.value,
                                    type = dashboardSettings.leftGauge,
                                    presentationCatalog = presentationCatalog,
                                    injectionTimingMode = dashboardSettings.injectionTimingMode,
                                    modifier = Modifier.weight(1f).fillMaxHeight().testTag("dashboard-rpm-gauge"),
                                    compact = compact,
                                    progressTag = "dashboard-rpm-progress",
                                    gaugeSlot = "left",
                                )
                            }
                            key(dashboardSettings.rightGauge) {
                                DashboardPrimaryGauge(
                                    value = primaryGaugeData?.primaryRight?.value,
                                    type = dashboardSettings.rightGauge,
                                    presentationCatalog = presentationCatalog,
                                    injectionTimingMode = dashboardSettings.injectionTimingMode,
                                    modifier = Modifier.weight(1f).fillMaxHeight().testTag("dashboard-speed-gauge"),
                                    compact = compact,
                                    progressTag = "dashboard-speed-progress",
                                    gaugeSlot = "right",
                                )
                            }
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

@Composable
private fun DashboardTheme(darkTheme: Boolean?, content: @Composable () -> Unit) {
    SecuComposeTheme(darkTheme = darkTheme) {
        val inheritedColors = MaterialTheme.colorScheme
        val warmLightColors = inheritedColors.copy(
            background = Color(0xFFD9C7AC),
            onBackground = Color(0xFF2E241B),
            surface = Color(0xFFE9DCC8),
            onSurface = Color(0xFF2E241B),
            surfaceDim = Color(0xFFD0BFA5),
            surfaceBright = Color(0xFFF2E7D6),
            surfaceContainerLowest = Color(0xFFF4EADB),
            surfaceContainerLow = Color(0xFFEFE3D1),
            surfaceContainer = Color(0xFFE9DCC8),
            surfaceContainerHigh = Color(0xFFE3D4BD),
            surfaceContainerHighest = Color(0xFFDDCDB4),
            surfaceVariant = Color(0xFFE3D4BD),
            onSurfaceVariant = Color(0xFF493A2C),
            outline = Color(0xFF786550),
            outlineVariant = Color(0xFFC2AF93),
        )
        val colors = if (inheritedColors.background.luminance() >= 0.5f) {
            warmLightColors
        } else {
            inheritedColors
        }
        MaterialTheme(colorScheme = colors) {
            CompositionLocalProvider(LocalContentColor provides colors.onBackground, content = content)
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
    DashboardPreviewContent(previewDashboardData())
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
    DashboardPreviewContent(
        previewDashboardData(
            rpm = 7050f,
            speed = 148f,
            ledCheckEngine = true,
            ledFan = true,
        ),
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
    DashboardPreviewContent(
        previewDashboardData(
            oilTemperature = 0f,
            afr = 14.7f,
            fuelLevel = 36f,
        ),
    )
}

@Composable
private fun DashboardPreviewContent(data: DashboardViewData) {
    DashboardContent(
        data = data,
        connectionState = Connected,
        dashboardSettings = DashboardUiState(settingsLoaded = true),
        presentationCatalog = SensorPresentationCatalog(),
        onOpenGaugeSettings = {},
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
    primaryLeft = DashboardGaugeReading(GaugeType.RPM, rpm),
    primaryRight = DashboardGaugeReading(GaugeType.VEHICLE_SPEED, speed),
)

@Composable
private fun DashboardStatusBar(
    data: DashboardViewData?,
    connectionState: ConnectionState?,
    compact: Boolean,
    modifier: Modifier = Modifier,
) {
    val statusText = when (connectionState) {
        Connected -> stringResource(if (data == null) R.string.dashboard_status_no_data else R.string.dashboard_status_online)
        Connecting, Reconnecting, UsbAwaitingEcu, UsbReconnecting, UsbReconnectingAwaitingEcu -> stringResource(R.string.dashboard_status_connecting)
        Disconnected -> stringResource(R.string.dashboard_status_offline)
        else -> stringResource(R.string.dashboard_status_no_data)
    }
    val palette = LocalSecuPalette.current
    val statusColor = when (connectionState) {
        Connected -> if (data == null) MaterialTheme.colorScheme.onSurfaceVariant else palette.success
        Connecting, Reconnecting, UsbAwaitingEcu, UsbReconnecting, UsbReconnectingAwaitingEcu -> palette.warning
        Disconnected -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(
        modifier = modifier.fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .testTag("dashboard-status-bar"),
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
private fun DashboardPrimaryGauge(
    value: Float?,
    type: GaugeType,
    presentationCatalog: SensorPresentationCatalog,
    injectionTimingMode: InjectionTimingMode,
    modifier: Modifier,
    compact: Boolean,
    progressTag: String,
    gaugeSlot: String,
) {
    val presentation = presentationCatalog.gauge(type)
    val scale = remember(type, presentation, injectionTimingMode) {
        GaugeScale(type, presentation, injectionTimingMode)
    }
    ArcGauge(
        value = value,
        title = stringResource(presentation.title),
        unit = stringResource(presentation.units),
        catalog = presentationCatalog,
        scale = scale,
        modifier = modifier,
        compact = compact,
        progressTag = progressTag,
        gaugeSlot = gaugeSlot,
    )
}

@Composable
private fun ArcGauge(
    value: Float?,
    title: String,
    unit: String,
    catalog: SensorPresentationCatalog,
    scale: GaugeScale,
    modifier: Modifier = Modifier,
    compact: Boolean,
    progressTag: String,
    gaugeSlot: String,
) {
    val validValue = value?.takeIf(Float::isFinite)
    val normalized = scale.normalized(validValue)?.takeIf(Float::isFinite)
    val animatedProgress = rememberAnimatedGaugeProgress(normalized ?: 0f)
    val colors = MaterialTheme.colorScheme
    val context = androidx.compose.ui.platform.LocalContext.current
    val resources = androidx.compose.ui.platform.LocalResources.current
    val activeColor = when {
        validValue == null -> colors.onSurfaceVariant
        else -> scale.sectionFor(validValue)?.let { Color(resources.getColor(it.color, context.theme)) }
            ?: colors.primary
    }
    val trackColor = if (colors.background.luminance() > 0.5f) {
        lerp(colors.outline, colors.background, 0.18f)
    } else {
        colors.surfaceVariant
    }
    val formattedValue = validValue?.let { catalog.formatValue(scale.type, it) } ?: "—"

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(
            modifier = Modifier
                .fillMaxHeight()
                .aspectRatio(1.22f)
                .testTag(progressTag)
        ) {
            val drawnProgress = animatedProgress()
            val diameter = min(size.width, size.height) * 0.88f
            val strokeWidth = diameter * 0.055f
            val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
            val arcSize = Size(diameter, diameter)
            drawArc(
                color = trackColor,
                startAngle = GaugeScale.START_ANGLE_DEGREES,
                sweepAngle = GaugeScale.SWEEP_ANGLE_DEGREES,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )
            drawArc(
                color = activeColor,
                startAngle = GaugeScale.START_ANGLE_DEGREES,
                sweepAngle = GaugeScale.SWEEP_ANGLE_DEGREES * drawnProgress,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )

            val center = Offset(size.width / 2f, size.height / 2f)
            val outerRadius = diameter / 2f
            val innerRadius = outerRadius - strokeWidth * 1.9f
            repeat(DASHBOARD_DECORATIVE_TICK_COUNT) { index ->
                val fraction = index.toFloat() / (DASHBOARD_DECORATIVE_TICK_COUNT - 1).toFloat()
                val angleRad = (GaugeScale.START_ANGLE_DEGREES + GaugeScale.SWEEP_ANGLE_DEGREES * fraction) *
                    PI.toFloat() / 180f
                val tickColor = if (fraction <= drawnProgress) activeColor else colors.onSurfaceVariant.copy(alpha = 0.38f)
                drawLine(
                    color = tickColor,
                    start = Offset(center.x + cos(angleRad) * innerRadius, center.y + sin(angleRad) * innerRadius),
                    end = Offset(center.x + cos(angleRad) * outerRadius, center.y + sin(angleRad) * outerRadius),
                    strokeWidth = strokeWidth * 0.16f,
                    cap = StrokeCap.Round,
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = formattedValue,
                color = colors.onSurface,
                fontSize = if (compact) 48.sp else 68.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.testTag("dashboard-gauge-value-$gaugeSlot"),
            )
            Text(
                text = unit,
                color = colors.onSurfaceVariant,
                fontSize = if (compact) 15.sp else 18.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.testTag("dashboard-gauge-unit-$gaugeSlot"),
            )
        }
        Text(
            text = title,
            color = colors.onSurfaceVariant,
            fontSize = if (compact) 11.sp else 12.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 32.dp, vertical = 4.dp)
                .testTag("dashboard-gauge-title-$gaugeSlot"),
        )
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
