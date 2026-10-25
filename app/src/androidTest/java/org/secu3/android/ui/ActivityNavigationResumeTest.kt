package org.secu3.android.ui

import androidx.test.core.app.ActivityScenario
import androidx.lifecycle.ViewModelProvider
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Test

class ActivityNavigationResumeTest {
    @Test fun queuedNavigationDrainsWhenActivityReturnsToResumed() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.moveToState(androidx.lifecycle.Lifecycle.State.STARTED)
            scenario.onActivity { activity ->
                activity.openSettings()
                assertEquals(START_ROUTE, activity.rootNavigationController?.currentDestination?.route)
            }

            scenario.moveToState(androidx.lifecycle.Lifecycle.State.RESUMED)

            scenario.onActivity { activity ->
                assertEquals(SETTINGS_ROUTE, activity.rootNavigationController?.currentDestination?.route)
            }
        }
    }

    @Test fun finalConnectionLossReturnsFromFeatureRouteToStart() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.openSettings()
            }
            scenario.moveToState(androidx.lifecycle.Lifecycle.State.STARTED)
            scenario.onActivity { activity ->
                terminalFlow(activity).value = 700L
            }
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            scenario.onActivity { activity ->
                assertEquals(SETTINGS_ROUTE, activity.rootNavigationController?.currentDestination?.route)
            }

            scenario.moveToState(androidx.lifecycle.Lifecycle.State.RESUMED)

            scenario.onActivity { activity ->
                assertEquals(START_ROUTE, activity.rootNavigationController?.currentDestination?.route)
            }
        }
    }

    @Test fun replacementAttemptClearsQueuedTerminalStartBeforeResume() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.openSettings()
            }
            scenario.moveToState(androidx.lifecycle.Lifecycle.State.STARTED)
            scenario.onActivity { activity ->
                terminalFlow(activity).value = 701L
            }
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            scenario.onActivity { activity ->
                terminalFlow(activity).value = null
            }
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()

            scenario.moveToState(androidx.lifecycle.Lifecycle.State.RESUMED)

            scenario.onActivity { activity ->
                assertEquals(SETTINGS_ROUTE, activity.rootNavigationController?.currentDestination?.route)
            }
        }
    }

    @Test fun replayedHandledTerminalResultDoesNotPullUserBackToStart() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.openSettings()
                terminalFlow(activity).value = 702L
            }
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            scenario.onActivity { activity ->
                assertEquals(START_ROUTE, activity.rootNavigationController?.currentDestination?.route)
                activity.openSettings()
            }

            scenario.recreate()
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()

            scenario.onActivity { activity ->
                assertEquals(SETTINGS_ROUTE, activity.rootNavigationController?.currentDestination?.route)
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun terminalFlow(activity: MainActivity): MutableStateFlow<Long?> {
        val manager = ViewModelProvider(activity)[MainActivityViewModel::class.java].connectionManager
        return manager.javaClass.getDeclaredField("_terminalSessionGeneration").apply { isAccessible = true }
            .get(manager) as MutableStateFlow<Long?>
    }
}
