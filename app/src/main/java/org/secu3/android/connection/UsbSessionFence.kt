package org.secu3.android.connection

/** Session identity shared by USB open, packet parsing, timeout, and detach paths. */
internal class UsbSessionFence {
    private var currentGeneration = 0L
    private var activeDeviceId: Int? = null
    private var confirmedGeneration: Long? = null

    @Synchronized fun begin(deviceId: Int): Long {
        currentGeneration += 1
        activeDeviceId = deviceId
        confirmedGeneration = null
        return currentGeneration
    }

    @Synchronized fun advance(): Long {
        currentGeneration += 1
        activeDeviceId = null
        confirmedGeneration = null
        return currentGeneration
    }

    @Synchronized fun isCurrent(deviceId: Int, generation: Long): Boolean =
        activeDeviceId == deviceId && currentGeneration == generation

    @Synchronized fun confirm(deviceId: Int, generation: Long): Boolean {
        if (!isCurrent(deviceId, generation)) return false
        confirmedGeneration = generation
        return true
    }

    @Synchronized fun isConfirmed(deviceId: Int, generation: Long): Boolean =
        isCurrent(deviceId, generation) && confirmedGeneration == generation

    @Synchronized fun invalidateConfirmation(deviceId: Int, generation: Long): Boolean {
        if (!isCurrent(deviceId, generation)) return false
        confirmedGeneration = null
        return true
    }

    @Synchronized fun detach(deviceId: Int): Boolean {
        if (activeDeviceId != deviceId) return false
        currentGeneration += 1
        activeDeviceId = null
        confirmedGeneration = null
        return true
    }

    @Synchronized fun activeDeviceId(): Int? = activeDeviceId
    @Synchronized fun generation(): Long = currentGeneration
}
