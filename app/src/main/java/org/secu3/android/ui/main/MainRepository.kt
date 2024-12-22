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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.secu3.android.BuildConfig
import org.secu3.android.db.AppDatabase
import org.secu3.android.db.models.GaugeState
import org.secu3.android.db.models.IndicatorState
import org.secu3.android.network.ApiService
import org.secu3.android.network.models.GitHubRelease
import org.secu3.android.ui.sensors.models.GaugeType
import org.secu3.android.ui.sensors.models.IndicatorType
import org.secu3.android.utils.AppPrefs
import org.secu3.android.utils.toResult
import org.threeten.bp.LocalDate
import javax.inject.Inject

class MainRepository @Inject constructor(
    private val apiService: ApiService,
    private val downloadManager: DownloadManager,
    private val appPrefs: AppPrefs,
    private val db: AppDatabase
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

                val localVersion = Version.parse(versionName)

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
        val asset = release.assets.first { it.name.contains(".apk") }

        val uri = Uri.parse(asset.browserDownloadUrl)

        val request = DownloadManager.Request(uri).apply {
            setTitle(asset.name)
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, asset.name)
        }
        downloadManager.enqueue(request)
    }

    suspend fun checkAndInitDb() = withContext(Dispatchers.IO) {
        if (appPrefs.isDbInitNeed) {
            val gauges = listOf(
                GaugeState(0, GaugeType.RPM, 0),
                GaugeState(0, GaugeType.MAP, 1),
                GaugeState(0, GaugeType.VOLTAGE, 2),
                GaugeState(0, GaugeType.TEMPERATURE, 3),
            )

            db.gaugeStateDao().insertAll(gauges)

            val indicators = listOf(
                IndicatorState(0, IndicatorType.GAS_VALVE, 0),
                IndicatorState(0, IndicatorType.THROTTLE, 1),
                IndicatorState(0, IndicatorType.FI_FUEL, 2),
                IndicatorState(0, IndicatorType.POWER_VALVE, 3),
                IndicatorState(0, IndicatorType.STARTER_BLOCKING, 4),
                IndicatorState(0, IndicatorType.AE, 5),
                IndicatorState(0, IndicatorType.COOLING_FAN, 6),
                IndicatorState(0, IndicatorType.CHECK_ENGINE, 7),
                IndicatorState(0, IndicatorType.REV_LIM_FUEL_CUT, 8),
                IndicatorState(0, IndicatorType.FLOOD_CLEAR_MODE, 9)
            )

            db.indicatorStateDao().insertAll(indicators)

            appPrefs.isDbInitNeed = false
        }
    }

}