package org.secu3.android.utils

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.secu3.android.R
import java.util.UUID

class UserPrefsAppearanceTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun legacyBooleanAppliesOnlyWhenExplicitAppearanceKeyIsMissing() = runBlocking {
        val fixture = IsolatedUserPrefs(context, UUID.randomUUID().toString())
        try {
            val appearanceKey = stringPreferencesKey(context.getString(R.string.pref_appearance_mode_key))
            val legacyKey = booleanPreferencesKey(context.getString(R.string.pref_night_mode_key))
            fixture.dataStore.edit { it[legacyKey] = false }
            assertEquals(AppearanceMode.Light, fixture.prefs.settings.first().appearanceMode)
            fixture.dataStore.edit {
                it[legacyKey] = true
                it[appearanceKey] = "future_theme"
            }
            val settings = fixture.prefs.settings.first()
            assertEquals(AppearanceMode.System, settings.appearanceMode)
            assertEquals("future_theme", fixture.dataStore.data.first()[appearanceKey])
            fixture.dataStore.edit { it.remove(appearanceKey) }
            assertEquals(AppearanceMode.Dark, fixture.prefs.settings.first().appearanceMode)
        } finally {
            fixture.close()
        }
    }

    @Test
    fun appearanceWritesAreObservedAsSettingsSnapshots() = runBlocking {
        val fixture = IsolatedUserPrefs(context, UUID.randomUUID().toString())
        try {
            assertEquals(AppearanceMode.System, fixture.prefs.settings.first().appearanceMode)
            fixture.prefs.setAppearanceMode(AppearanceMode.Dark)
            assertEquals(AppearanceMode.Dark, fixture.prefs.settings.first().appearanceMode)
        } finally {
            fixture.close()
        }
    }
}
