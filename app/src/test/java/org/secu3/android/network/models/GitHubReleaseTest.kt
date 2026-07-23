package org.secu3.android.network.models

import org.junit.Assert.assertEquals
import org.junit.Test

class GitHubReleaseTest {

    @Test
    fun displayNameReturnsReleaseNameWhenPresent() {
        val release = release(name = "Secu3Droid 0.19.0")

        assertEquals("Secu3Droid 0.19.0", release.displayName)
    }

    @Test
    fun displayNameFallsBackToTagNameWhenReleaseNameIsBlank() {
        val release = release(name = " ")

        assertEquals("v0.19.0", release.displayName)
    }

    @Test
    fun displayNameFallsBackToTagNameWhenReleaseNameIsNull() {
        val release = release(name = null)

        assertEquals("v0.19.0", release.displayName)
    }

    private fun release(name: String?): GitHubRelease {
        return GitHubRelease(
            url = "",
            htmlUrl = "",
            assetsUrl = "",
            uploadUrl = "",
            tarballUrl = "",
            zipballUrl = "",
            id = 1,
            nodeId = "",
            tagName = "v0.19.0",
            targetCommitish = "master",
            name = name,
            body = "",
            draft = false,
            prerelease = false,
            createdAt = "",
            publishedAt = "",
            author = author(),
            assets = emptyList(),
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
            siteAdmin = false,
        )
    }
}
