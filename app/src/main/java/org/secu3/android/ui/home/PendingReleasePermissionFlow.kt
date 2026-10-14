package org.secu3.android.ui.home

import org.secu3.android.network.models.GitHubRelease

internal class PendingReleasePermissionFlow {
    private var requestedRelease: GitHubRelease? = null
    fun begin(release: GitHubRelease) { requestedRelease = release }
    fun complete(granted: Boolean): GitHubRelease? = requestedRelease.also { requestedRelease = null }.takeIf { granted }
    fun cancel() { requestedRelease = null }
}
