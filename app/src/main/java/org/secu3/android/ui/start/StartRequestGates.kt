package org.secu3.android.ui.start

internal class BluetoothBondRequestGate {
    private var pendingAddress: String? = null
    fun begin(address: String) { pendingAddress = address }
    fun matches(address: String): Boolean = pendingAddress == address
    fun complete(address: String): Boolean {
        if (!matches(address)) return false
        pendingAddress = null
        return true
    }
    fun cancel() { pendingAddress = null }
    fun isPending(): Boolean = pendingAddress != null
}

internal fun shouldResumeBluetoothDiscovery(
    pickerVisible: Boolean,
    connecting: Boolean,
    connectionRunning: Boolean,
    connected: Boolean,
    pairing: Boolean,
): Boolean = pickerVisible && !connecting && !connectionRunning && !connected && !pairing
