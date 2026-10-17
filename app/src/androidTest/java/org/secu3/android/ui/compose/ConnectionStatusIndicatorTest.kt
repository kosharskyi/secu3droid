package org.secu3.android.ui.compose

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.secu3.android.R
import org.secu3.android.connection.Connected
import org.secu3.android.connection.ConnectionState
import org.secu3.android.connection.Disconnected
import org.secu3.android.connection.InProgress

class ConnectionStatusIndicatorTest {
    @get:Rule val compose = createComposeRule()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun statusUsesFixedIconSlotAndLocalizedAccessibilityLabelAtLargeFontScale() {
        val state = mutableStateOf<ConnectionState?>(Connected)
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(density = 1f, fontScale = 2f)) {
                ConnectionStatusIndicator(state.value, testTag = "toolbar-status")
            }
        }

        compose.onNodeWithContentDescription(context.getString(R.string.home_connection_connected)).assertExists()
        compose.runOnIdle { state.value = InProgress }
        compose.onNodeWithContentDescription(context.getString(R.string.home_connection_connecting)).assertExists()
        compose.runOnIdle { state.value = Disconnected }
        compose.onNodeWithContentDescription(context.getString(R.string.home_connection_disconnected)).assertExists()
        compose.runOnIdle { state.value = null }
        compose.onNodeWithContentDescription(context.getString(R.string.connection_status_unknown)).assertExists()
        compose.onNodeWithText(context.getString(R.string.connection_status_unknown)).assertDoesNotExist()
        val statusBounds = compose.onNodeWithTag("toolbar-status", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        assertEquals(48f, statusBounds.width, 0f)
        assertEquals(48f, statusBounds.height, 0f)
    }

    @Test
    fun nullConnectionStateHasDistinctNoDataLabel() {
        compose.setContent { SecuComposeTheme { ConnectionStatusIndicator(null, testTag = "toolbar-status") } }

        compose.onNodeWithContentDescription(context.getString(R.string.connection_status_unknown)).assertExists()
        compose.onNodeWithContentDescription(context.getString(R.string.home_connection_disconnected)).assertDoesNotExist()
    }

    @Test
    fun longPressShowsFullLocalizedStatusTooltip() {
        val connectedLabel = context.getString(R.string.home_connection_connected)
        compose.setContent { SecuComposeTheme { ConnectionStatusIndicator(Connected, testTag = "toolbar-status") } }

        compose.onNodeWithText(connectedLabel).assertDoesNotExist()
        compose.onNodeWithTag("toolbar-status", useUnmergedTree = true).performTouchInput { longClick() }
        compose.onNodeWithText(connectedLabel).assertIsDisplayed()
    }

    @Test
    fun narrowLargeFontToolbarKeepsTitleSettingsAndStatusInSeparateSlots() {
        val title = "A deliberately long destination title"
        var settingsCalls = 0
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(density = 1f, fontScale = 2f)) {
                SecuComposeTheme {
                    Box(Modifier.width(320.dp)) {
                        SecuTopAppBar(
                            title = title,
                            connectionState = Connected,
                            onNavigateUp = {},
                            actions = {
                                IconButton(
                                    onClick = { settingsCalls++ },
                                    modifier = Modifier.testTag("toolbar-settings"),
                                ) {
                                    Icon(Icons.Default.Settings, contentDescription = "Settings")
                                }
                            },
                        )
                    }
                }
            }
        }

        compose.onNodeWithText(title).assertIsDisplayed()
        compose.onNodeWithContentDescription(context.getString(R.string.home_connection_connected)).assertExists()
        val titleBounds = compose.onNodeWithText(title).fetchSemanticsNode().boundsInRoot
        val settingsBounds = compose.onNodeWithTag("toolbar-settings").fetchSemanticsNode().boundsInRoot
        val statusBounds = compose.onNodeWithTag("connection-status-indicator", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        assertEquals(48f, statusBounds.width, 0f)
        org.junit.Assert.assertTrue(titleBounds.right <= settingsBounds.left)
        org.junit.Assert.assertTrue(settingsBounds.right <= statusBounds.left)
        org.junit.Assert.assertTrue(statusBounds.right <= 320f)
        compose.onNodeWithTag("toolbar-settings").performClick()
        assertEquals(1, settingsCalls)
    }
}
