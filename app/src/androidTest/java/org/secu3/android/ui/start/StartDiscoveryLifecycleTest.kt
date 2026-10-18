package org.secu3.android.ui.start

import android.Manifest
import android.bluetooth.BluetoothManager
import android.os.Build
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Before
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.secu3.android.ui.MainActivity
import org.secu3.android.ui.START_ROUTE

class StartDiscoveryLifecycleTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Before fun grantOnlyPermissionsThatWereInitiallyMissing() {
        val targetPackageName = compose.activity.packageName
        val needed = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            listOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            listOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        needed.forEach { permission ->
            if (compose.activity.checkSelfPermission(permission) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                InstrumentationRegistry.getInstrumentation().uiAutomation.grantRuntimePermission(targetPackageName, permission)
            }
        }
    }

    @Test fun openPickerRestartsDiscoveryAfterPauseAndRetainsItsRows() {
        val (viewModel, adapter) = viewModelAndEnabledAdapter()
        compose.runOnIdle { viewModel.startBtDiscovery() }
        assertTrue(compose.runOnIdle { viewModel.uiState.value.showBluetoothDevices })
        waitForDiscovery(adapter, expected = true)

        moveToCreatedAndWaitForDiscoveryToStop(adapter)
        val rowsBeforePause = compose.runOnIdle { viewModel.uiState.value.bluetoothDevices }
        compose.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)

        waitForDiscovery(adapter, expected = true)
        compose.onNodeWithText(compose.activity.getString(org.secu3.android.R.string.select_a_device_from_the_list)).assertExists()
        val resumedRows = compose.runOnIdle { viewModel.uiState.value.bluetoothDevices }
        assertTrue(compose.runOnIdle { viewModel.uiState.value.showBluetoothDevices })
        assertEquals(rowsBeforePause.map { it.id }, resumedRows.take(rowsBeforePause.size).map { it.id })
    }

    @Test fun dismissedPickerDoesNotRestartDiscoveryAfterPause() {
        val (viewModel, adapter) = viewModelAndEnabledAdapter()
        compose.runOnIdle { viewModel.startBtDiscovery() }
        waitForDiscovery(adapter, expected = true)
        compose.runOnIdle {
            viewModel.cancelBtDiscovery()
            viewModel.dismissBluetoothDevices()
            assertFalse(viewModel.uiState.value.showBluetoothDevices)
        }

        moveToCreatedAndWaitForDiscoveryToStop(adapter)
        compose.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)

        repeat(10) {
            Thread.sleep(100)
            assertFalse("Dismissed picker must not restart adapter discovery", adapter.isDiscovering)
        }
        assertFalse(compose.runOnIdle { viewModel.uiState.value.showBluetoothDevices })
    }

    private fun viewModelAndEnabledAdapter(): Pair<StartScreenViewModel, android.bluetooth.BluetoothAdapter> {
        var result: Pair<StartScreenViewModel, android.bluetooth.BluetoothAdapter>? = null
        compose.activityRule.scenario.onActivity { activity ->
            val manager = activity.getSystemService(BluetoothManager::class.java)
            val adapter = manager?.adapter
            assumeTrue("Bluetooth adapter is unavailable", adapter != null)
            assumeTrue("Bluetooth is disabled; test does not change global Bluetooth state", adapter!!.isEnabled)
            val entry = requireNotNull(activity.rootNavigationController).getBackStackEntry(START_ROUTE)
            val vm = ViewModelProvider(entry.viewModelStore, activity.defaultViewModelProviderFactory, entry.defaultViewModelCreationExtras)[StartScreenViewModel::class.java]
            result = vm to adapter
        }
        return requireNotNull(result)
    }

    private fun moveToCreatedAndWaitForDiscoveryToStop(adapter: android.bluetooth.BluetoothAdapter) {
        compose.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        waitForDiscovery(adapter, expected = false)
    }

    private fun waitForDiscovery(adapter: android.bluetooth.BluetoothAdapter, expected: Boolean) {
        compose.waitUntil(timeoutMillis = 15_000) {
            InstrumentationRegistry.getInstrumentation().runOnMainSync { }
            adapter.isDiscovering == expected
        }
    }
}
