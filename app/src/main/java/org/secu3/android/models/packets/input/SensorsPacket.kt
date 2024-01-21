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
package org.secu3.android.models.packets.input

import org.secu3.android.models.packets.base.BaseSecu3Packet
import org.secu3.android.utils.getBitValue
import org.secu3.android.utils.setBitValue

data class SensorsPacket(
    var rpm: Int = 0,
    var map: Float = 0f,
    var voltage: Float = 0f,
    var temperature: Float = 0f,
    var currentAngle: Float = 0f,
    var knockValue: Float = 0f,
    var knockRetard: Float = 0f,
    var airflow: Int = 0,
    var sensorsFlags: Int = 0,
    var tps: Float = 0f,                  // TPS throttle position sensor (0...100%, x2); aka IAC valve
    var addI1: Float = 0f,                // ADD_I1 voltage
    var addI2: Float = 0f,                // ADD_I2 voltage
    var ecuErrors: Int = 0,            // Check Engine errors
    var chokePosition: Float = 0f,
    var gasDosePosition: Float = 0f,      // gas dosator position
    var speed: Float = 0f,              // vehicle speed (2 bytes)
    var distance: Float = 0f,             // distance (3 bytes)
    var fuelFlowFrequency: Float = 0f,           // instant fuel flow (frequency: 16000 pulses per 1L of burnt fuel)

    //Intake air temperature
    var airtempSensor: Float = 0f,        // 0x7FFF indicates that it is not used, voltage will be shown on the dashboard


    //corrections
    val serviceFlags: Int = 0,
    var strtAalt: Float = 0f,         // advance angle from start map
    var idleAalt: Float = 0f,         // advance angle from idle map
    var workAalt: Float = 0f,        // advance angle from work map
    var tempAalt: Float = 0f,         // advance angle from coolant temperature correction map
    var airtAalt: Float = 0f,        // advance angle from air temperature correction map
    var idlregAac: Float = 0f,        // advance angle correction from idling RPM regulator
    var octanAac: Float = 0f,         // octane correction value

    var lambda: FloatArray = FloatArray(2),           // lambda correction
    var injPw: Float = 0f,            // injector pulse width
    var tpsdot: Short = 0,           // TPS opening/closing speed

    // if SECU-3T
    var map2: Float = 0f,
    var mapd: Float = 0f,   //differential pressure

    var tmp2: Float = 0f,

    var sensAfr: FloatArray = FloatArray(2),              // #if defined(FUEL_INJECT) || defined(CARB_AFR) || defined(GD_CONTROL)

    var load: Float = 0f,
    var baroPress: Float = 0f,

    var iit: Int = 0,
    var injTimBegin: Float = 0f,
    var injTimEnd: Float = 0f,

    var rigidArg: Float = 0f,          // idling regulator's rigidity
    var grts: Float = 0f,                  // fas reducer's temperature
    var rxlaf: Int = 0,                 // RxL air flow
    var ftls: Float = 0f,                  //fuel tank level
    var egts: Float = 0f,                  //exhaust gas temperature
    var ops: Float = 0f,                   //oil pressure

    var sens_injDuty: Float = 0f,               //injector's duty in % (value * 2)
    var sens_maf: Float = 0f,                   //Air flow (g/sec) * 64 from the MAF sensor
    var ventDuty: Float = 0f,                   //PWM duty of cooling fan

    var uniOutput: Int = 0,                   //states of universal outputs
    var mapdot: Short = 0,                 //MAPdot
    var fts: Float = 0f,                    //fuel temperature
    var cons_fuel: Float = 0f,              //consumed fuel, L * 1024

    var lambda_mx: Float = 0f,              //mix of 2 lambda sensors
    var corrAfr: Float = 0f,                // Current value of air to fuel ration from AFR map
    var tchrg: Float = 0f,                  // Corrected value of MAT; charge temp

) : BaseSecu3Packet(){

    private var isSpeedUnitKm = true
    var isAddI2Enabled = false

    var inj_ffd: Float = 0f         // fuel flow in L/100km
    var inj_ffh: Float = 0f         // consumption in L/h

    val ephhValveBit: Int //idle cutoff valve
        get() = sensorsFlags.getBitValue(BITNUMBER_EPHH_VALVE)

    val carbBit: Int // throttle limit switch
        get() = sensorsFlags.getBitValue(BITNUMBER_CARB)

    val gasBit: Int
        get() = sensorsFlags.getBitValue(BITNUMBER_GAS)

    val epmValveBit: Int
        get() = sensorsFlags.getBitValue(BITNUMBER_EPM_VALVE)

    val checkEngineBit: Int
        get() = sensorsFlags.getBitValue(BITNUMBER_CE_STATE)

    val coolFanBit: Int
        get() = sensorsFlags.getBitValue(BITNUMBER_COOL_FAN)

    val starterBlockBit: Int
        get() = sensorsFlags.getBitValue(BITNUMBER_ST_BLOCK)

    val accelerationEnrichment: Int
        get() = sensorsFlags.getBitValue(BITNUMBER_ACCELERATION)

    val fc_revlim: Int // Rev. lim. fuel cut
        get() = sensorsFlags.getBitValue(BITNUMBER_FC_REVLIM)

    val floodclear: Int
        get() = sensorsFlags.getBitValue(BITNUMBER_FLOODCLEAR)

    val sys_locked: Int
        get() = sensorsFlags.getBitValue(BITNUMBER_SYS_LOCKED)

    val ign_i: Int
        get() = sensorsFlags.getBitValue(BITNUMBER_IGN_I)

    val cond_i: Int
        get() = sensorsFlags.getBitValue(BITNUMBER_COND_I)

    val epas_i: Int
        get() = sensorsFlags.getBitValue(BITNUMBER_EPAS_I)

    val aftstr_enr: Int // afterstart enrichment
        get() = sensorsFlags.getBitValue(BITNUMBER_AFTSTR_ENR)

    val iac_closed_loop: Int
        get() = sensorsFlags.getBitValue(BITNUMBER_IAC_CLOSED_LOOP)


    // UniOut States of universal outputs
    val uniOut0Bit: Int
        get() = uniOutput.getBitValue(0)

    val uniOut1Bit: Int
        get() = uniOutput.getBitValue(1)

    val uniOut2Bit: Int
        get() = uniOutput.getBitValue(2)

    val uniOut3Bit: Int
        get() = uniOutput.getBitValue(3)

    val uniOut4Bit: Int
        get() = uniOutput.getBitValue(4)

    val uniOut5Bit: Int
        get() = uniOutput.getBitValue(5)




    // ServiceFlags
    var knkret_use: Boolean
        get() = serviceFlags.getBitValue(0) > 0
        set(value) { serviceFlags.setBitValue(value, 0) }

    var strtUse: Boolean
        get() = serviceFlags.getBitValue(1) > 0
        set(value) { serviceFlags.setBitValue(value, 1) }

    var idleUse: Boolean
        get() = serviceFlags.getBitValue(2) > 0
        set(value) { serviceFlags.setBitValue(value, 2) }

    var workUse: Boolean
        get() = serviceFlags.getBitValue(3) > 0
        set(value) { serviceFlags.setBitValue(value, 3) }

    var tempUse: Boolean
        get() = serviceFlags.getBitValue(4) > 0
        set(value) { serviceFlags.setBitValue(value, 4) }

    var airtUse: Boolean
        get() = serviceFlags.getBitValue(5) > 0
        set(value) { serviceFlags.setBitValue(value, 5) }

    var idlregUse: Boolean
        get() = serviceFlags.getBitValue(6) > 0
        set(value) { serviceFlags.setBitValue(value, 6) }

    var octanUse: Boolean
        get() = serviceFlags.getBitValue(7) > 0
        set(value) { serviceFlags.setBitValue(value, 7) }

    var rigidUse: Boolean
        get() = serviceFlags.getBitValue(8) > 0
        set(value) { serviceFlags.setBitValue(value, 8) }



    companion object {

        private const val fffConst = 1600

        internal const val DESCRIPTOR = 'q'

        fun parse(data: String) = SensorsPacket().apply {
            rpm = data.get2Bytes(2)
            map = data.get2Bytes(4).toFloat() / MAP_MULTIPLIER
            voltage = data.get2Bytes(6).toFloat() / VOLTAGE_MULTIPLIER

            temperature = data.get2Bytes(8).toShort().toFloat().div(TEMPERATURE_MULTIPLIER).coerceIn(-99.9f, 999.0f)
            currentAngle = data.get2Bytes(10).toShort().toFloat() / ANGLE_DIVIDER
            knockValue = data.get2Bytes(12).toFloat() * ADC_DISCRETE

            data.get2Bytes(14).takeIf { it != 32767 }?.toFloat()?.let {
                knockRetard = it.div(ANGLE_DIVIDER)
                knkret_use = true
            }

            airflow = data[16].code

            sensorsFlags = data.get2Bytes(17)

            tps = data[19].code.toFloat() / TPS_MULTIPLIER

            addI1 = data.get2Bytes(20).toFloat() * ADC_DISCRETE
            addI2 = data.get2Bytes(22).toFloat() * ADC_DISCRETE

            ecuErrors = data.get4Bytes(24)
            chokePosition = data[28].code.toFloat() / CHOKE_MULTIPLIER
            gasDosePosition = data[29].code.toFloat() / GAS_DOSE_MULTIPLIER

            data.get2Bytes(30).toFloat().div(32.0f).coerceAtMost(999.9f).let {
                speed = it
                if (isSpeedUnitKm.not()) {
                    speed /= 1.609344f
                }
            }
            data.get3Bytes(32).toFloat().div(125.0f).coerceAtMost(99999.99f).let {
                distance = it
                if (isSpeedUnitKm.not()) {
                    distance /= 1.609344f
                }
            }

            fuelFlowFrequency = data.get2Bytes(35).toFloat().div(256.0f)

            //calculate value of fuel flow in L/100km
            if (speed > .0f) {
                inj_ffd = fuelFlowFrequency.div(speed).times((3600.0f * 100.0f).div(fffConst)).coerceIn(0f, 999.999f)
            }

            inj_ffh = 3600.0f * fuelFlowFrequency / fffConst  //consumption in L/h


            data.get2Bytes(37).takeIf { it != 0x7FFF }?.let {
                airtempSensor = it.toShort().toFloat().div(TEMPERATURE_MULTIPLIER).coerceIn(-99.9f, 999.0f)
                isAddI2Enabled = true
            }

            data.get2Bytes(39).takeIf { it != 32767 }?.let {
                strtAalt = it.toShort().toFloat().div(ANGLE_DIVIDER)
                strtUse = true
            }

            data.get2Bytes(41).takeIf { it != 32767 }?.let {
                idleAalt = it.toShort().toFloat().div(ANGLE_DIVIDER)
                idleUse = true
            }
            data.get2Bytes(43).takeIf { it != 32767 }?.let {
                workAalt = it.toShort().toFloat().div(ANGLE_DIVIDER)
                workUse = true
            }

            data.get2Bytes(45).takeIf { it != 32767 }?.let {
                tempAalt = it.toShort().toFloat().div(ANGLE_DIVIDER)
                tempUse = true
            }
            data.get2Bytes(47).takeIf { it != 32767 }?.let {
                airtAalt = it.toShort().toFloat().div(ANGLE_DIVIDER)
                airtUse = true
            }
            data.get2Bytes(49).takeIf { it != 32767 }?.let {
                idlregAac = it.toShort().toFloat().div(ANGLE_DIVIDER)
                idlregUse = true
            }
            data.get2Bytes(51).takeIf { it != 32767 }?.let {
                octanAac = it.toShort().toFloat().div(ANGLE_DIVIDER)
                octanUse = true
            }

            lambda[0] = data.get2Bytes(53).toShort().toFloat().div(512.0f).times(100.0f)  //obtain value in %

            // Injector PW(ms)
            injPw = data.get2Bytes(55).toFloat().times(3.2f).div(1000.0f)

            tpsdot = data.get2Bytes(57).toShort()

            map2 = data.get2Bytes(59).toFloat() / MAP_MULTIPLIER
            mapd = map2 - map

            tmp2 = data.get2Bytes(61).toShort().toFloat().div(TEMPERATURE_MULTIPLIER).coerceIn(-99.9f, 999.0f)


            sensAfr[0] = data.get2Bytes(63).toFloat() / AFR_MULTIPLIER
            load = data.get2Bytes(65).toFloat() / LOAD_PHYSICAL_MAGNITUDE_MULTIPLIER
            baroPress = data.get2Bytes(67).toFloat() / MAP_MULTIPLIER


            //inj.timing with info
            iit = data.get2Bytes(69)
            val mode = iit shr 14 and 0x3
            val inj_timing = (iit and 0x3FFF).toFloat() / 16.0f //inj.timing in crankshaft degrees
            val inj_pw_degr: Float = 360.0f / (1000.0f * 60.0f) * rpm * injPw //inj. PW in crankshaft degrees
            if (mode == 0)
            { //begin
                injTimBegin = inj_timing;
                injTimEnd = inj_timing - inj_pw_degr;
            }
            else if (mode == 1)
            { //middle
                injTimBegin = inj_timing + (inj_pw_degr / 2);
                injTimEnd = inj_timing - (inj_pw_degr / 2);
            }
            else
            {//end
                injTimBegin = inj_timing + inj_pw_degr;
                injTimEnd = inj_timing;
            }
            if (injTimBegin > 720.0f)
                injTimBegin -= 720.0f;
            if (injTimEnd < 0)
                injTimEnd += 720.0f;



            data[71].code.takeIf { it != 255 }?.toFloat()?.let {
                rigidUse = true
                rigidArg = 1.0f + it.div(256.0f / 7.0f)
            }

            //Gas reducer's temperature
            grts = data.get2Bytes(72).toShort().toFloat().div(TEMPERATURE_MULTIPLIER).coerceIn(-99.9f, 999.0f)           // gas reducer's temperature

            rxlaf = data.get2Bytes(74).times(32)           // RxL air flow
            ftls = data.get2Bytes(76).toFloat().div(FTLS_MULT)  // fuel tank level
            egts = data.get2Bytes(78).toFloat().div(EGTS_MULT)  // exhaust gas temperature
            ops = data.get2Bytes(80).toFloat().div(OPS_MULT)    // oil pressure

            sens_injDuty = data[82].code.toFloat() / 2.0f

            //mass air flow (g/sec)
            sens_maf = data.get2Bytes(83).toFloat() / MAFS_MULT
            ventDuty = data[85].code.toFloat().div(2.0f)

            uniOutput = data[86].code

            mapdot = data.get2Bytes(87).toShort()
            fts = data.get2Bytes(89).toFloat()  / FTS_MULT
            cons_fuel = data.get3Bytes(91).toFloat() / 1024.0f //consumed fuel

            sensAfr[1] = data.get2Bytes(94).toFloat() / AFR_MULTIPLIER

            lambda[1] = data.get2Bytes(96).toShort().toFloat().div(512.0f).times(100.0f)  //obtain value in %

            //mixed voltages from two EGO sensors
            lambda_mx = data.get2Bytes(98).times(ADC_DISCRETE)

            //AFR value from map
            corrAfr = data.get2Bytes(100).toFloat().div(AFR_MULTIPLIER)

            //Corrected MAT
            tchrg = data.get2Bytes(102).toShort().toFloat().div(TEMPERATURE_MULTIPLIER).coerceIn(-99.9f, 999.0f)
        }


        private const val BITNUMBER_EPHH_VALVE = 0
        private const val BITNUMBER_CARB = 1
        private const val BITNUMBER_GAS = 2
        private const val BITNUMBER_EPM_VALVE = 3
        private const val BITNUMBER_CE_STATE = 4
        private const val BITNUMBER_COOL_FAN = 5
        private const val BITNUMBER_ST_BLOCK = 6
        private const val BITNUMBER_ACCELERATION = 7  // acceleration enrichment flag
        private const val BITNUMBER_FC_REVLIM = 8  // fuel cut rev.lim. flag
        private const val BITNUMBER_FLOODCLEAR = 9  // flood clear mode flag
        private const val BITNUMBER_SYS_LOCKED = 10  // system locked flag (immobilizer)
        private const val BITNUMBER_IGN_I = 11  // IGN_I flag
        private const val BITNUMBER_COND_I = 12  // COND_I flag
        private const val BITNUMBER_EPAS_I = 13  // EPAS_I flag
        private const val BITNUMBER_AFTSTR_ENR = 14  // after start enrichment flag
        private const val BITNUMBER_IAC_CLOSED_LOOP = 15  // IAC closed loop flag
    }
}
