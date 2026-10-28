package org.secu3.android.network.releases

import android.app.DownloadManager
import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import java.io.IOException
import java.util.UUID
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Before
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.secu3.android.network.ApiService
import org.secu3.android.network.models.Asset
import org.secu3.android.network.models.Author
import org.secu3.android.network.models.GitHubRelease
import org.secu3.android.utils.AppPrefs
import org.secu3.android.utils.IsolatedUserPrefs
import org.threeten.bp.LocalDate
import retrofit2.Response

class ReleaseRepositoryTest {

    private lateinit var fixture: IsolatedUserPrefs
    private lateinit var appPrefs: AppPrefs
    private lateinit var downloadManager: DownloadManager

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        fixture = IsolatedUserPrefs(context, "release-repository-${UUID.randomUUID()}")
        appPrefs = AppPrefs(fixture.dataStore)
        downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    }

    @After
    fun tearDown() = runBlocking { fixture.close() }

    @Test
    fun findReleaseApkAsset_returnsAssetWithExpectedReleaseName() {
        val expectedAsset = asset("Secu3Droid-v0.18.0.apk")
        val release = release(
            tagName = "v0.18.0",
            assets = listOf(
                asset("app-debug.apk"),
                expectedAsset,
                asset("Secu3Droid-v0.18.0-debug.apk")
            )
        )

        val actualAsset = repository(object : ApiService {
            override suspend fun getReleases() = Result.success(emptyList<GitHubRelease>())
            override suspend fun getLatestRelease() = Response.success(release)
        }).findReleaseApkAsset(release)

        assertEquals(expectedAsset, actualAsset)
    }

    @Test
    fun findReleaseApkAsset_ignoresApkWithVersionSuffix() {
        val release = release(
            tagName = "v0.18.0",
            assets = listOf(asset("Secu3Droid-v0.18.0-debug.apk"))
        )

        val actualAsset = repository(object : ApiService {
            override suspend fun getReleases() = Result.success(emptyList<GitHubRelease>())
            override suspend fun getLatestRelease() = Response.success(release)
        }).findReleaseApkAsset(release)

        assertNull(actualAsset)
    }

    @Test
    fun findReleaseApkAsset_ignoresGenericApkAsset() {
        val release = release(
            tagName = "v0.18.0",
            assets = listOf(asset("app-release.apk"))
        )

        val actualAsset = repository(object : ApiService {
            override suspend fun getReleases() = Result.success(emptyList<GitHubRelease>())
            override suspend fun getLatestRelease() = Response.success(release)
        }).findReleaseApkAsset(release)

        assertNull(actualAsset)
    }

    @Test
    fun findReleaseApkAsset_returnsNullWhenExpectedAssetIsMissing() {
        val release = release(tagName = "v0.18.0", assets = emptyList())

        val actualAsset = repository(object : ApiService {
            override suspend fun getReleases() = Result.success(emptyList<GitHubRelease>())
            override suspend fun getLatestRelease() = Response.success(release)
        }).findReleaseApkAsset(release)

        assertNull(actualAsset)
    }

    @Test
    fun productionChecksAtMostOncePerDayAndWritesDateAfterSuccess() = runBlocking {
        val api = CountingApi(release("v999.0.0", emptyList()))
        val today = LocalDate.of(2026, 10, 28)
        val repository = repository(api, debug = false, today = { today })

        assertEquals("v999.0.0", repository.getNewRelease()?.tagName)
        assertEquals(today, appPrefs.lastAppVersionCheck.first())
        assertNull(repository.getNewRelease())
        assertEquals(1, api.calls)
    }

    @Test
    fun productionSkipsChecksForTodayOrFutureTimestamp() = runBlocking {
        val api = CountingApi(release("v999.0.0", emptyList()))
        val today = LocalDate.of(2026, 10, 28)
        appPrefs.setLastAppVersionCheck(today.plusDays(1))
        val repository = repository(api, debug = false, today = { today })

        assertNull(repository.getNewRelease())
        assertEquals(0, api.calls)
    }

    @Test
    fun debugBuildChecksOnEveryCall() = runBlocking {
        val api = CountingApi(release("v999.0.0", emptyList()))
        val repository = repository(api, debug = true, today = { LocalDate.of(2026, 10, 28) })

        assertEquals("v999.0.0", repository.getNewRelease()?.tagName)
        assertEquals("v999.0.0", repository.getNewRelease()?.tagName)
        assertEquals(2, api.calls)
    }

    @Test
    fun concurrentProductionChecksShareOneRequest() = runBlocking {
        val requestStarted = CompletableDeferred<Unit>()
        val response = CompletableDeferred<GitHubRelease>()
        val api = object : ApiService {
            var calls = 0
            override suspend fun getReleases() = Result.success(emptyList<GitHubRelease>())
            override suspend fun getLatestRelease(): Response<GitHubRelease> {
                calls++
                requestStarted.complete(Unit)
                return Response.success(response.await())
            }
        }
        val repository = repository(api, debug = false, today = { LocalDate.of(2026, 10, 28) })
        val first = async(start = CoroutineStart.UNDISPATCHED) { repository.getNewRelease() }
        withTimeout(2_000) { requestStarted.await() }
        val second = async(start = CoroutineStart.UNDISPATCHED) { repository.getNewRelease() }
        response.complete(release("v999.0.0", emptyList()))

        assertEquals("v999.0.0", first.await()?.tagName)
        assertNull(second.await())
        assertEquals(1, api.calls)
    }

    @Test
    fun cancelledRequestReleasesMutexWithoutWritingDate() = runBlocking {
        val requestStarted = CompletableDeferred<Unit>()
        val api = object : ApiService {
            var calls = 0
            override suspend fun getReleases() = Result.success(emptyList<GitHubRelease>())
            override suspend fun getLatestRelease(): Response<GitHubRelease> {
                calls++
                if (calls == 1) {
                    requestStarted.complete(Unit)
                    awaitCancellation()
                }
                return Response.success(release("v999.0.0", emptyList()))
            }
        }
        val repository = repository(api, debug = false, today = { LocalDate.of(2026, 10, 28) })
        val initialDate = appPrefs.lastAppVersionCheck.first()
        val activeRequest = async(start = CoroutineStart.UNDISPATCHED) { repository.getNewRelease() }
        withTimeout(2_000) { requestStarted.await() }
        val waitingRequest = async(start = CoroutineStart.UNDISPATCHED) { repository.getNewRelease() }
        waitingRequest.cancelAndJoin()
        activeRequest.cancelAndJoin()

        assertEquals(initialDate, appPrefs.lastAppVersionCheck.first())
        assertEquals("v999.0.0", repository.getNewRelease()?.tagName)
        assertEquals(2, api.calls)
    }

    @Test
    fun successfulResponseAfterMidnightStoresResponseDay() = runBlocking {
        val yesterday = LocalDate.of(2026, 10, 27)
        val today = yesterday.plusDays(1)
        var dateReads = 0
        val api = CountingApi(release("v999.0.0", emptyList()))
        val repository = repository(api, debug = false) {
            if (dateReads++ == 0) yesterday else today
        }

        assertEquals("v999.0.0", repository.getNewRelease()?.tagName)
        assertEquals(today, appPrefs.lastAppVersionCheck.first())
    }

    @Test
    fun nextDayAllowsCheckAndMalformedReleaseStillConsumesThatDay() = runBlocking {
        val yesterday = LocalDate.of(2026, 10, 27)
        val today = yesterday.plusDays(1)
        appPrefs.setLastAppVersionCheck(yesterday)
        val api = CountingApi(release("invalid-version", emptyList()))

        assertNull(repository(api, debug = false, today = { today }).getNewRelease())
        assertEquals(1, api.calls)
        assertEquals(today, appPrefs.lastAppVersionCheck.first())
    }

    @Test
    fun networkFailureDoesNotWriteCheckDate() = runBlocking {
        val initialDate = appPrefs.lastAppVersionCheck.first()
        val api = object : ApiService {
            override suspend fun getReleases() = Result.success(emptyList<GitHubRelease>())
            override suspend fun getLatestRelease(): Response<GitHubRelease> = throw IOException("Offline")
        }
        assertNull(repository(api, debug = false, today = { LocalDate.of(2026, 10, 28) }).getNewRelease())
        assertEquals(initialDate, appPrefs.lastAppVersionCheck.first())
    }

    @Test
    fun releaseOlderThanInstalledVersionIsIgnoredAfterSuccessfulCheck() = runBlocking {
        val today = LocalDate.of(2026, 10, 28)
        val api = CountingApi(release("v0.19.1", emptyList()))
        val repository = repository(api, debug = false, versionName = "0.19.2", today = { today })

        assertNull(repository.getNewRelease())
        assertEquals(today, appPrefs.lastAppVersionCheck.first())
    }

    private fun repository(
        api: ApiService,
        debug: Boolean = true,
        versionName: String = if (debug) "0.19.2-debug" else "0.19.2",
        today: () -> LocalDate = LocalDate::now,
    ) = ReleaseRepository(api, downloadManager, appPrefs, debug, versionName, today)

    private class CountingApi(private val release: GitHubRelease) : ApiService {
        var calls = 0
        override suspend fun getReleases() = Result.success(emptyList<GitHubRelease>())
        override suspend fun getLatestRelease(): Response<GitHubRelease> {
            calls++
            return Response.success(release)
        }
    }

    private fun release(tagName: String, assets: List<Asset>): GitHubRelease {
        return GitHubRelease(
            url = "",
            htmlUrl = "",
            assetsUrl = "",
            uploadUrl = "",
            tarballUrl = "",
            zipballUrl = "",
            id = 1,
            nodeId = "",
            tagName = tagName,
            targetCommitish = "master",
            name = tagName,
            body = "",
            draft = false,
            prerelease = false,
            createdAt = "",
            publishedAt = "",
            author = author(),
            assets = assets
        )
    }

    private fun asset(name: String): Asset {
        return Asset(
            url = "",
            browserDownloadUrl = "https://example.com/$name",
            id = name.hashCode(),
            nodeId = "",
            name = name,
            label = "",
            state = "uploaded",
            contentType = "application/vnd.android.package-archive",
            size = 1,
            downloadCount = 0,
            createdAt = "",
            updatedAt = "",
            uploader = author()
        )
    }

    private fun author(): Author {
        return Author(
            login = "",
            id = 1,
            nodeId = "",
            avatarUrl = "",
            gravatarId = "",
            url = "",
            htmlUrl = "",
            followersUrl = "",
            followingUrl = "",
            gistsUrl = "",
            starredUrl = "",
            subscriptionsUrl = "",
            organizationsUrl = "",
            reposUrl = "",
            eventsUrl = "",
            receivedEventsUrl = "",
            type = "User",
            siteAdmin = false
        )
    }
}
