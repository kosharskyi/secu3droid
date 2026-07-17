package org.secu3.android.ui.errors

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.secu3.android.R
import org.secu3.android.connection.Connected
import org.secu3.android.models.CheckEngineError

class ErrorsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun loadingStateShowsProgressTextAndHidesErrors() {
        composeTestRule.setContent {
            ErrorsScreen(
                connectionState = Connected,
                errors = listOf(CheckEngineError(title = "CKPS", isActive = true)),
                isLoading = true,
                onBackClick = {},
                onClearClick = {},
            )
        }

        composeTestRule.onNodeWithText(context.getString(R.string.errors_loading_state)).assertIsDisplayed()
        composeTestRule.onAllNodesWithText("CKPS").assertCountEquals(0)
        composeTestRule.onAllNodesWithText(context.getString(R.string.errors_empty_state)).assertCountEquals(0)
    }

    @Test
    fun emptyStateShowsWhenThereAreNoActiveOrSavedErrors() {
        composeTestRule.setContent {
            ErrorsScreen(
                connectionState = Connected,
                errors = listOf(CheckEngineError(title = "Inactive", isActive = false, isSaved = false)),
                isLoading = false,
                onBackClick = {},
                onClearClick = {},
            )
        }

        composeTestRule.onNodeWithText(context.getString(R.string.errors_summary, 0, 0)).assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.errors_empty_state)).assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Inactive").assertCountEquals(0)
    }

    @Test
    fun listShowsOnlyActiveOrSavedErrors() {
        composeTestRule.setContent {
            ErrorsScreen(
                connectionState = Connected,
                errors = listOf(
                    CheckEngineError(title = "Active", isActive = true, isSaved = false),
                    CheckEngineError(title = "Saved", isActive = false, isSaved = true),
                    CheckEngineError(title = "Both", isActive = true, isSaved = true),
                    CheckEngineError(title = "Inactive", isActive = false, isSaved = false),
                ),
                isLoading = false,
                onBackClick = {},
                onClearClick = {},
            )
        }

        composeTestRule.onNodeWithText(context.getString(R.string.errors_summary, 2, 2)).assertIsDisplayed()
        composeTestRule.onNodeWithText("Active").assertIsDisplayed()
        composeTestRule.onNodeWithText("Saved").assertIsDisplayed()
        composeTestRule.onNodeWithText("Both").assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Inactive").assertCountEquals(0)

        composeTestRule.onNodeWithTag(errorStatusTag("Active", ErrorStatus.Active)).assertIsSelected()
        composeTestRule.onNodeWithTag(errorStatusTag("Active", ErrorStatus.Saved)).assertIsNotSelected()
        composeTestRule.onNodeWithTag(errorStatusTag("Saved", ErrorStatus.Active)).assertIsNotSelected()
        composeTestRule.onNodeWithTag(errorStatusTag("Saved", ErrorStatus.Saved)).assertIsSelected()
        composeTestRule.onNodeWithTag(errorStatusTag("Both", ErrorStatus.Active)).assertIsSelected()
        composeTestRule.onNodeWithTag(errorStatusTag("Both", ErrorStatus.Saved)).assertIsSelected()
    }

    @Test
    fun clearButtonInvokesCallback() {
        var clicked = false

        composeTestRule.setContent {
            ErrorsScreen(
                connectionState = Connected,
                errors = emptyList(),
                isLoading = false,
                onBackClick = {},
                onClearClick = { clicked = true },
            )
        }

        composeTestRule.onNodeWithContentDescription(context.getString(R.string.errors_clear_all_short)).performClick()

        assertTrue(clicked)
    }
}
