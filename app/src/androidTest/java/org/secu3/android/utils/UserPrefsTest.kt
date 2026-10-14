package org.secu3.android.utils

import androidx.core.content.edit
import androidx.test.platform.app.InstrumentationRegistry
import androidx.lifecycle.ViewModelStore
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.secu3.android.R
import org.secu3.android.ui.sensors.settings.SensorsSettingsViewModel
import org.secu3.android.ui.settings.SettingsViewModel

class UserPrefsTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val sharedPreferences = context.getDefaultSharedPreferences()
    private val logExportDirectoryKey = context.getString(R.string.pref_log_export_directory_key)

    @Before
    fun setUp() {
        clearLogExportDirectory()
    }

    @After
    fun tearDown() {
        clearLogExportDirectory()
    }

    @Test
    fun logExportDestinationDefaultsToDownloadsWhenNoFolderIsSelected() {
        val userPrefs = UserPrefs(context)

        assertEquals(LogExportDestination.DefaultDownloads, userPrefs.logExportDestination)
        assertNull(userPrefs.logExportDirectoryUri)
    }

    @Test
    fun logExportDestinationUsesCustomTreeWhenFolderIsSelected() {
        val uri = "content://com.android.externalstorage.documents/tree/primary%3ASecu3"
        val userPrefs = UserPrefs(context)

        userPrefs.logExportDirectoryUri = uri

        assertEquals(LogExportDestination.CustomTree(uri), userPrefs.logExportDestination)
        assertEquals(uri, userPrefs.logExportDirectoryUri)
    }

    @Test
    fun defaultPreferencesReadAndWriteTheLegacyPackageNamedStore() {
        val legacyStore = context.getSharedPreferences("${context.packageName}_preferences", android.content.Context.MODE_PRIVATE)
        val logKey = context.getString(R.string.pref_write_log_key)
        val delimiterKey = context.getString(R.string.pref_log_csv_delimeter_key)
        val retriesKey = context.getString(R.string.pref_connection_retries_key)
        val columnsKey = "columns_count"
        val oldViewKey = "old_sensors_view"
        val keys = listOf(logKey, delimiterKey, retriesKey, columnsKey, oldViewKey)
        val previous = keys.associateWith { key -> if (legacyStore.contains(key)) legacyStore.all[key] else null }
        val viewModelStore = ViewModelStore()
        try {
            legacyStore.edit()
                .putBoolean(logKey, true)
                .putString(delimiterKey, "|")
                .putString(retriesKey, "7")
                .putInt(columnsKey, 5)
                .putBoolean(oldViewKey, false)
                .commit()

            val userPrefs = UserPrefs(context)
            assertTrue(userPrefs.isSensorLoggerEnabled)
            assertEquals("|", userPrefs.CSVDelimeter)
            assertEquals(7, userPrefs.connectionRetries)
            assertEquals(5, userPrefs.columnsCount)

            userPrefs.isSensorLoggerEnabled = false
            assertFalse(legacyStore.getBoolean(logKey, true))

            val settings = SettingsViewModel(context)
            val sensors = SensorsSettingsViewModel(UserPrefs(context), context)
            viewModelStore.put("settings", settings)
            viewModelStore.put("sensors", sensors)
            assertTrue(settings.state.value.writeLog.not())
            assertEquals("|", settings.state.value.csvDelimiter)
            assertEquals("7", settings.state.value.connectionRetries)
            assertEquals(5, sensors.state.value.columns)

            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                legacyStore.edit().putBoolean(logKey, true).putBoolean(oldViewKey, true).putInt(columnsKey, 6).commit()
            }
            assertTrue(settings.state.value.writeLog)
            assertTrue(sensors.state.value.oldView)
            assertEquals(6, sensors.state.value.columns)
        } finally {
            legacyStore.edit().apply {
                keys.forEach(::remove)
                previous.forEach { (key, value) ->
                    when (value) {
                        is Boolean -> putBoolean(key, value)
                        is String -> putString(key, value)
                        is Int -> putInt(key, value)
                    }
                }
            }.commit()
            InstrumentationRegistry.getInstrumentation().runOnMainSync { viewModelStore.clear() }
        }
    }

    private fun clearLogExportDirectory() {
        sharedPreferences.edit { remove(logExportDirectoryKey) }
    }
}
