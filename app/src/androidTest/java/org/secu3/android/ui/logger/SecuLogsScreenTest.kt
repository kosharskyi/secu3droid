package org.secu3.android.ui.logger

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.foundation.layout.width
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.secu3.android.R
import org.secu3.android.ui.compose.SecuComposeTheme
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
                pendingDeleteFile = null,
                onDeleteRequest = {},
                onDeleteCancel = {},
                onDeleteConfirm = {},
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
                pendingDeleteFile = null,
                onDeleteRequest = {},
                onDeleteCancel = {},
                onDeleteConfirm = {},
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
                pendingDeleteFile = null,
                onDeleteRequest = {},
                onDeleteCancel = {},
                onDeleteConfirm = {},
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
                pendingDeleteFile = null,
                onDeleteRequest = {},
                onDeleteCancel = {},
                onDeleteConfirm = {},
            )
        }

        composeTestRule.onNodeWithContentDescription(context.getString(R.string.share_log_file)).performClick()

        assertEquals(file, sharedFile)
    }

    @Test
    fun deleteConfirmationInvokesDeleteCallback() {
        val file = createLogFile("secu-log-delete.csv")
        var deletedFile: File? = null
        val pendingDelete = mutableStateOf<File?>(null)

        composeTestRule.setContent {
            SecuLogsScreen(
                files = listOf(file),
                snackbarHostState = SnackbarHostState(),
                onBackClick = {},
                onShareClick = {},
                onDownloadClick = {},
                pendingDeleteFile = pendingDelete.value,
                onDeleteRequest = { pendingDelete.value = it },
                onDeleteCancel = { pendingDelete.value = null },
                onDeleteConfirm = { deletedFile = pendingDelete.value; pendingDelete.value = null },
            )
        }

        composeTestRule.onNodeWithContentDescription(context.getString(R.string.delete_log_file)).performClick()
        composeTestRule.onNodeWithText(context.getString(R.string.dialog_confirm_delete_log_file_title)).assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.dialog_confirm_delete_log_message)).assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(android.R.string.cancel)).performClick()
        assertEquals(null, deletedFile)
        composeTestRule.onNodeWithText(file.name).assertIsDisplayed()

        composeTestRule.onNodeWithContentDescription(context.getString(R.string.delete_log_file)).performClick()
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
                pendingDeleteFile = null,
                onDeleteRequest = {},
                onDeleteCancel = {},
                onDeleteConfirm = {},
            )
        }

        composeTestRule.onNodeWithContentDescription(context.getString(R.string.back)).performClick()

        assertTrue(clicked)
    }

    @Test
    fun longFilenameAndActionsRemainUsableAtNarrowWidthAndLargeFontInBothThemes() {
        val name = "secu-logger-session-with-a-deliberately-long-filename-for-wrapping.csv"
        val file = createLogFile(name)
        val darkTheme = mutableStateOf(false)
        var shares = 0
        var downloads = 0
        var deleteRequests = 0
        val largeFontConfiguration = context.resources.configuration.apply {
            fontScale = 1.7f
        }

        composeTestRule.setContent {
            CompositionLocalProvider(LocalConfiguration provides largeFontConfiguration) {
                SecuComposeTheme(darkTheme = darkTheme.value) {
                    androidx.compose.foundation.layout.Box(modifier = androidx.compose.ui.Modifier.width(280.dp)) {
                        SecuLogsScreen(
                            files = listOf(file),
                            snackbarHostState = SnackbarHostState(),
                            onBackClick = {},
                            onShareClick = { shares++ },
                            onDownloadClick = { downloads++ },
                            pendingDeleteFile = null,
                            onDeleteRequest = { deleteRequests++ },
                            onDeleteCancel = {},
                            onDeleteConfirm = {},
                        )
                    }
                }
            }
        }

        repeat(2) {
            val filename = composeTestRule.onNodeWithText(name).performScrollTo().assertIsDisplayed()
            filename.assertHeightIsAtLeast(60.dp)
            composeTestRule.onNodeWithContentDescription(context.getString(R.string.share_log_file))
                .performScrollTo().performClick()
            composeTestRule.onNodeWithText(context.getString(R.string.save))
                .performScrollTo().performClick()
            composeTestRule.onNodeWithContentDescription(context.getString(R.string.delete_log_file))
                .performScrollTo().performClick()
            darkTheme.value = true
            composeTestRule.waitForIdle()
        }

        assertEquals(2, shares)
        assertEquals(2, downloads)
        assertEquals(2, deleteRequests)
    }

    private fun createLogFile(name: String): File {
        testDir.mkdirs()
        return File(testDir, name).apply {
            writeText("rpm;map;tps\n900;35;0\n")
        }
    }
}
