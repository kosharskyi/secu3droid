package org.secu3.android.connection

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import org.secu3.android.models.RawPacket
import org.secu3.android.models.packets.base.OutputPacket
import org.secu3.android.utils.UserPrefs

abstract class Connection (
    private val prefs: UserPrefs
) {

    protected val INPUT_PACKET_SYMBOL = 0x40    // '@'
    protected val OUTPUT_PACKET_SYMBOL = 0x21   // '!'
    protected val END_PACKET_SYMBOL = 0x0D      // '\r'


    protected val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    protected val maxConnectionAttempts: Int
        get() = prefs.connectionRetries

    var connectionAttempts = 0
        protected set

    var isRunning = false
        protected set

    abstract val isConnected: Boolean

    protected val mReceivedPacketFlow = MutableSharedFlow<RawPacket>()
    val receivedPacketFlow: SharedFlow<RawPacket>
        get() = mReceivedPacketFlow

    protected val mConnectionStateFlow = MutableSharedFlow<ConnectionState>()
    val connectionStateFlow: Flow<ConnectionState>
        get() = mConnectionStateFlow


    fun stopConnection() {
        if (isRunning.not()) {
            return
        }

        isRunning = false
        disconnect()
    }

    abstract fun sendData(sendPacket: OutputPacket)

    protected abstract fun disconnect()


    protected fun escRxPacket(packetBuffer: IntArray): IntArray? = PacketReceiver.unescapeRx(packetBuffer)

    protected fun escTxPacket(packetBuffer: IntArray): IntArray = PacketReceiver.escapeTx(packetBuffer)

    protected fun isChecksumValid(data: IntArray): Boolean = PacketReceiver.isChecksumValid(data)

    protected fun calculateChecksum(packet: IntArray): UByteArray {
        val checksum = PacketReceiver.calculateChecksum(packet)
        return UByteArray(2) { checksum[it].toUByte() }
    }

    companion object {
        internal const val MAX_PACKET_SIZE = PacketReceiver.MAX_PACKET_SIZE
    }

}
