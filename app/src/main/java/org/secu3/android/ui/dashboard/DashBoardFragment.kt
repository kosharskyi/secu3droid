/*
 *    SecuDroid  - An open source, free manager for SECU-3 engine control unit
 *    Copyright (C) 2024 Vitaliy O. Kosharskyi. Ukraine, Kyiv
 *
 *    SECU-3  - An open source, free engine control unit
 *    Copyright (C) 2007-2024 Alexey A. Shabelnikov. Ukraine, Kyiv
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
 *
 *    contacts:
 *                    http://secu-3.org
 *                    email: vetalkosharskiy@gmail.com
 */
package org.secu3.android.ui.dashboard

import android.content.Context
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import org.secu3.android.R
import org.secu3.android.connection.Connected
import org.secu3.android.connection.ConnectionState
import org.secu3.android.connection.Disconnected
import org.secu3.android.connection.InProgress
import org.secu3.android.utils.Task
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

@AndroidEntryPoint
class DashBoardFragment : Fragment() {

    private val mViewModel: DashboardViewModel by viewModels()
    private var wasKeepScreenOnEnabled = false

    override fun onAttach(context: Context) {
        super.onAttach(context)
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setContent {
                val data by mViewModel.packetLiveData.observeAsState()
                val status by mViewModel.statusLiveData.observeAsState()

                DashboardScreen(
                    data = data,
                    connectionState = status,
                )
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (mViewModel.isBluetoothDeviceAddressNotSelected()) {
            Toast.makeText(context, getString(R.string.choose_bluetooth_adapter), Toast.LENGTH_LONG).show()
        }
    }

    @Synchronized
    override fun onResume() {
        super.onResume()
        keepScreenOn()
        enterImmersiveMode()
        mViewModel.setTask(Task.Secu3ReadSensors)
    }

    override fun onPause() {
        super.onPause()
        exitImmersiveMode()
        restoreKeepScreenOn()
    }

    override fun onDestroy() {
        super.onDestroy()
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }

    private fun enterImmersiveMode() {
        val window = requireActivity().window
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.systemBars())
        }
    }

    private fun exitImmersiveMode() {
        val window = requireActivity().window
        WindowInsetsControllerCompat(window, window.decorView).show(WindowInsetsCompat.Type.systemBars())
        WindowCompat.setDecorFitsSystemWindows(window, true)
    }

    private fun keepScreenOn() {
        val window = requireActivity().window
        wasKeepScreenOnEnabled = window.attributes.flags and WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON != 0
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun restoreKeepScreenOn() {
        if (wasKeepScreenOnEnabled.not() && mViewModel.isKeepScreenAliveActive.not()) {
            requireActivity().window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    @Composable
    private fun DashboardScreen(
        data: DashboardViewData?,
        connectionState: ConnectionState?,
    ) {
        SideEffect {
            WindowInsetsControllerCompat(requireActivity().window, requireView()).isAppearanceLightStatusBars = false
        }

        DashboardContent(
            data = data,
            connectionState = connectionState,
        )
    }

    @Composable
    private fun DashboardContent(
        data: DashboardViewData?,
        connectionState: ConnectionState?,
    ) {
        MaterialTheme {
            Surface(color = DashboardColors.Background) {
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(DashboardColors.Background)
                        .padding(horizontal = 24.dp, vertical = 14.dp)
                ) {
                    val compact = maxHeight < 380.dp
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        DashboardStatusBar(
                            data = data,
                            connectionState = connectionState,
                            compact = compact,
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(28.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ArcGauge(
                                value = data?.rpm ?: 0f,
                                minValue = 0f,
                                maxValue = 8000f,
                                unit = stringResource(R.string.units_rpm),
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                compact = compact,
                                cautionAt = 0.72f,
                                criticalAt = 0.88f,
                            )

                            ArcGauge(
                                value = data?.speed ?: 0f,
                                minValue = 0f,
                                maxValue = 220f,
                                unit = stringResource(R.string.units_km_h),
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                compact = compact,
                                cautionAt = 0.65f,
                                criticalAt = 0.85f,
                            )
                        }

                        SecondaryReadouts(
                            data = data,
                            compact = compact,
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
        backgroundColor = 0xFF06090D,
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
        backgroundColor = 0xFF06090D,
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
        backgroundColor = 0xFF06090D,
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
            Connected -> stringResource(R.string.dashboard_status_online)
            InProgress -> stringResource(R.string.dashboard_status_connecting)
            Disconnected -> stringResource(R.string.dashboard_status_offline)
            else -> stringResource(R.string.dashboard_status_no_data)
        }
        val statusColor = when (connectionState) {
            Connected -> DashboardColors.Normal
            InProgress -> DashboardColors.Caution
            Disconnected -> DashboardColors.Critical
            else -> DashboardColors.Muted
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatusChip(statusText, statusColor, compact)

            if (data?.ledCheckEngine == true) {
                StatusChip(stringResource(R.string.dashboard_warning_check_engine), DashboardColors.Critical, compact)
            }
            if ((data?.temperature ?: 0f) >= 105f) {
                StatusChip(stringResource(R.string.dashboard_warning_overheat), DashboardColors.Critical, compact)
            } else if ((data?.temperature ?: 0f) >= 100f) {
                StatusChip(stringResource(R.string.dashboard_warning_hot), DashboardColors.Caution, compact)
            }
            if ((data?.voltage ?: 14f) < 11.8f) {
                StatusChip(stringResource(R.string.dashboard_warning_low_voltage), DashboardColors.Critical, compact)
            } else if ((data?.voltage ?: 14f) < 12.3f) {
                StatusChip(stringResource(R.string.dashboard_warning_voltage), DashboardColors.Caution, compact)
            }
            if ((data?.oilTemperature ?: 0f) >= 130f) {
                StatusChip(stringResource(R.string.dashboard_warning_oil_temp), DashboardColors.Critical, compact)
            } else if ((data?.oilTemperature ?: 0f) >= 115f) {
                StatusChip(stringResource(R.string.dashboard_warning_oil_hot), DashboardColors.Caution, compact)
            }
            if (data?.ledRevLimit == true) {
                StatusChip(stringResource(R.string.dashboard_warning_rev_limit), DashboardColors.Critical, compact)
            }
            if ((data?.knockRetard ?: 0f) > 0f) {
                StatusChip(stringResource(R.string.dashboard_warning_knock), DashboardColors.Critical, compact)
            }
            if (data?.ledFan == true) {
                StatusChip(stringResource(R.string.dashboard_indicator_fan), DashboardColors.Caution, compact)
            }
            if (data?.ledGasoline == true) {
                StatusChip(stringResource(R.string.dashboard_indicator_gas), DashboardColors.Normal, compact)
            }
            if (data?.ledEco == true) {
                StatusChip(stringResource(R.string.dashboard_indicator_eco), DashboardColors.Normal, compact)
            }
            if (data?.ledPower == true) {
                StatusChip(stringResource(R.string.dashboard_indicator_power), DashboardColors.Caution, compact)
            }
            if (data?.ledChoke == true) {
                StatusChip(stringResource(R.string.dashboard_indicator_choke), DashboardColors.Caution, compact)
            }
        }
    }

    @Composable
    private fun StatusChip(text: String, color: Color, compact: Boolean) {
        Text(
            text = text,
            color = color,
            fontSize = if (compact) 12.sp else 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(color.copy(alpha = 0.16f))
                .padding(horizontal = 10.dp, vertical = if (compact) 4.dp else 6.dp)
        )
    }

    @Composable
    private fun ArcGauge(
        value: Float,
        minValue: Float,
        maxValue: Float,
        unit: String,
        modifier: Modifier = Modifier,
        compact: Boolean,
        cautionAt: Float,
        criticalAt: Float,
    ) {
        val normalized = ((value - minValue) / (maxValue - minValue)).coerceIn(0f, 1f)
        val valueColor = when {
            normalized >= criticalAt -> DashboardColors.Critical
            normalized >= cautionAt -> DashboardColors.Caution
            else -> DashboardColors.Normal
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
                    color = DashboardColors.Track,
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
                    val tickColor = if (fraction <= normalized) valueColor else DashboardColors.Tick
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
                    text = value.toInt().toString(),
                    color = DashboardColors.Value,
                    fontSize = if (compact) 48.sp else 68.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = unit,
                    color = valueColor,
                    fontSize = if (compact) 15.sp else 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }

    @Composable
    private fun SecondaryReadouts(
        data: DashboardViewData?,
        compact: Boolean,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ReadoutTile(stringResource(R.string.dashboard_readout_clt), data?.temperature, stringResource(R.string.units_degrees_celcius), false, compact, Modifier.weight(1f))
            ReadoutTile(stringResource(R.string.dashboard_readout_voltage), data?.voltage, stringResource(R.string.units_volts), true, compact, Modifier.weight(1f))
            ReadoutTile(stringResource(R.string.dashboard_readout_load), data?.load, stringResource(R.string.units_percents), false, compact, Modifier.weight(1f))
            ReadoutTile(stringResource(R.string.dashboard_readout_tps), data?.tps, stringResource(R.string.units_percents), false, compact, Modifier.weight(1f))
            val optionalReadout = optionalDailyReadout(data)
            ReadoutTile(
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
            modifier = modifier
                .clip(RoundedCornerShape(8.dp))
                .background(DashboardColors.Panel)
                .padding(horizontal = 12.dp, vertical = if (compact) 7.dp else 10.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = label,
                    color = DashboardColors.Label,
                    fontSize = if (compact) 11.sp else 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = unit,
                    color = DashboardColors.Muted,
                    fontSize = if (compact) 10.sp else 12.sp
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = formattedValue,
                color = DashboardColors.Value,
                fontSize = if (compact) 24.sp else 31.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }

    private object DashboardColors {
        val Background = Color(0xFF06090D)
        val Panel = Color(0xFF121821)
        val Track = Color(0xFF27313D)
        val Tick = Color(0xFF4B5663)
        val Value = Color(0xFFEAF2F8)
        val Label = Color(0xFF93A3B5)
        val Muted = Color(0xFF647386)
        val Normal = Color(0xFF45D483)
        val Caution = Color(0xFFFFC857)
        val Critical = Color(0xFFFF4D5A)
    }
}
