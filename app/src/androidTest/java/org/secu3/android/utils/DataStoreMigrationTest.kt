package org.secu3.android.utils

import android.content.Context
import android.content.ContextWrapper
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.joinAll
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.secu3.android.R
import org.secu3.android.di.modules.UtilsModule
import org.secu3.android.models.SensorGaugeStyle
import org.threeten.bp.LocalDate

class DataStoreMigrationTest {
    private val base = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun productionUserMigrationMovesOnlyActiveKeysAndExistingDataStoreWins(): Unit = runBlocking {
        val id = UUID.randomUUID().toString()
        val root = File(base.cacheDir, "migration-$id").apply { mkdirs() }
        val legacy = base.getSharedPreferences("migration-$id", Context.MODE_PRIVATE)
        val wrappedContext = object : ContextWrapper(base) {
            override fun getSharedPreferences(name: String, mode: Int) = legacy
            override fun getFilesDir() = File(root, "files").apply { mkdirs() }
            override fun getApplicationContext(): Context = this
            override fun deleteSharedPreferences(name: String) = base.deleteSharedPreferences("migration-$id")
        }
        val target = wrappedContext.preferencesDataStoreFile("user_preferences")
        val appearanceKey = stringPreferencesKey(base.getString(R.string.pref_appearance_mode_key))
        val legacyAppearanceKey = base.getString(R.string.pref_night_mode_key)
        val unknownStyleKey = stringPreferencesKey(UserPrefs.SENSOR_GAUGE_STYLE_KEY)
        val retiredColumnsKey = "columns_count"
        val storeJob = SupervisorJob()
        val storeScope = CoroutineScope(storeJob + Dispatchers.IO)
        val existingStore = PreferenceDataStoreFactory.create(scope = storeScope, produceFile = { target })
        existingStore.edit { it[appearanceKey] = AppearanceMode.Dark.preferenceValue }
        close(storeScope, storeJob)

        legacy.edit().putString(base.getString(R.string.pref_appearance_mode_key), AppearanceMode.Light.preferenceValue)
            .putBoolean(legacyAppearanceKey, false)
            .putString(UserPrefs.SENSOR_GAUGE_STYLE_KEY, "future_style")
            .putString(base.getString(R.string.pref_connection_retries_key), "6")
            .putString(base.getString(R.string.pref_bluetooth_device_key), "Legacy ECU")
            .putInt(retiredColumnsKey, 4)
            .commit()

        val migrationJob = SupervisorJob()
        val migrationScope = CoroutineScope(migrationJob + Dispatchers.IO)
        val migratedStore = UtilsModule.getUserPrefsDataStore(
            wrappedContext,
            migrationScope,
            PreferencesMigrations(wrappedContext),
        )
        val settings = UserPrefs(wrappedContext, migratedStore).settings.first()
        assertEquals(AppearanceMode.Dark, settings.appearanceMode)
        assertEquals("6", settings.connectionRetriesText)
        assertEquals(SensorGaugeStyle.PreciseArc, settings.sensorGaugeStyle)
        assertEquals("Legacy ECU", settings.bluetoothDeviceName)
        assertNull(settings.bluetoothDeviceAddress)
        val migratedValues = migratedStore.data.first()
        assertEquals("future_style", migratedValues[unknownStyleKey])
        assertEquals(4, legacy.getInt(retiredColumnsKey, -1))
        assertFalse(legacy.contains(base.getString(R.string.pref_appearance_mode_key)))
        assertFalse(legacy.contains(base.getString(R.string.pref_connection_retries_key)))
        close(migrationScope, migrationJob)
        legacy.edit().clear().commit()
        target.deleteRecursively()
    }

    @Test
    fun productionAppMigrationPreservesDateAcrossDataStoreReopen(): Unit = runBlocking {
        val id = UUID.randomUUID().toString()
        val legacy = base.getSharedPreferences("migration-app-$id", Context.MODE_PRIVATE)
        val root = File(base.cacheDir, "migration-app-$id").apply { mkdirs() }
        val targetContext = object : ContextWrapper(base) {
            override fun getSharedPreferences(name: String, mode: Int) = legacy
            override fun getFilesDir() = File(root, "files").apply { mkdirs() }
            override fun getApplicationContext(): Context = this
            override fun deleteSharedPreferences(name: String) = base.deleteSharedPreferences("migration-app-$id")
        }
        val target = targetContext.preferencesDataStoreFile("app_preferences")
        legacy.edit().putString(AppPrefs.LAST_APP_VERSION_CHECK_KEY, "2026-10-03").commit()

        val migrationJob = SupervisorJob()
        val migrationScope = CoroutineScope(migrationJob + Dispatchers.IO)
        val migratedStore = UtilsModule.getAppPrefsDataStore(
            targetContext,
            migrationScope,
            PreferencesMigrations(targetContext),
        )
        val appPrefs = AppPrefs(migratedStore)
        assertEquals(LocalDate.parse("2026-10-03"), appPrefs.lastAppVersionCheck.first())
        assertFalse(legacy.contains(AppPrefs.LAST_APP_VERSION_CHECK_KEY))
        appPrefs.setLastAppVersionCheck(LocalDate.parse("2026-10-27"))
        close(migrationScope, migrationJob)

        val reopenedJob = SupervisorJob()
        val reopenedScope = CoroutineScope(reopenedJob + Dispatchers.IO)
        val reopened = PreferenceDataStoreFactory.create(scope = reopenedScope, produceFile = { target })
        assertEquals("2026-10-27", reopened.data.first()[stringPreferencesKey(AppPrefs.LAST_APP_VERSION_CHECK_KEY)])
        close(reopenedScope, reopenedJob)
        legacy.edit().clear().commit()
        target.deleteRecursively()
    }

    private suspend fun close(scope: CoroutineScope, job: Job) {
        scope.cancel()
        job.cancelAndJoin()
    }

    @Test
    fun productionUserMigrationWithAnEmptyLegacyStoreKeepsDefaultsUnpersisted() = runBlocking {
        val id = UUID.randomUUID().toString()
        val legacyName = "migration-empty-$id"
        val root = File(base.cacheDir, legacyName).apply { mkdirs() }
        val legacy = base.getSharedPreferences(legacyName, Context.MODE_PRIVATE)
        val context = object : ContextWrapper(base) {
            override fun getApplicationContext(): Context = this
            override fun getFilesDir() = File(root, "files").apply { mkdirs() }
            override fun getSharedPreferences(name: String, mode: Int) = legacy
            override fun deleteSharedPreferences(name: String) = base.deleteSharedPreferences(legacyName)
        }
        val job = SupervisorJob()
        val scope = CoroutineScope(job + Dispatchers.IO)
        try {
            val store = UtilsModule.getUserPrefsDataStore(context, scope, PreferencesMigrations(context))
            val settings = UserPrefs(context, store).settings.first()
            assertEquals(AppearanceMode.System, settings.appearanceMode)
            assertEquals(";", settings.csvDelimiter)
            assertFalse(settings.isWakeLockEnabled)
            assertEquals(base.getString(R.string.defaultConnectionRetries), settings.connectionRetriesText)
            assertTrue(store.data.first().asMap().isEmpty())
        } finally {
            close(scope, job)
            base.deleteSharedPreferences(legacyName)
            root.deleteRecursively()
        }
    }
}
