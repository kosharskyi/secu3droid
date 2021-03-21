/* SecuDroid  - An open source, free manager for SECU-3 engine control unit
   Copyright (C) 2020 Vitaliy O. Kosharskiy. Ukraine, Kharkiv

   SECU-3  - An open source, free engine control unit
   Copyright (C) 2007 Alexey A. Shabelnikov. Ukraine, Kyiv

   This program is free software: you can redistribute it and/or modify
   it under the terms of the GNU General Public License as published by
   the Free Software Foundation, either version 3 of the License, or
   (at your option) any later version.

   This program is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.

   You should have received a copy of the GNU General Public License
   along with this program.  If not, see <http://www.gnu.org/licenses/>.

   contacts:
              http://secu-3.org
              email: vetalkosharskiy@gmail.com
*/
package org.secu3.android.models.packets

data class SensorsPacket(var rpm: Int = 0,
                         var map: Float = 0f,
                         var voltage: Float = 0f,
                         var temperature: Float = 0f,
                         var currentAngle: Float = 0f,
                         var knockValue: Float = 0f,
                         var knockRetard: Float = 0f,
                         var airflow: Int = 0,
                         var sensorsFlags: Int = 0,
                         var tps: Float = 0f,                  // TPS throttle position sensor (0...100%, x2)
                         var addI1: Float = 0f,                // ADD_I1 voltage
                         var addI2: Float = 0f,                // ADD_I2 voltage
                         var ecuErrors: Short = 0,            // Check Engine errors
                         var chokePosition: Float = 0f,
                         var gasDosePosition: Int = 0,      // gas dosator position
                         private var rawSpeed: Int = 0,              // vehicle speed (2 bytes)
                         private var rawDistance: Int = 0,             // distance (3 bytes)
                         var fuelInject: Int = 0,           // instant fuel flow (frequency: 16000 pulses per 1L of burnt fuel)
                         var airtempSensor: Float = 0f,        // 0x7FFF indicates that it is not used, voltage will be shown on the dashboard


    //corrections
                         var strtAalt: Int = 0,         // advance angle from start map
                         var idleAalt: Int = 0,         // advance angle from idle map
                         var workAalt: Int = 0,        // advance angle from work map
                         var tempAalt: Int = 0,         // advance angle from coolant temperature correction map
                         var airtAalt: Int = 0,        // advance angle from air temperature correction map
                         var idlregAac: Int = 0,        // advance angle correction from idling RPM regulator
                         var octanAac: Int = 0,         // octane correction value

                         var lambda: Int = 0,           // lambda correction
                         var injPw: Int = 0,            // injector pulse width
                         var tpsdot: Int = 0,           // TPS opening/closing speed

    // if SECU-3T
                         var map2: Float = 0f,
                         var tmp2: Float = 0f,

                         var afr: Float = 0f,              // #if defined(FUEL_INJECT) || defined(CARB_AFR) || defined(GD_CONTROL)

                         var load: Int = 0,
                         var baroPress: Int = 0,

                         var iit: Short = 0,
                         var rigidArg: Int = 0,
                         var grts: Int = 0,                  // fas reducer's temperature
                         var rxlaf: Int = 0                  // RxL air flow

) : BaseSecu3Packet(){

    fun getSpeed(): Float {
        var speed = 0f

        if (rawSpeed != 0 && rawSpeed != 65535) { //speed sensor is used, value is correct

            val periodS = rawSpeed / 312500f      //period in seconds

            speed = ((periodDistance / periodS) * 3600.0f) / 1000.0f; //Km/h

            if (speed >= 999.9f) {
                speed = 999.9f
            }
        }

        return speed
    }

    fun getDistance(): Float {
        var distance = (periodDistance * rawDistance) / 1000.0f

        if (distance > 9999.99f) {
            distance = 9999.99f
        }

        return distance
    }

    val gasBit: Int
        get() = sensorsFlags.getBitValue(BITNUMBER_GAS)

    val ephhValveBit: Int
        get() = sensorsFlags.getBitValue(BITNUMBER_EPHH_VALVE)

    val epmValveBit: Int
        get() = sensorsFlags.getBitValue(BITNUMBER_EPM_VALVE)

    val coolFanBit: Int
        get() = sensorsFlags.getBitValue(BITNUMBER_COOL_FAN)

    val checkEngineBit: Int
        get() = sensorsFlags.getBitValue(BITNUMBER_CE_STATE)

    val carbBit: Int
        get() = sensorsFlags.getBitValue(BITNUMBER_CARB)

    val stBlockBit: Int
        get() = sensorsFlags.getBitValue(BITNUMBER_ST_BLOCK)




    companion object {

        internal const val DESCRIPTOR = 'q'

        fun parse(data: String) = SensorsPacket().apply {
            rpm = data.get2Bytes(2)
            map = data.get2Bytes(4).toFloat() / MAP_MULTIPLIER
            voltage = data.get2Bytes(6).toFloat() / VOLTAGE_MULTIPLIER

            temperature = data.get2Bytes(8).toShort().toFloat() / TEMPERATURE_MULTIPLIER
            currentAngle = data.get2Bytes(10).toFloat() / ANGLE_DIVIDER
            knockValue = data.get2Bytes(12).toFloat() / ADC_MULTIPLIER
            knockRetard = data.get2Bytes(14).toFloat() / ANGLE_DIVIDER
            airflow = data[16].toInt()

            sensorsFlags = data.get2Bytes(17)

            tps = data[19].toInt().toFloat() / TPS_MULTIPLIER

            addI1 = data.get2Bytes(20).toFloat() / VOLTAGE_MULTIPLIER
            addI2 = data.get2Bytes(22).toFloat() / VOLTAGE_MULTIPLIER

            ecuErrors = data.get4Bytes(24).toShort()
            chokePosition = data[28].toInt().toFloat() / CHOKE_MULTIPLIER
            gasDosePosition = data[29].toInt() / GAS_DOSE_MULTIPLIER

            rawSpeed = data.get2Bytes(30)
            rawDistance = data.get3Bytes(32)

            fuelInject = data.get2Bytes(35)

            data.get2Bytes(37).takeIf { it != 0x7FFF }?.let {
                airtempSensor = it.toShort().toFloat() / TEMPERATURE_MULTIPLIER
            }

            strtAalt = data.get2Bytes(39)
            idleAalt = data.get2Bytes(41)
            workAalt = data.get2Bytes(43)
            tempAalt = data.get2Bytes(45)
            airtAalt = data.get2Bytes(47)
            idlregAac = data.get2Bytes(49)
            octanAac = data.get2Bytes(51)

            lambda = data.get2Bytes(53)

            injPw = data.get2Bytes(55)

            tpsdot = data.get2Bytes(57)

            map2 = data.get2Bytes(59).toFloat() / MAP_MULTIPLIER
            tmp2 = data.get2Bytes(61).toShort().toFloat() / TEMPERATURE_MULTIPLIER


            afr = data.get2Bytes(63).toFloat() / AFR_MULTIPLIER
            load = data.get2Bytes(65)
            baroPress = data.get2Bytes(67)

            iit = data.get2Bytes(69).toShort()
            rigidArg = data[71].toInt()
            grts = data.get2Bytes(72)           // gas reducer's temperature
            rxlaf = data.get2Bytes(74)           // RxL air flow, note: it is divided by 32
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
    }
}
