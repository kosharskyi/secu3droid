package org.secu3.android.connection

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UsbAttachmentAttemptGateTest {
    @Test fun systemOpenConfirmationCanGrantAccessAfterTheInitialPermissionRequest() {
        val gate = UsbAttachmentAttemptGate()
        gate.begin(deviceId = 32)
        assertFalse(gate.hasOpened(32))
        assertNull(gate.begin(deviceId = 32))

        // Android's attach chooser can grant access separately from requestPermission's result.
        assertTrue(gate.claimOpen(32))
        assertTrue(gate.hasOpened(32))
        assertFalse(gate.claimOpen(32))
        gate.detach(32)
        gate.begin(32)
        assertFalse(gate.hasOpened(32))
        assertTrue(gate.claimOpen(32))
    }

    @Test fun permissionDeniedDoesNotRepeatUntilPhysicalDetach() {
        val gate = UsbAttachmentAttemptGate()
        val token = gate.begin(deviceId = 12)!!

        assertTrue(gate.accepts(deviceId = 12, token = token))
        assertNull(gate.begin(deviceId = 12))
        assertTrue(gate.hasAttempted(12))

        gate.detach(12)
        val nextToken = gate.begin(deviceId = 12)!!
        assertTrue(nextToken > token)
        assertFalse(gate.accepts(deviceId = 12, token = token))
        assertTrue(gate.accepts(deviceId = 12, token = nextToken))
    }

    @Test fun unrelatedDetachCannotInvalidatePendingOrConfirmedAttachmentAttempt() {
        val gate = UsbAttachmentAttemptGate()
        val token = gate.begin(deviceId = 20)!!

        gate.detach(21)

        assertNull(gate.begin(deviceId = 20))
        assertTrue(gate.accepts(deviceId = 20, token = token))
    }
}
