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

import android.content.Context
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import com.google.firebase.analytics.FirebaseAnalytics
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.secu3.android.BuildConfig
import org.secu3.android.utils.prefs.UserPrefs

@Singleton
class UsageAnalytics @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userPrefs: UserPrefs,
) {
    private var debugBuild = BuildConfig.DEBUG
    private var updateSdkCollection: (Boolean) -> Unit = { enabled ->
        FirebaseAnalytics.getInstance(context).setAnalyticsCollectionEnabled(enabled)
    }
    private var sendEvent: (String, Bundle) -> Unit = { event, parameters ->
        FirebaseAnalytics.getInstance(context).logEvent(event, parameters)
    }
    private var elapsedRealtime: () -> Long = SystemClock::elapsedRealtime

    internal constructor(
        context: Context,
        userPrefs: UserPrefs,
        debugBuild: Boolean,
        updateSdkCollection: (Boolean) -> Unit,
        sendEvent: (String, Bundle) -> Unit,
        elapsedRealtime: () -> Long = SystemClock::elapsedRealtime,
    ) : this(context, userPrefs) {
        this.debugBuild = debugBuild
        this.updateSdkCollection = updateSdkCollection
        this.sendEvent = sendEvent
        this.elapsedRealtime = elapsedRealtime
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val lock = Any()
    private val consentMutex = Mutex()
    @Volatile private var collectionEnabled = false
    private var sdkInitialized = false
    private var pendingConsent: Boolean? = null
    private var started = false
    private var connectionTransport: String? = null
    private var connectionConnected = false
    private var connectionElapsedMilliseconds = 0L
    private var connectedSinceMilliseconds: Long? = null
    private var measuredConnectedSegment = false

    fun start() {
        if (debugBuild) return
        synchronized(lock) {
            if (started) return
            started = true
        }
        scope.launch {
            userPrefs.observeSettings {
                synchronized(lock) { applyCollectionEnabled(false) }
            }.collect { settings ->
                synchronized(lock) {
                    val pending = pendingConsent
                    if (pending != null && pending != settings.isUsageAnalyticsEnabled) return@synchronized
                    applyCollectionEnabled(settings.isUsageAnalyticsEnabled)
                }
            }
        }
    }

    internal fun stopForTests() {
        scope.cancel()
    }

    /** Called before persisting a user's choice so turning collection off takes effect immediately. */
    private fun prepareConsentChange(enabled: Boolean) {
        if (debugBuild) return
        synchronized(lock) {
            pendingConsent = enabled
            if (!enabled) applyCollectionEnabled(false)
        }
    }

    suspend fun setUserConsent(enabled: Boolean) = consentMutex.withLock {
        prepareConsentChange(enabled)
        userPrefs.setUsageAnalyticsEnabled(enabled)
        val persisted = try {
            userPrefs.settings.first().isUsageAnalyticsEnabled
        } catch (failure: Exception) {
            synchronized(lock) { applyCollectionEnabled(false) }
            throw failure
        }
        synchronized(lock) {
            if (persisted == enabled) applyCollectionEnabled(enabled)
            else applyCollectionEnabled(false)
        }
    }

    fun logConnectionAttempt(transport: String) = log(
        "connection_attempt",
        "transport" to transport,
    )

    fun logConnectionResult(transport: String, result: String, category: String, scope: String) = log(
        "connection_result",
        "transport" to transport,
        "result" to result,
        "category" to category,
        "scope" to scope,
    )

    fun logScreenOpened(screen: String) = log(
        "screen_opened",
        "screen_name" to screen,
    )

    fun logRecordingStarted(format: String) = log(
        "log_recording_started",
        "format" to format,
    )

    fun logExportResult(result: String) = log(
        "log_export_result",
        "result" to result,
    )

    fun connectionRunStarted(transport: String) = synchronized(lock) {
        connectionTransport = transport
        connectionConnected = false
        connectionElapsedMilliseconds = 0L
        connectedSinceMilliseconds = null
        measuredConnectedSegment = false
    }

    fun connectionBecameConnected() = synchronized(lock) {
        if (connectionTransport == null || connectionConnected) return@synchronized
        connectionConnected = true
        if (collectionEnabled) startConnectedSegment()
    }

    fun connectionRetrying() = synchronized(lock) {
        if (!connectionConnected) return@synchronized
        closeConnectedSegment()
        connectionConnected = false
    }

    fun connectionRunEnded() = synchronized(lock) {
        val transport = connectionTransport ?: return@synchronized
        closeConnectedSegment()
        if (collectionEnabled && measuredConnectedSegment) {
            logConnectionEndedLocked(transport, connectionElapsedMilliseconds / 1_000L)
        }
        clearConnectionTiming()
    }

    private fun log(event: String, vararg parameters: Pair<String, String>) {
        synchronized(lock) {
            if (debugBuild || !collectionEnabled) return
            val bundle = Bundle().apply {
                parameters.forEach { (key, value) -> putString(key, value) }
            }
            try {
                sendEvent(event, bundle)
            } catch (failure: Exception) {
                Log.e(TAG, "Could not send analytics event $event", failure)
            }
        }
    }

    private fun applyCollectionEnabled(enabled: Boolean) {
        val target = enabled && !debugBuild
        if (!target) {
            closeConnectedSegment()
            connectionElapsedMilliseconds = 0L
            measuredConnectedSegment = false
            collectionEnabled = false
            if (sdkInitialized) runCatching { updateSdkCollection(false) }
                .onFailure { Log.e(TAG, "Could not disable Firebase Analytics", it) }
            return
        }
        if (collectionEnabled) return
        sdkInitialized = true
        try {
            updateSdkCollection(true)
            collectionEnabled = true
            if (connectionConnected) startConnectedSegment()
        } catch (failure: Exception) {
            collectionEnabled = false
            Log.e(TAG, "Could not enable Firebase Analytics", failure)
        }
    }

    private fun startConnectedSegment() {
        connectedSinceMilliseconds = elapsedRealtime()
        measuredConnectedSegment = true
    }

    private fun closeConnectedSegment() {
        val startedAt = connectedSinceMilliseconds ?: return
        connectionElapsedMilliseconds += (elapsedRealtime() - startedAt).coerceAtLeast(0L)
        connectedSinceMilliseconds = null
    }

    private fun clearConnectionTiming() {
        connectionTransport = null
        connectionConnected = false
        connectionElapsedMilliseconds = 0L
        connectedSinceMilliseconds = null
        measuredConnectedSegment = false
    }

    private fun logConnectionEndedLocked(transport: String, durationSeconds: Long) {
        val bundle = Bundle().apply {
            putString("transport", transport)
            putLong("duration_seconds", durationSeconds)
        }
        try {
            sendEvent("connection_ended", bundle)
        } catch (failure: Exception) {
            Log.e(TAG, "Could not send analytics event connection_ended", failure)
        }
    }

    private companion object { const val TAG = "UsageAnalytics" }
}
