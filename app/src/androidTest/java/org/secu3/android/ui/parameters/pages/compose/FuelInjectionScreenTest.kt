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

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.secu3.android.models.packets.out.params.InjctrParPacket
import org.secu3.android.models.packets.input.FirmwareInfoPacket
import org.secu3.android.domain.parameters.FuelBankValues
import org.secu3.android.domain.parameters.FuelInjectionValues
import org.secu3.android.domain.parameters.ParameterCapabilities
import org.secu3.android.ui.compose.SecuComposeTheme

class FuelInjectionScreenTest {
    @get:Rule val compose = createComposeRule()
    private val labels = InjctrParPacket.InjConfig.entries.associate { it.id to it.name }
    private fun bank(config: Int = 1, pulses: Int = 2, map: Boolean = false) = FuelBankValues(
        300f, config, pulses, 10f, 5f, map, 0, 1f, 10f, listOf(0, 1, 2, 3, 5, 4), InjctrParPacket(ckpsEngineCyl = 4).validPulses(config))
    private fun data() = FuelInjectionValues(2f, listOf(bank(), bank(4, 4, true)), 120f, true, false, true, false, true, 0.2f, 0.3f)
    private fun show(state: FuelInjectionValues?, capabilities: ParameterCapabilities = ParameterCapabilities.All,
                     configs: (Int, Int) -> Unit = { _, _ -> }, pulses: (Int, Int) -> Unit = { _, _ -> }) {
        compose.setContent { SecuComposeTheme { FuelInjectionScreen(state, labels, {}, configs, pulses, { _, _ -> }, { _, _ -> }, capabilities) } }
    }

    @Test fun loadingIsCenteredAndDoesNotExposeEditableControls() {
        show(null)
        compose.onNodeWithTag("fuel_injection_loading").assertIsDisplayed()
        compose.onNodeWithTag("inj-engine-displacement").assertDoesNotExist()
    }

    @Test fun bothBanksAndConditionalTimingControlsRender() {
        show(data())
        compose.onNodeWithTag("inj-flow-0").assertIsDisplayed()
        compose.onNodeWithTag("inj-flow-1").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("inj-use-map-1").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("inj-timing-1").assertIsNotEnabled()
        compose.onNodeWithTag("inj-cranking-1").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("inj-angle-spec-1").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("inj-use-map-0-switch").assertIsDisplayed()
    }

    @Test fun secu3tDisablesDesktopRestrictedGeneralFlags() {
        show(data(), ParameterCapabilities(FirmwareInfoPacket(options = (1 shl 17) or (1 shl 28))))
        compose.onNodeWithTag("inj-additional-corrections-switch").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithTag("inj-differential-pressure-switch").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithTag("inj-switch-rows-switch").performScrollTo().assertIsNotEnabled()
    }

    @Test fun firmwareWithoutInjectionShowsUnavailableMessage() {
        show(data(), ParameterCapabilities(FirmwareInfoPacket(options = 0)))
        compose.onNodeWithTag("fuel_injection_disabled").assertIsDisplayed()
        compose.onNodeWithTag("inj-engine-displacement").assertDoesNotExist()
    }

    @Test fun configurationAndPulseDropdownsSendUiValues() {
        val selected = mutableListOf<Pair<Int, Int>>()
        show(data(), configs = { bank, value -> selected += bank to value }, pulses = { bank, value -> selected += bank to value })
        compose.onNodeWithTag("inj-config-0").performClick()
        compose.onNodeWithTag("inj-choice-0-0-3").performClick()
        compose.onNodeWithTag("inj-pulses-0").performScrollTo().performClick()
        compose.onNodeWithTag("inj-choice-0-1-4").performClick()
        assert(selected.contains(0 to 3))
        assert(selected.contains(0 to 4))
    }

    @Test fun longScreenCanScrollToGeneralSettings() {
        show(data())
        compose.onNode(hasTestTag("inj-increase-rate")).performScrollTo().assertIsDisplayed()
    }

    @Test fun wholeDegreeDisplayUsesRoundToIntWithoutQuantizingRawDomainValues() {
        val values = data().copy(banks = listOf(
            bank().copy(timing = -1.5f, cranking = -.03125f),
            bank(),
        ))
        show(values)
        compose.onNodeWithTag("inj-timing-0-value", useUnmergedTree = true).assertTextEquals("-1 °")
        compose.onNodeWithTag("inj-cranking-0-value", useUnmergedTree = true).assertTextEquals("0 °")
        assertEquals(-1.5f, values.banks[0].timing, 0f)
        assertEquals(-.03125f, values.banks[0].cranking, 0f)
    }
}
