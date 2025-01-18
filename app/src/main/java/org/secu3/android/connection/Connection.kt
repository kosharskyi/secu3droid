package org.secu3.android.connection

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import org.secu3.android.models.RawPacket
import org.secu3.android.utils.UserPrefs

abstract class Connection (
    private val prefs: UserPrefs
) {

    internal val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    internal val maxConnectionAttempts: Int
        get() = prefs.connectionRetries

    var connectionAttempts = 0
        internal set

    var isRunning = false
        internal set

    abstract val isConnected: Boolean

    internal val mReceivedPacketFlow = MutableSharedFlow<RawPacket>()
    val receivedPacketFlow: Flow<RawPacket>
        get() = mReceivedPacketFlow

    internal val mConnectionStateFlow = MutableSharedFlow<ConnectionState>()
    val connectionStateFlow: Flow<ConnectionState>
        get() = mConnectionStateFlow


    fun stopConnection() {
        isRunning = false
        disconnect()
    }

    abstract internal fun disconnect()

}