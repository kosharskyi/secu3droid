/*
 *    SecuDroid  - An open source, free manager for SECU-3 engine control unit
 *    Copyright (C) 2024 Vitaliy O. Kosharskyi. Ukraine, Kyiv
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
package org.secu3.android.ui

import android.app.ComponentCaller
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavHostController
import dagger.hilt.android.AndroidEntryPoint
import org.secu3.android.R

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val viewModel: MainActivityViewModel by viewModels()
    private var rootNavController: NavHostController? = null
    internal val rootNavigationController: NavHostController? get() = rootNavController
    private var pendingComposeRoute: String? = null
    private var activeComposeRoute: String? = null
    private lateinit var legacyContainer: View
    private lateinit var composeNavigationView: ComposeView

    companion object {
        private const val STATE_ACTIVE_COMPOSE_ROUTE = "active_compose_route"
        private const val STATE_PARAMETERS_ROUTE_ACTIVE = "parameters_route_active"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        legacyContainer = findViewById(R.id.nav_host_fragment)
        composeNavigationView = ComposeView(this).apply {
            id = R.id.root_compose_nav_view
            visibility = View.GONE
            setContent { RootNavigation(this@MainActivity) }
        }
        findViewById<ConstraintLayout>(R.id.main_activity_root).addView(
            composeNavigationView,
            ConstraintLayout.LayoutParams(0, 0).apply {
                startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
                topToTop = ConstraintLayout.LayoutParams.PARENT_ID
                bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
            },
        )
        activeComposeRoute = savedInstanceState?.getString(STATE_ACTIVE_COMPOSE_ROUTE)
            ?: if (savedInstanceState?.getBoolean(STATE_PARAMETERS_ROUTE_ACTIVE) == true) PARAMETERS_ROUTE else null
        if (activeComposeRoute != null) {
            if (setNativeHostMaxLifecycle(Lifecycle.State.CREATED)) {
                legacyContainer.visibility = View.GONE
                composeNavigationView.visibility = View.VISIBLE
            } else {
                pendingComposeRoute = activeComposeRoute
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(STATE_ACTIVE_COMPOSE_ROUTE, activeComposeRoute)
        outState.putBoolean(STATE_PARAMETERS_ROUTE_ACTIVE, activeComposeRoute == PARAMETERS_ROUTE)
        super.onSaveInstanceState(outState)
    }

    override fun onPostResume() {
        super.onPostResume()
        when (rootNavController?.currentDestination?.route) {
            PARAMETERS_ROUTE, SETTINGS_ROUTE -> showComposeOverlay(rootNavController?.currentDestination?.route)
            LEGACY_ROUTE -> showLegacyHost()
        }
        processPendingComposeNavigation()
    }

    private fun processPendingComposeNavigation() {
        val route = rootNavController?.currentDestination?.route ?: return
        val pendingRoute = pendingComposeRoute
        if (pendingRoute != null) {
            pendingComposeRoute = null
            if (route != pendingRoute) {
                openComposeDestination(pendingRoute)
            } else {
                showComposeOverlay(pendingRoute)
            }
        }
    }

    fun openParameters() {
        openComposeDestination(PARAMETERS_ROUTE)
    }

    fun openSettings() {
        openComposeDestination(SETTINGS_ROUTE)
    }

    private fun openComposeDestination(route: String) {
        val controller = rootNavController
        if (supportFragmentManager.isStateSaved || controller?.currentDestination == null) {
            pendingComposeRoute = route
            return
        }
        if (controller.currentDestination?.route == route) return
        if (!showComposeOverlay(route)) {
            pendingComposeRoute = route
            return
        }
        controller.navigate(route) { launchSingleTop = true }
    }

    internal fun attachRootNavController(controller: NavHostController) {
        rootNavController = controller
        if (pendingComposeRoute != null && lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            onRootDestinationChanged(controller.currentDestination?.route)
            processPendingComposeNavigation()
        }
    }

    internal fun detachRootNavController(controller: NavHostController) {
        if (rootNavController === controller) rootNavController = null
    }

    internal fun onRootDestinationChanged(route: String?) {
        when (route) {
            PARAMETERS_ROUTE, SETTINGS_ROUTE -> showComposeOverlay(route)
            LEGACY_ROUTE -> showLegacyHost()
        }
        if (pendingComposeRoute != null && lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            processPendingComposeNavigation()
        }
    }

    private fun showComposeOverlay(route: String?): Boolean {
        if (route == null) return false
        if (supportFragmentManager.isStateSaved || !setNativeHostMaxLifecycle(Lifecycle.State.CREATED)) return false
        activeComposeRoute = route
        legacyContainer.visibility = View.GONE
        composeNavigationView.visibility = View.VISIBLE
        return true
    }

    private fun showLegacyHost(): Boolean {
        if (supportFragmentManager.isStateSaved || !setNativeHostMaxLifecycle(Lifecycle.State.RESUMED)) return false
        activeComposeRoute = null
        legacyContainer.visibility = View.VISIBLE
        composeNavigationView.visibility = View.GONE
        handleKeepScreenOn()
        return true
    }

    private fun setNativeHostMaxLifecycle(state: Lifecycle.State): Boolean {
        if (supportFragmentManager.isStateSaved) return false
        val host: Fragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) ?: return false
        if (host.lifecycle.currentState != state) {
            supportFragmentManager.beginTransaction()
                .setMaxLifecycle(host, state)
                .commitNow()
        }
        return true
    }

    override fun onResume() {
        super.onResume()

        handleKeepScreenOn()
    }

    private fun handleKeepScreenOn() {
        if (viewModel.prefs.isKeepScreenAliveActive) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    override fun onNewIntent(intent: Intent, caller: ComponentCaller) {
        super.onNewIntent(intent, caller)

        // do nothing
    }
}
