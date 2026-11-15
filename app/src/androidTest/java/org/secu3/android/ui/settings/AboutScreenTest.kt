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
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.secu3.android.BuildConfig
import org.secu3.android.R
import org.secu3.android.ui.compose.SecuComposeTheme
import kotlin.math.min

class AboutScreenTest {
    @get:Rule val compose = createComposeRule()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun contentUsesTheAvailableWidthUpToItsReadingWidth() {
        compose.setContent {
            SecuComposeTheme { AboutScreen(onNavigateUp = {}, onOpenLink = {}) }
        }
        val viewport = compose.onNodeWithTag("about-viewport").getUnclippedBoundsInRoot()
        val content = compose.onNodeWithTag("about-content").getUnclippedBoundsInRoot()
        val viewportWidth = (viewport.right - viewport.left).value
        val contentWidth = (content.right - content.left).value
        assertEquals(min(640f, viewportWidth - 32f), contentWidth, 1f)
        assertEquals((content.left - viewport.left).value, (viewport.right - content.right).value, 1f)
        compose.onNodeWithText(context.getString(R.string.pref_about_title)).assertIsDisplayed()
        compose.onNodeWithText(context.getString(R.string.about_version, BuildConfig.VERSION_NAME))
            .assertIsDisplayed()
    }

    @Test fun linkRowsOpenTheirDestinations() {
        val expectedLinks = mapOf(
            R.string.about_forum to "https://secu-3.org/forum/viewtopic.php?f=8&t=11&start=2147483647",
            R.string.about_telegram to "https://t.me/+JrqNrXQddPQzOThi",
            R.string.about_website to "http://secu-3.org",
            R.string.about_license to "http://www.gnu.org/licenses/gpl-3.0-standalone.html",
            R.string.about_sources to "https://github.com/kosharskyi/secu3droid",
        )
        val opened = mutableListOf<String>()
        compose.setContent {
            SecuComposeTheme { AboutScreen(onNavigateUp = {}, onOpenLink = { opened += it }) }
        }
        expectedLinks.keys.forEach { label ->
            compose.onNodeWithText(context.getString(label))
                .performScrollTo().assertIsDisplayed().performClick()
        }
        compose.runOnIdle { assertEquals(expectedLinks.values.toList(), opened) }
    }
}
