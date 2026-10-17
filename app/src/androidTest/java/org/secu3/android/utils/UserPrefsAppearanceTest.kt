package org.secu3.android.utils

import androidx.core.content.edit
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import org.junit.Assert.assertEquals
import org.junit.Test
import org.secu3.android.R

class UserPrefsAppearanceTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val rawPreferences = context.getDefaultSharedPreferences()
    private val modeKey = context.getString(R.string.pref_appearance_mode_key)
    private val legacyKey = context.getString(R.string.pref_night_mode_key)

    @Test
    fun appearancePersistsAcrossUserPrefsRecreationAndLegacyDarkIsRead() = withIsolatedKeys {
        val prefs = UserPrefs(context)
        assertEquals(AppearanceMode.System, prefs.appearanceMode)
        prefs.appearanceMode = AppearanceMode.Dark
        assertEquals(AppearanceMode.Dark, UserPrefs(context).appearanceMode)

        rawPreferences.edit {
            remove(modeKey)
            putBoolean(legacyKey, true)
        }
        assertEquals(AppearanceMode.Dark, UserPrefs(context).appearanceMode)
    }

    @Test
    fun appearanceFlowReflectsExternalPreferenceEdits() = withIsolatedKeys {
        val prefs = UserPrefs(context)
        val values = mutableListOf<AppearanceMode>()
        runBlocking {
            val initial = CompletableDeferred<AppearanceMode>()
            val collector = launch(start = CoroutineStart.UNDISPATCHED) {
                prefs.appearanceModeFlow.onEach { initial.complete(it) }.take(2).toList(values)
            }
            assertEquals(AppearanceMode.System, withTimeout(5_000) { initial.await() })
            rawPreferences.edit { putString(modeKey, AppearanceMode.Light.preferenceValue) }
            withTimeout(5_000) { collector.join() }
        }
        assertEquals(listOf(AppearanceMode.System, AppearanceMode.Light), values)
    }

    private fun withIsolatedKeys(block: () -> Unit) {
        val hadMode = rawPreferences.contains(modeKey)
        val previousMode = rawPreferences.getString(modeKey, null)
        val hadLegacy = rawPreferences.contains(legacyKey)
        val previousLegacy = rawPreferences.getBoolean(legacyKey, false)
        try {
            rawPreferences.edit { remove(modeKey); remove(legacyKey) }
            block()
        } finally {
            rawPreferences.edit {
                if (hadMode) putString(modeKey, previousMode) else remove(modeKey)
                if (hadLegacy) putBoolean(legacyKey, previousLegacy) else remove(legacyKey)
            }
        }
    }
}
