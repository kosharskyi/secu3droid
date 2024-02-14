/*
 *    SecuDroid  - An open source, free manager for SECU-3 engine control unit
 *    Copyright (C) 2024 Vitalii O. Kosharskyi. Ukraine, Kyiv
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

package org.secu3.android.ui.main

import android.app.DownloadManager
import android.net.Uri
import android.os.Environment
import org.secu3.android.BuildConfig
import org.secu3.android.network.ApiService
import org.secu3.android.network.models.GitHubRelease
import retrofit2.Response
import javax.inject.Inject
import kotlin.math.min

class MainRepository @Inject constructor(
    private val apiService: ApiService,
    private val downloadManager: DownloadManager
) {

    suspend fun <T : Any> Response<T>.toResult(): Result<T> {
        return try {
            if (isSuccessful) {
                val body = body()
                if (body != null) {
                    Result.success(body)
                } else {
                    Result.failure(NullPointerException("Response body is null"))
                }
            } else {
                val errorBody = errorBody()?.string()
                val message = errorBody ?: message()
                Result.failure(Throwable("Unsuccessful response: $message"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getNewRelease(): GitHubRelease? {
        apiService.getLatestRelease().toResult().onSuccess { release ->
            val remoteVersion = release.tagName.split(".").map { it.toInt() }

            val versionName = if (BuildConfig.DEBUG) {
                BuildConfig.VERSION_NAME.split("-")[0]
            } else {
                BuildConfig.VERSION_NAME
            }

            val localVersion = versionName.split(".").map { it.toInt() }

            for (i in 0 until min(remoteVersion.size, localVersion.size)) {
                if (remoteVersion[i] > localVersion[i]) {
                    return release
                }
            }
        }

        return null
    }

    fun downloadReleaseFile(release: GitHubRelease) {
        val asset = release.assets.first { it.name.contains(".apk") }

        val uri = Uri.parse(asset.browserDownloadUrl)

        val request = DownloadManager.Request(uri).apply {
            setTitle(asset.name)
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, asset.name)
        }
        val id = downloadManager.enqueue(request)


    }

}