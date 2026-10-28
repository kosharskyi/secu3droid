/*
 *    SecuDroid  - An open source, free manager for SECU-3 engine control unit
 *    Copyright (C) 2026 Vitalii O. Kosharskyi. Ukraine, Kyiv
 *
 *    SECU-3  - An open source, free engine control unit
 *    Copyright (C) 2007-2026 Alexey A. Shabelnikov. Ukraine, Kyiv
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
package org.secu3.android.ui.parameters.pages.compose

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toPixelMap
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.secu3.android.domain.parameters.AnglesValues
import org.secu3.android.ui.compose.SecuComposeTheme

class AnglesScreenTest {
    @get:Rule val compose = createComposeRule()
    private val context get() = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun loadingThenAllValuesAndFlagsRenderWithoutCallbacksOnRecomposition() {
        var edits = 0
        var changes = 0
        val state = mutableStateOf<AnglesValues?>(null)
        compose.setContent { SecuComposeTheme { AnglesScreen(state.value, { edits++ }, { _, _ -> changes++ }) } }
        compose.onNodeWithTag("angles_loading").assertIsDisplayed()
        val values = AnglesValues(1f, 2f, 3f, 4f, 5f, 0, true, false, true, 6f)
        compose.runOnIdle { state.value = values }
        AnglesNumber.entries.forEach { number ->
            compose.onNodeWithTag("angles_${number.name.lowercase()}").performScrollTo().assertIsDisplayed()
            val expected = String.format(java.util.Locale.getDefault(), "%.${number.precision}f", number.value(values)) + " " +
                context.getString(number.unitRes)
            compose.onNodeWithTag("angles_${number.name.lowercase()}-value", useUnmergedTree = true).assertTextEquals(expected)
        }
        AnglesFlag.entries.forEach { compose.onNodeWithTag("angles_flag_${it.name.lowercase()}").performScrollTo().assertIsDisplayed() }
        compose.runOnIdle { state.value = values.copy(maxAngle = 8f) }
        assertEquals(0, edits)
        assertEquals(0, changes)
    }

    @Test fun editAndSwitchActionsReportOnlyUserInput() {
        var edited: AnglesNumber? = null
        val changes = mutableListOf<Pair<AnglesFlag, Boolean>>()
        val values = AnglesValues(1f, 2f, 3f, 4f, 5f, 0, false, false, false, 6f)
        compose.setContent { SecuComposeTheme { AnglesScreen(values, { edited = it }, { flag, checked -> changes += flag to checked }) } }
        compose.onNodeWithTag("angles_min").performClick()
        assertEquals(AnglesNumber.MIN, edited)
        compose.onNodeWithTag("angles_flag_working_map-switch").performScrollTo().performClick()
        assertEquals(listOf(AnglesFlag.WORKING_MAP to true), changes)
    }

    @Test fun octaneCorrectionIsDisabledWhileZeroAngleIsActive() {
        val values = AnglesValues(1f, 2f, 3f, 4f, 5f, 2, false, false, false, 6f)
        compose.setContent { SecuComposeTheme { AnglesScreen(values, {}, { _, _ -> }) } }
        compose.onNodeWithTag("angles_octane").assertIsNotEnabled()
    }

    @Test fun octaneCorrectionFadesAndRemainsVisibleInLightAndDarkThemes() {
        val values = mutableStateOf(AnglesValues(1f, 2f, 3f, 4f, 5f, 1, false, false, false, 6f))
        val darkTheme = mutableStateOf(false)
        var editCount = 0
        compose.setContent {
            SecuComposeTheme(darkTheme = darkTheme.value) {
                AnglesScreen(values.value, { editCount++ }, { _, _ -> })
            }
        }

        repeat(2) { themeIndex ->
            if (themeIndex == 1) compose.runOnIdle { darkTheme.value = true }
            val row = compose.onNodeWithTag("angles_octane").performScrollTo()
            row.assertIsDisplayed().assertIsNotEnabled()
            val disabledImage = row.captureToImage()

            compose.runOnIdle { values.value = values.value.copy(zeroAdvAngle = 0) }
            row.assertIsDisplayed().assertHasClickAction().performClick()
            compose.runOnIdle { assertEquals(themeIndex + 1, editCount) }
            val enabledImage = row.captureToImage()
            assertTrue("Octane row color changes when enabled in theme $themeIndex", changedPixelCount(disabledImage, enabledImage) > 0)

            compose.runOnIdle { values.value = values.value.copy(zeroAdvAngle = 1) }
            row.assertIsDisplayed().assertIsNotEnabled()
            compose.runOnIdle { assertEquals(themeIndex + 1, editCount) }
        }
    }

    private fun changedPixelCount(first: ImageBitmap, second: ImageBitmap): Int {
        val firstPixels = first.toPixelMap()
        val secondPixels = second.toPixelMap()
        assertEquals(firstPixels.width, secondPixels.width)
        assertEquals(firstPixels.height, secondPixels.height)
        var changed = 0
        for (x in 0 until firstPixels.width) for (y in 0 until firstPixels.height) {
            if (firstPixels[x, y] != secondPixels[x, y]) changed++
        }
        return changed
    }
}
