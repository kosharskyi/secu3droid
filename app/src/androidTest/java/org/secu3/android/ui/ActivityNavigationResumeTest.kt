package org.secu3.android.ui

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Test
import org.secu3.android.connection.Connected
import org.secu3.android.connection.ConnectionState
import org.secu3.android.connection.Disconnected
import org.secu3.android.connection.Connecting
import org.secu3.android.connection.Reconnecting
import org.secu3.android.connection.UsbAwaitingEcu

class ActivityNavigationResumeTest {
    @Test fun queuedNavigationDrainsWhenActivityReturnsToResumed() = withActivity { scenario ->
        scenario.moveToState(Lifecycle.State.STARTED)
        scenario.onActivity { activity ->
            activity.openSettings()
            assertRoute(activity, START_ROUTE)
        }
        scenario.moveToState(Lifecycle.State.RESUMED)
        scenario.onActivity { assertRoute(it, SETTINGS_ROUTE) }
    }

    @Test fun disconnectedReturnsFromFeatureRouteToStartAfterResume() = withActivity { scenario ->
        scenario.onActivity { it.openSettings() }
        scenario.moveToState(Lifecycle.State.STARTED)
        scenario.onActivity { disconnect(it) }
        idle()
        scenario.onActivity { assertRoute(it, SETTINGS_ROUTE) }

        scenario.moveToState(Lifecycle.State.RESUMED)
        scenario.onActivity { assertRoute(it, START_ROUTE) }
    }

    @Test fun latestConnectedOnlyNavigatesHomeFromStart() = withActivity { scenario ->
        scenario.onActivity { it.openSettings() }
        scenario.moveToState(Lifecycle.State.STARTED)
        scenario.onActivity { disconnect(it) }
        idle()
        scenario.onActivity { publish(it, Connected) }
        idle()

        scenario.moveToState(Lifecycle.State.RESUMED)
        scenario.onActivity { assertRoute(it, SETTINGS_ROUTE) }
    }

    @Test fun explicitHomeNavigationWorksFromFeatureRoute() = withActivity { scenario ->
        scenario.onActivity { it.openSettings(); it.openHome() }
        idle()
        scenario.onActivity { assertRoute(it, HOME_ROUTE) }
    }

    @Test fun connectionAdmissionFollowsTheLiveStartDestination() = withActivity { scenario ->
        scenario.onActivity { activity ->
            assertEquals(true, connectionInitiationAllowed(activity))
            activity.openSettings()
            assertRoute(activity, SETTINGS_ROUTE)
            assertEquals(false, connectionInitiationAllowed(activity))
            activity.openStart()
            assertRoute(activity, START_ROUTE)
            assertEquals(true, connectionInitiationAllowed(activity))
        }
    }

    @Test fun lateBluetoothPermissionCompletionCannotStartOutsideStart() = withActivity { scenario ->
        scenario.onActivity { activity ->
            activity.openSettings()
            assertRoute(activity, SETTINGS_ROUTE)
            assertEquals(false, connectionInitiationAllowed(activity))
        }
        scenario.onActivity { activity ->
            kotlinx.coroutines.runBlocking { viewModel(activity).connectionManager.startBtConnection() }
            assertEquals(false, viewModel(activity).connectionManager.isConnectionRunning)
        }
    }

    @Test fun bluetoothReconnectingKeepsFeatureRoute() = withActivity { scenario ->
        scenario.onActivity { publish(it, Connected) }
        idle()
        scenario.onActivity { it.openSettings(); publish(it, Reconnecting) }
        idle()
        scenario.onActivity { assertRoute(it, SETTINGS_ROUTE) }
    }

    @Test fun handledDisconnectedDoesNotPullUserBackAfterRecreation() = withActivity { scenario ->
        scenario.onActivity { it.openSettings(); disconnect(it) }
        idle()
        scenario.onActivity { assertRoute(it, START_ROUTE); it.openSettings() }

        scenario.recreate()
        idle()
        scenario.onActivity { assertRoute(it, SETTINGS_ROUTE) }
    }

    @Test fun anotherLossReturnsToStartAfterHomeRecovery() = withActivity { scenario ->
        scenario.onActivity { publish(it, Connected) }
        idle()
        scenario.onActivity { assertRoute(it, HOME_ROUTE); publish(it, Disconnected) }
        idle()
        scenario.onActivity { assertRoute(it, START_ROUTE); publish(it, Connected) }
        idle()
        scenario.onActivity { assertRoute(it, HOME_ROUTE) }

        scenario.moveToState(Lifecycle.State.STARTED)
        scenario.onActivity { publish(it, Disconnected) }
        idle()
        scenario.moveToState(Lifecycle.State.RESUMED)
        scenario.onActivity { assertRoute(it, START_ROUTE) }
    }

    @Test fun cableRemovalAfterHandledEcuTimeoutReturnsFromLogsToStart() = withActivity { scenario ->
        scenario.onActivity { disconnect(it); publish(it, UsbAwaitingEcu) }
        idle()
        scenario.onActivity { assertRoute(it, START_ROUTE); it.openLogs() }
        idle()
        scenario.onActivity { assertRoute(it, LOGS_ROUTE); publish(it, Disconnected) }
        idle()
        scenario.onActivity { assertRoute(it, START_ROUTE) }
    }

    @Test fun rapidLossAndRecoveryUsesOnlyLatestStateAfterResume() = withActivity { scenario ->
        scenario.onActivity { it.openSettings() }
        scenario.moveToState(Lifecycle.State.STARTED)
        scenario.onActivity { activity ->
            publish(activity, Connected)
            publish(activity, Disconnected)
            publish(activity, UsbAwaitingEcu)
            publish(activity, Connected)
        }
        idle()
        scenario.moveToState(Lifecycle.State.RESUMED)
        idle()
        scenario.onActivity { assertRoute(it, SETTINGS_ROUTE) }
    }

    @Test fun queuedDisconnectedNavigationSurvivesActivityRecreation() = withActivity { scenario ->
        scenario.onActivity { it.openSettings() }
        scenario.moveToState(Lifecycle.State.STARTED)
        scenario.onActivity { disconnect(it) }
        idle()
        scenario.onActivity { activity ->
            assertRoute(activity, SETTINGS_ROUTE)
            assertEquals(null, viewModel(activity).pendingNavigation.value)
        }

        scenario.recreate()
        scenario.moveToState(Lifecycle.State.RESUMED)
        idle()
        scenario.onActivity { activity ->
            assertRoute(activity, START_ROUTE)
            assertEquals(null, viewModel(activity).pendingNavigation.value)
        }
    }

    @Test fun usbHandshakeStatusAfterResumeDoesNotInferAnEstablishedLoss() = withActivity { scenario ->
        scenario.onActivity { it.openSettings() }
        scenario.moveToState(Lifecycle.State.STARTED)
        scenario.onActivity { publish(it, Connected) }
        idle()
        scenario.onActivity { publish(it, Disconnected); publish(it, UsbAwaitingEcu) }
        idle()
        scenario.moveToState(Lifecycle.State.RESUMED)
        scenario.onActivity { assertRoute(it, SETTINGS_ROUTE) }
    }

    @Test fun initialUsbHandshakeTimeoutDoesNotNavigateAwayFromSettings() = withActivity { scenario ->
        scenario.onActivity { it.openSettings(); publish(it, Connecting); publish(it, UsbAwaitingEcu) }
        idle()
        scenario.onActivity { assertRoute(it, SETTINGS_ROUTE) }
    }

    @Test fun connectedReplayDoesNotInterruptFeatureRouteAfterRecreation() = withActivity { scenario ->
        scenario.onActivity { publish(it, Connected) }
        idle()
        scenario.onActivity { assertRoute(it, HOME_ROUTE); it.openSettings() }
        scenario.recreate()
        idle()
        scenario.onActivity { assertRoute(it, SETTINGS_ROUTE) }
    }

    @Test fun pendingHomeIsDiscardedWhenConnectionPreparationRestarts() = withActivity { scenario ->
        scenario.onActivity { it.openSettings() }
        scenario.moveToState(Lifecycle.State.STARTED)
        scenario.onActivity { publish(it, Connected) }
        idle()
        scenario.onActivity { publish(it, Connecting) }
        idle()
        scenario.moveToState(Lifecycle.State.RESUMED)
        scenario.onActivity { assertRoute(it, SETTINGS_ROUTE) }
    }

    private fun withActivity(block: (ActivityScenario<MainActivity>) -> Unit) {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            try {
                scenario.onActivity { disconnect(it) }
                idle()
                block(scenario)
            } finally {
                scenario.onActivity { disconnect(it) }
                idle()
            }
        }
    }

    private fun disconnect(activity: MainActivity) {
        publish(activity, Connecting)
        publish(activity, Disconnected)
    }

    private fun publish(activity: MainActivity, state: ConnectionState) {
        val manager = viewModel(activity).connectionManager
        manager.javaClass.getDeclaredMethod("publishConnectionState", ConnectionState::class.java)
            .apply { isAccessible = true }.invoke(manager, state)
    }

    private fun viewModel(activity: MainActivity) = ViewModelProvider(activity)[MainActivityViewModel::class.java]
    private fun connectionInitiationAllowed(activity: MainActivity): Boolean {
        val manager = viewModel(activity).connectionManager
        return manager.javaClass.getDeclaredField("connectionInitiationAllowed").apply { isAccessible = true }.getBoolean(manager)
    }
    private fun assertRoute(activity: MainActivity, expected: String) = assertEquals(expected, activity.rootNavigationController?.currentDestination?.route)
    private fun idle() = InstrumentationRegistry.getInstrumentation().waitForIdleSync()
}
