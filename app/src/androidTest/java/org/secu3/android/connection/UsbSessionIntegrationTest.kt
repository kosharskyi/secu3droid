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
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.collect
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
import org.secu3.android.models.packets.input.SensorsPacket
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
            BluetoothPermissionPolicy(),
            UiMessageQueue(),
        )
        val viewModelStore = ViewModelStore()
        InstrumentationRegistry.getInstrumentation().runOnMainSync { viewModelStore.put("start", viewModel) }
        try {
            store.initialReadStarted.await()
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                viewModel.onStartRouteActive(true)
                viewModel.onStartActivityForegrounded(true)
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
            assertEquals("NONE", fixture.currentTransport())
            assertFalse(fixture.isBluetoothRunning())
            fixture.selectBluetoothSessionForTest()
            fixture.publishConnectionStateForTest(Connecting)
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
            fixture.await { fixture.btDriver.packets.subscriptionCount.value > 0 }
            fixture.emitBluetoothPacket(firmwarePacket())
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
                        fixture.emitUsbPacket(1L, firmwarePacket())
                    }
                }.first()
            }

            assertTrue(requestSent)
            assertTrue(reply is FirmwareInfoPacket)
            assertTrue(fixture.manager.isConnected)
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

    @Test fun mergedPacketFlowReceivesBluetoothAndUsbPacketsAcrossTransportSelection() = runBlocking {
        val fixture = Fixture()
        val packets = Channel<org.secu3.android.models.packets.base.InputPacket>(Channel.UNLIMITED)
        val collector = launch(start = CoroutineStart.UNDISPATCHED) {
            fixture.manager.receivedPacketFlow.collect(packets::send)
        }
        try {
            fixture.selectBluetoothSessionForTest()
            fixture.await { fixture.btDriver.packets.subscriptionCount.value > 0 }
            fixture.emitBluetoothPacket(firmwarePacket())
            assertTrue(withTimeout(3_000) { packets.receive() } is FirmwareInfoPacket)

            fixture.attach(74)
            fixture.await { fixture.usbDriver.packets.subscriptionCount.value > 0 }
            fixture.emitUsbPacket(1L, firmwarePacket())
            assertTrue(withTimeout(3_000) { packets.receive() } is FirmwareInfoPacket)
        } finally {
            collector.cancel()
            fixture.close()
        }
    }

    @Test fun sensorCalibrationWaitsForSettingsWithoutBlockingFirmwareAndUpdatesBothStreams() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val calibrationKey = stringPreferencesKey(UserPrefs.FUEL_FLOW_PULSES_PER_LITRE_KEY)
        val store = DelayedPreferencesDataStore(mutablePreferencesOf(calibrationKey to "8000"))
        val fixture = Fixture(delayedStore = store)
        try {
            store.initialReadStarted.await()
            fixture.selectBluetoothSessionForTest()
            val packets = Channel<org.secu3.android.models.packets.base.InputPacket>(Channel.UNLIMITED)
            val collector = launch(start = CoroutineStart.UNDISPATCHED) {
                fixture.manager.receivedPacketFlow.collect(packets::send)
            }
            fixture.await { fixture.btDriver.packets.subscriptionCount.value > 0 }

            // A sensor frame before settings is discarded, while firmware still reaches the
            // consumer and can complete connection setup.
            fixture.emitBluetoothPacket(sensorFlowFrame())
            fixture.emitBluetoothPacket(firmwarePacket())
            assertTrue(withTimeout(3_000) { packets.receive() } is FirmwareInfoPacket)
            assertTrue(packets.tryReceive().isFailure)

            store.releaseInitialRead.complete(Unit)
            fixture.await { fixture.currentFuelFlowCalibration() == 8_000 }
            fixture.emitBluetoothPacket(sensorFlowFrame())
            val firstSensor = withTimeout(3_000) { packets.receive() } as SensorsPacket
            assertEquals(4.5f, firstSensor.inj_ffh, .0001f)
            assertEquals(4.5f, firstSensor.inj_ffd, .0001f)

            fixture.prefs.setFuelFlowPulsesPerLitre(32_000)
            fixture.await { fixture.currentFuelFlowCalibration() == 32_000 }
            val subscribed = CompletableDeferred<Unit>()
            val requestSensor = async(start = CoroutineStart.UNDISPATCHED) {
                fixture.manager.receivedPacketFlow { subscribed.complete(Unit) }
                    .filterIsInstance<SensorsPacket>().first()
            }
            subscribed.await()
            fixture.emitBluetoothPacket(sensorFlowFrame())
            val requestResult = withTimeout(3_000) { requestSensor.await() }
            assertEquals(1.125f, requestResult.inj_ffh, .0001f)
            assertEquals(1.125f, requestResult.inj_ffd, .0001f)
            collector.cancel()
        } finally {
            store.releaseInitialRead.complete(Unit)
            fixture.close()
        }
    }

    @Test fun usbOpenFailureRetriesWithoutEndingAttachmentSession() = runBlocking {
        val fixture = Fixture()
        try {
            fixture.requestPermissionOrStart(usbDevice("/dev/bus/usb/099/001", 0x1234, 0x5678), hasPermission = true)
            fixture.await(6_000) { Connection::class.java.getDeclaredField("nextAttemptId").apply { isAccessible = true }.getLong(fixture.usb) >= 2 }
            assertEquals(Connecting, fixture.manager.currentConnectionState)
            assertEquals(0, fixture.disconnectionCount())
        } finally { fixture.close() }
    }

    @Test fun establishedUsbReadFailureUsesReconnectingAfterInitialConnecting() = runBlocking {
        val fixture = Fixture()
        try {
            fixture.attach(116)
            fixture.startUsbReader()
            fixture.await { fixture.usb.isConnected }
            assertEquals(Connecting, fixture.manager.currentConnectionState)

            fixture.queueUsbInput(firmwarePacketWire())
            fixture.await { fixture.manager.isConnected }
            fixture.closeUsbEndpoint()
            fixture.await { fixture.manager.currentConnectionState == UsbReconnecting }
            assertEquals(1, fixture.disconnectionCount())
        } finally { fixture.close() }
    }

    @Test fun usbFirmwareHandshakeTimeoutRetriesWithoutEndingAttachmentSession() = runBlocking {
        val fixture = Fixture()
        try {
            fixture.attach(115)
            fixture.startUsbReader()
            val transportJob = fixture.activeUsbRunJob()
            fixture.await(9_000) { fixture.activeUsbAttemptId() == 2L }
            assertEquals(UsbAwaitingEcu, fixture.manager.currentConnectionState)
            assertEquals(transportJob, fixture.activeUsbRunJob())
            assertEquals(0, fixture.disconnectionCount())
        } finally { fixture.close() }
    }

    @Test fun openUsbPortWithoutValidEcuPacketsEmitsAnAttemptScopedNoResponse() = runBlocking {
        val fixture = Fixture()
        try {
            fixture.attach(107)
            assertTrue("the simulated USB adapter remains open before ECU silence expires", fixture.usb.isConnected)
            val startedAt = System.nanoTime()
            val noResponse = async(start = CoroutineStart.UNDISPATCHED) {
                withTimeout(8_000) { fixture.attemptFailures.first { it.state == UsbAwaitingEcu } }
            }
            fixture.startUsbReader()

            assertEquals(UsbAwaitingEcu, noResponse.await().state)
            assertTrue("valid-packet silence must wait for its deadline", (System.nanoTime() - startedAt) / 1_000_000 >= 5_000)
            assertFalse("the timed-out serial endpoint is closed for the normal retry path", fixture.usb.isConnected)
        } finally {
            fixture.close()
        }
    }

    @Test fun invalidUsbBytesDoNotExtendEcuLivenessDeadline() = runBlocking {
        val fixture = Fixture()
        val garbage = launch {
            while (true) {
                fixture.queueUsbInput(byteArrayOf(0x01, 0x40, 0x79, 0x0d))
                delay(300)
            }
        }
        try {
            fixture.attach(109)
            val startedAt = System.nanoTime()
            val noResponse = async(start = CoroutineStart.UNDISPATCHED) {
                withTimeout(8_000) { fixture.attemptFailures.first { it.state == UsbAwaitingEcu } }
            }
            fixture.startUsbReader()

            assertEquals(UsbAwaitingEcu, noResponse.await().state)
            assertTrue("malformed bytes must not postpone the liveness deadline", (System.nanoTime() - startedAt) / 1_000_000 < 6_500)
        } finally {
            garbage.cancel()
            fixture.close()
        }
    }

    @Test fun validUsbPacketRestartsEcuLivenessDeadline() = runBlocking {
        val fixture = Fixture()
        try {
            fixture.attach(110)
            val noResponse = async(start = CoroutineStart.UNDISPATCHED) {
                withTimeout(10_000) { fixture.attemptFailures.first { it.state == UsbReconnectingAwaitingEcu } }
            }
            fixture.startUsbReader()
            delay(2_200)
            val validPacketQueuedAt = System.nanoTime()
            fixture.queueUsbInput(firmwarePacketWire())

            assertEquals(UsbReconnectingAwaitingEcu, noResponse.await().state)
            assertTrue(
                "a valid ECU packet starts a fresh full liveness interval after delivery",
                (System.nanoTime() - validPacketQueuedAt) / 1_000_000 >= 4_500,
            )
        } finally {
            fixture.close()
        }
    }

    @Test fun delayedPacketConsumerDoesNotCauseFalseEcuSilenceTimeout() = runBlocking {
        val fixture = Fixture()
        val firstPacketReceived = CompletableDeferred<Unit>()
        val releaseFirstPacket = CompletableDeferred<Unit>()
        val secondPacketReceived = CompletableDeferred<Unit>()
        val secondPacketRead = CompletableDeferred<Unit>()
        val packetReadCount = AtomicInteger()
        val slowConsumer = launch(start = CoroutineStart.UNDISPATCHED) {
            fixture.usb.receivedPacketFlow.collect {
                if (firstPacketReceived.complete(Unit)) releaseFirstPacket.await()
                else secondPacketReceived.complete(Unit)
            }
        }
        val packetObserver = launch(start = CoroutineStart.UNDISPATCHED) {
            fixture.attemptPackets.collect {
                if (packetReadCount.incrementAndGet() == 2) secondPacketRead.complete(Unit)
            }
        }
        try {
            fixture.attach(111)
            val noResponse = async(start = CoroutineStart.UNDISPATCHED) {
                withTimeout(16_000) { fixture.attemptFailures.first { it.state == UsbReconnectingAwaitingEcu } }
            }
            fixture.startUsbReader()
            val wirePacket = firmwarePacketWire()
            fixture.queueUsbInput(wirePacket + wirePacket)
            withTimeout(3_000) { secondPacketRead.await() }
            delay(5_500)
            assertFalse("read loop must stay suspended while the packet consumer is backpressured", noResponse.isCompleted)
            releaseFirstPacket.complete(Unit)
            withTimeout(3_000) { secondPacketReceived.await() }

            assertNull(
                "consumer suspension must not spend the ECU packet deadline",
                withTimeoutOrNull(4_500) { noResponse.await() },
            )
            assertEquals(UsbReconnectingAwaitingEcu, noResponse.await().state)
        } finally {
            releaseFirstPacket.complete(Unit)
            packetObserver.cancel()
            slowConsumer.cancel()
            fixture.close()
        }
    }

    @Test fun noResponseForConfirmedUsbClearsFirmwareAndIgnoresStaleAttempts() = runBlocking {
        val fixture = Fixture()
        try {
            fixture.attach(108)
            fixture.confirmFirmware()
            val transportJob = fixture.activeUsbRunJob()
            assertTrue(fixture.manager.isUsbSessionLocked)
            fixture.failUsbAttempt(0L, true)
            assertTrue("an earlier attempt cannot invalidate the confirmed retry", fixture.manager.isUsbSessionLocked)

            fixture.failUsbAttempt(1L, true)
            assertEquals(UsbReconnectingAwaitingEcu, fixture.manager.currentConnectionState)
            assertNull(fixture.manager.firmwareInfo.value)
            assertFalse(fixture.manager.isUsbSessionLocked)
            assertEquals("an established ECU loss publishes Disconnected", 1, fixture.disconnectionCount())
            assertEquals("retry stays within the selected transport run", transportJob, fixture.activeUsbRunJob())
            fixture.emitUsbPacket(1L, firmwarePacket())
            delay(100)
            assertEquals("late firmware from a timed-out attempt cannot restore Connected", UsbReconnectingAwaitingEcu, fixture.manager.currentConnectionState)
            assertNull(fixture.manager.firmwareInfo.value)

            fixture.beginUsbRetry(2L)
            assertEquals("ignition guidance remains active after the established connection loses ECU replies", UsbReconnectingAwaitingEcu, fixture.manager.currentConnectionState)
            fixture.confirmFirmware(2L)
            assertEquals(Connected, fixture.manager.currentConnectionState)
            assertTrue("a new attempt on the same attachment restores the send gate", fixture.manager.isConnected)
            assertNotNull(fixture.manager.firmwareInfo.value)
            assertEquals("new firmware confirmation supersedes the loss status", Connected, fixture.states.last())

            fixture.failUsbAttempt(2L, false)
            assertEquals("ordinary transport failures end the recovered session", 2, fixture.disconnectionCount())
            assertNull(fixture.manager.firmwareInfo.value)
            assertFalse(fixture.manager.isUsbSessionLocked)

            fixture.beginUsbRetry(3L)
            fixture.confirmFirmware(3L)
            assertEquals(Connected, fixture.states.last())
            assertTrue(fixture.manager.isConnected)
        } finally {
            fixture.close()
        }
    }

    @Test fun physicalDetachAfterEcuTimeoutPublishesAnotherDisconnected() = runBlocking {
        val fixture = Fixture()
        try {
            fixture.attach(112)
            fixture.confirmFirmware()
            val transportJob = fixture.activeUsbRunJob()

            fixture.failUsbAttempt(1L, true)
            assertEquals(1, fixture.disconnectionCount())
            assertEquals(transportJob, fixture.activeUsbRunJob())

            assertTrue(fixture.manager.onUsbDeviceDetached(112))
            assertEquals("cable removal publishes a fresh Disconnected after ECU silence", 2, fixture.disconnectionCount())
            assertNull(fixture.activeUsbRunJob())
        } finally {
            fixture.close()
        }
    }

    @Test fun productionRetryLoopRecoversAcrossSuccessiveLogicalSessions() = runBlocking {
        val fixture = Fixture()
        try {
            fixture.attach(113)
            // Keep the production coordinator running; only physical USB opening is bypassed.
            fixture.startUsbReader()
            fixture.await { fixture.activeUsbAttemptId() == 1L }
            fixture.confirmFirmware(1L)

            repeat(2) { index ->
                val attemptId = index + 1L
                val transportJob = fixture.activeUsbRunJob()
                fixture.failUsbAttempt(attemptId, true)
                assertEquals(index + 1, fixture.disconnectionCount())
                assertEquals(transportJob, fixture.activeUsbRunJob())

                fixture.await(6_000) { fixture.activeUsbAttemptId() == attemptId + 1 }
                    fixture.confirmFirmware(attemptId + 1)
                assertEquals(Connected, fixture.states.last())
                assertEquals(Connected, fixture.manager.currentConnectionState)
            }
        } finally {
            fixture.close()
        }
    }

    @Test fun blockedUsbOpeningCannotOutliveTheFirmwareDeadline() = runBlocking {
        val fixture = Fixture()
        try {
            fixture.attach(117)
            fixture.usb.openDelayMillis = 6_500
            fixture.startUsbReader()
            fixture.await(6_000) { fixture.manager.currentConnectionState == UsbAwaitingEcu }
            assertFalse(fixture.manager.isConnected)
            assertNull(fixture.usb.activeAttemptId)
            assertEquals(0, fixture.disconnectionCount())
        } finally { fixture.close() }
    }

    @Test fun closedUsbEndpointEndsTheConfirmedSessionWithoutReadException() = runBlocking {
        val fixture = Fixture()
        try {
            fixture.attach(116)
            fixture.startUsbReader()
            fixture.await { fixture.activeUsbAttemptId() == 1L }
            fixture.queueUsbInput(firmwarePacketWire())
            fixture.await { fixture.manager.currentConnectionState == Connected }
            fixture.closeUsbEndpoint()
            fixture.await { fixture.disconnectionCount() == 1 }
            assertEquals(1, fixture.disconnectionCount())
            assertFalse(fixture.manager.isConnected)
        } finally { fixture.close() }
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
            assertTrue(fixture.manager.isConnected)
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
            fixture.emitBluetoothEvent(ConnectionEvent(Disconnected))
            fixture.emitBluetoothEvent(ConnectionEvent(ConnectionFailed(IOException("old Bluetooth failure"))))
            delay(100)

            assertEquals(Connected, fixture.manager.currentConnectionState)
            assertTrue(fixture.manager.isUsbSessionLocked)
            assertTrue(fixture.manager.isConnected)
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
                assertFalse(fixture.manager.isConnected)
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

            assertNull(fixture.manager.synchronizeUsbAttachments(Result.success(emptyList())))
            assertEquals(1, fixture.disconnectionCount())
            assertEquals(Disconnected, fixture.manager.currentConnectionState)
            assertFalse(fixture.manager.isUsbSessionLocked)

            assertNull(fixture.manager.synchronizeUsbAttachments(Result.success(emptyList())))
            assertEquals("repeated synchronization does not repeat the loss", 1, fixture.disconnectionCount())
        } finally {
            fixture.close()
        }
    }

    @Test fun usbDetachStopsServiceImmediatelyAndPublishesDisconnected() = runBlocking {
        val fixture = Fixture()
        try {
            fixture.setWakeLockPreference(true)
            fixture.attach(80)
            fixture.confirmFirmware()
            fixture.foregroundOnStart()
            assertTrue(fixture.serviceIsStarted())

            assertTrue(fixture.manager.onUsbDeviceDetached(80))

            assertEquals(false, fixture.serviceContext.commandHistory.last())
            assertEquals(Disconnected, fixture.states.last())
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
                fixture.selectBluetoothSessionForTest()
                fixture.manager.onAppBackgrounded()
                assertNull("policy must remain unknown until DataStore's first read", fixture.backgroundPolicy())
                assertEquals(0, fixture.disconnectionCount())

                store.releaseInitialRead.complete(Unit)
                fixture.await { fixture.backgroundPolicy() == savedEnabled }
                if (savedEnabled) {
                    assertTrue("saved background permission keeps an active retry session", fixture.manager.isConnectionRunning)
                    assertEquals(0, fixture.disconnectionCount())
                } else {
                    fixture.await { fixture.manager.currentConnectionState == Disconnected }
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
            assertFalse(fixture.isBluetoothRunning())
            assertEquals(Disconnected, fixture.manager.currentConnectionState)
            assertEquals("NONE", fixture.currentTransport())
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
            fixture.selectBluetoothSessionForTest()
            val states = CopyOnWriteArrayList<ConnectionState>()
            val collector = launch(
                start = kotlinx.coroutines.CoroutineStart.UNDISPATCHED,
            ) {
                fixture.manager.connectionStateFlow.collect(states::add)
            }
            fixture.startBluetoothTransport()

            fixture.await { fixture.manager.currentConnectionState is ConnectionFailed }
            assertEquals(0, fixture.disconnectionCount())
            assertFalse(fixture.serviceIsStarted())
            assertEquals(false, fixture.serviceContext.commandHistory.last())

            fixture.await(timeoutMillis = 10_000) { fixture.manager.currentConnectionState == ConnectionTimeout }
            assertEquals(1, fixture.disconnectionCount())
            assertEquals(ConnectionTimeout, fixture.manager.currentConnectionState)
            assertFalse(fixture.serviceIsStarted())
            assertEquals(false, fixture.serviceContext.commandHistory.last())

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

    @Test fun bluetoothTransientFailureThenSuccessKeepsServiceWithoutDisconnected() = runBlocking {
        val fixture = Fixture()
        try {
            fixture.setWakeLockPreference(true)
            fixture.selectBluetoothSessionForTest()
            fixture.emitBluetoothEvent(ConnectionEvent(ConnectionFailed(IOException("temporary"))))
            fixture.await { fixture.manager.currentConnectionState is ConnectionFailed }

            assertEquals(0, fixture.disconnectionCount())
            assertFalse(fixture.serviceIsStarted())
            assertEquals(false, fixture.serviceContext.commandHistory.last())

            fixture.emitBluetoothEvent(ConnectionEvent(Connected))
            fixture.await { fixture.manager.currentConnectionState == Connected }

            assertEquals(0, fixture.disconnectionCount())
            assertTrue(fixture.serviceIsStarted())
            assertEquals(listOf(true, false, true), fixture.serviceContext.commandHistory.takeLast(3))
            assertEquals(Connected, fixture.manager.currentConnectionState)
        } finally {
            fixture.close()
        }
    }

    @Test fun establishedBluetoothLossStaysOnReconnectProgressWithoutDisconnecting() = runBlocking {
        val fixture = Fixture()
        try {
            fixture.setWakeLockPreference(true)
            fixture.selectBluetoothSessionForTest()
            fixture.logger.startLogging()
            fixture.await { fixture.logger.isLoggerStarted }

            fixture.btDriver.fail(Reconnecting, ConnectionEvent.End.RETRYING)
            fixture.await { !fixture.logger.isLoggerStarted }

            assertEquals(Reconnecting, fixture.states.last())
            assertEquals(0, fixture.disconnectionCount())
            assertTrue(fixture.serviceIsStarted())
            assertTrue(fixture.manager.isConnectionRunning)
            assertTrue(fixture.serviceContext.commandHistory.takeLast(2).all { it })

            fixture.btDriver.setAttempt(2L)
            fixture.btDriver.publish(Connected)
            assertEquals(Connected, fixture.states.last())
        } finally { fixture.close() }
    }

    @Test fun explicitManagerCommandsDoNotDependOnAppVisibility() = runBlocking {
        val fixture = Fixture()
        val device = usbDevice("/dev/bus/usb/001/068", 0x0403, 0x6001)
        try {
            fixture.manager.onAppBackgrounded()
            val permission = fixture.manager.synchronizeUsbAttachments(Result.success(listOf(device)))
            assertNull(permission)
            assertNull(fixture.pendingPermission())
            fixture.manager.startBtConnection()
            assertTrue(fixture.states.contains(Connecting))
        } finally {
            fixture.close()
        }
    }

    @Test fun bluetoothRetryStateClearsCanonicalFirmwareBeforeNextConnection() = runBlocking {
        val fixture = Fixture()
        try {
            fixture.selectBluetoothSessionForTest()
            fixture.await { fixture.btDriver.packets.subscriptionCount.value > 0 }
            fixture.emitBluetoothPacket(firmwarePacket())
            fixture.await { fixture.manager.firmwareInfo.value != null }

            fixture.emitBluetoothEvent(ConnectionEvent(Reconnecting))
            fixture.await { fixture.manager.firmwareInfo.value == null }
        } finally {
            fixture.close()
        }
    }

    @Test fun bluetoothRetryExhaustionAfterDisconnectPublishesFinalTimeout() = runBlocking {
        val fixture = Fixture()
        try {
            fixture.setWakeLockPreference(true)
            fixture.selectBluetoothSessionForTest()
            fixture.emitBluetoothEvent(ConnectionEvent(Connected))
            fixture.await { fixture.manager.currentConnectionState == Connected }
            fixture.emitBluetoothEvent(ConnectionEvent(Disconnected))
            fixture.emitBluetoothEvent(
                ConnectionEvent(ConnectionTimeout, end = ConnectionEvent.End.STOPPED),
            )
            fixture.await { fixture.manager.currentConnectionState == ConnectionTimeout }

            assertEquals(1, fixture.disconnectionCount())
            assertEquals(ConnectionTimeout, fixture.manager.currentConnectionState)
            assertFalse(fixture.serviceIsStarted())
            assertEquals(false, fixture.serviceContext.commandHistory.last())
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
            assertEquals(0, fixture.disconnectionCount())
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
            val oldRunJob = fixture.activeUsbRunJob()

            val result = fixture.manager.synchronizeUsbAttachments(Result.success(listOf(replacement)))

            assertNull(result)
            assertEquals(1, fixture.disconnectionCount())
            assertEquals(Disconnected, fixture.manager.currentConnectionState)
            assertFalse(fixture.manager.isUsbSessionLocked)
            assertNull(fixture.activeUsbRunJob())
        } finally {
            fixture.close()
        }
    }

    @SdkSuppress(minSdkVersion = 35)
    @Test fun reconciliationCleansDetachedUsbSessionWithoutStartingReplacement() = runBlocking {
        val fixture = Fixture()
        val original = usbDevice("/dev/bus/usb/001/057", 0x1234, 0x5678)
        val replacement = usbDevice("/dev/bus/usb/001/057", 0x4321, 0x8765)
        try {
            fixture.attach(original.deviceId, original.toIdentity())
            fixture.confirmFirmware()
            fixture.manager.reconcileUsbAttachments(Result.success(listOf(replacement)))
            assertEquals(1, fixture.disconnectionCount())
            assertEquals(Disconnected, fixture.manager.currentConnectionState)
            assertFalse(fixture.manager.isConnectionRunning)
            assertFalse(fixture.manager.isUsbSessionLocked)
        } finally {
            fixture.close()
        }
    }

    @Test fun bluetoothBackgroundPreferenceControlsServiceWithoutEndingConnection() {
        val fixture = Fixture()
        try {
            fixture.foregroundOnStart()
            fixture.selectBluetoothSessionForTest()
            runBlocking { fixture.manager.onBackgroundWorkPreferenceChanged(false) }
            assertFalse(fixture.serviceIsStarted())
            assertTrue(fixture.manager.isConnectionRunning)

            runBlocking { fixture.manager.onBackgroundWorkPreferenceChanged(true) }
            runBlocking { fixture.await { fixture.serviceIsStarted() } }
            assertTrue(fixture.serviceIsStarted())
            assertTrue(fixture.serviceContext.commandHistory.last())

            runBlocking { fixture.manager.onBackgroundWorkPreferenceChanged(false) }
            runBlocking { fixture.await { !fixture.serviceIsStarted() } }

            assertFalse(fixture.serviceIsStarted())
            assertTrue(fixture.manager.isConnectionRunning)
            assertEquals(false, fixture.serviceContext.commandHistory.last())
        } finally {
            fixture.close()
        }
    }

    @Test fun serviceStartFailureDoesNotEndConnectionAndForegroundReconcilesAgain() {
        val fixture = Fixture()
        try {
            fixture.setWakeLockPreference(true)
            fixture.serviceContext.failNextStart = true
            fixture.selectBluetoothSessionForTest()

            assertEquals(Connected, fixture.manager.currentConnectionState)
            assertTrue(fixture.manager.isConnectionRunning)
            assertFalse(fixture.serviceIsStarted())

            fixture.manager.onAppForegrounded()

            assertTrue(fixture.serviceIsStarted())
            val startsBeforeReconcile = fixture.serviceContext.startedServices.size
            fixture.manager.onAppForegrounded()
            assertTrue(fixture.serviceContext.startedServices.size > startsBeforeReconcile)
            assertTrue(fixture.serviceIsStarted())
            assertEquals(Connected, fixture.manager.currentConnectionState)
        } finally {
            fixture.close()
        }
    }

    @Test fun backgroundPreferenceEndsBtOnlyWhenTheAppActuallyLeavesForeground() {
        val fixture = Fixture()
        try {
            fixture.selectBluetoothSessionForTest()
            fixture.foregroundOnStart()
            runBlocking { fixture.manager.onBackgroundWorkPreferenceChanged(false) }

            assertTrue(fixture.manager.isConnectionRunning)
            fixture.manager.onAppBackgrounded()

            assertFalse(fixture.manager.isConnectionRunning)
            assertEquals(Disconnected, fixture.states.last())
        } finally {
            fixture.close()
        }
    }

    @Test fun enabledBackgroundPreferencePreservesBtSessionAndServiceOnBackground() {
        val fixture = Fixture()
        try {
            fixture.setWakeLockPreference(true)
            fixture.selectBluetoothSessionForTest()
            fixture.foregroundOnStart()
            assertTrue(fixture.serviceIsStarted())

            fixture.manager.onAppBackgrounded()

            assertTrue(fixture.manager.isConnectionRunning)
            assertTrue(fixture.serviceIsStarted())
            assertEquals(0, fixture.disconnectionCount())
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
            assertTrue(fixture.manager.isConnected)
            assertEquals(Connected, fixture.manager.currentConnectionState)
            assertEquals(0, fixture.disconnectionCount())
        } finally {
            fixture.close()
        }
    }

    @Test fun staleUsbPacketsCannotConfirmOrNavigateAReplacementRetryAttempt() = runBlocking {
        val fixture = Fixture()
        try {
            fixture.attach(91)
            fixture.emitUsbPacket(9, firmwarePacket())
            delay(100)
            assertFalse(fixture.manager.isConnected)

            fixture.confirmFirmware(10)
            assertEquals(Connected, fixture.manager.currentConnectionState)
            fixture.beginUsbRetry(11)
            fixture.emitUsbPacket(10, firmwarePacket())
            delay(100)
            assertFalse(fixture.manager.isConnected)
            assertEquals(Connecting, fixture.manager.currentConnectionState)

            fixture.confirmFirmware(11)
            assertTrue(fixture.manager.isConnected)
            assertEquals(Connected, fixture.manager.currentConnectionState)
        } finally {
            fixture.close()
        }
    }

    @Test fun backgroundPolicyStopBypassesUsbManualStopGuardAndPublishesDisconnected() = runBlocking {
        val fixture = Fixture()
        try {
            fixture.attach(95)
            fixture.confirmFirmware()
            assertFalse(fixture.manager.stopConnection())

            fixture.manager.onAppBackgrounded()

            assertFalse(fixture.manager.isConnectionRunning)
            assertFalse(fixture.manager.isUsbSessionLocked)
            assertEquals(Disconnected, fixture.states.last())
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
            assertTrue(fixture.manager.isConnected)
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

    @Test fun cancellingOldUsbPermissionTokenPreservesNewerPendingRequest() {
        val fixture = Fixture()
        val oldDevice = usbDevice("/dev/bus/usb/001/071", 0x0403, 0x6001)
        val newDevice = usbDevice("/dev/bus/usb/001/072", 0x0403, 0x6001)
        try {
            val oldRequest = fixture.requestPermissionOrStart(oldDevice, hasPermission = false)!!
            fixture.manager.cancelUsbPermissionRequest(oldRequest.token)
            val newRequest = fixture.requestPermissionOrStart(newDevice, hasPermission = false)!!

            fixture.manager.cancelUsbPermissionRequest(oldRequest.token)

            assertSame(newRequest, fixture.pendingPermission())
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
            assertNull(fixture.activeUsbRunJob())
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
            assertNull(fixture.activeUsbRunJob())
        } finally {
            fixture.close()
        }
    }

    @Test fun validUsbPermissionCompletionStartsOnlyAfterGrantAndPermissionAreCurrent() {
        val fixture = Fixture()
        val device = usbDevice("/dev/bus/usb/001/065", 0x0403, 0x6001)
        try {
            fixture.foregroundOnStart()
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
            assertNull(fixture.activeUsbRunJob())
            assertNull(fixture.requestPermissionOrStart(device, hasPermission = false))

            fixture.foregroundOnStart()
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

    private inner class Fixture(private val delayedStore: DataStore<Preferences>? = null) {
        private val context = InstrumentationRegistry.getInstrumentation().targetContext
        private val prefsFixture: IsolatedUserPrefs? = if (delayedStore == null) IsolatedUserPrefs(context, "usb-session-${UUID.randomUUID()}") else null
        val prefs = delayedStore?.let { UserPrefs(context, it) } ?: requireNotNull(prefsFixture).prefs
        val usb = FakeUsbConnection(context.getSystemService(Context.USB_SERVICE) as UsbManager, prefs)
        private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        private val bt = BtConnection(prefs, bluetoothManager)
        val logger = SecuLogger(prefs, FileHelper(context))
        val serviceContext = RecordingServiceContext(context)
        private val notificationPublisher = ConnectionNotificationPublisher(serviceContext)
        val serviceManager = ServiceManager(serviceContext)
        val manager = Secu3ConnectionManager(usb, bt, prefs, logger, serviceManager, notificationPublisher)
        val states = CopyOnWriteArrayList<ConnectionState>()
        private val stateObserver = CoroutineScope(kotlinx.coroutines.Dispatchers.Unconfined).launch(start = CoroutineStart.UNDISPATCHED) {
            manager.connectionStateFlow.drop(1).collect(states::add)
        }
        fun disconnectionCount() = states.count { it == Disconnected }
        init {
            if (delayedStore == null) runBlocking { manager.onBackgroundWorkPreferenceChanged(false) }
            foregroundOnStart()
        }
        fun foregroundOnStart() {
            manager.onAppForegrounded()
        }
        fun setWakeLockPreference(value: Boolean) = runBlocking { manager.onBackgroundWorkPreferenceChanged(value) }
        fun isBluetoothRunning() = bt.isRunning
        fun connectionRetriesPreference() = runBlocking { prefs.settings.first().connectionRetries }
        fun setConnectionRetriesPreference(value: Int) = runBlocking { prefs.setConnectionRetries(value) }
        fun bluetoothAddressPreference() = runBlocking { prefs.settings.first().bluetoothDeviceAddress }
        fun setBluetoothAddressPreference(value: String?) = runBlocking {
            prefs.setBluetoothDevice(value, prefs.settings.first().bluetoothDeviceName)
        }
        fun backgroundPolicy() = (managerField("backgroundWorkEnabled").get(manager) as MutableStateFlow<Boolean?>).value
        fun currentFuelFlowCalibration() = connectionField("currentFuelFlowPulsesPerLitre").get(bt) as Int?
        fun currentTransport(): String {
            val current = managerField("currentConnection").get(manager)
            return when {
                current === usb -> "USB"
                current === bt -> "BLUETOOTH"
                else -> "NONE"
            }
        }
        val writes = CopyOnWriteArrayList<ByteArray>()
        private var fakeEndpointOpen: AtomicBoolean? = null
        private var fakePort: UsbSerialPort? = null
        private val fakeInput = LinkedBlockingQueue<ByteArray>()
        val usbDriver = ConnectionTestDriver(manager, usb)
        val btDriver = ConnectionTestDriver(manager, bt)
        val attemptPackets get() = usbDriver.packets
        val attemptFailures get() = manager.connectionStateFlow.map { ConnectionEvent(it) }
        suspend fun emitUsbPacket(attempt: Long, packet: RawPacket) = usbDriver.emit(packet, attempt)
        suspend fun emitBluetoothPacket(packet: RawPacket) = btDriver.emit(packet)

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
            usb.nextPort = ::createPort
            usbDriver.select("USB")
            usb.javaClass.superclass.getDeclaredField("activeAttachment").apply { isAccessible = true }.set(usb, activeIdentity)
            usbDriver.installEndpoint(createPort())
        }

        private fun createPort(): UsbSerialPort {
            val open = AtomicBoolean(true)
            fakeEndpointOpen = open
            return (Proxy.newProxyInstance(UsbSerialPort::class.java.classLoader, arrayOf(UsbSerialPort::class.java)) { proxy, method, args ->
                when (method.name) {
                    "isOpen" -> open.get()
                    "close" -> { open.set(false); null }
                    "write" -> { writes += (args!![0] as ByteArray).copyOf(); null }
                    "read" -> {
                        val destination = args!![0] as ByteArray
                        val bytes = fakeInput.poll(1_000, TimeUnit.MILLISECONDS)
                        if (bytes == null) 0 else { bytes.copyInto(destination); bytes.size }
                    }
                    "toString" -> "isolated-usb-session-test"
                    "hashCode" -> System.identityHashCode(proxy)
                    "equals" -> proxy === args?.get(0)
                    else -> null
                }
            } as UsbSerialPort).also { fakePort = it }
        }

        fun closeUsbEndpoint() {
            fakeEndpointOpen?.set(false)
        }

        fun startUsbReader(): Unit = synchronized(manager) {
            connectionField("nextAttemptId").setLong(usb, 0L)
            usb.startConnection(usbDevice("/test/usb/reader", 0x1234, 0x5678))
            Secu3ConnectionManager::class.java.getDeclaredMethod("synchronizeUsbOutcome").apply { isAccessible = true }.invoke(manager)
        }
        fun queueUsbInput(bytes: ByteArray) { fakeInput.offer(bytes) }

        suspend fun confirmFirmware(attemptId: Long = 1L) {
            usbDriver.setAttempt(attemptId)
            usbDriver.emit(firmwarePacket(), attemptId)
            await { manager.isConnected && manager.currentConnectionState == Connected && manager.fwInfo != null }
        }
        suspend fun beginUsbRetry(attemptId: Long) {
            restoreUsbEndpoint()
            usbDriver.setAttempt(attemptId)
            if (usb.currentConnectionState != UsbAwaitingEcu && usb.currentConnectionState != UsbReconnecting &&
                usb.currentConnectionState != UsbReconnectingAwaitingEcu
            ) usbDriver.publish(Connecting)
        }
        fun restoreUsbEndpoint() { usbDriver.installEndpoint(createPort()) }

        suspend fun await(timeoutMillis: Long = 3_000, predicate: () -> Boolean) {
            withTimeout(timeoutMillis) { while (!predicate()) delay(10) }
        }

        fun activeUsbAttemptId() = usb.activeAttemptId
        suspend fun failUsbAttempt(attemptId: Long, noResponse: Boolean) {
            if (usb.activeAttemptId != attemptId) return
            val end = if (usb.readyAttemptId == attemptId) ConnectionEvent.End.RETRYING else null
            val state = when {
                noResponse && end == ConnectionEvent.End.RETRYING -> UsbReconnectingAwaitingEcu
                noResponse -> UsbAwaitingEcu
                end == ConnectionEvent.End.RETRYING -> UsbReconnecting
                else -> Connecting
            }
            usbDriver.fail(state, end)
            await { manager.currentConnectionState == state && !manager.isUsbSessionLocked }
        }

        fun close() {
            if (!manager.stopConnection()) manager.activeUsbDeviceIdOrNull?.let(manager::onUsbDeviceDetached)
            serviceManager.stopService(SecuConnectionService::class.java)
            stateObserver.cancel()
            (managerField("scope").get(manager) as CoroutineScope).cancel()
            usbDriver.shutdown()
            btDriver.shutdown()
            (SecuLogger::class.java.getDeclaredField("loggerScope").apply { isAccessible = true }.get(logger) as CoroutineScope).cancel()
            runBlocking { prefsFixture?.close() }
        }

        fun requestPermissionOrStart(device: UsbDevice, hasPermission: Boolean): UsbPermissionRequest? = synchronized(manager) {
            val method = UsbConnection::class.java.getDeclaredMethod(
                "requestPermissionOrStart", UsbDevice::class.java, UsbAttachmentIdentity::class.java, Boolean::class.javaPrimitiveType, Function0::class.java,
            ).apply { isAccessible = true }
            val identity = UsbAttachmentIdentity(device.deviceId, device.deviceName, device.vendorId, device.productId)
            val result = method.invoke(usb, device, identity, hasPermission, {}) as UsbPermissionRequest?
            Secu3ConnectionManager::class.java.getDeclaredMethod("synchronizeUsbOutcome").apply { isAccessible = true }.invoke(manager)
            return result
        }

        fun completePendingPermission(
            device: UsbDevice,
            token: Long,
            permissionGranted: Boolean,
            attachedDevice: UsbDevice?,
            hasPermission: Boolean,
            supported: Boolean,
            conflictsWithActiveDevice: Boolean,
        ): Unit = synchronized(manager) {
            val method = UsbConnection::class.java.getDeclaredMethod(
                "completePendingPermission", UsbDevice::class.java, Long::class.javaPrimitiveType,
                Boolean::class.javaPrimitiveType, UsbDevice::class.java, Boolean::class.javaPrimitiveType,
                Boolean::class.javaPrimitiveType, Boolean::class.javaPrimitiveType, Function0::class.java,
            ).apply { isAccessible = true }
            method.invoke(usb, device, token, permissionGranted, attachedDevice, hasPermission, supported, conflictsWithActiveDevice, {})
            Secu3ConnectionManager::class.java.getDeclaredMethod("synchronizeUsbOutcome").apply { isAccessible = true }.invoke(manager)
        }

        fun pendingPermission() = UsbConnection::class.java.getDeclaredField("pendingPermission").apply { isAccessible = true }.get(usb) as UsbPermissionRequest?

        fun publishConnectionStateForTest(state: ConnectionState) {
            val publish = Secu3ConnectionManager::class.java.getDeclaredMethod("publishConnectionState", ConnectionState::class.java).apply { isAccessible = true }
            publish.invoke(manager, state)
        }

        fun selectBluetoothSessionForTest(): kotlinx.coroutines.Job = btDriver.select("BLUETOOTH")
        suspend fun startBluetoothTransport() {
            connectionField("isRunning").setBoolean(bt, false)
            bt.startConnection()
        }
        suspend fun emitBluetoothEvent(event: ConnectionEvent) = btDriver.emitEvent(event)

        fun activeUsbRunJob(): kotlinx.coroutines.Job? {
            return (managerField("currentConnection").get(manager) as? Connection<*>)
                ?.takeIf { it === usb }?.let { connectionField("connectionJob").get(usb) as? kotlinx.coroutines.Job }
        }
        fun serviceIsStarted() = serviceContext.commandHistory.lastOrNull() == true

        private fun managerField(name: String) = Secu3ConnectionManager::class.java.getDeclaredField(name).apply { isAccessible = true }
        private fun connectionField(name: String) = Connection::class.java.getDeclaredField(name).apply { isAccessible = true }
    }

    private class FakeUsbConnection(manager: UsbManager, prefs: UserPrefs) : UsbConnection(manager, prefs) {
        var nextPort: (() -> UsbSerialPort)? = null
        var openDelayMillis = 0L
        override fun connectToDevice(device: UsbDevice): UsbSerialPort {
            val port = nextPort?.invoke() ?: return super.connectToDevice(device)
            check(installEndpoint(port))
            if (openDelayMillis > 0) Thread.sleep(openDelayMillis)
            return port
        }
    }

    private class DelayedPreferencesDataStore(initial: Preferences) : DataStore<Preferences> {
        val initialReadStarted = CompletableDeferred<Unit>()
        val releaseInitialRead = CompletableDeferred<Unit>()
        var failOnceAfterInitialEmission = false
        private val failedReaders = java.util.concurrent.ConcurrentHashMap.newKeySet<kotlinx.coroutines.Job>()
        private val values = MutableStateFlow(initial)
        override val data = flow {
            initialReadStarted.complete(Unit)
            releaseInitialRead.await()
            emit(values.value)
            if (failOnceAfterInitialEmission && failedReaders.add(kotlinx.coroutines.currentCoroutineContext()[kotlinx.coroutines.Job]!!)) {
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
        val commandHistory = CopyOnWriteArrayList<Boolean>()
        var failNextStart = false

        override fun startForegroundService(service: Intent): ComponentName? {
            if (failNextStart) { failNextStart = false; throw IllegalStateException("service start failed") }
            startedServices += Intent(service)
            commandHistory += true
            return service.component
        }

        override fun startService(service: Intent): ComponentName? {
            if (failNextStart) { failNextStart = false; throw IllegalStateException("service start failed") }
            startedServices += Intent(service)
            commandHistory += true
            return service.component
        }

        override fun stopService(name: Intent): Boolean {
            stoppedServices += Intent(name)
            commandHistory += false
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

        private fun firmwarePacketWire(): ByteArray {
            val base = intArrayOf('@'.code, 'y'.code) + "SECU-3 test firmware".padEnd(52).map { it.code }.toIntArray() +
                intArrayOf(0, 0, 0, 0, 0x10)
            val checksum = PacketReceiver.calculateChecksum(base.copyOfRange(2, base.size))
            return (base + intArrayOf(checksum[1], checksum[0], 0x0d)).map(Int::toByte).toByteArray()
        }

    private fun sensorFlowFrame() = RawPacket(
        intArrayOf('@'.code, SensorsPacket.DESCRIPTOR.code) + IntArray(116).also {
            it[29] = 0x0C; it[30] = 0x80 // 100 km/h
            it[34] = 0x0A; it[35] = 0x00 // 10 Hz fuel flow
        } + intArrayOf(0, 0),
    )
    }
}
