package org.secu3.android.ui.parameters.dialogs

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.platform.app.InstrumentationRegistry
import android.view.KeyEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.secu3.android.ui.compose.SecuComposeTheme

class ParameterChoiceDialogTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun cancelClosesWithoutSelecting() {
        val open = mutableStateOf(true)
        val selected = mutableListOf<Int>()
        compose.setContent {
            SecuComposeTheme {
                if (open.value) ParameterChoiceDialog(
                    title = "Choice",
                    choices = listOf(ParameterChoice(41, "Forty one", "choice-41")),
                    onDismiss = { open.value = false },
                    onSelect = selected::add,
                )
            }
        }

        compose.onNodeWithText(InstrumentationRegistry.getInstrumentation().targetContext.getString(android.R.string.cancel)).performClick()
        compose.waitForIdle()

        assertTrue(selected.isEmpty())
        compose.onAllNodesWithTag("choice-41").assertCountEquals(0)
    }

    @Test
    fun selectingLateChoicePreservesCallerIdAndDismisses() {
        val open = mutableStateOf(true)
        val selected = mutableListOf<Int>()
        val choices = (0..30).map { ParameterChoice(0x0400 + it, "Choice $it", "choice-$it") }
        compose.setContent {
            SecuComposeTheme {
                if (open.value) ParameterChoiceDialog(
                    title = "Choice",
                    choices = choices,
                    onDismiss = { open.value = false },
                    onSelect = selected::add,
                )
            }
        }

        compose.onNodeWithTag("choice-30").performScrollTo().performClick()
        compose.waitForIdle()

        assertEquals(listOf(0x041E), selected)
        compose.onAllNodesWithTag("choice-30").assertCountEquals(0)
    }

    @Test
    fun backDismissesWithoutSelecting() {
        val open = mutableStateOf(true)
        val selected = mutableListOf<Int>()
        compose.setContent {
            SecuComposeTheme {
                if (open.value) ParameterChoiceDialog(
                    title = "Choice",
                    choices = listOf(ParameterChoice(7, "Seven", "choice-7")),
                    onDismiss = { open.value = false },
                    onSelect = selected::add,
                )
            }
        }

        compose.onNodeWithTag("choice-7").assertIsDisplayed()
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
        compose.waitForIdle()

        assertTrue(selected.isEmpty())
        compose.onAllNodesWithTag("choice-7").assertCountEquals(0)
    }
}
