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
package org.secu3.android.ui.parameters.pages.compose

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.secu3.android.models.packets.out.params.LambdaParamPacket

class LambdaMappingTest {
    @Test fun valuesAndEditingMapEveryControlIncludingNegativeCoolantThreshold() {
        val p = LambdaParamPacket(strPerStp = 3, msPerStp = 120, stepSizeP = 1.25f, stepSizeM = 2f, corrLimitP = 30f,
            corrLimitM = 31f, swtPoint = .5f, tempThrd = -12.5f, rpmThrd = 800, activDelay = 45, deadBand = .1f,
            senstype = 1, gdStoichval = 15.6f, heatingTime0 = 15, heatingTime1 = 30, temperThrd = 70,
            heatingAct = .06f, aflowThrd = 320000f, lambdaSelectedChanel = 0x81, flags = 0b1101)
        val values = LambdaValues.from(p)
        assertEquals(3f, values.value(LambdaNumber.STROKES))
        assertEquals(120f, values.value(LambdaNumber.MS))
        assertEquals(1.25f, values.value(LambdaNumber.STEP_POS))
        assertEquals(2f, values.value(LambdaNumber.STEP_NEG))
        assertEquals(30f, values.value(LambdaNumber.LIMIT_POS))
        assertEquals(31f, values.value(LambdaNumber.LIMIT_NEG))
        assertEquals(.5f, values.value(LambdaNumber.SWITCH))
        assertEquals(-12.5f, values.value(LambdaNumber.CTS))
        assertEquals(800f, values.value(LambdaNumber.RPM))
        assertEquals(45f, values.value(LambdaNumber.DELAY))
        assertEquals(.1f, values.value(LambdaNumber.DEADBAND))
        assertEquals(15.6f, values.value(LambdaNumber.STOICH))
        assertEquals(15f, values.value(LambdaNumber.HEAT_COLD))
        assertEquals(30f, values.value(LambdaNumber.HEAT_HOT))
        assertEquals(70f, values.value(LambdaNumber.HEAT_TEMP))
        assertEquals(.06f, values.value(LambdaNumber.HEAT_PWM))
        assertEquals(320000f, values.value(LambdaNumber.AIR_FLOW))
        assertEquals(1, values.sensorType)
        assertEquals(true, values.flags[LambdaFlag.HEATING_VOLTAGE])
        assertEquals(false, values.flags[LambdaFlag.IDLING_CORRECTION])
        assertEquals(true, values.flags[LambdaFlag.BEFORE_CRANKING])
        assertEquals(true, values.flags[LambdaFlag.MIX_SENSORS])
        assertEquals(listOf(true, false, false, false, false, false, false, true), values.channels)
        applyLambdaNumber(p, LambdaNumber.CTS, -18.25f)
        assertEquals(-18.25f, p.tempThrd)
        applyLambdaNumber(p, LambdaNumber.HEAT_TEMP, 300f)
        assertEquals(255, p.temperThrd)
        applyLambdaNumber(p, LambdaNumber.MS, 129f)
        assertEquals(120, p.msPerStp)
    }

    @Test fun flagsAndChannelsPreserveOtherBitsAndIgnoreInvalidChannelIndex() {
        val p = LambdaParamPacket(flags = 0b1010, lambdaSelectedChanel = 0b1010_0001)
        applyLambdaFlag(p, LambdaFlag.HEATING_VOLTAGE, true)
        applyLambdaFlag(p, LambdaFlag.IDLING_CORRECTION, false)
        applyLambdaFlag(p, LambdaFlag.BEFORE_CRANKING, true)
        assertEquals(0b1101, p.flags)
        applyLambdaChannel(p, 3, true)
        applyLambdaChannel(p, 7, false)
        assertEquals(0b0010_1001, p.lambdaSelectedChanel)
        applyLambdaChannel(p, 8, true)
        assertEquals(0b0010_1001, p.lambdaSelectedChanel)
        assertTrue(p.determineLambdaHeatingByVoltage)
        assertFalse(p.lambdaCorrectionOnIdling)
        assertTrue(p.heatingBeforeCranking)
    }
}
