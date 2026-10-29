/*
 *    SecuDroid  - An open source, free manager for SECU-3 engine control unit
 *    Copyright (C) 2026 Vitalii O. Kosharskyi. Ukraine, Kyiv
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 */
package org.secu3.android.domain.parameters

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.secu3.android.models.FnName
import org.secu3.android.models.packets.input.FirmwareInfoPacket
import org.secu3.android.models.packets.input.FnNameDatPacket
import org.secu3.android.models.packets.out.params.FunSetParamPacket

class FunctionsControlTest {
    @Test
    fun receiveIsPassiveAndAllFourteenNamedEditsUpdateOnlyTheirField() {
        var sends = 0
        val control = FunctionsControl({ null }) { sends++; true }
        val packet = FunSetParamPacket(
            loadLower = 1f, loadUpper = 2f,
            mapCurveOffset = 3f, mapCurveGradient = 4f,
            tpsCurveOffset = 5f, tpsCurveGradient = 6f,
            map2CurveOffset = 7f, map2CurveGradient = 8f,
            gpsCurveOffset = 9f, gpsCurveGradient = 10f,
            fpsCurveOffset = 11f, fpsCurveGradient = 12f,
            apps1CurveOffset = 13f, apps1CurveGradient = 14f,
        )
        control.receive(parameters(packet))
        assertEquals(0, sends)

        val edits: List<Pair<(Float) -> Boolean, Float>> = listOf(
            control::editLoadLower to 20.5f,
            control::editLoadUpper to 100.25f,
            control::editMapCurveOffset to -1.25f,
            control::editMapCurveGradient to -100f,
            control::editTpsCurveOffset to .5f,
            control::editTpsCurveGradient to -10f,
            control::editMap2CurveOffset to 1.5f,
            control::editMap2CurveGradient to 75f,
            control::editGpsCurveOffset to -2f,
            control::editGpsCurveGradient to 125f,
            control::editFpsCurveOffset to 2f,
            control::editFpsCurveGradient to 300f,
            control::editApps1CurveOffset to -3f,
            control::editApps1CurveGradient to -400f,
        )
        val expected = listOf(
            listOf(20.5f, 2f, 3f, 4f, 5f, 6f, 7f, 8f, 9f, 10f, 11f, 12f, 13f, 14f),
            listOf(20.5f, 100.25f, 3f, 4f, 5f, 6f, 7f, 8f, 9f, 10f, 11f, 12f, 13f, 14f),
            listOf(20.5f, 100.25f, -1.25f, 4f, 5f, 6f, 7f, 8f, 9f, 10f, 11f, 12f, 13f, 14f),
            listOf(20.5f, 100.25f, -1.25f, -100f, 5f, 6f, 7f, 8f, 9f, 10f, 11f, 12f, 13f, 14f),
            listOf(20.5f, 100.25f, -1.25f, -100f, .5f, 6f, 7f, 8f, 9f, 10f, 11f, 12f, 13f, 14f),
            listOf(20.5f, 100.25f, -1.25f, -100f, .5f, -10f, 7f, 8f, 9f, 10f, 11f, 12f, 13f, 14f),
            listOf(20.5f, 100.25f, -1.25f, -100f, .5f, -10f, 1.5f, 8f, 9f, 10f, 11f, 12f, 13f, 14f),
            listOf(20.5f, 100.25f, -1.25f, -100f, .5f, -10f, 1.5f, 75f, 9f, 10f, 11f, 12f, 13f, 14f),
            listOf(20.5f, 100.25f, -1.25f, -100f, .5f, -10f, 1.5f, 75f, -2f, 10f, 11f, 12f, 13f, 14f),
            listOf(20.5f, 100.25f, -1.25f, -100f, .5f, -10f, 1.5f, 75f, -2f, 125f, 11f, 12f, 13f, 14f),
            listOf(20.5f, 100.25f, -1.25f, -100f, .5f, -10f, 1.5f, 75f, -2f, 125f, 2f, 12f, 13f, 14f),
            listOf(20.5f, 100.25f, -1.25f, -100f, .5f, -10f, 1.5f, 75f, -2f, 125f, 2f, 300f, 13f, 14f),
            listOf(20.5f, 100.25f, -1.25f, -100f, .5f, -10f, 1.5f, 75f, -2f, 125f, 2f, 300f, -3f, 14f),
            listOf(20.5f, 100.25f, -1.25f, -100f, .5f, -10f, 1.5f, 75f, -2f, 125f, 2f, 300f, -3f, -400f),
        )
        edits.forEachIndexed { index, (edit, value) ->
            assertTrue(edit(value))
            assertEquals(index + 1, sends)
            val actual = control.values.value!!
            assertEquals(expected[index], listOf(
                actual.loadLower, actual.loadUpper, actual.mapCurveOffset, actual.mapCurveGradient,
                actual.tpsCurveOffset, actual.tpsCurveGradient, actual.map2CurveOffset,
                actual.map2CurveGradient, actual.gpsCurveOffset, actual.gpsCurveGradient,
                actual.fpsCurveOffset, actual.fpsCurveGradient, actual.apps1CurveOffset,
                actual.apps1CurveGradient,
            ))
        }
    }

    @Test
    fun resetClearsLoadedStateAndRejectsAllCommandsWithoutSending() {
        var sends = 0
        val control = FunctionsControl({ null }) { sends++; true }
        control.receive(parameters(FunSetParamPacket()))
        control.reset()
        assertNull(control.values.value)
        assertFalse(control.editMapCurveOffset(1f))
        assertFalse(control.setUseLoadGrid(true))
        assertFalse(control.selectGasolineMap(0))
        assertFalse(control.selectGasMap(0))
        assertFalse(control.selectLoadSource(0))
        assertFalse(control.selectMapsForPetrol(0))
        assertFalse(control.selectMapsForGas(0))
        assertFalse(control.selectBarometricCorrection(0))
        assertFalse(control.selectVe2Function(0))
        assertFalse(control.selectGasVoltageInput(0))
        assertFalse(control.updateMafEngineDisplacement(1f))
        assertEquals(0, sends)
    }

    @Test
    fun rejectedAndInvalidEditsDoNotMutateOrSendAndSnapshotsAreImmutable() {
        var accept = true
        val outputs = mutableListOf<org.secu3.android.models.packets.base.OutputPacket>()
        val control = FunctionsControl({ null }) { outputs += it; accept }
        val packet = FunSetParamPacket(mapCurveOffset = 1f, funcFlags = 0xFE, gasVUni = 6)
        control.receive(parameters(packet))

        assertFalse(control.editMapCurveOffset(Float.NaN))
        assertFalse(control.editMapCurveOffset(Float.POSITIVE_INFINITY))
        assertFalse(control.editMapCurveOffset(-5.01f))
        assertFalse(control.editMapCurveOffset(5.01f))
        assertTrue(outputs.isEmpty())

        assertTrue(control.editMapCurveOffset(2f))
        val firstFrame = outputs.single().pack()
        assertEquals(6, firstFrame[24]) // Unknown incoming raw code is preserved, not normalized to 15.
        accept = false
        assertFalse(control.editMapCurveOffset(3f))
        assertEquals(2f, packet.mapCurveOffset)
        assertEquals(2f, control.values.value?.mapCurveOffset)
        accept = true
        assertTrue(control.setUseLoadGrid(false))
        assertArrayEquals(firstFrame, outputs.first().pack())
        assertEquals(0xFE, packet.funcFlags)
    }

    @Test
    fun selectorsUseRawProtocolCodesAndPreserveSiblingMapNibble() {
        val frames = mutableListOf<IntArray>()
        val control = FunctionsControl({ null }) { frames += it.pack(); true }
        val packet = FunSetParamPacket(mapselUni = 0xA5)
        control.receive(parameters(packet))
        assertEquals(10, packet.mapselUniGas)

        for (code in 0..5) {
            packet.mapselUni = 0xA5
            assertTrue(control.selectMapsForPetrol(code))
            assertEquals(code, packet.mapselUniPetrol)
            assertEquals("mapsel raw=0x${packet.mapselUni.toString(16)}", 10, packet.mapselUniGas)
            assertTrue(control.selectMapsForGas(code))
            assertEquals(code, packet.mapselUniGas)
            assertEquals(code, packet.mapselUniPetrol)
            assertTrue(control.selectGasVoltageInput(code))
            assertEquals(code, packet.gasVUni)
        }
        assertTrue(control.selectMapsForPetrol(15))
        assertTrue(control.selectMapsForGas(15))
        assertTrue(control.selectGasVoltageInput(15))
        assertEquals(15, packet.mapselUniPetrol)
        assertEquals(15, packet.mapselUniGas)
        assertEquals(15, packet.gasVUni)
        assertFalse(control.selectMapsForPetrol(6))
        assertFalse(control.selectMapsForGas(-1))
        assertFalse(control.selectGasVoltageInput(6))
        assertFalse(control.selectGasolineMap(-1))
        assertFalse(control.selectGasolineMap(2))
        assertFalse(control.selectGasMap(-1))
        assertFalse(control.selectLoadSource(5))
        assertFalse(control.selectBarometricCorrection(4))
        assertEquals(15, packet.mapselUniPetrol)
        assertEquals(15, packet.mapselUniGas)
        assertEquals(15, packet.gasVUni)
        assertEquals(21, frames.size)
    }

    @Test
    fun allNumericBoundsAndLiveCapabilitiesRejectWithoutSending() {
        var firmware: FirmwareInfoPacket? = null
        var sends = 0
        val control = FunctionsControl({ firmware }) { sends++; true }
        control.receive(parameters(FunSetParamPacket()))
        val invalidFloats = listOf(Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY)
        val cases = listOf(
            control::editLoadLower to listOf(.249f, 105.001f),
            control::editLoadUpper to listOf(.249f, 500.001f),
            control::editMapCurveOffset to listOf(-5.001f, 5.001f),
            control::editMapCurveGradient to listOf(-150.001f, 150.001f),
            control::editTpsCurveOffset to listOf(-5.001f, 5.001f),
            control::editTpsCurveGradient to listOf(-100.001f, 100.001f),
            control::editMap2CurveOffset to listOf(-5.001f, 5.001f),
            control::editMap2CurveGradient to listOf(-150.001f, 150.001f),
            control::editGpsCurveOffset to listOf(-5.001f, 5.001f),
            control::editGpsCurveGradient to listOf(-150.001f, 150.001f),
            control::editFpsCurveOffset to listOf(-5.001f, 5.001f),
            control::editFpsCurveGradient to listOf(-500.001f, 500.001f),
            control::editApps1CurveOffset to listOf(-5.001f, 5.001f),
            control::editApps1CurveGradient to listOf(-500.001f, 500.001f),
        ).map { (edit, bounds) -> edit to (bounds + invalidFloats) }
        cases.flatMap { (edit, values) -> values.map { edit to it } }
            .forEach { (edit, value) -> assertFalse("$value", edit(value)) }
        assertEquals(0, sends)

        val packet = FunSetParamPacket(loadSrcCfg = 0)
        control.receive(parameters(packet))
        firmware = FirmwareInfoPacket(options = 1 shl 17) // SECU-3T disables GPS/FPS/APPS.
        listOf(
            control::editGpsCurveOffset,
            control::editGpsCurveGradient,
            control::editFpsCurveOffset,
            control::editFpsCurveGradient,
            control::editApps1CurveOffset,
            control::editApps1CurveGradient,
        ).forEach { assertFalse(it(1f)) }
        assertFalse(control.selectVe2Function(1)) // FI is disabled.
        firmware = null // Unknown firmware remains permissive.
        assertTrue(control.editGpsCurveOffset(1f))
        assertTrue(control.selectVe2Function(1))
        assertEquals(2, sends)

        val gated = FunSetParamPacket(loadSrcCfg = 1).apply { useLoadGrid = true }
        control.receive(parameters(gated))
        assertFalse(control.editLoadLower(2f))
        assertFalse(control.editLoadUpper(2f))
        assertEquals(2, sends)
        assertTrue(control.setUseLoadGrid(false))
        assertFalse(control.editLoadUpper(2f)) // TPS remains a live upper-load gate.
        assertEquals(3, sends)
    }

    @Test
    fun parsedReplyPreservesReadOnlyTelemetryAndUnknownTailWhenEditing() {
        val actualReply = receiveFixture(FunSetParamPacket(), 0xFEDC, 0xABCD, intArrayOf(0xCA, 0xFE))
        val parsed = FunSetParamPacket().parse(actualReply) as FunSetParamPacket
        val frames = mutableListOf<IntArray>()
        val control = FunctionsControl({ null }) { frames += it.pack(); true }
        control.receive(parameters(parsed))
        assertTrue(control.editMapCurveOffset(1f))
        val command = frames.single()
        assertEquals(46, command.size)
        assertNotEquals("Read-only TPS telemetry is omitted", 0xFE, command[32])
        assertNotEquals("Read-only APPS telemetry is omitted", 0xAB, command.last())
        assertArrayEquals(intArrayOf(0xCA, 0xFE), command.takeLast(2).toIntArray())
    }

    @Test
    fun mafRefusalRollsBackBothDependentFields() {
        val control = FunctionsControl({ FirmwareInfoPacket(options = 0) }) { false }
        val packet = FunSetParamPacket(loadSrcCfg = 4, ckpsEngineCyl = 4, injCylDisp = .5f, mafload_const = 123f)
        control.receive(parameters(packet))
        assertFalse(control.updateMafEngineDisplacement(3f))
        assertEquals(.5f, packet.injCylDisp, 0f)
        assertEquals(123f, packet.mafload_const, 0f)
        assertEquals(2f, requireNotNull(control.engineDisplacementForMafEditor()), 0f)
    }

    @Test
    fun incomingUnknownSelectorsStayUnselectedAndGasVoltageRawSixPacksUnchanged() {
        val packet = FunSetParamPacket(fnGasoline = 9, loadSrcCfg = 9, mapselUni = 0x66, gasVUni = 6)
        val parsed = FunSetParamPacket().parse(receiveFixture(packet)) as FunSetParamPacket
        assertEquals(6, parsed.gasVUni)
        assertEquals(6, parsed.pack()[24])
    }

    @Test
    fun mafEditorFollowsLiveLoadSourceAndFirmwareAndMatchesDesktopWireValues() {
        var firmware: FirmwareInfoPacket? = FirmwareInfoPacket(options = 0)
        val frames = mutableListOf<IntArray>()
        val control = FunctionsControl({ firmware }) { frames += it.pack(); true }
        val packet = FunSetParamPacket(loadSrcCfg = 4, ckpsEngineCyl = 4, injCylDisp = .5f)
        control.receive(parameters(packet))
        assertEquals(2f, requireNotNull(control.engineDisplacementForMafEditor()), 0f)
        for (invalid in listOf(.009f, 8.001f, Float.NaN, Float.POSITIVE_INFINITY)) {
            assertFalse(control.updateMafEngineDisplacement(invalid))
        }
        assertTrue(frames.isEmpty())

        assertTrue(control.updateMafEngineDisplacement(3f))
        assertEquals(.75f, packet.injCylDisp, 0f)
        assertEquals(430954.25f, packet.mafload_const, .01f)
        assertArrayEquals(intArrayOf(0x30, 0x00), frames.single().sliceArray(26..27))
        assertArrayEquals(intArrayOf(0x00, 0x06, 0x93, 0x6A), frames.single().sliceArray(28..31))

        assertTrue(control.selectLoadSource(0))
        assertNull(control.engineDisplacementForMafEditor())
        assertFalse(control.updateMafEngineDisplacement(2f))
        assertTrue(control.selectLoadSource(4))
        firmware = FirmwareInfoPacket(options = 1 shl 28)
        assertNull(control.engineDisplacementForMafEditor())
        assertFalse(control.updateMafEngineDisplacement(2f))

        firmware = FirmwareInfoPacket(options = 0)
        assertTrue(control.selectLoadSource(4))
        val noCylinders = FunSetParamPacket(loadSrcCfg = 4, ckpsEngineCyl = 0, injCylDisp = 1f)
        control.receive(parameters(noCylinders))
        assertNull(control.engineDisplacementForMafEditor())
        assertFalse(control.updateMafEngineDisplacement(2f))
    }

    @Test
    fun twoLiterMafConstantMatchesDesktopCalculation() {
        val control = FunctionsControl({ FirmwareInfoPacket(options = 0) }) { true }
        val packet = FunSetParamPacket(loadSrcCfg = 4, ckpsEngineCyl = 4, injCylDisp = .5f)
        control.receive(parameters(packet))
        assertTrue(control.updateMafEngineDisplacement(2f))
        assertEquals(646431.375f, packet.mafload_const, .01f)
        assertArrayEquals(intArrayOf(0x00, 0x09, 0xDD, 0x1F), packet.pack().sliceArray(28..31))
    }

    private fun parameters(packet: FunSetParamPacket): FunctionParameters = FunctionParameters(
        names = FnNameDatPacket().apply {
            tablesNumber = 2
            fnNameList = mutableListOf(FnName(0, "Alpha"), FnName(1, "Beta"))
        },
        settings = packet,
    )

    private fun receiveFixture(
        packet: FunSetParamPacket,
        tpsRaw: Int = 0x0190,
        appsRaw: Int = 400,
        tail: IntArray = intArrayOf(),
    ): IntArray {
        val command = packet.pack()
        return intArrayOf('@'.code) + command.copyOfRange(0, 32) +
            intArrayOf(tpsRaw ushr 8 and 0xFF, tpsRaw and 0xFF) +
            command.copyOfRange(32, command.size) +
            intArrayOf(appsRaw ushr 8 and 0xFF, appsRaw and 0xFF) + tail
    }
}
