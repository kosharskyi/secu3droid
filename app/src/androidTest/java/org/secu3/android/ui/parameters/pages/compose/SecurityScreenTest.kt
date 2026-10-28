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
 *    contacts: http://secu-3.org, vetalkosharskiy@gmail.com
 */
package org.secu3.android.ui.parameters.pages.compose

import androidx.compose.ui.test.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.runtime.mutableStateOf
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.secu3.android.R
import org.secu3.android.ui.compose.SecuComposeTheme
import org.secu3.android.ui.parameters.pages.SecurityScreen
import org.secu3.android.domain.parameters.SecurityValues

class SecurityScreenTest {
    @get:Rule val compose = createComposeRule()
    private val types = listOf("BC417", "BK3231", "BK3231S", "BC352", "BK3432", "BK3431S")
    private val targetContext get() = InstrumentationRegistry.getInstrumentation().targetContext
    private fun isKeyValid(value: String) = value.length == 12 && value.all { it.digitToIntOrNull(16) != null }

    @Test fun loadingAndRenderingDoNotEmitChanges() {
        var calls = 0
        compose.setContent { SecuComposeTheme { SecurityScreen(null,"","","","",types,false,"",::isKeyValid,{calls++},{calls++},{calls++},{calls++},{calls++},{calls++},{calls++},{calls++},{calls++}) } }
        val indicator = compose.onNodeWithTag("security-loading").assertIsDisplayed()
        indicator.assertWidthIsEqualTo(40.dp).assertHeightIsEqualTo(40.dp)
        val viewport = compose.onNodeWithTag("parameter-loading-container").fetchSemanticsNode().boundsInRoot
        val indicatorBounds = indicator.fetchSemanticsNode().boundsInRoot
        assertEquals(viewport.center.x, indicatorBounds.center.x, 0.5f)
        assertEquals(viewport.center.y, indicatorBounds.center.y, 0.5f)
        compose.waitForIdle()
        assertEquals(0, calls)
    }

    @Test fun explicitTypeTogglesKeysCredentialsAndShowsReadonlyFlagValues() {
        val actions = mutableListOf<String>()
        val name = mutableStateOf(""); val pass = mutableStateOf(""); val expanded = mutableStateOf(false)
        compose.setContent { SecuComposeTheme { SecurityScreen(SecurityValues(true,false,0,true,false,"010203040506","AABBCCDDEEFF"),name.value,pass.value,"010203040506","AABBCCDDEEFF",types,expanded.value,"",::isKeyValid,
            {expanded.value = it},{actions += "type"},{actions += "bt"},{actions += "imm"},{name.value = it; actions += "name"},{pass.value = it; actions += "pass"},{actions += "key1"},{actions += "key2"},{actions += "apply"; name.value = ""; pass.value = ""}) } }
        assertEquals(emptyList<String>(), actions)
        compose.onNodeWithTag("security-credentials-hint").assertTextEquals(targetContext.getString(R.string.sec_credentials_write_only))
        assertEquals(emptyList<String>(), actions)
        compose.onNodeWithTag("security-name").performTextInput("ecu")
        compose.onNodeWithTag("security-password").performTextInput("1234")
        compose.onNodeWithTag("security-apply").performClick()
        compose.runOnIdle { assertEquals("", name.value); assertEquals("", pass.value) }
        compose.onNodeWithTag("security-type").performClick()
        compose.onNodeWithText("BK3432").performClick()
        compose.onNodeWithTag("security-key1").performTextClearance()
        compose.onNodeWithTag("security-key1").performTextInput("112233445566")
        compose.onNodeWithTag("security-key2").performTextClearance()
        compose.onNodeWithTag("security-key2").performTextInput("AABBCCDDEEFF")
        compose.onNodeWithTag("security-key1").assertIsEnabled()
        compose.onNodeWithTag("security-bt-switch").performClick()
        compose.onNodeWithTag("security-imm-switch").performClick()
        compose.onNodeWithTag("security-reserve-readonly-value").performScrollTo().assertTextEquals(targetContext.getString(R.string.sec_enabled)).assert(hasNoClickAction())
        compose.onNodeWithTag("security-reserve-readonly-description").assertTextEquals(targetContext.getString(R.string.sec_reserve_params_description))
        compose.onNodeWithTag("security-crc-readonly-value").performScrollTo().assertTextEquals(targetContext.getString(R.string.sec_disabled)).assert(hasNoClickAction())
        compose.onNodeWithTag("security-crc-readonly-description").assertTextEquals(targetContext.getString(R.string.sec_firmware_integrity_description))
        assertEquals(listOf("name","pass","apply","type","key1","key2","bt","imm"), actions.distinct())
    }

    @Test fun malformedCompleteKeyShowsLocalizedErrorHint() {
        val key = androidx.compose.runtime.mutableStateOf("")
        compose.setContent { SecuComposeTheme { SecurityScreen(SecurityValues(false,false,0,false,false,"",""),"","",key.value,"",types,false,"",::isKeyValid,
            {},{},{},{},{},{},{ key.value = it },{},{}) } }
        compose.onNodeWithTag("security-key1").performTextInput("GGGGGGGGGGGG")
        compose.onNodeWithTag("security-key1").assertTextContains(targetContext.getString(R.string.sec_ibutton_key_invalid), substring = true)
    }
}
