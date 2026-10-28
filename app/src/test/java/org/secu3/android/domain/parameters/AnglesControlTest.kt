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
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.secu3.android.models.packets.base.OutputPacket
import org.secu3.android.models.packets.out.params.AnglesParamPacket

class AnglesControlTest {
    @Test fun receiveIsPassivePublishesRawFieldsAndResetClearsValues() {
        val sent = mutableListOf<OutputPacket>()
        val control = AnglesControl { sent += it; true }
        val packet = AnglesParamPacket(45f, -12f, -5.5f, 1.2f, 2.3f, 2, 0b101, -3.4f)

        control.receive(packet)

        assertEquals(AnglesValues(45f, -12f, -5.5f, 1.2f, 2.3f, 2, true, false, true, -3.4f), control.values.value)
        assertTrue(sent.isEmpty())
        control.reset()
        assertEquals(null, control.values.value)
        assertFalse(control.editMaxAngle(2f))
        assertTrue(sent.isEmpty())
    }

    @Test fun numericCommandsCoverAllFieldsAndSendImmutableSnapshots() {
        val sent = mutableListOf<OutputPacket>()
        val control = AnglesControl { sent += it; true }
        val packet = AnglesParamPacket().apply { maxAngle = 10f }
        control.receive(packet)
        val original = packet

        val edits = listOf<() -> Boolean>(
            { control.editMaxAngle(11.25f) },
            { control.editMinAngle(-12.5f) },
            { control.editAngleCorrection(-4.5f) },
            { control.editAngleDecSpeed(1.25f) },
            { control.editAngleIncSpeed(2.5f) },
            { control.editShiftIgnitionTiming(-3.75f) },
        )
        edits.forEach { assertTrue(it()) }

        assertSame(original, packet)
        assertEquals(6, sent.size)
        assertNotSame(packet, sent.first())
        val firstSnapshot = sent.first().pack()
        val expectedFirst = intArrayOf('m'.code, 0x01, 0x68, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
        assertArrayEquals(expectedFirst, firstSnapshot)
        firstSnapshot[1] = 0
        assertArrayEquals(expectedFirst, sent.first().pack())
        assertArrayEquals(
            intArrayOf('m'.code, 0x01, 0x68, 0xFE, 0x70, 0xFF, 0x70, 0, 0x28, 0, 0x50, 0, 0, 0xFF, 0x88),
            sent.last().pack(),
        )
        assertEquals(11.25f, control.values.value?.maxAngle)
        assertEquals(-12.5f, control.values.value?.minAngle)
        assertEquals(-4.5f, control.values.value?.angleCorrection)
        assertEquals(1.25f, control.values.value?.angleDecSpeed)
        assertEquals(2.5f, control.values.value?.angleIncSpeed)
        assertEquals(-3.75f, control.values.value?.shiftIgnitionTiming)
    }

    @Test fun explicitFlagCommandsApplyDesktopMutualExclusionAndPreserveOtherBits() {
        val sent = mutableListOf<OutputPacket>()
        val control = AnglesControl { sent += it; true }
        val packet = AnglesParamPacket(zeroAdvAngle = 2, igntimFlags = 0b1010_1100)
        control.receive(packet)

        assertTrue(control.setZeroAdvAngle(true))
        assertEquals(1, packet.zeroAdvAngle)
        assertEquals(0b1010_1000, packet.igntimFlags)
        assertTrue(control.setZeroAdvAngleWithCorr(true))
        assertEquals(0, packet.zeroAdvAngle)
        assertEquals(0b1010_1100, packet.igntimFlags)
        assertTrue(control.setZeroAdvAngle(false))
        assertEquals(0, packet.zeroAdvAngle)
        assertEquals(0b1010_1100, packet.igntimFlags)
        assertTrue(control.setZeroAdvAngleWithCorr(false))
        assertEquals(0, packet.zeroAdvAngle)
        assertEquals(0b1010_1000, packet.igntimFlags)
        assertTrue(control.setAlwaysUseIgnitionMap(true))
        assertEquals(0b1010_1001, packet.igntimFlags)
        assertTrue(control.setApplyManualTimingCorrOnIdl(true))
        assertEquals(0b1010_1011, packet.igntimFlags)
        assertEquals(6, sent.size)
    }

    @Test fun staleCorrectionAndInvalidEditorsAreRejectedWithoutSending() {
        val sent = mutableListOf<OutputPacket>()
        val control = AnglesControl { sent += it; true }
        control.receive(AnglesParamPacket(zeroAdvAngle = 2))

        assertFalse(control.editAngleCorrection(1f))
        assertFalse(control.editMaxAngle(Float.NaN))
        assertFalse(control.editMinAngle(61f))
        assertFalse(control.editAngleDecSpeed(-.1f))
        assertFalse(control.editAngleIncSpeed(10.1f))
        assertFalse(control.editShiftIgnitionTiming(56f))
        assertTrue(sent.isEmpty())
    }

    @Test fun refusalRestoresMutuallyExclusiveFieldsAndPreservesPackedTailSnapshot() {
        val sent = mutableListOf<OutputPacket>()
        val accepted = mutableListOf<Boolean>(false, true)
        val control = AnglesControl { packet -> sent += packet; accepted.removeAt(0) }
        val packet = AnglesParamPacket(12f, -8f, 3f, 2f, 1f, 2, 0b1010_1100, -4f)
        packet.parse(intArrayOf('@'.code, 'm'.code) + intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2, 0b1010_1100, 0, 0, 0xCA, 0xFE))
        control.receive(packet)
        val before = packet.pack()

        assertFalse(control.setZeroAdvAngle(true))
        assertArrayEquals(before, packet.pack())
        assertEquals(2, packet.zeroAdvAngle)
        assertTrue(packet.zeroAdvAngleWithCorr)
        val attempted = before.copyOf().apply {
            this[11] = 1
            this[12] = 0b1010_1000
        }
        assertArrayEquals(attempted, sent.first().pack())

        assertTrue(control.editAngleDecSpeed(1.5f))
        assertArrayEquals(intArrayOf('m'.code, 0, 0, 0, 0, 0, 0, 0, 48, 0, 0, 2, 0b1010_1100, 0, 0, 0xCA, 0xFE), sent.last().pack())
        assertArrayEquals(attempted, sent.first().pack())
    }

    @Test fun unrelatedEditRetainsIncomingSignedExtremeWordsAndUnknownFlags() {
        val sent = mutableListOf<OutputPacket>()
        val control = AnglesControl { sent += it; true }
        val packet = AnglesParamPacket()
        packet.parse(intArrayOf('@'.code, 'm'.code, 0x80, 0, 0x7F, 0xFF, 0x80, 0, 0x7F, 0xFF,
            0x80, 0, 0, 0xA8, 0x80, 0, 0xCA, 0xFE))
        control.receive(packet)

        assertTrue(control.setAlwaysUseIgnitionMap(true))

        assertArrayEquals(intArrayOf('m'.code, 0x80, 0, 0x7F, 0xFF, 0x80, 0, 0x7F, 0xFF,
            0x80, 0, 0, 0xA9, 0x80, 0, 0xCA, 0xFE), sent.single().pack())
    }
}
