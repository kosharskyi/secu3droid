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
import org.junit.Assert.assertTrue
import org.junit.Test
import org.secu3.android.models.packets.base.OutputPacket
import org.secu3.android.models.packets.input.FirmwareInfoPacket
import org.secu3.android.models.packets.out.params.DbwParamPacket

class DbwControlTest {
    @Test
    fun receiveIsPassiveEveryNamedEditMapsToItsFieldAndKeepsHomePosition() {
        val writes = mutableListOf<IntArray>()
        val snapshots = mutableListOf<OutputPacket>()
        val control = DbwControl({ null }, { packet -> snapshots += packet; writes += packet.pack(); true })
        val packet = DbwParamPacket(.1f, .2f, .3f, 40f, 50f, .12f, 6f, 7f, 8f, 9f, 43.21f)

        control.receive(packet)
        assertEquals(0, writes.size)
        assertEquals(DbwValues(.1f, .2f, .3f, 40f, 50f, .12f, 6f, 7f, 8f, 9f, 43.21f), control.values.value)

        val edits: List<Pair<(Float) -> Boolean, Float>> = listOf(
            control::editEtcP to 1.125f,
            control::editEtcI to 1.25f,
            control::editEtcD to 1.375f,
            control::editNegativeMaxDuty to 45.5f,
            control::editPositiveMaxDuty to 55.5f,
            control::editPidPeriod to .15f,
            control::editFrictionTorqueOpen to 6.4f,
            control::editFrictionTorqueClose to 7.5f,
            control::editFrictionThreshold to 12f,
            control::editFrictionIdleAdditionMax to 10.5f,
        )
        val expectedValues = listOf(
            DbwValues(1.125f, .2f, .3f, 40f, 50f, .12f, 6f, 7f, 8f, 9f, 43.21f),
            DbwValues(1.125f, 1.25f, .3f, 40f, 50f, .12f, 6f, 7f, 8f, 9f, 43.21f),
            DbwValues(1.125f, 1.25f, 1.375f, 40f, 50f, .12f, 6f, 7f, 8f, 9f, 43.21f),
            DbwValues(1.125f, 1.25f, 1.375f, 45.5f, 50f, .12f, 6f, 7f, 8f, 9f, 43.21f),
            DbwValues(1.125f, 1.25f, 1.375f, 45.5f, 55.5f, .12f, 6f, 7f, 8f, 9f, 43.21f),
            DbwValues(1.125f, 1.25f, 1.375f, 45.5f, 55.5f, .15f, 6f, 7f, 8f, 9f, 43.21f),
            DbwValues(1.125f, 1.25f, 1.375f, 45.5f, 55.5f, .15f, 6.4f, 7f, 8f, 9f, 43.21f),
            DbwValues(1.125f, 1.25f, 1.375f, 45.5f, 55.5f, .15f, 6.4f, 7.5f, 8f, 9f, 43.21f),
            DbwValues(1.125f, 1.25f, 1.375f, 45.5f, 55.5f, .15f, 6.4f, 7.5f, 12f, 9f, 43.21f),
            DbwValues(1.125f, 1.25f, 1.375f, 45.5f, 55.5f, .15f, 6.4f, 7.5f, 12f, 10.5f, 43.21f),
        )
        edits.forEachIndexed { index, (edit, value) ->
            val before = control.values.value
            assertTrue(edit(value))
            assertEquals(index + 1, writes.size)
            assertEquals(value, listOf(
                packet.etc_p, packet.etc_i, packet.etc_d, packet.etc_nmax_duty,
                packet.etc_pmax_duty, packet.pid_period, packet.frictorq_open,
                packet.frictorq_close, packet.frictorq_thrd, packet.frictorq_idleadd_max,
            )[index])
            assertEquals(43.21f, control.values.value?.homePosition)
            assertEquals(43.21f, packet.homePosition_edited)
            assertTrue(before !== control.values.value)
            assertEquals(expectedValues[index], control.values.value)
        }
        assertEquals(DbwValues(1.125f, 1.25f, 1.375f, 45.5f, 55.5f, .15f, 6.4f, 7.5f, 12f, 10.5f, 43.21f), control.values.value)
        assertArrayEquals(
            DbwParamPacket(1.125f, .2f, .3f, 40f, 50f, .12f, 6f, 7f, 8f, 9f, 43.21f).pack(),
            snapshots.first().pack(),
        )
    }

    @Test
    fun fixedPointEncodingMatchesDesktopAndOmitsReadOnlyHomePosition() {
        val writes = mutableListOf<IntArray>()
        val control = DbwControl({ null }, { packet -> writes += packet.pack(); true })
        control.receive(DbwParamPacket(.125f, .25f, .5f, 45.5f, 50f, .12f, 6.25f, 7.5f, 8f, 9.5f, 77f))
        assertTrue(control.editEtcP(.125f))
        assertArrayEquals(
            intArrayOf('P'.code, 0x02, 0x00, 0x04, 0x00, 0x08, 0x00, 91, 100, 12, 100, 120, 8, 0x02, 0x60),
            writes.last(),
        )
        assertEquals(15, writes.last().size) // descriptor plus 14 editable bytes
    }

    @Test
    fun receivesSixteenPayloadBytesAndPreservesUnknownTailAfterEdits() {
        val homeWord = 47 * 64 + 32
        val tail = intArrayOf(0xA5, 0x5A)
        val original = DbwParamPacket(.125f, .25f, .5f, 45f, 50f, .12f, 6.25f, 7.5f, 8f, 9.5f, 0f)
        val wire = intArrayOf('@'.code) + original.pack() + intArrayOf(homeWord shr 8, homeWord and 0xff) + tail
        assertEquals(20, wire.size) // prefix + descriptor + 14 write bytes + home position + tail
        val received = DbwParamPacket().parse(wire) as DbwParamPacket
        assertEquals(47.5f, received.homePosition_edited, 0f)

        var sent: OutputPacket? = null
        val control = DbwControl({ null }, { sent = it; true })
        control.receive(received)
        assertTrue(control.editEtcI(.375f))
        val repacked = requireNotNull(sent).pack()
        assertEquals(tail.toList(), repacked.takeLast(tail.size))
        assertEquals(47.5f, received.homePosition_edited, 0f)
        assertEquals(15, repacked.size - tail.size)
    }

    @Test
    fun invalidValuesUnsupportedFirmwareAndSessionRefusalDoNotChangeVisibleState() {
        var firmware: FirmwareInfoPacket? = null
        var accept = true
        val writes = mutableListOf<IntArray>()
        val packet = DbwParamPacket(1f, homePosition_edited = 12.5f)
        val control = DbwControl({ firmware }, { output -> writes += output.pack(); accept })
        control.receive(packet)
        val initial = control.values.value

        assertFalse(control.editEtcP(Float.NaN))
        assertFalse(control.editEtcP(Float.POSITIVE_INFINITY))
        assertFalse(control.editEtcP(-.001f))
        assertFalse(control.editEtcP(5.001f))
        assertEquals(0, writes.size)

        firmware = FirmwareInfoPacket(options = 1 shl 17) // SECU3T has no DBW controls.
        assertFalse(control.editEtcP(2f))
        assertEquals(0, writes.size)
        assertEquals(initial, control.values.value)

        firmware = FirmwareInfoPacket(options = 0)
        accept = false
        assertFalse(control.editEtcP(2f))
        assertEquals(1, writes.size)
        assertEquals(1f, packet.etc_p)
        assertEquals(initial, control.values.value)

        accept = true
        assertTrue(control.editEtcP(2f))
        firmware = FirmwareInfoPacket(options = 1 shl 17)
        assertFalse(control.editEtcI(3f))
        assertEquals(2f, control.values.value?.etcP)
        assertEquals(12.5f, control.values.value?.homePosition)

        control.reset()
        assertEquals(null, control.values.value)
        assertFalse(control.editEtcP(3f))
    }

    @Test
    fun everyEditorRejectsNonFiniteAndOutOfRangeValues() {
        var sends = 0
        val control = DbwControl({ null }, { sends++; true })
        control.receive(DbwParamPacket())
        val edits = listOf(
            Triple(control::editEtcP, 0f, 5f),
            Triple(control::editEtcI, 0f, 5f),
            Triple(control::editEtcD, 0f, 5f),
            Triple(control::editNegativeMaxDuty, 0f, 100f),
            Triple(control::editPositiveMaxDuty, 0f, 100f),
            Triple(control::editPidPeriod, 0f, .2f),
            Triple(control::editFrictionTorqueOpen, 0f, 15.9f),
            Triple(control::editFrictionTorqueClose, 0f, 15.9f),
            Triple(control::editFrictionThreshold, 0f, 25f),
            Triple(control::editFrictionIdleAdditionMax, 0f, 15f),
        )
        edits.forEach { (edit, min, max) ->
            assertFalse(edit(Float.NaN))
            assertFalse(edit(Float.NEGATIVE_INFINITY))
            assertFalse(edit(Float.POSITIVE_INFINITY))
            assertFalse(edit(min - .001f))
            assertFalse(edit(max + .001f))
        }
        assertEquals(0, sends)
        assertEquals(DbwValues(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f), control.values.value)
    }

    @Test
    fun unrelatedEditKeepsReceivedValuesOutsideEditorBounds() {
        var sent: OutputPacket? = null
        val control = DbwControl({ null }, { sent = it; true })
        control.receive(DbwParamPacket(etc_p = 6.25f, pid_period = .3f))

        assertTrue(control.editEtcI(1f))

        assertEquals(6.25f, control.values.value?.etcP)
        assertEquals(.3f, control.values.value?.pidPeriod)
        assertArrayEquals(DbwParamPacket(etc_p = 6.25f, etc_i = 1f, pid_period = .3f).pack(), requireNotNull(sent).pack())
    }
}
