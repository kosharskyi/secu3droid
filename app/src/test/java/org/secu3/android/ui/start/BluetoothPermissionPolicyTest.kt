package org.secu3.android.ui.start

import android.Manifest
import org.junit.Assert.assertArrayEquals
import org.secu3.android.connection.BluetoothPermissionPolicy
import org.junit.Test

class BluetoothPermissionPolicyTest {
    private val policy = BluetoothPermissionPolicy()

    @Test fun api31XiaomiIncludesLegacyBluetoothPermissionButLaterVersionsDoNot() {
        assertArrayEquals(
            arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH),
            policy.connectionPermissions(31, manufacturer = "Xiaomi", brand = "", model = ""),
        )
        assertArrayEquals(
            arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN),
            policy.connectionPermissions(32, manufacturer = "Xiaomi", brand = "", model = ""),
        )
    }

    @Test fun legacyAndApi29LocationPermissionSetsArePreserved() {
        assertArrayEquals(
            arrayOf(Manifest.permission.BLUETOOTH, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
            policy.connectionPermissions(28, manufacturer = "", brand = "", model = ""),
        )
        assertArrayEquals(
            arrayOf(Manifest.permission.BLUETOOTH, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_BACKGROUND_LOCATION),
            policy.connectionPermissions(29, manufacturer = "", brand = "", model = ""),
        )
    }

    @Test fun connectionAndDiscoveryKeepTheirDistinctPermissionSets() {
        assertArrayEquals(
            arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT),
            policy.discoveryPermissions(31),
        )
        assertArrayEquals(
            arrayOf(Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN, Manifest.permission.ACCESS_FINE_LOCATION),
            policy.discoveryPermissions(30),
        )
    }

    @Test fun api31OemExceptionMatchesXiaomiPocoAndPocoModelCaseInsensitively() {
        val expected = arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH)
        assertArrayEquals(expected, policy.connectionPermissions(31, manufacturer = "", brand = "pOcO", model = ""))
        assertArrayEquals(expected, policy.connectionPermissions(31, manufacturer = "", brand = "", model = "POCO F5"))
        assertArrayEquals(
            arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN),
            policy.connectionPermissions(31, manufacturer = "", brand = "", model = "Redmi"),
        )
    }
}
