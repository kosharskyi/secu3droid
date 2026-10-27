package org.secu3.android.utils

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.secu3.android.R
import org.secu3.android.models.InjectionTimingMode
import org.secu3.android.models.SensorGaugeStyle
import java.util.UUID

class UserPrefsTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun defaultsAndExplicitValuesAreReadFromOneSettingsSnapshot() = runBlocking {
        val fixture = IsolatedUserPrefs(context, UUID.randomUUID().toString())
        try {
            val prefs = fixture.prefs
            val initial = prefs.settings.first()
            assertEquals(LogExportDestination.DefaultDownloads, initial.logExportDestination)
            assertEquals(";", initial.csvDelimiter)
            assertEquals(SensorGaugeStyle.PreciseArc, initial.sensorGaugeStyle)
            assertEquals(InjectionTimingMode.Default, initial.injectionTimingMode)
            prefs.setSensorLoggerEnabled(true)
            prefs.setCsvDelimiter("|")
            val updated = prefs.settings.first()
            assertTrue(updated.isSensorLoggerEnabled)
            assertEquals("|", updated.csvDelimiter)
        } finally {
            fixture.close()
        }
    }

    @Test
    fun bluetoothIdentityAndRetryValidationUseAtomicExplicitWrites() = runBlocking {
        val fixture = IsolatedUserPrefs(context, UUID.randomUUID().toString())
        try {
            val prefs = fixture.prefs
            prefs.setBluetoothDevice("00:11:22:33:44:55", "ECU")
            val selected = prefs.settings.first()
            assertEquals("00:11:22:33:44:55", selected.bluetoothDeviceAddress)
            assertEquals("ECU", selected.bluetoothDeviceName)
            prefs.setBluetoothDevice(null, null)
            val cleared = prefs.settings.first()
            assertNull(cleared.bluetoothDeviceAddress)
            assertNull(cleared.bluetoothDeviceName)
            assertFalse(prefs.saveConnectionRetries("0"))
            assertFalse(prefs.saveConnectionRetries("bad"))
            assertTrue(prefs.saveConnectionRetries("7"))
            assertEquals("7", prefs.settings.first().connectionRetriesText)
        } finally {
            fixture.close()
        }
    }

    @Test
    fun retryValidationRejectsInvalidTextAndCanonicalizesPositiveIntegers() = runBlocking {
        val fixture = IsolatedUserPrefs(context, UUID.randomUUID().toString())
        try {
            val prefs = fixture.prefs
            val original = prefs.settings.first().connectionRetriesText
            listOf("", "-1", "0", "2147483648", "999999999999999999999999").forEach { invalid ->
                assertFalse("$invalid must be rejected", prefs.saveConnectionRetries(invalid))
                assertEquals(original, prefs.settings.first().connectionRetriesText)
            }
            assertTrue(prefs.saveConnectionRetries("009"))
            assertEquals("9", prefs.settings.first().connectionRetriesText)
            assertEquals(9, prefs.settings.first().connectionRetries)
        } finally {
            fixture.close()
        }
    }

    @Test
    fun exportDestinationCanBeSelectedAndCleared() = runBlocking {
        val fixture = IsolatedUserPrefs(context, UUID.randomUUID().toString())
        try {
            val prefs = fixture.prefs
            val uri = "content://com.android.externalstorage.documents/tree/primary%3ASecu3"
            prefs.setLogExportDirectoryUri(uri)
            assertEquals(LogExportDestination.CustomTree(uri), prefs.settings.first().logExportDestination)
            assertEquals(uri, prefs.settings.first().logExportDirectoryUri)
            prefs.setLogExportDirectoryUri(null)
            assertEquals(LogExportDestination.DefaultDownloads, prefs.settings.first().logExportDestination)
            assertNull(prefs.settings.first().logExportDirectoryUri)
        } finally {
            fixture.close()
        }
    }

    @Test
    fun retiredColumnKeyIsNotPartOfSettingsOrWrites() = runBlocking {
        val fixture = IsolatedUserPrefs(context, UUID.randomUUID().toString())
        try {
            val retiredKey = stringPreferencesKey("columns_count")
            fixture.dataStore.edit { it[retiredKey] = "3" }
            fixture.prefs.setOldSensorViewEnabled(true)
            assertEquals("3", fixture.dataStore.data.first()[retiredKey])
            assertTrue(fixture.prefs.settings.first().oldSensorViewEnabled)
        } finally {
            fixture.close()
        }
    }
}
