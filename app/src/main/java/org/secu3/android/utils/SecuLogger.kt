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

import org.secu3.android.models.packets.SensorsPacket
import org.threeten.bp.LocalTime
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecuLogger @Inject constructor(private val prefs: LifeTimePrefs, private val fileHelper: FileHelper) {

    private var mLogFile: File? = null
    private var mMark: Int = 0

    private var mIsLoggingRunning = false
    private lateinit var mCsvWriter: FileWriter

    val isLoggerStarted: Boolean
        get() = mIsLoggingRunning

    fun prepareToLog() {
        if (mIsLoggingRunning) {
            return
        }

        mLogFile = fileHelper.getNewLogFile
        mMark = 1
    }

    fun startLogging() {
        if (mIsLoggingRunning) {
            return
        }

        if (mLogFile == null) {
            return
        }

        try {
            mCsvWriter = FileWriter(mLogFile, true)
            mIsLoggingRunning = true
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun stopLogging() {
        if (mIsLoggingRunning.not()) {
            return
        }

        try {
            mCsvWriter.close()
            mIsLoggingRunning = false
        } catch (e: IOException) {
            e.printStackTrace()
        }

        mLogFile = null
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

            try {
                mCsvWriter.append(sensorsPacket.scvString(mMark, prefs.CSVDelimeter))
                mCsvWriter.flush()
                mMark = 0
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    private fun SensorsPacket.scvString(mark: Int, delimeter: String): String {
        val now = LocalTime.now().toString().let {
            it.substring(0, it.lastIndex)
        }

        val values = listOf(
            now,
            " %05d$delimeter%6.2f".format(Locale.US, rpm, currentAngle), // Locale.US to format float values with dots instead of comma
            " %6.2f".format(Locale.US, map),
            " %5.2f".format(Locale.US, voltage),
            " %6.2f".format(Locale.US, temperature),
            " %4.2f".format(Locale.US, knockValue),
            " %5.2f".format(Locale.US, knockRetard),
            " %02d".format(Locale.US, airflow),
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
            " %05d".format(Locale.US, tpsdot),
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
            " %07d".format(Locale.US, rxlaf),
            " %6.2f".format(Locale.US, sens_maf),
            " %5.1f".format(Locale.US, ventDuty),
            " %02d".format(Locale.US, uniOutput),
            " %05d".format(Locale.US, mapdot),
            " %5.1f".format(Locale.US, fts),
            " %9.3f".format(Locale.US, cons_fuel),
            " %6.2f".format(Locale.US, lambda[1]),
            " %5.2f".format(Locale.US, sensAfr[1]),
            " %5.2f".format(Locale.US, corrAfr),
            " %5.1f".format(Locale.US, tchrg),
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
}