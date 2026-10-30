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

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextInputSelection
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.secu3.android.ui.parameters.ParameterEditRequest

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

        composeTestRule.onNodeWithText("Integer parameter").assertIsDisplayed()
        composeTestRule.onNodeWithTag("numeric_value").performTextClearance()
        composeTestRule.onNodeWithTag("numeric_value").performTextInput("4")
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

        composeTestRule.onNodeWithTag("numeric_value").performTextClearance()
        composeTestRule.onNodeWithTag("numeric_value").performTextInput("-1.0")
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

        composeTestRule.onNodeWithTag("numeric_value").performTextClearance()
        composeTestRule.onNodeWithTag("numeric_value").performTextInput("2.5")
        val invalidValueMessage = InstrumentationRegistry.getInstrumentation().targetContext
            .getString(org.secu3.android.R.string.numeric_edit_invalid_value)
        composeTestRule.onNodeWithText(invalidValueMessage).assertIsDisplayed()
        composeTestRule.onNodeWithText("OK").assertIsNotEnabled()
        composeTestRule.onNodeWithTag("numeric_cancel").performClick()

        assertEquals(null, confirmed)
        assertEquals(true, canceled)
    }

    @Test
    fun integerButtonsStepOutOfRangeHugeAndIncompleteTextAsExpected() {
        var current = 4
        val request = ParameterEditRequest.IntValue("Integer steps", 4, 0, 10, 3) {}
        composeTestRule.setContent {
            MaterialTheme(colorScheme = lightColorScheme()) {
                NumericEditDialogContent(
                    title = "Integer steps", initialText = "4", allowDecimal = false, allowNegative = true,
                    validate = { request.parseInput(it) != null },
                    onStep = { value, direction ->
                        current = request.stepValue(value, current, direction)
                        current.toString()
                    }, onConfirm = { _, _ -> }, onCancel = {},
                )
            }
        }

        fun step(input: String, increase: Boolean, expected: String) {
            composeTestRule.onNodeWithTag("numeric_value").performTextClearance()
            composeTestRule.onNodeWithTag("numeric_value").performTextInput(input)
            composeTestRule.onNodeWithContentDescription(if (increase) "Increase value" else "Decrease value").performClick()
            composeTestRule.onNodeWithTag("numeric_value").assertTextEquals(expected)
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
        val request = ParameterEditRequest.FloatValue("Float steps", 1f, -2f, 2f, .5f, 2) {}
        composeTestRule.setContent {
            MaterialTheme(colorScheme = lightColorScheme()) {
                NumericEditDialogContent(
                    title = "Float steps", initialText = "1.00", allowDecimal = true, allowNegative = true,
                    validate = { request.parseInput(it) != null },
                    onStep = { value, direction ->
                        current = request.stepValue(value, current, direction)
                        request.formatValue(current)
                    }, onConfirm = { _, _ -> }, onCancel = {},
                )
            }
        }

        fun step(input: String, increase: Boolean, expected: String) {
            composeTestRule.onNodeWithTag("numeric_value").performTextClearance()
            composeTestRule.onNodeWithTag("numeric_value").performTextInput(input)
            composeTestRule.onNodeWithContentDescription(if (increase) "Increase value" else "Decrease value").performClick()
            composeTestRule.onNodeWithTag("numeric_value").assertTextEquals(expected)
        }

        step("2.5", increase = false, expected = "2.00")
        step("2.5", increase = true, expected = "2.00")
        step("-2.5", increase = true, expected = "-2.00")
        step("-2.5", increase = false, expected = "-2.00")
        step("-.", increase = true, expected = "-1.50")
        step(".", increase = false, expected = "-2.00")
    }

    @Test
    fun insertingAtSelectionKeepsTextBeforeAndAfterCursor() {
        var confirmed: String? = null
        composeTestRule.setContent {
            MaterialTheme(colorScheme = lightColorScheme()) {
                NumericEditDialogContent(
                    title = "Integer parameter", initialText = "123", allowDecimal = false, allowNegative = true,
                    validate = { it.toIntOrNull() != null }, onStep = { value, _ -> value },
                    onConfirm = { value, _ -> confirmed = value }, onCancel = {},
                )
            }
        }

        composeTestRule.onNodeWithTag("numeric_value").performTextInputSelection(TextRange(1))
        composeTestRule.onNodeWithTag("numeric_value").performTextInput("9")
        composeTestRule.onNodeWithTag("numeric_value").assertTextEquals("1923")
        composeTestRule.onNodeWithText("OK").performClick()
        assertEquals("1923", confirmed)
    }

    @Test
    fun imeDoneDoesNotConfirmInvalidInput() {
        var confirmed: String? = null
        composeTestRule.setContent {
            MaterialTheme(colorScheme = lightColorScheme()) {
                NumericEditDialogContent(
                    title = "Integer parameter", initialText = "12", allowDecimal = false, allowNegative = true,
                    validate = { it.toIntOrNull()?.let { value -> value in 0..20 } == true },
                    onStep = { value, _ -> value }, onConfirm = { value, _ -> confirmed = value }, onCancel = {},
                )
            }
        }

        composeTestRule.onNodeWithTag("numeric_value").performTextClearance()
        composeTestRule.onNodeWithTag("numeric_value").performTextInput("-")
        composeTestRule.onNodeWithTag("numeric_value").performImeAction()

        assertEquals(null, confirmed)
        composeTestRule.onNodeWithText("OK").assertIsNotEnabled()
    }

    @Test
    fun movingCursorWithoutChangingTextDoesNotMarkValueChanged() {
        var didChange: Boolean? = null
        composeTestRule.setContent {
            MaterialTheme(colorScheme = lightColorScheme()) {
                NumericEditDialogContent(
                    title = "Integer parameter", initialText = "123", allowDecimal = false, allowNegative = true,
                    validate = { it.toIntOrNull() != null }, onStep = { value, _ -> value },
                    onConfirm = { _, changed -> didChange = changed }, onCancel = {},
                )
            }
        }

        composeTestRule.onNodeWithTag("numeric_value").performTextInputSelection(TextRange(1))
        composeTestRule.onNodeWithText("OK").performClick()

        assertEquals(false, didChange)
    }
}
