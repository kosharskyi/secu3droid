/*
 *    SecuDroid  - An open source, free manager for SECU-3 engine control unit
 *    Copyright (C) 2024 Vitaliy O. Kosharskyi. Ukraine, Kyiv
 *
 *    SECU-3  - An open source, free engine control unit
 *    Copyright (C) 2007-2024 Alexey A. Shabelnikov. Ukraine, Kyiv
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *    contacts:
 *                    http://secu-3.org
 *                    email: vetalkosharskiy@gmail.com
 */

package org.secu3.android.utils

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import org.threeten.bp.LocalDateTime
import org.threeten.bp.format.DateTimeFormatter
import java.io.File
import java.io.FileInputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileHelper @Inject constructor(@ApplicationContext private val context: Context) {

    private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private val rootDir = context.filesDir

    private val cacheDir = context.cacheDir

    val logsDir: File
        get() = File(rootDir, "logs").also {
            it.mkdir()
        }

    val listOfLogs: List<File>
        get() = logsDir.listFiles()?.toList()?.sortedByDescending { LocalDateTime.parse(it.nameWithoutExtension, dateTimeFormatter) } ?: emptyList()

    val generateCsvFile: File
        get() {
            val name = LocalDateTime.now().format(dateTimeFormatter)
            return File(logsDir, "${name}.csv")
        }

    val generateS3lFile: File
        get() {
            val name = LocalDateTime.now().format(dateTimeFormatter)
            return File(logsDir, "${name}.s3l")
        }

    fun getFileUri(file: File): Uri? {
        return try {
            FileProvider.getUriForFile(
                context,
                "org.secu3.android.fileprovider",
                file)
        } catch (e: IllegalArgumentException) {
            Log.e("File Selector",
                "The selected file can't be shared: $file")
            null
        }
    }

    fun saveLogToDefaultDownloads(file: File): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return saveLogToMediaStoreDownloads(file)
        }

        val directory = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            DEFAULT_LOG_EXPORT_DIRECTORY_NAME,
        ).also {
            it.mkdirs()
        }
        val destination = File(directory, file.name)
        file.copyTo(destination, overwrite = true)
        return defaultLogExportDirectoryLabel
    }

    fun saveLogToDirectoryUri(file: File, directoryUri: String): String {
        val treeUri = directoryUri.toUri()
        val directoryDocumentUri = DocumentsContract.buildDocumentUriUsingTree(
            treeUri,
            DocumentsContract.getTreeDocumentId(treeUri),
        )
        val documentUri = DocumentsContract.createDocument(
            context.contentResolver,
            directoryDocumentUri,
            file.mimeType(),
            file.name,
        ) ?: throw IllegalStateException("Unable to create ${file.name}")

        context.contentResolver.openOutputStream(documentUri, "w")?.use { outputStream ->
            FileInputStream(file).use { inputStream ->
                inputStream.copyTo(outputStream)
            }
        } ?: throw IllegalStateException("Unable to write ${file.name}")

        return directoryUri.toDirectoryLabel()
    }

    val defaultLogExportDirectoryLabel: String
        get() = "${Environment.DIRECTORY_DOWNLOADS}/$DEFAULT_LOG_EXPORT_DIRECTORY_NAME"

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun saveLogToMediaStoreDownloads(file: File): String {
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, file.name)
            put(MediaStore.Downloads.MIME_TYPE, file.mimeType())
            put(MediaStore.Downloads.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/$DEFAULT_LOG_EXPORT_DIRECTORY_NAME")
            put(MediaStore.Downloads.IS_PENDING, 1)
        }

        val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: throw IllegalStateException("Unable to create ${file.name}")

        try {
            context.contentResolver.openOutputStream(uri, "w")?.use { outputStream ->
                FileInputStream(file).use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
            } ?: throw IllegalStateException("Unable to write ${file.name}")

            ContentValues().apply {
                put(MediaStore.Downloads.IS_PENDING, 0)
            }.also {
                context.contentResolver.update(uri, it, null, null)
            }
        } catch (e: Exception) {
            context.contentResolver.delete(uri, null, null)
            throw e
        }

        return defaultLogExportDirectoryLabel
    }

    private fun File.mimeType(): String {
        return when (extension.lowercase()) {
            "csv" -> "text/csv"
            "s3l" -> "application/octet-stream"
            else -> "application/octet-stream"
        }
    }

    private fun String.toDirectoryLabel(): String {
        return runCatching {
            val treeDocumentId = DocumentsContract.getTreeDocumentId(toUri())
            treeDocumentId.substringAfter(':').ifBlank { treeDocumentId }
        }.getOrDefault(this)
    }

    private companion object {
        const val DEFAULT_LOG_EXPORT_DIRECTORY_NAME = "Secu3"
    }

}
