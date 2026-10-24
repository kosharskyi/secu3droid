package org.secu3.android.ui.start

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StartRequestGatesTest {
    @Test fun bondEventsMustMatchSelectedDevice() {
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
