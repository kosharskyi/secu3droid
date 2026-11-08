package org.secu3.android.models

/** Independent presentation choices for the Dashboard's two primary gauges. */
enum class DashboardGaugeStyle(val preferenceValue: String) {
    Classic("classic"),
    PreciseArc("precise_arc"),
    ThinNeedle("thin_needle"),
    FilledArc("filled_arc"),
    TriangularNeedle("triangular_needle");

    companion object {
        val Default = FilledArc

        fun fromPreference(value: String?): DashboardGaugeStyle =
            entries.firstOrNull { it.preferenceValue == value } ?: Default
    }
}
