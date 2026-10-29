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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.secu3.android.domain.parameters.KnockValues
import org.secu3.android.models.packets.out.params.KnockParamPacket
import org.secu3.android.ui.compose.SecuComposeTheme

class KnockScreenTest {
    @get:Rule val compose = createComposeRule()
    private val context get() = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext

    private fun formattedDegrees(value: Float) = String.format(java.util.Locale.getDefault(), "%.2f", value) + " " +
        context.getString(org.secu3.android.R.string.units_degrees_cs)

    @Test fun loadingAndDisplayDoesNotInvokeCallbacksOnCompositionOrRecomposition() {
        var calls = 0
        val state = mutableStateOf<KnockValues?>(null)
        val bpf = listOf("1 kHz", "2 kHz")
        val integration = listOf("10 ms", "20 ms")
        compose.setContent { SecuComposeTheme { KnockScreen(state.value, bpf, integration, {calls++}, {calls++}, {_,_->calls++}, {_,_->calls++}) } }
        val indicator = compose.onNodeWithTag("knock_loading").assertIsDisplayed()
        indicator.assertWidthIsEqualTo(40.dp).assertHeightIsEqualTo(40.dp)
        val viewport = compose.onNodeWithTag("parameter-loading-container").fetchSemanticsNode().boundsInRoot
        val indicatorBounds = indicator.fetchSemanticsNode().boundsInRoot
        assertEquals(viewport.center.x, indicatorBounds.center.x, 0.5f)
        assertEquals(viewport.center.y, indicatorBounds.center.y, 0.5f)
        val packet=KnockParamPacket(kWndBeginAngle=12.5f, selectedChanels=0xA5, useKnockChannel=5)
        val data=KnockValues.from(packet)
        compose.runOnIdle { state.value=data }
        compose.onNodeWithTag("knock_begin-value", useUnmergedTree=true).assertTextEquals(formattedDegrees(12.5f))
        compose.onNodeWithTag("knock_channel_1").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("knock_channel_8").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("knock_selector_bpf").performScrollTo().assertIsDisplayed()
        compose.runOnIdle { state.value=data }
        assertEquals(0,calls)
    }

    @Test fun explicitActionsUpdateImmutableSnapshotAndVisibleValues() {
        val actions = mutableListOf<String>()
        val packet = KnockParamPacket(kWndBeginAngle = 12.5f, selectedChanels = 0xA5)
        val bpfOptions = listOf("1 kHz", "2 kHz")
        val integrationOptions = listOf("10 ms", "20 ms")
        val state = mutableStateOf<KnockValues?>(KnockValues.from(packet))
        compose.setContent {
            SecuComposeTheme {
                KnockScreen(state.value, bpfOptions, integrationOptions, { number ->
                    actions += "edit:$number"
                    packet.kWndBeginAngle = 22.25f
                    state.value = KnockValues.from(packet)
                }, { checked ->
                    actions += "enable:$checked"
                    packet.useKnockChannel = if (checked) 1 else 0
                    state.value = KnockValues.from(packet)
                }, { index, checked ->
                    actions += "channel:$index:$checked"
                    packet.selectKnockChanel(index, checked)
                    state.value = KnockValues.from(packet)
                }, { selector, index ->
                    actions += "selector:$selector:$index"
                    if (selector == KnockSelector.BPF) packet.bpfFrequency = index else packet.intTimeCost = index
                    state.value = KnockValues.from(packet)
                })
            }
        }
        assertEquals(emptyList<String>(), actions)

        compose.onNodeWithTag("knock_begin").performClick()
        compose.onNodeWithTag("knock_begin-value", useUnmergedTree = true).assertTextEquals(formattedDegrees(22.25f))
        assertEquals("edit:BEGIN", actions.last())

        compose.onNodeWithTag("knock_channel_1").performScrollTo()
        compose.onNodeWithTag("knock_channel_checkbox_1").performClick()
        compose.onNodeWithTag("knock_channel_checkbox_1").assertIsOff()
        assertEquals("channel:0:false", actions.last())

        compose.onNodeWithTag("knock_selector_bpf").performScrollTo().performClick()
        compose.onNodeWithTag("knock_selector_option_1").performClick()
        compose.onNodeWithTag("knock_selector_bpf").assertTextContains("2 kHz", substring = true)
        assertEquals("selector:BPF:1", actions.last())

        KnockNumber.entries.forEach { number ->
            compose.onNodeWithTag("knock_${number.name.lowercase()}").performScrollTo().performClick()
        }
        compose.onNodeWithTag("knock_enable-switch").performClick()
        for (i in 0..7) {
            compose.onNodeWithTag("knock_channel_${i + 1}").performScrollTo()
            compose.onNodeWithTag("knock_channel_checkbox_${i + 1}").performClick()
        }
        compose.onNodeWithTag("knock_selector_integration").performScrollTo().performClick()
        compose.onNodeWithTag("knock_selector_option_1").performClick()
        assertEquals("selector:INTEGRATION:1", actions.last())
    }

    @Test fun resourceOptionCountsMatchProtocolCodeRangesAndUnknownCodesStayVisibleOnCancel() {
        val bpfOptions = context.resources.getStringArray(org.secu3.android.R.array.knock_filter_frequencies).toList()
        val integrationOptions = context.resources.getStringArray(org.secu3.android.R.array.knock_integration_timeouts).toList()
        assertEquals(64, bpfOptions.size)
        assertEquals(32, integrationOptions.size)
        assertTrue(bpfOptions.first().isNotBlank())
        assertTrue(integrationOptions.first().isNotBlank())

        var selections = 0
        val values = KnockValues.from(KnockParamPacket(bpfFrequency = 64, intTimeCost = 32))
        compose.setContent {
            SecuComposeTheme {
                KnockScreen(values, bpfOptions, integrationOptions, {}, {}, { _, _ -> }, { _, _ -> selections++ })
            }
        }
        compose.onNodeWithTag("knock_selector_bpf").performScrollTo().assertTextContains("Code 64")
        compose.onNodeWithTag("knock_selector_integration").performScrollTo().assertTextContains("Code 32")

        compose.onNodeWithTag("knock_selector_bpf").performClick()
        compose.onNodeWithText(context.getString(android.R.string.cancel)).performClick()
        compose.onNodeWithTag("knock_selector_bpf").assertTextContains("Code 64")
        assertEquals(0, selections)
    }

}
