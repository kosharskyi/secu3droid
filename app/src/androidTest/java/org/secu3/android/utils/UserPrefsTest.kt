package org.secu3.android.utils

import androidx.core.content.edit
import android.content.ContextWrapper
import android.content.SharedPreferences
import androidx.test.platform.app.InstrumentationRegistry
import androidx.lifecycle.ViewModelStore
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.secu3.android.R
import org.secu3.android.models.SensorGaugeStyle
import org.secu3.android.ui.sensors.settings.SensorsSettingsViewModel
import org.secu3.android.ui.settings.SettingsViewModel

class UserPrefsTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val sharedPreferences = context.getSharedPreferences("${context.packageName}_preferences", android.content.Context.MODE_PRIVATE)
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
    fun bluetoothAddressPersistsSeparatelyAndLegacyNameDoesNotBecomeAnAddress() {
        val nameKey = context.getString(R.string.pref_bluetooth_device_key)
        val addressKey = context.getString(R.string.pref_bluetooth_device_address_key)
        val keys = listOf(nameKey, addressKey)
        val previous = keys.associateWith { key -> if (sharedPreferences.contains(key)) sharedPreferences.all[key] else null }
        try {
            sharedPreferences.edit().remove(nameKey).remove(addressKey).commit()
            val prefs = UserPrefs(context)

            prefs.bluetoothDeviceName = "ECU"
            assertNull("legacy name-only installs require reselection", prefs.bluetoothDeviceAddress)

            prefs.bluetoothDeviceAddress = "00:11:22:33:44:55"
            val recreatedPrefs = UserPrefs(context)
            assertEquals("00:11:22:33:44:55", recreatedPrefs.bluetoothDeviceAddress)
            assertEquals("ECU", recreatedPrefs.bluetoothDeviceName)
        } finally {
            sharedPreferences.edit().apply {
                keys.forEach(::remove)
                previous.forEach { (key, value) -> if (value is String) putString(key, value) }
            }.commit()
        }
    }

    @Test
    fun sensorGaugeStyleDefaultsAndUnknownValuesReadAsPreciseArcWithoutRepairingStoredPreference() {
        val key = UserPrefs.SENSOR_GAUGE_STYLE_KEY
        val hadValue = sharedPreferences.contains(key)
        val previous = sharedPreferences.getString(key, null)
        try {
            sharedPreferences.edit().remove(key).commit()
            assertEquals(SensorGaugeStyle.PreciseArc, UserPrefs(context).sensorGaugeStyle)
            assertFalse("reading the default must not create a stored preference", sharedPreferences.contains(key))

            sharedPreferences.edit().putString(key, "unknown-style").commit()
            assertEquals(SensorGaugeStyle.PreciseArc, UserPrefs(context).sensorGaugeStyle)
            assertEquals("unknown-style", sharedPreferences.getString(key, null))

            UserPrefs(context).sensorGaugeStyle = SensorGaugeStyle.Current
            assertEquals(SensorGaugeStyle.Current, UserPrefs(context).sensorGaugeStyle)
            assertEquals("current", sharedPreferences.getString(key, null))

            UserPrefs(context).sensorGaugeStyle = SensorGaugeStyle.ThinNeedle
            assertEquals(SensorGaugeStyle.ThinNeedle, UserPrefs(context).sensorGaugeStyle)
            assertEquals("thin_needle", sharedPreferences.getString(key, null))
        } finally {
            sharedPreferences.edit().apply {
                if (hadValue) putString(key, previous) else remove(key)
            }.commit()
        }
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
            assertEquals(5, legacyStore.getInt(columnsKey, -1))

            userPrefs.isSensorLoggerEnabled = false
            assertFalse(legacyStore.getBoolean(logKey, true))

            val settings = SettingsViewModel(context, userPrefs)
            val sensors = SensorsSettingsViewModel(userPrefs)
            viewModelStore.put("settings", settings)
            viewModelStore.put("sensors", sensors)
            assertTrue(settings.state.value.writeLog.not())
            assertEquals("|", settings.state.value.csvDelimiter)
            assertEquals("7", settings.state.value.connectionRetries)
            assertFalse(sensors.state.value.oldView)

            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                legacyStore.edit().putBoolean(logKey, true).putBoolean(oldViewKey, true).putInt(columnsKey, 6).commit()
            }
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            assertTrue(settings.state.value.writeLog)
            assertTrue(sensors.state.value.oldView)
            assertEquals(6, legacyStore.getInt(columnsKey, -1))
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

    @Test
    fun connectionRetryPreferenceRejectsInvalidValuesAndStoresCanonicalPositiveValues() {
        val key = context.getString(R.string.pref_connection_retries_key)
        val hadValue = sharedPreferences.contains(key)
        val previous = sharedPreferences.getString(key, null)
        try {
            sharedPreferences.edit().putString(key, "7").commit()
            val prefs = UserPrefs(context)
            assertEquals(1, prefs.parsePositiveConnectionRetries("1"))
            assertEquals(250, prefs.parsePositiveConnectionRetries("250"))
            listOf("0", "-1", "2147483648", "").forEach {
                assertNull(prefs.parsePositiveConnectionRetries(it))
                assertFalse(prefs.saveConnectionRetries(it))
                assertEquals("7", sharedPreferences.getString(key, null))
            }
            assertTrue(prefs.saveConnectionRetries("009"))
            assertEquals("9", prefs.connectionRetriesText)
            assertEquals(9, prefs.connectionRetries)
        } finally {
            sharedPreferences.edit().apply {
                if (hadValue) putString(key, previous) else remove(key)
            }.commit()
        }
    }

    @Test
    fun preferenceChangeFlowEmitsInitialAndExternalValuesAndUnregistersOnCancellation() = runBlocking {
        val changedKey = "preference-flow-test"
        val activeListeners = mutableSetOf<SharedPreferences.OnSharedPreferenceChangeListener>()
        val (prefs, rawStore, contextWithTrackedPreferences) = createTrackedUserPrefs(activeListeners)
        val values = mutableListOf<Unit>()
        val initialEmission = CompletableDeferred<Unit>()
        val secondEmission = CompletableDeferred<Unit>()
        var emissionsCollector: kotlinx.coroutines.Job? = null
        var cancellationCollector: kotlinx.coroutines.Job? = null
        var viewModelStore: ViewModelStore? = null
        try {
            val twoEmissionCollector = launch(start = CoroutineStart.UNDISPATCHED) {
                prefs.preferenceChanges
                    .onEach {
                        values += it
                        if (values.size == 1) initialEmission.complete(Unit)
                        if (values.size == 2) secondEmission.complete(Unit)
                    }
                    .take(2)
                    .collect()
            }
            emissionsCollector = twoEmissionCollector
            withTimeout(5_000) { initialEmission.await() }
            assertEquals(1, activeListeners.size)

            rawStore.edit().putBoolean(changedKey, true).commit()
            withTimeout(5_000) { secondEmission.await() }
            withTimeout(5_000) { twoEmissionCollector.join() }
            assertEquals(listOf(Unit, Unit), values)
            assertTrue(activeListeners.isEmpty())

            val cancellationInitial = CompletableDeferred<Unit>()
            val activeCollector = launch(start = CoroutineStart.UNDISPATCHED) {
                prefs.preferenceChanges.onEach { cancellationInitial.complete(Unit) }.collect()
            }
            cancellationCollector = activeCollector
            withTimeout(5_000) { cancellationInitial.await() }
            assertEquals(1, activeListeners.size)
            withTimeout(5_000) { activeCollector.cancelAndJoin() }
            assertTrue(activeListeners.isEmpty())

            val store = ViewModelStore()
            viewModelStore = store
            store.put("settings", SettingsViewModel(contextWithTrackedPreferences, prefs))
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            assertEquals(1, activeListeners.size)
            InstrumentationRegistry.getInstrumentation().runOnMainSync { store.clear() }
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            assertTrue(activeListeners.isEmpty())
        } finally {
            emissionsCollector?.let { withTimeout(5_000) { it.cancelAndJoin() } }
            cancellationCollector?.let { withTimeout(5_000) { it.cancelAndJoin() } }
            viewModelStore?.let { store ->
                InstrumentationRegistry.getInstrumentation().runOnMainSync { store.clear() }
                InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            }
            rawStore.edit().remove(changedKey).commit()
        }
    }

    private fun createTrackedUserPrefs(
        activeListeners: MutableSet<SharedPreferences.OnSharedPreferenceChangeListener>,
    ): Triple<UserPrefs, SharedPreferences, ContextWrapper> {
        val rawStore = context.getSharedPreferences("${context.packageName}_preferences", android.content.Context.MODE_PRIVATE)
        val trackedStore = object : SharedPreferences by rawStore {
            override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
                activeListeners += listener
                rawStore.registerOnSharedPreferenceChangeListener(listener)
            }

            override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
                activeListeners -= listener
                rawStore.unregisterOnSharedPreferenceChangeListener(listener)
            }
        }
        val wrappedContext = object : ContextWrapper(context) {
            override fun getSharedPreferences(name: String, mode: Int): SharedPreferences = trackedStore
        }
        return Triple(UserPrefs(wrappedContext), rawStore, wrappedContext)
    }

    private fun clearLogExportDirectory() {
        sharedPreferences.edit { remove(logExportDirectoryKey) }
    }
}
