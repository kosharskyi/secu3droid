package org.secu3.android.domain.ltft

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class LtftVeImageTest {
    private val codec = LtftVeImage()

    @Test
    fun parseAcceptsOnlyValidatedManagerFlashAndEepromImages() {
        val flash = managerFlash()
        val flashImage = codec.parse(flash)
        assertNotNull(flashImage)
        assertTrue(flashImage!!.isFlash)
        assertEquals(listOf("Set 1", "Set 2", "Set 3", "Set 4"), flashImage.setNames)

        val eeprom = managerEeprom()
        val eepromImage = codec.parse(eeprom)
        assertNotNull(eepromImage)
        assertFalse(eepromImage!!.isFlash)
        assertEquals(listOf("EE set"), eepromImage.setNames)

        val legacyFlash = flash.copyOf()
        "128 ".toByteArray(Charsets.US_ASCII).copyInto(legacyFlash, legacyFlash.size - 4)
        assertNull(codec.parse(legacyFlash))
        val flashWithoutInjection = flash.copyOf()
        flashWithoutInjection[112515] = 0
        placeCrc(flashWithoutInjection, 129022, 0, 129022)
        assertNull(codec.parse(flashWithoutInjection))

        flash[0] = 1
        assertNull(codec.parse(flash))
        eeprom[2000] = 1
        assertNull(codec.parse(eeprom))
    }

    @Test
    fun crcMatchesModbusCrc16GoldenVector() {
        assertEquals(0x4b37, crc16("123456789".toByteArray(Charsets.US_ASCII), 0, 9))
    }

    @Test
    fun parseLoadsSparseHexUsingSegmentAddressesAndFillsErasedFlash() {
        val flash = managerFlash(fill = 0xff.toByte()).apply {
            this[0] = ':'.code.toByte()
            placeCrc(this, 129022, 0, 129022)
        }
        val records = hexRecords(flash, sparse = true).toMutableList().apply {
            add(lastIndex, ":0400000300000000F9") // Start segment address, ignored by Manager.
        }
        val input = records.joinToString("\n").lowercase().toByteArray(Charsets.US_ASCII)

        val image = codec.parse(input)!!
        assertTrue(image.isFlash)
        assertEquals(listOf("Set 1", "Set 2", "Set 3", "Set 4"), image.setNames)
        assertArrayEquals(flash, image.bytes)
        assertArrayEquals(flash, codec.parse(flash)!!.bytes) // ':' is also a valid first BIN byte.
    }

    @Test
    fun parseRejectsInvalidHexRecordsAndImages() {
        val flash = managerFlash(fill = 0xff.toByte())
        val records = hexRecords(flash, sparse = true)
        fun assertRejected(lines: List<String>) {
            assertNull(codec.parse(lines.joinToString("\r\n", postfix = "\r\n").toByteArray(Charsets.US_ASCII)))
        }

        assertRejected(records.toMutableList().apply {
            this[0] = first().dropLast(1) + if (first().last() == '0') "1" else "0"
        })
        assertRejected(records.toMutableList().apply { this[0] = first().replaceRange(1, 3, "11") })
        assertRejected(records.dropLast(1))
        assertRejected(records + records.first())
        assertRejected(records.toMutableList().apply { add(lastIndex, ":01000001FFFF") }) // EOF with a payload.
        assertRejected(records.toMutableList().apply { add(lastIndex, ":020000021001EB") }) // Unaligned segment.
        assertRejected(records.toMutableList().apply { add(lastIndex, ":020000041000EA") }) // Unsupported type 04.
        assertRejected(records.toMutableList().apply {
            add(lastIndex, ":020000022000DC") // Address base 0x20000.
            add(lastIndex, hexRecord(0, byteArrayOf(1)))
        })
        assertRejected(records.toMutableList().apply { removeAt(lastIndex - 1) }) // Truncated address range.
        assertRejected(hexRecords(flash.copyOf().apply { this[0] = 1 }, sparse = true)) // Valid HEX, wrong firmware CRC.
        assertRejected(hexRecords(managerEeprom(), sparse = false)) // EEPROM stays BIN.
        assertNull(codec.parse(ByteArray(524289).apply { this[0] = ':'.code.toByte() }))
    }

    @Test
    fun flashApplyWritesManagerHexWithTheCorrectedFirmwareCrc() {
        val image = codec.parse(managerFlash())!!
        val tables = listOf(List(256) { 10.0f }, List(256) { -20.0f })
        val binary = codec.apply(image, 2, LtftVeImage.Source.Average, tables)
        val expected = hexRecords(binary, sparse = false).joinToString("\r\n", postfix = "\r\n")

        val hex = codec.apply(image, 2, LtftVeImage.Source.Average, tables, LtftVeImage.Format.Hex)

        assertEquals(expected, String(hex, Charsets.US_ASCII))
        assertArrayEquals(binary, codec.parse(hex)!!.bytes)
        assertEquals(973, packed12(binary, 123621, 255))
        assertTrue(runCatching {
            codec.apply(codec.parse(managerEeprom())!!, 0, LtftVeImage.Source.Average, tables, LtftVeImage.Format.Hex)
        }.isFailure)
    }

    @Test
    fun flashApplyUpdatesOnlySelectedVeAndFirmwareCrc() {
        val original = managerFlash()
        val image = codec.parse(original)!!
        val tables = listOf(List(256) { 10.0f }, List(256) { -20.0f })

        val changed = codec.apply(image, 1, LtftVeImage.Source.Ltft1, tables)

        assertEquals(1126, packed12(changed, 120788, 0))
        assertEquals(1126, packed12(changed, 120788, 1))
        assertEquals(1024, packed12(changed, 117955, 0))
        assertEquals(1024, packed12(changed, 123621, 255))
        assertOnlyRangesChanged(original, changed, listOf(120788 until 121172, 129022 until 129024))
        assertEquals(crc16(changed, 0, 129022), u16(changed, 129022))
        assertEquals(131072, changed.size)
        val secondBank = codec.apply(image, 1, LtftVeImage.Source.Ltft2, tables)
        assertEquals(819, packed12(secondBank, 120788, 255))
    }

    @Test
    fun eepromApplyUsesAverageAndUpdatesOnlyVeAndTableCrc() {
        val original = managerEeprom()
        val image = codec.parse(original)!!
        val tables = listOf(List(256) { 10.0f }, List(256) { -20.0f })

        val changed = codec.apply(image, 0, LtftVeImage.Source.Average, tables)

        assertEquals(973, packed12(changed, 907, 0))
        assertEquals(973, packed12(changed, 907, 255))
        assertOnlyRangesChanged(original, changed, listOf(907 until 1291, 3418 until 3420))
        assertEquals(crc16(changed, 587, 2831), u16(changed, 3418))
    }

    @Test
    fun applyClampsAtManagerBoundsAndRejectsBadSelectionOrTables() {
        val source = managerFlash()
        setPacked12(source, 117955, 0, 1)
        setPacked12(source, 117955, 1, 4095)
        placeCrc(source, 129022, 0, 129022)
        val image = codec.parse(source)!!
        val firstTable = MutableList(256) { 0.0f }.apply {
            this[0] = -1000.0f
            this[1] = 1000.0f
        }
        val tables = listOf(firstTable, List(256) { 0.0f })

        val changed = codec.apply(image, 0, LtftVeImage.Source.Ltft1, tables)
        assertEquals(20, packed12(changed, 117955, 0))
        assertEquals(4076, packed12(changed, 117955, 1))
        assertTrue(runCatching { codec.apply(image, 4, LtftVeImage.Source.Ltft1, tables) }.isFailure)
        assertTrue(runCatching { codec.apply(image, 0, LtftVeImage.Source.Ltft1, listOf(emptyList(), emptyList())) }.isFailure)
    }

    private fun managerFlash(fill: Byte = 0) = ByteArray(131072) { fill }.apply {
        putU16(this, 129020, 16502)
        putU16(this, 112520, 890)
        putU16(this, 112510, 880)
        this[112509] = 0x40
        putU32(this, 112512, (1L shl 28) or (1L shl 1))
        putName(this, 117635, "Set 1")
        putName(this, 120468, "Set 2")
        putName(this, 123301, "Set 3")
        putName(this, 126134, "Set 4")
        listOf(117955, 120788, 123621, 126454).forEach { base ->
            repeat(256) { setPacked12(this, base, it, 1024) }
        }
        putMagic(this)
        placeCrc(this, 129022, 0, 129022)
    }

    private fun managerEeprom() = ByteArray(4096).apply {
        putName(this, 587, "EE set")
        repeat(256) { setPacked12(this, 907, it, 1024) }
        putMagic(this)
        placeCrc(this, 567, 1, 566)
        placeCrc(this, 3418, 587, 2831)
    }

    private fun assertOnlyRangesChanged(before: ByteArray, after: ByteArray, allowed: List<IntRange>) {
        before.indices.filter { i -> allowed.none { i in it } }.forEach { i -> assertEquals("byte $i", before[i], after[i]) }
    }

    private fun hexRecords(bytes: ByteArray, sparse: Boolean): List<String> = buildList {
        for (offset in bytes.indices step 16) {
            if (offset == 0x10000) add(":020000021000EC")
            val data = bytes.copyOfRange(offset, offset + 16)
            if (!sparse || data.any { it != 0xff.toByte() }) add(hexRecord(offset and 0xffff, data))
        }
        add(":00000001FF")
    }

    private fun hexRecord(address: Int, data: ByteArray): String {
        val checksum = -(data.size + (address shr 8) + (address and 0xff) + data.sumOf { it.toInt() and 0xff }) and 0xff
        return String.format(Locale.ROOT, ":%02X%04X00", data.size, address) +
            data.joinToString("") { String.format(Locale.ROOT, "%02X", it.toInt() and 0xff) } +
            String.format(Locale.ROOT, "%02X", checksum)
    }

    private fun putName(bytes: ByteArray, offset: Int, name: String) {
        bytes.fill(0, offset, offset + 16)
        name.toByteArray(Charsets.US_ASCII).copyInto(bytes, offset)
    }

    private fun putMagic(bytes: ByteArray) {
        "1284".toByteArray(Charsets.US_ASCII).copyInto(bytes, bytes.size - 4)
    }

    private fun packed12(bytes: ByteArray, offset: Int, index: Int): Int {
        val at = offset + index + (index shr 1)
        val first = bytes[at].toInt() and 0xff
        val second = bytes[at + 1].toInt() and 0xff
        return if (index and 1 == 0) first or ((second and 0x0f) shl 8) else (first ushr 4) or (second shl 4)
    }

    private fun setPacked12(bytes: ByteArray, offset: Int, index: Int, value: Int) {
        val at = offset + index + (index shr 1)
        if (index and 1 == 0) {
            bytes[at] = value.toByte()
            bytes[at + 1] = ((bytes[at + 1].toInt() and 0xf0) or (value ushr 8)).toByte()
        } else {
            bytes[at] = ((bytes[at].toInt() and 0x0f) or ((value and 0x0f) shl 4)).toByte()
            bytes[at + 1] = (value ushr 4).toByte()
        }
    }

    private fun placeCrc(bytes: ByteArray, target: Int, start: Int, length: Int) = putU16(bytes, target, crc16(bytes, start, length))

    private fun crc16(bytes: ByteArray, start: Int, length: Int): Int {
        var crc = 0xffff
        for (i in start until start + length) {
            crc = crc xor (bytes[i].toInt() and 0xff)
            repeat(8) { crc = if (crc and 1 != 0) (crc ushr 1) xor 0xa001 else crc ushr 1 }
        }
        return crc and 0xffff
    }

    private fun putU16(bytes: ByteArray, offset: Int, value: Int) {
        bytes[offset] = value.toByte()
        bytes[offset + 1] = (value ushr 8).toByte()
    }

    private fun putU32(bytes: ByteArray, offset: Int, value: Long) {
        putU16(bytes, offset, value.toInt() and 0xffff)
        putU16(bytes, offset + 2, (value ushr 16).toInt() and 0xffff)
    }

    private fun u16(bytes: ByteArray, offset: Int) = (bytes[offset].toInt() and 0xff) or ((bytes[offset + 1].toInt() and 0xff) shl 8)
}
