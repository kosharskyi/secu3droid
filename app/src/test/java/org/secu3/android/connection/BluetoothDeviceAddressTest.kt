package org.secu3.android.connection

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BluetoothDeviceAddressTest {
    private data class BondedDevice(val address: String, val name: String)

    @Test fun sameDisplayNameDoesNotChangeAddressSelection() {
        val devices = listOf(
            BondedDevice("00:11:22:33:44:55", "ECU"),
            BondedDevice("AA:BB:CC:DD:EE:FF", "ECU"),
        )

        assertEquals(devices[1], BluetoothDeviceAddress.find("aa:bb:cc:dd:ee:ff", devices) { it.address })
    }

    @Test fun renamedDeviceStillResolvesByAddress() {
        val renamed = BondedDevice("00:11:22:33:44:55", "Renamed ECU")

        assertEquals(renamed, BluetoothDeviceAddress.find("00:11:22:33:44:55", listOf(renamed)) { it.address })
    }

    @Test fun nameOnlyLegacyPreferenceCannotResolveADevice() {
        val devices = listOf(BondedDevice("00:11:22:33:44:55", "Previously selected ECU"))

        assertNull(BluetoothDeviceAddress.find(null, devices) { it.address })
    }

    @Test fun malformedAddressCannotResolveADevice() {
        val devices = listOf(BondedDevice("00:11:22:33:44:55", "ECU"))

        assertNull(BluetoothDeviceAddress.find("ECU", devices) { it.address })
    }
}
