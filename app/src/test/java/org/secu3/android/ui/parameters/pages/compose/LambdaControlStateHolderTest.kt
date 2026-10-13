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
import org.junit.Assert.assertTrue
import org.junit.Test
import org.secu3.android.models.packets.out.params.LambdaParamPacket

class LambdaControlStateHolderTest {
    @Test fun editsApplyEveryNumericMappingAndSendOncePerEdit() {
        val packet = LambdaParamPacket()
        val sent = mutableListOf<LambdaParamPacket>()
        val holder = LambdaControlStateHolder(sent::add)
        holder.onPacketReceived(packet)
        sent.clear()

        LambdaNumber.entries.forEach { number -> holder.updateValue(number, number.max + 100f) }

        assertEquals(LambdaNumber.entries.size, sent.size)
        LambdaNumber.entries.forEach { number ->
            assertEquals(number.max, holder.values!!.value(number))
        }
        assertEquals(255, packet.strPerStp)
        assertEquals(2500, packet.msPerStp)
        assertEquals(45f, packet.stepSizeP)
        assertEquals(45f, packet.stepSizeM)
        assertEquals(100f, packet.corrLimitP)
        assertEquals(100f, packet.corrLimitM)
        assertEquals(5f, packet.swtPoint)
        assertEquals(300f, packet.tempThrd)
        assertEquals(30000, packet.rpmThrd)
        assertEquals(255, packet.activDelay)
        assertEquals(5f, packet.deadBand)
        assertEquals(22f, packet.gdStoichval)
        assertEquals(255, packet.heatingTime0)
        assertEquals(255, packet.heatingTime1)
        assertEquals(255, packet.temperThrd)
        assertEquals(2.55f, packet.heatingAct)
        assertEquals(2_000_000f, packet.aflowThrd)
    }

    @Test fun msValueReflectsPacketTruncationAndNumericClipping() {
        val packet = LambdaParamPacket(msPerStp = 120)
        val holder = LambdaControlStateHolder({})
        holder.onPacketReceived(packet)
        holder.updateValue(LambdaNumber.MS, 129f)
        assertEquals(120, packet.msPerStp)
        assertEquals(120f, holder.values!!.value(LambdaNumber.MS))
        holder.updateValue(LambdaNumber.MS, -10f)
        assertEquals(0, packet.msPerStp)
        assertEquals(0f, holder.values!!.value(LambdaNumber.MS))
    }

    @Test fun receiveAndPreloadEditsDoNotSendAndMappingGateWrapsReceive() {
        val packet = LambdaParamPacket(senstype = 7)
        val sends = mutableListOf<LambdaParamPacket>()
        val gate = mutableListOf<Boolean>()
        val holder = LambdaControlStateHolder(sends::add, { gate += false }, { gate += true })
        holder.updateFlag(LambdaFlag.BEFORE_CRANKING, true)
        holder.updateChannel(0, true)
        holder.updateSensorType(0, 2)
        holder.updateValue(LambdaNumber.CTS, 1f)
        assertTrue(sends.isEmpty())
        assertEquals(null, holder.values)

        holder.onPacketReceived(packet)
        assertTrue(sends.isEmpty())
        assertEquals(listOf(false, true), gate)
        assertEquals(7, holder.values!!.sensorType)
    }

    @Test fun resetClearsPacketAndIgnoresEditsWithoutEchoingReceivedPacket() {
        val sends = mutableListOf<LambdaParamPacket>()
        val holder = LambdaControlStateHolder(sends::add)
        holder.onPacketReceived(LambdaParamPacket())
        assertTrue(sends.isEmpty())

        holder.reset()
        holder.updateFlag(LambdaFlag.BEFORE_CRANKING, true)
        holder.updateChannel(0, true)
        holder.updateSensorType(0, 2)
        holder.updateValue(LambdaNumber.CTS, 1f)

        assertEquals(null, holder.values)
        assertTrue(sends.isEmpty())
    }

    @Test fun flagsSensorsAndChannelsSendOnceAndInvalidIndicesAreIgnored() {
        val packet = LambdaParamPacket(flags = 0b1010, senstype = 0, lambdaSelectedChanel = 0b1010_0001)
        val sends = mutableListOf<LambdaParamPacket>()
        val holder = LambdaControlStateHolder(sends::add)
        holder.onPacketReceived(packet)
        holder.updateFlag(LambdaFlag.HEATING_VOLTAGE, true)
        holder.updateSensorType(1, 2)
        holder.updateChannel(3, true)
        holder.updateChannel(7, false)
        assertEquals(4, sends.size)
        assertEquals(0b1011, packet.flags)
        assertTrue(packet.determineLambdaHeatingByVoltage)
        assertEquals(1, packet.senstype)
        assertEquals(0b0010_1001, packet.lambdaSelectedChanel)
        assertEquals(1, holder.values!!.sensorType)
        assertEquals(listOf(true, false, false, true, false, true, false, false), holder.values!!.channels)

        holder.updateSensorType(-1, 2)
        holder.updateSensorType(2, 2)
        holder.updateChannel(-1, true)
        holder.updateChannel(8, true)
        assertEquals(4, sends.size)
        assertEquals(1, packet.senstype)
        assertTrue(holder.values!!.channels[0])
    }
}
