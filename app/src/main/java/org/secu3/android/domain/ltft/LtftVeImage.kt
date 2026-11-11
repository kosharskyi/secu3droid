package org.secu3.android.domain.ltft

import android.content.ContentResolver
import android.net.Uri
import android.provider.DocumentsContract
import java.nio.charset.Charset
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class LtftVeImage @Inject constructor() {
    class Image internal constructor(
        val isFlash: Boolean,
        val setNames: List<String>,
        internal val bytes: ByteArray,
    )

    enum class Source { Ltft1, Ltft2, Average }

    sealed interface ReadResult {
        data class Opened(val image: Image) : ReadResult
        data object Invalid : ReadResult
        data object Failed : ReadResult
    }

    fun parse(bytes: ByteArray): Image? {
        val flash = bytes.size == FLASH_SIZE
        if (!flash && bytes.size != EEPROM_SIZE) return null
        if (!hasMagic(bytes)) return null

        val names: List<String>
        if (flash) {
            if (readU16(bytes, FLASH_FW_DATA_SIZE_OFFSET) != FW_DATA_SIZE ||
                readU16(bytes, FLASH_CODE_DATA_SIZE_OFFSET) != CODE_DATA_SIZE ||
                readU16(bytes, FLASH_IOREM_SIZE_OFFSET) != IOREM_SIZE ||
                (bytes[FLASH_IOREM_VERSION_OFFSET].toInt() ushr 4 and 0x0f) != IOREM_MAJOR_VERSION ||
                (readU32(bytes, FLASH_CONFIG_OFFSET) and (1L shl FUEL_INJECTION_BIT)) == 0L ||
                (readU32(bytes, FLASH_CONFIG_OFFSET) and (1L shl ATMEGA1284_BIT)) == 0L ||
                readU16(bytes, FLASH_CRC_OFFSET) != crc16(bytes, 0, FLASH_CRC_OFFSET)
            ) return null
            names = (0 until SET_COUNT).map { decodeName(bytes, FLASH_SET_NAME_BASES[it]) }
        } else {
            if (readU16(bytes, EEPROM_PARAMS_CRC_OFFSET) != crc16(bytes, EEPROM_PARAMS_START, EEPROM_PARAMS_CRC_OFFSET - EEPROM_PARAMS_START) ||
                readU16(bytes, EEPROM_TABLES_CRC_OFFSET) != crc16(bytes, EEPROM_TABLES_START, EEPROM_TABLES_CRC_OFFSET - EEPROM_TABLES_START)
            ) return null
            names = listOf(decodeName(bytes, EEPROM_TABLES_START))
        }
        return Image(flash, names, bytes.copyOf())
    }

    fun apply(image: Image, setIndex: Int, source: Source, tables: List<List<Float>>): ByteArray {
        require(setIndex in image.setNames.indices) { "Set index is out of range" }
        require(tables.size == 2 && tables.all { it.size == VE_CELL_COUNT }) { "Expected two LTFT tables with 256 cells each" }
        require(tables.all { row -> row.all { it.isFinite() } }) { "LTFT values must be finite" }

        val bytes = image.bytes.copyOf()
        val veStart = if (image.isFlash) FLASH_VE_BASES[setIndex] else EEPROM_VE_START
        for (i in 0 until VE_CELL_COUNT) {
            val trim = when (source) {
                Source.Ltft1 -> tables[0][i]
                Source.Ltft2 -> tables[1][i]
                Source.Average -> (tables[0][i] + tables[1][i]) / 2.0f
            }
            val current = readPacked12(bytes, veStart, i) / VE_SCALE
            val adjusted = (current + current * trim / 100.0f).coerceIn(VE_MIN, VE_MAX)
            writePacked12(bytes, veStart, i, (adjusted * VE_SCALE + 0.5f).toInt())
        }
        if (image.isFlash) putU16(bytes, FLASH_CRC_OFFSET, crc16(bytes, 0, FLASH_CRC_OFFSET))
        else putU16(bytes, EEPROM_TABLES_CRC_OFFSET, crc16(bytes, EEPROM_TABLES_START, EEPROM_TABLES_CRC_OFFSET - EEPROM_TABLES_START))
        return bytes
    }

    suspend fun read(resolver: ContentResolver, uri: Uri): ReadResult {
        return withContext(Dispatchers.IO) {
            try {
                val input = resolver.openInputStream(uri) ?: return@withContext ReadResult.Failed
                val bytes = input.use { stream ->
                    val output = java.io.ByteArrayOutputStream()
                    val buffer = ByteArray(8192)
                    var total = 0
                    while (true) {
                        val count = stream.read(buffer)
                        if (count < 0) break
                        if (count == 0) {
                            val next = stream.read()
                            if (next < 0) break
                            if (++total > FLASH_SIZE) return@withContext ReadResult.Invalid
                            output.write(next)
                        } else {
                            total += count
                            if (total > FLASH_SIZE) return@withContext ReadResult.Invalid
                            output.write(buffer, 0, count)
                        }
                    }
                    output.toByteArray()
                }
                parse(bytes)?.let(ReadResult::Opened) ?: ReadResult.Invalid
            } catch (e: CancellationException) {
                throw e
            } catch (_: IOException) {
                ReadResult.Failed
            } catch (_: SecurityException) {
                ReadResult.Failed
            } catch (_: IllegalArgumentException) {
                ReadResult.Failed
            }
        }
    }

    suspend fun write(
        resolver: ContentResolver,
        uri: Uri,
        image: Image,
        setIndex: Int,
        source: Source,
        tables: List<List<Float>>,
    ): Boolean {
        return withContext(Dispatchers.IO) {
            val modifiedBytes = try {
                apply(image, setIndex, source, tables)
            } catch (_: IllegalArgumentException) {
                return@withContext false
            }
            val saved = try {
                resolver.openOutputStream(uri, "wt")?.use { it.write(modifiedBytes) } != null
            } catch (e: CancellationException) {
                deleteFailedDocument(resolver, uri)
                throw e
            } catch (_: IOException) {
                false
            } catch (_: SecurityException) {
                false
            } catch (_: IllegalArgumentException) {
                false
            }
            if (!saved) deleteFailedDocument(resolver, uri)
            saved
        }
    }

    private fun deleteFailedDocument(resolver: ContentResolver, uri: Uri) {
        if (uri.scheme != ContentResolver.SCHEME_CONTENT) return
        try {
            DocumentsContract.getDocumentId(uri)
            DocumentsContract.deleteDocument(resolver, uri)
        } catch (_: Exception) {
            // Best effort only; preserve the original write failure.
        }
    }

    private fun hasMagic(bytes: ByteArray) = bytes.copyOfRange(bytes.size - MAGIC.size, bytes.size).contentEquals(MAGIC)

    private fun decodeName(bytes: ByteArray, start: Int): String {
        val nameBytes = bytes.copyOfRange(start, start + NAME_SIZE)
        val length = nameBytes.indexOf(0).let { if (it < 0) nameBytes.size else it }
        return String(nameBytes, 0, length, WINDOWS_OEM).trim()
    }

    private fun readPacked12(bytes: ByteArray, start: Int, index: Int): Int {
        val offset = start + index + (index shr 1)
        val first = bytes[offset].toInt() and 0xff
        val second = bytes[offset + 1].toInt() and 0xff
        return if (index and 1 == 0) first or ((second and 0x0f) shl 8) else (first ushr 4) or (second shl 4)
    }

    private fun writePacked12(bytes: ByteArray, start: Int, index: Int, value: Int) {
        val offset = start + index + (index shr 1)
        if (index and 1 == 0) {
            bytes[offset] = value.toByte()
            bytes[offset + 1] = ((bytes[offset + 1].toInt() and 0xf0) or ((value ushr 8) and 0x0f)).toByte()
        } else {
            bytes[offset] = ((bytes[offset].toInt() and 0x0f) or ((value and 0x0f) shl 4)).toByte()
            bytes[offset + 1] = (value ushr 4).toByte()
        }
    }

    private fun crc16(bytes: ByteArray, start: Int, length: Int): Int {
        var crc = 0xffff
        for (index in start until start + length) {
            crc = crc xor (bytes[index].toInt() and 0xff)
            repeat(8) { crc = if (crc and 1 != 0) (crc ushr 1) xor 0xa001 else crc ushr 1 }
        }
        return crc and 0xffff
    }

    private fun readU16(bytes: ByteArray, offset: Int) = (bytes[offset].toInt() and 0xff) or ((bytes[offset + 1].toInt() and 0xff) shl 8)
    private fun readU32(bytes: ByteArray, offset: Int) = (readU16(bytes, offset).toLong() or (readU16(bytes, offset + 2).toLong() shl 16)) and 0xffffffffL
    private fun putU16(bytes: ByteArray, offset: Int, value: Int) {
        bytes[offset] = value.toByte()
        bytes[offset + 1] = (value ushr 8).toByte()
    }

    companion object {
        private const val FLASH_SIZE = 131072
        private const val EEPROM_SIZE = 4096
        private const val SET_COUNT = 4
        private const val VE_CELL_COUNT = 256
        private const val VE_SCALE = 2048.0f
        private const val VE_MIN = 0.01f
        private const val VE_MAX = 1.99f
        private const val NAME_SIZE = 16
        private const val EEPROM_PARAMS_START = 1
        private const val EEPROM_PARAMS_CRC_OFFSET = 567
        private const val EEPROM_TABLES_START = 587
        private const val EEPROM_VE_START = 907
        private const val EEPROM_TABLES_CRC_OFFSET = 3418
        private const val FLASH_CRC_OFFSET = 129022
        private const val FLASH_FW_DATA_SIZE_OFFSET = 129020
        private const val FLASH_CODE_DATA_SIZE_OFFSET = 112520
        private const val FLASH_IOREM_SIZE_OFFSET = 112510
        private const val FLASH_IOREM_VERSION_OFFSET = 112509
        private const val FLASH_CONFIG_OFFSET = 112512
        private const val FW_DATA_SIZE = 16502
        private const val CODE_DATA_SIZE = 890
        private const val IOREM_SIZE = 880
        private const val IOREM_MAJOR_VERSION = 4
        private const val FUEL_INJECTION_BIT = 28
        private const val ATMEGA1284_BIT = 1
        private val FLASH_SET_NAME_BASES = intArrayOf(117635, 120468, 123301, 126134)
        private val FLASH_VE_BASES = intArrayOf(117955, 120788, 123621, 126454)
        private val MAGIC = byteArrayOf('1'.code.toByte(), '2'.code.toByte(), '8'.code.toByte(), '4'.code.toByte())
        private val WINDOWS_OEM: Charset = Charset.forName("IBM866")
    }
}
