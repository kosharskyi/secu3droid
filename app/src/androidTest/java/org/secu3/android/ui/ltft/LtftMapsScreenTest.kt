package org.secu3.android.ui.ltft

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
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
import org.secu3.android.ui.compose.SecuComposeTheme

class LtftMapsScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun selectsEachTableAndRefreshesTheCurrentRead() {
        var refreshes = 0
        compose.setContent {
            SecuComposeTheme { LtftMapsScreen(ready(), Connected, {}, { refreshes++ }) }
        }

        compose.onNodeWithText(context.getString(R.string.ltft_maps_table, 1)).assertIsSelected()
        compose.onNodeWithText(decimal(12.5f)).assertIsDisplayed()
        compose.onNodeWithText(context.getString(R.string.ltft_maps_table, 2)).performClick()
        compose.onNodeWithText(context.getString(R.string.ltft_maps_table, 2)).assertIsSelected()
        compose.onNodeWithText(decimal(-7.5f)).assertIsDisplayed()
        compose.onNodeWithText(decimal(12.5f)).assertDoesNotExist()
        compose.onNodeWithContentDescription(context.getString(R.string.refresh)).performClick()
        compose.runOnIdle { assertEquals(1, refreshes) }
    }

    @Test
    fun axesStayVisibleWhileScrollingTheMap() {
        compose.setContent {
            SecuComposeTheme {
                Box(Modifier.height(400.dp)) { LtftMapsScreen(ready(), Connected, {}, {}) }
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
        compose.setContent {
            SecuComposeTheme { LtftMapsScreen(state, connection, {}, {}) }
        }

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
        compose.setContent {
            SecuComposeTheme { LtftMapsScreen(state, Connected, {}, { retries++ }) }
        }

        text(R.string.ltft_maps_unsupported).assertIsDisplayed()
        text(R.string.retry).assertDoesNotExist()
        compose.runOnIdle { state = LtftMapsState.Reading }
        text(R.string.ltft_maps_reading).assertIsDisplayed()
        compose.runOnIdle { state = LtftMapsState.TimedOut }
        text(R.string.ltft_maps_timeout).assertIsDisplayed()
        text(R.string.retry).performClick()
        compose.runOnIdle { assertEquals(1, retries) }
    }

    private fun text(id: Int) = compose.onNodeWithText(context.getString(id))

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
