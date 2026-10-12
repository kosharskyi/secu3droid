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

import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.preference.PreferenceManager
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.secu3.android.R

class SettingsPreferencePersistenceTest {
    @get:Rule val compose = createAndroidComposeRule<SettingsActivity>()

    @Test fun writeLogToggleUpdatesAndPersistsSharedPreference() {
        val key = compose.activity.getString(R.string.pref_write_log_key)
        val preference = PreferenceManager.getDefaultSharedPreferences(compose.activity)
        val originalValue = preference.getBoolean(key, false)
        val toggle = compose.onNodeWithTag("settings-toggle-${R.string.pref_write_log_key}")
        try {
            if (originalValue) toggle.assertIsOn() else toggle.assertIsOff()
            compose.onNodeWithText(compose.activity.getString(R.string.pref_write_log_title)).performClick()
            compose.waitForIdle()
            val changedValue = !originalValue
            if (changedValue) toggle.assertIsOn() else toggle.assertIsOff()
            assertEquals(changedValue, preference.getBoolean(key, originalValue))
        } finally {
            preference.edit().putBoolean(key, originalValue).commit()
            compose.waitForIdle()
        }
    }
}
