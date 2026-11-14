package org.secu3.android.utils.prefs

import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test
import java.util.UUID

class ParameterSettingsPrefsTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun defaultsAndSavedDensityPairAreCoherentAndValidated(): Unit = runBlocking {
        val fixture = IsolatedUserPrefs(context, UUID.randomUUID().toString())
        val storeJob = SupervisorJob()
        val prefs = ParameterSettingsPrefs(fixture.dataStore, CoroutineScope(storeJob + Dispatchers.IO))
        try {
            assertEquals(
                FuelDensities(.710f, .536f),
                withTimeout(5_000) { prefs.settings.first { it != null } },
            )
            assertEquals(.72f, prefs.parseDensity("0,72"))
            assertEquals(.72f, prefs.parseDensity("0.72"))
            assertNull(prefs.parseDensity("0.19"))
            assertNull(prefs.parseDensity("2.01"))

            prefs.save(.72f, .54f)
            assertEquals(FuelDensities(.72f, .54f), prefs.settings.value)
            assertThrows(IllegalArgumentException::class.java) { runBlocking { prefs.save(.1f, .54f) } }
        } finally {
            storeJob.cancelAndJoin()
            fixture.close()
        }
    }
}
