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

internal fun SensorsPacket.serviceFlagsForLog(): Int =
    serviceFlags.setBitValue(corrIdlve > 0, 9)

internal fun SensorsPacket.csvServiceFlagsColumn(): String =
    " %5d".format(Locale.US, serviceFlagsForLog())

private fun openBinaryOutput(file: File): LoggerOutput {
    val stream = FileOutputStream(file)
    return try {
        BinaryFileLoggerOutput(stream, stream.channel)
    } catch (error: IOException) {
        try {
            stream.close()
        } catch (closeError: IOException) {
            error.addSuppressed(closeError)
        }
        throw error
    }
}

private class BinaryFileLoggerOutput(
    private val stream: FileOutputStream,
    private val channel: FileChannel,
) : LoggerOutput {
    override fun writeBinary(bytes: ByteArray) {
        val buffer = ByteBuffer.wrap(bytes)
        while (buffer.hasRemaining()) channel.write(buffer)
    }

    override fun appendCsv(text: String) = Unit
    override fun flushCsv() = Unit

    override fun close() {
        var closeFailure: IOException? = null
        try {
            channel.close()
        } catch (error: IOException) {
            closeFailure = error
        }
        try {
            stream.close()
        } catch (error: IOException) {
            if (closeFailure == null) closeFailure = error else closeFailure.addSuppressed(error)
        }
        closeFailure?.let { throw it }
    }
}

private class FileWriterOutput(private val writer: FileWriter) : LoggerOutput {
    override fun writeBinary(bytes: ByteArray) = Unit
    override fun appendCsv(text: String) {
        writer.append(text)
    }
    override fun flushCsv() = writer.flush()
    override fun close() = writer.close()
}

internal fun encodeBinaryLogRecord(packet: SensorsPacket, mark: Int, now: LocalTime): ByteArray {
    // Float = 4 bytes; Int = 4 bytes; Byte = 1 byte; Short = 2 bytes
    val buf = ByteBuffer.allocate(252).also {
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
        put(alignByte)           // airflow-to-flags alignment
        putShort(flags.toShort())
        putShort(flags1.toShort())
        put(alignByte)           // flags-to-TPS alignment
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
        putFloat(if (packet.isAddI2Enabled) packet.airtempSensor else 999.99f)    //magic number indicates that IAT is not used
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
        // TODO: update capacity if new field is added
        put(mark.toByte())
        put(alignByte)
        putShort(packet.serviceFlagsForLog().toShort())
        putInt(packet.ecuErrors)
    }

    return buf.array()
}
@Singleton
class SecuLogger @Inject constructor(private val prefs: UserPrefs, private val fileHelper: FileHelper) {

    private val loggerSession = LoggerSession()
    private val loggerLock = Any()
    private var mLogFile: File? = null
    private var mMark: Int = 0

    private var mCsvDelimeter: String = ";"
    private val mCsvFormatter = DateTimeFormatter.ofPattern("HH:mm:ss.SS")

    val isLoggerStarted: Boolean
        get() = loggerSession.state.value.started
    internal val state = loggerSession.state

    internal fun acknowledgeLoggerError(errorId: Long) = loggerSession.acknowledgeError(errorId)

    fun prepareToLog() {
        synchronized(loggerLock) {
            if (isLoggerStarted) return
            mLogFile = if (prefs.isBinaryLogFormatEnabled) {
                fileHelper.generateS3lFile
            } else {
                fileHelper.generateCsvFile
            }
        }
    }

    fun startLogging() {
        synchronized(loggerLock) {
            if (isLoggerStarted) return
            val logFile = mLogFile ?: return
            val binary = logFile.extension == "s3l"
            val csvHeader = if (!binary) {
                mCsvDelimeter = prefs.CSVDelimeter
                if (prefs.isCsvTitleEnabled) generateCsvTitle(delimeter = mCsvDelimeter) else null
            } else {
                null
            }
            loggerSession.start(
                openOutput = { if (binary) openBinaryOutput(logFile) else FileWriterOutput(FileWriter(logFile, true)) },
                binary = binary,
                csvHeader = csvHeader,
            )
        }
    }

    fun stopLogging() {
        synchronized(loggerLock) {
            loggerSession.stop()
            mLogFile = null
        }
    }

    fun setMark1() {
        synchronized(loggerLock) { mMark = 1 }
    }

    fun setMark2() {
        synchronized(loggerLock) { mMark = 2 }
    }

    fun setMark3() {
        synchronized(loggerLock) { mMark = 4 }
    }

    fun logPacket(sensorsPacket: SensorsPacket) {
        synchronized(loggerLock) {
            if (isLoggerStarted) {
                if (prefs.isSensorLoggerEnabled.not()) {
                    stopLogging()
                    return
                }

                if (mLogFile?.extension == "s3l") {
                    writeBinaryLog(sensorsPacket)
                } else {
                    val csvString = sensorsPacket.scvString(mMark, mCsvDelimeter)
                    writeInCsv(csvString)
                }
                mMark = 0
            }
        }
    }

    private fun writeBinaryLog(packet: SensorsPacket) {
        val buf = encodeBinaryLogRecord(packet, mMark, LocalTime.now())
        loggerSession.writeBinary(buf)
    }

    private fun writeInCsv(csvString: String) {
        loggerSession.writeCsv(csvString)
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
            " %6.2f".format(Locale.US, lambdaCorr),
            " %6.2f".format(Locale.US, injPw),
            " %5d".format(Locale.US, tpsdot),
            " %6.2f".format(Locale.US, map2),
            " %6.2f".format(Locale.US, tmp2),
            " %7.2f".format(Locale.US, mapd),
            " %5.2f".format(Locale.US, afr),
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
            " %6.2f".format(Locale.US, maf),
            " %5.1f".format(Locale.US, ventDuty),
            " %2d".format(Locale.US, uniOutput),
            " %5d".format(Locale.US, mapdot),
            " %5.1f".format(Locale.US, fts),
            " %9.3f".format(Locale.US, cons_fuel),
            " %6.2f".format(Locale.US, lambdaCorr2),
            " %5.2f".format(Locale.US, afr2),
            " %5.2f".format(Locale.US, afrMap),
            " %5.1f".format(Locale.US, tchrg),
            " %6.2f".format(Locale.US, gasPressureSensor),
            " %7.2f".format(Locale.US, fuelPressureSensor),
            " %5.1f".format(Locale.US, apps1),
            " %5.1f".format(Locale.US, ots),
            " %5.1f".format(Locale.US, estTorque),
            " %5.1f".format(Locale.US, reqTorque),
            // TODO: update titles if new field is added
            " %01d".format(Locale.US, mark),
            csvServiceFlagsColumn(),
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
            "Gpa4_i",
            "Input1",
            "Input2",
            "Auto_i",
            "Mapsel0",
            "Refprs_i",
            "Altrn_i",
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
            "FPS",
            "APPS",
            "OTS",
            "EstTorque",
            "ReqTorque",
            "LogMarks",
            "ServFlag",
            "CECodes"
        )

        val csvRow = values.joinToString(separator = delimeter)

        return "$csvRow\r\n"
    }
}
