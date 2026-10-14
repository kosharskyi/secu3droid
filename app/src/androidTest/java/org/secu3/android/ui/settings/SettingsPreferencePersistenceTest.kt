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
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import androidx.preference.PreferenceManager
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.secu3.android.R
import org.secu3.android.ui.MainActivity
import org.secu3.android.ui.SETTINGS_ROUTE

class SettingsPreferencePersistenceTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun settingsPersistsAcrossRecreationAndReentryAndTracksExternalChanges() {
        val key = compose.activity.getString(R.string.pref_write_log_key)
        val preference = PreferenceManager.getDefaultSharedPreferences(compose.activity)
        val originalValue = preference.getBoolean(key, false)
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
            assertEquals(changedValue, preference.getBoolean(key, originalValue))

            compose.activityRule.scenario.recreate()
            compose.waitForIdle()
            if (changedValue) compose.onNodeWithTag("settings-toggle-${R.string.pref_write_log_key}").assertIsOn()
            else compose.onNodeWithTag("settings-toggle-${R.string.pref_write_log_key}").assertIsOff()

            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                preference.edit().putBoolean(key, originalValue).commit()
            }
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
            preference.edit().putBoolean(key, originalValue).commit()
            compose.waitForIdle()
        }
    }

    @Test fun aboutDialogOpensAndDismissesWithBack() {
        compose.onNodeWithContentDescription("Settings icon").performClick()
        compose.waitForIdle()
        compose.onNodeWithText(compose.activity.getString(R.string.pref_about_title)).performClick()
        compose.onNodeWithTag("settings-about-dialog").assertIsDisplayed()
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
        compose.waitForIdle()
        compose.onNodeWithTag("settings-about-dialog").assertDoesNotExist()
    }

    @Test fun folderPickerCancelAndGrantFailuresDoNotSaveAndSuccessPersistsOnlyGrantedFlags() {
        val uri = Uri.parse("content://example/tree/logs")
        val saved = mutableListOf<String>()
        val permissions = mutableListOf<Pair<Uri, Int>>()
        consumeFolderPickerResult(
            Activity.RESULT_CANCELED,
            Intent().setData(uri),
            takePermission = { value, flags -> permissions += value to flags },
            save = saved::add,
        )
        consumeFolderPickerResult(
            Activity.RESULT_OK,
            Intent(),
            takePermission = { value, flags -> permissions += value to flags },
            save = saved::add,
        )
        assertEquals(emptyList<Pair<Uri, Int>>(), permissions)
        consumeFolderPickerResult(
            Activity.RESULT_OK,
            Intent().setData(uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION),
            takePermission = { value, flags -> permissions += value to flags; throw SecurityException("grant denied") },
            save = saved::add,
        )
        assertEquals(emptyList<String>(), saved)
        assertEquals(listOf(uri to Intent.FLAG_GRANT_READ_URI_PERMISSION), permissions)
        permissions.clear()

        consumeFolderPickerResult(
            Activity.RESULT_OK,
            Intent().setData(uri).addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                    Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or Intent.FLAG_GRANT_PREFIX_URI_PERMISSION,
            ),
            takePermission = { value, flags -> permissions += value to flags },
            save = saved::add,
        )
        assertEquals(listOf(uri to (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)), permissions)
        assertEquals(listOf(uri.toString()), saved)
        assertEquals(
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or Intent.FLAG_GRANT_PREFIX_URI_PERMISSION,
            folderPickerIntent().flags,
        )
    }
}
