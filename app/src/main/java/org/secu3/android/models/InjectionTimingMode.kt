package org.secu3.android.models

/** Display convention for the ECU's 0...720 degree injection timing values. */
enum class InjectionTimingMode(
    val preferenceValue: String,
    val minValue: Float,
    val maxValue: Float,
) {
    Btdc720("btdc_720", 0f, 720f),
    Atdc720("atdc_720", 0f, 720f),
    Btdc360("btdc_360", -360f, 360f),
    Atdc360("atdc_360", -360f, 360f);

    /** Converts the ECU phase into the selected display convention, matching desktop behavior. */
    fun transform(source: Float): Float {
        if (!source.isFinite()) return source
        if (source > 720f) {
            val wrapped = source - 720f
            return when (this) {
                Btdc720 -> wrapped
                Atdc720 -> 720f - wrapped
                Btdc360 -> wrapped
                Atdc360 -> -wrapped
            }.let { if (it == 0f) 0f else it }
        }
        val transformed = when (this) {
            Btdc720 -> source
            Atdc720 -> 720f - source
            Btdc360 -> if (source < 360f) source else -(720f - source)
            Atdc360 -> -(if (source < 360f) source else -(720f - source))
        }
        return if (transformed == 0f) 0f else transformed
    }

    companion object {
        val Default: InjectionTimingMode = Btdc720

        fun fromPreference(value: String?): InjectionTimingMode =
            entries.firstOrNull { it.preferenceValue == value } ?: Default
    }
}
