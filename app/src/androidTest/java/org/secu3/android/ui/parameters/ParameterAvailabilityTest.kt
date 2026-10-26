package org.secu3.android.ui.parameters

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.secu3.android.models.packets.input.FirmwareInfoPacket
import org.secu3.android.domain.parameters.ParameterCapabilities
import org.secu3.android.models.packets.out.params.StarterParamPacket
import org.secu3.android.models.packets.out.params.TemperatureParamPacket
import org.secu3.android.ui.compose.SecuComposeTheme
import org.secu3.android.ui.parameters.pages.compose.*

class ParameterAvailabilityTest {
    @get:Rule val compose = createComposeRule()

    @Test fun actualFirmwareOptionsDisableAndEnableTemperatureControls() {
        val options = mutableStateOf(0)
        var edits = 0
        compose.setContent {
            SecuComposeTheme {
                TemperatureScreen(TemperatureUiState.Data(TemperatureValues.from(TemperatureParamPacket())),
                    { edits++ }, { _, _ -> }, capabilities = ParameterCapabilities(FirmwareInfoPacket(options = options.value)))
            }
        }
        compose.onNodeWithTag("temperature_flag_pwm_control-switch").assertIsNotEnabled()
        compose.onNodeWithTag("temperature_ac_on").performScrollTo().assertIsNotEnabled()
        compose.runOnIdle { options.value = (1 shl 28) or (1 shl 8) }
        compose.onNodeWithTag("temperature_ac_on").assertIsEnabled().performClick()
        compose.onNodeWithTag("temperature_flag_pwm_control-switch").performScrollTo().assertIsEnabled()
        assertEquals(1, edits)
    }

    @Test fun rereadingStarterKeepsTheSamePageScrollPosition() {
        val data = StarterUiState.Data(StarterValues.from(StarterParamPacket()))
        val state = mutableStateOf<StarterUiState>(data)
        compose.setContent { SecuComposeTheme { StarterScreen(state.value, {}, { _, _ -> }) } }
        val last = "starter_flag_row_limit_cranking_pw"
        compose.onNodeWithTag(last).performScrollTo().assertIsDisplayed()
        val before = compose.onNodeWithTag(last).fetchSemanticsNode().boundsInRoot.top
        compose.runOnIdle { state.value = StarterUiState.Loading }
        compose.onNodeWithTag("starter_loading").assertIsDisplayed()
        compose.runOnIdle { state.value = data }
        compose.onNodeWithTag(last).assertIsDisplayed()
        assertEquals(before, compose.onNodeWithTag(last).fetchSemanticsNode().boundsInRoot.top, 1f)
    }
}
