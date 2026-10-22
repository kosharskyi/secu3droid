package org.secu3.android.utils

import org.junit.Assert.assertEquals
import org.junit.Test
import org.threeten.bp.format.DateTimeFormatter
import java.io.File
import java.nio.file.Files

class FileHelperOrderingTest {
    @Test
    fun validLogsSortFirstByDateAndFallbackFilesByModificationThenName() {
        val root = Files.createTempDirectory("file-helper-order").toFile()
        try {
            val olderValid = File(root, "2026-10-20 10:00:00.csv").apply { writeText("older"); setLastModified(9_000) }
            val newerValid = File(root, "2026-10-22 10:00:00.s3l").apply { writeText("newer"); setLastModified(1_000) }
            val fallbackOlder = File(root, "notes.csv").apply { writeText("notes"); setLastModified(2_000) }
            val fallbackTieZ = File(root, "z-legacy.s3l").apply { writeText("z"); setLastModified(3_000) }
            val fallbackTieA = File(root, "a-legacy.csv").apply { writeText("a"); setLastModified(3_000) }
            val directory = File(root, "2026-10-23 10:00:00").apply { mkdir() }

            val result = orderLogFiles(
                listOf(fallbackOlder, olderValid, directory, fallbackTieZ, newerValid, fallbackTieA),
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            )

            assertEquals(
                listOf(newerValid, olderValid, fallbackTieA, fallbackTieZ, fallbackOlder),
                result,
            )
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun logMimeTypeIsSharedAndCaseInsensitive() {
        assertEquals("text/csv", logFileMimeType(File("trip.CsV")))
        assertEquals("application/octet-stream", logFileMimeType(File("trip.S3L")))
        assertEquals("application/octet-stream", logFileMimeType(File("unknown.bin")))
    }
}
