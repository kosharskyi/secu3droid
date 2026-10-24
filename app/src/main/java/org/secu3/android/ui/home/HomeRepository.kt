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

package org.secu3.android.ui.home

import android.app.DownloadManager
import android.os.Environment
import androidx.core.net.toUri
import org.secu3.android.BuildConfig
import org.secu3.android.network.ApiService
import org.secu3.android.network.models.Asset
import org.secu3.android.network.models.GitHubRelease
import org.secu3.android.utils.AppPrefs
import org.secu3.android.utils.toResult
import org.threeten.bp.LocalDate
import javax.inject.Inject

class HomeRepository @Inject constructor(
    private val apiService: ApiService,
    private val downloadManager: DownloadManager,
    private val appPrefs: AppPrefs
) {

    suspend fun getNewRelease(): GitHubRelease? {
        try {
            apiService.getLatestRelease().toResult().onSuccess { release ->

                appPrefs.lastAppVersionCheck = LocalDate.now()

                val remoteVersion = Version.parse(release.tagName)

                val versionName = if (BuildConfig.DEBUG) {
                    BuildConfig.VERSION_NAME.split("-")[0]
                } else {
                    BuildConfig.VERSION_NAME
                }

                val localVersion = Version.parse("v$versionName")

                if (remoteVersion <= localVersion) {
                    return null
                }

                return release
            }
        } catch (e: Exception) {
            return null
        }

        return null
    }

    fun downloadReleaseFile(release: GitHubRelease) {
        val asset = findReleaseApkAsset(release) ?: return

        val uri = asset.browserDownloadUrl.toUri()

        val request = DownloadManager.Request(uri).apply {
            setTitle(asset.name)
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, asset.name)
        }
        downloadManager.enqueue(request)
    }

    companion object {
        internal fun findReleaseApkAsset(release: GitHubRelease): Asset? {
            val expectedAssetName = "Secu3Droid-${release.tagName}.apk"
            return release.assets.firstOrNull { it.name == expectedAssetName }
        }
    }

}
