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
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
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

    private fun navigateNativeHostToHome(): NavHostFragment {
        lateinit var nativeHost: NavHostFragment
        compose.activityRule.scenario.onActivity { activity ->
            nativeHost = activity.supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
            nativeHost.navController.navigate(R.id.homeFragment)
        }
        compose.waitForIdle()
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
