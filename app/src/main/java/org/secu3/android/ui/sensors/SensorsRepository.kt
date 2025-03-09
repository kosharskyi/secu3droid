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

package org.secu3.android.ui.sensors

import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.secu3.android.db.AppDatabase
import org.secu3.android.db.models.GaugeState
import org.secu3.android.db.models.IndicatorState
import org.secu3.android.models.packets.input.SensorsPacket
import org.secu3.android.ui.sensors.models.GaugeItem
import org.secu3.android.ui.sensors.models.GaugeType
import org.secu3.android.ui.sensors.models.IndicatorItem
import org.secu3.android.ui.sensors.models.IndicatorType
import org.secu3.android.utils.AppPrefs
import java.util.Locale
import javax.inject.Inject

class SensorsRepository @Inject constructor(
    private val mPrefs: AppPrefs,
    private val db: AppDatabase,
)  {

    suspend fun getStoredGaugeStates(): List<GaugeState> = withContext(Dispatchers.IO) {
        db.gaugeStateDao().getAll()
    }

    suspend fun addGauge(gaugeType: GaugeType) = withContext(Dispatchers.IO)  {
        db.withTransaction {
            val maxIdx = db.gaugeStateDao().getMaxIdx()?.inc() ?: 0
            val state = GaugeState(0, gaugeType, maxIdx, false)
            db.gaugeStateDao().insertOrIgnore(state)
        }
    }

    suspend fun deleteGauge(gaugeType: GaugeType) = withContext(Dispatchers.IO)  {
        db.gaugeStateDao().deleteGauge(gaugeType)
    }

    suspend fun convertToGaugeItemList(packet: SensorsPacket) = withContext(Dispatchers.IO) {
        db.gaugeStateDao().getAllOrderByIdx().map { getGaugeItem(it, packet) }
    }

    private fun getGaugeItem(state: GaugeState, it: SensorsPacket): GaugeItem {
        val value = when (state.gaugeType) {
            GaugeType.RPM -> it.rpm.toString()
            GaugeType.MAP -> String.format(Locale.US, "%.1f", it.map)
            GaugeType.MAP_TURBO -> String.format(Locale.US, "%.1f", it.map)
            GaugeType.VOLTAGE -> String.format(Locale.US, "%.1f", it.voltage)
            GaugeType.CURRENT_ANGLE -> String.format(Locale.US, "%.2f", it.currentAngle)
            GaugeType.TEMPERATURE -> String.format(Locale.US, "%.1f", it.temperature)
            GaugeType.ADD1 -> String.format(Locale.US, "%.3f", it.addI1)
            GaugeType.ADD2 -> String.format(Locale.US, "%.3f", it.addI2)
            GaugeType.INJ_PW -> String.format(Locale.US, "%.2f", it.injPw)
            GaugeType.IAT -> String.format(Locale.US, "%.1f", it.airtempSensor)
            GaugeType.EGO_CORR -> String.format(Locale.US, "%.2f", it.lambdaCorr)
            GaugeType.CHOKE_POSITION -> String.format(Locale.US, "%.1f", it.chokePosition)
            GaugeType.AIR_FLOW -> it.airflow.toString()
            GaugeType.VEHICLE_SPEED -> String.format(Locale.US, "%.1f", it.speed)
            GaugeType.TPS_DOT -> it.tpsdot.toString()
            GaugeType.MAP2 -> String.format(Locale.US, "%.1f", it.map2)
            GaugeType.IAT2 -> String.format(Locale.US, "%.1f", it.tmp2)
            GaugeType.KNOCK_RETARD -> String.format(Locale.US, "%.1f", it.knockRetard)
            GaugeType.KNOCK_SIGNAL -> String.format(Locale.US, "%.3f", it.knockValue)
            GaugeType.WBO_AFR -> String.format(Locale.US, "%.1f", it.afr)
            GaugeType.IAC_VALVE -> String.format(Locale.US, "%.1f", it.tps)
            GaugeType.GAS_DISPENSER -> it.gasDosePosition.toString()
            GaugeType.SYNTHETIC_LOAD -> String.format(Locale.US, "%.1f", it.load)
            GaugeType.BEGIN_INJ_PHASE -> String.format(Locale.US, "%.1f", it.injTimBegin)
            GaugeType.END_INJ_PHASE -> String.format(Locale.US, "%.1f", it.injTimEnd)
            GaugeType.FUEL_CONSUMPTION_HZ -> it.fuelFlowFrequency.toString()
            GaugeType.GRTS -> String.format(Locale.US, "%.1f", it.grts)
            GaugeType.FUEL_LEVEL -> String.format(Locale.US, "%.1f", it.ftls)
            GaugeType.EXHAUST_GAS_TEMP -> String.format(Locale.US, "%.1f", it.egts)
            GaugeType.OIL_PRESSURE -> String.format(Locale.US, "%.1f", it.ops)
            GaugeType.INJ_DUTY -> String.format(Locale.US, "%.1f", it.sens_injDuty)
            GaugeType.MAF -> String.format(Locale.US, "%.1f", it.maf)
            GaugeType.FAN_DUTY -> it.ventDuty.toString()
        }

        return GaugeItem(state, value)
    }

    suspend fun getStoredIndicatorStates(): List<IndicatorState> = withContext(Dispatchers.IO) {
        db.indicatorStateDao().getAll()
    }

    suspend fun addIndicator(indicatorType: IndicatorType) = withContext(Dispatchers.IO)  {
        db.withTransaction {
            val maxIdx = db.indicatorStateDao().getMaxIdx()?.inc() ?: 0
            val state = IndicatorState(0, indicatorType, maxIdx)
            db.indicatorStateDao().insertOrIgnore(state)
        }
    }

    suspend fun deleteIndicator(indicatorType: IndicatorType) = withContext(Dispatchers.IO)  {
        db.indicatorStateDao().deleteIndicator(indicatorType)
    }

    suspend fun convertToIndicatorItemList(packet: SensorsPacket) = withContext(Dispatchers.IO) {
        db.indicatorStateDao().getAllOrderByIdx().map { getIndicatorItem(it, packet) }
    }

    private fun getIndicatorItem(state: IndicatorState, packet: SensorsPacket): IndicatorItem {
        val value = when (state.indicatorType) {
            IndicatorType.GAS_VALVE -> packet.gasBit > 0
            IndicatorType.THROTTLE -> packet.carbBit > 0
            IndicatorType.FI_FUEL -> packet.gasBit > 0
            IndicatorType.POWER_VALVE -> packet.epmValveBit > 0
            IndicatorType.STARTER_BLOCKING -> packet.starterBlockBit > 0
            IndicatorType.AE -> packet.accelerationEnrichment > 0
            IndicatorType.COOLING_FAN -> packet.coolFanBit > 0
            IndicatorType.CHECK_ENGINE -> packet.checkEngineBit > 0
            IndicatorType.REV_LIM_FUEL_CUT -> packet.fc_revlim > 0
            IndicatorType.FLOOD_CLEAR_MODE -> packet.floodclear > 0
            IndicatorType.SYSTEM_LOCKED -> packet.sys_locked > 0
            IndicatorType.IGN_I_INPUT -> packet.ign_i > 0
            IndicatorType.COND_I_INPUT -> packet.cond_i > 0
            IndicatorType.EPAS_I_INPUT -> packet.epas_i > 0
            IndicatorType.AFTERSTART_ENR -> packet.aftstr_enr > 0
            IndicatorType.IAC_CLOSED_LOOP -> packet.iac_closed_loop > 0
            IndicatorType.UNIV_OUT1 -> packet.uniOut0Bit > 0
            IndicatorType.UNIV_OUT2 -> packet.uniOut1Bit > 0
            IndicatorType.UNIV_OUT3 -> packet.uniOut2Bit > 0
            IndicatorType.UNIV_OUT4 -> packet.uniOut3Bit > 0
            IndicatorType.UNIV_OUT5 -> packet.uniOut4Bit > 0
            IndicatorType.UNIV_OUT6 -> packet.uniOut5Bit > 0
        }

        return IndicatorItem(state, value)
    }

    suspend fun updateGauges(swipedList: List<GaugeState>) = withContext(Dispatchers.IO) {
        db.gaugeStateDao().updateAll(swipedList)
    }

    suspend fun updateIndicators(swipedList: List<IndicatorState>) = withContext(Dispatchers.IO) {
        db.indicatorStateDao().updateAll(swipedList)
    }
}