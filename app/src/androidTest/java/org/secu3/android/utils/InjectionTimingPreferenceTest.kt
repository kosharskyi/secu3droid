package org.secu3.android.utils

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.secu3.android.models.InjectionTimingMode
import java.util.UUID

class InjectionTimingPreferenceTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun unknownTimingValueRemainsStoredAndSetterPersistsCanonicalValue() = runBlocking {
        val fixture = IsolatedUserPrefs(context, UUID.randomUUID().toString())
        try {
            val key = stringPreferencesKey(UserPrefs.INJECTION_TIMING_MODE_KEY)
            fixture.dataStore.edit { it[key] = "future_timing" }
            assertEquals(InjectionTimingMode.Default, fixture.prefs.settings.first().injectionTimingMode)
            assertEquals("future_timing", fixture.dataStore.data.first()[key])
            fixture.prefs.setInjectionTimingMode(InjectionTimingMode.Atdc360)
            assertEquals("atdc_360", fixture.dataStore.data.first()[key])
        } finally {
            fixture.close()
        }
    }
}
