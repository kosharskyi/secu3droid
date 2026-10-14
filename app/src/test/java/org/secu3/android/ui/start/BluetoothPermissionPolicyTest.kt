package org.secu3.android.ui.start

import android.Manifest
import org.junit.Assert.assertArrayEquals
import org.junit.Test

class BluetoothPermissionPolicyTest {
    @Test fun api31XiaomiIncludesLegacyBluetoothPermissionButLaterVersionsDoNot() {
        assertArrayEquals(
            arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH),
            bluetoothPermissionsFor(31, xiaomi = true),
        )
        assertArrayEquals(
            arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN),
            bluetoothPermissionsFor(32, xiaomi = true),
        )
    }

    @Test fun legacyAndApi29LocationPermissionSetsArePreserved() {
        assertArrayEquals(
            arrayOf(Manifest.permission.BLUETOOTH, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
            bluetoothPermissionsFor(28, xiaomi = false),
        )
        assertArrayEquals(
            arrayOf(Manifest.permission.BLUETOOTH, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_BACKGROUND_LOCATION),
            bluetoothPermissionsFor(29, xiaomi = false),
        )
    }
}
