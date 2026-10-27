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

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.secu3.android.R
import org.secu3.android.ui.compose.SecuComposeTheme

class SettingsScreenTest {
    @get:Rule val compose = createComposeRule()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val initial = SettingsUiState(true, false, ";", false, "EngineLogs", "3", true, false)

    @Test fun allRowsShowPersistedSummariesAndInvokeCallbacks() {
        val state = mutableStateOf(initial)
        val toggled = mutableListOf<Pair<Int, Boolean>>()
        var folder = false
        var about = false
        var back = false
        compose.setContent {
            SecuComposeTheme {
                SettingsContent(state.value, null, { back = true }, { key, value ->
                    toggled += key to value
                    state.value = when (key) {
                        R.string.pref_write_log_key -> state.value.copy(writeLog = value)
                        R.string.pref_log_csv_write_title_key -> state.value.copy(writeCsvHeader = value)
                        R.string.pref_write_binary_log_key -> state.value.copy(writeBinaryLog = value)
                        R.string.pref_keep_screen_key -> state.value.copy(keepScreenOn = value)
                        R.string.pref_wakelock_key -> state.value.copy(wakeLock = value)
                        else -> state.value
                    }
                }, {},
                    { folder = true }, {}, { true }, {}, {}, {}, { about = true })
            }
        }
        listOf(R.string.pref_write_log_title, R.string.pref_log_csv_write_title, R.string.pref_log_csv_delimeter_title,
            R.string.pref_write_binary_log_title, R.string.pref_log_export_directory_title, R.string.pref_connection_retries_title,
            R.string.pref_keep_screen_title, R.string.pref_wakelock_title, R.string.pref_about_title)
            .forEach { compose.onNodeWithText(context.getString(it)).performScrollTo().assertIsDisplayed() }
        compose.onNodeWithText("EngineLogs").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(context.getString(R.string.pref_connection_retries_summary, "3")).performScrollTo().assertIsDisplayed()
        val toggles = listOf(
            R.string.pref_write_log_key to initial.writeLog,
            R.string.pref_log_csv_write_title_key to initial.writeCsvHeader,
            R.string.pref_write_binary_log_key to initial.writeBinaryLog,
            R.string.pref_keep_screen_key to initial.keepScreenOn,
            R.string.pref_wakelock_key to initial.wakeLock,
        )
        toggles.forEach { (key, initiallyChecked) ->
            val toggle = compose.onNodeWithTag("settings-toggle-$key").performScrollTo()
            if (initiallyChecked) toggle.assertIsOn() else toggle.assertIsOff()
            val titleResource = when (key) {
                R.string.pref_write_log_key -> R.string.pref_write_log_title
                R.string.pref_log_csv_write_title_key -> R.string.pref_log_csv_write_title
                R.string.pref_write_binary_log_key -> R.string.pref_write_binary_log_title
                R.string.pref_keep_screen_key -> R.string.pref_keep_screen_title
                else -> R.string.pref_wakelock_title
            }
            compose.onNodeWithText(context.getString(titleResource)).performClick()
            val updated = compose.onNodeWithTag("settings-toggle-$key").performScrollTo()
            if (initiallyChecked) updated.assertIsOff() else updated.assertIsOn()
        }
        compose.runOnIdle { assertEquals(5, toggled.size) }
        compose.onNodeWithText(context.getString(R.string.pref_log_export_directory_title)).performScrollTo().performClick()
        compose.onNodeWithText(context.getString(R.string.pref_about_title)).performScrollTo().performClick()
        compose.onNodeWithContentDescription(context.getString(R.string.back)).performClick()
        compose.runOnIdle { assertTrue(folder && about && back) }
    }

    @Test fun delimiterSelectionAndRetryValidationConfirmAndCancelWork() {
        val current = mutableStateOf(initial)
        val dialog = mutableStateOf<SettingsDialog?>(null)
        var selected = ""
        var confirmed = ""
        compose.setContent {
            SecuComposeTheme {
                SettingsContent(current.value, dialog.value, {}, { _, _ -> }, { selected = it; current.value = current.value.copy(csvDelimiter = it); dialog.value = null },
                    {}, { dialog.value = SettingsDialog.Retries }, { it == "3" || it == "9" }, { confirmed = it; current.value = current.value.copy(connectionRetries = it); dialog.value = null },
                    { dialog.value = it }, { dialog.value = null }, {})
            }
        }
        compose.onNodeWithText(context.getString(R.string.pref_log_csv_delimeter_title)).performScrollTo().performClick()
        val colonLabel = context.resources.getStringArray(R.array.csv_delimeters)[2]
        compose.onNodeWithText(colonLabel).performClick()
        compose.runOnIdle {
            assertEquals(":", selected)
            assertEquals(":", current.value.csvDelimiter)
        }
        compose.onNodeWithText(colonLabel, substring = false).assertIsDisplayed()
        compose.onNodeWithText(context.getString(R.string.pref_connection_retries_title)).performScrollTo().performClick()
        compose.onNodeWithText("3").performTextReplacement("0")
        compose.onNodeWithText(context.getString(android.R.string.ok)).assertIsNotEnabled()
        compose.onNodeWithText(context.getString(android.R.string.cancel)).performClick()
        compose.runOnIdle { assertEquals("3", current.value.connectionRetries) }
        compose.onNodeWithText(context.getString(R.string.pref_connection_retries_summary, "3")).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(context.getString(R.string.pref_connection_retries_title)).performScrollTo().performClick()
        compose.onNodeWithText("3").performTextReplacement("9")
        compose.onNodeWithText(context.getString(android.R.string.ok)).performClick()
        compose.runOnIdle { assertEquals("9", confirmed) }
        compose.onNodeWithText(context.getString(R.string.pref_connection_retries_summary, "9")).performScrollTo().assertIsDisplayed()
    }

    @Test fun longExportLocationWrapsAndRowActionWorksAtNarrowWidthAndLargeFontInBothThemes() {
        val darkTheme = mutableStateOf(false)
        val exportLocation = "content://com.example.documents/tree/" + "engine-log-folder/".repeat(5)
        val state = initial.copy(exportFolder = exportLocation)
        var folderSelections = 0
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 1.7f)) {
                SecuComposeTheme(darkTheme = darkTheme.value) {
                    Box(Modifier.width(280.dp)) {
                        SettingsContent(
                            state = state,
                            dialog = null,
                            onNavigateUp = {},
                            onToggle = { _, _ -> },
                            onSelectDelimiter = {},
                            onChooseFolder = { folderSelections++ },
                            onEditRetries = {},
                            onValidateRetries = { true },
                            onSaveRetries = {},
                            onSelectDialog = {},
                            onDismissDialog = {},
                            onShowAbout = {},
                        )
                    }
                }
            }
        }

        val folderSummary = compose.onNodeWithTag("settings-export-folder-summary", useUnmergedTree = true).performScrollTo().assertIsDisplayed()
        assertTrue(folderSummary.fetchSemanticsNode().boundsInRoot.height > context.resources.displayMetrics.density * 60f)
        compose.onNodeWithText(context.getString(R.string.pref_log_export_directory_title)).performScrollTo().performClick()
        compose.runOnIdle { assertEquals(1, folderSelections) }

        darkTheme.value = true
        compose.onNodeWithTag("settings-export-folder-summary", useUnmergedTree = true).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(context.getString(R.string.pref_log_export_directory_title)).performScrollTo().performClick()
        compose.runOnIdle { assertEquals(2, folderSelections) }
    }
}
