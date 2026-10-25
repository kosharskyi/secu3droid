package org.secu3.android.connection

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UsbAttachmentAttemptGateTest {
    @Test fun unsupportedAndConflictingDevicesDoNotPromptOrStart() {
        val gate = UsbAttachmentAttemptGate()
        assertEquals(UsbAdmissionDecision.Ignored, gate.request(attachment(1), false, false, false))
        assertEquals(UsbAdmissionDecision.Ignored, gate.request(attachment(2), true, true, false))
        assertEquals(UsbAdmissionDecision.PermissionRequired(1), gate.request(attachment(1), true, false, false))
    }

    @Test fun permissionPromptIsDeduplicatedButGrantFromAttachChooserStartsAutomatically() {
        val gate = UsbAttachmentAttemptGate()
        val device = attachment(32)
        val first = gate.request(device, supported = true, conflictsWithActiveDevice = false, hasPermission = false)
        assertTrue(first is UsbAdmissionDecision.PermissionRequired)
        val token = (first as UsbAdmissionDecision.PermissionRequired).token

        assertEquals(UsbAdmissionDecision.Ignored, gate.request(device, true, false, false))
        assertFalse(gate.completePermission(device, token, false, false, true, true, false))
        assertEquals(UsbAdmissionDecision.Ignored, gate.request(device, true, false, false))

        assertEquals(UsbAdmissionDecision.Started, gate.request(device, true, false, true))
    }

    @Test fun permissionCompletionRequiresFreshPermissionAttachmentSupportAndNoConflict() {
        fun rejected(attached: Boolean, supported: Boolean, hasPermission: Boolean, conflicts: Boolean): Boolean {
            val gate = UsbAttachmentAttemptGate()
            val target = attachment(12)
            val token = (gate.request(target, true, false, false) as UsbAdmissionDecision.PermissionRequired).token
            return gate.completePermission(target, token, true, hasPermission, attached, supported, conflicts)
        }

        assertFalse(rejected(true, true, false, false))
        assertFalse(rejected(false, true, true, false))
        assertFalse(rejected(true, false, true, false))
        assertFalse(rejected(true, true, true, true))
        val gate = UsbAttachmentAttemptGate()
        val target = attachment(12)
        val token = (gate.request(target, true, false, false) as UsbAdmissionDecision.PermissionRequired).token
        assertTrue(gate.completePermission(target, token, true, true, true, true, false))
        assertFalse(gate.completePermission(target, token, true, true, true, true, false))
    }

    @Test fun stalePermissionTokenCannotOpenReplacementThatReusesDeviceId() {
        val gate = UsbAttachmentAttemptGate()
        val old = attachment(20, "usb/old")
        val oldToken = (gate.request(old, true, false, false) as UsbAdmissionDecision.PermissionRequired).token
        gate.detach(old.deviceId)
        val replacement = attachment(20, "usb/replacement")
        val newToken = (gate.request(replacement, true, false, false) as UsbAdmissionDecision.PermissionRequired).token

        assertNotEquals(oldToken, newToken)
        assertFalse(gate.completePermission(old, oldToken, true, true, true, true, false))
        assertTrue(gate.completePermission(replacement, newToken, true, true, true, true, false))
    }

    @Test fun cancelledBackgroundPermissionCannotBlockAutomaticAdmissionAfterReturning() {
        val gate = UsbAttachmentAttemptGate()
        val device = attachment(22)
        val token = (gate.request(device, true, false, false) as UsbAdmissionDecision.PermissionRequired).token

        gate.cancelPermission(device, token)

        assertFalse(gate.completePermission(device, token, true, true, true, true, false))
        assertEquals(UsbAdmissionDecision.Started, gate.request(device, true, false, true))
    }

    @Test fun detachClearsDeniedPromptHistoryAndSnapshotReconciliationInvalidatesPendingPermission() {
        val gate = UsbAttachmentAttemptGate()
        val device = attachment(28)
        val token = (gate.request(device, true, false, false) as UsbAdmissionDecision.PermissionRequired).token
        gate.completePermission(device, token, false, false, true, true, false)
        assertEquals(UsbAdmissionDecision.Ignored, gate.request(device, true, false, false))

        gate.reconcile(emptySet())
        val freshToken = (gate.request(device, true, false, false) as UsbAdmissionDecision.PermissionRequired).token
        assertNotEquals(token, freshToken)
        gate.detach(device.deviceId)
        assertFalse(gate.completePermission(device, freshToken, true, true, true, true, false))
    }

    @Test fun unlimitedRetriesAreNotGatedByPhysicalAttachmentHistory() {
        val gate = UsbAttachmentAttemptGate()
        val device = attachment(41)

        repeat(100) {
            assertEquals(UsbAdmissionDecision.Started, gate.request(device, true, false, true))
        }
    }

    @Test fun firstEligibleDeviceKeepsSnapshotOrderAndSkipsPreviouslyDeniedPermissionPrompt() {
        val gate = UsbAttachmentAttemptGate()
        val denied = attachment(51)
        val unsupported = attachment(52)
        val next = attachment(53)
        val token = (gate.request(denied, true, false, false) as UsbAdmissionDecision.PermissionRequired).token
        gate.completePermission(denied, token, false, false, true, true, false)

        fun first(hasPermission: Set<UsbAttachmentIdentity>) = gate.firstEligibleAttachment(
            candidates = listOf(denied, unsupported, next),
            identity = { it },
            supported = { it != unsupported },
            hasPermission = { it in hasPermission },
        )

        assertEquals(next, first(emptySet()))
        assertEquals(denied, first(setOf(denied)))
    }

    private fun attachment(id: Int, name: String = "usb/$id") = UsbAttachmentIdentity(id, name, 0x1234, 0x5678)
}
