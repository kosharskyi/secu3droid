package org.secu3.android.domain.parameters

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.secu3.android.models.packets.base.OutputPacket
import org.secu3.android.models.packets.out.params.SecurityParamPacket

class SecurityControlTest {
    private fun packet(flags: Int = 0xA0) = SecurityParamPacket(
        btFlags = flags,
        iButton0 = listOf(1, 2, 3, 4, 5, 6),
        iButton1 = listOf(10, 11, 12, 13, 14, 15),
        btType = 4,
    )

    @Test fun receivesAndResetsPassivelyAndDoesNotSendBeforeReceive() {
        val sent = mutableListOf<OutputPacket>()
        val control = SecurityControl { packet -> sent += packet; true }
        assertFalse(control.changeBluetooth(true))
        control.receive(packet())
        assertTrue(sent.isEmpty())
        assertEquals("060504030201", control.values.value?.key0)
        control.reset()
        assertNull(control.values.value)
        assertFalse(control.changeImmobilizer(true))
        assertTrue(sent.isEmpty())
    }

    @Test fun validKeySendsWithImmobilizerOffAndSnapshotPreservesUnknownFlagsAndTail() {
        val sent = mutableListOf<OutputPacket>()
        val source = SecurityParamPacket().parse(
            intArrayOf('@'.code, '#'.code, 0, 0, 0xA0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 0xFE, 0x77),
        ) as SecurityParamPacket
        val control = SecurityControl { packet -> sent += packet; true }
        control.receive(source)

        assertTrue(control.changeKey(0, "112233445566"))
        val keySnapshot = sent.single().pack()
        assertEquals(listOf(0x66, 0x55, 0x44, 0x33, 0x22, 0x11), keySnapshot.slice(4..9))
        assertEquals(0xA0, keySnapshot[3])
        assertEquals(listOf(0xFE, 0x77), keySnapshot.takeLast(2))
        assertFalse(source.useImmobilizer)
        assertEquals(listOf(0x66, 0x55, 0x44, 0x33, 0x22, 0x11), source.iButton0)
        assertFalse(control.changeKey(0, "11223344556"))
        assertFalse(control.changeKey(0, "11223344556G"))
        assertFalse(control.changeKey(2, "112233445566"))
        assertEquals(1, sent.size)
    }

    @Test fun flagsPreserveUnknownBitsAndBluetoothToggleSetsBaudBitOnDisable() {
        val sent = mutableListOf<OutputPacket>()
        val control = SecurityControl { packet -> sent += packet; true }
        val source = packet(flags = 0xA1)
        control.receive(source)
        assertTrue(control.changeBluetooth(false))
        assertEquals(0xA2, sent.single().pack()[3])
        assertTrue(control.changeImmobilizer(true))
        assertEquals(0xA6, sent.last().pack()[3])
    }

    @Test fun credentialsRequireBluetoothAndValidOemNameAndDigitsAndSnapshotsAreRedacted() {
        val sent = mutableListOf<OutputPacket>()
        val source = packet()
        val control = SecurityControl { packet -> sent += packet; true }
        control.receive(source)

        assertEquals(SecurityCredentialsResult.NOT_ADMITTED, control.applyCredentials("ECU", "1234"))
        assertTrue(control.changeBluetooth(true))
        assertEquals(SecurityCredentialsResult.INVALID, control.applyCredentials("", ""))
        assertEquals(SecurityCredentialsResult.INVALID, control.applyCredentials("ECU", "123"))
        assertEquals(SecurityCredentialsResult.INVALID, control.applyCredentials("😀", "1234"))
        assertEquals(SecurityCredentialsResult.INVALID, control.applyCredentials("ECU", "12a4"))
        assertEquals(SecurityCredentialsResult.ADMITTED, control.applyCredentials("ECU", "12345678"))
        assertEquals(2, sent.size)
        assertEquals(3, sent.last().pack()[1])
        assertEquals(8, sent.last().pack()[2])
        assertEquals("", source.btName)
        assertEquals("", source.btPass)
    }

    @Test fun rejectedCommandsRollbackReceivedPacketAndDoNotChangePublishedValues() {
        val source = packet(flags = 0xA0)
        val before = source.pack()
        val control = SecurityControl { false }
        control.receive(source)
        assertFalse(control.changeType(2))
        assertFalse(control.changeKey(0, "112233445566"))
        assertArrayEquals(before, source.pack())
        assertEquals(4, control.values.value?.btType)
        assertEquals("060504030201", control.values.value?.key0)
    }

    @Test fun parserRequiresCompleteHexKeyAndReversesBytes() {
        val control = SecurityControl { true }
        assertEquals(true, control.isValidKey("112233445566"))
        assertFalse(control.isValidKey("11223344556"))
        assertFalse(control.isValidKey("11223344556G"))
    }
}
