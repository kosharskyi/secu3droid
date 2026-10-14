package org.secu3.android.ui.home

import org.junit.Assert.assertEquals
import org.junit.Test
import org.secu3.android.network.models.Author
import org.secu3.android.network.models.GitHubRelease

class PendingReleasePermissionFlowTest {
    @Test fun permissionResultConsumesOnlyTheSelectedReleaseOnce() {
        val flow = PendingReleasePermissionFlow()
        val selected = release(1)
        flow.begin(selected)
        flow.begin(release(2))
        assertEquals(release(2), flow.complete(granted = true))
        assertEquals(null, flow.complete(granted = true))
    }

    @Test fun cancelAndDeniedResultNeverProduceARelease() {
        val flow = PendingReleasePermissionFlow()
        flow.begin(release(1))
        flow.cancel()
        assertEquals(null, flow.complete(granted = true))
        flow.begin(release(2))
        assertEquals(null, flow.complete(granted = false))
    }

    private fun release(id: Int) = GitHubRelease(
        url = "", htmlUrl = "", assetsUrl = "", uploadUrl = "", tarballUrl = "", zipballUrl = "",
        id = id, nodeId = "", tagName = "v$id", targetCommitish = "main", name = "Release $id",
        body = "", draft = false, prerelease = false, createdAt = "", publishedAt = "",
        author = Author(
            login = "", id = 1, nodeId = "", avatarUrl = "", gravatarId = "", url = "", htmlUrl = "",
            followersUrl = "", followingUrl = "", gistsUrl = "", starredUrl = "", subscriptionsUrl = "",
            organizationsUrl = "", reposUrl = "", eventsUrl = "", receivedEventsUrl = "", type = "User", siteAdmin = false,
        ), assets = emptyList(),
    )
}
