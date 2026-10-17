package org.secu3.android.utils

/** User-selected appearance. System follows the Android system setting. */
enum class AppearanceMode(val preferenceValue: String) {
    System("system"),
    Light("light"),
    Dark("dark");

    companion object {
        fun fromPreference(value: String?): AppearanceMode =
            entries.firstOrNull { it.preferenceValue == value } ?: System
    }
}
