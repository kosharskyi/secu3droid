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
import org.junit.Test
import org.secu3.android.models.packets.out.params.StarterParamPacket

class StarterPacketMappingTest {
    @Test
    fun editorMetadataMatchesLegacyLimitsAndSpinSteps() {
        assertEquals(.01f, StarterNumber.CRANK_TO_RUN_COLD.step)
        assertEquals(.01f, StarterNumber.CRANK_TO_RUN_HOT.step)
        assertEquals(listOf(0f, 0f), listOf(StarterNumber.AFTERSTART_PETROL.min, StarterNumber.AFTERSTART_GAS.min))
        assertEquals(listOf(1020f, 1020f), listOf(StarterNumber.AFTERSTART_PETROL.max, StarterNumber.AFTERSTART_GAS.max))
        assertEquals(listOf(4f, 4f), listOf(StarterNumber.AFTERSTART_PETROL.step, StarterNumber.AFTERSTART_GAS.step))
    }

    @Test
    fun packetReadValuesPopulateEveryVisibleValueAndFlag() {
        val packet = StarterParamPacket(
            starterOff = 100,
            smapAbandon = 200,
            crankToRunTime = 1.25f,
            injAftstrStroke = 12,
            injPrimeCold = 3.4f,
            injPrimeHot = 4.5f,
            injPrimeDelay = 5.6f,
            injFloodclearTps = 7.8f,
            injAftStrokes1 = 13,
            stblStrCnt = 14,
            injCrankToRun_time1 = 2.5f,
        ).apply {
            allowStartOnClearFlood = true
            limitMaxInjPwOnCranking = false
        }
        val values = StarterValues.from(packet)

        assertEquals(100f, values.value(StarterNumber.STARTER_OFF))
        assertEquals(200f, values.value(StarterNumber.SMAP_ABANDON))
        assertEquals(1.25f, values.value(StarterNumber.CRANK_TO_RUN_COLD))
        assertEquals(2.5f, values.value(StarterNumber.CRANK_TO_RUN_HOT))
        assertEquals(12f, values.value(StarterNumber.AFTERSTART_PETROL))
        assertEquals(13f, values.value(StarterNumber.AFTERSTART_GAS))
        assertEquals(3.4f, values.value(StarterNumber.PRIME_COLD))
        assertEquals(4.5f, values.value(StarterNumber.PRIME_HOT))
        assertEquals(5.6f, values.value(StarterNumber.PRIME_DELAY))
        assertEquals(7.8f, values.value(StarterNumber.FLOOD_CLEAR_THRESHOLD))
        assertEquals(14f, values.value(StarterNumber.STBL_COUNTDOWN))
        assertEquals(true, values.flags.getValue(StarterFlag.ALLOW_FLOOD_CLEAR_START))
        assertEquals(false, values.flags.getValue(StarterFlag.LIMIT_CRANKING_PW))
    }

    @Test
    fun allStarterValuesMapToTheirPacketFieldsAndFlagsPreserveEachOther() {
        val packet = StarterParamPacket()
        StarterNumber.entries.forEach { applyStarterValue(packet, it, 23.5f + it.ordinal) }
        assertEquals(23, packet.starterOff)
        assertEquals(24, packet.smapAbandon)
        assertEquals(25.5f, packet.crankToRunTime)
        assertEquals(26.5f, packet.injCrankToRun_time1)
        assertEquals(27, packet.injAftstrStroke)
        assertEquals(28, packet.injAftStrokes1)
        assertEquals(29.5f, packet.injPrimeCold)
        assertEquals(30.5f, packet.injPrimeHot)
        assertEquals(31.5f, packet.injPrimeDelay)
        assertEquals(32.5f, packet.injFloodclearTps)
        assertEquals(33, packet.stblStrCnt)
        packet.allowStartOnClearFlood = true
        packet.limitMaxInjPwOnCranking = true
        packet.allowStartOnClearFlood = false
        assertEquals(false, packet.allowStartOnClearFlood)
        assertEquals(true, packet.limitMaxInjPwOnCranking)
    }
}
