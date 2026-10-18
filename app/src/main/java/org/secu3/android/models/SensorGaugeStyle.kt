package org.secu3.android.models

/** Persisted globally; changing dial style never changes a gauge's numeric-view setting. */
enum class SensorGaugeStyle(val preferenceValue: String) {
    Current("current"),
    PreciseArc("precise_arc"),
    ThinNeedle("thin_needle");

    companion object {
        val Default: SensorGaugeStyle = PreciseArc

        fun fromPreference(value: String?): SensorGaugeStyle = entries.firstOrNull { it.preferenceValue == value } ?: Default
    }
}
