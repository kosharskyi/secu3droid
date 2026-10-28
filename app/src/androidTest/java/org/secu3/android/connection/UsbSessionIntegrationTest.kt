package org.secu3.android.connection

import android.bluetooth.BluetoothManager
import android.content.ComponentName
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.app.DownloadManager
import android.location.LocationManager
import android.hardware.usb.UsbConfiguration
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Binder
import android.os.Parcel
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import androidx.lifecycle.ViewModelStore
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import com.hoho.android.usbserial.driver.UsbSerialPort
import java.io.IOException
import java.lang.reflect.Proxy
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.emitAll
import java.util.UUID
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.secu3.android.models.RawPacket
import org.secu3.android.models.packets.input.FirmwareInfoPacket
import org.secu3.android.utils.FileHelper
import org.secu3.android.utils.SecuLogger
import org.secu3.android.utils.Task
import org.secu3.android.utils.UserPrefs
import org.secu3.android.utils.IsolatedUserPrefs
import org.secu3.android.utils.AppPrefs
import org.secu3.android.ui.messages.UiMessageQueue
import org.secu3.android.ui.start.StartScreenViewModel
import org.secu3.android.ui.home.HomeViewModel
import org.secu3.android.network.releases.ReleaseRepository
import org.secu3.android.network.ApiService
import org.secu3.android.network.models.GitHubRelease
import retrofit2.Response

/** Exercises the production coordinator with an isolated serial port, never a physical ECU. */
class UsbSessionIntegrationTest {
    @Test fun startProgressTracksCancellablePreparationAndTerminalManagerEvent() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val addressKey = stringPreferencesKey(context.getString(org.secu3.android.R.string.pref_bluetooth_device_address_key))
        val store = DelayedPreferencesDataStore(mutablePreferencesOf(addressKey to "02:00:00:00:00:01"))
        val fixture = Fixture(delayedStore = store)
        val viewModel = StartScreenViewModel(
            fixture.prefs,
            context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager,
            context.getSystemService(Context.LOCATION_SERVICE) as LocationManager,
            fixture.manager,
            context.packageManager,
            context.getSystemService(Context.USB_SERVICE) as UsbManager,
            UiMessageQueue(),
        )
        val viewModelStore = ViewModelStore()
        InstrumentationRegistry.getInstrumentation().runOnMainSync { viewModelStore.put("start", viewModel) }
        try {
            store.initialReadStarted.await()
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                viewModel.startBluetoothConnection()
                assertTrue("preference preparation must immediately show Cancel", viewModel.uiState.value.connecting)
            }

            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                viewModel.cancelConnection()
                assertFalse(viewModel.uiState.value.connecting)
                viewModel.startBluetoothConnection()
                assertTrue("replacement preparation must remain visible while the same preference read is gated", viewModel.uiState.value.connecting)
                viewModel.cancelConnection()
                assertFalse(viewModel.uiState.value.connecting)
            }

            store.releaseInitialRead.complete(Unit)
            delay(100)
            assertEquals("NONE", fixture.selectedTransport())
            assertFalse(fixture.manager.isBtRunning)
            fixture.selectBluetoothSessionForTest()
            fixture.publishConnectionStateForTest(InProgress)
            fixture.await { viewModel.uiState.value.connecting }

            fixture.publishConnectionStateForTest(ConnectionTimeout)
            fixture.await { !viewModel.uiState.value.connecting }
            assertTrue("terminal status clears progress even before transport running state changes", fixture.manager.isConnectionRunning)
        } finally {
            InstrumentationRegistry.getInstrumentation().runOnMainSync { viewModelStore.clear() }
            fixture.close()
        }
    }

    @Test fun homeDoesNotPublishNavigationWhenConfirmedUsbStopIsRejected() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val fixture = Fixture()
        val appPrefsFixture = IsolatedUserPrefs(context, "home-usb-exit-${UUID.randomUUID()}")
        val appPrefs = AppPrefs(appPrefsFixture.dataStore)
        val api = object : ApiService {
            override suspend fun getReleases(): Result<List<GitHubRelease>> = Result.success(emptyList())
            override suspend fun getLatestRelease(): Response<GitHubRelease> = throw IOException("Offline test")
        }
        val viewModel = HomeViewModel(
            fixture.manager,
            ReleaseRepository(api, context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager, appPrefs,
                true, "0.19.2-debug", { org.threeten.bp.LocalDate.now() }),
            UiMessageQueue(),
        )
        val viewModelStore = ViewModelStore()
        InstrumentationRegistry.getInstrumentation().runOnMainSync { viewModelStore.put("home", viewModel) }
        try {
            fixture.attach(96)
            fixture.confirmFirmware()
            assertTrue(fixture.manager.isUsbSessionLocked)
            assertFalse(fixture.manager.stopConnection())

            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                viewModel.confirmExit()
                assertFalse("a rejected confirmed-USB stop must not request Start navigation", viewModel.returnToStart.value)
            }
        } finally {
            InstrumentationRegistry.getInstrumentation().runOnMainSync { viewModelStore.clear() }
            fixture.close()
            appPrefsFixture.close()
        }
    }

    @Test fun firmwareStateReplaysCurrentUsbFirmwareAndClearsOnSessionReplacement() = runBlocking {
        val fixture = Fixture()
        try {
            assertNull(fixture.manager.firmwareInfo.value)
            fixture.attach(74)
            fixture.confirmFirmware()
            val firstFirmware = fixture.manager.fwInfo
            assertNotNull(firstFirmware)
            assertSame(firstFirmware, fixture.manager.firmwareInfo.first())

            assertTrue(fixture.manager.onUsbDeviceDetached(74))
            assertNull(fixture.manager.firmwareInfo.value)
            assertNull(fixture.manager.fwInfo)

            fixture.attach(75)
            assertNull(fixture.manager.firmwareInfo.first())
            fixture.confirmFirmware()
            assertNotNull(fixture.manager.firmwareInfo.value)
            assertSame(fixture.manager.fwInfo, fixture.manager.firmwareInfo.value)
            assertTrue(firstFirmware !== fixture.manager.firmwareInfo.value)
        } finally {
            fixture.close()
        }
    }

    @Test fun firmwareStateClearsWhenBluetoothSessionStops() = runBlocking {
        val fixture = Fixture()
        try {
            fixture.selectBluetoothSessionForTest()
            fixture.await { fixture.btPackets.subscriptionCount.value > 0 }
            fixture.btPackets.emit(firmwarePacket())
            fixture.await { fixture.manager.firmwareInfo.value != null }
            assertSame(fixture.manager.fwInfo, fixture.manager.firmwareInfo.first())

            assertTrue(fixture.manager.stopConnection())
            assertNull(fixture.manager.firmwareInfo.value)
            assertNull(fixture.manager.fwInfo)
        } finally {
            fixture.close()
        }
    }

    @Test fun requestSubscribesBeforeSendingAndReceivesImmediateFirmwareReply() = runBlocking {
        val fixture = Fixture()
        try {
            fixture.attach(73)
            val existingSubscribers = fixture.attemptPackets.subscriptionCount.value
            var requestSent = false

            val reply = withTimeout(3_000) {
                fixture.manager.receivedPacketFlow {
                    assertTrue(fixture.attemptPackets.subscriptionCount.value > existingSubscribers)
                    requestSent = true
                    launch(start = CoroutineStart.UNDISPATCHED) {
                        fixture.attemptPackets.emit(UsbAttemptPacket(1L, firmwarePacket()))
                    }
                }.first()
            }

            assertTrue(requestSent)
            assertTrue(reply is FirmwareInfoPacket)
            assertTrue(fixture.manager.isUsbConnected)
        } finally {
            fixture.close()
        }
    }

    @Test fun requestWithoutSelectedTransportWaitsWithoutSending() = runBlocking {
        val fixture = Fixture()
        try {
            var requestSent = false
            val reply = withTimeoutOrNull(100) {
                fixture.manager.receivedPacketFlow { requestSent = true }.first()
            }

            assertNull(reply)
            assertFalse(requestSent)
        } finally {
            fixture.close()
        }
    }

    @Test fun usbOpenFailureEmitsAttemptIdAndManagerRetriesThatAttempt() = runBlocking {
        val fixture = Fixture()
        val device = usbDevice("/dev/bus/usb/099/001", 0x1234, 0x5678)
        val packetCollector = launch(start = CoroutineStart.UNDISPATCHED) {
            fixture.manager.receivedPacketFlow.collect { }
        }
        try {
            fixture.manager.onAppForegrounded()
            val permission = fixture.requestPermissionOrStart(device, hasPermission = false)!!
            val observedFailures = async(start = CoroutineStart.UNDISPATCHED) {
                withTimeout(10_000) { fixture.attemptFailures.take(2).toList() }
            }
            val observedConnectionFailure = async(start = CoroutineStart.UNDISPATCHED) {
                withTimeout(8_000) { fixture.usb.connectionStateFlow.filterIsInstance<ConnectionFailed>().first() }
            }
            fixture.await { fixture.attemptFailures.subscriptionCount.value >= 2 }
            fixture.completePendingPermission(
                device = device,
                token = permission.token,
                permissionGranted = true,
                attachedDevice = device,
                hasPermission = true,
                supported = true,
                conflictsWithActiveDevice = false,
            )

            assertEquals(listOf(1L, 2L), observedFailures.await())
            assertEquals("No USB devices found", observedConnectionFailure.await().e.message)
        } finally {
            packetCollector.cancel()
            fixture.close()
        }
    }

    @Test fun managerIgnoresFailureFromStaleUsbAttempt() = runBlocking {
        val fixture = Fixture()
        val signals = Channel<Any>(Channel.UNLIMITED)
        try {
            fixture.attach(72)
            fixture.setUsbAttemptSignals(signals)
            fixture.handleUsbAttemptFailure(2L)
            assertFalse(signals.tryReceive().isSuccess)

            fixture.handleUsbAttemptFailure(1L)
            val signal = withTimeout(3_000) { signals.receive() }
            assertEquals("FAILED", (signal as Enum<*>).name)
        } finally {
            fixture.close()
        }
    }

    @Test fun usbFirmwareHandshakeTimeoutRetriesWithoutEndingAttachmentSession() = runBlocking {
        val fixture = Fixture()
        val device = usbDevice("/dev/bus/usb/099/002", 0x1234, 0x5678)
        val packetCollector = launch(start = CoroutineStart.UNDISPATCHED) {
            fixture.manager.receivedPacketFlow.collect { }
        }
        try {
            // Keep the production manager retry loop active while preventing Android USB I/O.
            fixture.usb.cancelScopes()
            fixture.manager.onAppForegrounded()
            fixture.requestPermissionOrStart(device, hasPermission = true)
            fixture.await { fixture.activeUsbAttemptId() == 1L }
            val sessionGeneration = fixture.manager.activeUsbSessionGeneration
            val startedAt = System.nanoTime()

            fixture.await(timeoutMillis = 10_000) { fixture.activeUsbAttemptId() == 2L }

            val elapsedMillis = (System.nanoTime() - startedAt) / 1_000_000
            assertTrue("Handshake timeout should wait for the configured deadline", elapsedMillis >= 5_000)
            assertEquals(sessionGeneration, fixture.manager.activeUsbSessionGeneration)
            assertNull(fixture.manager.fwInfo)
            assertNull(fixture.manager.terminalSessionGeneration.value)
        } finally {
            packetCollector.cancel()
            fixture.close()
        }
    }

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
            assertTrue(fixture.manager.isUsbSessionLocked)

            assertTrue(fixture.manager.onUsbDeviceDetached(71))
            assertFalse(fixture.manager.isConnected)
            assertFalse(fixture.manager.isUsbSessionLocked)
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
            assertTrue(fixture.manager.isUsbSessionLocked)
            assertTrue(fixture.manager.isUsbConnected)
        } finally {
            fixture.close()
        }
    }

    @Test fun confirmedUsbRemainsLockedUntilRetryCleanupWhenEndpointCloses() = runBlocking {
        val fixture = Fixture()
        try {
            fixture.attach(82)
            fixture.confirmFirmware()

            synchronized(fixture.manager) {
                fixture.closeUsbEndpoint()
                assertTrue(fixture.manager.isUsbSessionLocked)
                assertFalse(fixture.manager.isUsbConnected)
                assertFalse(fixture.manager.stopConnection())
            }
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

            assertNull(fixture.manager.synchronizeUsbAttachments(Result.success(emptyList())))
            assertEquals(endedGeneration, fixture.manager.terminalSessionGeneration.value)
            assertEquals(Disconnected, fixture.manager.currentConnectionState)
            assertFalse(fixture.manager.isUsbSessionLocked)

            assertNull(fixture.manager.synchronizeUsbAttachments(Result.success(emptyList())))
            assertEquals(endedGeneration, fixture.manager.terminalSessionGeneration.value)
        } finally {
            fixture.close()
        }
    }

    @Test fun usbDetachStopsOwnedServiceImmediatelyAndPublishesTerminalGeneration() = runBlocking {
        val fixture = Fixture()
        try {
            fixture.setWakeLockPreference(true)
            fixture.attach(80)
            fixture.confirmFirmware()
            fixture.manager.onAppForegrounded()
            val generation = fixture.manager.activeUsbSessionGeneration!!
            assertNotNull(fixture.serviceController.ownerToken())

            assertTrue(fixture.manager.onUsbDeviceDetached(80))

            assertEquals(1, fixture.serviceContext.stoppedServices.size)
            assertEquals(generation, fixture.manager.terminalSessionGeneration.value)
        } finally {
            fixture.close()
        }
    }

    @Test fun delayedFirstBackgroundPolicyUsesTheRealBackgroundTransition() = runBlocking {
        for (savedEnabled in listOf(true, false)) {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            val policyKey = booleanPreferencesKey(context.getString(org.secu3.android.R.string.pref_wakelock_key))
            val store = DelayedPreferencesDataStore(mutablePreferencesOf(policyKey to savedEnabled))
            val fixture = Fixture(delayedStore = store)
            try {
                store.initialReadStarted.await()
                val generation = fixture.selectBluetoothSessionForTest()
                fixture.manager.onAppBackgrounded()
                assertNull("policy must remain unknown until DataStore's first read", fixture.backgroundPolicy())
                assertNull(fixture.manager.terminalSessionGeneration.value)

                store.releaseInitialRead.complete(Unit)
                fixture.await { fixture.backgroundPolicy() == savedEnabled }
                if (savedEnabled) {
                    assertTrue("saved background permission keeps an active retry session", fixture.manager.isConnectionRunning)
                    assertNull(fixture.manager.terminalSessionGeneration.value)
                } else {
                    fixture.await { fixture.manager.terminalSessionGeneration.value == generation }
                    assertFalse(fixture.manager.isConnectionRunning)
                }
            } finally {
                fixture.close()
            }
        }
    }

    @Test fun canceledBluetoothPreflightCannotSelectTransportAfterPreferencesLoad() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val addressKey = androidx.datastore.preferences.core.stringPreferencesKey(
            context.getString(org.secu3.android.R.string.pref_bluetooth_device_address_key),
        )
        val store = DelayedPreferencesDataStore(mutablePreferencesOf(addressKey to "02:00:00:00:00:01"))
        val fixture = Fixture(delayedStore = store)
        try {
            store.initialReadStarted.await()
            val preflight = launch(start = CoroutineStart.UNDISPATCHED) { fixture.manager.startBtConnection() }
            fixture.manager.stopConnection()
            store.releaseInitialRead.complete(Unit)
            preflight.join()
            assertFalse(fixture.manager.isBtRunning)
            assertEquals(Disconnected, fixture.manager.currentConnectionState)
            assertEquals("NONE", fixture.selectedTransport())
        } finally {
            fixture.close()
        }
    }

    @Test fun managerBackgroundPolicyRecoversWhenSameValueReturnsAfterReadFailure() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val key = booleanPreferencesKey(context.getString(org.secu3.android.R.string.pref_wakelock_key))
        val store = DelayedPreferencesDataStore(mutablePreferencesOf(key to true)).apply {
            failOnceAfterInitialEmission = true
        }
        val fixture = Fixture(delayedStore = store)
        try {
            store.initialReadStarted.await()
            store.releaseInitialRead.complete(Unit)
            fixture.await { fixture.backgroundPolicy() == null }
            fixture.await(timeoutMillis = 5_000) { fixture.backgroundPolicy() == true }
        } finally {
            fixture.close()
        }
    }

    @Test fun bluetoothRetryExhaustionStopsServiceAndPublishesFinalTimeout() = runBlocking {
        val fixture = Fixture()
        val originalRetries = fixture.connectionRetriesPreference()
        val originalAddress = fixture.bluetoothAddressPreference()
        try {
            fixture.setWakeLockPreference(true)
            fixture.setConnectionRetriesPreference(2)
            fixture.setBluetoothAddressPreference("02:00:00:00:00:01")
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
            assertNull(fixture.manager.terminalSessionGeneration.value)
            assertEquals(generation, fixture.serviceController.ownerConnectionGeneration())
            assertTrue(fixture.serviceContext.stoppedServices.isEmpty())

            fixture.await(timeoutMillis = 10_000) { fixture.manager.terminalSessionGeneration.value != null }
            assertEquals(generation, fixture.manager.terminalSessionGeneration.value)
            assertEquals(ConnectionTimeout, fixture.manager.currentConnectionState)
            assertNull(fixture.serviceController.ownerToken())
            assertEquals(1, fixture.serviceContext.stoppedServices.size)

            fixture.await { states.contains(ConnectionTimeout) }
            val disconnectedIndex = states.indexOfLast { it == Disconnected }
            val timeoutIndex = states.indexOfLast { it == ConnectionTimeout }
            assertTrue("terminal stream must retain Disconnected before ConnectionTimeout", disconnectedIndex >= 0 && timeoutIndex > disconnectedIndex)
            collector.cancel()
        } finally {
            fixture.setConnectionRetriesPreference(originalRetries)
            fixture.setBluetoothAddressPreference(originalAddress)
            fixture.close()
        }
    }

    @Test fun replacingBluetoothConnectionJobCancelsOldRetryLoop() = runBlocking {
        val fixture = Fixture()
        val originalRetries = fixture.connectionRetriesPreference()
        val originalAddress = fixture.bluetoothAddressPreference()
        val events = CopyOnWriteArrayList<BluetoothConnectionEvent>()
        val collector = launch(start = CoroutineStart.UNDISPATCHED) {
            fixture.btEvents.collect(events::add)
        }
        try {
            fixture.setConnectionRetriesPreference(3)
            fixture.setBluetoothAddressPreference("02:00:00:00:00:01")
            val oldGeneration = fixture.selectBluetoothSessionForTest()
            fixture.startBluetoothTransport(oldGeneration)
            fixture.await { events.any { it.generation == oldGeneration && it.state is ConnectionFailed } }

            val newGeneration = fixture.selectBluetoothSessionForTest()
            fixture.startBluetoothTransport(newGeneration)
            fixture.await { events.any { it.generation == newGeneration && it.state is ConnectionFailed } }
            delay(2_300)

            assertTrue(events.any { it.generation == newGeneration && it.state == ConnectionAttempt(2) })
            assertFalse(events.any { it.generation == oldGeneration && it.state == ConnectionAttempt(2) })
            assertFalse(events.any { it.generation == oldGeneration && it.retriesExhausted })
        } finally {
            collector.cancel()
            fixture.setConnectionRetriesPreference(originalRetries)
            fixture.setBluetoothAddressPreference(originalAddress)
            fixture.close()
        }
    }

    @Test fun bluetoothTransientFailureThenSuccessKeepsServiceAndDoesNotPublishTerminalResult() = runBlocking {
        val fixture = Fixture()
        try {
            fixture.setWakeLockPreference(true)
            val generation = fixture.selectBluetoothSessionForTest()
            val owner = fixture.serviceController.ensureStarted(generation)
            fixture.emitBluetoothEvent(BluetoothConnectionEvent(generation, ConnectionFailed(IOException("temporary"))))
            fixture.await { fixture.manager.currentConnectionState is ConnectionFailed }

            assertNull(fixture.manager.terminalSessionGeneration.value)
            assertEquals(owner, fixture.serviceController.ownerToken())
            assertTrue(fixture.serviceContext.stoppedServices.isEmpty())

            fixture.emitBluetoothEvent(BluetoothConnectionEvent(generation, Connected))
            fixture.await { fixture.manager.currentConnectionState == Connected }

            assertNull(fixture.manager.terminalSessionGeneration.value)
            assertEquals(owner, fixture.serviceController.ownerToken())
            assertEquals(Connected, fixture.manager.currentConnectionState)
            assertTrue(fixture.serviceContext.stoppedServices.isEmpty())
        } finally {
            fixture.close()
        }
    }

    @Test fun bluetoothRetryStateClearsCanonicalFirmwareBeforeNextConnection() = runBlocking {
        val fixture = Fixture()
        try {
            val generation = fixture.selectBluetoothSessionForTest()
            fixture.await { fixture.btPackets.subscriptionCount.value > 0 }
            fixture.btPackets.emit(firmwarePacket())
            fixture.await { fixture.manager.firmwareInfo.value != null }

            fixture.emitBluetoothEvent(BluetoothConnectionEvent(generation, InProgress))
            fixture.await { fixture.manager.firmwareInfo.value == null }
        } finally {
            fixture.close()
        }
    }

    @Test fun staleBluetoothTerminalEventCannotReplaceNewBluetoothGeneration() = runBlocking {
        val fixture = Fixture()
        try {
            fixture.setWakeLockPreference(true)
            val oldGeneration = fixture.selectBluetoothSessionForTest()
            fixture.serviceController.ensureStarted(oldGeneration)
            val newGeneration = fixture.selectBluetoothSessionForTest()
            fixture.serviceController.ensureStarted(newGeneration)
            fixture.emitBluetoothEvent(
                BluetoothConnectionEvent(
                    generation = oldGeneration,
                    state = ConnectionTimeout,
                    retriesExhausted = true,
                ),
            )
            delay(100)

            assertNull(fixture.manager.terminalSessionGeneration.value)
            assertEquals(newGeneration, fixture.serviceController.ownerConnectionGeneration())
            assertTrue(fixture.manager.isConnectionRunning)
            assertTrue(fixture.serviceContext.stoppedServices.isEmpty())
        } finally {
            fixture.close()
        }
    }

    @Test fun bluetoothRetryExhaustionAfterDisconnectPublishesEndedGeneration() = runBlocking {
        val fixture = Fixture()
        try {
            fixture.setWakeLockPreference(true)
            val generation = fixture.selectBluetoothSessionForTest()
            fixture.serviceController.ensureStarted(generation)
            fixture.emitBluetoothEvent(BluetoothConnectionEvent(generation, Connected))
            fixture.await { fixture.manager.currentConnectionState == Connected }
            fixture.emitBluetoothEvent(BluetoothConnectionEvent(generation, Disconnected))
            fixture.emitBluetoothEvent(
                BluetoothConnectionEvent(generation, ConnectionTimeout, retriesExhausted = true),
            )
            fixture.await { fixture.manager.terminalSessionGeneration.value != null }

            assertEquals(generation, fixture.manager.terminalSessionGeneration.value)
            assertEquals(ConnectionTimeout, fixture.manager.currentConnectionState)
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

            assertNull(result)
            assertNull(fixture.manager.terminalSessionGeneration.value)
            assertEquals(Connected, fixture.manager.currentConnectionState)
            assertTrue(fixture.manager.isUsbSessionLocked)
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

            assertNull(result)
            assertEquals(oldGeneration, fixture.manager.terminalSessionGeneration.value)
            assertEquals(Disconnected, fixture.manager.currentConnectionState)
            assertFalse(fixture.manager.isUsbSessionLocked)
            assertTrue(fixture.manager.activeUsbSessionGeneration != oldGeneration)
        } finally {
            fixture.close()
        }
    }

    @Test fun lateServiceDestroyCannotStopReplacementUsbSession() = runBlocking {
        val fixture = Fixture()
        try {
            fixture.setWakeLockPreference(true)
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
            assertTrue(fixture.manager.isUsbSessionLocked)
            assertEquals(replacementOwner, fixture.serviceController.ownerToken())
        } finally {
            fixture.close()
        }
    }

    @Test fun currentUsbServiceDestroyRestartsServiceWhileAppIsVisible() = runBlocking {
        val fixture = Fixture()
        try {
            fixture.setWakeLockPreference(true)
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
            assertTrue(fixture.manager.isUsbSessionLocked)
            assertNull(fixture.manager.terminalSessionGeneration.value)
        } finally {
            fixture.close()
        }
    }

    @Test fun currentUsbServiceDestroyEndsSessionWhenAppIsBackgrounded() = runBlocking {
        val fixture = Fixture()
        try {
            fixture.setWakeLockPreference(true)
            fixture.attach(109)
            fixture.confirmFirmware()
            fixture.manager.onAppForegrounded()
            val owner = fixture.serviceController.ownerToken()!!
            val generation = fixture.manager.activeUsbSessionGeneration
            fixture.logger.startLogging()
            fixture.await { fixture.logger.isLoggerStarted }
            fixture.manager.onAppBackgrounded()

            fixture.manager.onConnectionServiceDestroyed(owner)
            fixture.await { !fixture.logger.isLoggerStarted }

            assertFalse(fixture.manager.isConnectionRunning)
            assertFalse(fixture.manager.isUsbConnected)
            assertFalse(fixture.manager.isUsbSessionLocked)
            assertFalse(fixture.logger.isLoggerStarted)
            assertNull(fixture.serviceController.ownerToken())
            assertEquals(generation, fixture.manager.terminalSessionGeneration.value)
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
            val generation = fixture.selectBluetoothSessionForTest()
            runBlocking { fixture.manager.onBackgroundWorkPreferenceChanged(false) }
            assertTrue(fixture.serviceContext.startedServices.isEmpty())
            assertTrue(fixture.manager.isConnectionRunning)

            runBlocking { fixture.manager.onBackgroundWorkPreferenceChanged(true) }
            assertNotNull(fixture.serviceController.ownerToken())
            assertEquals(1, fixture.serviceContext.startedServices.size)

            runBlocking { fixture.manager.onBackgroundWorkPreferenceChanged(false) }

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
            val generation = fixture.selectBluetoothSessionForTest()
            fixture.manager.onAppForegrounded()
            runBlocking { fixture.manager.onBackgroundWorkPreferenceChanged(false) }

            assertTrue(fixture.manager.isConnectionRunning)
            fixture.manager.onAppBackgrounded()

            assertFalse(fixture.manager.isConnectionRunning)
            assertEquals(generation, fixture.manager.terminalSessionGeneration.value)
        } finally {
            fixture.close()
        }
    }

    @Test fun enabledBackgroundPreferencePreservesBtSessionAndServiceOnBackground() {
        val fixture = Fixture()
        try {
            fixture.setWakeLockPreference(true)
            val generation = fixture.selectBluetoothSessionForTest()
            fixture.manager.onAppForegrounded()
            val owner = fixture.serviceController.ownerToken()
            assertNotNull(owner)
            assertEquals(generation, fixture.serviceController.ownerConnectionGeneration())

            fixture.manager.onAppBackgrounded()

            assertTrue(fixture.manager.isConnectionRunning)
            assertEquals(owner, fixture.serviceController.ownerToken())
            assertNull(fixture.manager.terminalSessionGeneration.value)
        } finally {
            fixture.close()
        }
    }

    @Test fun lateDestroyFromPreviousBluetoothSessionCannotStopReplacement() {
        val fixture = Fixture()
        try {
            fixture.setWakeLockPreference(true)
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
            fixture.setWakeLockPreference(true)
            fixture.selectBluetoothSessionForTest()
            fixture.manager.onAppForegrounded()
            val stoppedOwner = fixture.serviceController.ownerToken()!!
            assertTrue(fixture.manager.stopConnection())
            assertNull(fixture.manager.terminalSessionGeneration.value)

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
            fixture.setWakeLockPreference(true)
            val generation = fixture.selectBluetoothSessionForTest()
            fixture.manager.onAppForegrounded()
            val owner = fixture.serviceController.ownerToken()!!
            fixture.manager.onAppBackgrounded()

            fixture.manager.onConnectionServiceDestroyed(owner)

            assertFalse(fixture.manager.isConnectionRunning)
            assertNull(fixture.serviceController.ownerToken())
            assertEquals(generation, fixture.manager.terminalSessionGeneration.value)
        } finally {
            fixture.close()
        }
    }

    @Test fun currentBluetoothServiceDestroyRestartsServiceWhileAppIsVisible() {
        val fixture = Fixture()
        try {
            fixture.setWakeLockPreference(true)
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
            assertNull(fixture.manager.terminalSessionGeneration.value)
        } finally {
            fixture.close()
        }
    }

    @Test fun unavailableAttachmentSnapshotDoesNotTreatUsbAsDetached() = runBlocking {
        val fixture = Fixture()
        try {
            fixture.attach(78)
            fixture.confirmFirmware()

            val whileConnected = fixture.manager.synchronizeUsbAttachments(Result.failure(SecurityException("USB list unavailable")))
            assertNull(whileConnected)
            assertTrue(fixture.manager.isUsbConnected)
            assertEquals(Connected, fixture.manager.currentConnectionState)
            assertNull(fixture.manager.terminalSessionGeneration.value)
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

    @Test fun backgroundPolicyStopBypassesUsbManualStopGuardAndPublishesTerminalGeneration() = runBlocking {
        val fixture = Fixture()
        try {
            fixture.attach(95)
            fixture.confirmFirmware()
            val generation = fixture.manager.activeUsbSessionGeneration
            assertFalse(fixture.manager.stopConnection())

            fixture.manager.onAppBackgrounded()

            assertFalse(fixture.manager.isConnectionRunning)
            assertFalse(fixture.manager.isUsbSessionLocked)
            assertEquals(generation, fixture.manager.terminalSessionGeneration.value)
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

    @Test fun managerOwnsOneUsbPermissionRequestAndDeniedPromptHistoryUntilDetach() {
        val fixture = Fixture()
        val device = usbDevice("/dev/bus/usb/001/061", 0x0403, 0x6001)
        try {
            val first = fixture.requestPermissionOrStart(device, hasPermission = false)
            assertNotNull(first)
            assertSame(first, fixture.pendingPermission())
            assertNull(fixture.requestPermissionOrStart(device, hasPermission = true))
            assertSame(first, fixture.pendingPermission())

            fixture.completePendingPermission(
                device = device,
                token = first!!.token,
                permissionGranted = false,
                attachedDevice = null,
                hasPermission = false,
                supported = true,
                conflictsWithActiveDevice = false,
            )
            assertNull(fixture.pendingPermission())
            assertNull(fixture.requestPermissionOrStart(device, hasPermission = false))
            assertNull(fixture.requestPermissionOrStart(device, hasPermission = true))
            assertEquals(device.deviceId, fixture.manager.activeUsbDeviceIdOrNull)

            fixture.manager.onUsbDeviceDetached(device.deviceId)
            val retried = fixture.requestPermissionOrStart(device, hasPermission = false)
            assertNotNull(retried)
            assertTrue(retried!!.token > first.token)
        } finally {
            fixture.close()
        }
    }

    @Test fun staleUsbPermissionCompletionCannotClearNewerRequest() {
        val fixture = Fixture()
        val original = usbDevice("/dev/bus/usb/001/062", 0x0403, 0x6001)
        val replacement = usbDevice("/dev/bus/usb/001/062", 0x1a86, 0x7523)
        try {
            assertEquals(original.deviceId, replacement.deviceId)
            val oldRequest = fixture.requestPermissionOrStart(original, hasPermission = false)!!
            fixture.manager.onUsbDeviceDetached(original.deviceId)
            val newRequest = fixture.requestPermissionOrStart(replacement, hasPermission = false)!!

            fixture.completePendingPermission(
                device = original,
                token = oldRequest.token,
                permissionGranted = true,
                attachedDevice = original,
                hasPermission = true,
                supported = true,
                conflictsWithActiveDevice = false,
            )

            assertSame(newRequest, fixture.pendingPermission())
            assertNull(fixture.manager.activeUsbSessionGeneration)
        } finally {
            fixture.close()
        }
    }

    @Test fun matchingUsbPermissionCompletionRechecksCurrentAdmissionAndConsumesInvalidRequest() {
        val fixture = Fixture()
        try {
            val invalidCompletions = listOf(
                Triple(false, true, true),
                Triple(true, false, true),
                Triple(true, true, false),
            )
            invalidCompletions.forEachIndexed { index, (permissionGranted, hasPermission, supported) ->
                val device = usbDevice("/dev/bus/usb/001/${64 + index}", 0x0403, 0x6001)
                val request = fixture.requestPermissionOrStart(device, hasPermission = false)!!
                fixture.completePendingPermission(
                    device = device,
                    token = request.token,
                    permissionGranted = permissionGranted,
                    attachedDevice = device,
                    hasPermission = hasPermission,
                    supported = supported,
                    conflictsWithActiveDevice = false,
                )
                assertNull(fixture.pendingPermission())
            }
            val conflictedDevice = usbDevice("/dev/bus/usb/001/067", 0x0403, 0x6001)
            val conflictedRequest = fixture.requestPermissionOrStart(conflictedDevice, hasPermission = false)!!
            fixture.completePendingPermission(
                device = conflictedDevice,
                token = conflictedRequest.token,
                permissionGranted = true,
                attachedDevice = conflictedDevice,
                hasPermission = true,
                supported = true,
                conflictsWithActiveDevice = true,
            )
            assertNull(fixture.pendingPermission())

            val detachedDevice = usbDevice("/dev/bus/usb/001/068", 0x0403, 0x6001)
            val detachedRequest = fixture.requestPermissionOrStart(detachedDevice, hasPermission = false)!!
            fixture.completePendingPermission(
                device = detachedDevice,
                token = detachedRequest.token,
                permissionGranted = true,
                attachedDevice = null,
                hasPermission = false,
                supported = false,
                conflictsWithActiveDevice = false,
            )
            assertNull(fixture.pendingPermission())
            assertNull(fixture.manager.activeUsbSessionGeneration)
        } finally {
            fixture.close()
        }
    }

    @Test fun validUsbPermissionCompletionStartsOnlyAfterGrantAndPermissionAreCurrent() {
        val fixture = Fixture()
        val device = usbDevice("/dev/bus/usb/001/065", 0x0403, 0x6001)
        try {
            fixture.manager.onAppForegrounded()
            val request = fixture.requestPermissionOrStart(device, hasPermission = false)!!
            fixture.completePendingPermission(
                device = device,
                token = request.token,
                permissionGranted = true,
                attachedDevice = device,
                hasPermission = true,
                supported = true,
                conflictsWithActiveDevice = false,
            )

            assertNull(fixture.pendingPermission())
            assertEquals(device.deviceId, fixture.manager.activeUsbDeviceIdOrNull)
        } finally {
            fixture.close()
        }
    }

    @Test fun backgroundCancelsPendingUsbPromptButRetainsPromptHistoryAndIgnoresLateResult() {
        val fixture = Fixture()
        val device = usbDevice("/dev/bus/usb/001/069", 0x0403, 0x6001)
        try {
            val request = fixture.requestPermissionOrStart(device, hasPermission = false)!!
            fixture.manager.onAppBackgrounded()
            assertNull(fixture.pendingPermission())

            fixture.completePendingPermission(
                device = device,
                token = request.token,
                permissionGranted = true,
                attachedDevice = device,
                hasPermission = true,
                supported = true,
                conflictsWithActiveDevice = false,
            )
            assertNull(fixture.manager.activeUsbSessionGeneration)
            assertNull(fixture.requestPermissionOrStart(device, hasPermission = false))

            fixture.manager.onAppForegrounded()
            assertNull(fixture.requestPermissionOrStart(device, hasPermission = true))
            assertEquals(device.deviceId, fixture.manager.activeUsbDeviceIdOrNull)
        } finally {
            fixture.close()
        }
    }

    @Test fun unavailableUsbSnapshotPreservesPermissionStateAndEmptySnapshotClearsIt() {
        val fixture = Fixture()
        val device = usbDevice("/dev/bus/usb/001/070", 0x0403, 0x6001)
        try {
            val request = fixture.requestPermissionOrStart(device, hasPermission = false)!!
            assertNull(fixture.manager.synchronizeUsbAttachments(Result.failure(SecurityException("USB list unavailable"))))
            assertSame(request, fixture.pendingPermission())

            assertNull(fixture.manager.synchronizeUsbAttachments(Result.success(emptyList())))
            assertNull(fixture.pendingPermission())
            val retried = fixture.requestPermissionOrStart(device, hasPermission = false)
            assertNotNull(retried)
            assertTrue(retried!!.token > request.token)
        } finally {
            fixture.close()
        }
    }

    private class Fixture(private val delayedStore: DataStore<Preferences>? = null) {
        private val context = InstrumentationRegistry.getInstrumentation().targetContext
        private val prefsFixture: IsolatedUserPrefs? = if (delayedStore == null) IsolatedUserPrefs(context, "usb-session-${UUID.randomUUID()}") else null
        val prefs = delayedStore?.let { UserPrefs(context, it) } ?: requireNotNull(prefsFixture).prefs
        val usb = UsbConnection(context.getSystemService(Context.USB_SERVICE) as UsbManager)
        private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        private val bt = BtConnection(prefs, bluetoothManager)
        val logger = SecuLogger(prefs, FileHelper(context))
        val serviceContext = RecordingServiceContext(context)
        val serviceController = ConnectionServiceController(serviceContext, ConnectionNotificationPublisher(serviceContext))
        val manager = Secu3ConnectionManager(usb, bt, prefs, bluetoothManager, logger, serviceController)
        init {
            if (delayedStore == null) runBlocking { manager.onBackgroundWorkPreferenceChanged(false) }
            manager.onAppForegrounded()
        }
        fun setWakeLockPreference(value: Boolean) = runBlocking { manager.onBackgroundWorkPreferenceChanged(value) }
        fun connectionRetriesPreference() = runBlocking { prefs.settings.first().connectionRetries }
        fun setConnectionRetriesPreference(value: Int) = runBlocking { prefs.setConnectionRetries(value) }
        fun bluetoothAddressPreference() = runBlocking { prefs.settings.first().bluetoothDeviceAddress }
        fun setBluetoothAddressPreference(value: String?) = runBlocking {
            prefs.setBluetoothDevice(value, prefs.settings.first().bluetoothDeviceName)
        }
        fun backgroundPolicy() = (managerField("backgroundWorkEnabled").get(manager) as MutableStateFlow<Boolean?>).value
        fun selectedTransport(): String {
            val selection = managerField("selectedTransport").get(manager) as MutableStateFlow<*>
            val transport = selection.value!!::class.java.getDeclaredField("transport").apply { isAccessible = true }.get(selection.value)
            return (transport as Enum<*>).name
        }
        val writes = CopyOnWriteArrayList<ByteArray>()
        private var fakeEndpointOpen: AtomicBoolean? = null
        val attemptPackets: MutableSharedFlow<UsbAttemptPacket> = usb.javaClass.getDeclaredField("_attemptPackets").apply { isAccessible = true }.get(usb) as MutableSharedFlow<UsbAttemptPacket>
        @Suppress("UNCHECKED_CAST")
        val attemptFailures: MutableSharedFlow<Long> = usb.javaClass.getDeclaredField("_attemptFailures").apply { isAccessible = true }.get(usb) as MutableSharedFlow<Long>
        val btStates: MutableSharedFlow<ConnectionState> = connectionField("mConnectionStateFlow").get(bt) as MutableSharedFlow<ConnectionState>
        @Suppress("UNCHECKED_CAST")
        val btPackets: MutableSharedFlow<RawPacket> = connectionField("mReceivedPacketFlow").get(bt) as MutableSharedFlow<RawPacket>
        @Suppress("UNCHECKED_CAST")
        val btEvents: MutableSharedFlow<BluetoothConnectionEvent> = bt.javaClass.getDeclaredField("_sessionEvents").apply { isAccessible = true }.get(bt) as MutableSharedFlow<BluetoothConnectionEvent>

        // The endpoint injection matches existing transport integration fixtures. It bypasses only
        // Android's physical USB opening; packet parsing, session identity and send gates are real.
        suspend fun attach(
            deviceId: Int,
            activeIdentity: UsbAttachmentIdentity = UsbAttachmentIdentity(
                deviceId = deviceId,
                deviceName = "/test/usb/$deviceId",
                vendorId = 0,
                productId = 0,
            ),
        ) {
            val open = AtomicBoolean(true)
            fakeEndpointOpen = open
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
            synchronized(manager) { managerField("usbSelectionReady").set(manager, subscribed) }
            @Suppress("UNCHECKED_CAST")
            val selected = managerField("selectedTransport").get(manager) as MutableStateFlow<Any>
            selected.value = selection
            managerField("activeUsbAttemptId").set(manager, 1L)
            withTimeout(3_000) { subscribed.await() }
            synchronized(manager) {
                if (managerField("usbSelectionReady").get(manager) === subscribed) {
                    managerField("usbSelectionReady").set(manager, null)
                }
            }
            await { btEvents.subscriptionCount.value > 0 }
        }

        fun closeUsbEndpoint() {
            fakeEndpointOpen?.set(false)
        }

        suspend fun confirmFirmware(attemptId: Long = 1L) {
            managerField("activeUsbAttemptId").set(manager, attemptId)
            attemptPackets.emit(UsbAttemptPacket(attemptId, firmwarePacket()))
            await { manager.isUsbConnected && manager.isUsbSessionLocked }
        }

        fun beginUsbRetry(attemptId: Long) {
            managerField("activeUsbAttemptId").set(manager, attemptId)
            managerField("confirmedUsbAttemptId").set(manager, null)
            val publish = Secu3ConnectionManager::class.java.getDeclaredMethod("publishConnectionState", ConnectionState::class.java).apply { isAccessible = true }
            publish.invoke(manager, InProgress)
        }

        suspend fun await(timeoutMillis: Long = 3_000, predicate: () -> Boolean) {
            withTimeout(timeoutMillis) { while (!predicate()) delay(10) }
        }

        fun setUsbAttemptSignals(signals: Channel<Any>) {
            managerField("usbAttemptSignals").set(manager, signals)
        }

        fun activeUsbAttemptId() = managerField("activeUsbAttemptId").get(manager) as Long?

        fun handleUsbAttemptFailure(attemptId: Long) {
            val method = Secu3ConnectionManager::class.java.getDeclaredMethod(
                "handleUsbAttemptFailure", Long::class.javaPrimitiveType,
            ).apply { isAccessible = true }
            method.invoke(manager, attemptId)
        }

        fun close() {
            if (!manager.stopConnection()) manager.activeUsbDeviceIdOrNull?.let(manager::onUsbDeviceDetached)
            serviceController.stop()
            (managerField("scope").get(manager) as CoroutineScope).cancel()
            usb.cancelScopes()
            bt.cancelScopes()
            (SecuLogger::class.java.getDeclaredField("loggerScope").apply { isAccessible = true }.get(logger) as CoroutineScope).cancel()
            runBlocking { prefsFixture?.close() }
        }

        fun requestPermissionOrStart(device: UsbDevice, hasPermission: Boolean): UsbPermissionRequest? {
            val method = Secu3ConnectionManager::class.java.getDeclaredMethod(
                "requestPermissionOrStart", UsbDevice::class.java, UsbAttachmentIdentity::class.java, Boolean::class.javaPrimitiveType,
            ).apply { isAccessible = true }
            val identity = UsbAttachmentIdentity(device.deviceId, device.deviceName, device.vendorId, device.productId)
            return method.invoke(manager, device, identity, hasPermission) as UsbPermissionRequest?
        }

        fun completePendingPermission(
            device: UsbDevice,
            token: Long,
            permissionGranted: Boolean,
            attachedDevice: UsbDevice?,
            hasPermission: Boolean,
            supported: Boolean,
            conflictsWithActiveDevice: Boolean,
        ) {
            val method = Secu3ConnectionManager::class.java.getDeclaredMethod(
                "completePendingUsbPermission", UsbDevice::class.java, Long::class.javaPrimitiveType,
                Boolean::class.javaPrimitiveType, UsbDevice::class.java, Boolean::class.javaPrimitiveType,
                Boolean::class.javaPrimitiveType, Boolean::class.javaPrimitiveType,
            ).apply { isAccessible = true }
            method.invoke(manager, device, token, permissionGranted, attachedDevice, hasPermission, supported, conflictsWithActiveDevice)
        }

        fun pendingPermission() = managerField("pendingUsbPermission").get(manager) as UsbPermissionRequest?

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

        fun startBluetoothTransport(generation: Long) = bt.startConnection(generation, bluetoothAddressPreference() ?: "02:00:00:00:00:01")

        suspend fun emitBluetoothEvent(event: BluetoothConnectionEvent) = btEvents.emit(event)

        private fun managerField(name: String) = Secu3ConnectionManager::class.java.getDeclaredField(name).apply { isAccessible = true }
        private fun connectionField(name: String) = Connection::class.java.getDeclaredField(name).apply { isAccessible = true }
    }

    private class DelayedPreferencesDataStore(initial: Preferences) : DataStore<Preferences> {
        val initialReadStarted = CompletableDeferred<Unit>()
        val releaseInitialRead = CompletableDeferred<Unit>()
        var failOnceAfterInitialEmission = false
        private val values = MutableStateFlow(initial)
        override val data = flow {
            initialReadStarted.complete(Unit)
            releaseInitialRead.await()
            emit(values.value)
            if (failOnceAfterInitialEmission) {
                failOnceAfterInitialEmission = false
                throw IOException("temporary preference read failure")
            }
            emitAll(values)
        }

        override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
            val updated = transform(values.value)
            values.value = updated
            return updated
        }
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
