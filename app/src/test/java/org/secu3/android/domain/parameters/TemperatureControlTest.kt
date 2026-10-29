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
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.secu3.android.models.packets.base.OutputPacket
import org.secu3.android.models.packets.input.FirmwareInfoPacket
import org.secu3.android.models.packets.out.params.TemperatureParamPacket

class TemperatureControlTest {
    @Test fun receiveIsPassivePublishesRawValuesAndResetClearsThem() {
        val sent = mutableListOf<OutputPacket>()
        val control = TemperatureControl({ null }) { sent += it; true }
        val packet = TemperatureParamPacket(
            tmpFlags = 0b1000_0101,
            ventOn = -900f,
            ventOff = 900f,
            ventPwmFrq = 0,
            condPvtOn = -2f,
            condPvtOff = 8f,
            condMinRpm = 0,
            ventTmr = 655.35f,
        )

        control.receive(packet)

        assertEquals(TemperatureValues(-900f, 900f, 0, -2f, 8f, 0, 655.35f, true, true, false), control.values.value)
        assertTrue(sent.isEmpty())
        control.reset()
        assertEquals(null, control.values.value)
        assertFalse(control.editFanTimer(30))
        assertTrue(sent.isEmpty())
    }

    @Test fun numericCommandsCoverAllFieldsAndKeepCallTimeSnapshotsImmutable() {
        val sent = mutableListOf<OutputPacket>()
        val control = TemperatureControl({ null }) { sent += it; true }
        val packet = parsePacket(
            flags = 0x80,
            on = 95f,
            off = 90f,
            frequency = 100,
            conditionerOn = 1.234f,
            conditionerOff = 2.345f,
            rpm = 900,
            timerWord = 127,
            tail = intArrayOf(0xCA, 0xFE),
        )
        control.receive(packet)

        assertTrue(control.editFanOn(100f))
        val firstSnapshot = sent.single().pack()
        assertArrayEquals(
            intArrayOf('j'.code, 0x80, 0x01, 0x90, 0x01, 0x68, 0x14, 0x7B, 0x01, 0xEE, 0x03, 0xAA, 0x03, 0x84, 0x00, 0x7F, 0xCA, 0xFE),
            firstSnapshot,
        )
        val original = packet

        assertTrue(control.editFanOff(95f))
        assertTrue(control.editPwmFrequency(270))
        assertTrue(control.editConditionerOn(3.21f))
        assertTrue(control.editConditionerOff(4.32f))
        assertTrue(control.editConditionerMinRpm(1200))
        assertTrue(control.editFanTimer(30))

        assertSame(original, packet)
        assertEquals(7, sent.size)
        assertNotSame(packet, sent.first())
        firstSnapshot[2] = 0
        assertArrayEquals(
            intArrayOf('j'.code, 0x80, 0x01, 0x90, 0x01, 0x68, 0x14, 0x7B, 0x01, 0xEE, 0x03, 0xAA, 0x03, 0x84, 0x00, 0x7F, 0xCA, 0xFE),
            sent.first().pack(),
        )
        assertEquals(100f, control.values.value?.fanOn)
        assertEquals(95f, control.values.value?.fanOff)
        assertEquals(270, control.values.value?.pwmFrequency)
        assertEquals(3.21f, control.values.value?.conditionerOn)
        assertEquals(4.32f, control.values.value?.conditionerOff)
        assertEquals(1200, control.values.value?.conditionerMinRpm)
        assertEquals(30f, control.values.value?.fanTimer)
        assertEquals(30f, packet.ventTmr)
        assertArrayEquals(intArrayOf(0xCA, 0xFE), sent.last().pack().takeLast(2).toIntArray())
    }

    @Test fun relayHysteresisAdjustsTheDependentThresholdAndPwmPreservesOnThreshold() {
        val sent = mutableListOf<OutputPacket>()
        val control = TemperatureControl({ null }) { sent += it; true }
        val packet = TemperatureParamPacket(ventOn = 80f, ventOff = 80f)
        control.receive(packet)

        assertTrue(control.editFanOn(80f))
        assertEquals(79.75f, packet.ventOff)
        assertTrue(control.editFanOff(80f))
        assertEquals(80.25f, packet.ventOn)
        assertEquals(2, sent.size)

        val pwmPacket = TemperatureParamPacket(tmpFlags = 0b1000_0000, ventOn = 80f, ventOff = 80f)
        control.receive(pwmPacket)
        assertTrue(control.setPwmControl(true))
        assertEquals(80f, pwmPacket.ventOn)
        assertEquals(79.75f, pwmPacket.ventOff)
        assertEquals(0b1000_0100, pwmPacket.tmpFlags)
        assertTrue(control.editFanOn(70f))
        assertEquals(70f, pwmPacket.ventOn)
        assertEquals(79.75f, pwmPacket.ventOff)
        assertTrue(control.editFanOff(80f))
        assertEquals(80.25f, pwmPacket.ventOn)
        assertEquals(80f, pwmPacket.ventOff)

        val disablingPwmPacket = TemperatureParamPacket(tmpFlags = 0x84, ventOn = 80f, ventOff = 81f)
        control.receive(disablingPwmPacket)
        assertTrue(control.setPwmControl(false))
        assertEquals(80f, disablingPwmPacket.ventOn)
        assertEquals(79.75f, disablingPwmPacket.ventOff)
        assertEquals(0x80, disablingPwmPacket.tmpFlags)
    }

    @Test fun exactHysteresisBoundaryNeedsNoCorrectionAndUnrelatedEditDoesNotNormalize() {
        val sent = mutableListOf<OutputPacket>()
        val control = TemperatureControl({ null }) { sent += it; true }
        val boundaryPacket = TemperatureParamPacket(ventOn = 80f, ventOff = 79.75f)
        control.receive(boundaryPacket)
        assertTrue(control.editFanOn(80f))
        assertEquals(79.75f, boundaryPacket.ventOff)

        val inconsistentPacket = TemperatureParamPacket(ventOn = 100f, ventOff = 105f)
        control.receive(inconsistentPacket)
        assertTrue(control.setCoolantSensor(true))
        assertEquals(100f, inconsistentPacket.ventOn)
        assertEquals(105f, inconsistentPacket.ventOff)
    }

    @Test fun outOfRangeAutomaticCorrectionsRejectWholeCommandAndRestorePacket() {
        val sent = mutableListOf<OutputPacket>()
        val control = TemperatureControl({ null }) { sent += it; true }
        val onEdge = TemperatureParamPacket(ventOn = -9f, ventOff = -9f)
        control.receive(onEdge)
        val onBefore = onEdge.pack()
        assertFalse(control.editFanOn(-10f))
        assertArrayEquals(onBefore, onEdge.pack())

        val offEdge = TemperatureParamPacket(ventOn = 125f, ventOff = 124.75f)
        control.receive(offEdge)
        val offBefore = offEdge.pack()
        assertFalse(control.editFanOff(125f))
        assertArrayEquals(offBefore, offEdge.pack())

        val pwmEdge = TemperatureParamPacket(tmpFlags = 0x80, ventOn = -10f, ventOff = -9f)
        control.receive(pwmEdge)
        val pwmBefore = pwmEdge.pack()
        assertFalse(control.setPwmControl(true))
        assertArrayEquals(pwmBefore, pwmEdge.pack())
        assertEquals(0x80, pwmEdge.tmpFlags)
        assertTrue(sent.isEmpty())
    }

    @Test fun currentFirmwareCapabilitiesGateEveryDependentCommand() {
        val sent = mutableListOf<OutputPacket>()
        val firmware = MutableStateFlow<FirmwareInfoPacket?>(FirmwareInfoPacket(options = 0))
        val control = TemperatureControl({ firmware.value }) { sent += it; true }
        control.receive(TemperatureParamPacket())

        assertFalse(control.setPwmControl(true))
        assertFalse(control.editPwmFrequency(270))
        assertFalse(control.setSensorCurveTable(true))
        assertFalse(control.editConditionerOn(1f))
        assertFalse(control.editConditionerOff(1f))
        assertFalse(control.editConditionerMinRpm(1200))
        assertTrue(sent.isEmpty())

        firmware.value = FirmwareInfoPacket(options = 1 shl 28)
        assertTrue(control.editConditionerOn(1f))
        assertTrue(control.editConditionerMinRpm(1200))
        assertFalse(control.setPwmControl(true))
        assertFalse(control.setSensorCurveTable(true))

        firmware.value = FirmwareInfoPacket(options = (1 shl 28) or (1 shl 8) or (1 shl 16))
        assertTrue(control.setPwmControl(true))
        assertTrue(control.editPwmFrequency(270))
        assertTrue(control.setSensorCurveTable(true))

        firmware.value = FirmwareInfoPacket(options = 1 shl 28)
        assertFalse(control.setPwmControl(false))
        assertFalse(control.editPwmFrequency(300))
        assertFalse(control.setSensorCurveTable(false))

        firmware.value = FirmwareInfoPacket(options = (1 shl 28) or (1 shl 17) or (1 shl 8) or (1 shl 16))
        assertFalse(control.editConditionerOn(2f))
        assertFalse(control.editConditionerOff(2f))
        assertTrue(control.editConditionerMinRpm(1500))
    }

    @Test fun invalidOrRefusedCommandsRestoreRawWordsFlagsAndTailInPlace() {
        val sent = mutableListOf<OutputPacket>()
        var admitted = false
        val control = TemperatureControl({ null }) { sent += it; admitted }
        val packet = parsePacket(
            flags = 0xA1,
            on = -.25f,
            off = -8f,
            frequency = 100,
            conditionerOn = 1.234f,
            conditionerOff = 2.345f,
            rpm = 900,
            timerWord = 127,
            tail = intArrayOf(0xCA, 0xFE),
        )
        control.receive(packet)
        val before = packet.pack()
        assertFalse(control.editConditionerOn(Float.NaN))
        assertFalse(control.editConditionerOn(6f))
        assertFalse(control.editPwmFrequency(9))
        assertFalse(control.editConditionerMinRpm(3001))
        assertFalse(control.editFanTimer(601))
        assertTrue(sent.isEmpty())

        assertFalse(control.setSensorCurveTable(true))
        assertArrayEquals(before, packet.pack())
        val attempted = before.copyOf().apply { this[1] = 0xA3 }
        assertArrayEquals(attempted, sent.single().pack())
        assertEquals(0xA1, packet.tmpFlags)

        admitted = true
        assertTrue(control.setCoolantSensor(true))
        assertArrayEquals(
            intArrayOf('j'.code, 0xA1, 0xFF, 0xFF, 0xFF, 0xE0, 0x14, 0x7B, 0x01, 0xEE, 0x03, 0xAA, 0x03, 0x84, 0x00, 0x7F, 0xCA, 0xFE),
            sent.last().pack(),
        )
    }

    private fun parsePacket(
        flags: Int,
        on: Float,
        off: Float,
        frequency: Int,
        conditionerOn: Float,
        conditionerOff: Float,
        rpm: Int,
        timerWord: Int,
        tail: IntArray,
    ): TemperatureParamPacket {
        val packet = TemperatureParamPacket(
            tmpFlags = flags,
            ventOn = on,
            ventOff = off,
            ventPwmFrq = frequency,
            condPvtOn = conditionerOn,
            condPvtOff = conditionerOff,
            condMinRpm = rpm,
            ventTmr = timerWord / 100f,
        )
        val packed = packet.pack()
        val payload = packed.drop(1).toIntArray()
        payload[13] = timerWord ushr 8
        payload[14] = timerWord and 0xFF
        return packet.parse(intArrayOf('@'.code, 'j'.code) + payload + tail) as TemperatureParamPacket
    }
}
