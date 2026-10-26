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
import org.secu3.android.models.FnName
import org.secu3.android.models.packets.input.FnNameDatPacket
import org.secu3.android.models.packets.out.params.FunSetParamPacket

class FunctionsStateHolderTest {
    @Test
    fun ignitionOnlyMafCanChangeDisplacementAndOnlyItsSharedFields() {
        val sent = mutableListOf<FunSetParamPacket>()
        val holder = FunctionsStateHolder(sent::add)
        holder.onNamesReceived(FnNameDatPacket())
        val packet = FunSetParamPacket(ckpsEngineCyl = 4, injCylDisp = .5f, tpsCurveGradient = -2.5f)
        holder.onPacketReceived(packet, listOf("MAP", "MAP baro", "TPS", "MAP+TPS", "MAF"), emptyList(), emptyList())

        holder.updateChoice(FunctionsChoices.Selector.LOAD_SOURCE, 4)
        assertEquals(4, (holder.state as FunctionsUiState.Data).values.loadSource)
        assertEquals(2f, holder.engineDisplacement!!, 0f)
        holder.updateEngineDisplacement(3f)
        assertEquals(.75f, packet.injCylDisp, 0f)
        assertTrue(packet.mafload_const > 0)
        assertEquals(-2.5f, packet.tpsCurveGradient, 0f)
        assertEquals(2, sent.size)
        holder.updateEngineDisplacement(0f)
        assertEquals(2, sent.size)
    }

    @Test
    fun waitsForNamesBeforePublishingPacketDataAndMapsNamesToChoices() {
        val sent = mutableListOf<FunSetParamPacket>()
        val holder = FunctionsStateHolder(sent::add)
        val packet = FunSetParamPacket(fnGasoline = 1, fnGas = 0)

        holder.onPacketReceived(packet, loads, bars, ve2)
        assertEquals(FunctionsUiState.Loading, holder.state)

        holder.onNamesReceived(namesPacket("Alpha", "Beta"))
        holder.onPacketReceived(packet, loads, bars, ve2)
        assertTrue("mapping received packets must not echo them", sent.isEmpty())

        val data = holder.state as FunctionsUiState.Data
        assertEquals("Beta", data.choices.selected[FunctionsChoices.Selector.GASOLINE_MAP])
        assertEquals("Alpha", data.choices.selected[FunctionsChoices.Selector.GAS_MAP])
        assertEquals(listOf("Alpha", "Beta"), data.choices.options[FunctionsChoices.Selector.GASOLINE_MAP])
        assertTrue(sent.isEmpty())
    }

    @Test
    fun validChoiceSendsAndInvalidChoiceDoesNotSend() {
        val sent = mutableListOf<FunSetParamPacket>()
        val holder = loadedHolder(sent)
        val packet = FunSetParamPacket()
        holder.onPacketReceived(packet, loads, bars, ve2)

        holder.updateChoice(FunctionsChoices.Selector.GASOLINE_MAP, 1)
        holder.updateChoice(FunctionsChoices.Selector.GAS_MAP, 9)

        assertEquals(1, packet.fnGasoline)
        assertEquals(1, sent.size)
        assertTrue(sent.single() === packet)
        assertEquals("Beta", (holder.state as FunctionsUiState.Data).choices.selected[FunctionsChoices.Selector.GASOLINE_MAP])
    }

    @Test
    fun flagAndNumericEditsUpdateStateAndSendPacket() {
        val sent = mutableListOf<FunSetParamPacket>()
        val holder = loadedHolder(sent)
        val packet = FunSetParamPacket()
        holder.onPacketReceived(packet, loads, bars, ve2)

        holder.updateFlag(true)
        holder.updateValue(FunctionsNumber.LOWER, 12.5f)

        assertTrue(packet.useLoadGrid)
        assertEquals(12.5f, packet.loadLower)
        val values = (holder.state as FunctionsUiState.Data).values
        assertTrue(values.useLoadGrid)
        assertEquals(12.5f, values.value(FunctionsNumber.LOWER))
        assertEquals(2, sent.size)
        assertTrue(sent.all { it === packet })
    }

    @Test
    fun editsBeforeCompleteLoadDoNotSend() {
        val sent = mutableListOf<FunSetParamPacket>()
        val holder = FunctionsStateHolder(sent::add)

        holder.updateFlag(true)
        holder.updateValue(FunctionsNumber.LOWER, 9f)
        holder.updateChoice(FunctionsChoices.Selector.GAS_MAP, 0)

        assertFalse((holder.state as? FunctionsUiState.Data) != null)
        assertTrue(sent.isEmpty())
    }

    @Test
    fun resetReturnsToLoadingAndIgnoresEdits() {
        val sent = mutableListOf<FunSetParamPacket>()
        val holder = loadedHolder(sent)
        holder.onPacketReceived(FunSetParamPacket(), loads, bars, ve2)
        assertTrue(sent.isEmpty())

        holder.reset()
        holder.onNamesReceived(namesPacket("Alpha", "Beta"))
        holder.updateFlag(true)
        holder.updateValue(FunctionsNumber.LOWER, 9f)
        holder.updateChoice(FunctionsChoices.Selector.GAS_MAP, 0)

        assertEquals(FunctionsUiState.Loading, holder.state)
        assertTrue(sent.isEmpty())
    }

    private fun loadedHolder(sent: MutableList<FunSetParamPacket>) = FunctionsStateHolder(sent::add).apply {
        onNamesReceived(namesPacket("Alpha", "Beta"))
    }

    private fun namesPacket(vararg names: String) = FnNameDatPacket().apply {
        fnNameList = names.mapIndexed { index, name -> FnName(index, name) }.toMutableList()
    }

    private companion object {
        val loads = listOf("load0", "load1", "load2", "load3", "load4")
        val bars = listOf("bar0", "bar1", "bar2", "bar3")
        val ve2 = listOf("ve0", "ve1", "ve2")
    }
}
