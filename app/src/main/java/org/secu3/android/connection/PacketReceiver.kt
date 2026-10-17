package org.secu3.android.connection

import java.io.EOFException
import java.io.Reader

/** One instance belongs to one transport read loop; receive boundaries need not match packets. */
internal class PacketReceiver {
    private val buffer = IntArray(2 + (MAX_PACKET_SIZE - 2) * 2)
    private var size = -1

    fun readNext(reader: Reader): IntArray {
        while (true) {
            val byte = reader.read()
            if (byte == -1) throw EOFException("ECU input stream closed")
            receive(byte)?.let { return it }
        }
    }

    fun receive(byte: Int): IntArray? {
        if (byte !in 0..255) {
            size = -1
            return null
        }
        if (byte == 0x40) {
            buffer[0] = byte
            size = 1
            return null
        }
        if (size < 0) return null
        if (byte == 0x0D) {
            val encoded = buffer.copyOf(size)
            size = -1
            val decoded = unescapeRx(encoded) ?: return null
            return decoded.takeIf { it.size <= MAX_PACKET_SIZE && isChecksumValid(it) }
        }
        if (size == buffer.size) {
            size = -1
            return null
        }
        buffer[size++] = byte
        return null
    }

    companion object {
        const val MAX_PACKET_SIZE = 250

        /** Header/descriptor are literal; all following bytes include the checksum, but no CR. */
        fun escapeTx(packet: IntArray): IntArray {
            val escaped = ArrayList<Int>(packet.size)
            packet.forEachIndexed { index, byte ->
                val replacement = if (index >= 2) when (byte) {
                    0x21 -> 0x81
                    0x0D -> 0x83
                    0x0A -> 0x84
                    else -> null
                } else null
                if (replacement != null) {
                    escaped += 0x0A
                    escaped += replacement
                } else escaped += byte
            }
            return escaped.toIntArray()
        }

        fun unescapeRx(packet: IntArray): IntArray? {
            if (packet.size < 2 || packet[0] != 0x40 || packet.any { it !in 0..255 }) return null
            val decoded = IntArray(packet.size)
            decoded[0] = packet[0]
            decoded[1] = packet[1]
            var count = 2
            var index = 2
            while (index < packet.size) {
                val byte = packet[index++]
                decoded[count++] = when (byte) {
                    0x0A -> {
                        if (index == packet.size) return null
                        when (packet[index++]) {
                            0x82 -> 0x40
                            0x83 -> 0x0D
                            0x84 -> 0x0A
                            else -> return null
                        }
                    }
                    0x40, 0x0D -> return null
                    else -> byte
                }
            }
            return decoded.copyOf(count)
        }

        fun isChecksumValid(packet: IntArray): Boolean {
            if (packet.size < 4 || packet[0] != 0x40 || packet.any { it !in 0..255 }) return false
            val crc = calculateChecksum(packet.copyOfRange(2, packet.size - 2))
            return packet.last() == crc[0] && packet[packet.lastIndex - 1] == crc[1]
        }

        fun calculateChecksum(payload: IntArray): IntArray {
            var first = 0
            var second = 0
            for (byte in payload) {
                first = (first + byte) and 255
                second = (second + first) and 255
            }
            first = (first + payload.size) and 255
            second = (second + first) and 255
            return intArrayOf(first, second)
        }
    }
}
