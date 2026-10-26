package org.secu3.android.domain.parameters

import org.junit.Assert.assertEquals
import org.junit.Test
import org.secu3.android.models.packets.base.OutputPacket
import org.secu3.android.models.packets.out.params.ChokeControlParPacket

class ChokePacketSnapshotTest {
    @Test fun snapshotsPreserveUnknownFlagsAndTrailingPacketData() {
        val source = ChokeControlParPacket(
            flags = 0b10100000,
            manualPositionD = -127,
            rpmIf = 42.5f,
            corrTime0 = 1.25f,
            corrTime1 = 2.5f,
            smFreq = 9,
            injCrankToRunTime = 3.75f,
        )
        val packed = source.pack()
        assertEquals(129, packed[4])
        val incomingPayload = packed.drop(1).toIntArray()
        incomingPayload[3] = 0
        val incoming = intArrayOf(0x40, 0x25) + incomingPayload + intArrayOf(0xD1, 0xE2)
        val parsed = ChokeControlParPacket().parse(incoming) as ChokeControlParPacket
        assertEquals(0, parsed.manualPositionD)
        assertEquals(42.5f, parsed.rpmIf)
        assertEquals(1.25f, parsed.corrTime0)
        assertEquals(2.5f, parsed.corrTime1)
        assertEquals(9, parsed.smFreq)
        assertEquals(3.75f, parsed.injCrankToRunTime)
        assertEquals(0b10100000, parsed.flags)
        assertEquals(0xD1, parsed.pack()[packed.size])
        assertEquals(0xE2, parsed.pack()[packed.size + 1])

        val snapshot: OutputPacket = parsed.snapshotWithManualPosition(-127)
        assertEquals(0, parsed.manualPositionD)
        val snapshotBytes = snapshot.pack()
        assertEquals(129, snapshotBytes[4])
        assertEquals(0b10100000, snapshotBytes[11])
        assertEquals(0xD1, snapshotBytes[packed.size])
        assertEquals(0xE2, snapshotBytes[packed.size + 1])
        snapshotBytes[4] = 0
        assertEquals(129, snapshot.pack()[4])
    }
}
