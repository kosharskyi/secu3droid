package org.secu3.android.connection

/** Keeps USB permission and open attempts unique to one physical attachment. */
internal class UsbAttachmentAttemptGate {
    private val attempted = mutableMapOf<UsbAttachmentIdentity, Long>()
    private val opened = mutableSetOf<Long>()
    private var nextToken = 0L
    private var pendingPermission: Pair<UsbAttachmentIdentity, Long>? = null

    @Synchronized fun request(
        attachment: UsbAttachmentIdentity,
        supported: Boolean,
        conflictsWithActiveDevice: Boolean,
        hasPermission: Boolean,
    ): UsbAdmissionDecision {
        if (!supported || conflictsWithActiveDevice) return UsbAdmissionDecision.Ignored
        attempted.keys.filter { it.deviceId == attachment.deviceId && it != attachment }
            .forEach { detach(it.deviceId) }
        val previous = attempted[attachment]
        if (previous == null && pendingPermission?.first?.let { it != attachment } == true) return UsbAdmissionDecision.Ignored
        val token = previous ?: (++nextToken).also { attempted[attachment] = it }
        if (token in opened) return UsbAdmissionDecision.Ignored
        if (hasPermission) {
            if (!opened.add(token)) return UsbAdmissionDecision.Ignored
            if (pendingPermission == (attachment to token)) pendingPermission = null
            return UsbAdmissionDecision.Started
        }
        if (previous != null) return UsbAdmissionDecision.Ignored
        pendingPermission = attachment to token
        return UsbAdmissionDecision.PermissionRequired(token)
    }

    @Synchronized fun accepts(attachment: UsbAttachmentIdentity, token: Long): Boolean = attempted[attachment] == token

    @Synchronized fun completePermission(
        attachment: UsbAttachmentIdentity,
        token: Long,
        permissionGranted: Boolean,
        hasPermission: Boolean,
        attached: Boolean,
        supported: Boolean,
        conflictsWithActiveDevice: Boolean,
    ): Boolean {
        if (pendingPermission != (attachment to token) || !accepts(attachment, token)) return false
        pendingPermission = null
        if (!permissionGranted || !hasPermission || !attached || !supported || conflictsWithActiveDevice) return false
        return opened.add(token)
    }

    @Synchronized fun hasAttempted(attachment: UsbAttachmentIdentity): Boolean = attachment in attempted

    @Synchronized fun hasOpened(attachment: UsbAttachmentIdentity): Boolean = attempted[attachment]?.let { it in opened } == true

    @Synchronized fun detach(deviceId: Int) {
        attempted.keys.filter { it.deviceId == deviceId }.toList().forEach { key -> attempted.remove(key)?.let(opened::remove) }
        if (pendingPermission?.first?.deviceId == deviceId) pendingPermission = null
    }

    @Synchronized fun reconcile(attachedDevices: Set<UsbAttachmentIdentity>) {
        attempted.keys.filter { it !in attachedDevices }.forEach { attachment ->
            attempted.remove(attachment)?.let(opened::remove)
            if (pendingPermission?.first == attachment) pendingPermission = null
        }
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
