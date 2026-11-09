package org.secu3.android.ui.errors

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.secu3.android.R
import org.secu3.android.connection.Connected
import org.secu3.android.connection.ConnectionState
import org.secu3.android.connection.Disconnected
import org.secu3.android.domain.errors.ErrorsReadStatus
import org.secu3.android.domain.errors.ErrorsSession
import org.secu3.android.domain.errors.ErrorsSnapshot
import org.secu3.android.domain.errors.ErrorsWriteStatus
import org.secu3.android.ui.compose.SecuComposeTheme

class ErrorsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun savedErrorsStayVisibleWhenActiveReadingTimesOut() {
        var retries = 0
        composeTestRule.setContent {
            Content(
                ErrorsSnapshot(savedMask = 1, activeReadStatus = ErrorsReadStatus.Timeout),
                onRetryActive = { retries++ },
            )
        }

        text(R.string.errors_active_read_timeout).assertIsDisplayed()
        text(R.string.retry).performClick()
        scrollToError(0)
        composeTestRule.onNodeWithTag("error-saved:0", useUnmergedTree = true).assertIsSelected()
        composeTestRule.onNodeWithTag("error-active:0", useUnmergedTree = true).assertDoesNotExist()
        composeTestRule.runOnIdle { assertEquals(1, retries) }
    }

    @Test
    fun activeErrorsStayVisibleWhenSavedReadingTimesOut() {
        var refreshes = 0
        composeTestRule.setContent {
            Content(
                ErrorsSnapshot(activeMask = 1, savedReadStatus = ErrorsReadStatus.Timeout),
                onReadSaved = { refreshes++ },
            )
        }

        text(R.string.errors_saved_read_timeout).assertIsDisplayed()
        text(R.string.errors_read_saved).performClick()
        composeTestRule.onNodeWithTag("errors-clear-button").assertIsNotEnabled()
        scrollToError(0)
        composeTestRule.onNodeWithTag("error-active:0", useUnmergedTree = true).assertIsSelected()
        composeTestRule.onNodeWithTag("error-saved:0", useUnmergedTree = true).assertDoesNotExist()
        composeTestRule.runOnIdle { assertEquals(1, refreshes) }
    }

    @Test
    fun zeroMaskIsShownSeparatelyFromReadingTheOtherSource() {
        composeTestRule.setContent {
            Content(ErrorsSnapshot(savedMask = 0, activeReadStatus = ErrorsReadStatus.Reading))
        }

        text(R.string.errors_active_loading).assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.errors_saved_count, 0)).assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.errors_active_count, 0)).assertDoesNotExist()
        text(R.string.errors_no_reported).assertIsDisplayed()
    }

    @Test
    fun flagsOutsideTheCatalogAreCountedAndDoNotShowAnEmptyState() {
        composeTestRule.setContent {
            Content(ErrorsSnapshot(savedMask = 1 shl 31))
        }

        text(R.string.errors_unknown_flags).assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.errors_saved_count, 1)).assertIsDisplayed()
        text(R.string.errors_no_reported).assertDoesNotExist()
    }

    @Test
    fun showsBothBadgesAndAllowsBrowsingInactiveCodes() {
        composeTestRule.setContent {
            Content(ErrorsSnapshot(activeMask = 1, savedMask = 1))
        }

        scrollToError(0)
        composeTestRule.onNodeWithText(context.getString(R.string.errors_blink_code, 21)).assertIsDisplayed()
        composeTestRule.onNodeWithTag("error-active:0", useUnmergedTree = true).assertIsSelected()
        composeTestRule.onNodeWithTag("error-saved:0", useUnmergedTree = true).assertIsSelected()
        composeTestRule.onNodeWithTag("error-card:1").assertDoesNotExist()
        text(R.string.errors_show_all).performClick()
        scrollToError(25)
        composeTestRule.onNodeWithText(context.getString(R.string.errors_blink_code, 29)).assertIsDisplayed()
        assertEquals(ErrorsSession.ERROR_COUNT, context.resources.getStringArray(R.array.errors_ecu_errors_names).size)
        assertEquals(ErrorsSession.ERROR_COUNT, context.resources.getIntArray(R.array.errors_ecu_blink_codes).size)
    }

    @Test
    fun draftKeepsSavedBadgeAndRequiresConfirmationToWrite() {
        var snapshot by mutableStateOf(ErrorsSnapshot(savedMask = 1))
        var confirmation by mutableStateOf<ErrorsConfirmation?>(null)
        var writes = 0
        composeTestRule.setContent {
            SecuComposeTheme {
                Content(
                    snapshot,
                    onEdit = { snapshot = snapshot.copy(draftMask = snapshot.savedMask) },
                    onSetAll = { selected -> snapshot = snapshot.copy(draftMask = if (selected) (1 shl ErrorsSession.ERROR_COUNT) - 1 else 0) },
                    onDraft = { bit, selected ->
                        assertEquals(0, bit)
                        snapshot = snapshot.copy(draftMask = if (selected) 1 else 0)
                    },
                    onWrite = { confirmation = ErrorsConfirmation.Write },
                )
                ErrorsConfirmationDialog(confirmation, { writes++; confirmation = null }, { confirmation = null })
            }
        }

        text(R.string.errors_edit_saved).performClick()
        text(R.string.errors_write_saved).assertIsNotEnabled()
        text(R.string.errors_select_all).performClick()
        text(R.string.errors_deselect_all).performClick()
        scrollToError(0)
        val draft = composeTestRule.onNodeWithTag("error-draft:0")
        draft.assertIsOff()
        composeTestRule.onNodeWithTag("error-saved:0", useUnmergedTree = true).assertIsSelected()
        draft.performClick()
        draft.assertIsOn()
        text(R.string.errors_write_saved).assertIsNotEnabled()
        draft.performClick()
        text(R.string.errors_write_saved).performClick()
        text(R.string.errors_confirm_write).assertIsDisplayed()
        composeTestRule.runOnIdle { assertEquals(0, writes) }
        composeTestRule.onNodeWithText(context.getString(android.R.string.ok)).performClick()
        composeTestRule.runOnIdle { assertEquals(1, writes) }
    }

    @Test
    fun uncertainWriteAllowsRefreshAndDisablesMoreWrites() {
        composeTestRule.setContent {
            Content(ErrorsSnapshot(savedMask = 1, writeStatus = ErrorsWriteStatus.AckTimeout))
        }

        text(R.string.errors_write_ack_timeout).assertIsDisplayed()
        text(R.string.errors_read_saved).assertIsEnabled()
        text(R.string.errors_edit_saved).assertIsNotEnabled()
        composeTestRule.onNodeWithTag("errors-clear-button").assertIsNotEnabled()
    }

    @Test
    fun clearingHistoryRequiresItsOwnConfirmation() {
        var confirmation by mutableStateOf<ErrorsConfirmation?>(null)
        var clears = 0
        composeTestRule.setContent {
            Content(
                ErrorsSnapshot(savedMask = 1),
                onClear = { confirmation = ErrorsConfirmation.Clear },
            )
            ErrorsConfirmationDialog(confirmation, { clears++; confirmation = null }, { confirmation = null })
        }

        composeTestRule.onNodeWithTag("errors-clear-button").performClick()
        text(R.string.dialog_confirm_clear_errors_message).assertIsDisplayed()
        composeTestRule.runOnIdle { assertEquals(0, clears) }
        composeTestRule.onNodeWithText(context.getString(android.R.string.cancel)).performClick()
        composeTestRule.runOnIdle { assertEquals(0, clears) }
        composeTestRule.onNodeWithTag("errors-clear-button").performClick()
        composeTestRule.onNodeWithText(context.getString(android.R.string.ok)).performClick()
        composeTestRule.runOnIdle { assertEquals(1, clears) }
    }

    @Test
    fun disconnectHidesCurrentDataAndDisablesClear() {
        var connection: ConnectionState by mutableStateOf(Connected)
        composeTestRule.setContent {
            Content(ErrorsSnapshot(activeMask = 1, savedMask = 1), connection)
        }

        composeTestRule.onNodeWithTag("errors-clear-button").assertIsEnabled()
        scrollToError(0)
        composeTestRule.runOnIdle { connection = Disconnected }
        composeTestRule.onNodeWithTag("error-card:0").assertDoesNotExist()
        composeTestRule.onNodeWithTag("errors-clear-button").assertIsNotEnabled()
    }

    @Test
    fun modeAndSettingsControlsForwardIndependentActions() {
        var live: Boolean? = null
        var hold: Boolean? = null
        var autoRead: Boolean? = null
        composeTestRule.setContent {
            Content(
                ErrorsSnapshot(autoReadSavedErrors = true),
                onLive = { live = it },
                onHold = { hold = it },
                onAutoRead = { autoRead = it },
            )
        }

        text(R.string.errors_live_mode).performClick()
        composeTestRule.onNodeWithContentDescription(context.getString(R.string.errors_settings)).performClick()
        text(R.string.errors_hold_active).performClick()
        text(R.string.errors_auto_read_saved).performClick()
        composeTestRule.runOnIdle {
            assertEquals(false, live)
            assertEquals(true, hold)
            assertEquals(false, autoRead)
        }
    }

    @Test
    fun helpIsAvailableWithoutAnEcuConnection() {
        composeTestRule.setContent { Content(ErrorsSnapshot(), Disconnected) }

        composeTestRule.onNodeWithContentDescription(context.getString(R.string.errors_help)).performClick()
        text(R.string.errors_help_text).assertIsDisplayed()
        text(R.string.close).performClick()
        text(R.string.errors_help_text).assertDoesNotExist()
    }

    private fun text(id: Int) = composeTestRule.onNodeWithText(context.getString(id))

    private fun scrollToError(bit: Int) {
        composeTestRule.onNodeWithTag("errors-list").performScrollToNode(hasTestTag("error-card:$bit"))
    }

    @Composable
    private fun Content(
        snapshot: ErrorsSnapshot,
        connection: ConnectionState = Connected,
        onClear: () -> Unit = {},
        onLive: (Boolean) -> Unit = {},
        onHold: (Boolean) -> Unit = {},
        onAutoRead: (Boolean) -> Unit = {},
        onRetryActive: () -> Unit = {},
        onReadSaved: () -> Unit = {},
        onEdit: () -> Unit = {},
        onSetAll: (Boolean) -> Unit = {},
        onDraft: (Int, Boolean) -> Unit = { _, _ -> },
        onWrite: () -> Unit = {},
    ) {
        SecuComposeTheme {
            ErrorsContent(
                connectionState = connection,
                snapshot = snapshot,
                onBackClick = {},
                onOpenLtft = {},
                onClearClick = onClear,
                onLiveChange = onLive,
                onHoldChange = onHold,
                onAutoReadChange = onAutoRead,
                onReadSaved = onReadSaved,
                onRetryActive = onRetryActive,
                onEditSaved = onEdit,
                onDraftChange = onDraft,
                onSetAllDraft = onSetAll,
                onDiscardDraft = {},
                onWriteDraft = onWrite,
            )
        }
    }
}
