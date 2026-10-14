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
import android.view.View
import androidx.compose.ui.test.assertIsSelected
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.fragment.NavHostFragment
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
            assertEquals(Lifecycle.State.CREATED, nativeHost.lifecycle.currentState)
            assertEquals(View.GONE, activity.findViewById<View>(R.id.nav_host_fragment).visibility)
            assertEquals(View.VISIBLE, activity.findViewById<View>(R.id.root_compose_nav_view).visibility)
            assertEquals(PARAMETERS_ROUTE, activity.rootNavigationController?.currentDestination?.route)
        }
        assertOnParametersRoute()

        clickParametersToolbarBack()
        assertNativeHomeRestored(nativeHost)

        compose.onNodeWithTag("home-card-parameters").performClick()
        compose.waitForIdle()
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
        compose.waitForIdle()
        assertNativeHomeRestored(nativeHost)
    }

    @Test fun routePagerRestoreThenPopCreatesFreshViewModel() {
        navigateNativeHostToHome()
        compose.onNodeWithTag("home-card-parameters").performClick()
        compose.waitForIdle()
        val initialViewModel = currentParametersViewModel()
        val anglesTitle = compose.activity.getString(R.string.params_tab_angles_title)
        compose.onNodeWithContentDescription(anglesTitle).performClick()
        compose.waitForIdle()
        compose.onNodeWithContentDescription(anglesTitle).assertIsSelected()
        assertNull(initialViewModel.parameterEditSession)

        compose.activityRule.scenario.recreate()
        compose.waitForIdle()
        val retainedViewModel = currentParametersViewModel()
        assertSame(initialViewModel, retainedViewModel)
        compose.onNodeWithContentDescription(anglesTitle).assertIsSelected()
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
        assertNativeHostRestored(nativeHost, R.id.startScreenFragment)

        compose.onNodeWithContentDescription("Settings icon").performClick()
        compose.waitForIdle()
        assertOnSettingsRoute(nativeHost)
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
        compose.waitForIdle()
        assertNativeHostRestored(nativeHost, R.id.startScreenFragment)

        compose.activityRule.scenario.onActivity { nativeHost.navController.navigate(R.id.homeFragment) }
        compose.waitForIdle()
        compose.onNodeWithTag("home-card-settings").performScrollTo().performClick()
        compose.waitForIdle()
        assertOnSettingsRoute(nativeHost)
        compose.onNodeWithContentDescription(compose.activity.getString(R.string.back)).performClick()
        compose.waitForIdle()
        assertNativeHostRestored(nativeHost, R.id.homeFragment)

        compose.onNodeWithTag("home-card-settings").performScrollTo().performClick()
        compose.waitForIdle()
        assertOnSettingsRoute(nativeHost)
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
        compose.waitForIdle()
        assertNativeHostRestored(nativeHost, R.id.homeFragment)
    }

    @Test fun settingsOverlayIsRestoredAcrossActivityRecreation() {
        val nativeHost = nativeHost()
        compose.onNodeWithContentDescription("Settings icon").performClick()
        compose.waitForIdle()
        assertOnSettingsRoute(nativeHost)
        compose.activityRule.scenario.recreate()
        compose.waitForIdle()
        val restoredHost = compose.activity.supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        assertOnSettingsRoute(restoredHost)
        compose.onNodeWithText(compose.activity.getString(R.string.title_activity_settings)).assertExists()
    }

    @Test fun startAndHomeLogsRoutesRestoreTheirLegacyDestinations() {
        val host = nativeHost()
        compose.onNodeWithContentDescription("Logs icon").performClick()
        compose.waitForIdle()
        assertFullScreenComposeRoute(host, LOGS_ROUTE, R.id.startScreenFragment)
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
        compose.waitForIdle()
        assertNativeHostRestored(host, R.id.startScreenFragment)

        compose.activityRule.scenario.onActivity { host.navController.navigate(R.id.homeFragment) }
        compose.waitForIdle()
        compose.onNodeWithTag("home-card-logs").performScrollTo().performClick()
        compose.waitForIdle()
        assertFullScreenComposeRoute(host, LOGS_ROUTE, R.id.homeFragment)
        compose.onNodeWithContentDescription(compose.activity.getString(R.string.back)).performClick()
        compose.waitForIdle()
        assertNativeHostRestored(host, R.id.homeFragment)
    }

    @Test fun homeErrorsRouteRestoresSystemBarsAndNativeHost() {
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
        assertFullScreenComposeRoute(host, ERRORS_ROUTE, R.id.homeFragment)
        compose.activityRule.scenario.onActivity { activity ->
            val insets = WindowInsetsControllerCompat(activity.window, activity.window.decorView)
            assertEquals(false, insets.isAppearanceLightStatusBars)
            assertEquals(false, insets.isAppearanceLightNavigationBars)
        }
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
        compose.waitForIdle()
        assertNativeHostRestored(host, R.id.homeFragment)
        compose.activityRule.scenario.onActivity { activity ->
            val insets = WindowInsetsControllerCompat(activity.window, activity.window.decorView)
            assertEquals(originalStatusBarColor, activity.window.statusBarColor)
            assertEquals(originalNavigationBarColor, activity.window.navigationBarColor)
            assertEquals(originalLightStatusBars, insets.isAppearanceLightStatusBars)
            assertEquals(originalLightNavigationBars, insets.isAppearanceLightNavigationBars)
        }
    }

    @Test fun errorsClearConfirmationSurvivesRecreationAndCancelDoesNotClear() {
        navigateNativeHostToHome()
        compose.onNodeWithTag("home-card-errors").performScrollTo().performClick()
        compose.waitForIdle()
        compose.onNodeWithContentDescription(compose.activity.getString(R.string.errors_clear_all_short)).performClick()
        compose.onNodeWithText(compose.activity.getString(R.string.dialog_confirm_clear_errors_message)).assertExists()

        compose.activityRule.scenario.recreate()
        compose.waitForIdle()
        compose.onNodeWithText(compose.activity.getString(R.string.dialog_confirm_clear_errors_message)).assertExists()
        compose.onNodeWithText(compose.activity.getString(android.R.string.cancel)).performClick()
        compose.waitForIdle()
        compose.onAllNodesWithText(compose.activity.getString(R.string.dialog_confirm_clear_errors_message)).assertCountEquals(0)
    }

    @Test fun firmwareModalKeepsHomeStartedBackClosesOnlySheetAndViewModelIsPerEntry() {
        val host = navigateNativeHostToHome()
        compose.activityRule.scenario.onActivity { it.openFirmware() }
        compose.waitForIdle()
        compose.onNodeWithText(compose.activity.getString(R.string.firmware_information_title)).assertExists()

        var firstViewModel: org.secu3.android.ui.firmware.FirmwareViewModel? = null
        compose.activityRule.scenario.onActivity { activity ->
            assertEquals(Lifecycle.State.STARTED, host.lifecycle.currentState)
            assertEquals(View.VISIBLE, activity.findViewById<View>(R.id.nav_host_fragment).visibility)
            assertEquals(View.VISIBLE, activity.findViewById<View>(R.id.root_compose_nav_view).visibility)
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
            assertEquals(Lifecycle.State.STARTED, restoredHost.lifecycle.currentState)
        }
        assertSame(firstViewModel, restoredViewModel)
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
        compose.waitForIdle()
        assertNativeHostRestored(restoredHost, R.id.homeFragment)
        compose.onNodeWithTag("home-ecu-status").assertExists()
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

    private fun navigateNativeHostToHome(): NavHostFragment {
        val nativeHost = nativeHost()
        compose.activityRule.scenario.onActivity { nativeHost.navController.navigate(R.id.homeFragment) }
        compose.waitForIdle()
        return nativeHost
    }

    private fun nativeHost(): NavHostFragment {
        lateinit var nativeHost: NavHostFragment
        compose.activityRule.scenario.onActivity { activity ->
            nativeHost = activity.supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
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

    private fun assertOnParametersRoute() {
        compose.onNodeWithText(compose.activity.getString(R.string.title_activity_param)).assertExists()
    }

    private fun assertOnSettingsRoute(nativeHost: NavHostFragment) {
        compose.activityRule.scenario.onActivity { activity ->
            assertEquals(Lifecycle.State.CREATED, nativeHost.lifecycle.currentState)
            assertEquals(View.GONE, activity.findViewById<View>(R.id.nav_host_fragment).visibility)
            assertEquals(View.VISIBLE, activity.findViewById<View>(R.id.root_compose_nav_view).visibility)
            assertEquals(SETTINGS_ROUTE, activity.rootNavigationController?.currentDestination?.route)
        }
        compose.onNodeWithText(compose.activity.getString(R.string.title_activity_settings)).assertExists()
    }

    private fun assertFullScreenComposeRoute(nativeHost: NavHostFragment, route: String, destination: Int) {
        compose.activityRule.scenario.onActivity { activity ->
            assertEquals(Lifecycle.State.CREATED, nativeHost.lifecycle.currentState)
            assertEquals(destination, nativeHost.navController.currentDestination?.id)
            assertEquals(View.GONE, activity.findViewById<View>(R.id.nav_host_fragment).visibility)
            assertEquals(View.VISIBLE, activity.findViewById<View>(R.id.root_compose_nav_view).visibility)
            assertEquals(route, activity.rootNavigationController?.currentDestination?.route)
        }
    }

    private fun assertNativeHostRestored(nativeHost: NavHostFragment, destination: Int) {
        compose.activityRule.scenario.onActivity { activity ->
            assertEquals(destination, nativeHost.navController.currentDestination?.id)
            assertEquals(Lifecycle.State.RESUMED, nativeHost.lifecycle.currentState)
            assertEquals(View.VISIBLE, activity.findViewById<View>(R.id.nav_host_fragment).visibility)
            assertEquals(View.GONE, activity.findViewById<View>(R.id.root_compose_nav_view).visibility)
            assertEquals(LEGACY_ROUTE, activity.rootNavigationController?.currentDestination?.route)
        }
    }

    private fun clickParametersToolbarBack() {
        compose.onNodeWithContentDescription(compose.activity.getString(R.string.back)).performClick()
        compose.waitForIdle()
    }

    private fun assertNativeHomeRestored(nativeHost: NavHostFragment) {
        compose.activityRule.scenario.onActivity { activity ->
            assertEquals(R.id.homeFragment, nativeHost.navController.currentDestination?.id)
            assertEquals(Lifecycle.State.RESUMED, nativeHost.lifecycle.currentState)
            assertEquals(View.VISIBLE, activity.findViewById<View>(R.id.nav_host_fragment).visibility)
            assertEquals(View.GONE, activity.findViewById<View>(R.id.root_compose_nav_view).visibility)
            assertEquals("legacy", activity.rootNavigationController?.currentDestination?.route)
        }
        compose.onNodeWithTag("home-card-parameters").assertExists()
    }
}
