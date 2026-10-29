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

import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.secu3.android.models.packets.base.OutputPacket
import org.secu3.android.models.packets.input.FirmwareInfoPacket
import org.secu3.android.models.packets.out.params.CarburParamPacket

class CarburControlTest {
    @Test fun receiveIsPassiveKeepsRawValuesAndResetClearsState() {
        val sent = mutableListOf<OutputPacket>()
        val control = CarburControl({ null }) { sent += it; true }
        val packet = parsePacket(
            idleLow = 30001, idleHigh = 1200, inverse = 2, valvePressure = -512f,
            idleLowGas = 1100, idleHighGas = 1300, cutoffDelay = 2.55f,
            tps = 31.5f, map = 251f, cts = -10.5f, revLow = 5000, revHigh = 5500,
            fuelOutput = 41, ignitionOutput = 42,
        )

        control.receive(packet)

        assertEquals(CarburValues(30001, 1200, 1100, 1300, 2.55f, -512f, 31.5f, 251f, -10.5f, 5000, 5500, true, 41, 42), control.values.value)
        assertTrue(sent.isEmpty())
        control.reset()
        assertEquals(null, control.values.value)
        assertFalse(control.editIdleLowRpm(1250))
        assertTrue(sent.isEmpty())
    }

    @Test fun numericCommandsCoverAllFieldsAndSendImmutableCallTimeSnapshots() {
        val sent = mutableListOf<OutputPacket>()
        val control = CarburControl({ null }) { sent += it; true }
        val packet = CarburParamPacket()
        control.receive(packet)

        val edits = listOf<() -> Boolean>(
            { control.editIdleLowRpm(251) },
            { control.editIdleHighRpm(252) },
            { control.editIdleLowGasRpm(253) },
            { control.editIdleHighGasRpm(254) },
            { control.editCutoffDelay(.5f) },
            { control.editValvePressure(1.5f) },
            { control.editTpsThreshold(25f) },
            { control.editMapThreshold(20f) },
            { control.editCoolantThreshold(-10.5f) },
            { control.editRevLimitLowRpm(501) },
            { control.editRevLimitHighRpm(502) },
        )
        edits.forEach { assertTrue(it()) }

        assertEquals(11, sent.size)
        assertNotSame(packet, sent.first())
        val first = intArrayOf('k'.code, 0, 251) + IntArray(22)
        assertArrayEquals(first, sent.first().pack())
        val firstCopy = sent.first().pack()
        firstCopy[1] = 0
        assertArrayEquals(first, sent.first().pack())
        assertArrayEquals(
            intArrayOf('k'.code, 0, 251, 0, 252, 0, 0, 96, 0, 253, 0, 254, 50, 6, 64, 5, 0, 0xFF, 0xD6, 1, 0xF5, 1, 0xF6, 0, 0),
            sent.last().pack(),
        )
        assertEquals(CarburValues(251, 252, 253, 254, .5f, 1.5f, 25f, 20f, -10.5f, 501, 502, false, 0, 0), control.values.value)
    }

    @Test fun selectorCodesMatchDesktopAndUnknownValuesStayRawUntilExplicitSelection() {
        val sent = mutableListOf<OutputPacket>()
        val control = CarburControl({ null }) { sent += it; true }
        val packet = CarburParamPacket(fuelcut_uni = 6, igncut_uni = 41)
        control.receive(packet)

        assertEquals(listOf(0, 1, 2, 3, 4, 5, 15), control.selectorCodes)
        assertEquals(6, control.values.value?.fuelCutOutput)
        assertEquals(41, control.values.value?.ignitionCutOutput)
        assertFalse(control.selectFuelCutOutput(7))
        assertFalse(control.selectFuelCutOutput(-1))
        assertEquals(6, packet.fuelcut_uni)
        assertEquals(41, packet.igncut_uni)
        assertTrue(control.selectFuelCutOutput(0))
        assertEquals(0, packet.fuelcut_uni)
        assertEquals(41, packet.igncut_uni)
        assertEquals(0, sent.last().pack()[23])
        assertTrue(control.selectFuelCutOutput(5))
        assertEquals(5, packet.fuelcut_uni)
        assertEquals(5, sent.last().pack()[23])
        assertTrue(control.selectIgnitionCutOutput(0))
        assertEquals(0, packet.igncut_uni)
        assertEquals(0, sent.last().pack()[24])
        assertTrue(control.selectIgnitionCutOutput(5))
        assertEquals(5, packet.igncut_uni)
        assertEquals(5, sent.last().pack()[24])
        assertTrue(control.selectFuelCutOutput(6))
        assertEquals(15, packet.fuelcut_uni)
        assertEquals(15, sent.last().pack()[23])
        assertTrue(control.selectIgnitionCutOutput(6))
        assertEquals(15, packet.igncut_uni)
        assertEquals(15, sent.last().pack()[24])
    }

    @Test fun rawInverseByteSurvivesUnrelatedEditsUntilExplicitToggle() {
        val sent = mutableListOf<OutputPacket>()
        val control = CarburControl({ null }) { sent += it; true }
        val packet = CarburParamPacket(carbInvers = 2, fuelcut_uni = 41, igncut_uni = 42)
        control.receive(packet)

        assertEquals(true, control.values.value?.inverted)
        assertTrue(control.editIdleLowRpm(1200))
        assertEquals(2, packet.carbInvers)
        assertEquals(41, packet.fuelcut_uni)
        assertEquals(42, packet.igncut_uni)
        assertTrue(control.setInverted(false))
        assertEquals(0, packet.carbInvers)
        assertEquals(41, packet.fuelcut_uni)
        assertEquals(42, packet.igncut_uni)
    }

    @Test fun invalidCommandsAreRejectedWithoutQuantizingOrSending() {
        val sent = mutableListOf<OutputPacket>()
        val control = CarburControl({ null }) { sent += it; true }
        control.receive(CarburParamPacket())

        assertFalse(control.editIdleLowRpm(249))
        assertFalse(control.editIdleHighRpm(30001))
        assertFalse(control.editIdleLowGasRpm(249))
        assertFalse(control.editIdleHighGasRpm(30001))
        assertFalse(control.editCutoffDelay(Float.NaN))
        assertFalse(control.editCutoffDelay(2.51f))
        assertFalse(control.editValvePressure(-.1f))
        assertFalse(control.editValvePressure(500.1f))
        assertFalse(control.editTpsThreshold(100.1f))
        assertFalse(control.editMapThreshold(250.1f))
        assertFalse(control.editCoolantThreshold(-40.1f))
        assertFalse(control.editRevLimitLowRpm(499))
        assertFalse(control.editRevLimitHighRpm(30001))
        assertTrue(sent.isEmpty())

        assertTrue(control.editIdleLowRpm(251))
        assertEquals(251, control.values.value?.idleLowRpm)
        assertEquals(0x00, sent.single().pack()[1])
        assertEquals(0xFB, sent.single().pack()[2])
    }

    @Test fun currentCapabilitiesGateMapAndFuelCutAndRevLimitsIndependently() {
        val sent = mutableListOf<OutputPacket>()
        val firmware = MutableStateFlow<FirmwareInfoPacket?>(FirmwareInfoPacket(options = 0))
        val control = CarburControl({ firmware.value }) { sent += it; true }
        control.receive(CarburParamPacket())

        assertFalse(control.editMapThreshold(20f))
        assertFalse(control.editCoolantThreshold(15f))
        assertFalse(control.selectFuelCutOutput(0))
        assertFalse(control.editRevLimitLowRpm(1000))
        assertFalse(control.editRevLimitHighRpm(2000))
        assertTrue(control.selectIgnitionCutOutput(0))
        assertTrue(control.editValvePressure(6.25f))
        assertEquals(2, sent.size)

        firmware.value = FirmwareInfoPacket(options = 1 shl 29)
        assertFalse(control.editMapThreshold(20f))
        assertFalse(control.selectFuelCutOutput(5))
        assertTrue(control.editRevLimitLowRpm(1000))
        assertTrue(control.editRevLimitHighRpm(2000))

        firmware.value = FirmwareInfoPacket(options = 1 shl 28)
        assertTrue(control.editMapThreshold(20f))
        assertTrue(control.editCoolantThreshold(15f))
        assertTrue(control.selectFuelCutOutput(5))
        assertTrue(control.editRevLimitLowRpm(1000))

        firmware.value = FirmwareInfoPacket(options = (1 shl 28) or (1 shl 8))
        assertTrue(control.editMapThreshold(21f))
        assertTrue(control.selectIgnitionCutOutput(4))
        firmware.value = FirmwareInfoPacket(options = 0)
        assertFalse(control.editMapThreshold(22f))
        assertFalse(control.selectFuelCutOutput(4))
    }

    @Test fun refusalRestoresMutatedFieldsAndPreservesSignedWordsUnknownSelectorsAndTail() {
        val sent = mutableListOf<OutputPacket>()
        val control = CarburControl({ null }) { sent += it; false }
        val packet = parsePacket(
            idleLow = 1000, idleHigh = 1200, inverse = 2, valvePressure = -512f,
            idleLowGas = 1100, idleHighGas = 1300, cutoffDelay = .55f,
            tps = 31.5f, map = 88.5f, cts = -10.5f, revLow = 5000, revHigh = 5500,
            fuelOutput = 41, ignitionOutput = 42, tail = intArrayOf(0xCA, 0xFE),
        )
        control.receive(packet)
        val before = packet.pack()
        val valuesBefore = control.values.value

        assertFalse(control.setInverted(false))
        assertEquals(2, packet.carbInvers)
        assertEquals(1000, packet.ieLot)
        assertArrayEquals(before, packet.pack())
        val attempted = before.copyOf().apply { this[5] = 0 }
        assertArrayEquals(attempted, sent.single().pack())
        assertEquals(valuesBefore, control.values.value)
        assertEquals(41, packet.fuelcut_uni)
        assertEquals(42, packet.igncut_uni)
        assertEquals(intArrayOf(0xCA, 0xFE).toList(), packet.pack().takeLast(2))
    }

    private fun parsePacket(
        idleLow: Int,
        idleHigh: Int,
        inverse: Int,
        valvePressure: Float,
        idleLowGas: Int,
        idleHighGas: Int,
        cutoffDelay: Float,
        tps: Float,
        map: Float,
        cts: Float,
        revLow: Int,
        revHigh: Int,
        fuelOutput: Int,
        ignitionOutput: Int,
        tail: IntArray = intArrayOf(),
    ): CarburParamPacket {
        val packed = CarburParamPacket(
            ieLot = idleLow, ieHit = idleHigh, carbInvers = inverse,
            feOnThresholds = valvePressure, ieLotG = idleLowGas, ieHitG = idleHighGas,
            shutoffDelay = cutoffDelay, tpsThreshold = tps, fuelcutMapThrd = map,
            fuelcutCtsThrd = cts, revlimLot = revLow, revlimHit = revHigh,
            fuelcut_uni = fuelOutput, igncut_uni = ignitionOutput,
        ).pack()
        return CarburParamPacket().parse(intArrayOf('@'.code, 'k'.code) + packed.drop(1).toIntArray() + tail) as CarburParamPacket
    }
}
