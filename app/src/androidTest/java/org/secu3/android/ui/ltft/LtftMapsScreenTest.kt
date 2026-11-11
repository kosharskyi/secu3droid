package org.secu3.android.ui.ltft

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.secu3.android.R
import org.secu3.android.connection.Connected
import org.secu3.android.connection.ConnectionState
import org.secu3.android.connection.Disconnected
import org.secu3.android.domain.ltft.LtftMapsState
import org.secu3.android.domain.ltft.LtftWorkingPoint
import org.secu3.android.domain.ltft.LtftWriteOperation
import org.secu3.android.domain.ltft.LtftWriteState
import org.secu3.android.ui.compose.SecuComposeTheme

class LtftMapsScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun selectsEachTableAndRefreshesTheCurrentRead() {
        var refreshes = 0
        compose.setContent { TestScreen(onRefresh = { refreshes++ }) }

        compose.onNodeWithText(context.getString(R.string.ltft_maps_table, 1)).assertIsSelected()
        compose.onNodeWithText(decimal(12.5f)).assertIsDisplayed()
        compose.onNodeWithText(context.getString(R.string.ltft_maps_table, 2)).performClick()
        compose.onNodeWithText(context.getString(R.string.ltft_maps_table, 2)).assertIsSelected()
        compose.onNodeWithText(decimal(-7.5f)).assertIsDisplayed()
        compose.onNodeWithText(decimal(12.5f)).assertDoesNotExist()
        cell(15, 0).performClick()
        compose.onNodeWithContentDescription(context.getString(R.string.refresh)).performClick()
        cell(15, 0).assertIsNotSelected()
        compose.runOnIdle { assertEquals(1, refreshes) }
    }

    @Test
    fun axesStayVisibleWhileScrollingTheMap() {
        compose.setContent {
            Box(Modifier.height(400.dp)) {
                TestScreen()
            }
        }

        val table = compose.onNodeWithTag("ltft-map-table")
        repeat(5) { table.performTouchInput { swipeLeft() } }
        compose.onNodeWithText("8000").assertIsDisplayed()
        compose.onNodeWithText(decimal(95f)).assertIsDisplayed()

        repeat(5) { table.performTouchInput { swipeUp() } }
        compose.onNodeWithText("8000").assertIsDisplayed()
        compose.onNodeWithText(decimal(20f)).assertIsDisplayed()
    }

    @Test
    fun showsOperatingPointAfterBarometricAxisArrivesAndHidesDataOnDisconnect() {
        var state by mutableStateOf(ready().copy(loadAxis = null, workingPoint = null))
        var connection by mutableStateOf<ConnectionState>(Connected)
        val point = LtftWorkingPoint(500, 95f, 0f, 15f)
        val pointText = context.getString(
            R.string.ltft_maps_operating_point,
            500,
            context.getString(R.string.units_rpm),
            decimal(95f),
            context.getString(R.string.units_pressure_kpa),
        )
        compose.setContent { TestScreen(state = state, connection = connection) }

        text(R.string.ltft_maps_waiting_baro).assertIsDisplayed()
        compose.onNodeWithContentDescription(pointText).assertDoesNotExist()
        compose.runOnIdle { state = ready().copy(workingPoint = point) }
        text(R.string.ltft_maps_waiting_baro).assertDoesNotExist()
        compose.onNodeWithContentDescription(pointText).assertIsDisplayed()
        compose.runOnIdle { connection = Disconnected }
        text(R.string.ltft_maps_disconnected).assertIsDisplayed()
        compose.onNodeWithTag("ltft-map-table").assertDoesNotExist()
    }

    @Test
    fun showsReadStatesAndOffersRetryOnTimeout() {
        var state by mutableStateOf<LtftMapsState>(LtftMapsState.Unsupported)
        var retries = 0
        compose.setContent { TestScreen(state = state, onRefresh = { retries++ }) }

        text(R.string.ltft_maps_unsupported).assertIsDisplayed()
        text(R.string.retry).assertDoesNotExist()
        compose.runOnIdle { state = LtftMapsState.Reading }
        text(R.string.ltft_maps_reading).assertIsDisplayed()
        compose.runOnIdle { state = LtftMapsState.TimedOut.Read }
        text(R.string.ltft_maps_timeout).assertIsDisplayed()
        text(R.string.retry).performClick()
        compose.runOnIdle { assertEquals(1, retries) }
    }

    @Test
    fun resetRequiresConfirmationAndDisconnectDismissesIt() {
        var connection by mutableStateOf<ConnectionState>(Connected)
        var resets = 0
        compose.setContent { TestScreen(connection = connection, onReset = { resets++ }) }
        text(R.string.ltft_maps_table, 2).performClick()
        actions().performClick()
        text(R.string.ltft_maps_reset_action).performClick()
        text(R.string.ltft_maps_reset_confirmation).assertIsDisplayed()
        text(android.R.string.cancel).performClick()
        compose.runOnIdle { assertEquals(0, resets) }

        actions().performClick()
        text(R.string.ltft_maps_reset_action).performClick()
        compose.runOnIdle { connection = Disconnected }
        text(R.string.ltft_maps_reset_confirmation).assertDoesNotExist()
        compose.runOnIdle { connection = Connected }
        text(R.string.ltft_maps_reset_confirmation).assertDoesNotExist()

        actions().performClick()
        text(R.string.ltft_maps_reset_action).performClick()
        text(android.R.string.ok).performClick()
        compose.runOnIdle { assertEquals(1, resets) }
    }

    @Test
    fun saveDisablesRefreshAndOtherActionsWhileWaitingForEcu() {
        var writeState by mutableStateOf<LtftWriteState>(LtftWriteState.Idle)
        var saves = 0
        compose.setContent {
            TestScreen(
                writeState = writeState,
                onSave = {
                    saves++
                    writeState = LtftWriteState.AwaitingAcknowledgement(LtftWriteOperation.Save)
                },
            )
        }
        actions().performClick()
        text(R.string.ltft_maps_save_action).performClick()
        text(R.string.ltft_maps_save_pending).assertIsDisplayed()
        actions().assertIsNotEnabled()
        compose.onNodeWithContentDescription(context.getString(R.string.refresh)).assertIsNotEnabled()
        compose.runOnIdle {
            assertEquals(1, saves)
            writeState = LtftWriteState.Succeeded(LtftWriteOperation.Save)
        }
        text(R.string.ltft_maps_save_success).assertIsDisplayed()
    }

    @Test
    fun retryAfterAcknowledgedResetOnlyRefreshesTheTables() {
        var state by mutableStateOf<LtftMapsState>(LtftMapsState.Reading)
        var writeState by mutableStateOf<LtftWriteState>(LtftWriteState.ReadingAfterReset)
        var refreshes = 0
        var resets = 0
        compose.setContent {
            TestScreen(state = state, writeState = writeState, onRefresh = { refreshes++ }, onReset = { resets++ })
        }
        text(R.string.ltft_maps_reading_after_reset).assertIsDisplayed()
        actions().assertDoesNotExist()
        compose.runOnIdle {
            state = LtftMapsState.TimedOut.ResetReadback
            writeState = LtftWriteState.ResetReadbackFailed
        }
        text(R.string.ltft_maps_reset_readback_failed).assertIsDisplayed()
        text(R.string.ltft_maps_timeout).assertDoesNotExist()
        text(R.string.ltft_maps_read_again).performClick()
        compose.runOnIdle {
            assertEquals(1, refreshes)
            assertEquals(0, resets)
        }
    }

    @Test
    fun refreshTimeoutShowsTheReadMessageAfterAnUnconfirmedReset() {
        var state by mutableStateOf<LtftMapsState>(LtftMapsState.TimedOut.ResetAcknowledgement)
        val writeState = LtftWriteState.Unconfirmed(LtftWriteOperation.Reset)
        var refreshes = 0
        compose.setContent { TestScreen(state = state, writeState = writeState, onRefresh = { refreshes++ }) }

        text(R.string.ltft_maps_reset_unconfirmed).assertIsDisplayed()
        text(R.string.ltft_maps_read_again).performClick()
        compose.runOnIdle {
            assertEquals(1, refreshes)
            state = ready()
        }
        text(R.string.ltft_maps_reset_unconfirmed).assertIsDisplayed()

        compose.runOnIdle { state = LtftMapsState.TimedOut.Read }
        text(R.string.ltft_maps_timeout).assertIsDisplayed()
        text(R.string.ltft_maps_reset_unconfirmed).assertDoesNotExist()
        text(R.string.retry).performClick()
        compose.runOnIdle { assertEquals(2, refreshes) }
    }

    @Test
    fun copiesSingleCellsAndRectanglesAndClearsSelectionWhenChangingTables() {
        var state by mutableStateOf(ready())
        var copiedRows: IntRange? = null
        var copiedColumns: IntRange? = null
        compose.setContent {
            TestScreen(
                state = state,
                onCopy = { _, rows, columns ->
                    copiedRows = rows
                    copiedColumns = columns
                },
            )
        }

        cell(14, 1).performTouchInput { click() }
        cell(14, 1).assertIsSelected()
        cell(15, 0).assertIsNotSelected()
        actions().performClick()
        text(R.string.ltft_maps_copy_selection).performClick()
        compose.runOnIdle {
            assertEquals(14..14, copiedRows)
            assertEquals(1..1, copiedColumns)
        }

        cell(15, 0).performClick()
        compose.runOnIdle { state = state.copy(workingPoint = LtftWorkingPoint(500, 95f, 0f, 15f)) }
        cell(15, 1).assertIsSelected()
        cell(14, 0).assertIsSelected()
        cell(15, 2).assertIsNotSelected()
        actions().performClick()
        text(R.string.ltft_maps_copy_selection).performClick()
        compose.runOnIdle {
            assertEquals(14..15, copiedRows)
            assertEquals(0..1, copiedColumns)
        }

        cell(15, 2).performClick()
        cell(15, 2).assertIsSelected()
        cell(14, 1).assertIsNotSelected()
        text(R.string.ltft_maps_table, 2).performClick()
        cell(15, 2).assertIsNotSelected()
        actions().performClick()
        text(R.string.ltft_maps_copy_selection).assertIsNotEnabled()
    }

    @Test
    fun exportsTheWholeCurrentTableWithTheChosenDecimalSeparator() {
        val state = ready()
        var exportedTable: Int? = null
        var exportedCorrections: List<Float>? = null
        var exportedRpm: List<Int>? = null
        var separator: Char? = null
        compose.setContent {
            TestScreen(
                state = state,
                onExportCsv = { table, corrections, rpm, decimalSeparator ->
                    exportedTable = table
                    exportedCorrections = corrections
                    exportedRpm = rpm
                    separator = decimalSeparator
                },
            )
        }

        text(R.string.ltft_maps_table, 2).performClick()
        cell(15, 0).performClick()
        actions().performClick()
        text(R.string.ltft_maps_export_csv).performClick()
        text(android.R.string.cancel).performClick()
        compose.runOnIdle { assertEquals(null, exportedTable) }

        actions().performClick()
        text(R.string.ltft_maps_export_csv).performClick()
        text(R.string.ltft_maps_decimal_comma).performClick()
        text(R.string.ltft_maps_export_csv).performClick()
        compose.runOnIdle {
            assertEquals(1, exportedTable)
            assertEquals(state.tables[1], exportedCorrections)
            assertEquals(state.rpmAxis, exportedRpm)
            assertEquals(',', separator)
        }
    }

    @Composable
    private fun TestScreen(
        state: LtftMapsState = ready(),
        connection: ConnectionState = Connected,
        writeState: LtftWriteState = LtftWriteState.Idle,
        onRefresh: () -> Unit = {},
        onReset: () -> Unit = {},
        onSave: () -> Unit = {},
        onCopy: (List<Float>, IntRange, IntRange) -> Unit = { _, _, _ -> },
        onExportCsv: (Int, List<Float>, List<Int>, Char) -> Unit = { _, _, _, _ -> },
    ) {
        SecuComposeTheme {
            LtftMapsScreen(state, connection, writeState, {}, onRefresh, onReset, onSave, onCopy, onExportCsv)
        }
    }

    private fun cell(row: Int, column: Int) = compose.onNodeWithTag("ltft-cell-$row-$column")

    private fun actions() = compose.onNodeWithContentDescription(context.getString(R.string.ltft_maps_actions))

    private fun text(id: Int) = compose.onNodeWithText(context.getString(id))

    private fun text(id: Int, table: Int) = compose.onNodeWithText(context.getString(id, table))

    private fun decimal(value: Float) = String.format(context.resources.configuration.locales[0], "%.1f", value)

    private fun ready() = LtftMapsState.Ready(
        tables = listOf(
            List(256) { if (it == 240) 12.5f else 0f },
            List(256) { if (it == 240) -7.5f else 0f },
        ),
        rpmAxis = List(16) { 500 * (it + 1) },
        loadAxis = List(16) { 20f + 5f * it },
        isLoadPressure = true,
    )
}
