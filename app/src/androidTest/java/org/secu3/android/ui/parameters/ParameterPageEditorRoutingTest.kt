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
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertIsEnabled
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.secu3.android.R
import org.secu3.android.domain.parameters.ParameterCapabilities
import org.secu3.android.models.packets.input.FirmwareInfoPacket
import org.secu3.android.models.packets.out.params.AnglesParamPacket
import org.secu3.android.models.packets.out.params.LambdaParamPacket
import org.secu3.android.models.packets.out.params.MiscellaneousParamPacket
import org.secu3.android.models.packets.out.params.SecurityParamPacket
import org.secu3.android.models.packets.out.params.StarterParamPacket
import org.secu3.android.models.packets.out.params.TemperatureParamPacket
import org.secu3.android.models.packets.out.params.CarburParamPacket
import org.secu3.android.ui.MainActivity
import org.secu3.android.ui.compose.SecuComposeTheme
import org.secu3.android.ui.parameters.pages.compose.StarterNumber
import org.secu3.android.ui.parameters.pages.compose.TemperatureNumber
import org.secu3.android.ui.parameters.pages.compose.CarburNumber
import org.secu3.android.ui.parameters.dialogs.NumericEditDialog

class ParameterPageEditorRoutingTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun starterIntegerEditorUsesDesktopBoundsAndRefusedCommandKeepsCommittedValue() {
        val vm = paramsViewModel()
        vm.starterControl.reset()
        vm.starterControl.receive(StarterParamPacket(starterOff = 120))
        renderPage(R.string.params_tab_start_title, vm)

        compose.onNodeWithTag("starter_starter_off").performClick()
        val session = requireNotNull(vm.parameterEditSession)
        val request = session.request as ParameterEditRequest.IntValue
        assertEquals(120, request.value)
        assertEquals(compose.activity.getString(R.string.starter_off_title), request.title)
        assertEquals(0, request.min)
        assertEquals(6000, request.max)
        assertEquals(10, request.step)
        assertFalse(vm.editStarterInt(StarterNumber.CRANK_TO_RUN_COLD, 2))
        assertFalse(vm.editStarterFloat(StarterNumber.STARTER_OFF, Float.NaN))
        compose.runOnIdle { vm.acceptParameterEditor(session.id, 140) }
        assertEquals(120f, StarterNumber.STARTER_OFF.value(requireNotNull(vm.starterControl.values.value)))
    }

    @Test fun anglesFloatEditorRequestRoutesValueAndKeepsCommittedStateWhenSessionRefuses() {
        val vm = paramsViewModel()
        val control = vm.anglesControl
        control.reset()
        control.receive(AnglesParamPacket(maxAngle = 4f))
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
        assertEquals(4f, control.values.value?.maxAngle)
        assertEquals(false, vm.isSendAllowed)
    }

    @Test fun temperatureEditorsPreserveWholeSecondTimerContractAndRejectWithoutSession() {
        val vm = paramsViewModel()
        val control = vm.temperatureControl
        control.reset()
        control.receive(TemperatureParamPacket(ventOn = 95f, ventOff = 90f, ventTmr = 1.27f))
        vm.isSendAllowed = false
        renderPage(R.string.params_tab_temperature_title, vm)

        compose.onNodeWithTag("temperature_fan_timer").performClick()
        val timerSession = requireNotNull(vm.parameterEditSession)
        val timerRequest = timerSession.request as ParameterEditRequest.IntValue
        assertEquals(1, timerRequest.value)
        assertEquals(compose.activity.getString(R.string.temper_fans_timer_title), timerRequest.title)
        assertEquals(0, timerRequest.min)
        assertEquals(600, timerRequest.max)
        assertEquals(1, timerRequest.step)
        compose.runOnIdle { vm.acceptParameterEditor(timerSession.id, 30) }
        assertEquals(1.27f, control.values.value?.fanTimer)

        compose.onNodeWithTag("temperature_fan_on").performClick()
        val fanSession = requireNotNull(vm.parameterEditSession)
        val fanRequest = fanSession.request as ParameterEditRequest.FloatValue
        assertEquals(95f, fanRequest.value)
        assertEquals(-10f, fanRequest.min)
        assertEquals(125f, fanRequest.max)
        assertEquals(.2f, fanRequest.step)
        compose.runOnIdle { vm.acceptParameterEditor(fanSession.id, 100f) }
        assertEquals(95f, control.values.value?.fanOn)
        assertEquals(1.27f, control.values.value?.fanTimer)
        assertEquals(false, vm.isSendAllowed)
    }

    @Test fun carburEditorRowsRouteAllNamedValuesThroughTheMatchingEditorContract() {
        val vm = paramsViewModel()
        val control = vm.carburControl
        control.reset()
        control.receive(CarburParamPacket(
            ieLot = 800, ieHit = 1200, ieLotG = 850, ieHitG = 1250,
            shutoffDelay = .25f, feOnThresholds = 6.25f, tpsThreshold = 5f,
            fuelcutMapThrd = 20f, fuelcutCtsThrd = 15f, revlimLot = 8000, revlimHit = 8100,
        ))
        vm.isSendAllowed = false
        renderPage(R.string.params_tab_fuel_cutoff_title, vm)

        val initial = requireNotNull(control.values.value)
        CarburNumber.entries.forEach { number ->
            compose.onNodeWithTag("carbur_${number.name.lowercase()}").performScrollTo().performClick()
            val session = requireNotNull(vm.parameterEditSession)
            if (number.integer) {
                val request = session.request as ParameterEditRequest.IntValue
                assertEquals(compose.activity.getString(number.title), request.title)
                assertEquals(number.value(initial).toInt(), request.value)
                assertEquals(number.min.toInt(), request.min)
                assertEquals(number.max.toInt(), request.max)
                assertEquals(number.step.toInt(), request.step)
                compose.runOnIdle { vm.acceptParameterEditor(session.id, request.value + 1) }
            } else {
                val request = session.request as ParameterEditRequest.FloatValue
                assertEquals(compose.activity.getString(number.title), request.title)
                assertEquals(number.value(initial), request.value)
                assertEquals(number.min, request.min)
                assertEquals(number.max, request.max)
                assertEquals(number.step, request.step)
                assertEquals(number.precision, request.precision)
                compose.runOnIdle { vm.acceptParameterEditor(session.id, request.value + .1f) }
            }
            assertEquals(initial, control.values.value)
        }
        assertEquals(false, vm.isSendAllowed)
    }

    @Test fun miscellaneousEvapEditorsPreserveDependentBounds() {
        val vm = paramsViewModel()
        val control = vm.miscellaneousControl
        control.reset()
        val packet = MiscellaneousParamPacket(evapAfbegin = 5_000).apply { evapAfEnd = 175_000 }
        control.receive(packet)
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
        assertEquals(10_000, control.values.value?.begin)

        compose.onNodeWithTag("misc-end").performScrollTo().performClick()
        val endSession = requireNotNull(vm.parameterEditSession)
        val end = endSession.request as ParameterEditRequest.IntValue
        assertEquals(control.values.value?.end, end.value)
        assertEquals(26_500, end.min)
        assertEquals(2_000_000, end.max)
        assertEquals(1, end.step)
        val previousSlope = packet.evapAfslope
        compose.runOnIdle { vm.acceptParameterEditor(endSession.id, 180_000) }
        assertTrue(packet.evapAfslope != previousSlope)
        assertEquals(false, vm.isSendAllowed)
        assertTrue(control.values.value != null)
    }

    @Test fun lambdaPageContentPassesCarbAfrCapabilityToChannelsAndStoich() {
        val vm = paramsViewModel()
        val holder = vm.lambdaControlStateHolder
        holder.reset()
        val packet = LambdaParamPacket()
        holder.onPacketReceived(packet)
        val capabilities = ParameterCapabilities(FirmwareInfoPacket(options = 1 shl 30))
        renderPage(R.string.params_tab_lambda_control_title, vm, capabilities)

        compose.onNodeWithTag("lambda_channel_1").assertIsEnabled().performScrollTo().performClick()
        compose.onNodeWithTag("lambda_stoich_disabled").assertExists()

        assertEquals(1, packet.lambdaSelectedChanel)
        assertEquals(null, vm.parameterEditSession)
    }

    @Test fun securityPageContentRoutesKeyAndCredentialBuffersWhileSessionRefusesCommands() {
        val vm = paramsViewModel()
        val packet = SecurityParamPacket(
            btFlags = 0xA1,
            iButton0 = listOf(1, 2, 3, 4, 5, 6),
            iButton1 = listOf(10, 11, 12, 13, 14, 15),
        )
        vm.securityControl.reset()
        vm.securityControl.receive(packet)
        renderPage(R.string.params_tab_security_title, vm)

        compose.onNodeWithTag("security-name").performTextInput("ECU")
        compose.onNodeWithTag("security-password").performTextInput("1234")
        compose.onNodeWithTag("security-apply").assertIsEnabled().performClick()
        assertEquals("ECU", vm.securityName)
        assertEquals("1234", vm.securityPassword)
        assertEquals("", packet.btName)
        assertEquals("", packet.btPass)

        compose.onNodeWithTag("security-key1").performTextClearance()
        compose.onNodeWithTag("security-key1").performTextInput("112233")
        compose.onNodeWithTag("security-key1").assertTextContains("112233", substring = true)
        assertEquals("060504030201", vm.securityControl.values.value?.key0)
        compose.onNodeWithTag("security-key1").performTextClearance()
        compose.onNodeWithTag("security-key1").performTextInput("GGGGGGGGGGGG")
        compose.onNodeWithTag("security-key1").assertTextContains("GGGGGGGGGGGG", substring = true)
        compose.onNodeWithText(compose.activity.getString(R.string.sec_ibutton_key_invalid)).assertExists()
        assertEquals("060504030201", vm.securityControl.values.value?.key0)

        compose.onNodeWithTag("security-key1").performTextClearance()
        compose.onNodeWithTag("security-key1").performTextInput("112233445566")
        compose.onNodeWithTag("security-key1").assertTextContains("112233445566", substring = true)
        assertEquals("060504030201", vm.securityControl.values.value?.key0)
        assertFalse(vm.isSendAllowed)
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

    private fun renderPage(
        titleRes: Int,
        vm: ParamsViewModel,
        capabilities: ParameterCapabilities = vm.capabilities.value,
    ) {
        compose.activityRule.scenario.onActivity { activity ->
            activity.addContentView(
                ComposeView(activity).apply {
                    setContent { SecuComposeTheme { ParameterPageContent(requireNotNull(ParameterPageCatalog().pageForTitle(titleRes)), titleRes, vm, capabilities, true, { true }, vm::openParameterEditor) } }
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
