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
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.secu3.android.R
import org.secu3.android.ui.compose.SecuComposeTheme
import org.secu3.android.domain.parameters.MiscellaneousControl
import org.secu3.android.domain.parameters.MiscellaneousValues
import org.secu3.android.ui.parameters.pages.MiscToggle
import org.secu3.android.ui.parameters.pages.MiscellaneousScreen

class MiscellaneousScreenTest {
    @get:Rule val compose = createComposeRule()
    private val context get() = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
    private val initial = MiscellaneousValues(0x0103,20,true,5000,33f / 32f,65f / 32f,true,false,true,5000,175000,100 to 158500,21500 to 2_000_000,2.5f,100,200,400f,false)
    private val rates = MiscellaneousControl.BAUD_RATES.mapValues { it.value.toString() }

    @Test fun loadingScrollAndRecompositionHaveNoCallbacks() {
        var calls=0
        val state=mutableStateOf<MiscellaneousValues?>(null)
        compose.setContent { SecuComposeTheme { MiscellaneousScreen(state.value,rates,{calls++},{calls++},{calls++},{_,_->calls++}) } }
        compose.onNodeWithTag("misc-loading").assertIsDisplayed()
        compose.runOnIdle { state.value=initial }
        compose.onNodeWithTag("misc-baud-selector").assertIsDisplayed()
        compose.onNodeWithTag("misc-start-value", useUnmergedTree=true).assertTextEquals("1 ${context.getString(R.string.units_degrees_cs)}")
        compose.onNodeWithTag("misc-duration-value", useUnmergedTree=true).assertTextEquals("2 ${context.getString(R.string.units_degrees_cs)}")
        compose.onNodeWithTag("misc-vss").performScrollTo().assertIsDisplayed()
        compose.runOnIdle { state.value=initial.copy(period=30) }
        compose.onNodeWithTag("misc-period-value",useUnmergedTree=true).assertTextEquals("30 ${context.getString(R.string.units_ms)}")
        compose.runOnIdle { state.value=initial.copy(hasValidationError=true) }
        compose.onNodeWithTag("misc-validation-error").performScrollTo().assertIsDisplayed()
        assertEquals(0,calls)
    }

    @Test fun selectorAndEveryControlDispatchOnlyExplicitAction() {
        val actions=mutableListOf<String>()
        val state=mutableStateOf(initial.copy(baudDivisor=0x1234))
        compose.setContent { SecuComposeTheme { MiscellaneousScreen(state.value,rates,
            {key -> actions += "int:$key"; if(key.name=="PERIOD") state.value=state.value.copy(period=40)},
            {actions += "float"}, {code -> actions += "baud:$code"; state.value=state.value.copy(baudDivisor=code)},
            {toggle,value -> actions += "toggle:$toggle:$value"}) } }
        assertEquals(emptyList<String>(),actions)
        compose.onNodeWithText("Code 4660").assertIsDisplayed()
        compose.onNodeWithTag("misc-baud-selector").performClick()
        compose.onNodeWithTag("misc-baud-option-259", useUnmergedTree=true).performClick()
        assertEquals("baud:259",actions.last()) // 0x0103
        compose.onNodeWithText("9600").assertIsDisplayed()
        compose.onNodeWithTag("misc-period").performClick()
        compose.onNodeWithTag("misc-period-value",useUnmergedTree=true).assertTextEquals("40 ${context.getString(R.string.units_ms)}")
        listOf("cutoff","start","duration","begin","end","timeout","pwm0","pwm1","vss").forEach { key ->
            compose.onNodeWithTag("misc-$key").performScrollTo().performClick()
            if(key=="timeout") assertEquals("float",actions.last()) else assertEquals("int:${key.uppercase()}",actions.last())
        }
        compose.onNodeWithTag("misc-ign-switch").performScrollTo().performClick()
        assertEquals("toggle:${MiscToggle.IGN}:false",actions.last())
        listOf("pump" to MiscToggle.PUMP,"gas" to MiscToggle.GAS,"petrol" to MiscToggle.PETROL).forEach { (tag,toggle) ->
            compose.onNodeWithTag("misc-$tag-switch").performScrollTo().performClick()
            assertEquals("toggle:$toggle:${tag == "gas"}",actions.last())
        }
    }
}
