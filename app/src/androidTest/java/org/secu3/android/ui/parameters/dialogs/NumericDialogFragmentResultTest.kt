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
package org.secu3.android.ui.parameters.dialogs

import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.secu3.android.ui.MainActivity

class NumericDialogFragmentResultTest {
    @get:Rule
    val activityRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun integerDialogEmitsSteppedValueOnlyWhenConfirmed() {
        val emitted = mutableListOf<Int>()
        activityRule.activityRule.scenario.onActivity { activity ->
            val dialog = ParamIntEditDialogFragment.newInstance(3, "Integer", 2, 20, -20)
            dialog.newValueLiveData.observe(activity) { emitted.add(it) }
            dialog.show(activity.supportFragmentManager, "integer")
        }

        activityRule.onNodeWithContentDescription("Increase value").performClick()
        activityRule.onNodeWithContentDescription("Increase value").performClick()
        activityRule.onNodeWithText("OK").performClick()
        activityRule.waitForIdle()

        assertEquals(listOf(7), emitted)
    }

    @Test
    fun floatDialogCancelDismissesWithoutEmittingAValue() {
        val emitted = mutableListOf<Float>()
        activityRule.activityRule.scenario.onActivity { activity ->
            val dialog = ParamFloatEditDialogFragment.newInstance(1.25f, "Float", .25f, 2f, -2f, 2)
            dialog.newValueLiveData.observe(activity) { emitted.add(it) }
            dialog.show(activity.supportFragmentManager, "float")
        }

        activityRule.onNodeWithText("Cancel").performClick()
        activityRule.waitForIdle()

        activityRule.onAllNodesWithText("Float").assertCountEquals(0)
        assertEquals(emptyList<Float>(), emitted)
    }
}
