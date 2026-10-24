package org.secu3.android.connection

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UsbAttachmentAttemptGateTest {
    @Test fun unsupportedAndConflictingDevicesNeverAcquireAnAttempt() {
        val gate = UsbAttachmentAttemptGate()

        assertEquals(UsbAdmissionDecision.Ignored, gate.request(attachment(1), supported = false, conflictsWithActiveDevice = false, hasPermission = true))
        assertEquals(UsbAdmissionDecision.Ignored, gate.request(attachment(2), supported = true, conflictsWithActiveDevice = true, hasPermission = false))
        assertFalse(gate.hasAttempted(attachment(1)))
        assertFalse(gate.hasAttempted(attachment(2)))
    }

    @Test fun pendingPermissionBlocksOtherDevicesAndLaterChooserGrantOpensOnce() {
        val gate = UsbAttachmentAttemptGate()
        val first = gate.request(attachment(32), supported = true, conflictsWithActiveDevice = false, hasPermission = false)
        assertTrue(first is UsbAdmissionDecision.PermissionRequired)
        val token = (first as UsbAdmissionDecision.PermissionRequired).token

        assertEquals(UsbAdmissionDecision.Ignored, gate.request(attachment(33), supported = true, conflictsWithActiveDevice = false, hasPermission = true))
        assertEquals(UsbAdmissionDecision.Ignored, gate.request(attachment(32), supported = true, conflictsWithActiveDevice = false, hasPermission = false))
        assertFalse(gate.completePermission(attachment(32), token, permissionGranted = false, hasPermission = false, attached = true, supported = true, conflictsWithActiveDevice = false))
        assertTrue(gate.request(attachment(33), supported = true, conflictsWithActiveDevice = false, hasPermission = false) is UsbAdmissionDecision.PermissionRequired)

        assertEquals(UsbAdmissionDecision.Started, gate.request(attachment(32), supported = true, conflictsWithActiveDevice = false, hasPermission = true))
        assertEquals(UsbAdmissionDecision.Ignored, gate.request(attachment(32), supported = true, conflictsWithActiveDevice = false, hasPermission = true))
        assertTrue(gate.hasOpened(attachment(32)))
    }

    @Test fun completionRequiresFreshPermissionAttachmentSupportAndNoConflict() {
        fun rejected(attached: Boolean, supported: Boolean, hasPermission: Boolean, conflicts: Boolean): Boolean {
            val gate = UsbAttachmentAttemptGate()
            val target = attachment(12)
            val token = (gate.request(target, true, false, false) as UsbAdmissionDecision.PermissionRequired).token
            return gate.completePermission(target, token, true, hasPermission, attached, supported, conflicts)
        }

        assertFalse(rejected(attached = true, supported = true, hasPermission = false, conflicts = false))
        assertFalse(rejected(attached = false, supported = true, hasPermission = true, conflicts = false))
        assertFalse(rejected(attached = true, supported = false, hasPermission = true, conflicts = false))
        assertFalse(rejected(attached = true, supported = true, hasPermission = true, conflicts = true))
        val gate = UsbAttachmentAttemptGate()
        val target = attachment(12)
        val token = (gate.request(target, true, false, false) as UsbAdmissionDecision.PermissionRequired).token
        assertTrue(gate.completePermission(target, token, true, true, true, true, false))
        assertFalse(gate.completePermission(target, token, true, true, true, true, false))
    }

    @Test fun stalePermissionTokenCannotOpenReplacementAttachment() {
        val gate = UsbAttachmentAttemptGate()
        val oldTarget = attachment(20, "usb/old")
        val oldToken = (gate.request(oldTarget, true, false, false) as UsbAdmissionDecision.PermissionRequired).token
        gate.detach(20)
        val newTarget = attachment(20, "usb/new")
        val newToken = (gate.request(newTarget, true, false, false) as UsbAdmissionDecision.PermissionRequired).token

        assertNotEquals(oldToken, newToken)
        assertFalse(gate.completePermission(oldTarget, oldToken, true, true, true, true, false))
        assertTrue(gate.completePermission(newTarget, newToken, true, true, true, true, false))
    }

    @Test fun replacementWithReusedDeviceIdGetsFreshAdmissionAndRejectsOldResult() {
        val gate = UsbAttachmentAttemptGate()
        val oldTarget = attachment(25, "usb/old")
        val oldToken = (gate.request(oldTarget, true, false, false) as UsbAdmissionDecision.PermissionRequired).token
        val replacement = attachment(25, "usb/replacement")
        val replacementToken = (gate.request(replacement, true, false, false) as UsbAdmissionDecision.PermissionRequired).token

        assertNotEquals(oldToken, replacementToken)
        assertFalse(gate.completePermission(oldTarget, oldToken, true, true, true, true, false))
        assertTrue(gate.completePermission(replacement, replacementToken, true, true, true, true, false))
    }

    @Test fun reconcilingMissingAttachmentInvalidatesPendingToken() {
        val gate = UsbAttachmentAttemptGate()
        val target = attachment(28)
        val oldToken = (gate.request(target, true, false, false) as UsbAdmissionDecision.PermissionRequired).token

        gate.reconcile(emptySet())

        assertFalse(gate.completePermission(target, oldToken, true, true, true, true, false))
        val newToken = (gate.request(target, true, false, false) as UsbAdmissionDecision.PermissionRequired).token
        assertNotEquals(oldToken, newToken)
    }

    @Test fun unrelatedDetachCannotInvalidatePendingAttempt() {
        val gate = UsbAttachmentAttemptGate()
        val target = attachment(20)
        val token = (gate.request(target, true, false, false) as UsbAdmissionDecision.PermissionRequired).token

        gate.detach(21)

        assertTrue(gate.completePermission(target, token, true, true, true, true, false))
        assertEquals(UsbAdmissionDecision.Ignored, gate.request(target, true, false, false))
    }

    private fun attachment(id: Int, name: String = "usb/$id") = UsbAttachmentIdentity(id, name, 0x1234, 0x5678)
}
