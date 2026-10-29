/*
 *    SecuDroid  - An open source, free manager for SECU-3 engine control unit
 *    Copyright (C) 2026 Vitalii O. Kosharskyi. Ukraine, Kyiv
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 */
package org.secu3.android.ui.parameters.pages.compose

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.secu3.android.domain.parameters.CarburValues
import org.secu3.android.domain.parameters.ParameterCapabilities
import org.secu3.android.ui.compose.SecuComposeTheme

class CarburScreenTest {
    @get:Rule val compose = createComposeRule()
    private val options = listOf("1", "2", "3", "4", "5", "6", "no")
    private val selectorCodes = listOf(0, 1, 2, 3, 4, 5, 15)
    private fun values(fuel: Int = 1, ignition: Int = 15) = CarburValues(
        800, 1200, 850, 1250, .25f, 6.25f, 5f, 20f, 15f, 8000, 8100,
        false, fuel, ignition,
    )

    @Test fun loadingScrollAndRecompositionDoNotCallActions() {
        var callbacks = 0
        val state = mutableStateOf<CarburValues?>(null)
        compose.setContent {
            SecuComposeTheme {
                CarburScreen(state.value, options, selectorCodes, { callbacks++ }, { callbacks++ }, { _, _ -> callbacks++ })
            }
        }
        compose.onNodeWithTag("carbur_loading").assertIsDisplayed()
        compose.runOnIdle { state.value = values() }
        CarburNumber.entries.forEach { compose.onNodeWithTag("carbur_${it.name.lowercase()}").performScrollTo().assertIsDisplayed() }
        compose.onNodeWithTag("carbur_invers").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("carbur_fuelcut_uni").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("carbur_igncut_uni").performScrollTo().assertIsDisplayed()
        compose.runOnIdle { state.value = values() }
        assertEquals(0, callbacks)
    }

    @Test fun allParameterAndSelectorActionsInvokeCallbacksAndDropdownsAreOutlined() {
        val edits = mutableListOf<CarburNumber>()
        val selectors = mutableListOf<Pair<Boolean, Int>>()
        var switches = 0
        compose.setContent {
            SecuComposeTheme {
                CarburScreen(values(), options, selectorCodes, edits::add, { switches++ }, { ignition, i -> selectors += ignition to i })
            }
        }
        CarburNumber.entries.forEach { compose.onNodeWithTag("carbur_${it.name.lowercase()}").performScrollTo().performClick() }
        assertEquals(CarburNumber.entries, edits)
        compose.onNodeWithTag("carbur_invers-switch").performScrollTo().performClick()
        assertEquals(1, switches)
        listOf("carbur_fuelcut_uni" to false, "carbur_igncut_uni" to true).forEach { (tag, ignition) ->
            compose.onNodeWithTag(tag).performScrollTo().assertHasClickAction().performClick()
            compose.onNodeWithTag("carbur_option_6").performClick()
            assertEquals(ignition, selectors.last().first)
            assertEquals(6, selectors.last().second)
            compose.onNodeWithTag("${tag}-dropdown-indicator", useUnmergedTree = true).assertIsDisplayed()
        }
    }

    @Test fun wireCodesDisplayOutputNumbersAndKeepUnknownCodesVisible() {
        val state = mutableStateOf(values(fuel = 0, ignition = 5))
        compose.setContent {
            SecuComposeTheme { CarburScreen(state.value, options, selectorCodes, {}, {}, { _, _ -> }) }
        }
        compose.onNodeWithTag("carbur_fuelcut_uni-value", useUnmergedTree = true).assertTextEquals("1")
        compose.onNodeWithTag("carbur_igncut_uni-value", useUnmergedTree = true).performScrollTo().assertTextEquals("6")
        compose.runOnIdle { state.value = values(fuel = 15, ignition = 41) }
        compose.onNodeWithTag("carbur_fuelcut_uni-value", useUnmergedTree = true).assertTextEquals("no")
        compose.onNodeWithTag("carbur_igncut_uni-value", useUnmergedTree = true).performScrollTo().assertTextEquals("Code 41")
        compose.runOnIdle { state.value = values(fuel = 6, ignition = 42) }
        compose.onNodeWithTag("carbur_fuelcut_uni-value", useUnmergedTree = true).assertTextEquals("Code 6")
        compose.onNodeWithTag("carbur_igncut_uni-value", useUnmergedTree = true).performScrollTo().assertTextEquals("Code 42")
    }

    @Test fun fuelSelectorRequiresInjectionButIgnitionSelectorRemainsAvailable() {
        compose.setContent {
            SecuComposeTheme {
                CarburScreen(
                    values(), options, selectorCodes, {}, {}, { _, _ -> },
                    capabilities = ParameterCapabilities(org.secu3.android.models.packets.input.FirmwareInfoPacket(options = 0)),
                )
            }
        }
        compose.onNodeWithTag("carbur_fuelcut_uni").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithTag("carbur_igncut_uni").assertHasClickAction()
    }
}
