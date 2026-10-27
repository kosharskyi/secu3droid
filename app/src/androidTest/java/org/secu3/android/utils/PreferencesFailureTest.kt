/*
 *    SecuDroid  - An open source, free manager for SECU-3 engine control unit
 *    Copyright (C) 2026 Vitalii O. Kosharskyi. Ukraine, Kyiv
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 */
package org.secu3.android.utils

import android.content.ContextWrapper
import android.content.Context
import android.app.DownloadManager
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.ViewModelStore
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.io.IOException
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.secu3.android.R
import org.secu3.android.models.packets.input.SensorsPacket
import org.secu3.android.network.ApiService
import org.secu3.android.network.models.Author
import org.secu3.android.network.models.GitHubRelease
import org.secu3.android.ui.home.HomeRepository
import org.secu3.android.ui.messages.UiMessageQueue
import org.secu3.android.ui.settings.SettingsDialog
import org.secu3.android.ui.settings.SettingsViewModel
import retrofit2.Response

class PreferencesFailureTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext

    @Test
    fun failedWriteKeepsTheDialogAndStoredValueUntilASuccessfulRetry() = runBlocking {
        val fixture = IsolatedUserPrefs(context, "failed-write-${UUID.randomUUID()}")
        val storage = FailingDataStore(fixture.dataStore)
        val prefs = UserPrefs(context, storage)
        val messages = UiMessageQueue()
        val viewModelStore = ViewModelStore()
        lateinit var viewModel: SettingsViewModel
        try {
            prefs.setCsvDelimiter(";")
            instrumentation.runOnMainSync {
                viewModel = SettingsViewModel(context, prefs, messages)
                viewModelStore.put("settings", viewModel)
                viewModel.showDialog(SettingsDialog.Delimiter)
            }
            val initial = withTimeout(5_000) { viewModel.state.filterNotNull().first() }

            storage.failWrites = true
            instrumentation.runOnMainSync { viewModel.setDelimiter(":") }
            withTimeout(5_000) {
                messages.pending.first { pending -> pending.any { it.textRes == R.string.preferences_storage_error } }
            }
            assertEquals(SettingsDialog.Delimiter, viewModel.dialog.value)
            assertEquals(initial, viewModel.state.value)
            assertEquals(";", prefs.settings.first().csvDelimiter)

            storage.failWrites = false
            instrumentation.runOnMainSync { viewModel.setDelimiter(":") }
            withTimeout(5_000) { viewModel.state.filterNotNull().first { it.csvDelimiter == ":" } }
            withTimeout(5_000) { viewModel.dialog.first { it == null } }
            assertEquals(":", prefs.settings.first().csvDelimiter)
        } finally {
            instrumentation.runOnMainSync { viewModelStore.clear() }
            fixture.close()
        }
    }

    @Test
    fun reusedObservationFlowReportsReadFailureToEachCollectorAndRecoversWithoutResettingData() = runBlocking {
        val fixture = IsolatedUserPrefs(context, "failed-observation-${UUID.randomUUID()}")
        val storage = FailingDataStore(fixture.dataStore)
        val prefs = UserPrefs(context, storage)
        prefs.setCsvDelimiter("|")
        storage.failReads = true
        val failures = AtomicInteger()
        val bothFailures = CompletableDeferred<Unit>()
        val observed = prefs.observeSettings {
            if (failures.incrementAndGet() == 2) bothFailures.complete(Unit)
        }
        val first = async(start = CoroutineStart.UNDISPATCHED) { observed.first() }
        val second = async(start = CoroutineStart.UNDISPATCHED) { observed.first() }
        try {
            withTimeout(5_000) { bothFailures.await() }
            storage.failReads = false
            assertEquals("|", withTimeout(5_000) { first.await() }.csvDelimiter)
            assertEquals("|", withTimeout(5_000) { second.await() }.csvDelimiter)
            assertEquals(2, failures.get())
            assertEquals("|", fixture.prefs.settings.first().csvDelimiter)
        } finally {
            first.cancelAndJoin()
            second.cancelAndJoin()
            fixture.close()
        }
    }

    @Test
    fun loggerReportsPreferenceReadFailuresOnStartAndDuringRecordingAndCanRecover() = runBlocking {
        val fixture = IsolatedUserPrefs(context, "failed-logger-${UUID.randomUUID()}")
        val storage = FailingDataStore(fixture.dataStore)
        val prefs = UserPrefs(context, storage)
        val root = File(context.cacheDir, "failed-logger-files-${UUID.randomUUID()}").also { it.mkdirs() }
        val fileContext = object : ContextWrapper(context) {
            override fun getFilesDir(): File = root
        }
        val files = FileHelper(fileContext)
        val logger = SecuLogger(prefs, files)
        try {
            prefs.setSensorLoggerEnabled(true)
            storage.failReads = true
            logger.startLogging()
            withTimeout(5_000) { logger.awaitCommandsForTests() }
            assertFalse(logger.isLoggerStarted)
            val startError = logger.state.value.pendingErrorId
            assertNotNull(startError)
            assertTrue(files.listOfLogs.isEmpty())

            storage.failReads = false
            logger.acknowledgeLoggerError(requireNotNull(startError))
            logger.startLogging()
            withTimeout(5_000) { logger.awaitCommandsForTests() }
            assertTrue(logger.isLoggerStarted)
            withTimeout(5_000) { logger.logPacket(SensorsPacket()) }
            val activeFile = files.listOfLogs.single()
            val acceptedBytes = activeFile.readBytes()

            storage.failReads = true
            withTimeout(5_000) { logger.logPacket(SensorsPacket(rpm = 2_000)) }
            assertFalse(logger.isLoggerStarted)
            assertTrue(requireNotNull(logger.state.value.pendingErrorId) > startError)
            assertArrayEquals(acceptedBytes, activeFile.readBytes())
        } finally {
            logger.stopLogging()
            withTimeout(5_000) { logger.shutdownForTests() }
            fixture.close()
            root.deleteRecursively()
        }
    }

    @Test
    fun releaseCheckDoesNotHideTimestampWriteFailureAsANetworkFailure() = runBlocking {
        val fixture = IsolatedUserPrefs(context, "failed-release-date-${UUID.randomUUID()}")
        val storage = FailingDataStore(fixture.dataStore)
        val prefs = AppPrefs(storage)
        val api = object : ApiService {
            var failRequest = false
            override suspend fun getReleases(): Result<List<GitHubRelease>> = Result.success(emptyList())
            override suspend fun getLatestRelease(): Response<GitHubRelease> {
                if (failRequest) throw IOException("Network unavailable")
                return Response.success(release())
            }
        }
        val repository = HomeRepository(api, context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager, prefs)
        try {
            val initialDate = prefs.lastAppVersionCheck.first()
            storage.failWrites = true
            val failure = runCatching { repository.getNewRelease() }.exceptionOrNull()
            assertTrue(failure is IOException)
            assertEquals("Preference write failed", failure?.message)
            assertEquals(initialDate, prefs.lastAppVersionCheck.first())

            api.failRequest = true
            assertNull(repository.getNewRelease())
        } finally {
            fixture.close()
        }
    }

    private fun release() = GitHubRelease(
        url = "", htmlUrl = "", assetsUrl = "", uploadUrl = "", tarballUrl = "", zipballUrl = "",
        id = 1, nodeId = "", tagName = "v999.0.0", targetCommitish = "main", name = "Release",
        body = "", draft = false, prerelease = false, createdAt = "", publishedAt = "",
        author = Author(
            login = "", id = 1, nodeId = "", avatarUrl = "", gravatarId = "", url = "", htmlUrl = "",
            followersUrl = "", followingUrl = "", gistsUrl = "", starredUrl = "", subscriptionsUrl = "",
            organizationsUrl = "", reposUrl = "", eventsUrl = "", receivedEventsUrl = "", type = "User", siteAdmin = false,
        ), assets = emptyList(),
    )

    private class FailingDataStore(private val delegate: DataStore<Preferences>) : DataStore<Preferences> {
        @Volatile var failReads = false
        @Volatile var failWrites = false

        override val data: Flow<Preferences> = flow {
            if (failReads) throw IOException("Preference read failed")
            emitAll(delegate.data)
        }

        override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences {
            if (failWrites) throw IOException("Preference write failed")
            return delegate.updateData(transform)
        }
    }
}
