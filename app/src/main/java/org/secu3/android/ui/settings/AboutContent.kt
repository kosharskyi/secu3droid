/*
 *    SecuDroid  - An open source, free manager for SECU-3 engine control unit
 *    Copyright (C) 2024 Vitalii O. Kosharskyi. Ukraine, Kyiv
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 */
package org.secu3.android.ui.settings

import android.text.Spanned
import android.text.style.URLSpan
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import androidx.core.text.HtmlCompat
import org.secu3.android.BuildConfig
import org.secu3.android.R
import org.threeten.bp.LocalDateTime

@Composable
internal fun AboutContent(
    open: (String) -> Unit,
    onLinkBounds: ((resourceId: Int, bounds: Rect) -> Unit)? = null,
) {
    val year = LocalDateTime.now().year
    Surface {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(R.drawable.gplv3_icon),
                    contentDescription = null,
                    modifier = Modifier.height(24.dp),
                )
                Text(
                    text = stringResource(R.string.about_title),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(start = 16.dp),
                )
            }
            Text("${stringResource(R.string.about_version)} ${BuildConfig.VERSION_NAME}")
            Text(stringResource(R.string.about_copyright_1, year))
            Text(stringResource(R.string.about_copyright_2, year))
            LinkedText(R.string.about_app_desc, open, onLinkBounds)
            LinkedText(R.string.about_license, open, onLinkBounds)
            LinkedText(R.string.about_sources, open, onLinkBounds)
        }
    }
}

@Composable
private fun LinkedText(
    resourceId: Int,
    open: (String) -> Unit,
    onLinkBounds: ((resourceId: Int, bounds: Rect) -> Unit)?,
) {
    val resourceText = LocalResources.current.getText(resourceId)
    val styledText = if (resourceText is Spanned && resourceText.getSpans(0, resourceText.length, URLSpan::class.java).isNotEmpty()) {
        resourceText
    } else {
        // Some resource configurations expose literal HTML through getText; parse it before fallback matching.
        HtmlCompat.fromHtml(resourceText.toString(), HtmlCompat.FROM_HTML_MODE_LEGACY)
    }
    val spannedText = styledText
    val links = spannedText.let { text ->
        text.getSpans(0, text.length, URLSpan::class.java)
            .map { span -> Triple(text.getSpanStart(span), text.getSpanEnd(span), span.url) }
    }.orEmpty().toMutableList()

    // The source URL is plain text in the localized resources; the old TextView auto-linked it.
    if (resourceId == R.string.about_sources && links.isEmpty()) {
        sourceUrlRange(styledText.toString())?.let { (start, end) ->
            links += Triple(start, end, styledText.subSequence(start, end).toString())
        }
    }

    val annotated = buildAnnotatedString {
        var cursor = 0
        links.sortedBy { it.first }.forEach { (start, end, url) ->
            if (start < cursor || end > styledText.length) return@forEach
            append(styledText.subSequence(cursor, start))
            withLink(LinkAnnotation.Clickable(url) { open(url) }) {
                pushStyle(
                    SpanStyle(
                        color = MaterialTheme.colorScheme.primary,
                        textDecoration = TextDecoration.Underline,
                    ),
                )
                append(styledText.subSequence(start, end))
                pop()
            }
            cursor = end
        }
        append(styledText.subSequence(cursor, styledText.length))
    }
    Text(
        annotated,
        modifier = Modifier.testTag("about-linked-text-$resourceId"),
        onTextLayout = { layout ->
            if (onLinkBounds != null) {
                links.forEach { (start, end, _) ->
                    if (start < 0 || end > annotated.length || start >= end) return@forEach
                    val first = layout.getBoundingBox(start)
                    var left = first.left
                    var top = first.top
                    var right = first.right
                    var bottom = first.bottom
                    (start + 1 until end).forEach { offset ->
                        val box = layout.getBoundingBox(offset)
                        left = minOf(left, box.left)
                        top = minOf(top, box.top)
                        right = maxOf(right, box.right)
                        bottom = maxOf(bottom, box.bottom)
                    }
                    onLinkBounds(resourceId, Rect(left, top, right, bottom))
                }
            }
        },
    )
}

internal fun sourceUrlRange(text: String): Pair<Int, Int>? {
    val url = "https://github.com/kosharskyi/secu3droid"
    val start = text.indexOf(url)
    return if (start >= 0) start to start + url.length else null
}
