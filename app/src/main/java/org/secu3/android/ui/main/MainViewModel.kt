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

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import org.secu3.android.Secu3Repository
import org.secu3.android.models.packets.input.FirmwareInfoPacket
import org.secu3.android.network.models.GitHubRelease
import org.secu3.android.utils.AppPrefs
import org.secu3.android.utils.Task
import org.threeten.bp.LocalDate
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val secu3Repository: Secu3Repository,
    private val mainRepository: MainRepository,
    private val appPrefs: AppPrefs) : ViewModel() {


    val connectionStatusLiveData: LiveData<Boolean>
        get() = secu3Repository.connectionStatusLiveData

    val firmware: FirmwareInfoPacket?
        get() = secu3Repository.fwInfo

    val newReleaseAvailable: LiveData<GitHubRelease>
        get() = flow {
            val now = LocalDate.now()

            if (appPrefs.lastAppVersionCheck.isBefore(now)) {
                mainRepository.getNewRelease()?.let {
                    emit(it)
                }
            }
        }.asLiveData()

    init {
        viewModelScope.launch {
            mainRepository.checkAndInitDb()
            secu3Repository.startConnect()
        }
    }

    fun sendNewTask(task: Task) {
        secu3Repository.sendNewTask(task)
    }

    fun closeConnection() {
        secu3Repository.disable()
    }

    fun downloadRelease(release: GitHubRelease) {
        mainRepository.downloadReleaseFile(release)
    }
}