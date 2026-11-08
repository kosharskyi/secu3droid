/*
 *    SecuDroid  - An open source, free manager for SECU-3 engine control unit
 *    Copyright (C) 2026 Vitalii O. Kosharskyi. Ukraine, Kyiv
 *
 *    SECU-3  - An open source, free engine control unit
 *    Copyright (C) 2007-2026 Alexey A. Shabelnikov. Ukraine, Kyiv
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *    contacts:
 *                    http://secu-3.org
 *                    email: vetalkosharskiy@gmail.com
 */

package org.secu3.android.utils.analytics

import android.os.Bundle
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.test.platform.app.InstrumentationRegistry
import java.io.IOException
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.secu3.android.utils.prefs.IsolatedUserPrefs
import org.secu3.android.utils.prefs.UserPrefs

class UsageAnalyticsTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun releasePathStaysLazyUntilConsentThenRevokesCollectionAndEventsImmediately() = runBlocking {
        val fixture = IsolatedUserPrefs(context, UUID.randomUUID().toString())
        val collectionChanges = CopyOnWriteArrayList<Boolean>()
        val events = CopyOnWriteArrayList<Pair<String, Bundle>>()
        val analytics = UsageAnalytics(
            context,
            fixture.prefs,
            debugBuild = false,
            updateSdkCollection = collectionChanges::add,
            sendEvent = { name, params -> events += name to Bundle(params) },
        )
        try {
            analytics.start()
            fixture.prefs.settings.first()
            analytics.logScreenOpened("settings")
            assertTrue(collectionChanges.isEmpty())
            assertTrue(events.isEmpty())

            analytics.setUserConsent(true)
            withTimeout(5_000) { while (collectionChanges.lastOrNull() != true) kotlinx.coroutines.yield() }
            analytics.logRecordingStarted("s3l")
            assertEquals("log_recording_started", events.single().first)
            assertEquals("s3l", events.single().second.getString("format"))

            analytics.setUserConsent(false)
            assertEquals(false, collectionChanges.last())
            analytics.logScreenOpened("settings")
            assertEquals(1, events.size)
            assertFalse(fixture.prefs.settings.first().isUsageAnalyticsEnabled)
        } finally {
            analytics.stopForTests()
            fixture.close()
        }
    }

    @Test
    fun unrelatedPreferenceSnapshotCannotReenablePendingRevocationAfterWriteFailure() = runBlocking {
        val fixture = IsolatedUserPrefs(context, UUID.randomUUID().toString())
        val store = ToggleWriteDataStore(fixture.dataStore)
        val prefs = UserPrefs(context, store)
        val collectionChanges = CopyOnWriteArrayList<Boolean>()
        val analytics = UsageAnalytics(
            context,
            prefs,
            debugBuild = false,
            updateSdkCollection = collectionChanges::add,
            sendEvent = { _, _ -> },
        )
        try {
            analytics.setUserConsent(true)
            analytics.start()
            withTimeout(5_000) { while (collectionChanges.lastOrNull() != true) kotlinx.coroutines.yield() }

            store.failWrites = true
            assertTrue(runCatching { analytics.setUserConsent(false) }.isFailure)
            assertEquals(false, collectionChanges.last())

            store.failWrites = false
            prefs.setCsvDelimiter("|")
            withTimeout(5_000) { prefs.settings.first { it.csvDelimiter == "|" } }
            kotlinx.coroutines.delay(100)
            assertEquals(false, collectionChanges.last())
        } finally {
            analytics.stopForTests()
            fixture.close()
        }
    }

    @Test
    fun debugBuildNeverInitializesOrEmitsEvenWithSavedConsent() = runBlocking {
        val fixture = IsolatedUserPrefs(context, UUID.randomUUID().toString())
        val collectionChanges = CopyOnWriteArrayList<Boolean>()
        val events = CopyOnWriteArrayList<String>()
        val analytics = UsageAnalytics(
            context,
            fixture.prefs,
            debugBuild = true,
            updateSdkCollection = collectionChanges::add,
            sendEvent = { name, _ -> events += name },
        )
        try {
            fixture.prefs.setUsageAnalyticsEnabled(true)
            analytics.start()
            analytics.logConnectionAttempt("usb")
            analytics.connectionRunStarted("usb")
            analytics.connectionBecameConnected()
            analytics.logExportResult("success")
            analytics.connectionRunEnded()
            assertTrue(collectionChanges.isEmpty())
            assertTrue(events.isEmpty())
        } finally {
            analytics.stopForTests()
            fixture.close()
        }
    }

    @Test
    fun settingsReadFailureAfterRevocationFailsClosedAndSdkErrorsStayLocal() = runBlocking {
        val fixture = IsolatedUserPrefs(context, UUID.randomUUID().toString())
        val store = ToggleReadDataStore(fixture.dataStore)
        val prefs = UserPrefs(context, store)
        val collectionChanges = CopyOnWriteArrayList<Boolean>()
        val events = CopyOnWriteArrayList<String>()
        val analytics = UsageAnalytics(
            context,
            prefs,
            debugBuild = false,
            updateSdkCollection = collectionChanges::add,
            sendEvent = { name, _ -> events += name },
        )
        try {
            analytics.start()
            analytics.setUserConsent(true)
            withTimeout(5_000) { while (collectionChanges.lastOrNull() != true) kotlinx.coroutines.yield() }

            store.failReads = true
            runCatching { analytics.setUserConsent(false) }
            assertEquals(false, collectionChanges.last())
            analytics.logScreenOpened("settings")
            assertTrue(events.isEmpty())
        } finally {
            analytics.stopForTests()
            fixture.close()
        }
    }

    @Test
    fun sdkEnableAndEventSendFailuresDoNotEscapeTheAnalyticsOwner() = runBlocking {
        val fixture = IsolatedUserPrefs(context, UUID.randomUUID().toString())
        val analytics = UsageAnalytics(
            context,
            fixture.prefs,
            debugBuild = false,
            updateSdkCollection = { if (it) throw IllegalStateException("SDK init failed") },
            sendEvent = { _, _ -> throw IllegalStateException("SDK event failed") },
        )
        try {
            analytics.setUserConsent(true)
            analytics.logScreenOpened("settings")
            analytics.stopForTests()

            val eventFailure = UsageAnalytics(
                context,
                fixture.prefs,
                debugBuild = false,
                updateSdkCollection = {},
                sendEvent = { _, _ -> throw IllegalStateException("SDK event failed") },
            )
            eventFailure.setUserConsent(true)
            eventFailure.logScreenOpened("settings")
            eventFailure.stopForTests()
        } finally {
            analytics.stopForTests()
            fixture.close()
        }
    }

    @Test
    fun connectionDurationSumsEnabledConnectedSegmentsAndOnlyEmitsOnce() = runBlocking {
        val fixture = IsolatedUserPrefs(context, UUID.randomUUID().toString())
        val events = CopyOnWriteArrayList<Pair<String, Bundle>>()
        var now = 0L
        val analytics = UsageAnalytics(
            context,
            fixture.prefs,
            debugBuild = false,
            updateSdkCollection = {},
            sendEvent = { name, params -> events += name to Bundle(params) },
            elapsedRealtime = { now },
        )
        try {
            analytics.setUserConsent(true)
            analytics.connectionRunStarted("usb")
            analytics.connectionBecameConnected()
            now = 1_500L
            analytics.connectionRetrying()
            now += 45_000L
            analytics.connectionBecameConnected()
            now += 2_500L

            analytics.connectionRunEnded()
            analytics.connectionRunEnded()

            val ended = events.single { it.first == "connection_ended" }.second
            assertEquals("usb", ended.getString("transport"))
            assertEquals(4L, ended.getLong("duration_seconds"))
            assertEquals(1, events.count { it.first == "connection_ended" })
        } finally {
            analytics.stopForTests()
            fixture.close()
        }
    }

    @Test
    fun revokingConsentDropsPriorDurationAndReenablingWhileConnectedStartsFreshSegment() = runBlocking {
        val fixture = IsolatedUserPrefs(context, UUID.randomUUID().toString())
        val events = CopyOnWriteArrayList<Pair<String, Bundle>>()
        var now = 0L
        val analytics = UsageAnalytics(
            context,
            fixture.prefs,
            debugBuild = false,
            updateSdkCollection = {},
            sendEvent = { name, params -> events += name to Bundle(params) },
            elapsedRealtime = { now },
        )
        try {
            analytics.setUserConsent(true)
            analytics.connectionRunStarted("bluetooth")
            analytics.connectionBecameConnected()
            now = 10_000L
            analytics.setUserConsent(false)
            now += 20_000L
            analytics.setUserConsent(true)
            now += 2_500L
            analytics.connectionRunEnded()

            val ended = events.single { it.first == "connection_ended" }.second
            assertEquals("bluetooth", ended.getString("transport"))
            assertEquals(2L, ended.getLong("duration_seconds"))
        } finally {
            analytics.stopForTests()
            fixture.close()
        }
    }

    @Test
    fun waitingWithoutConfirmedConnectionDoesNotCreateDurationEvent() = runBlocking {
        val fixture = IsolatedUserPrefs(context, UUID.randomUUID().toString())
        val events = CopyOnWriteArrayList<String>()
        var now = 0L
        val analytics = UsageAnalytics(
            context,
            fixture.prefs,
            debugBuild = false,
            updateSdkCollection = {},
            sendEvent = { name, _ -> events += name },
            elapsedRealtime = { now },
        )
        try {
            analytics.setUserConsent(true)
            analytics.connectionRunStarted("usb")
            now = 15_000L
            analytics.connectionRetrying()
            analytics.connectionRunEnded()
            assertTrue(events.none { it == "connection_ended" })
        } finally {
            analytics.stopForTests()
            fixture.close()
        }
    }

    @Test
    fun confirmedSubsecondConnectionReportsZeroSeconds() = runBlocking {
        val fixture = IsolatedUserPrefs(context, UUID.randomUUID().toString())
        val events = CopyOnWriteArrayList<Pair<String, Bundle>>()
        var now = 0L
        val analytics = UsageAnalytics(
            context,
            fixture.prefs,
            debugBuild = false,
            updateSdkCollection = {},
            sendEvent = { name, params -> events += name to Bundle(params) },
            elapsedRealtime = { now },
        )
        try {
            analytics.setUserConsent(true)
            analytics.connectionRunStarted("usb")
            analytics.connectionBecameConnected()
            now = 250L
            analytics.connectionRunEnded()

            assertEquals(0L, events.single { it.first == "connection_ended" }.second.getLong("duration_seconds"))
        } finally {
            analytics.stopForTests()
            fixture.close()
        }
    }

    @Test
    fun readFailureDropsElapsedDurationBeforeConsentCanBeEnabledAgain() = runBlocking {
        val fixture = IsolatedUserPrefs(context, UUID.randomUUID().toString())
        val store = ToggleReadDataStore(fixture.dataStore)
        val prefs = UserPrefs(context, store)
        val events = CopyOnWriteArrayList<Pair<String, Bundle>>()
        var now = 0L
        val analytics = UsageAnalytics(
            context,
            prefs,
            debugBuild = false,
            updateSdkCollection = {},
            sendEvent = { name, params -> events += name to Bundle(params) },
            elapsedRealtime = { now },
        )
        try {
            analytics.setUserConsent(true)
            analytics.connectionRunStarted("usb")
            analytics.connectionBecameConnected()
            now = 10_000L
            store.failReads = true
            runCatching { analytics.setUserConsent(false) }
            store.failReads = false
            now += 20_000L
            analytics.setUserConsent(true)
            now += 2_500L
            analytics.connectionRunEnded()

            assertEquals(2L, events.single { it.first == "connection_ended" }.second.getLong("duration_seconds"))
        } finally {
            analytics.stopForTests()
            fixture.close()
        }
    }

    private class ToggleWriteDataStore(private val delegate: DataStore<Preferences>) : DataStore<Preferences> {
        @Volatile var failWrites = false
        override val data: Flow<Preferences> get() = delegate.data

        override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences {
            if (failWrites) throw IOException("Preference write failed")
            return delegate.updateData(transform)
        }
    }

    private class ToggleReadDataStore(private val delegate: DataStore<Preferences>) : DataStore<Preferences> {
        @Volatile var failReads = false
        override val data: Flow<Preferences> get() = flow {
            if (failReads) throw IOException("Preference read failed")
            emitAll(delegate.data)
        }

        override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences =
            delegate.updateData(transform)
    }
}
