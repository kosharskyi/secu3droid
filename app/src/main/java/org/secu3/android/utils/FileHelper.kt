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

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import org.threeten.bp.LocalDateTime
import org.threeten.bp.format.DateTimeFormatter
import java.io.File
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

}