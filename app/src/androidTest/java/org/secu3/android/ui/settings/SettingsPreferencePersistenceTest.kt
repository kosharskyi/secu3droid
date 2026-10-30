/*
 *    SecuDroid  - An open source, free manager for SECU-3 engine control unit
 *    Copyright (C) 2024 Vitalii O. Kosharskyi. Ukraine, Kyiv
 *
 *    SECU-3  - An open source, free engine control unit
 *    Copyright (C) 2007-2024 Alexey A. Shabelnikov. Ukraine, Kyiv
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *    contacts:
 *                    http://secu-3.org
 *                    email: vetalkosharskiy@gmail.com
 */
package org.secu3.android.ui.settings

import android.app.Activity
import android.content.ContentResolver
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.Lifecycle
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.secu3.android.R
import org.secu3.android.ui.MainActivity
import org.secu3.android.ui.SETTINGS_ROUTE
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withContext
import org.secu3.android.ui.messages.UiMessageQueue
import org.secu3.android.utils.FileHelper
import org.secu3.android.utils.IsolatedUserPrefs
import org.secu3.android.utils.UserPrefs
import java.io.IOException
import java.util.UUID

class SettingsPreferencePersistenceTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun settingsPersistsAcrossRecreationAndReentryAndTracksExternalChanges() {
        val originalValue = runBlocking { compose.activity.userPrefs.settings.first().isSensorLoggerEnabled }
        try {
            compose.onNodeWithContentDescription("Settings icon").performClick()
            compose.waitForIdle()
            assertEquals(SETTINGS_ROUTE, compose.activity.rootNavigationController?.currentDestination?.route)
            val toggle = compose.onNodeWithTag("settings-toggle-${R.string.pref_write_log_key}")
            if (originalValue) toggle.assertIsOn() else toggle.assertIsOff()
            compose.onNodeWithText(compose.activity.getString(R.string.pref_write_log_title)).performClick()
            compose.waitForIdle()
            val changedValue = !originalValue
            if (changedValue) toggle.assertIsOn() else toggle.assertIsOff()
            compose.waitUntil(5_000) {
                runBlocking { compose.activity.userPrefs.settings.first().isSensorLoggerEnabled == changedValue }
            }
            assertEquals(changedValue, runBlocking { compose.activity.userPrefs.settings.first().isSensorLoggerEnabled })

            compose.activityRule.scenario.recreate()
            compose.waitForIdle()
            if (changedValue) compose.onNodeWithTag("settings-toggle-${R.string.pref_write_log_key}").assertIsOn()
            else compose.onNodeWithTag("settings-toggle-${R.string.pref_write_log_key}").assertIsOff()

            runBlocking { compose.activity.userPrefs.setSensorLoggerEnabled(originalValue) }
            compose.waitForIdle()
            if (originalValue) compose.onNodeWithTag("settings-toggle-${R.string.pref_write_log_key}").assertIsOn()
            else compose.onNodeWithTag("settings-toggle-${R.string.pref_write_log_key}").assertIsOff()

            compose.onNodeWithContentDescription(compose.activity.getString(R.string.back)).performClick()
            compose.waitForIdle()
            compose.onNodeWithContentDescription("Settings icon").performClick()
            compose.waitForIdle()
            if (originalValue) compose.onNodeWithTag("settings-toggle-${R.string.pref_write_log_key}").assertIsOn()
            else compose.onNodeWithTag("settings-toggle-${R.string.pref_write_log_key}").assertIsOff()
        } finally {
            runBlocking { compose.activity.userPrefs.setSensorLoggerEnabled(originalValue) }
            compose.waitForIdle()
        }
    }

    @Test fun aboutDialogOpensAndDismissesWithBack() {
        compose.onNodeWithContentDescription("Settings icon").performClick()
        compose.waitForIdle()
        compose.onNodeWithText(compose.activity.getString(R.string.pref_about_title)).performScrollTo().performClick()
        compose.onNodeWithTag("settings-about-dialog").assertIsDisplayed()
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
        compose.waitForIdle()
        compose.onNodeWithTag("settings-about-dialog").assertDoesNotExist()
    }

    @Test fun fuelFlowCalibrationSavesThroughSettingsAndSurvivesActivityRecreation() {
        val initial = runBlocking { compose.activity.userPrefs.settings.first().fuelFlowPulsesPerLitre }
        val saved = if (initial == 8_000) 16_000 else 8_000
        try {
            compose.onNodeWithContentDescription("Settings icon").performClick()
            compose.waitForIdle()
            compose.onNodeWithTag("settings-fuel-flow-pulses-per-litre-row").performScrollTo().performClick()
            compose.onNodeWithTag("settings-fuel-flow-pulses-per-litre-input").performTextClearance()
            compose.onNodeWithTag("settings-fuel-flow-pulses-per-litre-input").performTextInput(saved.toString())
            compose.onNodeWithText(compose.activity.getString(android.R.string.ok)).performClick()
            compose.waitUntil(5_000) {
                runBlocking { compose.activity.userPrefs.settings.first().fuelFlowPulsesPerLitre == saved }
            }
            compose.activityRule.scenario.recreate()
            compose.waitForIdle()
            compose.onNodeWithTag("settings-fuel-flow-pulses-per-litre-row").assertIsDisplayed()
            compose.onNodeWithText(saved.toString(), substring = true).assertIsDisplayed()
        } finally {
            runBlocking { compose.activity.userPrefs.setFuelFlowPulsesPerLitre(initial) }
        }
    }

    @Test fun fuelFlowSaveFailureKeepsEditorOpenAndReportsStorageError() = runBlocking {
        val base = InstrumentationRegistry.getInstrumentation().targetContext
        val fixture = IsolatedUserPrefs(base, "fuel-flow-failure-${UUID.randomUUID()}")
        val failingStore = object : DataStore<Preferences> {
            override val data = fixture.dataStore.data
            override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences =
                throw IOException("write failed")
        }
        val messages = UiMessageQueue()
        val store = ViewModelStore()
        lateinit var viewModel: SettingsViewModel
        try {
            withContext(Dispatchers.Main) {
                viewModel = SettingsViewModel(base, UserPrefs(base, failingStore), FileHelper(base), messages)
                store.put("fuel-flow-failure", viewModel)
                viewModel.showDialog(SettingsDialog.FuelFlowPulsesPerLitre)
            }
            withTimeout(5_000) { viewModel.state.first { it != null } }
            withContext(Dispatchers.Main) { viewModel.saveFuelFlowPulsesPerLitre("8000") }
            withTimeout(5_000) { messages.pending.first { list -> list.any { it.textRes == R.string.preferences_storage_error } } }
            assertEquals(SettingsDialog.FuelFlowPulsesPerLitre, viewModel.dialog.value)
            assertEquals(16_000, fixture.prefs.settings.first().fuelFlowPulsesPerLitre)
        } finally {
            withContext(Dispatchers.Main) { store.clear() }
            fixture.close()
        }
    }

    @Test fun cancelingFolderPickerKeepsSavedDirectory() {
        val initial = runBlocking { compose.activity.userPrefs.settings.first().logExportDirectoryUri }
        compose.onNodeWithContentDescription("Settings icon").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("settings-export-folder-row").performScrollTo().performClick()
        compose.waitUntil(10_000) {
            InstrumentationRegistry.getInstrumentation().uiAutomation.rootInActiveWindow?.packageName
                ?.toString()?.endsWith(".documentsui") == true
        }
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
        compose.waitUntil(10_000) { compose.activity.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) }
        compose.onNodeWithTag("settings-export-folder-row").assertIsDisplayed()
        assertEquals(initial, runBlocking { compose.activity.userPrefs.settings.first().logExportDirectoryUri })
    }

    @Test fun successfulFolderPickerResultWithoutUriKeepsSavedDirectory() {
        val initial = runBlocking { compose.activity.userPrefs.settings.first().logExportDirectoryUri }
        compose.onNodeWithContentDescription("Settings icon").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("settings-export-folder-row").performScrollTo().performClick()
        compose.waitUntil(10_000) {
            InstrumentationRegistry.getInstrumentation().uiAutomation.rootInActiveWindow?.packageName
                ?.toString()?.endsWith(".documentsui") == true
        }
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val registry = compose.activity.activityResultRegistry
            val state = Bundle().also(registry::onSaveInstanceState)
            val keys = requireNotNull(state.getStringArrayList("KEY_COMPONENT_ACTIVITY_REGISTERED_KEYS"))
            val codes = requireNotNull(state.getIntegerArrayList("KEY_COMPONENT_ACTIVITY_REGISTERED_RCS"))
            val launchedKey = requireNotNull(state.getStringArrayList("KEY_COMPONENT_ACTIVITY_LAUNCHED_KEYS")).single()
            check(registry.dispatchResult(codes[keys.indexOf(launchedKey)], Activity.RESULT_OK, null))
        }
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
        compose.waitUntil(10_000) { compose.activity.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) }
        compose.onNodeWithTag("settings-export-folder-row").assertIsDisplayed()
        assertEquals(initial, runBlocking { compose.activity.userPrefs.settings.first().logExportDirectoryUri })
    }

    @Test fun exportFolderIsSavedOnlyAfterPersistingGrantedPermission() = runBlocking {
        val base = InstrumentationRegistry.getInstrumentation().targetContext
        val uri = Uri.parse("content://example/tree/logs")
        val grants = mutableListOf<Pair<Uri, Int>>()
        var failGrant = false
        val resolver = object : ContentResolver(base) {
            override fun takePersistableUriPermission(uri: Uri, modeFlags: Int) {
                grants += uri to modeFlags
                if (failGrant) throw SecurityException("grant denied")
            }
        }
        val context = object : ContextWrapper(base) {
            override fun getContentResolver() = resolver
        }
        val fixture = IsolatedUserPrefs(context, "settings-folder-${System.nanoTime()}")
        val store = ViewModelStore()
        lateinit var viewModel: SettingsViewModel
        try {
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                viewModel = SettingsViewModel(context, fixture.prefs, FileHelper(context), UiMessageQueue())
                store.put("settings-folder", viewModel)
            }
            val read = Intent.FLAG_GRANT_READ_URI_PERMISSION
            val write = Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            val extra = Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or Intent.FLAG_GRANT_PREFIX_URI_PERMISSION
            failGrant = true
            InstrumentationRegistry.getInstrumentation().runOnMainSync { viewModel.saveExportFolder(uri, read or extra) }
            assertEquals(listOf(uri to read), grants)
            assertEquals(null, fixture.prefs.settings.first().logExportDirectoryUri)

            failGrant = false
            InstrumentationRegistry.getInstrumentation().runOnMainSync { viewModel.saveExportFolder(uri, read or write or extra) }
            withTimeout(5_000) { fixture.prefs.settings.first { it.logExportDirectoryUri == uri.toString() } }
            assertEquals(listOf(uri to read, uri to (read or write)), grants)

            val noGrantUri = Uri.parse("content://example/tree/no-grant")
            InstrumentationRegistry.getInstrumentation().runOnMainSync { viewModel.saveExportFolder(noGrantUri, extra) }
            withTimeout(5_000) { fixture.prefs.settings.first { it.logExportDirectoryUri == noGrantUri.toString() } }
            assertEquals(2, grants.size)
        } finally {
            InstrumentationRegistry.getInstrumentation().runOnMainSync { store.clear() }
            fixture.close()
        }
    }
}
