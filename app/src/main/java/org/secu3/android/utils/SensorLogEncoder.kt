/*
 *    SecuDroid  - An open source, free manager for SECU-3 engine control unit
 *    Copyright (C) 2026 Vitalii O. Kosharskyi. Ukraine, Kyiv
 *
 *    SECU-3  - An open source, free engine control unit
 *    Copyright (C) 2007-2026 Alexey A. Shabelnikov. Ukraine, Kyiv
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

import org.secu3.android.models.packets.input.SensorsPacket
import org.threeten.bp.LocalTime
import org.threeten.bp.format.DateTimeFormatter
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SensorLogEncoder @Inject constructor() {
    private val csvFormatter = DateTimeFormatter.ofPattern("HH:mm:ss.SS")

    fun encodeBinaryRecord(packet: SensorsPacket, mark: Int, now: LocalTime): ByteArray {
        val buf = ByteBuffer.allocate(252).order(ByteOrder.LITTLE_ENDIAN)
        val alignByte = 0xCC.toByte()
        var flags = 0
        flags = flags.setBitValue(packet.carbBit > 0, 15)
        flags = flags.setBitValue(packet.gasBit > 0, 14)
        flags = flags.setBitValue(packet.ephhValveBit > 0, 13)
        flags = flags.setBitValue(packet.epmValveBit > 0, 12)
        flags = flags.setBitValue(packet.coolFanBit > 0, 11)
        flags = flags.setBitValue(packet.starterBlockBit > 0, 10)
        flags = flags.setBitValue(packet.accelerationEnrichment > 0, 9)
        flags = flags.setBitValue(packet.fc_revlim > 0, 8)
        flags = flags.setBitValue(packet.floodclear > 0, 7)
        flags = flags.setBitValue(packet.sys_locked > 0, 6)
        flags = flags.setBitValue(packet.checkEngineBit > 0, 5)
        flags = flags.setBitValue(packet.ign_i > 0, 4)
        flags = flags.setBitValue(packet.cond_i > 0, 3)
        flags = flags.setBitValue(packet.epas_i > 0, 2)
        flags = flags.setBitValue(packet.aftstr_enr > 0, 1)
        flags = flags.setBitValue(packet.iac_closed_loop > 0, 0)
        val flags1 = packet.additionalFlags shr 1

        buf.apply {
            put(now.hour.toByte())
            put(now.minute.toByte())
            put(now.second.toByte())
            put(alignByte)
            putShort(now.nano.div(1000000).toShort())
            putShort(packet.rpm.toShort())
            putFloat(packet.currentAngle)
            putFloat(packet.map)
            putFloat(packet.voltage)
            putFloat(packet.temperature)
            putFloat(packet.knockValue)
            putFloat(packet.knockRetard)
            put(packet.airflow.toByte())
            put(alignByte)
            putShort(flags.toShort())
            putShort(flags1.toShort())
            put(alignByte)
            put(alignByte)
            putFloat(packet.tps)
            putFloat(packet.addI1)
            putFloat(packet.addI2)
            putFloat(packet.chokePosition)
            putFloat(packet.gasDosePosition)
            putFloat(packet.speed)
            putFloat(packet.distance)
            putFloat(packet.inj_ffd.coerceIn(.0f, 999.999f))
            putFloat(packet.fuelFlowFrequency)
            putFloat(if (packet.isAddI2Enabled) packet.airtempSensor else 999.99f)
            putFloat(packet.strtAalt)
            putFloat(packet.idleAalt)
            putFloat(packet.workAalt)
            putFloat(packet.tempAalt)
            putFloat(packet.airtAalt)
            putFloat(packet.idlregAac)
            putFloat(packet.octanAac)
            putFloat(packet.lambdaCorr)
            putFloat(packet.injPw)
            putInt(packet.tpsdot.toInt())
            putFloat(packet.map2)
            putFloat(packet.tmp2)
            putFloat(packet.mapd)
            putFloat(packet.afr)
            putFloat(packet.load)
            putFloat(packet.baroPress)
            putFloat(packet.injTimBegin)
            putFloat(packet.injTimEnd)
            putFloat(packet.grts)
            putFloat(packet.ftls)
            putFloat(packet.egts)
            putFloat(packet.ops)
            putFloat(packet.sens_injDuty)
            putFloat(packet.rigidArg)
            putInt(packet.rxlaf)
            putFloat(packet.maf)
            putFloat(packet.ventDuty)
            put(packet.uniOutput.toByte())
            put(alignByte)
            put(alignByte)
            put(alignByte)
            putInt(packet.mapdot.toInt())
            putFloat(packet.fts)
            putFloat(packet.cons_fuel)
            putFloat(packet.lambdaCorr2)
            putFloat(packet.afr2)
            putFloat(packet.afrMap)
            putFloat(packet.tchrg)
            putFloat(packet.gasPressureSensor)
            putFloat(packet.fuelPressureSensor)
            putFloat(packet.apps1)
            putFloat(packet.ots)
            putFloat(packet.estTorque)
            putFloat(packet.reqTorque)
            put(mark.toByte())
            put(alignByte)
            putShort(serviceFlagsForLog(packet).toShort())
            putInt(packet.ecuErrors)
        }
        return buf.array()
    }

    fun encodeCsvRow(packet: SensorsPacket, mark: Int, delimiter: String, now: LocalTime): String {
        val time = now.format(csvFormatter)
        val values = listOf(
            time,
            " %5d$delimiter%6.2f".format(Locale.US, packet.rpm, packet.currentAngle),
            " %6.2f".format(Locale.US, packet.map),
            " %5.2f".format(Locale.US, packet.voltage),
            " %6.2f".format(Locale.US, packet.temperature),
            " %4.2f".format(Locale.US, packet.knockValue),
            " %5.2f".format(Locale.US, packet.knockRetard),
            " %2d".format(Locale.US, packet.airflow),
            " %01d".format(Locale.US, packet.carbBit),
            " %01d".format(Locale.US, packet.gasBit),
            " %01d".format(Locale.US, packet.ephhValveBit),
            " %01d".format(Locale.US, packet.epmValveBit),
            " %01d".format(Locale.US, packet.coolFanBit),
            " %01d".format(Locale.US, packet.starterBlockBit),
            " %01d".format(Locale.US, packet.accelerationEnrichment),
            " %01d".format(Locale.US, packet.fc_revlim),
            " %01d".format(Locale.US, packet.floodclear),
            " %01d".format(Locale.US, packet.sys_locked),
            " %01d".format(Locale.US, packet.checkEngineBit),
            " %01d".format(Locale.US, packet.ign_i),
            " %01d".format(Locale.US, packet.cond_i),
            " %01d".format(Locale.US, packet.epas_i),
            " %01d".format(Locale.US, packet.sensGpa4i),
            " %01d".format(Locale.US, packet.sensInput1),
            " %01d".format(Locale.US, packet.sensInput2),
            " %01d".format(Locale.US, packet.sensAutoI),
            " %01d".format(Locale.US, packet.sensMapsel0),
            " %01d".format(Locale.US, packet.sensRefprs_i),
            " %01d".format(Locale.US, packet.sensAltrn_i),
            " %01d".format(Locale.US, packet.aftstr_enr),
            " %01d".format(Locale.US, packet.iac_closed_loop),
            " %5.1f".format(Locale.US, packet.tps),
            " %6.3f".format(Locale.US, packet.addI1),
            " %6.3f".format(Locale.US, packet.addI2),
            " %5.1f".format(Locale.US, packet.chokePosition),
            " %5.1f".format(Locale.US, packet.gasDosePosition),
            " %5.1f".format(Locale.US, packet.speed),
            " %7.2f".format(Locale.US, packet.distance),
            " %7.3f".format(Locale.US, packet.inj_ffd),
            " %7.3f".format(Locale.US, packet.fuelFlowFrequency),
            " %6.2f".format(Locale.US, if (packet.isAddI2Enabled) packet.airtempSensor else 999.99f),
            " %6.2f".format(Locale.US, packet.strtAalt),
            " %6.2f".format(Locale.US, packet.idleAalt),
            " %6.2f".format(Locale.US, packet.workAalt),
            " %6.2f".format(Locale.US, packet.tempAalt),
            " %6.2f".format(Locale.US, packet.airtAalt),
            " %6.2f".format(Locale.US, packet.idlregAac),
            " %6.2f".format(Locale.US, packet.octanAac),
            " %6.2f".format(Locale.US, packet.lambdaCorr),
            " %6.2f".format(Locale.US, packet.injPw),
            " %5d".format(Locale.US, packet.tpsdot),
            " %6.2f".format(Locale.US, packet.map2),
            " %6.2f".format(Locale.US, packet.tmp2),
            " %7.2f".format(Locale.US, packet.mapd),
            " %5.2f".format(Locale.US, packet.afr),
            " %6.2f".format(Locale.US, packet.load),
            " %6.2f".format(Locale.US, packet.baroPress),
            " %5.1f".format(Locale.US, packet.injTimBegin),
            " %5.1f".format(Locale.US, packet.injTimEnd),
            " %5.1f".format(Locale.US, packet.grts),
            " %5.1f".format(Locale.US, packet.ftls),
            " %6.1f".format(Locale.US, packet.egts),
            " %4.2f".format(Locale.US, packet.ops),
            " %5.1f".format(Locale.US, packet.sens_injDuty),
            " %4.2f".format(Locale.US, packet.rigidArg),
            " %7d".format(Locale.US, packet.rxlaf),
            " %6.2f".format(Locale.US, packet.maf),
            " %5.1f".format(Locale.US, packet.ventDuty),
            " %2d".format(Locale.US, packet.uniOutput),
            " %5d".format(Locale.US, packet.mapdot),
            " %5.1f".format(Locale.US, packet.fts),
            " %9.3f".format(Locale.US, packet.cons_fuel),
            " %6.2f".format(Locale.US, packet.lambdaCorr2),
            " %5.2f".format(Locale.US, packet.afr2),
            " %5.2f".format(Locale.US, packet.afrMap),
            " %5.1f".format(Locale.US, packet.tchrg),
            " %6.2f".format(Locale.US, packet.gasPressureSensor),
            " %7.2f".format(Locale.US, packet.fuelPressureSensor),
            " %5.1f".format(Locale.US, packet.apps1),
            " %5.1f".format(Locale.US, packet.ots),
            " %5.1f".format(Locale.US, packet.estTorque),
            " %5.1f".format(Locale.US, packet.reqTorque),
            " %01d".format(Locale.US, mark),
            " %5d".format(Locale.US, serviceFlagsForLog(packet)),
            " ${Integer.toBinaryString(packet.ecuErrors).padStart(32, '0')}"
        )
        return values.joinToString(separator = delimiter) + "\r\n"
    }

    fun csvTitle(delimiter: String): String = listOf(
        "Time", "RPM", "IgnTim", "Map", "VBat", "CLT", "Knock", "KnockCorr", "LoadCurve",
        "CarbSw", "Gas_V", "IdleValve", "PowerValve", "CoolingFan", "StBlock", "AE", "FCRevLim",
        "FloodClear", "SysLocked", "CE", "Ign_i", "Cond_i", "Epas_I", "Gpa4_i", "Input1", "Input2",
        "Auto_i", "Mapsel0", "Refprs_i", "Altrn_i", "AftStrEnr", "IacClLoop", "TPS", "Add_i1",
        "Add_i2", "ChokePos", "GDPos", "VehSpeed", "PasDist", "FuelConsum", "FuelConsumF", "IAT",
        "StrtIgnTim", "IdleIgnTim", "WorkIgnTim", "TempIgnTim", "IATIgnTim", "IdlRegIgnTim",
        "IgnTimCorr", "EGOcorr", "InjPW", "TPSdot", "MAP2", "Tmp2", "DiffMAP", "AFR", "SynLoad",
        "BaroPress", "InjTimBeg", "InjTimEnd", "GRTS", "FTLS", "EGTS", "OPS", "InjDuty", "RigidArg",
        "Rxlaf", "MAF", "VentDuty", "UnivOuts", "MAPdot", "FTS", "FuelConsumed", "EGOcorr2", "AFR2",
        "AFRMap", "Tchrg", "GPS", "FPS", "APPS", "OTS", "EstTorque", "ReqTorque", "LogMarks",
        "ServFlag", "CECodes"
    ).joinToString(separator = delimiter) + "\r\n"

    private fun serviceFlagsForLog(packet: SensorsPacket): Int =
        packet.serviceFlags.setBitValue(packet.corrIdlve > 0, 9)
}
