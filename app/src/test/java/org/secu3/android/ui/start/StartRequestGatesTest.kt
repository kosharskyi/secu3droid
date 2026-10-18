package org.secu3.android.ui.start

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StartRequestGatesTest {
    @Test fun usbPermissionRequiresOneMatchingGrantedResultAndPlatformPermission() {
        val gate = UsbPermissionRequestGate()
        assertTrue(gate.begin(7))
        assertFalse(gate.begin(7))
        assertFalse(gate.complete(8, permissionGranted = true, devicePermissionGranted = true))
        assertFalse(gate.complete(7, permissionGranted = false, devicePermissionGranted = true))
        assertFalse(gate.complete(7, permissionGranted = true, devicePermissionGranted = true))

        assertTrue(gate.begin(7))
        assertFalse(gate.complete(7, permissionGranted = true, devicePermissionGranted = false))
        assertTrue(gate.begin(7))
        assertTrue(gate.complete(7, permissionGranted = true, devicePermissionGranted = true))
        assertFalse(gate.complete(7, permissionGranted = true, devicePermissionGranted = true))
    }

    @Test fun canceledUsbResultClearsPendingRequestAndBondEventsMustMatchSelectedDevice() {
        val usb = UsbPermissionRequestGate()
        assertTrue(usb.begin(1))
        assertFalse(usb.complete(null, permissionGranted = false, devicePermissionGranted = false))
        assertTrue(usb.begin(1))

        val bond = BluetoothBondRequestGate()
        bond.begin("selected")
        assertFalse(bond.matches("other"))
        assertFalse(bond.complete("other"))
        assertTrue(bond.matches("selected"))
        assertTrue(bond.complete("selected"))
        assertFalse(bond.complete("selected"))
    }

    @Test fun bluetoothDiscoveryResumesOnlyForVisibleIdlePicker() {
        assertTrue(shouldResumeBluetoothDiscovery(true, false, false, false, false))
        assertFalse(shouldResumeBluetoothDiscovery(false, false, false, false, false))
        assertFalse(shouldResumeBluetoothDiscovery(true, true, false, false, false))
        assertFalse(shouldResumeBluetoothDiscovery(true, false, true, false, false))
        assertFalse(shouldResumeBluetoothDiscovery(true, false, false, true, false))
        assertFalse(shouldResumeBluetoothDiscovery(true, false, false, false, true))
    }
}
