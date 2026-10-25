package org.secu3.android.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.secu3.android.connection.Secu3ConnectionManager
import org.secu3.android.utils.UserPrefs
import sun.misc.Unsafe

class UsbNavigationLifecycleTest {
    @Test fun terminalNavigationRemainsPendingUntilItIsActuallyHandled() {
        val viewModel = navigationViewModel()
        assertTrue(viewModel.claimUsbHomeNavigation(1))
        assertTrue(viewModel.queueTerminalStart(2))
        assertTrue(viewModel.queueTerminalStart(2))
        assertTrue(viewModel.pendingTerminalStart() == 2L)

        viewModel.completeTerminalStart(2)

        assertTrue(viewModel.pendingTerminalStart() == null)
        assertFalse(viewModel.queueTerminalStart(2))
    }

    @Test fun replacementAttemptCanCancelQueuedTerminalNavigation() {
        val viewModel = navigationViewModel()
        assertTrue(viewModel.claimUsbHomeNavigation(3))
        assertTrue(viewModel.queueTerminalStart(4))

        assertTrue(viewModel.cancelPendingTerminalStart() == 4L)
        assertTrue(viewModel.pendingTerminalStart() == null)
        assertTrue(viewModel.queueTerminalStart(5))
    }

    // Navigation bookkeeping does not call platform or transport APIs. Uninitialized dependencies
    // keep this regression test independent of Android's USB service, as in transport unit fixtures.
    private fun navigationViewModel(): MainActivityViewModel {
        val unsafe = Unsafe::class.java.getDeclaredField("theUnsafe").apply { isAccessible = true }.get(null) as Unsafe
        return MainActivityViewModel(
            unsafe.allocateInstance(UserPrefs::class.java) as UserPrefs,
            unsafe.allocateInstance(Secu3ConnectionManager::class.java) as Secu3ConnectionManager,
        )
    }
}
