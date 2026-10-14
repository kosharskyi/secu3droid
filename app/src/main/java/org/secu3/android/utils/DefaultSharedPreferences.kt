package org.secu3.android.utils

import android.content.Context
import android.content.SharedPreferences

/** Returns the same store used by AndroidX PreferenceManager.getDefaultSharedPreferences. */
fun Context.getDefaultSharedPreferences(): SharedPreferences =
    getSharedPreferences("${packageName}_preferences", Context.MODE_PRIVATE)
