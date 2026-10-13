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

import org.junit.Assert.*
import org.junit.Test
import org.secu3.android.models.packets.out.params.InjctrParPacket

class FuelInjectionStateHolderTest {
    private val sends = mutableListOf<InjctrParPacket>()
    private val gates = mutableListOf<Boolean>()
    private val holder = FuelInjectionStateHolder(sends::add, gates::add)

    private fun packet() = InjctrParPacket(
        flags = 0x80, config = intArrayOf(0x11, 0x11), flowRate = floatArrayOf(300f, 400f),
        sdIglConst = intArrayOf(123, 456), ckpsEngineCyl = 4, timing = intArrayOf(10, 20),
        timingCrk = intArrayOf(30, 40), injMaxPw = floatArrayOf(10f, 11f),
        injPwDecSpeed = 1f, injPwIncSpeed = 2f,
    ).also { it.engineDisp = 2f }

    @Test fun noPreloadSendResetAndReceiveNeverEchoPacket() {
        holder.changeAngleSpec(0, 1); holder.changeFlag(FuelInjectionFlag.AIR_DENSITY, true)
        assertTrue(sends.isEmpty())
        val p = packet()
        holder.receive(p)
        assertTrue(sends.isEmpty())
        assertEquals(listOf(false, true), gates)
        assertTrue(holder.isSendAllowed)
        holder.reset()
        assertEquals(FuelInjectionUiState.Loading, holder.uiState)
        assertFalse(holder.isSendAllowed)
        assertEquals(false, gates.last())
        holder.edit(FuelInjectionNumber.FLOW_0, 600f)
        assertTrue(sends.isEmpty())
    }

    @Test fun everyNumberEditCategoryUpdatesPacketValuesAndSendsOnce() {
        val p = packet(); holder.receive(p); sends.clear()
        val edits = listOf(
            FuelInjectionNumber.ENGINE_DISPLACEMENT to 3f,
            FuelInjectionNumber.FLOW_0 to 301f, FuelInjectionNumber.FLOW_1 to 401f,
            FuelInjectionNumber.TIMING_0 to 11f, FuelInjectionNumber.TIMING_1 to 21f,
            FuelInjectionNumber.CRANKING_0 to 31f, FuelInjectionNumber.CRANKING_1 to 41f,
            FuelInjectionNumber.MIN_PW_0 to .8f, FuelInjectionNumber.MIN_PW_1 to .9f,
            FuelInjectionNumber.MAX_PW_0 to 12f, FuelInjectionNumber.MAX_PW_1 to 13f,
            FuelInjectionNumber.PULSES_PER_LITER to 10000f,
            FuelInjectionNumber.DECREASE_RATE to 3f, FuelInjectionNumber.INCREASE_RATE to 4f,
        )
        edits.forEach { (n, v) -> holder.edit(n, v) }
        assertEquals(edits.size, sends.size)
        assertEquals(3f, p.engineDisp, .0001f)
        assertArrayEquals(floatArrayOf(301f, 401f), p.flowRate, .001f)
        assertArrayEquals(intArrayOf(11, 21), p.timing)
        assertArrayEquals(intArrayOf(31, 41), p.timingCrk)
        assertEquals(10000, p.fffConst)
        assertEquals(3f, p.injPwDecSpeed, .001f); assertEquals(4f, p.injPwIncSpeed, .001f)
        val values = (holder.uiState as FuelInjectionUiState.Data).values
        assertEquals(3f, values.engineDisplacement, .001f)
        assertEquals(401f, values.banks[1].flow, .001f)
    }

    @Test fun flowEditsRecalculateBothDerivedBankConstantsAndSyntheticLoad() {
        val p = packet(); holder.receive(p); sends.clear()
        holder.edit(FuelInjectionNumber.FLOW_0, 600f)
        holder.edit(FuelInjectionNumber.FLOW_1, 500f)
        val expected = packet().also {
            it.flowRate[0] = 600f
            it.flowRate[1] = 500f
            it.recalculateDerivedConstants()
        }
        assertArrayEquals(expected.sdIglConst, p.sdIglConst)
        assertArrayEquals(expected.injMafConst, p.injMafConst)
        assertEquals(expected.mafloadConst, p.mafloadConst)
        assertEquals(2, sends.size)
    }

    @Test fun bankSelectionsValidateIndicesAndChoicesAndChoosePulsesBeforeConfigCalculation() {
        val p = packet(); holder.receive(p); sends.clear()
        // Bank zero: old pulse 1 is invalid for two-bank alternating, so use first valid pulse 2.
        val before0 = p.sdIglConst[0]
        holder.changeConfig(0, 2)
        assertEquals(2, p.config0Pulses)
        assertEquals(2, p.config0)
        assertTrue(before0 > 0)
        val expected = InjctrParPacket(config = intArrayOf(0x22, 0x11), flowRate = floatArrayOf(300f, 400f), ckpsEngineCyl = 4).also { it.engineDisp = 2f }
        assertEquals(expected.sdIglConst[0], p.sdIglConst[0])
        // Bank one: old pulse 1 is invalid for full sequential, so use its only valid pulse 4.
        holder.changeConfig(1, 4)
        assertEquals(4, p.config1Pulses)
        assertEquals(4, p.config1)
        holder.changePulses(0, 4); holder.changePulses(1, 4)
        assertEquals(4, p.config0Pulses); assertEquals(4, p.config1Pulses)
        holder.changeAngleSpec(0, 2); holder.changeAngleSpec(1, 1)
        assertEquals(0x12, p.angleSpec)
        val count = sends.size
        holder.changeConfig(-1, 0); holder.changeConfig(2, 0)
        holder.changeConfig(0, 99)
        holder.changePulses(-1, 1); holder.changePulses(2, 1)
        holder.changePulses(0, 3); holder.changePulses(1, 3)
        holder.changeAngleSpec(-1, 0); holder.changeAngleSpec(2, 0)
        holder.changeAngleSpec(0, 3); holder.changeAngleSpec(1, -1)
        assertEquals(count, sends.size)
    }

    @Test fun eachFlagMapsExactlyAndPreservesUnknownFlagBits() {
        val p = packet(); holder.receive(p); sends.clear()
        FuelInjectionFlag.entries.forEach { holder.changeFlag(it, true) }
        assertEquals(0xFF, p.flags)
        assertEquals(FuelInjectionFlag.entries.size, sends.size)
        FuelInjectionFlag.entries.forEach { holder.changeFlag(it, false) }
        assertEquals(0x80, p.flags)
    }

    @Test fun firmwareUpdatesPresentationWithoutMutatingOrSendingAndSurvivesPacketReceive() {
        val p = packet()
        holder.updateFirmware(injectorEnabled = false, secu3t = true)
        holder.receive(p)
        val data = holder.uiState as FuelInjectionUiState.Data
        assertFalse(data.injectorEnabled); assertTrue(data.secu3t)
        assertTrue(sends.isEmpty())
        holder.updateFirmware(injectorEnabled = true, secu3t = false)
        val updated = holder.uiState as FuelInjectionUiState.Data
        assertTrue(updated.injectorEnabled); assertFalse(updated.secu3t)
        assertTrue(sends.isEmpty())
    }

    @Test fun editMetadataRetainsDialogParametersAndPacketRoundTripPreservesUnknownData() {
        holder.receive(packet())
        val spec = holder.editSpec(FuelInjectionNumber.FLOW_1)!!
        assertEquals(.02f, spec.step); assertEquals(50f, spec.min); assertEquals(1023f, spec.max)
        assertEquals(2, spec.precision); assertFalse(spec.integer)
        val raw = packet().also { it.config[0] = 0xA2; it.config[1] = 0xB4 }
        val parsed = InjctrParPacket().parse(intArrayOf(0x40, 0x21) + raw.pack().drop(1).toIntArray() + intArrayOf(0xAA, 0x55)) as InjctrParPacket
        holder.receive(parsed); holder.changeFlag(FuelInjectionFlag.USE_MAP_0, true)
        assertEquals(10, parsed.config0); assertEquals(11, parsed.config1)
        assertEquals(0x81, parsed.flags)
        assertArrayEquals(intArrayOf(0xAA, 0x55), parsed.pack().takeLast(2).toIntArray())
    }
}
