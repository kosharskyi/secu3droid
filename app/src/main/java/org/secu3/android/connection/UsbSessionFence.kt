package org.secu3.android.connection

/** Session identity shared by USB open, packet parsing, timeout, and detach paths. */
internal class UsbSessionFence {
    private var currentGeneration = 0L
    private var activeDeviceId: Int? = null

    @Synchronized fun begin(deviceId: Int): Long {
        currentGeneration += 1
        activeDeviceId = deviceId
        return currentGeneration
    }

    @Synchronized fun advance(): Long {
        currentGeneration += 1
        activeDeviceId = null
        return currentGeneration
    }

    @Synchronized fun isCurrent(deviceId: Int, generation: Long): Boolean =
        activeDeviceId == deviceId && currentGeneration == generation

    @Synchronized fun detach(deviceId: Int): Boolean {
        if (activeDeviceId != deviceId) return false
        currentGeneration += 1
        activeDeviceId = null
        return true
    }

    @Synchronized fun activeDeviceId(): Int? = activeDeviceId
    @Synchronized fun generation(): Long = currentGeneration
}
