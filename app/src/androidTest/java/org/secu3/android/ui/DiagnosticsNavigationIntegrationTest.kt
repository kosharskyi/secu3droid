package org.secu3.android.ui

import android.view.KeyEvent
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavHostController
import androidx.test.platform.app.InstrumentationRegistry
import com.hoho.android.usbserial.driver.UsbSerialPort
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.secu3.android.connection.Connection
import org.secu3.android.connection.ConnectionState
import org.secu3.android.connection.Connected
import org.secu3.android.connection.Disconnected
import org.secu3.android.connection.Secu3ConnectionManager
import org.secu3.android.connection.UsbConnection
import org.secu3.android.models.RawPacket
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.secu3.android.R
import org.secu3.android.ui.diagnostics.DiagnosticOutput
import org.secu3.android.ui.diagnostics.DiagnosticsViewModel
import java.lang.reflect.Proxy
import java.util.concurrent.CopyOnWriteArrayList

class DiagnosticsNavigationIntegrationTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val fakeTransports = mutableListOf<FakeUsbTransport>()

    @After
    fun closeFakeTransports() {
        fakeTransports.toList().forEach(FakeUsbTransport::disconnect)
        fakeTransports.clear()
    }

    @Test
    fun diagnosticsGraphSharesViewModelAcrossDialogAndRecreationThenClosesSessionOnExit() = runBlocking {
        val nativeHost = navigateHome()
        compose.activityRule.scenario.onActivity { it.openDiagnostics() }
        compose.waitForIdle()
        assertDiagnosticsHost(nativeHost)

        val firstVm = diagnosticsViewModel()
        val transport = connectFakeUsb(firstVm)
        await { firstVm.connectionStatusLiveData.value == Connected }
        assertFalse(firstVm.uiState.value.controlsEnabled)
        transport.emitDiagnosticInput(flags = 0)
        await { firstVm.uiState.value.controlsEnabled }
        compose.activityRule.scenario.onActivity {
            assertEquals(50f, firstVm.uiState.value.duty)
            firstVm.selectChannel(3)
            assertEquals(50f, firstVm.uiState.value.duty)
            firstVm.editFrequency(321f)
            firstVm.editDuty(37.5f)
        }
        compose.onNodeWithTag("diagnostics-settings-icon").performClick()
        compose.waitForIdle()
        assertEquals(DIAGNOSTICS_ADDITIONAL_ROUTE, compose.activity.rootNavigationController?.currentDestination?.route)
        assertSame(firstVm, diagnosticsViewModel())

        compose.onNodeWithTag("enable_bl_de_checkbox").performClick()
        compose.activityRule.scenario.recreate()
        compose.waitForIdle()
        val restoredHost = nativeHost()
        val restoredVm = diagnosticsViewModel()
        assertSame(firstVm, restoredVm)
        assertEquals(3, restoredVm.uiState.value.channel)
        assertEquals(321f, restoredVm.uiState.value.frequency)
        assertEquals(37.5f, restoredVm.uiState.value.duty)
        compose.onNodeWithTag("enable_bl_de_checkbox").assertIsOn()

        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
        compose.waitForIdle()
        assertEquals(DIAGNOSTICS_ROUTE, compose.activity.rootNavigationController?.currentDestination?.route)
        assertSame(restoredVm, diagnosticsViewModel())
        assertEquals(false, restoredVm.uiState.value.enableBlDe)
        compose.onNodeWithText("IGN_OUT3").assertExists()
        compose.onNodeWithText("321.0 Hz").assertExists()
        compose.onNodeWithText("37.5 %").assertExists()
        compose.onNodeWithContentDescription(compose.activity.getString(R.string.back)).performClick()
        compose.waitForIdle()
        assertNativeHome(restoredHost)
        compose.onAllNodesWithText(compose.activity.getString(R.string.disconnect_device)).assertCountEquals(0)

        compose.activityRule.scenario.onActivity { it.openDiagnostics() }
        compose.waitForIdle()
        val reopenedVm = diagnosticsViewModel()
        assertNotSame(restoredVm, reopenedVm)
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
        compose.waitForIdle()
        assertNativeHome(restoredHost)
    }

    @Test
    fun confirmingAdditionalOptionsCommitsInViewModelAndDoesNotCreateAnotherGraphEntry() = runBlocking {
        val host = navigateHome()
        compose.activityRule.scenario.onActivity { it.openDiagnostics() }
        compose.waitForIdle()
        val vm = diagnosticsViewModel()
        val transport = connectFakeUsb(vm)
        await { vm.connectionStatusLiveData.value == Connected }
        assertFalse(vm.uiState.value.controlsEnabled)
        transport.emitDiagnosticInput(flags = 0)
        await { vm.uiState.value.controlsEnabled }
        compose.onNodeWithTag("diagnostics-settings-icon").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("enable_bl_de_checkbox").performClick()
        compose.onNodeWithTag("diagnostic_ok").performClick()
        compose.waitForIdle()
        assertSame(vm, diagnosticsViewModel())
        assertEquals(DIAGNOSTICS_ROUTE, compose.activity.rootNavigationController?.currentDestination?.route)
        assertEquals(true, vm.uiState.value.enableBlDe)

        compose.activityRule.scenario.onActivity {
            vm.setOutput(DiagnosticOutput.BL, true)
            vm.setOutput(DiagnosticOutput.DE, true)
        }
        val blDeHigh = (1 shl 11) or (1 shl 12) or (1 shl 13) or (1 shl 14)
        assertEquals(blDeHigh, vm.uiState.value.outputBits)

        compose.onNodeWithTag("diagnostics-settings-icon").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("diagnostic_ok").performClick()
        compose.waitForIdle()
        assertSame(vm, diagnosticsViewModel())
        assertEquals(true, vm.uiState.value.enableBlDe)
        assertEquals(blDeHigh, vm.uiState.value.outputBits)

        compose.activityRule.scenario.onActivity { it.openDiagnostics() }
        compose.waitForIdle()
        assertSame(vm, diagnosticsViewModel())
        assertEquals(DIAGNOSTICS_ROUTE, compose.activity.rootNavigationController?.currentDestination?.route)
    }

    @Test
    fun linkLossClearsReadingsAndBlocksCommandsUntilFreshDiagnosticInput() = runBlocking {
        navigateHome()
        compose.activityRule.scenario.onActivity { it.openDiagnostics() }
        compose.waitForIdle()
        val vm = diagnosticsViewModel()
        assertFalse(vm.uiState.value.controlsEnabled)
        val transport = connectFakeUsb(vm)
        await { vm.connectionStatusLiveData.value == Connected }
        assertFalse(vm.uiState.value.controlsEnabled)
        transport.emitDiagnosticInput(flags = 1, voltageRaw = 500)
        await { vm.uiState.value.controlsEnabled }
        compose.onNodeWithTag("analog_Voltage-value").assertTextEquals("1.250 V")

        compose.activityRule.scenario.onActivity {
            vm.selectChannel(3)
            vm.editFrequency(321f)
            vm.editDuty(37.5f)
            vm.setOutput(DiagnosticOutput.IGN_OUT4, true)
            vm.applyAdditionalOptions(enableBlDe = true, enableTachO = true)
            vm.setOutput(DiagnosticOutput.BL, true)
            vm.setOutput(DiagnosticOutput.DE, true)
            vm.setOutput(DiagnosticOutput.TACH_O, true)
        }
        val activeFrequency = vm.uiState.value.frequency
        val activeDuty = vm.uiState.value.duty
        compose.waitForIdle()
        compose.onNodeWithTag("diagnostics-settings-icon").performClick()
        compose.waitForIdle()
        assertEquals(DIAGNOSTICS_ADDITIONAL_ROUTE, compose.activity.rootNavigationController?.currentDestination?.route)

        transport.disconnect()
        await { !vm.uiState.value.controlsEnabled && vm.diagInputLiveData.value == null }
        compose.waitForIdle()
        assertEquals(DIAGNOSTICS_ROUTE, compose.activity.rootNavigationController?.currentDestination?.route)
        compose.onNodeWithTag("analog_Voltage-value").assertTextEquals("— V")
        assertEquals(0, vm.uiState.value.channel)
        assertEquals(0b11, vm.uiState.value.outputBits)
        assertEquals(activeFrequency, vm.uiState.value.frequency)
        assertEquals(activeDuty, vm.uiState.value.duty)

        val bitsAfterLoss = vm.uiState.value.outputBits
        val writesAfterLoss = transport.writes.size
        compose.activityRule.scenario.onActivity {
            vm.selectChannel(5)
            vm.editFrequency(999f)
            vm.editDuty(99f)
            vm.setOutput(DiagnosticOutput.IGN_OUT1, false)
            vm.enableBlDe(false)
            vm.enableTachO(false)
            vm.applyAdditionalOptions(enableBlDe = true, enableTachO = true)
            vm.sendDiagOutPacket()
        }
        assertEquals(0, vm.uiState.value.channel)
        assertEquals(activeFrequency, vm.uiState.value.frequency)
        assertEquals(activeDuty, vm.uiState.value.duty)
        assertEquals(bitsAfterLoss, vm.uiState.value.outputBits)
        assertEquals(writesAfterLoss, transport.writes.size)

        val reconnected = connectFakeUsb(vm)
        await { vm.connectionStatusLiveData.value == Connected }
        assertFalse(vm.uiState.value.controlsEnabled)
        val writesBeforeFreshInput = reconnected.writes.size
        compose.activityRule.scenario.onActivity {
            vm.selectChannel(2)
            vm.editFrequency(999f)
            vm.editDuty(99f)
            vm.enableBlDe(true)
            vm.enableTachO(true)
            vm.sendDiagOutPacket()
        }
        assertEquals(0, vm.uiState.value.channel)
        assertEquals(activeFrequency, vm.uiState.value.frequency)
        assertEquals(activeDuty, vm.uiState.value.duty)
        assertEquals(writesBeforeFreshInput, reconnected.writes.size)
        reconnected.emitDiagnosticInput(flags = 0)
        await { vm.uiState.value.controlsEnabled }
        assertEquals(0, vm.uiState.value.outputBits)
        assertEquals(0, vm.uiState.value.channel)
        assertEquals(activeFrequency, vm.uiState.value.frequency)
        assertEquals(activeDuty, vm.uiState.value.duty)
    }

    private fun navigateHome(): NavHostController {
        lateinit var host: NavHostController
        compose.activityRule.scenario.onActivity { activity ->
            host = requireNotNull(activity.rootNavigationController)
            host.navigate(HOME_ROUTE)
        }
        compose.waitForIdle()
        return host
    }

    private fun nativeHost(): NavHostController {
        lateinit var host: NavHostController
        compose.activityRule.scenario.onActivity { activity ->
            host = requireNotNull(activity.rootNavigationController)
        }
        return host
    }

    private fun diagnosticsViewModel(): DiagnosticsViewModel {
        lateinit var vm: DiagnosticsViewModel
        compose.activityRule.scenario.onActivity { activity ->
            val controller = requireNotNull(activity.rootNavigationController)
            val graphEntry = controller.getBackStackEntry(DIAGNOSTICS_GRAPH_ROUTE)
            vm = ViewModelProvider(graphEntry.viewModelStore, activity.defaultViewModelProviderFactory, graphEntry.defaultViewModelCreationExtras)[DiagnosticsViewModel::class.java]
        }
        return vm
    }

    private fun assertDiagnosticsHost(host: NavHostController) {
        compose.activityRule.scenario.onActivity { activity ->
            assertEquals(DIAGNOSTICS_ROUTE, activity.rootNavigationController?.currentDestination?.route)
            assertEquals(Lifecycle.State.CREATED, host.getBackStackEntry(HOME_ROUTE).lifecycle.currentState)
            assertEquals(Lifecycle.State.RESUMED, host.getBackStackEntry(DIAGNOSTICS_ROUTE).lifecycle.currentState)
        }
        compose.onNodeWithText(compose.activity.getString(R.string.title_activity_diagnostics)).assertExists()
    }

    private fun assertNativeHome(host: NavHostController) {
        compose.activityRule.scenario.onActivity { activity ->
            assertEquals(HOME_ROUTE, host.currentDestination?.route)
            assertEquals(Lifecycle.State.RESUMED, host.getBackStackEntry(HOME_ROUTE).lifecycle.currentState)
            assertEquals(HOME_ROUTE, activity.rootNavigationController?.currentDestination?.route)
        }
        compose.onNodeWithTag("home-card-diagnostics").assertExists()
    }

    private fun connectFakeUsb(viewModel: DiagnosticsViewModel): FakeUsbTransport = runBlocking {
        val managerField = DiagnosticsViewModel::class.java.getDeclaredField("secu3ConnectionManager").apply { isAccessible = true }
        val manager = managerField.get(viewModel) as Secu3ConnectionManager
        val usbField = Secu3ConnectionManager::class.java.declaredFields.first { UsbConnection::class.java.isAssignableFrom(it.type) }
            .apply { isAccessible = true }
        val connection = usbField.get(manager) as UsbConnection
        val endpointField = Connection::class.java.getDeclaredField("endpoint").apply { isAccessible = true }
        val writes = CopyOnWriteArrayList<ByteArray>()
        val port = Proxy.newProxyInstance(UsbSerialPort::class.java.classLoader, arrayOf(UsbSerialPort::class.java)) { _, method, args ->
            when (method.name) {
                "isOpen" -> true
                "write" -> { (args?.firstOrNull() as? ByteArray)?.let { writes += it.copyOf() }; null }
                "read" -> 0
                "toString" -> "diagnostics-test-usb"
                "hashCode" -> System.identityHashCode(this)
                "equals" -> false
                else -> null
            }
        } as UsbSerialPort
        endpointField.set(connection, port)
        Connection::class.java.getDeclaredField("isRunning").apply { isAccessible = true }.setBoolean(connection, true)
        val stateFlowField = Connection::class.java.getDeclaredField("mConnectionStateFlow").apply { isAccessible = true }
        @Suppress("UNCHECKED_CAST")
        val stateFlow = stateFlowField.get(connection) as MutableSharedFlow<ConnectionState>
        delay(100)
        stateFlow.emit(Connected)
        FakeUsbTransport(connection, endpointField, port, stateFlow, writes).also(fakeTransports::add)
    }

    private suspend fun await(predicate: () -> Boolean) {
        withTimeout(4_000) { while (!predicate()) delay(10) }
    }

    private class FakeUsbTransport(
        private val connection: UsbConnection,
        private val endpointField: java.lang.reflect.Field,
        private val port: UsbSerialPort,
        private val stateFlow: MutableSharedFlow<ConnectionState>,
        val writes: CopyOnWriteArrayList<ByteArray>,
    ) {
        suspend fun emitDiagnosticInput(flags: Int, voltageRaw: Int = 0) {
            val rawFlowField = Connection::class.java.getDeclaredField("mReceivedPacketFlow").apply { isAccessible = true }
            @Suppress("UNCHECKED_CAST")
            val rawFlow = rawFlowField.get(connection) as MutableSharedFlow<RawPacket>
            rawFlow.emit(
                RawPacket(
                    intArrayOf('@'.code, '='.code, flags, voltageRaw shr 8, voltageRaw and 0xFF) +
                        IntArray(28) + intArrayOf(0, 0),
                ),
            )
        }

        fun disconnect() = runBlocking {
            if (endpointField.get(connection) === port) endpointField.set(connection, null)
            Connection::class.java.getDeclaredField("isRunning").apply { isAccessible = true }.setBoolean(connection, false)
            stateFlow.emit(Disconnected)
        }
    }

}
