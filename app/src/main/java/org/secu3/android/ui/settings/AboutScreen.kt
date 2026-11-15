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

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.secu3.android.BuildConfig
import org.secu3.android.R
import org.secu3.android.ui.compose.SecuTopAppBar
import org.secu3.android.ui.compose.SettingsActionRow
import org.secu3.android.ui.compose.SettingsSection
import org.threeten.bp.LocalDateTime

@Composable
internal fun AboutScreen(
    onNavigateUp: () -> Unit,
    onOpenLink: (String) -> Unit,
) {
    val year = LocalDateTime.now().year
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            SecuTopAppBar(
                title = stringResource(R.string.pref_about_title),
                onNavigateUp = onNavigateUp,
            )
        },
    ) { contentPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(contentPadding)
                .testTag("about-viewport").verticalScroll(rememberScrollState()).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier.widthIn(max = 640.dp).fillMaxWidth().testTag("about-content"),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_launcher),
                        contentDescription = null,
                        modifier = Modifier.size(56.dp),
                    )
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineSmall)
                        Text(
                            stringResource(R.string.about_version, BuildConfig.VERSION_NAME),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Text(stringResource(R.string.about_app_desc), style = MaterialTheme.typography.bodyLarge)
                SettingsSection(title = stringResource(R.string.about_community)) {
                    AboutLink(R.string.about_forum) {
                        onOpenLink("https://secu-3.org/forum/viewtopic.php?f=8&t=11&start=2147483647")
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    AboutLink(R.string.about_telegram) { onOpenLink("https://t.me/+JrqNrXQddPQzOThi") }
                }
                SettingsSection(title = stringResource(R.string.about_project)) {
                    AboutLink(R.string.about_website) { onOpenLink("http://secu-3.org") }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    AboutLink(R.string.about_license) {
                        onOpenLink("http://www.gnu.org/licenses/gpl-3.0-standalone.html")
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    AboutLink(R.string.about_sources) { onOpenLink("https://github.com/kosharskyi/secu3droid") }
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        stringResource(R.string.about_copyright_1, year),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        stringResource(R.string.about_copyright_2, year),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun AboutLink(titleRes: Int, onClick: () -> Unit) {
    SettingsActionRow(
        title = stringResource(titleRes),
        onClick = onClick,
        value = {
            Icon(
                Icons.AutoMirrored.Filled.OpenInNew,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
    )
}
