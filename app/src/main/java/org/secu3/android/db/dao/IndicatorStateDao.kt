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

package org.secu3.android.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import org.secu3.android.db.models.IndicatorState
import org.secu3.android.ui.sensors.models.IndicatorType

@Dao
interface IndicatorStateDao : BaseDao<IndicatorState>{

    @Query("SELECT * FROM indicator_state")
    suspend fun getAll(): List<IndicatorState>

    @Query("SELECT * FROM indicator_state ORDER BY idx")
    suspend fun getAllOrderByIdx(): List<IndicatorState>

    @Query("SELECT * FROM indicator_state WHERE indicator_type = :indicatorType")
    suspend fun getByIndicatorType(indicatorType: IndicatorType): IndicatorState?

    @Query("SELECT MAX(idx) FROM indicator_state")
    suspend fun getMaxIdx(): Int?

    @Query("UPDATE indicator_state SET idx = idx - 1 WHERE idx > :missingIdx")
    suspend fun updateIdxGreaterThan(missingIdx: Int)

    @Transaction
    suspend fun deleteIndicator(indicatorType: IndicatorType) {
        val state = getByIndicatorType(indicatorType) ?: return
        delete(state)
        updateIdxGreaterThan(state.idx)
    }
}