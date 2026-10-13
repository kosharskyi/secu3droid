/*
 *    SecuDroid  - An open source, free manager for SECU-3 engine control unit
 *    Copyright (C) 2024 Vitalii O. Kosharskyi. Ukraine, Kyiv
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 */
package org.secu3.android.ui.parameters

import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.ViewModelProvider
import android.view.ViewGroup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.secu3.android.R
import org.secu3.android.models.packets.out.params.AnglesParamPacket
import org.secu3.android.models.packets.out.params.MiscellaneousParamPacket
import org.secu3.android.models.packets.out.params.StarterParamPacket
import org.secu3.android.ui.MainActivity
import org.secu3.android.ui.compose.SecuComposeTheme
import org.secu3.android.ui.parameters.pages.compose.AnglesNumber
import org.secu3.android.ui.parameters.pages.compose.AnglesUiState
import org.secu3.android.ui.parameters.pages.compose.StarterUiState

class ParameterPageEditorRoutingTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun starterIntegerEditorRequestRoutesAcceptedValueToItsHolder() {
        val vm = paramsViewModel()
        val holder = vm.starterStateHolder
        holder.reset()
        holder.onPacketReceived(StarterParamPacket(starterOff = 120))
        vm.isSendAllowed = false
        val requests = mutableListOf<ParameterEditRequest>()
        renderPage(R.string.params_tab_start_title, vm, requests)

        compose.onNodeWithTag("starter_starter_off").performClick()
        val request = requests.single() as ParameterEditRequest.IntValue
        assertEquals(120, request.value)
        assertEquals(compose.activity.getString(R.string.starter_off_title), request.title)
        assertEquals(0, request.min)
        assertEquals(6000, request.max)
        assertEquals(10, request.step)
        compose.runOnIdle { request.onValue(140) }
        assertEquals(140f, (holder.state as StarterUiState.Data).values.value(org.secu3.android.ui.parameters.pages.compose.StarterNumber.STARTER_OFF))
        assertEquals(false, vm.isSendAllowed)
    }

    @Test fun anglesFloatEditorRequestRoutesAcceptedValueToItsHolder() {
        val vm = paramsViewModel()
        val holder = vm.anglesStateHolder
        holder.reset()
        holder.onPacketReceived(AnglesParamPacket(maxAngle = 4f))
        vm.isSendAllowed = false
        val requests = mutableListOf<ParameterEditRequest>()
        renderPage(R.string.params_tab_angles_title, vm, requests)

        compose.onNodeWithTag("angles_max").performClick()
        val request = requests.single() as ParameterEditRequest.FloatValue
        assertEquals(4f, request.value)
        assertEquals(compose.activity.getString(R.string.angles_max_angle_title), request.title)
        assertEquals(-15f, request.min)
        assertEquals(60f, request.max)
        assertEquals(.25f, request.step)
        assertEquals(2, request.precision)
        compose.runOnIdle { request.onValue(5.5f) }
        assertEquals(5.5f, (holder.state as AnglesUiState.Data).values.value(AnglesNumber.MAX))
        assertEquals(false, vm.isSendAllowed)
    }

    @Test fun miscellaneousEvapEditorsPreserveDependentBounds() {
        val vm = paramsViewModel()
        val holder = vm.miscellaneousStateHolder
        holder.reset()
        val packet = MiscellaneousParamPacket(evapAfbegin = 5_000).apply { evapAfEnd = 175_000 }
        holder.onPacketReceived(packet)
        vm.isSendAllowed = false
        val requests = mutableListOf<ParameterEditRequest>()
        renderPage(R.string.params_tab_miscellaneous_title, vm, requests)

        compose.onNodeWithTag("misc-begin").performScrollTo().performClick()
        val begin = requests.single() as ParameterEditRequest.IntValue
        assertEquals(5_000, begin.value)
        assertEquals(compose.activity.getString(R.string.miscel_evap_starting_air_flow_title), begin.title)
        assertEquals(100, begin.min)
        assertEquals(158_500, begin.max)
        assertEquals(1, begin.step)
        compose.runOnIdle { begin.onValue(10_000) }
        assertEquals(10_000, holder.currentPacket?.evapAfbegin)

        requests.clear()
        compose.onNodeWithTag("misc-end").performScrollTo().performClick()
        val end = requests.single() as ParameterEditRequest.IntValue
        assertEquals(holder.currentPacket?.evapAfEnd, end.value)
        assertEquals(26_500, end.min)
        assertEquals(200_000, end.max)
        assertEquals(1, end.step)
        val previousSlope = holder.currentPacket?.evapAfslope
        compose.runOnIdle { end.onValue(180_000) }
        assertTrue(holder.currentPacket?.evapAfslope != previousSlope)
        assertEquals(false, vm.isSendAllowed)
        assertTrue(holder.state != null)
    }

    private fun paramsViewModel(): ParamsViewModel {
        lateinit var viewModel: ParamsViewModel
        compose.activityRule.scenario.onActivity { activity ->
            viewModel = ViewModelProvider(activity)[ParamsViewModel::class.java]
        }
        return viewModel
    }

    private fun renderPage(titleRes: Int, vm: ParamsViewModel, requests: MutableList<ParameterEditRequest>) {
        compose.activityRule.scenario.onActivity { activity ->
            activity.addContentView(
                ComposeView(activity).apply {
                    setContent { SecuComposeTheme { ParameterPageContent(titleRes, vm, true, { true }, requests::add) } }
                },
                ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT),
            )
        }
        compose.waitForIdle()
    }
}
