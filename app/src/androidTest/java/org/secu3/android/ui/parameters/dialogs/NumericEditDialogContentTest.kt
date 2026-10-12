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

import android.text.InputType
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.clearText
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withInputType
import androidx.test.espresso.matcher.ViewMatchers.withText
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class NumericEditDialogContentTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun integerEditorStepsClampsAndConfirmsCurrentValue() {
        var confirmed: String? = null
        var changed = false
        composeTestRule.setContent {
            MaterialTheme(colorScheme = lightColorScheme()) {
                NumericEditDialogContent(
                    title = "Integer parameter", initialText = "9", allowDecimal = false, allowNegative = true,
                    validate = { it.toIntOrNull()?.let { value -> value in -10..10 } == true },
                    onStep = { value, direction -> ((value.toIntOrNull() ?: 9) + direction * 4).coerceIn(-10, 10).toString() },
                    onConfirm = { value, didChange -> confirmed = value; changed = didChange }, onCancel = {},
                )
            }
        }

        onView(withId(android.R.id.edit)).check(matches(withInputType(
            InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_SIGNED,
        )))
        composeTestRule.onNodeWithText("Integer parameter").assertIsDisplayed()
        onView(withId(android.R.id.edit)).perform(clearText(), replaceText("4"), closeSoftKeyboard())
        composeTestRule.onNodeWithContentDescription("Increase value").performClick()
        composeTestRule.onNodeWithText("OK").assertIsEnabled().performClick()

        assertEquals("8", confirmed)
        assertEquals(true, changed)
    }

    @Test
    fun floatEditorStepsFromValidTypedValue() {
        var confirmed: String? = null
        composeTestRule.setContent {
            MaterialTheme(colorScheme = lightColorScheme()) {
                NumericEditDialogContent(
                    title = "Float parameter", initialText = "1.25", allowDecimal = true, allowNegative = true,
                    validate = { it.toFloatOrNull()?.let { value -> value in -2f..2f } == true },
                    onStep = { value, direction -> ((value.toFloatOrNull() ?: 1.25f) + direction * .25f).coerceIn(-2f, 2f).toString() },
                    onConfirm = { value, _ -> confirmed = value }, onCancel = {},
                )
            }
        }

        onView(withId(android.R.id.edit)).check(matches(withInputType(
            InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL or InputType.TYPE_NUMBER_FLAG_SIGNED,
        )))
        onView(withId(android.R.id.edit)).perform(clearText(), replaceText("-1.0"), closeSoftKeyboard())
        composeTestRule.onNodeWithContentDescription("Increase value").performClick()
        composeTestRule.onNodeWithText("OK").performClick()

        assertEquals("-0.75", confirmed)
    }

    @Test
    fun floatEditorRejectsOutOfRangeInputAndCancelDoesNotConfirm() {
        var confirmed: String? = null
        var canceled = false
        composeTestRule.setContent {
            MaterialTheme(colorScheme = lightColorScheme()) {
                NumericEditDialogContent(
                    title = "Float parameter", initialText = "1.25", allowDecimal = true, allowNegative = true,
                    validate = { it.toFloatOrNull()?.let { value -> value in -2f..2f } == true },
                    onStep = { value, direction -> ((value.toFloatOrNull() ?: 1.25f) + direction * .25f).coerceIn(-2f, 2f).toString() },
                    onConfirm = { value, _ -> confirmed = value }, onCancel = { canceled = true },
                )
            }
        }

        onView(withId(android.R.id.edit)).perform(clearText(), replaceText("2.5"), closeSoftKeyboard())
        composeTestRule.onNodeWithText("Enter a value within the allowed range").assertIsDisplayed()
        composeTestRule.onNodeWithText("OK").assertIsNotEnabled()
        composeTestRule.onNodeWithText("Cancel").performClick()

        assertEquals(null, confirmed)
        assertEquals(true, canceled)
    }

    @Test
    fun integerButtonsStepOutOfRangeHugeAndIncompleteTextAsExpected() {
        var current = 4
        composeTestRule.setContent {
            MaterialTheme(colorScheme = lightColorScheme()) {
                NumericEditDialogContent(
                    title = "Integer steps", initialText = "4", allowDecimal = false, allowNegative = true,
                    validate = { ParamIntEditDialogFragment.parseValidInput(it, 0, 10) != null },
                    onStep = { value, direction ->
                        current = ParamIntEditDialogFragment.stepValueFromInput(value, current, 3, 0, 10, direction)
                        current.toString()
                    }, onConfirm = { _, _ -> }, onCancel = {},
                )
            }
        }

        fun step(input: String, increase: Boolean, expected: String) {
            onView(withId(android.R.id.edit)).perform(clearText(), replaceText(input), closeSoftKeyboard())
            composeTestRule.onNodeWithContentDescription(if (increase) "Increase value" else "Decrease value").performClick()
            onView(withId(android.R.id.edit)).check(matches(withText(expected)))
        }

        step("12", increase = false, expected = "9")
        step("12", increase = true, expected = "10")
        step("-2", increase = true, expected = "1")
        step("-2", increase = false, expected = "0")
        step("999999999999999999999999999", increase = false, expected = "10")
        step("-999999999999999999999999999", increase = true, expected = "0")
        step("-", increase = true, expected = "3")
        step("-", increase = false, expected = "0")
    }

    @Test
    fun floatButtonsStepOutOfRangeAndIncompleteTextAsExpected() {
        var current = 1f
        composeTestRule.setContent {
            MaterialTheme(colorScheme = lightColorScheme()) {
                NumericEditDialogContent(
                    title = "Float steps", initialText = "1.00", allowDecimal = true, allowNegative = true,
                    validate = { ParamFloatEditDialogFragment.isValidInput(it, -2f, 2f) },
                    onStep = { value, direction ->
                        current = ParamFloatEditDialogFragment.stepValueFromInput(value, current, .5f, -2f, 2f, direction)
                        "%.2f".format(java.util.Locale.US, current)
                    }, onConfirm = { _, _ -> }, onCancel = {},
                )
            }
        }

        fun step(input: String, increase: Boolean, expected: String) {
            onView(withId(android.R.id.edit)).perform(clearText(), replaceText(input), closeSoftKeyboard())
            composeTestRule.onNodeWithContentDescription(if (increase) "Increase value" else "Decrease value").performClick()
            onView(withId(android.R.id.edit)).check(matches(withText(expected)))
        }

        step("2.5", increase = false, expected = "2.00")
        step("2.5", increase = true, expected = "2.00")
        step("-2.5", increase = true, expected = "-2.00")
        step("-2.5", increase = false, expected = "-2.00")
        step("-.", increase = true, expected = "-1.50")
        step(".", increase = false, expected = "-2.00")
    }
}
