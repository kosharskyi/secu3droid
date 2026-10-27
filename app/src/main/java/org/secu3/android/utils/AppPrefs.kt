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

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.threeten.bp.LocalDate

@Singleton
class AppPrefs @Inject constructor(
    @Named(APP_PREFS_DATASTORE) private val dataStore: DataStore<Preferences>,
) {
    private val lastAppVersionCheckKey = stringPreferencesKey(LAST_APP_VERSION_CHECK_KEY)

    val lastAppVersionCheck: Flow<LocalDate> = dataStore.data.map { preferences ->
        preferences[lastAppVersionCheckKey]?.let(LocalDate::parse) ?: LocalDate.of(2000, 1, 1)
    }

    suspend fun setLastAppVersionCheck(value: LocalDate) {
        dataStore.edit { it[lastAppVersionCheckKey] = value.toString() }
    }

    companion object {
        const val APP_PREFS_DATASTORE = "appPrefsDataStore"
        const val LAST_APP_VERSION_CHECK_KEY = "last_app_version_check_stamp"
    }
}
