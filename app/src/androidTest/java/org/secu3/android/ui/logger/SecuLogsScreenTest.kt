package org.secu3.android.ui.logger

import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.secu3.android.R
import java.io.File

class SecuLogsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val testDir = File(context.cacheDir, "logs-screen-test")

    @After
    fun tearDown() {
        testDir.deleteRecursively()
    }

    @Test
    fun emptyStateShowsWhenThereAreNoLogs() {
        composeTestRule.setContent {
            SecuLogsScreen(
                files = emptyList(),
                snackbarHostState = SnackbarHostState(),
                onBackClick = {},
                onShareClick = {},
                onDownloadClick = {},
                onDeleteClick = {},
            )
        }

        composeTestRule.onNodeWithText(context.getString(R.string.empty_logs_list)).assertIsDisplayed()
        composeTestRule.onAllNodesWithText(context.getString(R.string.save)).assertCountEquals(0)
    }

    @Test
    fun logFileShowsNameAndActions() {
        val file = createLogFile("secu-log-001.csv")

        composeTestRule.setContent {
            SecuLogsScreen(
                files = listOf(file),
                snackbarHostState = SnackbarHostState(),
                onBackClick = {},
                onShareClick = {},
                onDownloadClick = {},
                onDeleteClick = {},
            )
        }

        composeTestRule.onNodeWithText(file.name).assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.save)).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription(context.getString(R.string.share_log_file)).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription(context.getString(R.string.delete_log_file)).assertIsDisplayed()
    }

    @Test
    fun saveButtonInvokesDownloadCallback() {
        val file = createLogFile("secu-log-save.csv")
        var savedFile: File? = null

        composeTestRule.setContent {
            SecuLogsScreen(
                files = listOf(file),
                snackbarHostState = SnackbarHostState(),
                onBackClick = {},
                onShareClick = {},
                onDownloadClick = { savedFile = it },
                onDeleteClick = {},
            )
        }

        composeTestRule.onNodeWithText(context.getString(R.string.save)).performClick()

        assertEquals(file, savedFile)
    }

    @Test
    fun shareButtonInvokesShareCallback() {
        val file = createLogFile("secu-log-share.csv")
        var sharedFile: File? = null

        composeTestRule.setContent {
            SecuLogsScreen(
                files = listOf(file),
                snackbarHostState = SnackbarHostState(),
                onBackClick = {},
                onShareClick = { sharedFile = it },
                onDownloadClick = {},
                onDeleteClick = {},
            )
        }

        composeTestRule.onNodeWithContentDescription(context.getString(R.string.share_log_file)).performClick()

        assertEquals(file, sharedFile)
    }

    @Test
    fun deleteConfirmationInvokesDeleteCallback() {
        val file = createLogFile("secu-log-delete.csv")
        var deletedFile: File? = null

        composeTestRule.setContent {
            SecuLogsScreen(
                files = listOf(file),
                snackbarHostState = SnackbarHostState(),
                onBackClick = {},
                onShareClick = {},
                onDownloadClick = {},
                onDeleteClick = { deletedFile = it },
            )
        }

        composeTestRule.onNodeWithContentDescription(context.getString(R.string.delete_log_file)).performClick()
        composeTestRule.onNodeWithText(context.getString(R.string.dialog_confirm_delete_log_file_title)).assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.dialog_confirm_delete_log_message)).assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(android.R.string.ok)).performClick()

        assertEquals(file, deletedFile)
    }

    @Test
    fun backButtonInvokesBackCallback() {
        var clicked = false

        composeTestRule.setContent {
            SecuLogsScreen(
                files = emptyList(),
                snackbarHostState = SnackbarHostState(),
                onBackClick = { clicked = true },
                onShareClick = {},
                onDownloadClick = {},
                onDeleteClick = {},
            )
        }

        composeTestRule.onNodeWithContentDescription(context.getString(R.string.back)).performClick()

        assertTrue(clicked)
    }

    private fun createLogFile(name: String): File {
        testDir.mkdirs()
        return File(testDir, name).apply {
            writeText("rpm;map;tps\n900;35;0\n")
        }
    }
}
