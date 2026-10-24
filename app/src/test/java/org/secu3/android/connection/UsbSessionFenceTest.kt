package org.secu3.android.connection

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UsbSessionFenceTest {
    @Test fun onlyTheActiveDeviceCanDetachTheCurrentGeneration() {
        val fence = UsbSessionFence()
        val firstGeneration = fence.begin(deviceId = 44)

        assertFalse(fence.detach(deviceId = 45))
        assertTrue(fence.isCurrent(deviceId = 44, generation = firstGeneration))
        assertTrue(fence.detach(deviceId = 44))
        assertFalse(fence.isCurrent(deviceId = 44, generation = firstGeneration))
    }

    @Test fun staleTimeoutFromAnOldAttachmentCannotMatchItsReplacement() {
        val fence = UsbSessionFence()
        val oldGeneration = fence.begin(deviceId = 60)
        assertTrue(fence.detach(deviceId = 60))
        val newGeneration = fence.begin(deviceId = 60)

        assertNotEquals(oldGeneration, newGeneration)
        assertFalse(fence.isCurrent(deviceId = 60, generation = oldGeneration))
        assertTrue(fence.isCurrent(deviceId = 60, generation = newGeneration))
    }

    @Test fun protocolGateOpensOnlyAfterCurrentFirmwareHandshake() {
        val fence = UsbSessionFence()
        val generation = fence.begin(deviceId = 72)

        assertFalse(fence.isConfirmed(deviceId = 72, generation = generation))
        assertFalse(fence.confirm(deviceId = 73, generation = generation))
        assertFalse(fence.isConfirmed(deviceId = 72, generation = generation))
        assertTrue(fence.confirm(deviceId = 72, generation = generation))
        assertTrue(fence.isConfirmed(deviceId = 72, generation = generation))

        fence.invalidateConfirmation(deviceId = 72, generation = generation)
        assertFalse(fence.isConfirmed(deviceId = 72, generation = generation))
    }
}
