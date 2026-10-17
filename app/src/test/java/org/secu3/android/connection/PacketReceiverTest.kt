package org.secu3.android.connection

import org.junit.Assert.*
import org.junit.Test
import org.secu3.android.models.RawPacket
import org.secu3.android.models.packets.input.CheckEngineErrorsPacket
import java.io.EOFException
import java.io.StringReader

class PacketReceiverTest {
    // Firmware Fletcher words: sum1 includes payload length; wire order is sum2, sum1.
    private val valid = intArrayOf(0x40, 'v'.code, 1, 2, 3, 4, 0x22, 0x0E, 0x0D)
    private val escaped = intArrayOf(0x40, 'x'.code, 10, 0x82, 10, 0x83, 10, 0x84, 0x3E, 0x5A, 0x0D)

    private fun collect(vararg chunks: IntArray): List<IntArray> {
        val receiver = PacketReceiver()
        val packets = mutableListOf<IntArray>()
        chunks.forEach { chunk -> chunk.forEach { byte -> receiver.receive(byte)?.let(packets::add) } }
        return packets
    }

    @Test fun everyChunkBoundaryIncludingEscapeAndChecksumBoundariesIsSupported() {
        for (frame in listOf(valid, escaped)) {
            for (split in 0..frame.size) {
                val packets = collect(frame.copyOfRange(0, split), frame.copyOfRange(split, frame.size))
                assertEquals("split=$split", 1, packets.size)
                val expected = if (frame === valid) valid.dropLast(1).toIntArray()
                    else intArrayOf(0x40, 'x'.code, 0x40, 0x0D, 0x0A, 0x3E, 0x5A)
                assertArrayEquals(expected, packets.single())
            }
        }
    }

    @Test fun noiseAndSeveralFramesInOneReadDoNotMergeTheirPayloads() {
        val packets = collect(intArrayOf(1, 2, 3, 13, 10, 0x82) + valid + escaped + valid)
        assertEquals(3, packets.size)
        assertArrayEquals(packets.first(), packets.last())
        val model = RawPacket(packets.first()).parse(null) as CheckEngineErrorsPacket
        assertEquals(0x01020304, model.errors)
    }

    @Test fun everyByteValueSurvivesPayloadAndChecksumEscaping() {
        fun encode(byte: Int) = when (byte) {
            0x40 -> listOf(10, 0x82)
            0x0D -> listOf(10, 0x83)
            0x0A -> listOf(10, 0x84)
            else -> listOf(byte)
        }
        for (byte in 0..255) {
            // Independent one-byte Fletcher formula, including the length update.
            val first = (byte + 1) and 255
            val second = (byte * 2 + 1) and 255
            val wire = intArrayOf(0x40, 'x'.code) +
                (encode(byte) + encode(second) + encode(first)).toIntArray() + intArrayOf(13)
            assertArrayEquals(intArrayOf(0x40, 'x'.code, byte, second, first), collect(wire).single())
        }
    }

    @Test fun allTruncationsResynchronizeAtTheNextLiteralHeader() {
        for (cut in 0 until escaped.size) {
            val packets = collect(escaped.copyOf(cut), valid)
            assertEquals("cut=$cut", 1, packets.size)
            assertArrayEquals(valid.dropLast(1).toIntArray(), packets.single())
        }
    }

    @Test fun badChecksumAndInvalidEscapesAreDiscardedBeforeTheNextValidPacket() {
        val corruptions = listOf(
            valid.copyOf().also { it[6] = it[6] xor 1 },
            valid.copyOf().also { it[7] = it[7] xor 1 },
            intArrayOf(0x40, 'x'.code, 10, 0x99, 0, 0, 13),
            intArrayOf(0x40, 'x'.code, 10, 0x81, 0, 0, 13),
            intArrayOf(0x40, 'x'.code, 0, 0, 10, 13),
            intArrayOf(0x40, 13),
            intArrayOf(0x40, 'x'.code, 13),
        )
        for (corrupt in corruptions) {
            val packets = collect(corrupt, valid)
            assertEquals(1, packets.size)
            assertArrayEquals(valid.dropLast(1).toIntArray(), packets.single())
        }
    }

    @Test fun invalidEscapesCannotBeSilentlyRemovedFromAnOtherwiseValidChecksum() {
        val injected = valid.copyOfRange(0, 2) + intArrayOf(10, 0x99) + valid.copyOfRange(2, valid.size)
        val packets = collect(injected, valid)
        assertEquals(1, packets.size)
    }

    @Test fun maximumDecodedFrameMayUseMoreThan250WireBytes() {
        // 246 payload bytes of 0x40: CRC is 0xB6,0x76; decoded length is 250.
        val wire = intArrayOf(0x40, 'x'.code) +
            (0 until 246).flatMap { listOf(10, 0x82) }.toIntArray() + intArrayOf(0xB6, 0x76, 13)
        assertTrue(wire.size > 250)
        val packets = collect(wire)
        assertEquals(1, packets.size)
        assertEquals(250, packets.single().size)
        assertArrayEquals(IntArray(246) { 0x40 }, packets.single().copyOfRange(2, 248))
    }

    @Test fun oversizedFramesDiscardTheRemainderUntilANewHeader() {
        val oversizedDecoded = intArrayOf(0x40, 'x'.code) + IntArray(247) + intArrayOf(247, 247, 13)
        val oversizedWire = intArrayOf(0x40, 'x'.code) + IntArray(600) + intArrayOf(13)
        for (bad in listOf(oversizedDecoded, oversizedWire)) {
            val packets = collect(bad, valid.copyOfRange(1, valid.size), valid)
            assertEquals(1, packets.size)
            assertArrayEquals(valid.dropLast(1).toIntArray(), packets.single())
        }
    }

    @Test fun everySingleBitCorruptionOfTheGoldenPayloadAndChecksumIsRejected() {
        for (index in 2..7) for (bit in 0..7) {
            val bad = valid.copyOf().also { it[index] = it[index] xor (1 shl bit) }
            assertEquals("index=$index bit=$bit", 1, collect(bad, valid).size)
        }
    }

    @Test fun framesCannotContinueAcrossNewReceiverInstances() {
        val beforeDisconnect = PacketReceiver()
        escaped.take(5).forEach { assertNull(beforeDisconnect.receive(it)) }
        val afterReconnect = PacketReceiver()
        escaped.drop(5).forEach { assertNull(afterReconnect.receive(it)) }
        val packets = valid.map(afterReconnect::receive).filterNotNull()
        assertEquals(1, packets.size)
    }

    @Test(timeout = 1_000) fun eofOnEmptyOrTruncatedInputIsReportedInsteadOfSpinning() {
        for (cut in 0 until escaped.size) {
            val reader = StringReader(escaped.take(cut).map(Int::toChar).joinToString(""))
            assertThrows(EOFException::class.java) { PacketReceiver().readNext(reader) }
        }
    }

    @Test fun readerSkipsCorruptFrameThenReturnsValidFramesAndReportsEof() {
        val bad = valid.copyOf().also { it[6] = it[6] xor 1 }
        val reader = StringReader((bad + valid + valid).map(Int::toChar).joinToString(""))
        val receiver = PacketReceiver()
        repeat(2) { assertArrayEquals(valid.dropLast(1).toIntArray(), receiver.readNext(reader)) }
        assertThrows(EOFException::class.java) { receiver.readNext(reader) }
    }
}
