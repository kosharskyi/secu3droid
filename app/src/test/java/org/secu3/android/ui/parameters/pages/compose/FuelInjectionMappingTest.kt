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
import org.secu3.android.models.packets.out.params.InjctrParPacket

class FuelInjectionMappingTest {
    @Test fun configurationIdsAreFilteredByCylinderCount() {
        assertEquals(listOf(0, 1, 4), InjctrParPacket(ckpsEngineCyl = 1).validConfigurations())
        assertEquals(listOf(0, 1, 2, 3, 5, 4), InjctrParPacket(ckpsEngineCyl = 4).validConfigurations())
        assertEquals(listOf(0, 1, 2, 3, 5, 4), InjctrParPacket(ckpsEngineCyl = 6).validConfigurations())
    }

    @Test fun squirtsMatchDesktopRulesAndUnknownConfigHasNoChoices() {
        assertEquals(listOf(1, 2, 4), InjctrParPacket(ckpsEngineCyl = 4).validPulses(InjctrParPacket.InjConfig.SIMULTANEOUS.id))
        assertEquals(listOf(2, 4), InjctrParPacket(ckpsEngineCyl = 4).validPulses(InjctrParPacket.InjConfig.TWO_BANK_ALTERN.id))
        assertEquals(listOf(3, 6), InjctrParPacket(ckpsEngineCyl = 6).validPulses(InjctrParPacket.InjConfig.SEMISEQUENTIAL.id))
        assertEquals(listOf(8), InjctrParPacket(ckpsEngineCyl = 8).validPulses(InjctrParPacket.InjConfig.FULLSEQUENTIAL.id))
        assertTrue(InjctrParPacket(ckpsEngineCyl = 4).validPulses(9).isEmpty())
    }

    @Test fun configAndFlagMutationsPreserveOtherPackedBitsAndUnknownConfig() {
        val packet = InjctrParPacket(flags = 0x80, config = intArrayOf(0xA2, 0xB4), flowRate = floatArrayOf(300f, 300f), ckpsEngineCyl = 4)
        packet.useTimingMap = true
        assertEquals(0x81, packet.flags)
        assertTrue(packet.useTimingMap)
        assertFalse(packet.useTimingMapG)
        packet.config0 = InjctrParPacket.InjConfig.THROTTLEBODY.id
        assertEquals(0x02, packet.config[0])
        assertEquals(0xB4, packet.config[1])
        assertEquals(11, packet.config1) // unknown firmware code is retained
    }

    @Test fun invalidTransientPulseDoesNotOverwriteDerivedConstant() {
        val packet = InjctrParPacket(flowRate = floatArrayOf(300f, 300f), ckpsEngineCyl = 4, sdIglConst = intArrayOf(123, 456))
        packet.config0Pulses = 0
        assertEquals(123, packet.sdIglConst[0])
    }

    @Test fun derivedConstantRecalculatesFromValidEngineAndFlowInputs() {
        val packet = InjctrParPacket(flowRate = floatArrayOf(300f, 300f), ckpsEngineCyl = 4, sdIglConst = intArrayOf(0, 0))
        packet.config0Pulses = 2
        packet.engineDisp = 2f
        val first = packet.sdIglConst[0]
        assertTrue(first > 0)
        packet.flowRate[0] = 600f
        packet.recalculateDerivedConstants()
        assertTrue(packet.sdIglConst[0] in 1 until first)
        packet.engineDisp = 0f
        assertTrue(packet.sdIglConst[0] in 1 until first)
        packet.flowRate[0] = 50f
        packet.recalculateDerivedConstants()
        assertEquals(524288, packet.sdIglConst[0])
        assertEquals(103200 * 4, packet.injMafConst[0])
        assertEquals((((101.35 * 64 * 128 * 120) / (64 * .0012041)) / (.5 * 1000 * 4)).toInt(), packet.mafloadConst)
    }

    @Test fun packetMutationPreservesUnknownCodesAndUnhandledTail() {
        val packet = InjctrParPacket(flags = 0x80, config = intArrayOf(0xA2, 0xB4), ckpsEngineCyl = 4)
        val raw = packet.pack().drop(1).toIntArray() + intArrayOf(0xAA, 0x55)
        val incomingFrame = intArrayOf(0x40, 0x21) + raw
        incomingFrame[19] = packet.ckpsEngineCyl
        val parsed = InjctrParPacket().parse(incomingFrame) as InjctrParPacket
        parsed.useTimingMap = true
        assertEquals(10, parsed.config0)
        assertEquals(11, parsed.config1)
        assertTrue(parsed.pack().takeLast(2).toIntArray().contentEquals(intArrayOf(0xAA, 0x55)))
    }
}
