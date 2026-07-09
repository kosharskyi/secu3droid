package org.secu3.android.models

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.secu3.android.models.packets.input.CheckEngineErrorsPacket
import org.secu3.android.models.packets.out.ChangeModePacket
import org.secu3.android.models.packets.out.OpCompNc
import org.secu3.android.utils.Task

class PacketTest {

    @Test
    fun changeModePacket_packsDescriptorNextDescriptorAndStubByte() {
        val packet = ChangeModePacket.getPacket(Task.Secu3ReadFirmwareInfo)

        assertArrayEquals(intArrayOf('h'.code, 'y'.code, 0), packet.pack())
    }

    @Test
    fun opCompNc_packsAndParsesSaveEepromCommand() {
        val packet = OpCompNc.getSaveEepromCommand()

        assertArrayEquals(intArrayOf('u'.code, 0, 1), packet.pack())

        val parsed = OpCompNc().parse(intArrayOf('@'.code, 'u'.code, 0, 1)) as OpCompNc

        assertTrue(parsed.isEepromParamSave)
    }

    @Test
    fun rawPacket_parsesCheckEngineErrorsPacket() {
        val parsed = RawPacket(
            intArrayOf('@'.code, 'v'.code, 0x01, 0x02, 0x03, 0x04, 0, 0)
        ).parse(null) as CheckEngineErrorsPacket

        assertEquals(0x01020304, parsed.errors)
        assertTrue(parsed.isError(2))
        assertFalse(parsed.isError(3))
    }

    @Test
    fun rawPacket_returnsNullForUnknownDescriptor() {
        val parsed = RawPacket(intArrayOf('@'.code, '?'.code, 0, 0)).parse(null)

        assertNull(parsed)
    }
}
