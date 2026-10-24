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

enum class IndicatorType {
    GAS_VALVE,
    THROTTLE,
    FI_FUEL,
    POWER_VALVE,
    STARTER_BLOCKING,
    AE,
    COOLING_FAN,
    CHECK_ENGINE,
    REV_LIM_FUEL_CUT,
    FLOOD_CLEAR_MODE,
    SYSTEM_LOCKED,
    IGN_I_INPUT,
    COND_I_INPUT,
    EPAS_I_INPUT,
    AFTERSTART_ENR,
    IAC_CLOSED_LOOP,
    UNIV_OUT1,
    UNIV_OUT2,
    UNIV_OUT3,
    UNIV_OUT4,
    UNIV_OUT5,
    UNIV_OUT6,
    GPA4_I,
    INPUT1,
    INPUT2,
    AUTO_I,
    MAPSEL0,
    REFPRS_I,
    ALTRN_I;

    fun isActive(packet: SensorsPacket): Boolean = when (this) {
        GAS_VALVE -> packet.gasBit > 0
        THROTTLE -> packet.carbBit > 0
        FI_FUEL -> packet.ephhValveBit > 0
        POWER_VALVE -> packet.epmValveBit > 0
        STARTER_BLOCKING -> packet.starterBlockBit > 0
        AE -> packet.accelerationEnrichment > 0
        COOLING_FAN -> packet.coolFanBit > 0
        CHECK_ENGINE -> packet.checkEngineBit > 0
        REV_LIM_FUEL_CUT -> packet.fc_revlim > 0
        FLOOD_CLEAR_MODE -> packet.floodclear > 0
        SYSTEM_LOCKED -> packet.sys_locked > 0
        IGN_I_INPUT -> packet.ign_i > 0
        COND_I_INPUT -> packet.cond_i > 0
        EPAS_I_INPUT -> packet.epas_i > 0
        AFTERSTART_ENR -> packet.aftstr_enr > 0
        IAC_CLOSED_LOOP -> packet.iac_closed_loop > 0
        UNIV_OUT1 -> packet.uniOut0Bit > 0
        UNIV_OUT2 -> packet.uniOut1Bit > 0
        UNIV_OUT3 -> packet.uniOut2Bit > 0
        UNIV_OUT4 -> packet.uniOut3Bit > 0
        UNIV_OUT5 -> packet.uniOut4Bit > 0
        UNIV_OUT6 -> packet.uniOut5Bit > 0
        GPA4_I -> packet.sensGpa4i > 0
        INPUT1 -> packet.sensInput1 > 0
        INPUT2 -> packet.sensInput2 > 0
        AUTO_I -> packet.sensAutoI > 0
        MAPSEL0 -> packet.sensMapsel0 > 0
        REFPRS_I -> packet.sensRefprs_i > 0
        ALTRN_I -> packet.sensAltrn_i > 0
    }
}
