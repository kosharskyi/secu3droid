/*
 *    SecuDroid  - An open source, free manager for SECU-3 engine control unit
 *    Copyright (C) 2024 Vitalii O. Kosharskyi. Ukraine, Kyiv
 *
 *    SECU-3  - An open source, free engine control unit
 *    Copyright (C) 2007-2024 Alexey A. Shabelnikov. Ukraine, Kyiv
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

package org.secu3.android.network.models

import com.google.gson.annotations.SerializedName

data class GitHubRelease(
    val url: String,

    @SerializedName("html_url")
    val htmlUrl: String,

    @SerializedName("assets_url")
    val assetsUrl: String,

    @SerializedName("upload_url")
    val uploadUrl: String,

    @SerializedName("tarball_url")
    val tarballUrl: String,

    @SerializedName("zipball_url")
    val zipballUrl: String,

    @SerializedName("id")
    val id: Int,

    @SerializedName("node_id")
    val nodeId: String,

    @SerializedName("tag_name")
    val tagName: String,

    @SerializedName("target_commitish")
    val targetCommitish: String,

    @SerializedName("name")
    val name: String,

    @SerializedName("body")
    val body: String,

    @SerializedName("draft")
    val draft: Boolean,

    @SerializedName("prerelease")
    val prerelease: Boolean,

    @SerializedName("created_at")
    val createdAt: String,

    @SerializedName("published_at")
    val publishedAt: String,

    @SerializedName("author")
    val author: Author,

    @SerializedName("assets")
    val assets: List<Asset>
)
