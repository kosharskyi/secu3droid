/*
 *    SecuDroid  - An open source, free manager for SECU-3 engine control unit
 *    Copyright (C) 2026 Vitalii O. Kosharskyi. Ukraine, Kyiv
 */

package org.secu3.android.ui.compose

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.secu3.android.R
import org.secu3.android.network.models.Author
import org.secu3.android.network.models.GitHubRelease
import org.secu3.android.ui.home.PendingReleasePermissionFlow

class ConfirmationDialogsTest {
    @get:Rule val compose = createComposeRule()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun bluetoothRationaleOkAndCancelButtonsCloseDialogAndInvokeCallbacks() = verifyDialog(
        R.string.permission_rationale_title, R.string.permission_rationale_message,
    )

    @Test fun downloadRationaleOkAndCancelButtonsCloseDialogAndInvokeCallbacks() = verifyDialog(
        R.string.download_manager_permission_rationale_title, R.string.download_manager_permission_rationale_message,
    )

    @Test fun clearErrorsOkAndCancelButtonsCloseDialogAndInvokeCallbacks() = verifyDialog(
        R.string.clear_all_errors, R.string.dialog_confirm_clear_errors_message,
    )

    @Test fun permissionHandoffKeepsSelectedReleaseUntilResultAndCancelClearsIt() {
        val flow = PendingReleasePermissionFlow()
        val first = release(1)
        val newer = release(2)

        var currentRelease = first
        flow.begin(first)
        // A later release becoming current must not replace the release selected for this request.
        currentRelease = newer
        assertEquals(first, flow.complete(granted = true))
        assertEquals(newer, currentRelease)

        flow.begin(first)
        flow.cancel()
        assertEquals(null, flow.complete(granted = true))

        flow.begin(newer)
        assertEquals(null, flow.complete(granted = false))
        assertEquals(null, flow.complete(granted = true))
    }

    private fun verifyDialog(title: Int, message: Int) {
        var confirmations = 0
        var cancellations = 0
        val titleText = context.getString(title)
        val visible = mutableStateOf(true)
        compose.setContent {
            MaterialTheme {
                if (visible.value) ConfirmationDialog(
                    title = title,
                    message = message,
                    onConfirm = { confirmations++; visible.value = false },
                    onCancel = { cancellations++; visible.value = false },
                )
            }
        }
        compose.onNodeWithText(titleText).assertIsDisplayed()
        compose.onNodeWithText(context.getString(android.R.string.ok)).performClick()
        compose.runOnIdle { assertEquals(1, confirmations) }
        compose.onNodeWithText(titleText).assertDoesNotExist()

        compose.runOnIdle { visible.value = true }
        compose.onNodeWithText(context.getString(android.R.string.cancel)).performClick()
        compose.runOnIdle { assertEquals(1, cancellations) }
        compose.onNodeWithText(titleText).assertDoesNotExist()
    }

    private fun release(id: Int) = GitHubRelease(
        url = "", htmlUrl = "", assetsUrl = "", uploadUrl = "", tarballUrl = "", zipballUrl = "",
        id = id, nodeId = "", tagName = "v$id", targetCommitish = "main", name = "Release $id",
        body = "", draft = false, prerelease = false, createdAt = "", publishedAt = "",
        author = Author(
            login = "", id = 1, nodeId = "", avatarUrl = "", gravatarId = "", url = "", htmlUrl = "",
            followersUrl = "", followingUrl = "", gistsUrl = "", starredUrl = "", subscriptionsUrl = "",
            organizationsUrl = "", reposUrl = "", eventsUrl = "", receivedEventsUrl = "", type = "User", siteAdmin = false,
        ),
        assets = emptyList(),
    )

}
