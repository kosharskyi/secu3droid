package org.secu3.android.models.packets.out.params

import org.junit.Assert.assertEquals
import org.junit.Test
import org.secu3.android.models.packets.base.OutputPacket

class GasDoseParamPacketTest {
    @Test
    fun scalesWireFieldsAndSnapshotsSignedManualPosition() {
        val packet = GasDoseParamPacket(
            steps = 321,
            testing = true,
            manualPositionD = -2,
            fcClosing = 30f,
            lambdaCorrLimitP = 12.5f,
            lambdaCorrLimitM = 20f,
            lambdaStoichval = 14.7f,
            freq = 3,
            maxFreqInit = 1,
        )
        val bytes = packet.pack()
        assertEquals(254, bytes[4])
        assertEquals(60, bytes[5])
        assertEquals(0, bytes[6])
        assertEquals(64, bytes[7])
        assertEquals(7, bytes[10])
        assertEquals(90, bytes[11])
        assertEquals(3, bytes[12])

        val receivedPayload = bytes.drop(1).toIntArray()
        receivedPayload[3] = 0
        val withExtra = intArrayOf('@'.code, '*'.code) + receivedPayload + intArrayOf(0xD1, 0xE2)
        val parsed = GasDoseParamPacket(manualPositionD = -17).parse(withExtra) as GasDoseParamPacket
        assertEquals(0, parsed.manualPositionD)
        assertEquals(30f, parsed.fcClosing)
        assertEquals(12.5f, parsed.lambdaCorrLimitP)
        assertEquals(20f, parsed.lambdaCorrLimitM, .1f)
        assertEquals(14.7f, parsed.lambdaStoichval, .01f)
        assertEquals(0xD1, parsed.pack()[bytes.size])
        assertEquals(0xE2, parsed.pack()[bytes.size + 1])

        val snapshot: OutputPacket = parsed.snapshotWithManualPosition(-127)
        assertEquals(0, parsed.manualPositionD)
        assertEquals(129, snapshot.pack()[4])
        val copy = snapshot.pack()
        copy[4] = 0
        assertEquals(129, snapshot.pack()[4])
    }
}
