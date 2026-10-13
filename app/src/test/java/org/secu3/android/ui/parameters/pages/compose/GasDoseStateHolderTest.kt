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
import org.secu3.android.models.packets.base.OutputPacket
import org.secu3.android.models.packets.out.params.GasDoseParamPacket

class GasDoseStateHolderTest {
    @Test fun editsSendZeroPlaceholderSnapshotsAndFrequencyIsValidated() {
        val f = Fixture()
        f.holder.receive(GasDoseParamPacket())
        f.holder.edit(GasDoseNumber.STEPS, 456f)
        assertEquals(456f, f.holder.values!!.steps)
        assertEquals(0, f.sent.last().pack()[4])
        f.holder.edit(GasDoseNumber.STOICHIOMETRIC_RATIO, 15.2f)
        f.holder.edit(GasDoseNumber.FUEL_CUT_CLOSING, 32f)
        f.holder.edit(GasDoseNumber.CORRECTION_POSITIVE, 13f)
        f.holder.edit(GasDoseNumber.CORRECTION_NEGATIVE, 14f)
        f.holder.selectFrequency(4)
        f.holder.selectFrequency(2)
        f.holder.setMaximumFrequency(true)
        f.holder.toggleTest()
        assertEquals(8, f.sent.size)
        f.sent.forEach { assertEquals(0, it.pack()[4]) }
        assertEquals(2, f.holder.values!!.frequency)
        assertTrue(f.holder.values!!.maxFrequencyAtInit)
        assertTrue(f.holder.values!!.testing)
        assertEquals(listOf(456f, 15.2f, 32f, 13f, 14f), GasDoseNumber.entries.map(f.holder.values!!::value))
    }

    @Test fun noPreloadSendFirmwareGateAndReceiveResetsDeltaAndKeepsPacketTail() {
        val f = Fixture()
        f.holder.edit(GasDoseNumber.STEPS, 55f)
        f.holder.manualStep(1)
        f.holder.flushManual()
        assertTrue(f.sent.isEmpty())
        val packetBytes = GasDoseParamPacket(steps = 100, manualPositionD = 40).pack()
        val incoming = intArrayOf('@'.code, '*'.code) + packetBytes.drop(1).toIntArray() + intArrayOf(0xD1, 0xE2)
        val packet = GasDoseParamPacket().parse(incoming) as GasDoseParamPacket
        f.holder.receive(packet)
        assertEquals(0, packet.manualPositionD)
        assertEquals(0xD1, packet.pack()[14])
        f.holder.manualStep(1)
        f.holder.setFirmwareAvailable(false)
        f.holder.edit(GasDoseNumber.STEPS, 500f)
        f.holder.selectFrequency(3)
        f.holder.setMaximumFrequency(true)
        f.holder.toggleTest()
        f.holder.flushManual()
        assertTrue(f.sent.isEmpty())
        f.holder.setFirmwareAvailable(true)
        f.holder.edit(GasDoseNumber.STEPS, 101f)
        assertEquals(101, packet.steps)
        assertEquals(0xD1, packet.pack()[14])
        f.sent.clear()
        f.holder.manualStep(1)
        f.holder.receive(GasDoseParamPacket(steps = 200))
        f.holder.finishManual()
        assertTrue(f.sent.isEmpty())
    }

    @Test fun manualChunksAreBoundedImmutableAndFinalFlushIsIdempotent() {
        val f = Fixture()
        f.holder.receive(GasDoseParamPacket(steps = 123, manualPositionD = 9))
        f.holder.manualStep(1)
        f.holder.manualStep(1)
        f.holder.flushManual()
        assertEquals(2, f.sent.single().pack()[4])
        f.holder.manualStep(-1)
        f.holder.cancelManual()
        f.holder.finishManual()
        assertEquals(1, f.sent.size)
        repeat(200) { f.holder.manualStep(1) }
        f.holder.finishManual()
        assertEquals(127, f.sent.last().pack()[4])
        repeat(200) { f.holder.manualStep(-1) }
        f.holder.finishManual()
        assertEquals(129, f.sent.last().pack()[4])
        f.holder.edit(GasDoseNumber.STEPS, 400f)
        assertEquals(0, f.sent.last().pack()[4])
    }

    @Test fun resetRequiresFreshPacketAndFirmwareDisableDropsPendingMovement() {
        val f = Fixture()
        f.holder.receive(GasDoseParamPacket(steps = 100))
        f.holder.manualStep(1)
        f.holder.reset()
        assertNull(f.holder.values)
        f.holder.edit(GasDoseNumber.STEPS, 200f)
        f.holder.selectFrequency(2)
        f.holder.manualStep(1)
        f.holder.finishManual()
        assertTrue(f.sent.isEmpty())

        f.holder.receive(GasDoseParamPacket(steps = 300))
        f.holder.manualStep(1)
        f.holder.setFirmwareAvailable(false)
        f.holder.setFirmwareAvailable(true)
        f.holder.finishManual()
        assertTrue(f.sent.isEmpty())
        f.holder.manualStep(-1)
        f.holder.finishManual()
        assertEquals(255, f.sent.single().pack()[4])
    }

    private class Fixture {
        val sent = mutableListOf<OutputPacket>()
        val holder = GasDoseStateHolder({}, sent::add)
    }
}
