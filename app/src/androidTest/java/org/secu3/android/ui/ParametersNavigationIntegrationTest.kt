/*
 *    SecuDroid  - An open source, free manager for SECU-3 engine control unit
 *    Copyright (C) 2026 Vitalii O. Kosharskyi. Ukraine, Kyiv
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 */
package org.secu3.android.ui

import android.view.KeyEvent
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.click
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.NavHostController
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Rule
import org.junit.Test
import org.secu3.android.R
import org.secu3.android.ui.parameters.ParameterEditRequest
import org.secu3.android.ui.parameters.ParamsViewModel

class ParametersNavigationIntegrationTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun homeBridgeCapsNativeHostAndBothBackPathsRestoreHome() {
        val nativeHost = navigateNativeHostToHome()
        compose.onNodeWithTag("home-card-parameters").performClick()
        compose.waitForIdle()

        compose.activityRule.scenario.onActivity { activity ->
            assertEquals(PARAMETERS_ROUTE, activity.rootNavigationController?.currentDestination?.route)
        }
        assertOnParametersRoute()

        clickParametersToolbarBack()
        assertNativeHomeRestored(nativeHost)

        compose.onNodeWithTag("home-card-parameters").performClick()
        compose.waitForIdle()
        assertOnParametersRoute()
        compose.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        compose.waitForIdle()
        assertNativeHomeRestored(nativeHost)
    }

    @Test fun routePagerRestoreThenPopCreatesFreshViewModel() {
        navigateNativeHostToHome()
        compose.onNodeWithTag("home-card-parameters").performClick()
        compose.waitForIdle()
        val initialViewModel = currentParametersViewModel()
        val anglesTitle = compose.activity.getString(R.string.params_tab_angles_title)
        compose.onNodeWithText(anglesTitle).performClick()
        compose.waitForIdle()
        compose.onNodeWithText(anglesTitle).assertIsSelected()
        assertNull(initialViewModel.parameterEditSession)

        compose.activityRule.scenario.recreate()
        compose.waitForIdle()
        val retainedViewModel = currentParametersViewModel()
        assertSame(initialViewModel, retainedViewModel)
        compose.onNodeWithText(anglesTitle).assertIsSelected()
        assertNull(retainedViewModel.parameterEditSession)

        compose.runOnIdle {
            retainedViewModel.openParameterEditor(
                ParameterEditRequest.IntValue("Stale", 1, 0, 2, 1) {},
            )
        }
        compose.activityRule.scenario.onActivity { activity ->
            requireNotNull(activity.rootNavigationController).popBackStack()
        }
        compose.waitForIdle()
        assertNull(initialViewModel.parameterEditSession)
        compose.onNodeWithTag("home-card-parameters").performClick()
        compose.waitForIdle()
        val reopenedViewModel = currentParametersViewModel()
        assertNotSame(initialViewModel, reopenedViewModel)
        assertNull(reopenedViewModel.parameterEditSession)
    }

    @Test fun startAndHomeSettingsRoutesCapLegacyHostAndRestoreItOnBothBackPaths() {
        val nativeHost = nativeHost()
        compose.onNodeWithContentDescription("Settings icon").performClick()
        compose.waitForIdle()
        assertOnSettingsRoute(nativeHost)
        compose.onNodeWithContentDescription(compose.activity.getString(R.string.back)).performClick()
        compose.waitForIdle()
        assertNativeHostRestored(nativeHost, START_ROUTE)

        compose.onNodeWithContentDescription("Settings icon").performClick()
        compose.waitForIdle()
        assertOnSettingsRoute(nativeHost)
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
        compose.waitForIdle()
        assertNativeHostRestored(nativeHost, START_ROUTE)

        compose.activityRule.scenario.onActivity { nativeHost.navigate(HOME_ROUTE) }
        compose.waitForIdle()
        compose.onNodeWithTag("home-card-settings").performScrollTo().performClick()
        compose.waitForIdle()
        assertOnSettingsRoute(nativeHost)
        compose.onNodeWithContentDescription(compose.activity.getString(R.string.back)).performClick()
        compose.waitForIdle()
        assertNativeHostRestored(nativeHost, HOME_ROUTE)

        compose.onNodeWithTag("home-card-settings").performScrollTo().performClick()
        compose.waitForIdle()
        assertOnSettingsRoute(nativeHost)
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
        compose.waitForIdle()
        assertNativeHostRestored(nativeHost, HOME_ROUTE)
    }

    @Test fun settingsOverlayIsRestoredAcrossActivityRecreation() {
        val nativeHost = nativeHost()
        compose.onNodeWithContentDescription("Settings icon").performClick()
        compose.waitForIdle()
        assertOnSettingsRoute(nativeHost)
        compose.activityRule.scenario.recreate()
        compose.waitForIdle()
        val restoredHost = requireNotNull(compose.activity.rootNavigationController)
        assertOnSettingsRoute(restoredHost)
        compose.onNodeWithText(compose.activity.getString(R.string.title_activity_settings)).assertExists()
    }

    @Test fun startHomeFeatureAndReturnToStartUseOneRootBackStack() {
        var host = nativeHost()
        compose.activityRule.scenario.onActivity { it.openHome() }
        compose.waitForIdle()
        val homeViewModel = currentHomeViewModel()
        compose.onNodeWithTag("home-card-settings").performScrollTo().performClick()
        compose.waitForIdle()
        assertOnSettingsRoute(host)
        compose.onNodeWithContentDescription(compose.activity.getString(R.string.back)).performClick()
        compose.waitForIdle()
        assertNativeHostRestored(host, HOME_ROUTE)
        compose.activityRule.scenario.recreate()
        compose.waitForIdle()
        host = nativeHost()
        assertSame(homeViewModel, currentHomeViewModel())
        compose.activityRule.scenario.onActivity { assertEquals(true, it.openStart()) }
        compose.waitForIdle()
        assertNativeHostRestored(host, START_ROUTE)
        compose.activityRule.scenario.onActivity { activity ->
            try {
                activity.rootNavigationController?.getBackStackEntry(HOME_ROUTE)
                throw AssertionError("Returning to Start must remove Home and its child routes")
            } catch (_: IllegalArgumentException) { }
            activity.openHome()
        }
        compose.waitForIdle()
        assertNotSame(homeViewModel, currentHomeViewModel())
    }

    @Test fun firmwareDialogKeepsHomeStartedConsumesSheetTouchesAndBackClosesOnlyDialog() {
        val host = navigateNativeHostToHome()
        compose.activityRule.scenario.onActivity { it.openFirmware() }
        compose.waitForIdle()
        compose.activityRule.scenario.onActivity { activity ->
            assertEquals(FIRMWARE_ROUTE, host.currentDestination?.route)
            assertEquals(Lifecycle.State.STARTED, host.getBackStackEntry(HOME_ROUTE).lifecycle.currentState)
        }
        compose.onNodeWithText(compose.activity.getString(R.string.firmware_information_title)).performTouchInput { click(center) }
        compose.waitForIdle()
        compose.activityRule.scenario.onActivity { assertEquals(FIRMWARE_ROUTE, host.currentDestination?.route) }
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
        compose.waitForIdle()
        assertNativeHostRestored(host, HOME_ROUTE)
        compose.onAllNodesWithText(compose.activity.getString(R.string.disconnect_device_msg)).assertCountEquals(0)

        compose.activityRule.scenario.onActivity { it.openFirmware() }
        compose.waitForIdle()
        compose.onNodeWithTag("firmware-overlay").performTouchInput { click(Offset(4f, 4f)) }
        compose.waitForIdle()
        assertNativeHostRestored(host, HOME_ROUTE)
    }

    @Test fun startAndHomeLogsRoutesRestoreTheirLegacyDestinations() {
        val host = nativeHost()
        compose.onNodeWithContentDescription("Logs icon").performClick()
        compose.waitForIdle()
        assertFullScreenComposeRoute(host, LOGS_ROUTE, START_ROUTE)
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
        compose.waitForIdle()
        assertNativeHostRestored(host, START_ROUTE)

        compose.activityRule.scenario.onActivity { host.navigate(HOME_ROUTE) }
        compose.waitForIdle()
        compose.onNodeWithTag("home-card-logs").performScrollTo().performClick()
        compose.waitForIdle()
        assertFullScreenComposeRoute(host, LOGS_ROUTE, HOME_ROUTE)
        compose.onNodeWithContentDescription(compose.activity.getString(R.string.back)).performClick()
        compose.waitForIdle()
        assertNativeHostRestored(host, HOME_ROUTE)
    }

    @Test fun homeErrorsRoutePreservesSharedAppearanceAndNativeHost() {
        val host = navigateNativeHostToHome()
        var originalStatusBarColor = 0
        var originalNavigationBarColor = 0
        var originalLightStatusBars = false
        var originalLightNavigationBars = false
        compose.activityRule.scenario.onActivity { activity ->
            originalStatusBarColor = activity.window.statusBarColor
            originalNavigationBarColor = activity.window.navigationBarColor
            val insets = WindowInsetsControllerCompat(activity.window, activity.window.decorView)
            originalLightStatusBars = insets.isAppearanceLightStatusBars
            originalLightNavigationBars = insets.isAppearanceLightNavigationBars
        }

        compose.onNodeWithTag("home-card-errors").performScrollTo().performClick()
        compose.waitForIdle()
        assertFullScreenComposeRoute(host, ERRORS_ROUTE, HOME_ROUTE)
        compose.activityRule.scenario.onActivity { activity ->
            val insets = WindowInsetsControllerCompat(activity.window, activity.window.decorView)
            assertEquals(originalLightStatusBars, insets.isAppearanceLightStatusBars)
            assertEquals(originalLightNavigationBars, insets.isAppearanceLightNavigationBars)
        }
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
        compose.waitForIdle()
        assertNativeHostRestored(host, HOME_ROUTE)
        compose.activityRule.scenario.onActivity { activity ->
            val insets = WindowInsetsControllerCompat(activity.window, activity.window.decorView)
            assertEquals(originalStatusBarColor, activity.window.statusBarColor)
            assertEquals(originalNavigationBarColor, activity.window.navigationBarColor)
            assertEquals(originalLightStatusBars, insets.isAppearanceLightStatusBars)
            assertEquals(originalLightNavigationBars, insets.isAppearanceLightNavigationBars)
        }
    }

    @Test fun errorsClearIsDisabledOfflineAndShowsNoConfirmationAcrossRecreation() {
        navigateNativeHostToHome()
        compose.onNodeWithTag("home-card-errors").performScrollTo().performClick()
        compose.waitForIdle()
        val clearButton = compose.onNodeWithContentDescription(compose.activity.getString(R.string.errors_clear_all_short))
        clearButton.assertIsNotEnabled()
        clearButton.performClick()
        compose.onAllNodesWithText(compose.activity.getString(R.string.dialog_confirm_clear_errors_message)).assertCountEquals(0)

        compose.activityRule.scenario.recreate()
        compose.waitForIdle()
        compose.onNodeWithContentDescription(compose.activity.getString(R.string.errors_clear_all_short)).assertIsNotEnabled()
        compose.onAllNodesWithText(compose.activity.getString(R.string.dialog_confirm_clear_errors_message)).assertCountEquals(0)
    }

    @Test fun firmwareModalKeepsHomeStartedBackClosesOnlySheetAndViewModelIsPerEntry() {
        val host = navigateNativeHostToHome()
        compose.activityRule.scenario.onActivity { it.openFirmware() }
        compose.waitForIdle()
        compose.onNodeWithText(compose.activity.getString(R.string.firmware_information_title)).assertExists()

        var firstViewModel: org.secu3.android.ui.firmware.FirmwareViewModel? = null
        compose.activityRule.scenario.onActivity { activity ->
            assertEquals(Lifecycle.State.STARTED, host.getBackStackEntry(HOME_ROUTE).lifecycle.currentState)
            assertEquals(FIRMWARE_ROUTE, activity.rootNavigationController?.currentDestination?.route)
            val entry = requireNotNull(activity.rootNavigationController).getBackStackEntry(FIRMWARE_ROUTE)
            firstViewModel = ViewModelProvider(entry.viewModelStore, activity.defaultViewModelProviderFactory, entry.defaultViewModelCreationExtras)[org.secu3.android.ui.firmware.FirmwareViewModel::class.java]
        }

        compose.activityRule.scenario.recreate()
        compose.waitForIdle()
        val restoredHost = nativeHost()
        var restoredViewModel: org.secu3.android.ui.firmware.FirmwareViewModel? = null
        compose.activityRule.scenario.onActivity { activity ->
            val entry = requireNotNull(activity.rootNavigationController).getBackStackEntry(FIRMWARE_ROUTE)
            restoredViewModel = ViewModelProvider(entry.viewModelStore, activity.defaultViewModelProviderFactory, entry.defaultViewModelCreationExtras)[org.secu3.android.ui.firmware.FirmwareViewModel::class.java]
            assertEquals(Lifecycle.State.STARTED, restoredHost.getBackStackEntry(HOME_ROUTE).lifecycle.currentState)
        }
        assertSame(firstViewModel, restoredViewModel)
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
        compose.waitForIdle()
        assertNativeHostRestored(restoredHost, HOME_ROUTE)
        compose.onNodeWithTag("home-ecu-status", useUnmergedTree = true).assertExists()
        compose.onAllNodesWithText(compose.activity.getString(R.string.disconnect_device)).assertCountEquals(0)

        compose.activityRule.scenario.onActivity { it.openFirmware() }
        compose.waitForIdle()
        var reopenedViewModel: org.secu3.android.ui.firmware.FirmwareViewModel? = null
        compose.activityRule.scenario.onActivity { activity ->
            val entry = requireNotNull(activity.rootNavigationController).getBackStackEntry(FIRMWARE_ROUTE)
            reopenedViewModel = ViewModelProvider(entry.viewModelStore, activity.defaultViewModelProviderFactory, entry.defaultViewModelCreationExtras)[org.secu3.android.ui.firmware.FirmwareViewModel::class.java]
        }
        assertNotSame(restoredViewModel, reopenedViewModel)
    }

    private fun navigateNativeHostToHome(): NavHostController {
        val nativeHost = nativeHost()
        compose.activityRule.scenario.onActivity { nativeHost.navigate(HOME_ROUTE) }
        compose.waitForIdle()
        return nativeHost
    }

    private fun nativeHost(): NavHostController {
        lateinit var nativeHost: NavHostController
        compose.activityRule.scenario.onActivity { activity ->
            nativeHost = requireNotNull(activity.rootNavigationController)
        }
        return nativeHost
    }

    private fun currentParametersViewModel(): ParamsViewModel {
        lateinit var viewModel: ParamsViewModel
        compose.activityRule.scenario.onActivity { activity ->
            val controller = requireNotNull(activity.rootNavigationController)
            val entry = controller.getBackStackEntry(PARAMETERS_ROUTE)
            viewModel = ViewModelProvider(
                entry.viewModelStore,
                activity.defaultViewModelProviderFactory,
                entry.defaultViewModelCreationExtras,
            )[ParamsViewModel::class.java]
        }
        return viewModel
    }

    private fun currentHomeViewModel(): org.secu3.android.ui.home.HomeViewModel {
        lateinit var viewModel: org.secu3.android.ui.home.HomeViewModel
        compose.activityRule.scenario.onActivity { activity ->
            val entry = requireNotNull(activity.rootNavigationController).getBackStackEntry(HOME_ROUTE)
            viewModel = ViewModelProvider(entry.viewModelStore, activity.defaultViewModelProviderFactory, entry.defaultViewModelCreationExtras)[org.secu3.android.ui.home.HomeViewModel::class.java]
        }
        return viewModel
    }

    private fun assertOnParametersRoute() {
        compose.onNodeWithText(compose.activity.getString(R.string.title_activity_param)).assertExists()
        compose.waitUntil(5_000) {
            var resumed = false
            compose.activityRule.scenario.onActivity { activity ->
                val entry = activity.rootNavigationController?.currentBackStackEntry
                resumed = entry?.destination?.route == PARAMETERS_ROUTE && entry.lifecycle.currentState == Lifecycle.State.RESUMED
            }
            resumed
        }
    }

    private fun assertOnSettingsRoute(nativeHost: NavHostController) {
        compose.activityRule.scenario.onActivity { activity ->
            val underlying = nativeHost.previousBackStackEntry
            assertEquals(Lifecycle.State.CREATED, underlying?.lifecycle?.currentState)
            assertEquals(SETTINGS_ROUTE, activity.rootNavigationController?.currentDestination?.route)
            assertEquals(Lifecycle.State.RESUMED, nativeHost.getBackStackEntry(SETTINGS_ROUTE).lifecycle.currentState)
        }
        compose.onNodeWithText(compose.activity.getString(R.string.title_activity_settings)).assertExists()
    }

    private fun assertFullScreenComposeRoute(nativeHost: NavHostController, route: String, destination: String) {
        compose.activityRule.scenario.onActivity { activity ->
            val underlying = nativeHost.getBackStackEntry(destination)
            assertEquals(Lifecycle.State.CREATED, underlying.lifecycle.currentState)
            assertEquals(Lifecycle.State.RESUMED, nativeHost.getBackStackEntry(route).lifecycle.currentState)
            assertEquals(route, activity.rootNavigationController?.currentDestination?.route)
        }
    }

    private fun assertNativeHostRestored(nativeHost: NavHostController, destination: String) {
        compose.activityRule.scenario.onActivity { activity ->
            assertEquals(destination, nativeHost.currentDestination?.route)
            assertEquals(Lifecycle.State.RESUMED, nativeHost.getBackStackEntry(destination).lifecycle.currentState)
            assertEquals(destination, activity.rootNavigationController?.currentDestination?.route)
        }
    }

    private fun clickParametersToolbarBack() {
        compose.onNodeWithContentDescription(compose.activity.getString(R.string.back)).performClick()
        compose.waitForIdle()
    }

    private fun assertNativeHomeRestored(nativeHost: NavHostController) {
        compose.waitUntil(5_000) {
            var restored = false
            compose.activityRule.scenario.onActivity { activity ->
                restored = nativeHost.currentDestination?.route == HOME_ROUTE &&
                    activity.rootNavigationController?.currentDestination?.route == HOME_ROUTE
            }
            restored
        }
        compose.activityRule.scenario.onActivity { activity ->
            assertEquals(HOME_ROUTE, nativeHost.currentDestination?.route)
            assertEquals(HOME_ROUTE, activity.rootNavigationController?.currentDestination?.route)
        }
        compose.onNodeWithTag("home-card-parameters").assertExists()
    }
}
