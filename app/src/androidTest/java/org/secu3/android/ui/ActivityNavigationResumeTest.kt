package org.secu3.android.ui

import androidx.test.core.app.ActivityScenario
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
}
