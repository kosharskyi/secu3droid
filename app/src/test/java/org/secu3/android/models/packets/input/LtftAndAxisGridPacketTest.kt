package org.secu3.android.models.packets.input

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test
import org.secu3.android.models.RawPacket
import org.secu3.android.models.packets.out.ChangeModePacket
import org.secu3.android.utils.Task

class LtftAndAxisGridPacketTest {

    @Test
    fun ltftMapParsesSignedValuesSelectorAndHeaderOnlyFragments() {
        val parsed = RawPacket(
            intArrayOf('@'.code, 'K'.code, 0x80, 0xFE, 0x80, 0x7F, 0, 0)
        ).parse(null) as LtftMapChunkPacket

        assertEquals(1, parsed.tableIndex)
        assertEquals(254, parsed.address)
        assertEquals(listOf(-25f, 127f * 100f / 512f), parsed.values)

        val emptyChunk = RawPacket(intArrayOf('@'.code, 'K'.code, 0, 0xFF, 0, 0))
            .parse(null) as LtftMapChunkPacket
        assertEquals(0, emptyChunk.tableIndex)
        assertEquals(255, emptyChunk.address)
        assertEquals(emptyList<Float>(), emptyChunk.values)
    }

    @Test
    fun ltftMapAcceptsMaximumChunkAndRejectsInvalidSizesAndAddressOverrun() {
        val fullChunk = intArrayOf('@'.code, 'K'.code, 1, 0xF0) + IntArray(16) + intArrayOf(0, 0)
        assertEquals(16, (RawPacket(fullChunk).parse(null) as LtftMapChunkPacket).values.size)

        val overlongFragment = intArrayOf('@'.code, 'K'.code, 0, 0) + IntArray(17) + intArrayOf(0, 0)
        assertNull(RawPacket(overlongFragment).parse(null))
        val overrun = intArrayOf('@'.code, 'K'.code, 0, 0xFF, 1, 2, 0, 0)
        assertNull(RawPacket(overrun).parse(null))

        assertThrows(IllegalArgumentException::class.java) {
            LtftMapChunkPacket().parse(intArrayOf('@'.code, 'K'.code, 0, 0) + IntArray(17))
        }
    }

    @Test
    fun rpmGridParsesUnsignedLittleEndianWordsAtZeroAddress() {
        val payload = intArrayOf(0) + IntArray(16 * 2).also {
            it[0] = 0x34
            it[1] = 0x12
            it[2] = 0xFF
            it[3] = 0xFF
        }
        val parsed = RawPacket(intArrayOf('@'.code, '"'.code) + payload + intArrayOf(0, 0))
            .parse(null) as RpmGridPacket

        assertEquals(16, parsed.values.size)
        assertEquals(0x1234, parsed.values[0])
        assertEquals(0xFFFF, parsed.values[1])
        assertThrows(IllegalArgumentException::class.java) {
            RpmGridPacket().parse(intArrayOf('@'.code, '"'.code, 1) + IntArray(32))
        }
        assertNull(RawPacket(intArrayOf('@'.code, '"'.code, 0) + IntArray(31) + intArrayOf(0, 0)).parse(null))
    }

    @Test
    fun loadGridParsesUnsignedLittleEndianWordsAndScalesByPhysicalMagnitude() {
        val payload = intArrayOf(0) + IntArray(16 * 2).also {
            it[0] = 0x00
            it[1] = 0x01
            it[2] = 0xFF
            it[3] = 0xFF
        }
        val parsed = RawPacket(intArrayOf('@'.code, ')'.code) + payload + intArrayOf(0, 0))
            .parse(null) as LoadGridPacket

        assertEquals(16, parsed.values.size)
        assertEquals(4f, parsed.values[0])
        assertEquals(65535f / 64f, parsed.values[1])
        assertThrows(IllegalArgumentException::class.java) {
            LoadGridPacket().parse(intArrayOf('@'.code, ')'.code, 0) + IntArray(31))
        }
    }

    @Test
    fun ltftAndAxisTasksPackTheirWireDescriptors() {
        assertArrayEquals(intArrayOf('h'.code, 'K'.code, 0), ChangeModePacket.getPacket(Task.Secu3ReadLtftMap).pack())
        assertArrayEquals(intArrayOf('h'.code, '"'.code, 0), ChangeModePacket.getPacket(Task.Secu3ReadRpmGrid).pack())
        assertArrayEquals(intArrayOf('h'.code, ')'.code, 0), ChangeModePacket.getPacket(Task.Secu3ReadLoadGrid).pack())
    }
}
