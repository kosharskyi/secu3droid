package org.secu3.android.connection

/** Tracks permission prompts for attached devices without limiting reconnect attempts. */
internal class UsbAttachmentAttemptGate {
    private val promptedAttachments = mutableSetOf<UsbAttachmentIdentity>()
    private var nextToken = 0L
    private var pendingPermission: Pair<UsbAttachmentIdentity, Long>? = null

    @Synchronized fun request(
        attachment: UsbAttachmentIdentity,
        supported: Boolean,
        conflictsWithActiveDevice: Boolean,
        hasPermission: Boolean,
    ): UsbAdmissionDecision {
        if (!supported || conflictsWithActiveDevice) return UsbAdmissionDecision.Ignored
        if (pendingPermission?.first == attachment) return UsbAdmissionDecision.Ignored
        if (pendingPermission != null) return UsbAdmissionDecision.Ignored
        if (hasPermission) return UsbAdmissionDecision.Started
        if (!promptedAttachments.add(attachment)) return UsbAdmissionDecision.Ignored
        val token = ++nextToken
        pendingPermission = attachment to token
        return UsbAdmissionDecision.PermissionRequired(token)
    }

    @Synchronized fun completePermission(
        attachment: UsbAttachmentIdentity,
        token: Long,
        permissionGranted: Boolean,
        hasPermission: Boolean,
        attached: Boolean,
        supported: Boolean,
        conflictsWithActiveDevice: Boolean,
    ): Boolean {
        if (pendingPermission != (attachment to token)) return false
        pendingPermission = null
        return permissionGranted && hasPermission && attached && supported && !conflictsWithActiveDevice
    }

    @Synchronized fun cancelPermission(attachment: UsbAttachmentIdentity, token: Long) {
        if (pendingPermission == (attachment to token)) pendingPermission = null
    }

    @Synchronized fun <T> firstEligibleAttachment(
        candidates: Iterable<T>,
        identity: (T) -> UsbAttachmentIdentity,
        supported: (T) -> Boolean,
        hasPermission: (T) -> Boolean,
    ): T? = candidates.firstOrNull { candidate ->
        val key = identity(candidate)
        supported(candidate) && (hasPermission(candidate) || (key !in promptedAttachments && pendingPermission == null))
    }

    @Synchronized fun detach(deviceId: Int) {
        promptedAttachments.removeAll { it.deviceId == deviceId }
        if (pendingPermission?.first?.deviceId == deviceId) pendingPermission = null
    }

    @Synchronized fun reconcile(attachedDevices: Set<UsbAttachmentIdentity>) {
        promptedAttachments.retainAll(attachedDevices)
        if (pendingPermission?.first !in attachedDevices) pendingPermission = null
    }
}

internal data class UsbAttachmentIdentity(
    val deviceId: Int,
    val deviceName: String,
    val vendorId: Int,
    val productId: Int,
)

internal sealed interface UsbAdmissionDecision {
    data object Ignored : UsbAdmissionDecision
    data class PermissionRequired(val token: Long) : UsbAdmissionDecision
    data object Started : UsbAdmissionDecision
}
