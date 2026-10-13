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
import org.secu3.android.models.packets.base.OutputPacket
import org.secu3.android.models.packets.out.params.ChokeControlParPacket

class ChokeControlStateHolderTest {
    private class Fixture {
        val states = mutableListOf<ChokeValues>()
        val sends = mutableListOf<OutputPacket>()
        val holder = ChokeControlStateHolder(states::add, sends::add)
    }

    @Test fun resetClearsPacketValuesAndPendingManualDeltaUntilFreshReceive() {
        val fixture = Fixture()
        val received = ChokeControlParPacket(smSteps = 777)
        fixture.holder.receive(received)
        assertTrue(fixture.sends.isEmpty())
        fixture.holder.manualStep(1)
        fixture.holder.reset()

        assertEquals(null, fixture.holder.values)
        fixture.holder.edit(ChokeNumber.STEPS, 900f)
        fixture.holder.toggleTest()
        fixture.holder.manualStep(1)
        fixture.holder.finishManual()
        assertTrue(fixture.sends.isEmpty())

        fixture.holder.receive(ChokeControlParPacket(smSteps = 600))
        fixture.holder.edit(ChokeNumber.STEPS, 900f)
        assertEquals(1, fixture.sends.size)
    }

    @Test fun manualDirectionsClampReleaseOnceAndZeroOrRepeatedReleaseDoesNotSend() {
        val positive = Fixture()
        positive.holder.receive(ChokeControlParPacket())
        repeat(150) { positive.holder.manualStep(1) }
        assertEquals(0, positive.sends.size)
        positive.holder.finishManual()
        assertEquals(1, positive.sends.size)
        assertEquals(127, positive.sends.single().pack()[4])
        positive.holder.finishManual()
        assertEquals(1, positive.sends.size)

        val negative = Fixture()
        negative.holder.receive(ChokeControlParPacket())
        repeat(150) { negative.holder.manualStep(-1) }
        negative.holder.finishManual()
        assertEquals(129, negative.sends.single().pack()[4])

        val zero = Fixture()
        zero.holder.receive(ChokeControlParPacket())
        zero.holder.manualStep(1)
        zero.holder.manualStep(-1)
        zero.holder.finishManual()
        assertTrue(zero.sends.isEmpty())
    }

    @Test fun periodicFlushesSendChunksAndReleaseSendsOnlyRemainder() {
        val fixture = Fixture()
        val packet = ChokeControlParPacket()
        fixture.holder.receive(packet)
        repeat(5) { fixture.holder.manualStep(1) }
        fixture.holder.flushManual()
        repeat(3) { fixture.holder.manualStep(1) }
        fixture.holder.flushManual()
        repeat(2) { fixture.holder.manualStep(1) }
        fixture.holder.finishManual()
        fixture.holder.finishManual()

        assertEquals(listOf(5, 3, 2), fixture.sends.map { it.pack()[4] })
        assertEquals(0, packet.manualPositionD)
    }

    @Test fun periodicChunksRespectWireClampAndCancelDropsOnlyUnsentRemainder() {
        val fixture = Fixture()
        fixture.holder.receive(ChokeControlParPacket())
        repeat(200) { fixture.holder.manualStep(1) }
        fixture.holder.flushManual()
        repeat(4) { fixture.holder.manualStep(-1) }
        fixture.holder.cancelManual()
        fixture.holder.finishManual()
        assertEquals(listOf(127), fixture.sends.map { it.pack()[4] })

        repeat(200) { fixture.holder.manualStep(-1) }
        fixture.holder.flushManual()
        assertEquals(129, fixture.sends.last().pack()[4])
    }

    @Test fun cancelAndIncomingPacketDiscardPendingManualDeltaWithoutSending() {
        val fixture = Fixture()
        val original = ChokeControlParPacket(manualPositionD = 23)
        fixture.holder.receive(original)
        assertEquals(0, original.manualPositionD)
        fixture.holder.manualStep(1)
        fixture.holder.cancelManual()
        fixture.holder.finishManual()
        assertTrue(fixture.sends.isEmpty())

        fixture.holder.manualStep(-1)
        val received = ChokeControlParPacket(smSteps = 777, manualPositionD = 88)
        fixture.holder.receive(received)
        assertEquals(0, received.manualPositionD)
        fixture.holder.finishManual()
        assertTrue(fixture.sends.isEmpty())
        assertEquals(777f, fixture.holder.values?.value(ChokeNumber.STEPS))
    }

    @Test fun noPacketDoesNotSendAndOrdinaryEditDuringManualHoldKeepsDeltaPending() {
        val fixture = Fixture()
        fixture.holder.manualStep(1)
        fixture.holder.finishManual()
        fixture.holder.toggleTest()
        fixture.holder.edit(ChokeNumber.STEPS, 900f)
        assertTrue(fixture.sends.isEmpty())

        val packet = ChokeControlParPacket(smSteps = 600, manualPositionD = 0)
        fixture.holder.receive(packet)
        fixture.holder.manualStep(1)
        fixture.holder.edit(ChokeNumber.STEPS, 900f)
        assertEquals(1, fixture.sends.size)
        assertEquals(0, fixture.sends[0].pack()[4])
        assertEquals(0, packet.manualPositionD)
        fixture.holder.finishManual()
        assertEquals(2, fixture.sends.size)
        assertEquals(1, fixture.sends[1].pack()[4])
        assertEquals(900, fixture.sends[1].pack()[1] * 256 + fixture.sends[1].pack()[2])
        assertEquals(0, packet.manualPositionD)

        val before = fixture.sends[0].pack()
        fixture.holder.edit(ChokeNumber.STEPS, 1000f)
        fixture.sends.last().pack()[4] = 55
        assertEquals(0, before[4])
        assertEquals(1, fixture.sends[1].pack()[4])
    }

    @Test fun ordinaryActionsSendOneZeroDeltaSnapshotAndPreserveUnknownFlags() {
        val fixture = Fixture()
        val packet = ChokeControlParPacket(flags = 0b10100000, smFreq = 0)
        fixture.holder.receive(packet)

        fixture.holder.edit(ChokeNumber.REGULATOR_FACTOR, .75f)
        fixture.holder.toggle(ChokeNumber.CLOSED_LOOP, true)
        fixture.holder.selectFrequency(3)
        fixture.holder.toggleTest()
        val count = fixture.sends.size
        fixture.holder.toggle(ChokeNumber.STEPS, true)
        fixture.holder.selectFrequency(-1)
        fixture.holder.selectFrequency(4)

        assertEquals(4, count)
        assertEquals(count, fixture.sends.size)
        fixture.sends.forEach { assertEquals(0, it.pack()[4]) }
        assertEquals(0b10100001, packet.flags)
        assertEquals(3, packet.smFreq)
        assertTrue(packet.testing)
        assertEquals(.75f, fixture.holder.values?.value(ChokeNumber.REGULATOR_FACTOR) ?: 0f)
        assertTrue(fixture.holder.values?.flags?.get(0) == true)
        assertFalse(fixture.holder.values?.testing == false)
    }
}
