package org.secu3.android.ui.sensors.models

import org.secu3.android.models.sensors.GaugeType
import java.text.DecimalFormatSymbols
import java.util.Locale
import org.secu3.android.models.InjectionTimingMode

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
internal class GaugeScale(
    val type: GaugeType,
    private val presentation: GaugePresentation,
    val injectionTimingMode: InjectionTimingMode = InjectionTimingMode.Default,
) {
    val minValue: Float = if (type == GaugeType.BEGIN_INJ_PHASE || type == GaugeType.END_INJ_PHASE) {
        injectionTimingMode.minValue
    } else {
        presentation.minValue
    }
    val maxValue: Float = if (type == GaugeType.BEGIN_INJ_PHASE || type == GaugeType.END_INJ_PHASE) {
        injectionTimingMode.maxValue
    } else {
        presentation.maxValue
    }
    val sections: List<GaugeSection> = presentation.sections
    val ticks: List<GaugeTick> = (0 until presentation.tickCount).map { index ->
        val fraction = if (presentation.tickCount <= 1) 0f else index.toFloat() / (presentation.tickCount - 1)
        GaugeTick(
            index = index,
            fraction = fraction,
            value = minValue + (maxValue - minValue) * fraction,
            angleDegrees = START_ANGLE_DEGREES + SWEEP_ANGLE_DEGREES * fraction,
        )
    }

    fun normalized(value: Float?): Float? = value?.let {
        ((it - minValue) / (maxValue - minValue)).coerceIn(0f, 1f)
    }

    fun angleAt(fraction: Float): Float = START_ANGLE_DEGREES + SWEEP_ANGLE_DEGREES * fraction

    fun sweepFor(fraction: Float): Float = SWEEP_ANGLE_DEGREES * fraction

    fun format(tick: GaugeTick): String = formatValue(tick.value)

    fun formatValue(value: Float): String {
        if (!value.isFinite()) return "—"
        val decimalSeparator = DecimalFormatSymbols.getInstance(Locale.getDefault()).decimalSeparator
        val formatted = String.format(Locale.getDefault(), "%.3f", value)
            .trimEnd('0')
            .trimEnd(decimalSeparator)
        return if (formatted == "-0") "0" else formatted
    }

    companion object {
        const val START_ANGLE_DEGREES = 135f
        const val SWEEP_ANGLE_DEGREES = 270f
    }
}
