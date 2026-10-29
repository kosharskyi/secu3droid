/*
 *    SecuDroid  - An open source, free manager for SECU-3 engine control unit
 *    Copyright (C) 2026 Vitalii O. Kosharskyi. Ukraine, Kyiv
 *
 *    SECU-3  - An open source, free engine control unit
 *    Copyright (C) 2007-2026 Alexey A. Shabelnikov. Ukraine, Kyiv
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *    contacts:
 *                    http://secu-3.org
 *                    email: vetalkosharskiy@gmail.com
 */
package org.secu3.android.ui.parameters.pages.compose

import androidx.compose.runtime.mutableStateOf
import org.secu3.android.domain.parameters.IdlingValues
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.secu3.android.ui.compose.SecuComposeTheme
import org.secu3.android.domain.parameters.ParameterCapabilities
import org.secu3.android.models.packets.input.FirmwareInfoPacket

class IdlingScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val context
        get() = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext

    private fun values(throttleAssistMap: Boolean = false) = IdlingValues(
        iFac1 = 1f, iFac2 = 1f, maxAngle = 1f, minAngle = 1f, targetRpm = 1,
        deadBandRpm = 1, turnOnTemperature = 1f, iacAddAfterExit = 1f, rpmAddOnRun = 1,
        proportionalNegative = 1.25f, integralNegative = 3.25f, proportionalPositive = 2.25f,
        integralPositive = 4.25f, transientThreshold1 = 1.2f, transientThreshold2 = 1.3f,
        integratorRpmLimit = 1, mapThreshold = 1f, iacMinimumPosition = 1f,
        iacMaximumPosition = 1f, iacDeadBandRpm = 1, differential = 1f,
        rigidityLoad = 1f, rigidityRpm = 1f, loadThreshold = 1f,
        regulatorEnabled = false, regulatorOnGas = false, proportionalMode = false,
        closedLoop = false, throttleAssistMap = throttleAssistMap, closedLoopOnGas = false,
        worksWithIacRegulator = false,
    )

    @Test
    fun firmwareAndCurrentThrottleModeGateOnlyTheirDependentFields() {
        compose.setContent {
            SecuComposeTheme {
                IdlingScreen(
                    values(throttleAssistMap = true),
                    {},
                    { _, _ -> },
                    capabilities = ParameterCapabilities(FirmwareInfoPacket(options = 1 shl 28)),
                )
            }
        }

        compose.onNodeWithTag("idling_iac_add").assertIsNotEnabled()
        compose.onNodeWithTag("idling_ifac1").assertIsEnabled()
        compose.onNodeWithTag("idling_p0").assertIsEnabled()
    }

    @Test
    fun loadingDataScrollAndRecompositionHaveNoCallbacks() {
        var editCount = 0
        var flagChangeCount = 0
        val state = mutableStateOf<IdlingValues?>(null)

        compose.setContent {
            SecuComposeTheme {
                IdlingScreen(state.value, { editCount++ }, { _, _ -> flagChangeCount++ })
            }
        }
        compose.onNodeWithTag("idling_loading").assertIsDisplayed()

        val current = values()
        compose.runOnIdle { state.value = current }

        IdlingNumber.entries.forEach { number ->
            compose.onNodeWithTag("idling_${number.name.lowercase()}")
                .performScrollTo()
                .assertIsDisplayed()
            val units = if (number.unitRes == 0) "" else " ${context.getString(number.unitRes)}"
            val expected = String.format(
                java.util.Locale.getDefault(),
                "%.${number.precision}f",
                number.value(current),
            ) + units
            compose.onNodeWithTag("idling_${number.name.lowercase()}-value", useUnmergedTree = true)
                .assertTextEquals(expected)
        }
        IdlingFlag.entries.forEach { flag ->
            compose.onNodeWithTag("idling_flag_${flag.name.lowercase()}")
                .performScrollTo()
                .assertIsDisplayed()
        }

        compose.runOnIdle {
            state.value = current.copy(iFac1 = current.iFac1)
        }
        assertEquals(0, editCount)
        assertEquals(0, flagChangeCount)
    }

    @Test
    fun everyValueAndFlagInvokesItsUserActionCallback() {
        val edits = mutableListOf<IdlingNumber>()
        val flagChanges = mutableListOf<Pair<IdlingFlag, Boolean>>()
        val current = values()
        compose.setContent {
            SecuComposeTheme {
                IdlingScreen(
                    current,
                    { edits += it },
                    { flag, checked -> flagChanges += flag to checked },
                )
            }
        }

        IdlingNumber.entries.forEach { number ->
            compose.onNodeWithTag("idling_${number.name.lowercase()}")
                .performScrollTo()
                .performClick()
            assertEquals(number, edits.last())
        }
        IdlingFlag.entries.forEach { flag ->
            compose.onNodeWithTag("idling_flag_${flag.name.lowercase()}-switch")
                .performScrollTo()
                .performClick()
            assertEquals(flag to true, flagChanges.last())
        }
    }
}
