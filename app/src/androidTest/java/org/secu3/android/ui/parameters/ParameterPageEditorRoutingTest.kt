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
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
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
import org.secu3.android.ui.parameters.dialogs.NumericEditDialog

class ParameterPageEditorRoutingTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun starterIntegerEditorRequestRoutesAcceptedValueToItsHolder() {
        val vm = paramsViewModel()
        val holder = vm.starterStateHolder
        holder.reset()
        holder.onPacketReceived(StarterParamPacket(starterOff = 120))
        vm.isSendAllowed = false
        renderPage(R.string.params_tab_start_title, vm)

        compose.onNodeWithTag("starter_starter_off").performClick()
        val session = requireNotNull(vm.parameterEditSession)
        val request = session.request as ParameterEditRequest.IntValue
        assertEquals(120, request.value)
        assertEquals(compose.activity.getString(R.string.starter_off_title), request.title)
        assertEquals(0, request.min)
        assertEquals(6000, request.max)
        assertEquals(10, request.step)
        compose.runOnIdle { vm.acceptParameterEditor(session.id, 140) }
        assertEquals(140f, (holder.state as StarterUiState.Data).values.value(org.secu3.android.ui.parameters.pages.compose.StarterNumber.STARTER_OFF))
        assertEquals(false, vm.isSendAllowed)
    }

    @Test fun anglesFloatEditorRequestRoutesAcceptedValueToItsHolder() {
        val vm = paramsViewModel()
        val holder = vm.anglesStateHolder
        holder.reset()
        holder.onPacketReceived(AnglesParamPacket(maxAngle = 4f))
        vm.isSendAllowed = false
        renderPage(R.string.params_tab_angles_title, vm)

        compose.onNodeWithTag("angles_max").performClick()
        val session = requireNotNull(vm.parameterEditSession)
        val request = session.request as ParameterEditRequest.FloatValue
        assertEquals(4f, request.value)
        assertEquals(compose.activity.getString(R.string.angles_max_angle_title), request.title)
        assertEquals(-15f, request.min)
        assertEquals(60f, request.max)
        assertEquals(.25f, request.step)
        assertEquals(2, request.precision)
        compose.runOnIdle { vm.acceptParameterEditor(session.id, 5.5f) }
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
        renderPage(R.string.params_tab_miscellaneous_title, vm)

        compose.onNodeWithTag("misc-begin").performScrollTo().performClick()
        val beginSession = requireNotNull(vm.parameterEditSession)
        val begin = beginSession.request as ParameterEditRequest.IntValue
        assertEquals(5_000, begin.value)
        assertEquals(compose.activity.getString(R.string.miscel_evap_starting_air_flow_title), begin.title)
        assertEquals(100, begin.min)
        assertEquals(158_500, begin.max)
        assertEquals(1, begin.step)
        compose.runOnIdle { vm.acceptParameterEditor(beginSession.id, 10_000) }
        assertEquals(10_000, holder.currentPacket?.evapAfbegin)

        compose.onNodeWithTag("misc-end").performScrollTo().performClick()
        val endSession = requireNotNull(vm.parameterEditSession)
        val end = endSession.request as ParameterEditRequest.IntValue
        assertEquals(holder.currentPacket?.evapAfEnd, end.value)
        assertEquals(26_500, end.min)
        assertEquals(200_000, end.max)
        assertEquals(1, end.step)
        val previousSlope = holder.currentPacket?.evapAfslope
        compose.runOnIdle { vm.acceptParameterEditor(endSession.id, 180_000) }
        assertTrue(holder.currentPacket?.evapAfslope != previousSlope)
        assertEquals(false, vm.isSendAllowed)
        assertTrue(holder.state != null)
    }

    @Test fun parameterEditSessionRejectsStaleAndRepeatedConfirmations() {
        val vm = paramsViewModel()
        var oldCalls = 0
        var currentCalls = 0
        var oldSessionId = 0L
        var currentSessionId = 0L
        compose.runOnIdle {
            vm.openParameterEditor(ParameterEditRequest.IntValue("Old", 1, 0, 10, 1) { oldCalls++ })
            oldSessionId = requireNotNull(vm.parameterEditSession).id
            vm.dismissParameterEditor(oldSessionId)
            vm.openParameterEditor(ParameterEditRequest.IntValue("Current", 2, 0, 10, 1) { currentCalls++ })
            currentSessionId = requireNotNull(vm.parameterEditSession).id

            vm.acceptParameterEditor(oldSessionId, 3)
            assertEquals(currentSessionId, vm.parameterEditSession?.id)
            vm.acceptParameterEditor(currentSessionId, 4)
            vm.acceptParameterEditor(currentSessionId, 5)
        }
        assertEquals(0, oldCalls)
        assertEquals(1, currentCalls)
        assertEquals(null, vm.parameterEditSession)

        var disconnectedSessionId = 0L
        compose.runOnIdle {
            vm.openParameterEditor(ParameterEditRequest.IntValue("Disconnected", 1, 0, 10, 1) { oldCalls++ })
            disconnectedSessionId = requireNotNull(vm.parameterEditSession).id
            vm.clearPageState()
            vm.acceptParameterEditor(disconnectedSessionId, 7)
        }
        assertEquals(0, oldCalls)
        assertEquals(null, vm.parameterEditSession)
    }

    @Test fun parameterEditSessionSurvivesActivityRecreation() {
        val vm = paramsViewModel()
        var accepted = 0
        var sessionId = 0L
        compose.runOnIdle {
            vm.openParameterEditor(ParameterEditRequest.FloatValue("Rotation", 1.25f, -2f, 2f, .25f, 2) { accepted++ })
            sessionId = requireNotNull(vm.parameterEditSession).id
        }
        compose.activityRule.scenario.recreate()
        val retainedViewModel = paramsViewModel()
        assertEquals(sessionId, retainedViewModel.parameterEditSession?.id)
        compose.runOnIdle { retainedViewModel.acceptParameterEditor(sessionId, 1.5f) }
        assertEquals(1, accepted)
        assertEquals(null, retainedViewModel.parameterEditSession)
    }

    @Test fun numericDialogTextAndAcceptedFloatSurviveActivityRecreation() {
        val vm = paramsViewModel()
        var accepted: Float? = null
        renderEditorDialog(vm)
        compose.runOnIdle {
            vm.openParameterEditor(
                ParameterEditRequest.FloatValue("Float restore", 1.25f, -2f, 2f, .25f, 2) { accepted = it },
            )
        }
        compose.onNodeWithTag("numeric_value").performTextClearance()
        compose.onNodeWithTag("numeric_value").performTextInput("1.75")

        compose.activityRule.scenario.recreate()
        renderEditorDialog(paramsViewModel())
        compose.waitForIdle()
        compose.onNodeWithTag("numeric_value").assertTextEquals("1.75")
        compose.onNodeWithTag("numeric_confirm").performClick()

        assertEquals(1.75f, accepted)
        assertEquals(null, vm.parameterEditSession)
    }

    @Test fun invalidAcceptedValuesDoNotClearOrInvokeTheCurrentRequest() {
        val vm = paramsViewModel()
        var accepted = 0
        var sessionId = 0L
        compose.runOnIdle {
            vm.openParameterEditor(ParameterEditRequest.FloatValue("Bounded", 1f, 0f, 2f, .1f, 1) { accepted++ })
            sessionId = requireNotNull(vm.parameterEditSession).id
            vm.acceptParameterEditor(sessionId, 3f)
            vm.acceptParameterEditor(sessionId, Float.NaN)
        }
        assertEquals(0, accepted)
        assertEquals(sessionId, vm.parameterEditSession?.id)
        compose.runOnIdle { vm.acceptParameterEditor(sessionId, 1.5f) }
        assertEquals(1, accepted)
        assertEquals(null, vm.parameterEditSession)
    }

    private fun paramsViewModel(): ParamsViewModel {
        lateinit var viewModel: ParamsViewModel
        compose.activityRule.scenario.onActivity { activity ->
            viewModel = ViewModelProvider(activity)[ParamsViewModel::class.java]
        }
        return viewModel
    }

    private fun renderPage(titleRes: Int, vm: ParamsViewModel) {
        compose.activityRule.scenario.onActivity { activity ->
            activity.addContentView(
                ComposeView(activity).apply {
                    setContent { SecuComposeTheme { ParameterPageContent(titleRes, vm, true, { true }, vm::openParameterEditor) } }
                },
                ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT),
            )
        }
        compose.waitForIdle()
    }

    private fun renderEditorDialog(vm: ParamsViewModel) {
        compose.activityRule.scenario.onActivity { activity ->
            activity.addContentView(
                ComposeView(activity).apply {
                    id = R.id.params_view_container
                    setContent {
                        SecuComposeTheme {
                            vm.parameterEditSession?.let { session ->
                                NumericEditDialog(
                                    request = session.request,
                                    sessionId = session.id,
                                    onConfirm = { vm.acceptParameterEditor(session.id, it) },
                                    onDismiss = { vm.dismissParameterEditor(session.id) },
                                )
                            }
                        }
                    }
                },
                ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT),
            )
        }
        compose.waitForIdle()
    }
}
