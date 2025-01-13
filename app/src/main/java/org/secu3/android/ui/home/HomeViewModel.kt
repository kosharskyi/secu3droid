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

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import org.secu3.android.BuildConfig
import org.secu3.android.connection.ConnectionState
import org.secu3.android.connection.Secu3Connection
import org.secu3.android.models.packets.input.FirmwareInfoPacket
import org.secu3.android.network.models.GitHubRelease
import org.secu3.android.utils.AppPrefs
import org.secu3.android.utils.Task
import org.secu3.android.utils.UserPrefs
import org.threeten.bp.LocalDate
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val secu3Connection: Secu3Connection,
    private val homeRepository: HomeRepository,
    private val appPrefs: AppPrefs,
    val prefs: UserPrefs,
) : ViewModel() {

    var isUserTapExit = false

    val connectionStatusLiveData: LiveData<ConnectionState>
        get() = secu3Connection.connectionStateFlow.asLiveData()

    val firmware: FirmwareInfoPacket?
        get() = secu3Connection.fwInfo

    val newReleaseAvailable: LiveData<GitHubRelease> = flow {
        val now = LocalDate.now()

        if (BuildConfig.DEBUG || appPrefs.lastAppVersionCheck.isBefore(now)) {
            homeRepository.getNewRelease()?.let {
                emit(it)
            }
        }
    }.asLiveData()

    init {
        viewModelScope.launch {
            homeRepository.checkAndInitDb()
        }
    }

    fun sendNewTask(task: Task) {
        secu3Connection.sendNewTask(task)
    }

    fun closeConnection() {
        isUserTapExit = true
        secu3Connection.disable()
    }

    fun downloadRelease(release: GitHubRelease) {
        homeRepository.downloadReleaseFile(release)
    }
}