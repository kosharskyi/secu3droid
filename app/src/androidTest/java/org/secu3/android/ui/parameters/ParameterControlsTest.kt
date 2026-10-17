package org.secu3.android.ui.parameters

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.secu3.android.ui.compose.SecuComposeTheme
import org.secu3.android.ui.parameters.compose.ParameterSwitchRow
import org.secu3.android.ui.parameters.compose.ParameterValueRow
import org.secu3.android.ui.parameters.compose.ParameterSelectionRow
import org.secu3.android.ui.parameters.compose.ParameterTextValueRow
import org.secu3.android.ui.parameters.compose.ParameterPage
import org.secu3.android.ui.parameters.compose.ParameterReadOnlyValueRow
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.mutableStateOf

@RunWith(AndroidJUnit4::class)
class ParameterControlsTest {
    @get:Rule val compose = createComposeRule()

    @Test fun controlsRemainReadableAndInteractiveInDarkModeAtLargeTextScale() {
        var changed = false
        val baseDensity = compose.density.density
        compose.setContent {
            SecuComposeTheme(darkTheme = true) {
                CompositionLocalProvider(LocalDensity provides Density(baseDensity, fontScale = 2f)) {
                    Column {
                        ParameterValueRow(
                            title = "A long engine parameter title that should reflow cleanly",
                            units = "degrees Celsius",
                            value = 123f,
                            precision = 0,
                            testTag = "large-value-row",
                            onClick = {},
                        )
                        ParameterSwitchRow("Enable long running engine correction", false, "large-switch-row") { changed = it }
                    }
                }
            }
        }

        compose.onNodeWithText("A long engine parameter title that should reflow cleanly").assertIsDisplayed()
        compose.onNodeWithText("123 degrees Celsius").assertIsDisplayed()
        compose.onNodeWithTag("large-switch-row").assertIsDisplayed().performClick()
        compose.runOnIdle { assertTrue(changed) }
        val bounds = compose.onNodeWithTag("large-value-row").getUnclippedBoundsInRoot()
        val rowHeight = bounds.bottom - bounds.top
                        assertTrue(rowHeight >= 66.dp)
    }

    @Test fun labeledFieldsWrapLongLocalizedLabelsAndKeepValuesAndActionsAtNarrowWidth() {
        var clickCount = 0
        var editCount = 0
        val dark = mutableStateOf(false)
        val baseDensity = compose.density.density
        compose.setContent {
            SecuComposeTheme(darkTheme = dark.value) {
                CompositionLocalProvider(LocalDensity provides Density(baseDensity, fontScale = 1.7f)) {
                    ParameterPage(Modifier.width(320.dp)) {
                            ParameterSelectionRow(
                                title = "Довга назва параметра керування системою впорскування",
                                value = "Розширене значення вибору",
                                testTag = "localized-selection",
                            ) { clickCount++ }
                            ParameterTextValueRow(
                                title = "Текстове поле з довгим перекладеним описом параметра",
                                value = "12345678901234567890",
                                testTag = "localized-editable",
                            ) { editCount++ }
                            ParameterTextValueRow("Тільки для читання", "Значення", "readonly-field")
                            ParameterTextValueRow(
                                title = "Довге числове значення параметра обертів двигуна",
                                value = "",
                                testTag = "localized-number",
                                numberText = "123456.78",
                                units = "об/хв",
                            )
                            ParameterSelectionRow("Вимкнений параметр", "Недоступно", "disabled-selection", enabled = false) {
                                clickCount += 100
                            }
                    }
                }
            }
        }

        compose.onNodeWithText("Довга назва параметра керування системою впорскування").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("localized-selection").assertHasClickAction()
            .assertTextContains("Довга назва параметра", substring = true)
            .assertTextContains("Розширене значення вибору", substring = true)
            .performClick()
        compose.onNodeWithText("Довга назва параметра керування системою впорскування").performClick()
        compose.onNodeWithTag("localized-selection-value", useUnmergedTree = true)
            .assertIsDisplayed().assertTextContains("Розширене значення вибору")
        compose.onNodeWithTag("localized-editable").assertHasClickAction()
            .performClick()
        compose.onNodeWithTag("localized-editable-value", useUnmergedTree = true).performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("readonly-field").performScrollTo().assertHasNoClickAction()
        compose.onNodeWithTag("readonly-field-value", useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithTag("localized-number-value", useUnmergedTree = true)
            .performScrollTo().assertIsDisplayed().assertTextContains("123456.78 об/хв")
        compose.onNodeWithTag("disabled-selection").performScrollTo().assertIsNotEnabled()
        compose.runOnIdle { assertTrue(clickCount == 2) }
        compose.runOnIdle { assertTrue(editCount == 1) }
        assertTextDoesNotOverflow("localized-selection-label")
        assertTextDoesNotOverflow("localized-selection-value")
        assertTextDoesNotOverflow("localized-number-label")
        assertTextDoesNotOverflow("localized-number-value")

        compose.runOnIdle { dark.value = true }
        compose.onNodeWithTag("localized-selection").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("localized-selection-value", useUnmergedTree = true).assertIsDisplayed()
        assertTextDoesNotOverflow("localized-selection-label")
        assertTextDoesNotOverflow("localized-selection-value")
        assertTextDoesNotOverflow("localized-number-label")
        assertTextDoesNotOverflow("localized-number-value")
    }

    private fun assertTextDoesNotOverflow(testTag: String) {
        val node = compose.onNodeWithTag(testTag, useUnmergedTree = true)
        var result: TextLayoutResult? = null
        node.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { action ->
            val results = mutableListOf<TextLayoutResult>()
            action(results)
            result = results.firstOrNull()
        }
        assertTrue("$testTag exposes text layout", result != null)
        assertTrue("$testTag text fits at narrow width and enlarged scale", !requireNotNull(result).hasVisualOverflow)
    }
}
