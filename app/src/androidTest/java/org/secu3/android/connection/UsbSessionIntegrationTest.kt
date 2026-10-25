package org.secu3.android.connection

import android.bluetooth.BluetoothManager
import android.content.ComponentName
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.hardware.usb.UsbConfiguration
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Binder
import android.os.Parcel
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import com.hoho.android.usbserial.driver.UsbSerialPort
import java.io.IOException
import java.lang.reflect.Proxy
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.secu3.android.models.RawPacket
import org.secu3.android.utils.FileHelper
import org.secu3.android.utils.SecuLogger
import org.secu3.android.utils.Task
import org.secu3.android.utils.UserPrefs

/** Exercises the production coordinator with an isolated serial port, never a physical ECU. */
class UsbSessionIntegrationTest {
    @Test fun firmwareHandshakeGatesWritesAndOnlyMatchingDetachEndsTheLockedSession() = runBlocking {
        val fixture = Fixture()
        try {
            fixture.attach(71)
            fixture.manager.sendNewTask(Task.Secu3ReadSensors)
            delay(100)
            assertFalse(fixture.manager.isConnected)
            assertTrue(fixture.writes.isEmpty())

            fixture.confirmFirmware()
            assertNotNull(fixture.manager.fwInfo)
            assertFalse(fixture.manager.stopConnection())
            fixture.manager.sendNewTask(Task.Secu3ReadSensors)
            fixture.await { fixture.writes.size == 1 }

            assertFalse(fixture.manager.onUsbDeviceDetached(72))
            assertTrue(fixture.manager.isUsbConnected)
            assertTrue(fixture.manager.usbSessionLocked.value)

            assertTrue(fixture.manager.onUsbDeviceDetached(71))
            assertFalse(fixture.manager.isConnected)
            assertFalse(fixture.manager.usbSessionLocked.value)
            assertNull(fixture.manager.fwInfo)
            assertEquals(Disconnected, fixture.manager.currentConnectionState)
            fixture.manager.sendNewTask(Task.Secu3ReadSensors)
            delay(100)
            assertEquals(1, fixture.writes.size)
        } finally {
            fixture.close()
        }
    }

    @Test fun delayedBluetoothStatesCannotBreakConfirmedUsb() = runBlocking {
        val fixture = Fixture()
        try {
            fixture.attach(81)
            fixture.confirmFirmware()
            fixture.btStates.emit(Disconnected)
            fixture.btStates.emit(ConnectionFailed(IOException("old Bluetooth failure")))
            delay(100)

            assertEquals(Connected, fixture.manager.currentConnectionState)
            assertTrue(fixture.manager.usbSessionLocked.value)
            assertTrue(fixture.manager.isUsbConnected)
        } finally {
            fixture.close()
        }
    }

    @Test fun attachmentSynchronizationDetectsMissedDetachBeforeAnotherResume() = runBlocking {
        val fixture = Fixture()
        try {
            fixture.attach(79)
            fixture.confirmFirmware()
            val endedGeneration = fixture.manager.activeUsbSessionGeneration

            assertEquals(UsbConnectionRequest.Ignored, fixture.manager.synchronizeUsbAttachments(Result.success(emptyList())))
            val terminalSession = fixture.manager.terminalSession.value
            assertEquals(ConnectionTerminationReason.UsbDetached, terminalSession?.reason)
            assertEquals(ConnectionTransport.USB, terminalSession?.transport)
            assertEquals(endedGeneration, terminalSession?.generation)
            assertEquals(Disconnected, fixture.manager.currentConnectionState)
            assertFalse(fixture.manager.usbSessionLocked.value)

            assertEquals(UsbConnectionRequest.Ignored, fixture.manager.synchronizeUsbAttachments(Result.success(emptyList())))
            assertSame(terminalSession, fixture.manager.terminalSession.value)
        } finally {
            fixture.close()
        }
    }

    @Test fun usbDetachStopsOwnedServiceImmediatelyAndPublishesTerminalGeneration() = runBlocking {
        val fixture = Fixture()
        try {
            fixture.prefs.isWakeLockEnabled = true
            fixture.attach(80)
            fixture.confirmFirmware()
            fixture.manager.onAppForegrounded()
            val generation = fixture.manager.activeUsbSessionGeneration!!
            assertNotNull(fixture.serviceController.ownerToken())

            assertTrue(fixture.manager.onUsbDeviceDetached(80))

            assertEquals(1, fixture.serviceContext.stoppedServices.size)
            assertEquals(
                ConnectionSessionTermination(
                    generation = generation,
                    transport = ConnectionTransport.USB,
                    reason = ConnectionTerminationReason.UsbDetached,
                    finalState = Disconnected,
                    sessionWasConnected = true,
                ),
                fixture.manager.terminalSession.value,
            )
        } finally {
            fixture.close()
        }
    }

    @Test fun bluetoothRetryExhaustionStopsServiceAndPublishesFinalTimeout() = runBlocking {
        val fixture = Fixture()
        val originalRetries = fixture.prefs.connectionRetries
        val originalAddress = fixture.prefs.bluetoothDeviceAddress
        try {
            fixture.prefs.isWakeLockEnabled = true
            fixture.prefs.connectionRetries = 2
            fixture.prefs.bluetoothDeviceAddress = "02:00:00:00:00:01"
            val generation = fixture.selectBluetoothSessionForTest()
            fixture.serviceController.ensureStarted(generation)
            val states = CopyOnWriteArrayList<ConnectionState>()
            val collector = launch(
                start = kotlinx.coroutines.CoroutineStart.UNDISPATCHED,
            ) {
                fixture.manager.connectionStateFlow.collect(states::add)
            }
            fixture.startBluetoothTransport(generation)

            fixture.await { fixture.manager.currentConnectionState is ConnectionFailed }
            assertNull(fixture.manager.terminalSession.value)
            assertEquals(generation, fixture.serviceController.ownerConnectionGeneration())
            assertTrue(fixture.serviceContext.stoppedServices.isEmpty())

            fixture.await(timeoutMillis = 10_000) { fixture.manager.terminalSession.value != null }
            val termination = fixture.manager.terminalSession.value
            assertEquals(ConnectionTerminationReason.RetriesExhausted, termination?.reason)
            assertEquals(generation, termination?.generation)
            assertEquals(ConnectionTransport.BLUETOOTH, termination?.transport)
            assertFalse(termination?.sessionWasConnected ?: true)
            assertEquals(ConnectionTimeout, termination?.finalState)
            assertEquals(ConnectionTimeout, fixture.manager.currentConnectionState)
            assertNull(fixture.serviceController.ownerToken())
            assertEquals(1, fixture.serviceContext.stoppedServices.size)

            fixture.await { states.contains(ConnectionTimeout) }
            val disconnectedIndex = states.indexOfLast { it == Disconnected }
            val timeoutIndex = states.indexOfLast { it == ConnectionTimeout }
            assertTrue("terminal stream must retain Disconnected before ConnectionTimeout", disconnectedIndex >= 0 && timeoutIndex > disconnectedIndex)
            collector.cancel()
        } finally {
            fixture.prefs.connectionRetries = originalRetries
            fixture.prefs.bluetoothDeviceAddress = originalAddress
            fixture.close()
        }
    }

    @Test fun bluetoothTransientFailureThenSuccessKeepsServiceAndDoesNotPublishTerminalResult() = runBlocking {
        val fixture = Fixture()
        try {
            fixture.prefs.isWakeLockEnabled = true
            val generation = fixture.selectBluetoothSessionForTest()
            val owner = fixture.serviceController.ensureStarted(generation)
            fixture.emitBluetoothEvent(BluetoothConnectionEvent(generation, ConnectionFailed(IOException("temporary"))))
            fixture.await { fixture.manager.currentConnectionState is ConnectionFailed }

            assertNull(fixture.manager.terminalSession.value)
            assertEquals(owner, fixture.serviceController.ownerToken())
            assertTrue(fixture.serviceContext.stoppedServices.isEmpty())

            fixture.emitBluetoothEvent(BluetoothConnectionEvent(generation, Connected, sessionWasConnected = true))
            fixture.await { fixture.manager.currentConnectionState == Connected }

            assertNull(fixture.manager.terminalSession.value)
            assertEquals(owner, fixture.serviceController.ownerToken())
            assertEquals(Connected, fixture.manager.currentConnectionState)
            assertTrue(fixture.serviceContext.stoppedServices.isEmpty())
        } finally {
            fixture.close()
        }
    }

    @Test fun staleBluetoothTerminalEventCannotReplaceNewBluetoothGeneration() = runBlocking {
        val fixture = Fixture()
        try {
            fixture.prefs.isWakeLockEnabled = true
            val oldGeneration = fixture.selectBluetoothSessionForTest()
            fixture.serviceController.ensureStarted(oldGeneration)
            val newGeneration = fixture.selectBluetoothSessionForTest()
            fixture.serviceController.ensureStarted(newGeneration)
            fixture.emitBluetoothEvent(
                BluetoothConnectionEvent(
                    generation = oldGeneration,
                    state = ConnectionTimeout,
                    retriesExhausted = true,
                    sessionWasConnected = true,
                ),
            )
            delay(100)

            assertNull(fixture.manager.terminalSession.value)
            assertEquals(newGeneration, fixture.serviceController.ownerConnectionGeneration())
            assertTrue(fixture.manager.isConnectionRunning)
            assertTrue(fixture.serviceContext.stoppedServices.isEmpty())
        } finally {
            fixture.close()
        }
    }

    @Test fun exhaustedRetriesAfterAnEstablishedBluetoothSessionRemainClassifiedAsLoss() = runBlocking {
        val fixture = Fixture()
        try {
            fixture.prefs.isWakeLockEnabled = true
            val generation = fixture.selectBluetoothSessionForTest()
            fixture.serviceController.ensureStarted(generation)
            fixture.emitBluetoothEvent(BluetoothConnectionEvent(generation, Connected, sessionWasConnected = true))
            fixture.await { fixture.manager.currentConnectionState == Connected }
            fixture.emitBluetoothEvent(BluetoothConnectionEvent(generation, Disconnected, sessionWasConnected = true))
            fixture.emitBluetoothEvent(
                BluetoothConnectionEvent(generation, ConnectionTimeout, retriesExhausted = true, sessionWasConnected = true),
            )
            fixture.await { fixture.manager.terminalSession.value != null }

            val termination = fixture.manager.terminalSession.value
            assertEquals(ConnectionTerminationReason.RetriesExhausted, termination?.reason)
            assertTrue(termination?.sessionWasConnected == true)
            assertNull(fixture.serviceController.ownerToken())
            assertEquals(1, fixture.serviceContext.stoppedServices.size)
        } finally {
            fixture.close()
        }
    }

    @SdkSuppress(minSdkVersion = 35)
    @Test fun attachmentSynchronizationRetainsSessionForSamePhysicalDevice() = runBlocking {
        val fixture = Fixture()
        val device = usbDevice("/dev/bus/usb/001/055", 0x1234, 0x5678)
        try {
            fixture.attach(device.deviceId, device.toIdentity())
            fixture.confirmFirmware()

            val result = fixture.manager.synchronizeUsbAttachments(Result.success(listOf(device)))

            assertEquals(UsbConnectionRequest.Ignored, result)
            assertNull(fixture.manager.terminalSession.value)
            assertEquals(Connected, fixture.manager.currentConnectionState)
            assertTrue(fixture.manager.usbSessionLocked.value)
        } finally {
            fixture.close()
        }
    }

    @SdkSuppress(minSdkVersion = 35)
    @Test fun attachmentSynchronizationDetachesSameIdPhysicalReplacement() = runBlocking {
        val fixture = Fixture()
        val original = usbDevice("/dev/bus/usb/001/056", 0x1234, 0x5678)
        val replacement = usbDevice("/dev/bus/usb/001/056", 0x4321, 0x8765)
        try {
            assertEquals(original.deviceId, replacement.deviceId)
            fixture.attach(original.deviceId, original.toIdentity())
            fixture.confirmFirmware()
            val oldGeneration = fixture.manager.activeUsbSessionGeneration

            val result = fixture.manager.synchronizeUsbAttachments(Result.success(listOf(replacement)))

            assertEquals(UsbConnectionRequest.Ignored, result)
            assertEquals(ConnectionTerminationReason.UsbDetached, fixture.manager.terminalSession.value?.reason)
            assertEquals(oldGeneration, fixture.manager.terminalSession.value?.generation)
            assertEquals(Disconnected, fixture.manager.currentConnectionState)
            assertFalse(fixture.manager.usbSessionLocked.value)
            assertTrue(fixture.manager.activeUsbSessionGeneration != oldGeneration)
        } finally {
            fixture.close()
        }
    }

    @Test fun lateServiceDestroyCannotStopReplacementUsbSession() = runBlocking {
        val fixture = Fixture()
        try {
            fixture.prefs.isWakeLockEnabled = true
            fixture.attach(107)
            fixture.confirmFirmware()
            fixture.manager.onAppForegrounded()
            val oldOwner = fixture.serviceController.ownerToken()!!
            fixture.manager.onAppForegrounded()
            assertEquals(1, fixture.serviceContext.startedServices.size)

            assertTrue(fixture.manager.onUsbDeviceDetached(107))
            assertEquals(1, fixture.serviceContext.stoppedServices.size)
            fixture.attach(108)
            fixture.confirmFirmware()
            fixture.manager.onAppForegrounded()
            val replacementOwner = fixture.serviceController.ownerToken()!!
            assertTrue(oldOwner != replacementOwner)
            assertEquals(2, fixture.serviceContext.startedServices.size)

            fixture.manager.onConnectionServiceDestroyed(oldOwner)

            assertTrue(fixture.manager.isUsbConnected)
            assertTrue(fixture.manager.usbSessionLocked.value)
            assertEquals(replacementOwner, fixture.serviceController.ownerToken())
        } finally {
            fixture.close()
        }
    }

    @Test fun currentUsbServiceDestroyRestartsServiceWhileAppIsVisible() = runBlocking {
        val fixture = Fixture()
        try {
            fixture.prefs.isWakeLockEnabled = true
            fixture.attach(108)
            fixture.confirmFirmware()
            fixture.manager.onAppForegrounded()
            val oldOwner = fixture.serviceController.ownerToken()!!
            val generation = fixture.manager.activeUsbSessionGeneration

            fixture.manager.onConnectionServiceDestroyed(oldOwner)

            val replacementOwner = fixture.serviceController.ownerToken()
            assertNotNull(replacementOwner)
            assertTrue(oldOwner != replacementOwner)
            assertEquals(generation, fixture.serviceController.ownerConnectionGeneration())
            assertEquals(2, fixture.serviceContext.startedServices.size)
            assertTrue(fixture.manager.isUsbConnected)
            assertTrue(fixture.manager.usbSessionLocked.value)
            assertNull(fixture.manager.terminalSession.value)
        } finally {
            fixture.close()
        }
    }

    @Test fun currentUsbServiceDestroyEndsSessionWhenAppIsBackgrounded() = runBlocking {
        val fixture = Fixture()
        try {
            fixture.prefs.isWakeLockEnabled = true
            fixture.attach(109)
            fixture.confirmFirmware()
            fixture.manager.onAppForegrounded()
            val owner = fixture.serviceController.ownerToken()!!
            fixture.logger.startLogging()
            fixture.await { fixture.logger.isLoggerStarted }
            fixture.manager.onAppBackgrounded()

            fixture.manager.onConnectionServiceDestroyed(owner)
            fixture.await { !fixture.logger.isLoggerStarted }

            assertFalse(fixture.manager.isConnectionRunning)
            assertFalse(fixture.manager.isUsbConnected)
            assertFalse(fixture.manager.usbSessionLocked.value)
            assertFalse(fixture.logger.isLoggerStarted)
            assertNull(fixture.serviceController.ownerToken())
            assertEquals(ConnectionTerminationReason.ServiceStopped, fixture.manager.terminalSession.value?.reason)
            assertEquals(ConnectionTransport.USB, fixture.manager.terminalSession.value?.transport)
            assertTrue(fixture.manager.terminalSession.value?.sessionWasConnected == true)
        } finally {
            fixture.close()
        }
    }

    @Test fun staleBluetoothServiceOwnerCannotStopUnconfirmedUsbReplacement() = runBlocking {
        val fixture = Fixture()
        try {
            fixture.attach(109)
            fixture.publishConnectionStateForTest(InProgress)
            val usbGeneration = fixture.manager.activeUsbSessionGeneration!!
            fixture.serviceController.ensureStarted(usbGeneration - 1) // previous Bluetooth session owner

            fixture.manager.onAppForegrounded()

            assertEquals(109, fixture.manager.activeUsbDeviceIdOrNull)
            assertTrue(fixture.manager.isUsbRunning)
            assertNull(fixture.serviceController.ownerToken())
        } finally {
            fixture.close()
        }
    }

    @Test fun failedForegroundStartReleasesOwnerForResumeRetry() {
        val fixture = Fixture()
        try {
            fixture.serviceContext.failNextStart = true
            runCatching { fixture.serviceController.ensureStarted(17) }
            assertNull(fixture.serviceController.ownerToken())

            val retry = fixture.serviceController.ensureStarted(17)

            assertEquals(retry, fixture.serviceController.ownerToken())
            assertEquals(1, fixture.serviceContext.startedServices.size)
        } finally {
            fixture.close()
        }
    }

    @Test fun bluetoothBackgroundPreferenceControlsServiceAndOwnedCleanup() {
        val fixture = Fixture()
        try {
            fixture.manager.onAppForegrounded()
            fixture.selectBluetoothSessionForTest()
            fixture.manager.onBackgroundWorkPreferenceChanged(false)
            assertTrue(fixture.serviceContext.startedServices.isEmpty())
            assertTrue(fixture.manager.isConnectionRunning)

            fixture.manager.onBackgroundWorkPreferenceChanged(true)
            assertNotNull(fixture.serviceController.ownerToken())
            assertEquals(1, fixture.serviceContext.startedServices.size)

            fixture.manager.onBackgroundWorkPreferenceChanged(false)

            assertNull(fixture.serviceController.ownerToken())
            assertTrue(fixture.manager.isConnectionRunning)
            assertEquals(1, fixture.serviceContext.stoppedServices.size)
        } finally {
            fixture.close()
        }
    }

    @Test fun backgroundPreferenceEndsBtOnlyWhenTheAppActuallyLeavesForeground() {
        val fixture = Fixture()
        try {
            fixture.selectBluetoothSessionForTest()
            fixture.manager.onAppForegrounded()
            fixture.manager.onBackgroundWorkPreferenceChanged(false)

            assertTrue(fixture.manager.isConnectionRunning)
            fixture.manager.onAppBackgrounded()

            assertFalse(fixture.manager.isConnectionRunning)
            assertEquals(ConnectionTerminationReason.Backgrounded, fixture.manager.terminalSession.value?.reason)
            assertEquals(ConnectionTransport.BLUETOOTH, fixture.manager.terminalSession.value?.transport)
        } finally {
            fixture.close()
        }
    }

    @Test fun enabledBackgroundPreferencePreservesBtSessionAndServiceOnBackground() {
        val fixture = Fixture()
        try {
            fixture.prefs.isWakeLockEnabled = true
            val generation = fixture.selectBluetoothSessionForTest()
            fixture.manager.onAppForegrounded()
            val owner = fixture.serviceController.ownerToken()
            assertNotNull(owner)
            assertEquals(generation, fixture.serviceController.ownerConnectionGeneration())

            fixture.manager.onAppBackgrounded()

            assertTrue(fixture.manager.isConnectionRunning)
            assertEquals(owner, fixture.serviceController.ownerToken())
            assertNull(fixture.manager.terminalSession.value)
        } finally {
            fixture.close()
        }
    }

    @Test fun lateDestroyFromPreviousBluetoothSessionCannotStopReplacement() {
        val fixture = Fixture()
        try {
            fixture.prefs.isWakeLockEnabled = true
            fixture.selectBluetoothSessionForTest()
            fixture.manager.onAppForegrounded()
            val oldOwner = fixture.serviceController.ownerToken()!!

            fixture.selectBluetoothSessionForTest()
            fixture.manager.onConnectionStateObservedInForeground(Connected)
            val replacementOwner = fixture.serviceController.ownerToken()!!
            assertTrue(oldOwner != replacementOwner)

            fixture.manager.onConnectionServiceDestroyed(oldOwner)

            assertTrue(fixture.manager.isConnectionRunning)
            assertEquals(replacementOwner, fixture.serviceController.ownerToken())
        } finally {
            fixture.close()
        }
    }

    @Test fun lateDestroyAfterStopAndRestartCannotStopNewBluetoothSession() {
        val fixture = Fixture()
        try {
            fixture.prefs.isWakeLockEnabled = true
            fixture.selectBluetoothSessionForTest()
            fixture.manager.onAppForegrounded()
            val stoppedOwner = fixture.serviceController.ownerToken()!!
            assertTrue(fixture.manager.stopConnection())

            fixture.selectBluetoothSessionForTest()
            fixture.manager.onAppForegrounded()
            val replacementOwner = fixture.serviceController.ownerToken()!!

            fixture.manager.onConnectionServiceDestroyed(stoppedOwner)

            assertTrue(fixture.manager.isConnectionRunning)
            assertEquals(replacementOwner, fixture.serviceController.ownerToken())
        } finally {
            fixture.close()
        }
    }

    @Test fun currentBluetoothServiceDestroyStopsItsOwnedSessionInBackground() {
        val fixture = Fixture()
        try {
            fixture.prefs.isWakeLockEnabled = true
            fixture.selectBluetoothSessionForTest()
            fixture.manager.onAppForegrounded()
            val owner = fixture.serviceController.ownerToken()!!
            fixture.manager.onAppBackgrounded()

            fixture.manager.onConnectionServiceDestroyed(owner)

            assertFalse(fixture.manager.isConnectionRunning)
            assertNull(fixture.serviceController.ownerToken())
            assertEquals(ConnectionTerminationReason.ServiceStopped, fixture.manager.terminalSession.value?.reason)
            assertEquals(ConnectionTransport.BLUETOOTH, fixture.manager.terminalSession.value?.transport)
        } finally {
            fixture.close()
        }
    }

    @Test fun currentBluetoothServiceDestroyRestartsServiceWhileAppIsVisible() {
        val fixture = Fixture()
        try {
            fixture.prefs.isWakeLockEnabled = true
            val generation = fixture.selectBluetoothSessionForTest()
            fixture.manager.onAppForegrounded()
            val owner = fixture.serviceController.ownerToken()!!

            fixture.manager.onConnectionServiceDestroyed(owner)

            val replacementOwner = fixture.serviceController.ownerToken()
            assertNotNull(replacementOwner)
            assertTrue(owner != replacementOwner)
            assertEquals(generation, fixture.serviceController.ownerConnectionGeneration())
            assertEquals(2, fixture.serviceContext.startedServices.size)
            assertTrue(fixture.manager.isConnectionRunning)
            assertNull(fixture.manager.terminalSession.value)
        } finally {
            fixture.close()
        }
    }

    @Test fun unavailableAttachmentSnapshotDoesNotTreatUsbAsDetached() = runBlocking {
        val fixture = Fixture()
        try {
            fixture.attach(78)
            fixture.confirmFirmware()

            val terminalBefore = fixture.manager.terminalSession.value
            val whileConnected = fixture.manager.synchronizeUsbAttachments(Result.failure(SecurityException("USB list unavailable")))
            assertEquals(UsbConnectionRequest.Ignored, whileConnected)
            assertSame(terminalBefore, fixture.manager.terminalSession.value)
            assertTrue(fixture.manager.isUsbConnected)
            assertEquals(Connected, fixture.manager.currentConnectionState)
            assertNull(fixture.manager.terminalSession.value)
        } finally {
            fixture.close()
        }
    }

    @Test fun staleUsbPacketsCannotConfirmOrNavigateAReplacementRetryAttempt() = runBlocking {
        val fixture = Fixture()
        try {
            fixture.attach(91)
            fixture.attemptPackets.emit(UsbAttemptPacket(9, firmwarePacket()))
            delay(100)
            assertFalse(fixture.manager.isUsbConnected)

            fixture.confirmFirmware(10)
            assertEquals(10L, fixture.manager.activeUsbHomeGeneration)
            fixture.beginUsbRetry(11)
            fixture.attemptPackets.emit(UsbAttemptPacket(10, firmwarePacket()))
            delay(100)
            assertFalse(fixture.manager.isUsbConnected)
            assertNull(fixture.manager.activeUsbHomeGeneration)

            fixture.confirmFirmware(11)
            assertTrue(fixture.manager.isUsbConnected)
            assertEquals(11L, fixture.manager.activeUsbHomeGeneration)
        } finally {
            fixture.close()
        }
    }

    @Test fun backgroundPolicyStopBypassesUsbManualStopGuardAndPublishesTypedReason() = runBlocking {
        val fixture = Fixture()
        try {
            fixture.attach(95)
            fixture.confirmFirmware()
            assertFalse(fixture.manager.stopConnection())

            fixture.manager.onAppBackgrounded()

            assertFalse(fixture.manager.isConnectionRunning)
            assertFalse(fixture.manager.usbSessionLocked.value)
            assertEquals(ConnectionTerminationReason.Backgrounded, fixture.manager.terminalSession.value?.reason)
            assertEquals(ConnectionTransport.USB, fixture.manager.terminalSession.value?.transport)
        } finally {
            fixture.close()
        }
    }

    @Test fun reattachedDeviceWithTheSameIdNeedsFreshFirmware() = runBlocking {
        val fixture = Fixture()
        try {
            fixture.attach(101)
            fixture.confirmFirmware()
            assertTrue(fixture.manager.onUsbDeviceDetached(101))
            fixture.attach(101)
            assertFalse(fixture.manager.isConnected)
            assertNull(fixture.manager.fwInfo)

            fixture.confirmFirmware()
            assertTrue(fixture.manager.isUsbConnected)
            assertFalse(fixture.manager.stopConnection())
        } finally {
            fixture.close()
        }
    }

    private class Fixture {
        private val context = InstrumentationRegistry.getInstrumentation().targetContext
        val prefs = UserPrefs(context)
        private val originalWakeLockPreference = prefs.isWakeLockEnabled
        init {
            prefs.isWakeLockEnabled = false
        }
        private val usb = UsbConnection(context.getSystemService(Context.USB_SERVICE) as UsbManager, prefs)
        private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        private val bt = BtConnection(prefs, bluetoothManager)
        val logger = SecuLogger(prefs, FileHelper(context))
        val serviceContext = RecordingServiceContext(context)
        val serviceController = ConnectionServiceController(serviceContext, ConnectionNotificationPublisher(serviceContext))
        val manager = Secu3ConnectionManager(usb, bt, prefs, bluetoothManager, logger, serviceController)
        val writes = CopyOnWriteArrayList<ByteArray>()
        val attemptPackets: MutableSharedFlow<UsbAttemptPacket> = usb.javaClass.getDeclaredField("_attemptPackets").apply { isAccessible = true }.get(usb) as MutableSharedFlow<UsbAttemptPacket>
        val btStates: MutableSharedFlow<ConnectionState> = connectionField("mConnectionStateFlow").get(bt) as MutableSharedFlow<ConnectionState>
        @Suppress("UNCHECKED_CAST")
        private val btEvents: MutableSharedFlow<BluetoothConnectionEvent> = bt.javaClass.getDeclaredField("_sessionEvents").apply { isAccessible = true }.get(bt) as MutableSharedFlow<BluetoothConnectionEvent>

        // The endpoint injection matches existing transport integration fixtures. It bypasses only
        // Android's physical USB opening; packet parsing, session identity and send gates are real.
        suspend fun attach(deviceId: Int, activeIdentity: UsbAttachmentIdentity? = null) {
            val open = AtomicBoolean(true)
            val port = Proxy.newProxyInstance(UsbSerialPort::class.java.classLoader, arrayOf(UsbSerialPort::class.java)) { proxy, method, args ->
                when (method.name) {
                    "isOpen" -> open.get()
                    "close" -> { open.set(false); null }
                    "write" -> { writes += (args!![0] as ByteArray).copyOf(); null }
                    "toString" -> "isolated-usb-session-test"
                    "hashCode" -> System.identityHashCode(proxy)
                    "equals" -> proxy === args?.get(0)
                    else -> null
                }
            } as UsbSerialPort
            connectionField("endpoint").set(usb, port)
            connectionField("isRunning").setBoolean(usb, true)
            val fence = managerField("usbSessionFence").get(manager) as UsbSessionFence
            val generation = fence.begin(deviceId)
            managerField("activeUsbAttachment").set(manager, activeIdentity)
            val selectionClass = Secu3ConnectionManager::class.java.declaredClasses.first { it.simpleName == "Selection" }
            val transportClass = Secu3ConnectionManager::class.java.declaredClasses.first { it.simpleName == "Transport" }
            val usbTransport = transportClass.enumConstants!!.first { (it as Enum<*>).name == "USB" }
            val selection = selectionClass.declaredConstructors.first().apply { isAccessible = true }.newInstance(usbTransport, generation)
            val subscribed = CompletableDeferred<Unit>()
            @Suppress("UNCHECKED_CAST")
            val barriers = managerField("usbSelectionReady").get(manager) as MutableMap<Long, CompletableDeferred<Unit>>
            synchronized(barriers) { barriers[generation] = subscribed }
            @Suppress("UNCHECKED_CAST")
            val selected = managerField("selectedTransport").get(manager) as MutableStateFlow<Any>
            selected.value = selection
            managerField("activeUsbAttemptId").set(manager, 1L)
            withTimeout(3_000) { subscribed.await() }
            synchronized(barriers) { barriers.remove(generation) }
            await { btEvents.subscriptionCount.value > 0 }
        }

        suspend fun confirmFirmware(attemptId: Long = 1L) {
            managerField("activeUsbAttemptId").set(manager, attemptId)
            attemptPackets.emit(UsbAttemptPacket(attemptId, firmwarePacket()))
            await { manager.isUsbConnected && manager.usbSessionLocked.value }
        }

        fun beginUsbRetry(attemptId: Long) {
            val deviceId = manager.activeUsbDeviceIdOrNull!!
            val generation = manager.activeUsbSessionGeneration!!
            (managerField("usbSessionFence").get(manager) as UsbSessionFence).invalidateConfirmation(deviceId, generation)
            managerField("activeUsbAttemptId").set(manager, attemptId)
            managerField("confirmedUsbHomeAttemptId").set(manager, null)
            managerField("_usbSessionLocked").get(manager).let { flow ->
                (flow as MutableStateFlow<Boolean>).value = false
            }
            val publish = Secu3ConnectionManager::class.java.getDeclaredMethod("publishConnectionState", ConnectionState::class.java).apply { isAccessible = true }
            publish.invoke(manager, InProgress)
        }

        suspend fun await(timeoutMillis: Long = 3_000, predicate: () -> Boolean) {
            withTimeout(timeoutMillis) { while (!predicate()) delay(10) }
        }

        fun close() {
            if (!manager.stopConnection()) manager.activeUsbDeviceIdOrNull?.let(manager::onUsbDeviceDetached)
            serviceController.stop()
            prefs.isWakeLockEnabled = originalWakeLockPreference
            (managerField("scope").get(manager) as CoroutineScope).cancel()
            usb.cancelScopes()
            bt.cancelScopes()
            (SecuLogger::class.java.getDeclaredField("loggerScope").apply { isAccessible = true }.get(logger) as CoroutineScope).cancel()
        }

        fun attachmentGate() = managerField("usbAttachmentAttempts").get(manager) as UsbAttachmentAttemptGate

        fun publishConnectionStateForTest(state: ConnectionState) {
            val publish = Secu3ConnectionManager::class.java.getDeclaredMethod("publishConnectionState", ConnectionState::class.java).apply { isAccessible = true }
            publish.invoke(manager, state)
        }

        fun selectBluetoothSessionForTest(): Long {
            val generation = (managerField("usbSessionFence").get(manager) as UsbSessionFence).advance()
            val selectionClass = Secu3ConnectionManager::class.java.declaredClasses.first { it.simpleName == "Selection" }
            val transportClass = Secu3ConnectionManager::class.java.declaredClasses.first { it.simpleName == "Transport" }
            val bluetoothTransport = transportClass.enumConstants!!.first { (it as Enum<*>).name == "BLUETOOTH" }
            val selection = selectionClass.declaredConstructors.first().apply { isAccessible = true }.newInstance(bluetoothTransport, generation)
            @Suppress("UNCHECKED_CAST")
            val selected = managerField("selectedTransport").get(manager) as MutableStateFlow<Any>
            selected.value = selection
            managerField("activeUsbAttachment").set(manager, null)
            managerField("currentConnectionState").set(manager, Connected)
            connectionField("isRunning").setBoolean(bt, true)
            return generation
        }

        fun startBluetoothTransport(generation: Long) = bt.startConnection(generation)

        suspend fun emitBluetoothEvent(event: BluetoothConnectionEvent) = btEvents.emit(event)

        private fun managerField(name: String) = Secu3ConnectionManager::class.java.getDeclaredField(name).apply { isAccessible = true }
        private fun connectionField(name: String) = Connection::class.java.getDeclaredField(name).apply { isAccessible = true }
    }

    private class RecordingServiceContext(base: Context) : ContextWrapper(base) {
        val startedServices = mutableListOf<Intent>()
        val stoppedServices = mutableListOf<Intent>()
        var failNextStart = false

        override fun startForegroundService(service: Intent): ComponentName? {
            if (failNextStart) { failNextStart = false; throw IllegalStateException("service start failed") }
            startedServices += Intent(service)
            return service.component
        }

        override fun startService(service: Intent): ComponentName? {
            if (failNextStart) { failNextStart = false; throw IllegalStateException("service start failed") }
            startedServices += Intent(service)
            return service.component
        }

        override fun stopService(name: Intent): Boolean {
            stoppedServices += Intent(name)
            return true
        }
    }

    private fun UsbDevice.toIdentity() = UsbAttachmentIdentity(deviceId, deviceName, vendorId, productId)

    /** Parcel layout verified against AOSP API 35 and the local API 36 source; callers require API 35+. */
    private fun usbDevice(name: String, vendorId: Int, productId: Int): UsbDevice {
        val parcel = Parcel.obtain()
        return try {
            parcel.writeString(name)
            parcel.writeInt(vendorId)
            parcel.writeInt(productId)
            parcel.writeInt(0)
            parcel.writeInt(0)
            parcel.writeInt(0)
            parcel.writeString(null)
            parcel.writeString(null)
            parcel.writeString("1.0")
            parcel.writeStrongBinder(Binder())
            parcel.writeParcelableArray(emptyArray<UsbConfiguration>(), 0)
            repeat(5) { parcel.writeInt(0) }
            parcel.setDataPosition(0)
            UsbDevice.CREATOR.createFromParcel(parcel)
        } finally {
            parcel.recycle()
        }
    }

    companion object {
        private fun firmwarePacket() = RawPacket(
            intArrayOf('@'.code, 'y'.code) + "SECU-3 test firmware".padEnd(52).map { it.code }.toIntArray() +
                intArrayOf(0, 0, 0, 0, 0x10, 0, 0),
        )
    }
}
