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

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import androidx.lifecycle.ViewModelStore
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.secu3.android.R
import org.secu3.android.models.InjectionTimingMode
import org.secu3.android.ui.MainActivity
import org.secu3.android.ui.SETTINGS_ROUTE
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withContext
import org.secu3.android.ui.messages.UiMessageQueue
import org.secu3.android.utils.prefs.IsolatedUserPrefs
import org.secu3.android.utils.prefs.UserPrefs
import java.io.IOException
import java.util.UUID

class SettingsPreferencePersistenceTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun usageAnalyticsConsentPersistsAcrossActivityRecreation() {
        val original = runBlocking { compose.activity.userPrefs.settings.first().isUsageAnalyticsEnabled }
        val tag = "settings-toggle-${R.string.pref_usage_analytics_key}"
        try {
            compose.onNodeWithContentDescription("Settings icon").performClick()
            compose.waitForIdle()
            val toggle = compose.onNodeWithTag(tag).performScrollTo()
            if (original) toggle.assertIsOn() else toggle.assertIsOff()
            toggle.performClick()
            val changed = !original
            compose.waitUntil(5_000) {
                runBlocking { compose.activity.userPrefs.settings.first().isUsageAnalyticsEnabled == changed }
            }

            compose.activityRule.scenario.recreate()
            compose.waitForIdle()
            val restored = compose.onNodeWithTag(tag).performScrollTo()
            if (changed) restored.assertIsOn() else restored.assertIsOff()
        } finally {
            runBlocking { compose.activity.userPrefs.setUsageAnalyticsEnabled(original) }
            compose.waitForIdle()
        }
    }

    @Test fun connectionRetryDraftSurvivesActivityRecreationWithoutSaving() {
        val persisted = runBlocking {
            compose.activity.userPrefs.settings.first().connectionRetriesText
        }
        val draft = ((persisted.toIntOrNull() ?: 3) + 1).toString()
        compose.onNodeWithContentDescription("Settings icon").performClick()
        compose.waitForIdle()
        assertEquals(SETTINGS_ROUTE, compose.activity.rootNavigationController?.currentDestination?.route)
        compose.onNodeWithText(compose.activity.getString(R.string.pref_connection_retries_title))
            .performScrollTo().performClick()
        compose.onNodeWithTag("settings-connection-retries-input").performTextReplacement(draft)

        compose.activityRule.scenario.recreate()
        compose.waitForIdle()
        compose.onNodeWithTag("settings-connection-retries-input").assertTextEquals(draft)
        assertEquals(
            "An unfinished retry edit must not update the preference",
            persisted,
            runBlocking { compose.activity.userPrefs.settings.first().connectionRetriesText },
        )

        compose.onNodeWithText(compose.activity.getString(android.R.string.cancel)).performClick()
        compose.onNodeWithText(compose.activity.getString(R.string.pref_connection_retries_title))
            .performScrollTo().performClick()
        compose.onNodeWithTag("settings-connection-retries-input").assertTextEquals(persisted)
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

    @Test fun injectionTimingConventionPersistsFromInstrumentSettings() {
        val prefs = compose.activity.userPrefs
        val original = runBlocking { prefs.settings.first().injectionTimingMode }
        try {
            runBlocking { prefs.setInjectionTimingMode(InjectionTimingMode.Btdc360) }
            compose.onNodeWithContentDescription("Settings icon").performClick()
            val row = compose.onNodeWithTag("injection-timing-mode-row")
            row.performScrollTo().assertTextContains(compose.activity.getString(R.string.injection_timing_btdc_360))
            row.performClick()
            val option = "injection-timing-mode-atdc_360"
            compose.onNodeWithTag(option).performClick()
            compose.waitUntil(5_000) { compose.onAllNodesWithTag(option).fetchSemanticsNodes().isEmpty() }
            assertEquals(InjectionTimingMode.Atdc360, runBlocking { prefs.settings.first().injectionTimingMode })

            compose.activityRule.scenario.recreate()
            row.performScrollTo().assertTextContains(compose.activity.getString(R.string.injection_timing_atdc_360))
        } finally {
            runBlocking { prefs.setInjectionTimingMode(original) }
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
                val prefs = UserPrefs(base, failingStore)
                val analytics = org.secu3.android.utils.analytics.UsageAnalytics(base, prefs)
                viewModel = SettingsViewModel(prefs, messages, analytics)
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
}
