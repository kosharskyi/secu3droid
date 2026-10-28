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
}
