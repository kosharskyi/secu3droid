/*
 *    SecuDroid  - An open source, free manager for SECU-3 engine control unit
 *    Copyright (C) 2024 Vitalii O. Kosharskyi. Ukraine, Kyiv
 *
 *    SECU-3  - An open source, free engine control unit
 *    Copyright (C) 2007-2024 Alexey A. Shabelnikov. Ukraine, Kyiv
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

package org.secu3.android.ui.diagnostics

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.runtime.mutableStateOf
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.secu3.android.R
import org.secu3.android.ui.compose.SecuComposeTheme

class DiagnosticAdditionalDialogTest {

    @get:Rule
    val compose = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun displaysWarningAndInitialOptionStates() {
        showDialog(initialEnableBlDe = true, initialEnableTachO = false)

        compose.onNodeWithText(context.getString(R.string.diag_bl_de_warning)).assertIsDisplayed()
        compose.onNodeWithTag("enable_bl_de_checkbox").assertIsOn()
        compose.onNodeWithTag("enable_tach_o_checkbox").assertIsOff()
    }

    @Test
    fun togglingThenCancelDoesNotApplyChanges() {
        val applied = mutableListOf<Pair<Boolean, Boolean>>()
        var cancelled = false
        showDialog(
            onOk = { blDe, tachO -> applied += blDe to tachO },
            onCancel = { cancelled = true },
        )

        compose.onNodeWithTag("enable_bl_de_checkbox").performClick()
        compose.onNodeWithTag("enable_tach_o_checkbox").performClick()
        compose.onNodeWithText(context.getString(android.R.string.cancel)).performClick()

        assertEquals(emptyList<Pair<Boolean, Boolean>>(), applied)
        assertEquals(true, cancelled)
    }

    @Test
    fun togglingThenOkAppliesBothValuesOnce() {
        val applied = mutableListOf<Pair<Boolean, Boolean>>()
        showDialog(onOk = { blDe, tachO -> applied += blDe to tachO })

        compose.onNodeWithTag("enable_bl_de_checkbox").performClick()
        compose.onNodeWithTag("enable_tach_o_checkbox").performClick()
        compose.onNodeWithTag("diagnostic_ok").performClick()

        assertEquals(listOf(false to true), applied)
    }

    @Test
    fun tachOVisibilityMatchesFirmwareAvailability() {
        val showTachO = mutableStateOf(true)
        compose.setContent {
            SecuComposeTheme {
                DiagnosticAdditionalDialogContent(
                    initialEnableBlDe = false,
                    initialEnableTachO = false,
                    showTachO = showTachO.value,
                    onOk = { _, _ -> },
                    onCancel = {},
                )
            }
        }
        compose.onNodeWithTag("enable_tach_o").assertIsDisplayed()
        showTachO.value = false
        compose.onNodeWithTag("enable_tach_o").assertDoesNotExist()
    }

    @Test
    fun optionsAndActionsRemainAvailableAtNarrowWidth() {
        val darkTheme = mutableStateOf(false)
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 1.7f)) {
                SecuComposeTheme(darkTheme = darkTheme.value) {
                    Box(Modifier.width(280.dp)) {
                        DiagnosticAdditionalDialogContent(
                            initialEnableBlDe = false,
                            initialEnableTachO = false,
                            showTachO = true,
                            onOk = { _, _ -> },
                            onCancel = {},
                        )
                    }
                }
            }
        }
        compose.onNodeWithTag("enable_bl_de").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("enable_tach_o").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("diagnostic_ok").performScrollTo().assertIsDisplayed()
        darkTheme.value = true
        compose.onNodeWithTag("enable_bl_de").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("diagnostic_ok").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun unsavedOptionChangesRestoreAfterCompositionStateRestore() {
        val restorationTester = StateRestorationTester(compose)
        restorationTester.setContent {
            SecuComposeTheme {
                DiagnosticAdditionalDialogContent(
                    initialEnableBlDe = false,
                    initialEnableTachO = false,
                    showTachO = true,
                    onOk = { _, _ -> },
                    onCancel = {},
                )
            }
        }
        compose.onNodeWithTag("enable_bl_de_checkbox").performClick()
        compose.onNodeWithTag("enable_tach_o_checkbox").performClick()

        restorationTester.emulateSavedInstanceStateRestore()

        compose.onNodeWithTag("enable_bl_de_checkbox").assertIsOn()
        compose.onNodeWithTag("enable_tach_o_checkbox").assertIsOn()
    }

    private fun showDialog(
        initialEnableBlDe: Boolean = true,
        initialEnableTachO: Boolean = false,
        showTachO: Boolean = true,
        onOk: (Boolean, Boolean) -> Unit = { _, _ -> },
        onCancel: () -> Unit = {},
    ) {
        compose.setContent {
            SecuComposeTheme {
                DiagnosticAdditionalDialogContent(
                    initialEnableBlDe = initialEnableBlDe,
                    initialEnableTachO = initialEnableTachO,
                    showTachO = showTachO,
                    onOk = onOk,
                    onCancel = onCancel,
                )
            }
        }
    }
}
