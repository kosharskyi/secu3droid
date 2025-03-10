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

        mLogFile = fileHelper.generateCsvFile
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

            val csvString = sensorsPacket.scvString(mMark, mCsvDelimeter)
            writeInCsv(csvString)

            mMark = 0
        }
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
            " %05d$delimeter%6.2f".format(rpm, currentAngle), // Locale.US to format float values with dots instead of comma
            " %6.2f".format(map),
            " %5.2f".format(voltage),
            " %6.2f".format(temperature),
            " %4.2f".format(knockValue),
            " %5.2f".format(knockRetard),
            " %02d".format(airflow),
            " %01d".format(carbBit),
            " %01d".format(gasBit),
            " %01d".format(ephhValveBit),
            " %01d".format(epmValveBit),
            " %01d".format(coolFanBit),
            " %01d".format(starterBlockBit),
            " %01d".format(accelerationEnrichment),
            " %01d".format(fc_revlim),
            " %01d".format(floodclear),
            " %01d".format(sys_locked),
            " %01d".format(checkEngineBit),
            " %01d".format(ign_i),
            " %01d".format(cond_i),
            " %01d".format(epas_i),
            " %01d".format(aftstr_enr),
            " %01d".format(iac_closed_loop),
            " %5.1f".format(tps),
            " %6.3f".format(addI1),
            " %6.3f".format(addI2),
            " %5.1f".format(chokePosition),
            " %5.1f".format(gasDosePosition),
            " %5.1f".format(speed),
            " %7.2f".format(distance),
            " %7.3f".format(inj_ffd),
            " %7.3f".format(fuelFlowFrequency),
            " %6.2f".format(if (isAddI2Enabled) airtempSensor else 999.99f), //magic number indicates that IAT is not used
            " %6.2f".format(strtAalt),
            " %6.2f".format(idleAalt),
            " %6.2f".format(workAalt),
            " %6.2f".format(tempAalt),
            " %6.2f".format(airtAalt),
            " %6.2f".format(idlregAac),
            " %6.2f".format(octanAac),
            " %6.2f".format(lambdaCorr),
            " %6.2f".format(injPw),
            " %05d".format(tpsdot),
            " %6.2f".format(map2),
            " %6.2f".format(tmp2),
            " %7.2f".format(mapd),
            " %5.2f".format(afr),
            " %6.2f".format(load),
            " %6.2f".format(baroPress),
            " %5.1f".format(injTimBegin),
            " %5.1f".format(injTimEnd),
            " %5.1f".format(grts),
            " %5.1f".format(ftls),
            " %6.1f".format(egts),
            " %4.2f".format(ops),
            " %5.1f".format(sens_injDuty),
            " %4.2f".format(rigidArg),
            " %07d".format(rxlaf),
            " %6.2f".format(maf),
            " %5.1f".format(ventDuty),
            " %02d".format(uniOutput),
            " %01d".format(mark),
            " %05d".format(serviceFlags),
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
            "LogMarks",
            "ServFlag",
            "CECodes"
        )

        val csvRow = values.joinToString(separator = delimeter)

        return "$csvRow\r\n"
    }
}