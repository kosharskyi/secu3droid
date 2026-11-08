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

    internal constructor(
        context: Context,
        userPrefs: UserPrefs,
        debugBuild: Boolean,
        updateSdkCollection: (Boolean) -> Unit,
        sendEvent: (String, Bundle) -> Unit,
    ) : this(context, userPrefs) {
        this.debugBuild = debugBuild
        this.updateSdkCollection = updateSdkCollection
        this.sendEvent = sendEvent
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val lock = Any()
    private val consentMutex = Mutex()
    @Volatile private var collectionEnabled = false
    private var sdkInitialized = false
    private var pendingConsent: Boolean? = null
    private var started = false

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
        } catch (failure: Exception) {
            collectionEnabled = false
            Log.e(TAG, "Could not enable Firebase Analytics", failure)
        }
    }

    private companion object { const val TAG = "UsageAnalytics" }
}
