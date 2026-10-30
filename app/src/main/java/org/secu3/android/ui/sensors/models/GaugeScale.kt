package org.secu3.android.ui.sensors.models

import org.secu3.android.models.sensors.GaugeType
import java.util.Locale
import kotlin.math.roundToInt
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
    val majorLabelTicks: List<GaugeTick> = if (type == GaugeType.RPM) {
        ticks.filter { it.index % 2 == 0 }
    } else {
        ticks
    }

    fun normalized(value: Float?): Float? = value?.let {
        ((it - minValue) / (maxValue - minValue)).coerceIn(0f, 1f)
    }

    fun angleAt(fraction: Float): Float = START_ANGLE_DEGREES + SWEEP_ANGLE_DEGREES * fraction

    fun sweepFor(fraction: Float): Float = SWEEP_ANGLE_DEGREES * fraction

    fun format(tick: GaugeTick): String = if (type == GaugeType.RPM) {
        formatValue(tick.value / RPM_TICK_DIVISOR)
    } else {
        formatValue(tick.value)
    }

    val multiplierLabel: String? get() = if (type == GaugeType.RPM) RPM_MULTIPLIER_LABEL else null

    fun formatValue(value: Float): String {
        if (!value.isFinite()) return "—"
        return String.format(Locale.getDefault(), "%d", value.roundToInt())
    }

    companion object {
        const val START_ANGLE_DEGREES = 135f
        const val SWEEP_ANGLE_DEGREES = 270f
        private const val RPM_TICK_DIVISOR = 100f
        const val RPM_MULTIPLIER_LABEL = "×100"
    }
}
