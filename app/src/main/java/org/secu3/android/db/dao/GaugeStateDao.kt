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
import org.secu3.android.db.models.GaugeState
import org.secu3.android.ui.sensors.models.GaugeType

@Dao
interface GaugeStateDao : BaseDao<GaugeState> {

    @Query("SELECT * FROM gauge_state")
    suspend fun getAll(): List<GaugeState>

    @Query("SELECT * FROM gauge_state ORDER BY idx")
    suspend fun getAllOrderByIdx(): List<GaugeState>

    @Query("SELECT * FROM gauge_state WHERE gauge_type = :gaugeType")
    suspend fun getByGaugeType(gaugeType: GaugeType): GaugeState?

    @Query("SELECT MAX(idx) FROM gauge_state")
    suspend fun getMaxIdx(): Int?

    @Query("UPDATE gauge_state SET idx = idx - 1 WHERE idx > :missingIdx")
    suspend fun updateIdxGreaterThan(missingIdx: Int)

    @Transaction
    suspend fun deleteGauge(gaugeType: GaugeType) {
        val state = getByGaugeType(gaugeType) ?: return
        delete(state)
        updateIdxGreaterThan(state.idx)
    }
}