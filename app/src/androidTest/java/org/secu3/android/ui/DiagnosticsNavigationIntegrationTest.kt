package org.secu3.android.ui

import android.view.KeyEvent
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavHostController
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Rule
import org.junit.Test
import org.secu3.android.R
import org.secu3.android.ui.diagnostics.DiagnosticOutput
import org.secu3.android.ui.diagnostics.DiagnosticsViewModel

class DiagnosticsNavigationIntegrationTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun diagnosticsGraphSharesViewModelAcrossDialogAndRecreationThenClosesSessionOnExit() {
        val nativeHost = navigateHome()
        compose.activityRule.scenario.onActivity { it.openDiagnostics() }
        compose.waitForIdle()
        assertDiagnosticsHost(nativeHost)

        val firstVm = diagnosticsViewModel()
        compose.activityRule.scenario.onActivity {
            assertEquals(50f, firstVm.uiState.value.duty)
            firstVm.selectChannel(3)
            assertEquals(50f, firstVm.uiState.value.duty)
            firstVm.editFrequency(321f)
            firstVm.editDuty(37.5f)
        }
        compose.onNodeWithTag("diagnostics-settings-icon").performClick()
        compose.waitForIdle()
        assertEquals(DIAGNOSTICS_ADDITIONAL_ROUTE, compose.activity.rootNavigationController?.currentDestination?.route)
        assertSame(firstVm, diagnosticsViewModel())

        compose.onNodeWithTag("enable_bl_de_checkbox").performClick()
        compose.activityRule.scenario.recreate()
        compose.waitForIdle()
        val restoredHost = nativeHost()
        val restoredVm = diagnosticsViewModel()
        assertSame(firstVm, restoredVm)
        assertEquals(3, restoredVm.uiState.value.channel)
        assertEquals(321f, restoredVm.uiState.value.frequency)
        assertEquals(37.5f, restoredVm.uiState.value.duty)
        compose.onNodeWithTag("enable_bl_de_checkbox").assertIsOn()

        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
        compose.waitForIdle()
        assertEquals(DIAGNOSTICS_ROUTE, compose.activity.rootNavigationController?.currentDestination?.route)
        assertSame(restoredVm, diagnosticsViewModel())
        assertEquals(false, restoredVm.uiState.value.enableBlDe)
        compose.onNodeWithText("IGN_OUT3").assertExists()
        compose.onNodeWithText("321.0 Hz").assertExists()
        compose.onNodeWithText("37.5 %").assertExists()
        compose.onNodeWithContentDescription(compose.activity.getString(R.string.back)).performClick()
        compose.waitForIdle()
        assertNativeHome(restoredHost)
        compose.onAllNodesWithText(compose.activity.getString(R.string.disconnect_device)).assertCountEquals(0)

        compose.activityRule.scenario.onActivity { it.openDiagnostics() }
        compose.waitForIdle()
        val reopenedVm = diagnosticsViewModel()
        assertNotSame(restoredVm, reopenedVm)
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
        compose.waitForIdle()
        assertNativeHome(restoredHost)
    }

    @Test
    fun confirmingAdditionalOptionsCommitsInViewModelAndDoesNotCreateAnotherGraphEntry() {
        val host = navigateHome()
        compose.activityRule.scenario.onActivity { it.openDiagnostics() }
        compose.waitForIdle()
        val vm = diagnosticsViewModel()
        compose.onNodeWithTag("diagnostics-settings-icon").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("enable_bl_de_checkbox").performClick()
        compose.onNodeWithTag("diagnostic_ok").performClick()
        compose.waitForIdle()
        assertSame(vm, diagnosticsViewModel())
        assertEquals(DIAGNOSTICS_ROUTE, compose.activity.rootNavigationController?.currentDestination?.route)
        assertEquals(true, vm.uiState.value.enableBlDe)

        compose.activityRule.scenario.onActivity {
            vm.setOutput(DiagnosticOutput.BL, true)
            vm.setOutput(DiagnosticOutput.DE, true)
        }
        val blDeHigh = (1 shl 11) or (1 shl 12) or (1 shl 13) or (1 shl 14)
        assertEquals(blDeHigh, vm.uiState.value.outputBits)

        compose.onNodeWithTag("diagnostics-settings-icon").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("diagnostic_ok").performClick()
        compose.waitForIdle()
        assertSame(vm, diagnosticsViewModel())
        assertEquals(true, vm.uiState.value.enableBlDe)
        assertEquals(blDeHigh, vm.uiState.value.outputBits)

        compose.activityRule.scenario.onActivity { it.openDiagnostics() }
        compose.waitForIdle()
        assertSame(vm, diagnosticsViewModel())
        assertEquals(DIAGNOSTICS_ROUTE, compose.activity.rootNavigationController?.currentDestination?.route)
    }

    private fun navigateHome(): NavHostController {
        lateinit var host: NavHostController
        compose.activityRule.scenario.onActivity { activity ->
            host = requireNotNull(activity.rootNavigationController)
            host.navigate(HOME_ROUTE)
        }
        compose.waitForIdle()
        return host
    }

    private fun nativeHost(): NavHostController {
        lateinit var host: NavHostController
        compose.activityRule.scenario.onActivity { activity ->
            host = requireNotNull(activity.rootNavigationController)
        }
        return host
    }

    private fun diagnosticsViewModel(): DiagnosticsViewModel {
        lateinit var vm: DiagnosticsViewModel
        compose.activityRule.scenario.onActivity { activity ->
            val controller = requireNotNull(activity.rootNavigationController)
            val graphEntry = controller.getBackStackEntry(DIAGNOSTICS_GRAPH_ROUTE)
            vm = ViewModelProvider(graphEntry.viewModelStore, activity.defaultViewModelProviderFactory, graphEntry.defaultViewModelCreationExtras)[DiagnosticsViewModel::class.java]
        }
        return vm
    }

    private fun assertDiagnosticsHost(host: NavHostController) {
        compose.activityRule.scenario.onActivity { activity ->
            assertEquals(DIAGNOSTICS_ROUTE, activity.rootNavigationController?.currentDestination?.route)
            assertEquals(Lifecycle.State.CREATED, host.getBackStackEntry(HOME_ROUTE).lifecycle.currentState)
            assertEquals(Lifecycle.State.RESUMED, host.getBackStackEntry(DIAGNOSTICS_ROUTE).lifecycle.currentState)
        }
        compose.onNodeWithText(compose.activity.getString(R.string.title_activity_diagnostics)).assertExists()
    }

    private fun assertNativeHome(host: NavHostController) {
        compose.activityRule.scenario.onActivity { activity ->
            assertEquals(HOME_ROUTE, host.currentDestination?.route)
            assertEquals(Lifecycle.State.RESUMED, host.getBackStackEntry(HOME_ROUTE).lifecycle.currentState)
            assertEquals(HOME_ROUTE, activity.rootNavigationController?.currentDestination?.route)
        }
        compose.onNodeWithTag("home-card-diagnostics").assertExists()
    }

}
