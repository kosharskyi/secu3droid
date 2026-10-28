package org.secu3.android.connection

import android.Manifest
import android.annotation.SuppressLint
import android.os.Build
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BluetoothPermissionPolicy @Inject constructor() {
    @SuppressLint("InlinedApi")
    fun connectionPermissions(
        apiLevel: Int = Build.VERSION.SDK_INT,
        manufacturer: String = Build.MANUFACTURER,
        brand: String = Build.BRAND,
        model: String = Build.MODEL,
    ): Array<String> {
        val base = if (apiLevel >= 31) {
            arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN).let {
                if (apiLevel == 31 && isXiaomiDevice(manufacturer, brand, model)) it + Manifest.permission.BLUETOOTH else it
            }
        } else {
            arrayOf(Manifest.permission.BLUETOOTH, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        }
        return base + if (apiLevel == 29) arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION) else emptyArray()
    }

    @SuppressLint("InlinedApi")
    fun discoveryPermissions(apiLevel: Int = Build.VERSION.SDK_INT): Array<String> = if (apiLevel >= 31) {
        arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
    } else {
        arrayOf(Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN, Manifest.permission.ACCESS_FINE_LOCATION)
    }

    private fun isXiaomiDevice(manufacturer: String, brand: String, model: String): Boolean =
        manufacturer.equals("Xiaomi", ignoreCase = true) ||
            brand.equals("Xiaomi", ignoreCase = true) ||
            brand.equals("POCO", ignoreCase = true) ||
            model.contains("POCO", ignoreCase = true)
}
