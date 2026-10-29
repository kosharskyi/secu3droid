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
import androidx.compose.ui.test.assertHasNoClickAction
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
import org.secu3.android.R
import org.secu3.android.domain.parameters.DbwValues
import org.secu3.android.domain.parameters.ParameterCapabilities
import org.secu3.android.ui.compose.SecuComposeTheme

class DbwScreenTest {
    @get:Rule val compose = createComposeRule()
    private val context get() = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun loadingDataAndHomePositionAreRenderedWithoutCallbacks() {
        var callbacks = 0
        val state = mutableStateOf<DbwValues?>(null)
        compose.setContent { SecuComposeTheme { DbwScreen(state.value, ParameterCapabilities.All, onEdit = { callbacks++ }) } }
        compose.onNodeWithTag("dbw_loading").assertIsDisplayed()
        val values = DbwValues(.1f, .2f, .3f, 40f, 50f, .12f, 6f, 7f, 8f, 9f, 37.25f)
        compose.runOnIdle { state.value = values }
        DbwNumber.entries.forEach { number ->
            compose.onNodeWithTag("dbw_${number.name.lowercase()}").performScrollTo().assertIsDisplayed()
        }
        compose.onNodeWithTag("dbw_home_position").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("dbw_home_position-value", useUnmergedTree = true)
            .assertTextEquals("${String.format(java.util.Locale.getDefault(), "%.2f", values.homePosition)} ${context.getString(R.string.units_percents)}")
        compose.onNodeWithTag("dbw_home_position").assertHasNoClickAction()
        assertEquals(0, callbacks)
    }

    @Test
    fun unsupportedFirmwareDimsEditorsAndRemovesClickActions() {
        compose.setContent {
            SecuComposeTheme {
                DbwScreen(DbwValues(1f, 2f, 3f, 40f, 50f, .1f, 6f, 7f, 8f, 9f, 10f), ParameterCapabilities(
                    org.secu3.android.models.packets.input.FirmwareInfoPacket(options = 1 shl 17),
                ), onEdit = {})
            }
        }
        compose.onNodeWithTag("dbw_etc_p").performScrollTo().assertIsNotEnabled()
    }

    @Test
    fun editableRowsInvokeOnlyTheirSelectedCallback() {
        val edits = mutableListOf<DbwNumber>()
        compose.setContent {
            SecuComposeTheme {
                DbwScreen(DbwValues(1f, 2f, 3f, 40f, 50f, .1f, 6f, 7f, 8f, 9f, 12f), ParameterCapabilities.All, edits::add)
            }
        }
        compose.onNodeWithTag("dbw_etc_p").performScrollTo().performClick()
        assertEquals(listOf(DbwNumber.ETC_P), edits)
    }
}
