package org.secu3.android.connection

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import org.secu3.android.models.RawPacket
import org.secu3.android.models.packets.base.BaseOutputPacket
import org.secu3.android.utils.UserPrefs
import java.util.ArrayList

abstract class Connection (
    private val prefs: UserPrefs
) {

    protected val INPUT_PACKET_SYMBOL = 0x40    // '@'
    protected val OUTPUT_PACKET_SYMBOL = 0x21   // '!'
    protected val END_PACKET_SYMBOL = 0x0D      // '\r'

    // There are several special reserved symbols in binary mode: 0x21, 0x40,
    // 0x0D, 0x0A
    private val FOBEGIN = 0x21 // '!' indicates beginning of the outgoing packet

    private val FIBEGIN = 0x40 // '@' indicates beginning of the ingoing packet

    private val FIOEND = 0x0D // '\r' indicates ending of the ingoing/outgoing packet

    private val FESC = 0x0A // '\n' Packet escape (FESC)

    // Following bytes are used only in escape sequeces and may appear in the
    // data without any problems
    private val TFOBEGIN = 0x81 // Transposed FOBEGIN

    private val TFIBEGIN = 0x82 // Transposed FIBEGIN

    private val TFIOEND = 0x83 // Transposed FIOEND

    private val TFESC = 0x84 // Transposed FESC


    protected val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    protected val maxConnectionAttempts: Int
        get() = prefs.connectionRetries

    var connectionAttempts = 0
        protected set

    var isRunning = false
        protected set

    abstract val isConnected: Boolean

    protected val mReceivedPacketFlow = MutableSharedFlow<RawPacket>()
    val receivedPacketFlow: Flow<RawPacket>
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

    abstract fun sendData(sendPacket: BaseOutputPacket)

    protected abstract fun disconnect()


    protected fun escRxPacket(packetBuffer: IntArray): IntArray {
        val buf = IntArray(packetBuffer.size)
        var esc = false
        var idx = 0
        for (i in packetBuffer.indices) {
            if (packetBuffer[i] == FESC && i >= 2) {
                esc = true
                continue
            }
            if (esc) {
                esc = false
                when(packetBuffer[i]) {
                    TFIBEGIN -> buf[idx++] = FIBEGIN
                    TFIOEND -> buf[idx++] = FIOEND
                    TFESC -> buf[idx++] = FESC
                }
            } else buf[idx++] = packetBuffer[i]
        }
        return buf.sliceArray(IntRange(0, idx - 1))
    }

    protected fun escTxPacket(packetBuffer: IntArray): IntArray {
        val buf = ArrayList<Int>(packetBuffer.size - 3)
        for (i in packetBuffer.indices) {
            if (i >= 2 && i < packetBuffer.size - 1) {
                if (packetBuffer[i] == FOBEGIN) {
                    buf.add(FESC)
                    buf.add(TFOBEGIN)
                    continue
                } else if (packetBuffer[i] == FIOEND) {
                    buf.add(FESC)
                    buf.add(TFIOEND)
                    continue
                } else if (packetBuffer[i] == FESC) {
                    buf.add(FESC)
                    buf.add(TFESC)
                    continue
                }
            }
            buf.add(packetBuffer[i])
        }
        val outBuf = IntArray(buf.size)
        for (i in buf.indices) {
            outBuf[i] = buf[i]
        }
        return outBuf
    }

    protected fun isChecksumValid(data: IntArray): Boolean {
        val packetCrc = ubyteArrayOf(
            data[data.lastIndex].toUByte(),
            data[data.lastIndex - 1].toUByte()
        )

        val checksum = calculateChecksum(data.sliceArray(2 until data.size - 2))

        if (packetCrc[0] != checksum[0] && packetCrc[1] != checksum[1]) {
            Log.e("RawPacket", "checksum is not valid")
            return false
        }

        return true
    }

    protected fun calculateChecksum(packet: IntArray): UByteArray {
        val crc22 = UByteArray(2) { 0u }

        for (i in packet) {
            crc22[0] = (crc22[0] + i.toUByte()).toUByte()
            crc22[1] = (crc22[1] + crc22[0]).toUByte()
        }

        crc22[0] = (crc22[0] + packet.size.toUByte()).toUByte()
        crc22[1] = (crc22[1] + crc22[0]).toUByte()

        return crc22
    }

    companion object {
        internal const val MAX_PACKET_SIZE = 250
    }

}