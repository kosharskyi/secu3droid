package org.secu3.android.connection

/** Keeps USB permission and open attempts unique to one physical attachment. */
internal class UsbAttachmentAttemptGate {
    private val attempted = mutableMapOf<Int, Long>()
    private val opened = mutableSetOf<Long>()
    private var nextToken = 0L

    @Synchronized fun begin(deviceId: Int): Long? {
        if (deviceId in attempted) return null
        val token = ++nextToken
        attempted[deviceId] = token
        return token
    }

    @Synchronized fun accepts(deviceId: Int, token: Long): Boolean = attempted[deviceId] == token

    @Synchronized fun claimOpen(deviceId: Int): Boolean {
        val token = attempted[deviceId] ?: return false
        return opened.add(token)
    }

    @Synchronized fun hasOpened(deviceId: Int): Boolean = attempted[deviceId]?.let { it in opened } == true

    @Synchronized fun detach(deviceId: Int) {
        attempted.remove(deviceId)?.let(opened::remove)
    }

    @Synchronized fun hasAttempted(deviceId: Int): Boolean = deviceId in attempted

    @Synchronized fun reconcile(attachedDeviceIds: Set<Int>) {
        attempted.keys.filter { it !in attachedDeviceIds }.forEach(::detach)
    }
}
