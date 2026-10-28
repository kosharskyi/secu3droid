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

package org.secu3.android.network.releases

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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReleaseRepository internal constructor(
    private val apiService: ApiService,
    private val downloadManager: DownloadManager,
    private val appPrefs: AppPrefs,
    private val isDebugBuild: Boolean,
    private val versionName: String,
    private val currentDate: () -> LocalDate,
) {
    @Inject
    constructor(
        apiService: ApiService,
        downloadManager: DownloadManager,
        appPrefs: AppPrefs,
    ) : this(apiService, downloadManager, appPrefs, BuildConfig.DEBUG, BuildConfig.VERSION_NAME, LocalDate::now)

    private val checkMutex = Mutex()

    suspend fun getNewRelease(): GitHubRelease? = checkMutex.withLock {
        if (!isDebugBuild && !appPrefs.lastAppVersionCheck.first().isBefore(currentDate())) return@withLock null

        val release = try {
            apiService.getLatestRelease().toResult().getOrNull()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (e: Exception) {
            null
        } ?: return@withLock null

        appPrefs.setLastAppVersionCheck(currentDate())

        try {
            val remoteVersion = Version.parse(release.tagName)
            val checkedVersionName = if (isDebugBuild) versionName.split("-")[0] else versionName
            val localVersion = Version.parse("v$checkedVersionName")
            release.takeIf { remoteVersion > localVersion }
        } catch (e: Exception) {
            null
        }
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

    internal fun findReleaseApkAsset(release: GitHubRelease): Asset? {
        val expectedAssetName = "Secu3Droid-${release.tagName}.apk"
        return release.assets.firstOrNull { it.name == expectedAssetName }
    }
}
