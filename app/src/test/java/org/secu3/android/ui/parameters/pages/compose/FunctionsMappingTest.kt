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
import org.secu3.android.models.packets.out.params.FunSetParamPacket

class FunctionsMappingTest {
    @Test
    fun tpsGradientUsesDesktopSignedScaleAndPreservesManagerTail() {
        // Manager payload from io-core: TPS slope -102/40.96 and unknown extension CA FE.
        val managerPayload = IntArray(51).also {
            it[0] = 64; it[1] = 'n'.code
            it[18] = 0xFF; it[19] = 0x9A
            it[47] = 0x01; it[48] = 0x90 // APPS1 raw = 1 V
            it[49] = 0xCA; it[50] = 0xFE
        }
        val packet = FunSetParamPacket().parse(managerPayload) as FunSetParamPacket

        assertEquals(-102f / 40.96f, packet.tpsCurveGradient, 0.0001f)
        packet.tpsCurveGradient = -2.5f
        val outgoing = packet.pack()

        assertEquals(0xFF, outgoing[17])
        assertEquals(0x9A, outgoing[18])
        assertEquals(0xCA, outgoing[outgoing.lastIndex - 1])
        assertEquals(0xFE, outgoing.last())
    }

    @Test
    fun allFourteenFloatsAndLoadGridMapBothWays() {
        val packet = FunSetParamPacket(
            loadLower = 1f,
            loadUpper = 2f,
            mapCurveOffset = 3f,
            mapCurveGradient = 4f,
            tpsCurveOffset = 5f,
            tpsCurveGradient = 6f,
            map2CurveOffset = 7f,
            map2CurveGradient = 8f,
            gpsCurveOffset = 9f,
            gpsCurveGradient = 10f,
            fpsCurveOffset = 11f,
            fpsCurveGradient = 12f,
            apps1CurveOffset = 13f,
            apps1CurveGradient = 14f,
        )
        val expected = FunctionsNumber.entries.mapIndexed { index, number -> number to (index + 1).toFloat() }.toMap()
        assertEquals(expected, FunctionsValues.from(packet).numbers)
        assertEquals(14, FunctionsNumber.entries.size)
        FunctionsNumber.entries.forEachIndexed { i, n -> applyFunctionsValue(packet, n, 100f + i) }
        assertEquals(100f, packet.loadLower)
        assertEquals(101f, packet.loadUpper)
        assertEquals(102f, packet.mapCurveOffset)
        assertEquals(103f, packet.mapCurveGradient)
        assertEquals(104f, packet.tpsCurveOffset)
        assertEquals(105f, packet.tpsCurveGradient)
        assertEquals(106f, packet.map2CurveOffset)
        assertEquals(107f, packet.map2CurveGradient)
        assertEquals(108f, packet.gpsCurveOffset)
        assertEquals(109f, packet.gpsCurveGradient)
        assertEquals(110f, packet.fpsCurveOffset)
        assertEquals(111f, packet.fpsCurveGradient)
        assertEquals(112f, packet.apps1CurveOffset)
        assertEquals(113f, packet.apps1CurveGradient)
        assertEquals((100..113).map(Int::toFloat), FunctionsValues.from(packet).numbers.values.toList())
        applyFunctionsFlag(packet, true)
        assertTrue(packet.useLoadGrid)
        val expectedMetadata = listOf(
            listOf(.25f, 105f, .25f, 2f), listOf(.25f, 500f, .25f, 2f),
            listOf(-5f, 5f, .0025f, 4f), listOf(-150f, 150f, .01f, 3f),
            listOf(-5f, 5f, .0025f, 4f), listOf(-100f, 100f, .01f, 3f),
            listOf(-5f, 5f, .0025f, 4f), listOf(-150f, 150f, .01f, 3f),
            listOf(-5f, 5f, .0025f, 4f), listOf(-150f, 150f, .01f, 3f),
            listOf(-5f, 5f, .0025f, 4f), listOf(-500f, 500f, .01f, 3f),
            listOf(-5f, 5f, .0025f, 4f), listOf(-500f, 500f, .01f, 3f),
        )
        FunctionsNumber.entries.forEachIndexed { index, number ->
            val metadata = expectedMetadata[index]
            assertEquals(metadata[0], number.min)
            assertEquals(metadata[1], number.max)
            assertEquals(metadata[2], number.step)
            assertEquals(metadata[3].toInt(), number.precision)
            assertTrue(number.titleRes != 0)
            assertTrue(number.unitRes != 0)
        }
    }

    @Test
    fun loadAxisUnitsAndLowerUpperEnableRulesFollowSourceAndGridMode() {
        assertEquals(org.secu3.android.R.string.units_pressure_kpa, functionsLoadAxisUnitRes(0))
        assertEquals(org.secu3.android.R.string.units_pressure_kpa, functionsLoadAxisUnitRes(1))
        assertEquals(org.secu3.android.R.string.units_percents, functionsLoadAxisUnitRes(2))
        assertEquals(org.secu3.android.R.string.units_percents, functionsLoadAxisUnitRes(4))

        val default = FunctionsValues(emptyMap(), useLoadGrid = false, loadSource = 0)
        assertTrue(functionsNumberEnabled(FunctionsNumber.LOWER, default))
        assertTrue(functionsNumberEnabled(FunctionsNumber.UPPER, default))
        assertFalse(functionsNumberEnabled(FunctionsNumber.UPPER, default.copy(loadSource = 1)))
        assertFalse(functionsNumberEnabled(FunctionsNumber.LOWER, default.copy(useLoadGrid = true)))
        assertFalse(functionsNumberEnabled(FunctionsNumber.UPPER, default.copy(useLoadGrid = true)))
        assertTrue(functionsNumberEnabled(FunctionsNumber.MAP_OFFSET, default.copy(useLoadGrid = true)))
    }

    @Test
    fun selectorOptionsMapCodesAndInvalidIncomingValuesStayUnselected() {
        val names = listOf("A", "B", "C")
        val choices = FunctionsChoices.from(
            FunSetParamPacket(
                fnGasoline = 2,
                fnGas = 1,
                loadSrcCfg = 4,
                mapselUni = 0x6F,
                barocorrType = 3,
                ve2MapFunc = 2,
                gasVUni = 42,
            ),
            names,
            listOf("l0", "l1", "l2", "l3", "l4"),
            listOf("b0", "b1", "b2", "b3"),
            listOf("v0", "v1", "v2"),
        )

        assertEquals("C", choices.selected[FunctionsChoices.Selector.GASOLINE_MAP])
        assertEquals("B", choices.selected[FunctionsChoices.Selector.GAS_MAP])
        assertEquals("l4", choices.selected[FunctionsChoices.Selector.LOAD_SOURCE])
        assertEquals("no", choices.selected[FunctionsChoices.Selector.MAPSEL_PETROL])
        assertEquals("6", choices.selected[FunctionsChoices.Selector.MAPSEL_GAS])
        assertEquals("b3", choices.selected[FunctionsChoices.Selector.BAROCORR])
        assertEquals("v2", choices.selected[FunctionsChoices.Selector.VE2])
        assertNull(choices.selected[FunctionsChoices.Selector.GAS_V_UNI])
    }

    @Test
    fun eachSelectorWritesCorrectProtocolFieldPreservingSiblingMapNibble() {
        FunctionsChoices.Selector.entries.forEach { selector ->
            val packet = FunSetParamPacket(mapselUni = 0xA5)
            assertTrue(applyFunctionsChoice(packet, selector, 1, listOf("a", "b", "c")))
            when (selector) {
                FunctionsChoices.Selector.GASOLINE_MAP -> assertEquals(1, packet.fnGasoline)
                FunctionsChoices.Selector.GAS_MAP -> assertEquals(1, packet.fnGas)
                FunctionsChoices.Selector.LOAD_SOURCE -> assertEquals(1, packet.loadSrcCfg)
                FunctionsChoices.Selector.MAPSEL_PETROL -> {
                    assertEquals(2, packet.mapselUniPetrol)
                    assertEquals(10, packet.mapselUniGas)
                }
                FunctionsChoices.Selector.MAPSEL_GAS -> {
                    assertEquals(5, packet.mapselUniPetrol)
                    assertEquals(2, packet.mapselUniGas)
                }
                FunctionsChoices.Selector.BAROCORR -> assertEquals(1, packet.barocorrType)
                FunctionsChoices.Selector.VE2 -> assertEquals(1, packet.ve2MapFunc)
                FunctionsChoices.Selector.GAS_V_UNI -> assertEquals(2, packet.gasVUni)
            }
        }

        FunctionsChoices.mapselCodes.forEachIndexed { index, code ->
            val packet = FunSetParamPacket(mapselUni = 0xA5)
            assertTrue(applyFunctionsChoice(packet, FunctionsChoices.Selector.MAPSEL_PETROL, index, emptyList()))
            assertEquals(code, packet.mapselUniPetrol)
            assertEquals(10, packet.mapselUniGas)
            assertTrue(applyFunctionsChoice(packet, FunctionsChoices.Selector.MAPSEL_GAS, index, emptyList()))
            assertEquals(code, packet.mapselUniGas)

            assertTrue(applyFunctionsChoice(packet, FunctionsChoices.Selector.GAS_V_UNI, index, emptyList()))
            assertEquals(code, packet.gasVUni)
        }

        val unchanged = FunSetParamPacket(
            fnGasoline = 2,
            fnGas = 1,
            loadSrcCfg = 3,
            mapselUni = 0xA5,
            barocorrType = 2,
            ve2MapFunc = 1,
            gasVUni = 4,
        )
        val original = unchanged.copy()
        FunctionsChoices.Selector.entries.forEach { selector ->
            assertFalse(applyFunctionsChoice(unchanged, selector, 99, emptyList()))
            assertEquals(original, unchanged)
        }
    }
}
