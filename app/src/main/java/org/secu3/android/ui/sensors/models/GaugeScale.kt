package org.secu3.android.ui.sensors.models

import java.util.Locale
import kotlin.math.abs
import kotlin.math.round

/** A configured position on a sensor gauge's shared scale. */
internal data class GaugeTick(
    val index: Int,
    val fraction: Float,
    val value: Float,
    val angleDegrees: Float,
)

/**
 * Shared scale semantics derived only from [GaugeType]. Renderers may change tick geometry and label
 * placement, but they must use these same configured tick values, angles, sections, and normalization.
 */
internal class GaugeScale(val type: GaugeType) {
    val sections: List<GaugeSection> = type.sections
    val ticks: List<GaugeTick> = (0 until type.tickCount).map { index ->
        val fraction = if (type.tickCount <= 1) 0f else index.toFloat() / (type.tickCount - 1)
        GaugeTick(
            index = index,
            fraction = fraction,
            value = type.minValue + (type.maxValue - type.minValue) * fraction,
            angleDegrees = START_ANGLE_DEGREES + SWEEP_ANGLE_DEGREES * fraction,
        )
    }

    fun normalized(value: Float?): Float? = value?.let {
        ((it - type.minValue) / (type.maxValue - type.minValue)).coerceIn(0f, 1f)
    }

    fun angleAt(fraction: Float): Float = START_ANGLE_DEGREES + SWEEP_ANGLE_DEGREES * fraction

    fun sweepFor(fraction: Float): Float = SWEEP_ANGLE_DEGREES * fraction

    fun format(tick: GaugeTick): String = formatValue(tick.value)

    companion object {
        const val START_ANGLE_DEGREES = 135f
        const val SWEEP_ANGLE_DEGREES = 270f

        fun formatValue(value: Float): String {
            val rounded = round(value)
            val format = if (abs(value - rounded) < .001f) "%.0f" else "%.1f"
            return String.format(Locale.getDefault(), format, value)
        }
    }
}
