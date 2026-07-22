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

package org.secu3.android.ui.logger

import android.content.Intent
import android.os.Build
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import dagger.hilt.android.lifecycle.HiltViewModel
import org.secu3.android.utils.FileHelper
import org.secu3.android.utils.LogExportDestination
import org.secu3.android.utils.UserPrefs
import java.io.File
import javax.inject.Inject

@HiltViewModel
class SecuLogsViewModel @Inject constructor(
    private val fileHelper: FileHelper,
    private val userPrefs: UserPrefs,
) : ViewModel() {

    val getListOfLogFiles: List<File>
        get() = fileHelper.listOfLogs

    fun requiresDefaultDownloadsPermission(): Boolean {
        return userPrefs.logExportDestination is LogExportDestination.DefaultDownloads &&
                Build.VERSION.SDK_INT <= Build.VERSION_CODES.P
    }

    suspend fun saveLogFile(file: File): SaveLogResult = withContext(Dispatchers.IO) {
        runCatching {
            val destination = fileHelper.saveLog(file, userPrefs.logExportDestination)
            SaveLogResult(isSuccess = true, destination = destination)
        }.getOrElse {
            SaveLogResult(isSuccess = false, destination = "")
        }
    }

    fun getShareIntent(file: File): Intent {
        val fileUri = fileHelper.getFileUri(file)

        return Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_STREAM, fileUri)
            type = "text/*"
            flags =  Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
    }

}

data class SaveLogResult(
    val isSuccess: Boolean,
    val destination: String,
)
