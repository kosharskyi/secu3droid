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
 */
package org.secu3.android.ui.parameters

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.swipeLeft
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.recyclerview.widget.RecyclerView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.viewpager2.widget.ViewPager2
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.secu3.android.connection.Connected
import org.secu3.android.R
import org.secu3.android.ui.compose.SecuComposeTheme

@RunWith(AndroidJUnit4::class)
class ParamsScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun tabsToolbarActionsStatusAndPagerStaySynchronized() {
        var backCalls = 0
        var saveCalls = 0
        lateinit var pager: ViewPager2
        compose.setContent {
            SecuComposeTheme {
                ParamsScreen(
                    titles = listOf("Starter", "Angles", "Idling", "Functions"),
                    connectionState = Connected,
                    onBack = { backCalls++ },
                    onSave = { saveCalls++ },
                    createPager = { context ->
                        ViewPager2(context).apply {
                            adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
                                override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
                                    object : RecyclerView.ViewHolder(
                                        FrameLayout(parent.context).apply {
                                            layoutParams = ViewGroup.LayoutParams(
                                                ViewGroup.LayoutParams.MATCH_PARENT,
                                                ViewGroup.LayoutParams.MATCH_PARENT,
                                            )
                                        },
                                    ) {}
                                override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) = Unit
                                override fun getItemCount() = 4
                            }
                            id = R.id.paramsPager
                            pager = this
                        }
                    },
                )
            }
        }

        compose.onNodeWithText("Idling").performClick()
        compose.onNodeWithText("Idling").assertIsSelected()
        compose.runOnIdle { assertEquals(2, pager.currentItem) }
        compose.onNodeWithContentDescription("Back").performClick()
        compose.onNodeWithContentDescription("Save").performClick()
        compose.onNodeWithContentDescription("Connected to ECU").assertExists()

        compose.waitUntil(timeoutMillis = 5_000) { pager.scrollState == ViewPager2.SCROLL_STATE_IDLE }
        onView(withId(R.id.paramsPager)).perform(swipeLeft())
        compose.waitForIdle()
        compose.onNodeWithText("Functions").assertIsSelected()
        compose.runOnIdle {
            assertEquals(3, pager.currentItem)
            assertEquals(1, backCalls)
            assertEquals(1, saveCalls)
        }
    }

    @Test fun selectedTabAndPagerIndexRestoreTogether() {
        var pager: ViewPager2? = null
        val restoration = StateRestorationTester(compose)
        restoration.setContent {
            SecuComposeTheme {
                ParamsScreen(
                    titles = listOf("Starter", "Angles", "Idling", "Functions"),
                    connectionState = Connected,
                    onBack = {},
                    onSave = {},
                    createPager = { context -> createTestPager(context).also { pager = it } },
                )
            }
        }
        compose.onNodeWithText("Idling").performClick()
        compose.waitForIdle()
        compose.runOnIdle { assertEquals(2, pager?.currentItem) }

        restoration.emulateSavedInstanceStateRestore()
        compose.waitForIdle()
        compose.onNodeWithText("Idling").assertIsSelected()
        compose.runOnIdle { assertEquals(2, pager?.currentItem) }
    }

    private fun createTestPager(context: android.content.Context): ViewPager2 = ViewPager2(context).apply {
        id = R.id.paramsPager
        adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
                object : RecyclerView.ViewHolder(
                    FrameLayout(parent.context).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT,
                        )
                    },
                ) {}
            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) = Unit
            override fun getItemCount() = 4
        }
    }
}
