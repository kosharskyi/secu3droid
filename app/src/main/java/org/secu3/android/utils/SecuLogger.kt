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

import org.secu3.android.models.packets.input.SensorsPacket
import org.threeten.bp.LocalTime
import org.threeten.bp.format.DateTimeFormatter
import java.io.File
import java.io.FileOutputStream
import java.io.FileWriter
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecuLogger @Inject constructor(private val prefs: UserPrefs, private val fileHelper: FileHelper) {

    private var mLogFile: File? = null
    private var mMark: Int = 0

    private var mIsLoggingRunning = false

    private var mCsvWriter: FileWriter? = null
    private var mCsvDelimeter: String = ";"
    private val mCsvFormatter = DateTimeFormatter.ofPattern("HH:mm:ss.SS")

    private var fileOutputStream: FileOutputStream? = null
    private var byteChanel: FileChannel? = null

    val isLoggerStarted: Boolean
        get() = mIsLoggingRunning

    fun prepareToLog() {
        if (mIsLoggingRunning) {
            return
        }

        mLogFile = if (prefs.isBinaryLogFormatEnabled) {
            fileHelper.generateS3lFile
        } else {
            fileHelper.generateCsvFile
        }
    }

    fun startLogging() {
        if (mIsLoggingRunning) {
            return
        }

        if (mLogFile == null) {
            return
        }

        if (mLogFile?.extension == "s3l") {
            fileOutputStream = FileOutputStream(mLogFile)
            byteChanel = fileOutputStream?.channel

            mIsLoggingRunning = true
            return
        }

        try {
            mCsvWriter = FileWriter(mLogFile, true)
            mIsLoggingRunning = true
            mCsvDelimeter = prefs.CSVDelimeter

            if (prefs.isCsvTitleEnabled) {
                writeInCsv(generateCsvTitle(delimeter = mCsvDelimeter))
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun stopLogging() {
        if (mIsLoggingRunning.not()) {
            return
        }

        try {
            mCsvWriter?.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }

        byteChanel?.close()
        fileOutputStream?.close()
        fileOutputStream = null
        byteChanel = null

        mCsvWriter = null

        mLogFile = null

        mIsLoggingRunning = false
    }

    fun setMark1() {
        mMark = 1
    }

    fun setMark2() {
        mMark = 2
    }

    fun setMark3() {
        mMark = 4
    }

    fun logPacket(sensorsPacket: SensorsPacket) {
        if (mIsLoggingRunning) {

            if (prefs.isSensorLoggerEnabled.not()) {
                stopLogging()
                return
            }

            if (mLogFile?.extension == "s3l") {  // check the extension to prevent disabling binary format in setting during log writing
                writeBinaryLog(sensorsPacket)
            } else {
                val csvString = sensorsPacket.scvString(mMark, mCsvDelimeter)
                writeInCsv(csvString)
            }
            mMark = 0
        }
    }

    private fun writeBinaryLog(packet: SensorsPacket) {
        val now = LocalTime.now()

        val buf = ByteBuffer.allocate(233).also {
            it.order(ByteOrder.LITTLE_ENDIAN)
        }

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

        var flags1 = packet.additionalFlags shr 1

        buf.apply {
            put(now.hour.toByte())
            put(now.minute.toByte())
            put(now.second.toByte())
            put(alignByte)           // align byte
            putShort(now.nano.div(1000000).toShort()) // milliseconds
            putShort(packet.rpm.toShort())
            putFloat(packet.currentAngle)
            putFloat(packet.map)
            putFloat(packet.voltage)
            putFloat(packet.temperature)
            putFloat(packet.knockValue)
            putFloat(packet.knockRetard)
            put(packet.airflow.toByte())
            put(alignByte)           // align byte
            put(alignByte)           // align byte
            put(alignByte)           // align byte
            putShort(flags.toShort())
            putShort(flags1.toShort())
            putFloat(packet.tps)
            putFloat(packet.addI1)
            putFloat(packet.addI2)
            putFloat(packet.chokePosition)
            putFloat(packet.gasDosePosition)
            putFloat(packet.speed)
            putFloat(packet.distance)
            putFloat(packet.inj_ffd.coerceIn(.0f, 999.999f))
            putFloat(packet.fuelFlowFrequency)
            putFloat(if (packet.isAddI2Enabled) packet.airtempSensor else 999.99f)    //magic number indicates that IAT is not used
            putFloat(packet.strtAalt)
            putFloat(packet.idleAalt)
            putFloat(packet.workAalt)
            putFloat(packet.tempAalt)
            putFloat(packet.airtAalt)
            putFloat(packet.idlregAac)
            putFloat(packet.octanAac)
            putFloat(packet.lambda[0])
            putFloat(packet.injPw)
            putInt(packet.tpsdot.toInt())
            putFloat(packet.map2)
            putFloat(packet.tmp2)
            putFloat(packet.mapd)
            putFloat(packet.sensAfr[0])
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
            putFloat(packet.sens_maf)
            putFloat(packet.ventDuty)
            put(packet.uniOutput.toByte())
            put(alignByte)
            put(alignByte)
            put(alignByte)
            putInt(packet.mapdot.toInt())
            putFloat(packet.fts)
            putFloat(packet.cons_fuel)
            putFloat(packet.lambda[1])
            putFloat(packet.sensAfr[1])
            putFloat(packet.corrAfr)
            putFloat(packet.tchrg)
            putFloat(packet.gasPressureSensor)
//            putFloat(packet.fps) // TODO: update capacity
            put(mMark.toByte())
            put(alignByte)
            putShort(packet.serviceFlags.toShort())
            putInt(packet.ecuErrors)
        }

        buf.flip()
        byteChanel?.write(buf)
    }

    private fun writeInCsv(csvString: String) {
        try {
            mCsvWriter?.append(csvString)
            mCsvWriter?.flush()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun SensorsPacket.scvString(mark: Int, delimeter: String): String {
        val now = LocalTime.now().format(mCsvFormatter)

        val values = listOf(
            now,
            " %5d$delimeter%6.2f".format(Locale.US, rpm, currentAngle), // Locale.US to format float values with dots instead of comma
            " %6.2f".format(Locale.US, map),
            " %5.2f".format(Locale.US, voltage),
            " %6.2f".format(Locale.US, temperature),
            " %4.2f".format(Locale.US, knockValue),
            " %5.2f".format(Locale.US, knockRetard),
            " %2d".format(Locale.US, airflow),
            " %01d".format(Locale.US, carbBit),
            " %01d".format(Locale.US, gasBit),
            " %01d".format(Locale.US, ephhValveBit),
            " %01d".format(Locale.US, epmValveBit),
            " %01d".format(Locale.US, coolFanBit),
            " %01d".format(Locale.US, starterBlockBit),
            " %01d".format(Locale.US, accelerationEnrichment),
            " %01d".format(Locale.US, fc_revlim),
            " %01d".format(Locale.US, floodclear),
            " %01d".format(Locale.US, sys_locked),
            " %01d".format(Locale.US, checkEngineBit),
            " %01d".format(Locale.US, ign_i),
            " %01d".format(Locale.US, cond_i),
            " %01d".format(Locale.US, epas_i),
            " %01d".format(Locale.US, sensGpa4i),
            " %01d".format(Locale.US, sensInput1),
            " %01d".format(Locale.US, sensInput2),
            " %01d".format(Locale.US, sensAutoI),
            " %01d".format(Locale.US, sensMapsel0),
            " %01d".format(Locale.US, sensRefprs_i),
            " %01d".format(Locale.US, sensAltrn_i),
            " %01d".format(Locale.US, aftstr_enr),
            " %01d".format(Locale.US, iac_closed_loop),
            " %5.1f".format(Locale.US, tps),
            " %6.3f".format(Locale.US, addI1),
            " %6.3f".format(Locale.US, addI2),
            " %5.1f".format(Locale.US, chokePosition),
            " %5.1f".format(Locale.US, gasDosePosition),
            " %5.1f".format(Locale.US, speed),
            " %7.2f".format(Locale.US, distance),
            " %7.3f".format(Locale.US, inj_ffd),
            " %7.3f".format(Locale.US, fuelFlowFrequency),
            " %6.2f".format(Locale.US, if (isAddI2Enabled) airtempSensor else 999.99f), //magic number indicates that IAT is not used
            " %6.2f".format(Locale.US, strtAalt),
            " %6.2f".format(Locale.US, idleAalt),
            " %6.2f".format(Locale.US, workAalt),
            " %6.2f".format(Locale.US, tempAalt),
            " %6.2f".format(Locale.US, airtAalt),
            " %6.2f".format(Locale.US, idlregAac),
            " %6.2f".format(Locale.US, octanAac),
            " %6.2f".format(Locale.US, lambda[0]),
            " %6.2f".format(Locale.US, injPw),
            " %5d".format(Locale.US, tpsdot),
            " %6.2f".format(Locale.US, map2),
            " %6.2f".format(Locale.US, tmp2),
            " %7.2f".format(Locale.US, mapd),
            " %5.2f".format(Locale.US, sensAfr[0]),
            " %6.2f".format(Locale.US, load),
            " %6.2f".format(Locale.US, baroPress),
            " %5.1f".format(Locale.US, injTimBegin),
            " %5.1f".format(Locale.US, injTimEnd),
            " %5.1f".format(Locale.US, grts),
            " %5.1f".format(Locale.US, ftls),
            " %6.1f".format(Locale.US, egts),
            " %4.2f".format(Locale.US, ops),
            " %5.1f".format(Locale.US, sens_injDuty),
            " %4.2f".format(Locale.US, rigidArg),
            " %7d".format(Locale.US, rxlaf),
            " %6.2f".format(Locale.US, sens_maf),
            " %5.1f".format(Locale.US, ventDuty),
            " %2d".format(Locale.US, uniOutput),
            " %5d".format(Locale.US, mapdot),
            " %5.1f".format(Locale.US, fts),
            " %9.3f".format(Locale.US, cons_fuel),
            " %6.2f".format(Locale.US, lambda[1]),
            " %5.2f".format(Locale.US, sensAfr[1]),
            " %5.2f".format(Locale.US, corrAfr),
            " %5.1f".format(Locale.US, tchrg),
            " %6.2f".format(Locale.US, gasPressureSensor),
            " %01d".format(Locale.US, mark),
            " %5d".format(Locale.US, serviceFlags),
            " $ceBits"
        )

        val csvRow = values.joinToString(separator = delimeter)

        return "$csvRow\r\n"
    }

    private val SensorsPacket.ceBits: String
        get() {
            val binaryString = Integer.toBinaryString(ecuErrors)
            return binaryString.padStart(32, '0')
        }

    private fun generateCsvTitle(delimeter: String): String {
        val values = listOf(
            "Time",
            "RPM",
            "IgnTim",
            "Map",
            "VBat",
            "CLT",
            "Knock",
            "KnockCorr",
            "LoadCurve",
            "CarbSw",
            "Gas_V",
            "IdleValve",
            "PowerValve",
            "CoolingFan",
            "StBlock",
            "AE",
            "FCRevLim",
            "FloodClear",
            "SysLocked",
            "CE",
            "Ign_i",
            "Cond_i",
            "Epas_I",
            "AftStrEnr",
            "IacClLoop",
            "TPS",
            "Add_i1",
            "Add_i2",
            "ChokePos",
            "GDPos",
            "VehSpeed",
            "PasDist",
            "FuelConsum",
            "FuelConsumF",
            "IAT",
            "StrtIgnTim",
            "IdleIgnTim",
            "WorkIgnTim",
            "TempIgnTim",
            "IATIgnTim",
            "IdlRegIgnTim",
            "IgnTimCorr",
            "EGOcorr",
            "InjPW",
            "TPSdot",
            "MAP2",
            "Tmp2",
            "DiffMAP",
            "AFR",
            "SynLoad",
            "BaroPress",
            "InjTimBeg",
            "InjTimEnd",
            "GRTS",
            "FTLS",
            "EGTS",
            "OPS",
            "InjDuty",
            "RigidArg",
            "Rxlaf",
            "MAF",
            "VentDuty",
            "UnivOuts",
            "MAPdot",
            "FTS",
            "FuelConsumed",
            "EGOcorr2",
            "AFR2",
            "AFRMap",
            "Tchrg",
            "GPS",
            "LogMarks",
            "ServFlag",
            "CECodes"
        )

        val csvRow = values.joinToString(separator = delimeter)

        return "$csvRow\r\n"
    }
}