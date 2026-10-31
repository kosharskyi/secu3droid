package org.secu3.android.ui

import androidx.test.core.app.ActivityScenario
import androidx.lifecycle.ViewModelProvider
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertTrue
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

    @Test fun newUsbSessionCanReturnToStartAfterHomeRecovery() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                terminalFlow(activity).value = 704L
            }
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            scenario.onActivity { activity ->
                assertEquals(START_ROUTE, activity.rootNavigationController?.currentDestination?.route)
                activity.openHome()
                val viewModel = ViewModelProvider(activity)[MainActivityViewModel::class.java]
                assertTrue(viewModel.claimUsbHomeNavigation(10L))
                assertEquals(HOME_ROUTE, activity.rootNavigationController?.currentDestination?.route)
            }

            scenario.moveToState(androidx.lifecycle.Lifecycle.State.STARTED)
            scenario.onActivity { activity -> terminalFlow(activity).value = null }
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            scenario.onActivity { activity -> terminalFlow(activity).value = 706L }
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            scenario.moveToState(androidx.lifecycle.Lifecycle.State.RESUMED)

            scenario.onActivity { activity ->
                assertEquals(START_ROUTE, activity.rootNavigationController?.currentDestination?.route)
            }
        }
    }

    @Test fun cableRemovalAfterHandledEcuTimeoutReturnsFromLogsToStart() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity -> terminalFlow(activity).value = 707L }
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            scenario.onActivity { activity ->
                assertEquals(START_ROUTE, activity.rootNavigationController?.currentDestination?.route)
                activity.openLogs()
            }
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            scenario.onActivity { activity ->
                assertEquals(LOGS_ROUTE, activity.rootNavigationController?.currentDestination?.route)
                terminalFlow(activity).value = 708L
            }
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            scenario.onActivity { activity ->
                assertEquals(START_ROUTE, activity.rootNavigationController?.currentDestination?.route)
            }
        }
    }

    @Test fun confirmedHomeClaimCancelsAnAlreadyQueuedStart() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            try {
                scenario.onActivity { activity ->
                    activity.openSettings()
                    val viewModel = ViewModelProvider(activity)[MainActivityViewModel::class.java]
                    assertTrue(viewModel.queueTerminalStart(705L))
                    setPrivateField(activity, "pendingNavigation", START_ROUTE)
                    setConfirmedUsbSession(viewModel.connectionManager, attemptId = 3L)
                    activity.javaClass.getDeclaredMethod("openUsbHome").apply { isAccessible = true }.invoke(activity)
                    assertEquals(HOME_ROUTE, activity.rootNavigationController?.currentDestination?.route)
                }
                InstrumentationRegistry.getInstrumentation().waitForIdleSync()
                scenario.onActivity { activity ->
                    assertEquals("recovery must supersede the queued Start", HOME_ROUTE, activity.rootNavigationController?.currentDestination?.route)
                }
            } finally {
                scenario.onActivity { activity ->
                    ViewModelProvider(activity)[MainActivityViewModel::class.java].connectionManager.onUsbDeviceDetached(1)
                    terminalFlow(activity).value = null
                }
            }
        }
    }

    @Test fun queuedTerminalNavigationSurvivesActivityRecreation() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { it.openSettings() }
            scenario.moveToState(androidx.lifecycle.Lifecycle.State.STARTED)
            scenario.onActivity { terminalFlow(it).value = 703L }
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()

            scenario.onActivity { activity ->
                assertEquals(SETTINGS_ROUTE, activity.rootNavigationController?.currentDestination?.route)
                assertEquals(703L, ViewModelProvider(activity)[MainActivityViewModel::class.java].pendingTerminalStart())
            }
            scenario.recreate()
            scenario.moveToState(androidx.lifecycle.Lifecycle.State.RESUMED)

            scenario.onActivity { activity ->
                assertEquals(START_ROUTE, activity.rootNavigationController?.currentDestination?.route)
                assertEquals(null, ViewModelProvider(activity)[MainActivityViewModel::class.java].pendingTerminalStart())
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun terminalFlow(activity: MainActivity): MutableStateFlow<Long?> {
        val manager = ViewModelProvider(activity)[MainActivityViewModel::class.java].connectionManager
        return manager.javaClass.getDeclaredField("_terminalSessionGeneration").apply { isAccessible = true }
            .get(manager) as MutableStateFlow<Long?>
    }

    private fun setConfirmedUsbSession(manager: org.secu3.android.connection.Secu3ConnectionManager, attemptId: Long) {
        val transportClass = Class.forName("org.secu3.android.connection.Secu3ConnectionManager\$Transport")
        val usb = transportClass.enumConstants!!.first { (it as Enum<*>).name == "USB" }
        val selectionClass = Class.forName("org.secu3.android.connection.Secu3ConnectionManager\$Selection")
        val fence = manager.javaClass.getDeclaredField("usbSessionFence").apply { isAccessible = true }.get(manager)
        val generation = fence
            .javaClass.getDeclaredMethod("begin", Int::class.javaPrimitiveType).apply { isAccessible = true }
            .invoke(fence, 1) as Long
        val selection = selectionClass.declaredConstructors.single().apply { isAccessible = true }.newInstance(usb, generation)
        @Suppress("UNCHECKED_CAST")
        (manager.javaClass.getDeclaredField("selectedTransport").apply { isAccessible = true }.get(manager) as MutableStateFlow<Any>).value = selection
        val attachmentClass = Class.forName("org.secu3.android.connection.UsbAttachmentIdentity")
        val attachment = attachmentClass.declaredConstructors.single().apply { isAccessible = true }
            .newInstance(1, "test-device", 1, 1)
        setPrivateField(manager, "activeUsbAttachment", attachment)
        setPrivateField(manager, "activeUsbAttemptId", attemptId)
        setPrivateField(manager, "confirmedUsbAttemptId", attemptId)
    }

    private fun setPrivateField(target: Any, name: String, value: Any?) {
        target.javaClass.getDeclaredField(name).apply { isAccessible = true }.set(target, value)
    }
}
