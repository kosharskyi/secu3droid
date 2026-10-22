package org.secu3.android.connection

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import android.util.Log
import org.secu3.android.models.RawPacket
import org.secu3.android.models.packets.base.OutputPacket
import org.secu3.android.utils.UserPrefs

abstract class Connection<E : Any> (
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

    @Volatile
    var isRunning = false
        protected set

    private val endpointLock = Any()
    private var endpoint: E? = null
    private val outboundWrites by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        SerialWriteQueue(
            scope = scope,
            isCurrentEndpoint = ::isCurrentEndpoint,
            write = ::writeEndpoint,
            afterWrite = { afterEndpointWrite() },
            onFailure = ::onOutboundWriteFailure,
        )
    }

    val isConnected: Boolean
        get() = synchronized(endpointLock) { endpoint?.let(::isEndpointOpen) == true }

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

    final fun sendData(sendPacket: OutputPacket) {
        val target = currentEndpoint() ?: return
        outboundWrites.enqueue(target, encode(sendPacket))
    }

    protected abstract fun disconnect()

    protected abstract fun isEndpointOpen(endpoint: E): Boolean

    protected abstract fun writeEndpoint(endpoint: E, bytes: ByteArray)

    protected abstract fun closeEndpoint(endpoint: E)

    protected open suspend fun afterEndpointWrite() = Unit

    protected open fun onOutboundWriteFailure(failure: Exception) {
        Log.e(javaClass.simpleName, "Error while sending data: ${failure.message}")
    }

    protected fun installEndpoint(endpoint: E): Boolean = synchronized(endpointLock) {
        if (!isRunning || this.endpoint != null) false
        else {
            this.endpoint = endpoint
            true
        }
    }

    protected fun currentEndpoint(): E? = synchronized(endpointLock) {
        endpoint?.takeIf { isRunning && isEndpointOpen(it) }
    }

    protected fun isCurrentEndpoint(endpoint: E): Boolean = synchronized(endpointLock) {
        isRunning && this.endpoint === endpoint && isEndpointOpen(endpoint)
    }

    protected fun detachCurrentEndpoint(): E? = synchronized(endpointLock) {
        endpoint.also { endpoint = null }
    }

    protected fun detachEndpoint(endpoint: E): Boolean = synchronized(endpointLock) {
        if (this.endpoint !== endpoint) false
        else {
            this.endpoint = null
            true
        }
    }

    protected fun invalidateEndpoint(endpoint: E): Boolean {
        if (!detachEndpoint(endpoint)) return false
        closeEndpoint(endpoint)
        return true
    }

    protected fun discardEndpoint(endpoint: E) {
        detachEndpoint(endpoint)
        closeEndpoint(endpoint)
    }

    private fun encode(packet: OutputPacket): ByteArray {
        val frame = intArrayOf(OUTPUT_PACKET_SYMBOL) + packet.pack()
        val checksum = PacketReceiver.calculateChecksum(frame.copyOfRange(2, frame.size))
        val checksummed = frame + checksum[1] + checksum[0]
        return (PacketReceiver.escapeTx(checksummed) + END_PACKET_SYMBOL)
            .map { it.toByte() }
            .toByteArray()
    }

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
