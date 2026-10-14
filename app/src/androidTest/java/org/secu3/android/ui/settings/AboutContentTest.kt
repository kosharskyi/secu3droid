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

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click
import androidx.compose.ui.geometry.Offset
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.secu3.android.BuildConfig
import org.secu3.android.R
import org.secu3.android.ui.compose.SecuComposeTheme

class AboutContentTest {
    @get:Rule val compose = createComposeRule()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun localizedAboutContentShowsVersionLicenseAndSource() {
        compose.setContent {
            SecuComposeTheme { AboutContent(open = {}) }
        }
        compose.onNodeWithText(context.getString(R.string.about_title)).assertIsDisplayed()
        compose.onNodeWithText("${context.getString(R.string.about_version)} ${BuildConfig.VERSION_NAME}").assertIsDisplayed()
        compose.onNodeWithText(context.getString(R.string.about_license), substring = true).assertIsDisplayed()
        compose.onNodeWithText(context.getString(R.string.about_sources), substring = true).assertIsDisplayed()
    }

    @Test fun sourceRepositoryLinkInvokesOpenCallback() {
        val opened = mutableListOf<String>()
        val linkCenter = androidx.compose.runtime.mutableStateOf(Offset.Zero)
        compose.setContent {
            SecuComposeTheme {
                AboutContent(
                    open = { opened += it },
                    onLinkBounds = { resource, bounds ->
                        if (resource == R.string.about_sources) linkCenter.value = bounds.center
                    },
                )
            }
        }
        val sourceText = compose.onNodeWithTag("about-linked-text-${R.string.about_sources}")
        sourceText.performScrollTo()
        compose.runOnIdle { assertNotEquals(Offset.Zero, linkCenter.value) }
        sourceText.performTouchInput { click(linkCenter.value) }
        compose.runOnIdle { assertEquals(listOf("https://github.com/kosharskyi/secu3droid"), opened) }
    }
}
