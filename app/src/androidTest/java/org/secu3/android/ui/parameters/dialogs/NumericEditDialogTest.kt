/*
 *    SecuDroid  - An open source, free manager for SECU-3 engine control unit
 *    Copyright (C) 2026 Vitalii O. Kosharskyi. Ukraine, Kyiv
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 */
package org.secu3.android.ui.parameters.dialogs

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import android.view.KeyEvent
import android.view.MotionEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.secu3.android.ui.compose.SecuComposeTheme
import org.secu3.android.ui.parameters.ParameterEditRequest

class NumericEditDialogTest {
    @get:Rule val compose = createComposeRule()

    @Test fun integerConfirmStepsAndInvokesAcceptedValueOnce() {
        val accepted = mutableListOf<Number>()
        compose.setContent {
            SecuComposeTheme {
                NumericEditDialog(
                    ParameterEditRequest.IntValue("Integer", 3, -20, 20, 2) {}, 41L,
                    accepted::add, {},
                )
            }
        }
        compose.onNodeWithContentDescription("Increase value").performClick()
        compose.onNodeWithTag("numeric_confirm").performClick()
        assertEquals(listOf(5), accepted)
    }

    @Test fun invalidFloatCannotConfirmAndCancelOnlyDismisses() {
        val accepted = mutableListOf<Number>()
        var dismissed = 0
        compose.setContent {
            SecuComposeTheme {
                NumericEditDialog(
                    ParameterEditRequest.FloatValue("Float", 1.25f, -2f, 2f, .25f, 2) {}, 42L,
                    accepted::add, { dismissed++ },
                )
            }
        }
        compose.onNodeWithTag("numeric_value").performTextClearance()
        compose.onNodeWithTag("numeric_value").performTextInput("2.5")
        compose.onNodeWithText("OK").assertIsNotEnabled()
        compose.onNodeWithTag("numeric_cancel").performClick()
        assertEquals(emptyList<Number>(), accepted)
        assertEquals(1, dismissed)
    }

    @Test fun unchangedFloatKeepsTheOriginalUnroundedValue() {
        val accepted = mutableListOf<Number>()
        compose.setContent {
            SecuComposeTheme {
                NumericEditDialog(
                    ParameterEditRequest.FloatValue("Float", 1.23456f, -2f, 2f, .25f, 2) {}, 43L,
                    accepted::add, {},
                )
            }
        }
        compose.onNodeWithText("1.23").assertIsDisplayed()
        compose.onNodeWithTag("numeric_confirm").performClick()
        assertEquals(listOf(1.23456f), accepted)
    }

    @Test fun steppedFallbackHandlesIncompleteTypedText() {
        val accepted = mutableListOf<Number>()
        compose.setContent {
            SecuComposeTheme {
                NumericEditDialog(ParameterEditRequest.IntValue("Integer", 8, 0, 10, 1) {}, 44L, accepted::add, {})
            }
        }
        compose.onNodeWithContentDescription("Increase value").performClick()
        compose.onNodeWithTag("numeric_value").performTextClearance()
        compose.onNodeWithTag("numeric_value").performTextInput("-")
        compose.onNodeWithContentDescription("Increase value").performClick()
        compose.onNodeWithTag("numeric_value").assertTextEquals("10")
        compose.onNodeWithTag("numeric_confirm").performClick()
        assertEquals(listOf(10), accepted)
    }

    @Test fun typedTextAndChangedFlagSurviveSaveableRestoration() {
        val restoration = StateRestorationTester(compose)
        var changed: Boolean? = null
        restoration.setContent {
            SecuComposeTheme {
                NumericEditDialogContent(
                    title = "Integer", initialText = "8", sessionKey = 47L, allowDecimal = false,
                    allowNegative = false, validate = { it.toIntOrNull() in 0..100 }, onStep = { text, _ -> text },
                    onConfirm = { _, didChange -> changed = didChange }, onCancel = {},
                )
            }
        }
        compose.onNodeWithTag("numeric_value").performTextClearance()
        compose.onNodeWithTag("numeric_value").performTextInput("12")
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithTag("numeric_value").assertTextEquals("12")
        compose.onNodeWithTag("numeric_confirm").performClick()
        assertEquals(true, changed)
    }

    @Test fun backDismissesWithoutAccepting() {
        val accepted = mutableListOf<Number>()
        var dismissed = 0
        compose.setContent {
            SecuComposeTheme {
                NumericEditDialog(ParameterEditRequest.IntValue("Back", 1, 0, 10, 1) {}, 45L, accepted::add) { dismissed++ }
            }
        }
        compose.onNodeWithTag("numeric_value").assertIsDisplayed()
        compose.waitForIdle()
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
        compose.waitUntil(timeoutMillis = 5_000) { dismissed == 1 }
        compose.waitForIdle()
        assertEquals(emptyList<Number>(), accepted)
        assertEquals(1, dismissed)
    }

    @Test fun outsideTapDismissesWithoutAccepting() {
        val accepted = mutableListOf<Number>()
        var dismissed = 0
        compose.setContent {
            SecuComposeTheme {
                NumericEditDialog(ParameterEditRequest.IntValue("Outside", 1, 0, 10, 1) {}, 46L, accepted::add) { dismissed++ }
            }
        }
        compose.onNodeWithTag("numeric_value").assertIsDisplayed()
        compose.waitForIdle()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val metrics = instrumentation.targetContext.resources.displayMetrics
        val x = 1f
        val y = metrics.heightPixels / 2f
        val downTime = android.os.SystemClock.uptimeMillis()
        val down = MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, x, y, 0)
        val up = MotionEvent.obtain(downTime, downTime + 20, MotionEvent.ACTION_UP, x, y, 0)
        try {
            instrumentation.sendPointerSync(down)
            instrumentation.sendPointerSync(up)
        } finally {
            down.recycle()
            up.recycle()
        }
        compose.waitUntil(timeoutMillis = 5_000) { dismissed == 1 }
        compose.waitForIdle()
        assertEquals(emptyList<Number>(), accepted)
        assertEquals(1, dismissed)
    }

    @Test fun dialogFitsNarrowWindowAtLargeFontInLightAndDarkThemes() {
        val darkTheme = mutableStateOf(false)
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 1.8f)) {
                SecuComposeTheme(darkTheme = darkTheme.value) {
                    Box(Modifier.width(320.dp)) {
                        NumericEditDialog(
                            ParameterEditRequest.IntValue("Large font value", 1234, 0, 9999, 1) {},
                            48L,
                            {},
                            {},
                        )
                    }
                }
            }
        }

        fun assertDialogFits() {
            compose.onNodeWithTag("numeric_editor_surface").assertIsDisplayed()
            compose.onNodeWithTag("numeric_value").assertIsDisplayed()
            compose.onNodeWithTag("numeric_cancel").assertIsDisplayed()
            compose.onNodeWithTag("numeric_confirm").assertIsDisplayed()
            val surface = compose.onNodeWithTag("numeric_editor_surface").fetchSemanticsNode().boundsInRoot
            val value = compose.onNodeWithTag("numeric_value").fetchSemanticsNode().boundsInRoot
            assertTrue("numeric field should remain inside dialog surface", value.left >= surface.left && value.right <= surface.right)
        }

        assertDialogFits()
        compose.runOnIdle { darkTheme.value = true }
        compose.waitForIdle()
        assertDialogFits()
    }
}
