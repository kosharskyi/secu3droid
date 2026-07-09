package org.secu3.android.ui.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.secu3.android.network.models.Asset
import org.secu3.android.network.models.Author
import org.secu3.android.network.models.GitHubRelease

class HomeRepositoryTest {

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

        val actualAsset = HomeRepository.findReleaseApkAsset(release)

        assertEquals(expectedAsset, actualAsset)
    }

    @Test
    fun findReleaseApkAsset_ignoresApkWithVersionSuffix() {
        val release = release(
            tagName = "v0.18.0",
            assets = listOf(asset("Secu3Droid-v0.18.0-debug.apk"))
        )

        val actualAsset = HomeRepository.findReleaseApkAsset(release)

        assertNull(actualAsset)
    }

    @Test
    fun findReleaseApkAsset_ignoresGenericApkAsset() {
        val release = release(
            tagName = "v0.18.0",
            assets = listOf(asset("app-release.apk"))
        )

        val actualAsset = HomeRepository.findReleaseApkAsset(release)

        assertNull(actualAsset)
    }

    @Test
    fun findReleaseApkAsset_returnsNullWhenExpectedAssetIsMissing() {
        val release = release(tagName = "v0.18.0", assets = emptyList())

        val actualAsset = HomeRepository.findReleaseApkAsset(release)

        assertNull(actualAsset)
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
