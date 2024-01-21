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

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.secu3.android.models.packets.input.SensorsPacket
import org.secu3.android.ui.sensors.models.GaugeItem
import org.secu3.android.ui.sensors.models.GaugeType
import org.secu3.android.ui.sensors.models.IndicatorItem
import org.secu3.android.ui.sensors.models.IndicatorType
import org.secu3.android.utils.LifeTimePrefs
import java.util.Locale
import javax.inject.Inject

class SensorsRepository @Inject constructor(private val mPrefs: LifeTimePrefs)  {

    suspend fun convertToGaugeItemList(packet: SensorsPacket) = withContext(Dispatchers.IO) {
        mPrefs.gaugesEnabled.map { getGaugeItem(it, packet) }
    }

    private fun getGaugeItem(type: GaugeType, it: SensorsPacket): GaugeItem {
        val value = when (type) {
            GaugeType.RPM -> it.rpm.toString()
            GaugeType.MAP -> String.format(Locale.US, "%.1f", it.map)
            GaugeType.VOLTAGE -> String.format(Locale.US, "%.1f", it.voltage)
            GaugeType.CURRENT_ANGLE -> String.format(Locale.US, "%.2f", it.currentAngle)
            GaugeType.TEMPERATURE -> String.format(Locale.US, "%.1f", it.temperature)
            GaugeType.ADD1 -> String.format(Locale.US, "%.3f", it.addI1)
            GaugeType.ADD2 -> String.format(Locale.US, "%.3f", it.addI2)
            GaugeType.INJ_PW -> String.format(Locale.US, "%.2f", it.injPw)
            GaugeType.IAT -> String.format(Locale.US, "%.1f", it.airtempSensor)
            GaugeType.EGO_CORR -> String.format(Locale.US, "%.2f", it.lambda[0])
            GaugeType.CHOKE_POSITION -> String.format(Locale.US, "%.1f", it.chokePosition)
            GaugeType.AIR_FLOW -> it.airflow.toString()
            GaugeType.VEHICLE_SPEED -> String.format(Locale.US, "%.1f", it.speed)
            GaugeType.TPS_DOT -> it.tpsdot.toString()
            GaugeType.MAP2 -> String.format(Locale.US, "%.1f", it.map2)
            GaugeType.DIFF_PRESSURE -> String.format(Locale.US, "%.1f", it.mapd)
            GaugeType.IAT2 -> String.format(Locale.US, "%.1f", it.tmp2)
            GaugeType.FUEL_CONSUMPTION -> String.format(Locale.US, "%.2f", it.cons_fuel)
            GaugeType.KNOCK_RETARD -> String.format(Locale.US, "%.1f", it.knockRetard)
            GaugeType.KNOCK_SIGNAL -> String.format(Locale.US, "%.3f", it.knockValue)
            GaugeType.WBO_AFR -> String.format(Locale.US, "%.1f", it.sensAfr[0])
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
            GaugeType.MAF -> String.format(Locale.US, "%.1f", it.sens_maf)
            GaugeType.FAN_DUTY -> it.ventDuty.toString()
            GaugeType.MAP_DOT -> it.mapdot.toString()
            GaugeType.FUEL_TEMP -> String.format(Locale.US, "%.1f", it.fts)
            GaugeType.EGO_CORR2 -> String.format(Locale.US, "%.1f", it.lambda[1])
            GaugeType.WBO_AFR2 -> String.format(Locale.US, "%.1f", it.sensAfr[1])
            GaugeType.WBO_AFR_TABL -> String.format(Locale.US, "%.2f", it.corrAfr)
//            GaugeType.AFR_DIFF -> String.format(Locale.US, "%.2f", it.lambda_mx)
//            GaugeType.AFR_DIFF2 -> TODO()
        }

        return GaugeItem(type, value)
    }

    suspend fun convertToIndicatorItemList(packet: SensorsPacket) = withContext(Dispatchers.IO) {
//        mPrefs.indicatorsEnabled.map { getIndicatorItem(it, packet) }
        IndicatorType.entries.map { getIndicatorItem(it, packet) }
    }

    private fun getIndicatorItem(type: IndicatorType, packet: SensorsPacket): IndicatorItem {
        val value = when (type) {
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

        return IndicatorItem(type, value)
    }
}