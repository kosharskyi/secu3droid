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

package org.secu3.android.models.sensors

import org.secu3.android.models.packets.input.SensorsPacket

enum class GaugeType {
    RPM,
    MAP,
    MAP_TURBO,
    VOLTAGE,
    CURRENT_ANGLE,
    TEMPERATURE,
    ADD1,
    ADD2,
    INJ_PW,
    IAT,
    EGO_CORR,
    CHOKE_POSITION,
    AIR_FLOW,
    VEHICLE_SPEED,
    TPS_DOT,
    MAP2,
    DIFF_PRESSURE,
    IAT2,
    FUEL_CONSUMPTION,
    KNOCK_RETARD,
    KNOCK_SIGNAL,
    WBO_AFR,
    IAC_VALVE,
    GAS_DISPENSER,
    SYNTHETIC_LOAD,
    BEGIN_INJ_PHASE,
    END_INJ_PHASE,
    FUEL_CONSUMPTION_HZ,
    GRTS,
    FUEL_LEVEL,
    EXHAUST_GAS_TEMP,
    OIL_PRESSURE,
    INJ_DUTY,
    MAF,
    FAN_DUTY,
    MAP_DOT,
    FUEL_TEMP,
    EGO_CORR2,
    WBO_AFR2,
    WBO_AFR_TABL,
    AFR_DIFF,
    AFR_DIFF2,
    GAS_PRESSURE_SENS,
    FUEL_PRESSURE_SENS,
    APPS1,
    OTS,
    EST_TORQUE,
    REQ_TORQUE,
    DISTANCE,
    FUEL_CONSUMPTION_HOURLY,
    CONSUMED_FUEL;

    fun getGaugeItem(packet: SensorsPacket): Float {
        return when (this) {
            RPM -> packet.rpm.toFloat()
            MAP -> packet.map
            MAP_TURBO -> packet.map
            VOLTAGE -> packet.voltage
            CURRENT_ANGLE -> packet.currentAngle
            TEMPERATURE -> packet.temperature
            ADD1 -> packet.addI1
            ADD2 -> packet.addI2
            INJ_PW -> packet.injPw
            IAT -> packet.airtempSensor
            EGO_CORR -> packet.lambdaCorr
            CHOKE_POSITION -> packet.chokePosition
            AIR_FLOW -> packet.airflow.toFloat()
            VEHICLE_SPEED -> packet.speed
            TPS_DOT -> packet.tpsdot.toFloat()
            MAP2 ->packet.map2
            DIFF_PRESSURE -> packet.mapd
            IAT2 -> packet.tmp2
            FUEL_CONSUMPTION -> packet.inj_ffd
            KNOCK_RETARD -> packet.knockRetard
            KNOCK_SIGNAL -> packet.knockValue
            WBO_AFR -> packet.afr
            IAC_VALVE -> packet.tps
            GAS_DISPENSER -> packet.gasDosePosition
            SYNTHETIC_LOAD -> packet.load
            BEGIN_INJ_PHASE -> packet.injTimBegin
            END_INJ_PHASE -> packet.injTimEnd
            FUEL_CONSUMPTION_HZ -> packet.fuelFlowFrequency
            GRTS -> packet.grts
            FUEL_LEVEL -> packet.ftls
            EXHAUST_GAS_TEMP -> packet.egts
            OIL_PRESSURE -> packet.ops
            INJ_DUTY -> packet.sens_injDuty
            MAF -> packet.maf
            FAN_DUTY -> packet.ventDuty
            MAP_DOT -> packet.mapdot.toFloat()
            FUEL_TEMP -> packet.fts
            EGO_CORR2 -> packet.lambdaCorr2
            WBO_AFR2 -> packet.afr2
            WBO_AFR_TABL -> packet.afrMap
            AFR_DIFF -> {
                val difAfr = packet.afr - packet.afrMap
                difAfr
            }
            AFR_DIFF2 -> {
                val difAfr = packet.afr2 - packet.afrMap
                difAfr
            }

            GAS_PRESSURE_SENS -> packet.gasPressureSensor

            FUEL_PRESSURE_SENS -> packet.fuelPressureSensor
            APPS1 -> packet.apps1
            OTS -> packet.ots
            EST_TORQUE -> packet.estTorque
            REQ_TORQUE -> packet.reqTorque
            DISTANCE -> packet.distance
            FUEL_CONSUMPTION_HOURLY -> packet.inj_ffh
            CONSUMED_FUEL -> packet.cons_fuel
        }
    }
}
